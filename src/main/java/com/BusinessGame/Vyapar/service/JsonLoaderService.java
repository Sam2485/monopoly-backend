package com.BusinessGame.Vyapar.service;

import com.BusinessGame.Vyapar.config.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
public class JsonLoaderService implements InitializingBean {

    private final ObjectMapper objectMapper;

    private List<BoardTile> boardTiles = new ArrayList<>();
    private Map<Integer, PropertyConfig> propertiesMap = new HashMap<>();
    private List<CardConfig> chanceRewards = new ArrayList<>();
    private List<CardConfig> chancePunishments = new ArrayList<>();
    private List<CardConfig> communityRewards = new ArrayList<>();
    private List<CardConfig> communityPunishments = new ArrayList<>();
    private GameRules gameRules;

    public JsonLoaderService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        loadConfigurations();
    }

    private void loadConfigurations() throws Exception {
        log.info("Loading static game configurations from JSON...");

        // 1. Load Board Layout
        try (InputStream is = new ClassPathResource("board.json").getInputStream()) {
            boardTiles = objectMapper.readValue(is, new TypeReference<List<BoardTile>>() {});
            log.info("Loaded {} board tiles.", boardTiles.size());
        }

        // 2. Load Properties
        try (InputStream is = new ClassPathResource("properties.json").getInputStream()) {
            List<PropertyConfig> propertiesList = objectMapper.readValue(is, new TypeReference<List<PropertyConfig>>() {});
            for (PropertyConfig prop : propertiesList) {
                propertiesMap.put(prop.getId(), prop);
            }
            log.info("Loaded {} property configurations.", propertiesMap.size());
        }

        // 3. Load Cards
        try (InputStream is = new ClassPathResource("chance-reward.json").getInputStream()) {
            chanceRewards = objectMapper.readValue(is, new TypeReference<List<CardConfig>>() {});
        }
        try (InputStream is = new ClassPathResource("chance-punishment.json").getInputStream()) {
            chancePunishments = objectMapper.readValue(is, new TypeReference<List<CardConfig>>() {});
        }
        try (InputStream is = new ClassPathResource("community-reward.json").getInputStream()) {
            communityRewards = objectMapper.readValue(is, new TypeReference<List<CardConfig>>() {});
        }
        try (InputStream is = new ClassPathResource("community-punishment.json").getInputStream()) {
            communityPunishments = objectMapper.readValue(is, new TypeReference<List<CardConfig>>() {});
            log.info("Loaded chance and community chest cards.");
        }

        // 4. Load Game Rules
        try (InputStream is = new ClassPathResource("game-rules.json").getInputStream()) {
            gameRules = objectMapper.readValue(is, GameRules.class);
            log.info("Loaded game rules: {}", gameRules);
        }

        validateConfigurations();
    }

    private void validateConfigurations() {
        if (boardTiles.size() != 36) {
            throw new IllegalStateException("Board configuration must have exactly 36 tiles!");
        }
        // Verify unique positions
        Set<Integer> positions = new HashSet<>();
        for (BoardTile tile : boardTiles) {
            if (!positions.add(tile.getPosition())) {
                throw new IllegalStateException("Duplicate position found in board.json: " + tile.getPosition());
            }
            if (tile.getPropertyId() != null && !propertiesMap.containsKey(tile.getPropertyId())) {
                throw new IllegalStateException("Property ID " + tile.getPropertyId() + " at position " + tile.getPosition() + " does not exist in properties.json!");
            }
        }
        log.info("Configurations validated successfully.");
    }

    public List<BoardTile> getBoardTiles() {
        return boardTiles;
    }

    public BoardTile getBoardTile(int position) {
        if (position < 0 || position >= boardTiles.size()) {
            throw new IllegalArgumentException("Invalid board position: " + position);
        }
        return boardTiles.get(position);
    }

    public PropertyConfig getPropertyConfig(int propertyId) {
        return propertiesMap.get(propertyId);
    }

    public Collection<PropertyConfig> getAllPropertyConfigs() {
        return propertiesMap.values();
    }

    public List<CardConfig> getChanceRewards() {
        return chanceRewards;
    }

    public List<CardConfig> getChancePunishments() {
        return chancePunishments;
    }

    public List<CardConfig> getCommunityRewards() {
        return communityRewards;
    }

    public List<CardConfig> getCommunityPunishments() {
        return communityPunishments;
    }

    public GameRules getGameRules() {
        return gameRules;
    }
}
