package com.BusinessGame.Vyapar.service;

import com.BusinessGame.Vyapar.common.enums.*;
import com.BusinessGame.Vyapar.common.exception.*;
import com.BusinessGame.Vyapar.dto.PlayerLobbyResponse;
import com.BusinessGame.Vyapar.dto.RoomResponse;
import com.BusinessGame.Vyapar.entity.*;
import com.BusinessGame.Vyapar.repository.*;
import com.BusinessGame.Vyapar.websocket.GameEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final GameRoomRepository gameRoomRepository;
    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final AuthenticationService authenticationService;
    private final TransactionService transactionService;
    private final GameEventPublisher eventPublisher;
    private final JsonLoaderService jsonLoaderService;
    private final Random random = new SecureRandom();

    public RoomService(GameRoomRepository gameRoomRepository,
                       PlayerRepository playerRepository,
                       UserRepository userRepository,
                       GameRepository gameRepository,
                       AuthenticationService authenticationService,
                       TransactionService transactionService,
                       GameEventPublisher eventPublisher,
                       JsonLoaderService jsonLoaderService) {
        this.gameRoomRepository = gameRoomRepository;
        this.playerRepository = playerRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.authenticationService = authenticationService;
        this.transactionService = transactionService;
        this.eventPublisher = eventPublisher;
        this.jsonLoaderService = jsonLoaderService;
    }

    @Transactional
    public RoomResponse createRoom(Integer maxPlayers) {
        if (maxPlayers == null || maxPlayers < 2 || maxPlayers > 6) {
            throw new IllegalArgumentException("Max players must be between 2 and 6");
        }

        User currentUser = authenticationService.getCurrentUser();

        // Generate unique room code
        String roomCode;
        do {
            roomCode = generateRoomCode();
        } while (gameRoomRepository.existsByRoomCode(roomCode));

        GameRoom room = new GameRoom();
        room.setId(UUID.randomUUID());
        room.setRoomCode(roomCode);
        room.setHostId(currentUser.getId());
        room.setMaxPlayers(maxPlayers);
        room.setCurrentPlayers(1);
        room.setStatus(RoomStatus.WAITING);
        room.setCreatedAt(LocalDateTime.now());
        gameRoomRepository.save(room);

        // Host automatically joins as the first player
        Player hostPlayer = new Player();
        hostPlayer.setId(UUID.randomUUID());
        hostPlayer.setGameId(room.getId());
        hostPlayer.setUserId(currentUser.getId());
        hostPlayer.setUsername(currentUser.getUsername());
        hostPlayer.setBalance(jsonLoaderService.getGameRules().getStartingMoney());
        hostPlayer.setPosition(0);
        hostPlayer.setNumberOfProperties(0);
        hostPlayer.setConsecutiveDoubles(0);
        hostPlayer.setJailTurns(0);
        hostPlayer.setSkippedTurns(0);
        hostPlayer.setConnected(true);
        hostPlayer.setReady(true); // Host is automatically ready
        hostPlayer.setStatus(PlayerStatus.ACTIVE);
        playerRepository.save(hostPlayer);

        return getRoomResponse(room);
    }

    @Transactional
    public RoomResponse joinRoom(String roomCode) {
        User currentUser = authenticationService.getCurrentUser();
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new VyaparException("Room not found with code: " + roomCode, "ROOM_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new GameAlreadyStartedException("Game has already started in this room");
        }

        // Check if player is already in the room
        Optional<Player> existingPlayer = playerRepository.findByGameIdAndUserId(room.getId(), currentUser.getId());
        if (existingPlayer.isPresent()) {
            return getRoomResponse(room);
        }

        if (room.getCurrentPlayers() >= room.getMaxPlayers()) {
            throw new VyaparException("Room is full", "ROOM_FULL", HttpStatus.BAD_REQUEST);
        }

        Player player = new Player();
        player.setId(UUID.randomUUID());
        player.setGameId(room.getId());
        player.setUserId(currentUser.getId());
        player.setUsername(currentUser.getUsername());
        player.setBalance(jsonLoaderService.getGameRules().getStartingMoney());
        player.setPosition(0);
        player.setNumberOfProperties(0);
        player.setConsecutiveDoubles(0);
        player.setJailTurns(0);
        player.setSkippedTurns(0);
        player.setConnected(true);
        player.setReady(false);
        player.setStatus(PlayerStatus.ACTIVE);
        playerRepository.save(player);

        room.setCurrentPlayers(room.getCurrentPlayers() + 1);
        gameRoomRepository.save(room);

        RoomResponse response = getRoomResponse(room);
        eventPublisher.publishRoomUpdate(room.getId(), response);

        return response;
    }

    @Transactional
    public RoomResponse leaveRoom(UUID roomId) {
        User currentUser = authenticationService.getCurrentUser();
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new VyaparException("Room not found", "ROOM_NOT_FOUND", HttpStatus.NOT_FOUND));

        Player player = playerRepository.findByGameIdAndUserId(roomId, currentUser.getId())
                .orElseThrow(() -> new PlayerNotFoundException("Player not found in room"));

        playerRepository.delete(player);

        int activePlayersCount = room.getCurrentPlayers() - 1;
        if (activePlayersCount <= 0) {
            gameRoomRepository.delete(room);
            return null;
        }

        room.setCurrentPlayers(activePlayersCount);

        // If host leaves, assign a new host
        if (room.getHostId().equals(currentUser.getId())) {
            List<Player> remainingPlayers = playerRepository.findByGameId(roomId);
            if (!remainingPlayers.isEmpty()) {
                Player newHost = remainingPlayers.get(0);
                room.setHostId(newHost.getUserId());
                newHost.setReady(true); // new host is ready
                playerRepository.save(newHost);
            }
        }
        gameRoomRepository.save(room);

        RoomResponse response = getRoomResponse(room);
        eventPublisher.publishRoomUpdate(room.getId(), response);

        return response;
    }

    @Transactional
    public RoomResponse readyPlayer(UUID roomId) {
        User currentUser = authenticationService.getCurrentUser();
        Player player = playerRepository.findByGameIdAndUserId(roomId, currentUser.getId())
                .orElseThrow(() -> new PlayerNotFoundException("Player not found in room"));

        // Host is always ready, toggle ready status only for non-host
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new VyaparException("Room not found", "ROOM_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!room.getHostId().equals(currentUser.getId())) {
            player.setReady(!player.getReady());
            playerRepository.save(player);
        }

        RoomResponse response = getRoomResponse(room);
        eventPublisher.publishRoomUpdate(room.getId(), response);

        return response;
    }

    @Transactional
    public Game startGame(UUID roomId) {
        User currentUser = authenticationService.getCurrentUser();
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new VyaparException("Room not found", "ROOM_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!room.getHostId().equals(currentUser.getId())) {
            throw new VyaparException("Only the host can start the game", "NOT_HOST", HttpStatus.FORBIDDEN);
        }

        List<Player> players = playerRepository.findByGameId(roomId);
        if (players.size() < 2) {
            throw new VyaparException("At least 2 players are required to start", "NOT_ENOUGH_PLAYERS", HttpStatus.BAD_REQUEST);
        }

        boolean allReady = players.stream().allMatch(Player::getReady);
        if (!allReady) {
            throw new VyaparException("All players must be ready to start", "PLAYERS_NOT_READY", HttpStatus.BAD_REQUEST);
        }

        room.setStatus(RoomStatus.PLAYING);
        gameRoomRepository.save(room);

        // Initialize Game
        Game game = new Game();
        game.setId(room.getId()); // Use same ID as room for 1-to-1 matching
        game.setRoomId(room.getId());
        game.setStatus(GameStatus.STARTED);
        game.setPendingAction(PendingAction.NONE);

        // Assign turn order randomly
        Collections.shuffle(players);
        for (int i = 0; i < players.size(); i++) {
            // Keep balance and starting position
            Player p = players.get(i);
            p.setPosition(0);
            p.setBalance(jsonLoaderService.getGameRules().getStartingMoney());
            playerRepository.save(p);
        }

        game.setCurrentTurnPlayerId(players.get(0).getId());
        game.setPendingAction(PendingAction.NONE); // Waiting for current player to roll dice
        gameRepository.save(game);

        transactionService.recordTransaction(
                game.getId(),
                players.get(0).getId(),
                TransactionType.START_REWARD,
                0,
                "Game Started! Turn order: " + players.stream().map(Player::getUsername).collect(Collectors.joining(" -> "))
        );

        // Broadcast room status transition to all joined clients
        eventPublisher.publishRoomUpdate(room.getId(), getRoomResponse(room));

        return game;
    }

    private RoomResponse getRoomResponse(GameRoom room) {
        List<Player> players = playerRepository.findByGameId(room.getId());
        List<PlayerLobbyResponse> playerLobbies = players.stream()
                .map(p -> new PlayerLobbyResponse(
                        p.getId(),
                        p.getUsername(),
                        "https://api.dicebear.com/7.x/pixel-art/svg?seed=" + p.getUsername(),
                        p.getReady(),
                        p.getUserId().equals(room.getHostId())
                )).collect(Collectors.toList());

        return new RoomResponse(
                room.getId(),
                room.getRoomCode(),
                room.getStatus().name(),
                playerLobbies
        );
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
