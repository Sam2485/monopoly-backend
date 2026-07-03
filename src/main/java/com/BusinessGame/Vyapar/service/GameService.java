package com.BusinessGame.Vyapar.service;

import com.BusinessGame.Vyapar.common.enums.GameStatus;
import com.BusinessGame.Vyapar.common.exception.GameNotFoundException;
import com.BusinessGame.Vyapar.config.model.PropertyConfig;
import com.BusinessGame.Vyapar.dto.GameStateResponse;
import com.BusinessGame.Vyapar.dto.PlayerResponse;
import com.BusinessGame.Vyapar.dto.PropertyResponse;
import com.BusinessGame.Vyapar.entity.Game;
import com.BusinessGame.Vyapar.entity.OwnedProperty;
import com.BusinessGame.Vyapar.entity.Player;
import com.BusinessGame.Vyapar.repository.GameRepository;
import com.BusinessGame.Vyapar.repository.OwnedPropertyRepository;
import com.BusinessGame.Vyapar.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final OwnedPropertyRepository ownedPropertyRepository;
    private final JsonLoaderService jsonLoaderService;

    public GameService(GameRepository gameRepository,
                       PlayerRepository playerRepository,
                       OwnedPropertyRepository ownedPropertyRepository,
                       JsonLoaderService jsonLoaderService) {
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.ownedPropertyRepository = ownedPropertyRepository;
        this.jsonLoaderService = jsonLoaderService;
    }

    @Transactional(readOnly = true)
    public GameStateResponse getGameState(UUID gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with ID: " + gameId));

        List<Player> players = playerRepository.findByGameId(gameId);
        List<PlayerResponse> playerResponses = players.stream()
                .map(p -> new PlayerResponse(
                        p.getId(),
                        p.getUsername(),
                        p.getBalance(),
                        p.getPosition(),
                        p.getNumberOfProperties(),
                        p.getStatus(),
                        p.getConnected(),
                        p.getHasBuiltHouseThisTurn(),
                        p.getTokenColor() != null ? p.getTokenColor() : "#a855f7"
                )).collect(Collectors.toList());

        List<OwnedProperty> ownedProperties = ownedPropertyRepository.findByGameId(gameId);
        Map<Integer, OwnedProperty> ownedMap = ownedProperties.stream()
                .collect(Collectors.toMap(OwnedProperty::getPropertyId, op -> op));

        List<PropertyResponse> propertyResponses = new ArrayList<>();
        Collection<PropertyConfig> propertyConfigs = jsonLoaderService.getAllPropertyConfigs();
        for (PropertyConfig config : propertyConfigs) {
            OwnedProperty op = ownedMap.get(config.getId());
            PropertyResponse propResp = new PropertyResponse();
            propResp.setPropertyId(config.getId());
            propResp.setPropertyName(config.getName());
            propResp.setGroup(config.getGroup());
            if (op != null) {
                propResp.setOwnerId(op.getOwnerId());
                propResp.setDevelopmentLevel(op.getDevelopmentLevel());
                propResp.setMortgaged(op.getMortgaged());
            } else {
                propResp.setOwnerId(null);
                propResp.setDevelopmentLevel(0);
                propResp.setMortgaged(false);
            }
            propertyResponses.add(propResp);
        }

        // Sort by ID for consistency
        propertyResponses.sort(Comparator.comparing(PropertyResponse::getPropertyId));

        GameStateResponse state = new GameStateResponse();
        state.setGameId(game.getId());
        state.setStatus(game.getStatus());
        state.setCurrentTurnPlayerId(game.getCurrentTurnPlayerId());
        state.setPlayers(playerResponses);
        state.setProperties(propertyResponses);
        state.setPendingAction(game.getPendingAction());
        state.setDice(null); // Loaded on demand when rolled
        state.setRestRoomPool(game.getRestRoomPool());
        state.setHasRolled(game.getHasRolled());

        return state;
    }
}
