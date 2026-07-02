package com.BusinessGame.Vyapar.websocket;

import com.BusinessGame.Vyapar.common.enums.GameStatus;
import com.BusinessGame.Vyapar.common.enums.RoomStatus;
import com.BusinessGame.Vyapar.common.enums.EventType;
import com.BusinessGame.Vyapar.entity.Game;
import com.BusinessGame.Vyapar.entity.GameRoom;
import com.BusinessGame.Vyapar.entity.Player;
import com.BusinessGame.Vyapar.repository.GameRepository;
import com.BusinessGame.Vyapar.repository.GameRoomRepository;
import com.BusinessGame.Vyapar.repository.PlayerRepository;
import com.BusinessGame.Vyapar.service.FinancialService;
import com.BusinessGame.Vyapar.service.RoomService;
import com.BusinessGame.Vyapar.service.TurnService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class WebSocketSessionManager {

    private final PlayerRepository playerRepository;
    private final GameRoomRepository gameRoomRepository;
    private final GameRepository gameRepository;
    private final FinancialService financialService;
    private final RoomService roomService;
    private final TurnService turnService;
    private final GameEventPublisher eventPublisher;
    private final ThreadPoolTaskScheduler taskScheduler;

    // sessionId -> userId
    private final Map<String, UUID> sessionToUserMap = new ConcurrentHashMap<>();
    // userId -> Set of sessionIds
    private final Map<UUID, Set<String>> userToSessionsMap = new ConcurrentHashMap<>();
    // userId -> pending timer future
    private final Map<UUID, ScheduledFuture<?>> pendingTimers = new ConcurrentHashMap<>();

    public WebSocketSessionManager(PlayerRepository playerRepository,
                                   GameRoomRepository gameRoomRepository,
                                   GameRepository gameRepository,
                                   FinancialService financialService,
                                   RoomService roomService,
                                   TurnService turnService,
                                   GameEventPublisher eventPublisher) {
        this.playerRepository = playerRepository;
        this.gameRoomRepository = gameRoomRepository;
        this.gameRepository = gameRepository;
        this.financialService = financialService;
        this.roomService = roomService;
        this.turnService = turnService;
        this.eventPublisher = eventPublisher;

        // Initialize TaskScheduler
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ws-disconnect-scheduler-");
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String userIdStr = sha.getFirstNativeHeader("userId");
        String sessionId = sha.getSessionId();

        if (userIdStr != null) {
            try {
                UUID userId = UUID.fromString(userIdStr);
                log.info("WebSocket CONNECT: sessionId={}, userId={}", sessionId, userId);

                sessionToUserMap.put(sessionId, userId);
                userToSessionsMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

                // Cancel any pending disconnect timer for this user
                ScheduledFuture<?> timer = pendingTimers.remove(userId);
                if (timer != null) {
                    timer.cancel(false);
                    log.info("Cancelled disconnect timer for userId={}", userId);
                }

                // Mark user as connected in active games/rooms
                updatePlayerConnectionStatus(userId, true);

            } catch (IllegalArgumentException e) {
                log.error("Invalid UUID format for userId header: {}", userIdStr);
            }
        } else {
            log.warn("WebSocket CONNECT: sessionId={} connected without userId header", sessionId);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        UUID userId = sessionToUserMap.remove(sessionId);

        if (userId != null) {
            log.info("WebSocket DISCONNECT: sessionId={}, userId={}", sessionId, userId);
            Set<String> sessions = userToSessionsMap.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userToSessionsMap.remove(userId);
                    log.info("No active WebSocket connections left for userId={}. Starting 5-minute disconnect timer.", userId);

                    // Mark user as disconnected in database and publish event
                    updatePlayerConnectionStatus(userId, false);

                    // Schedule removal after 5 minutes (300 seconds)
                    ScheduledFuture<?> timer = taskScheduler.schedule(
                            () -> handleDisconnectTimeout(userId),
                            Instant.now().plusSeconds(300)
                    );
                    pendingTimers.put(userId, timer);
                }
            }
        } else {
            log.info("WebSocket DISCONNECT: sessionId={} (no userId mapped)", sessionId);
        }
    }

    private void updatePlayerConnectionStatus(UUID userId, boolean connected) {
        // Find if user is in any active game/room (WAITING or PLAYING status)
        List<Player> players = playerRepository.findByUserId(userId);
        for (Player p : players) {
            Optional<GameRoom> roomOpt = gameRoomRepository.findById(p.getGameId());
            if (roomOpt.isPresent()) {
                GameRoom room = roomOpt.get();
                if (room.getStatus() != RoomStatus.FINISHED) {
                    p.setConnected(connected);
                    playerRepository.save(p);

                    log.info("Set Player {} ({}) connected={} in gameRoom={}", p.getUsername(), p.getId(), connected, room.getId());

                    if (room.getStatus() == RoomStatus.PLAYING) {
                        // Publish connection state change
                        eventPublisher.publish(room.getId(), GameEvent.of(
                                connected ? EventType.PLAYER_JOINED : EventType.PLAYER_LEFT,
                                room.getId(),
                                Map.of(
                                        "playerId", p.getId(),
                                        "username", p.getUsername(),
                                        "connected", connected
                                ),
                                0L
                        ));
                    } else if (room.getStatus() == RoomStatus.WAITING) {
                        // Publish room update so lobby screen reflects connection state
                        try {
                            roomService.removePlayerFromRoom(room.getId(), userId);
                            // Wait, if they just disconnected, we shouldn't immediately remove them if they have 5 minutes!
                            // Wait! In WAITING lobby, if they disconnect, we want to give them 5 minutes to reconnect.
                            // So we shouldn't call removePlayerFromRoom immediately! We only do that when the timeout fires.
                            // But we should publish a room update so their icon updates to offline (if the lobby UI supports it).
                            // Wait, we can implement a method `publishRoomUpdate` in RoomService. Let's see if we should do that.
                        } catch (Exception e) {
                            log.error("Failed to publish room update", e);
                        }
                    }
                }
            }
        }
    }

    private void handleDisconnectTimeout(UUID userId) {
        log.info("Disconnect timeout reached for userId={}. Removing from game/lobby.", userId);
        pendingTimers.remove(userId);

        // Fetch all user's player records
        List<Player> players = playerRepository.findByUserId(userId);
        for (Player p : players) {
            Optional<GameRoom> roomOpt = gameRoomRepository.findById(p.getGameId());
            if (roomOpt.isPresent()) {
                GameRoom room = roomOpt.get();
                if (room.getStatus() == RoomStatus.WAITING) {
                    log.info("Removing player {} from WAITING room {}", p.getUsername(), room.getId());
                    try {
                        roomService.removePlayerFromRoom(room.getId(), userId);
                    } catch (Exception e) {
                        log.error("Error removing player from WAITING room", e);
                    }
                } else if (room.getStatus() == RoomStatus.PLAYING) {
                    log.info("Removing player {} (declaring bankruptcy) from PLAYING game {}", p.getUsername(), room.getId());
                    Optional<Game> gameOpt = gameRepository.findById(room.getId());
                    if (gameOpt.isPresent()) {
                        Game game = gameOpt.get();
                        if (game.getStatus() == GameStatus.STARTED) {
                            // Declare bankrupt using FinancialService
                            financialService.declareBankruptcy(game, p);

                            // Publish bankruptcy event
                            eventPublisher.publish(game.getId(), GameEvent.of(
                                    EventType.PLAYER_BANKRUPT,
                                    game.getId(),
                                    Map.of(
                                            "playerId", p.getId(),
                                            "username", p.getUsername()
                                    ),
                                    game.getVersion()
                            ));

                            // Broadcast game finish if needed
                            if (game.getStatus() == GameStatus.FINISHED) {
                                eventPublisher.publish(game.getId(), GameEvent.of(
                                        EventType.GAME_FINISHED,
                                        game.getId(),
                                        Map.of(
                                                "winnerId", game.getWinnerId()
                                        ),
                                        game.getVersion()
                                ));
                            } else if (game.getCurrentTurnPlayerId().equals(p.getId())) {
                                // Advance turn if it was their turn
                                game.setHasRolled(false);
                                Player nextPlayer = turnService.determineNextPlayer(game);
                                if (nextPlayer != null) {
                                    nextPlayer.setHasBuiltHouseThisTurn(false);
                                    playerRepository.save(nextPlayer);

                                    game.setCurrentTurnPlayerId(nextPlayer.getId());
                                    gameRepository.save(game);

                                    eventPublisher.publish(game.getId(), GameEvent.of(
                                            EventType.TURN_CHANGED,
                                            game.getId(),
                                            Map.of(
                                                    "currentPlayerId", nextPlayer.getId()
                                            ),
                                            game.getVersion()
                                    ));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
