package com.BusinessGame.Vyapar.service;

import com.BusinessGame.Vyapar.common.enums.TransactionType;
import com.BusinessGame.Vyapar.common.exception.*;
import com.BusinessGame.Vyapar.config.model.PropertyConfig;
import com.BusinessGame.Vyapar.entity.OwnedProperty;
import com.BusinessGame.Vyapar.entity.Player;
import com.BusinessGame.Vyapar.entity.Game;
import com.BusinessGame.Vyapar.config.model.BoardTile;
import com.BusinessGame.Vyapar.repository.OwnedPropertyRepository;
import com.BusinessGame.Vyapar.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PropertyService {

    private final PlayerRepository playerRepository;
    private final OwnedPropertyRepository ownedPropertyRepository;
    private final JsonLoaderService jsonLoaderService;
    private final TransactionService transactionService;

    public PropertyService(PlayerRepository playerRepository,
                           OwnedPropertyRepository ownedPropertyRepository,
                           JsonLoaderService jsonLoaderService,
                           TransactionService transactionService) {
        this.playerRepository = playerRepository;
        this.ownedPropertyRepository = ownedPropertyRepository;
        this.jsonLoaderService = jsonLoaderService;
        this.transactionService = transactionService;
    }

    @Transactional
    public void buyProperty(UUID gameId, UUID playerId, Integer propertyId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found"));
        PropertyConfig config = jsonLoaderService.getPropertyConfig(propertyId);
        if (config == null) {
            throw new PropertyNotFoundException("Property configuration not found for ID: " + propertyId);
        }

        Optional<OwnedProperty> existingOwner = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, propertyId);
        if (existingOwner.isPresent()) {
            throw new PropertyAlreadyOwnedException("Property is already owned");
        }

        int price = config.getPrice();
        if (player.getBalance() < price) {
            throw new InsufficientBalanceException("Insufficient balance to buy property");
        }

        // Deduct price and save player
        player.setBalance(player.getBalance() - price);
        player.setNumberOfProperties(player.getNumberOfProperties() + 1);
        playerRepository.save(player);

        // Save ownership
        OwnedProperty ownership = new OwnedProperty(gameId, propertyId, playerId, 0, false, null);
        ownedPropertyRepository.save(ownership);

        // Record transaction
        transactionService.recordTransaction(
                gameId,
                playerId,
                TransactionType.PROPERTY_PURCHASE,
                price,
                player.getUsername() + " bought property " + config.getName() + " for ₹" + price
        );
    }

    @Transactional
    public void buildHouse(Game game, UUID playerId, Integer propertyId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found"));
        PropertyConfig config = jsonLoaderService.getPropertyConfig(propertyId);
        if (config == null || !"PROPERTY".equals(config.getType().name())) {
            throw new PropertyNotFoundException("Only standard properties can have houses");
        }

        if (!Boolean.TRUE.equals(game.getHasRolled())) {
            throw new VyaparException("You must roll the dice before building a house.", "MUST_ROLL_FIRST", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        if (Boolean.TRUE.equals(player.getHasBuiltHouseThisTurn())) {
            throw new VyaparException("You can only build one house/hotel per landing.", "ALREADY_BUILT_THIS_TURN", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        BoardTile tile = jsonLoaderService.getBoardTile(player.getPosition());
        if (tile.getPropertyId() == null || !tile.getPropertyId().equals(propertyId)) {
            throw new VyaparException("You can only build a house on the property you currently landed on.", "MUST_LAND_ON_PROPERTY", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        OwnedProperty ownership = ownedPropertyRepository.findByGameIdAndPropertyId(game.getId(), propertyId)
                .orElseThrow(() -> new VyaparException("Property not owned by anyone", "PROPERTY_NOT_OWNED", org.springframework.http.HttpStatus.BAD_REQUEST));

        if (!ownership.getOwnerId().equals(playerId)) {
            throw new VyaparException("You do not own this property", "NOT_PROPERTY_OWNER", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        if (ownership.getMortgaged()) {
            throw new MortgageException("Property is mortgaged, cannot build houses");
        }

        if (ownership.getDevelopmentLevel() >= 3) {
            throw new HouseBuildException("Property already has maximum of 3 houses");
        }

        // Check majority ownership: must own at least 3 out of 5 properties in the same color group
        String group = config.getGroup();
        List<PropertyConfig> groupConfigs = jsonLoaderService.getAllPropertyConfigs().stream()
                .filter(p -> group.equals(p.getGroup()))
                .collect(Collectors.toList());

        long ownedInGroup = groupConfigs.stream()
                .filter(p -> ownedPropertyRepository.findByGameIdAndPropertyId(game.getId(), p.getId())
                        .map(op -> op.getOwnerId().equals(playerId) && !op.getMortgaged())
                        .orElse(false))
                .count();

        if (ownedInGroup < 3) {
            throw new VyaparException("You need majority ownership (at least 3 active properties in " + group + ") to build", "INVALID_PROPERTY_GROUP", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        int cost = config.getHousePrice();
        if (player.getBalance() < cost) {
            throw new InsufficientBalanceException("Insufficient balance to build a house (costs ₹" + cost + ")");
        }

        // Deduct cost and set flag
        player.setBalance(player.getBalance() - cost);
        player.setHasBuiltHouseThisTurn(true);
        playerRepository.save(player);

        // Increment house count
        ownership.setDevelopmentLevel(ownership.getDevelopmentLevel() + 1);
        ownedPropertyRepository.save(ownership);

        // Record transaction
        transactionService.recordTransaction(
                game.getId(),
                playerId,
                TransactionType.HOUSE_BUILT,
                cost,
                player.getUsername() + " built house #" + ownership.getDevelopmentLevel() + " on " + config.getName() + " for ₹" + cost
        );
    }

    @Transactional
    public void buildHotel(Game game, UUID playerId, Integer propertyId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found"));
        PropertyConfig config = jsonLoaderService.getPropertyConfig(propertyId);
        if (config == null) {
            throw new PropertyNotFoundException("Property not found");
        }

        if (!Boolean.TRUE.equals(game.getHasRolled())) {
            throw new VyaparException("You must roll the dice before building a hotel.", "MUST_ROLL_FIRST", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        if (Boolean.TRUE.equals(player.getHasBuiltHouseThisTurn())) {
            throw new VyaparException("You can only build one house/hotel per landing.", "ALREADY_BUILT_THIS_TURN", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        BoardTile tile = jsonLoaderService.getBoardTile(player.getPosition());
        if (tile.getPropertyId() == null || !tile.getPropertyId().equals(propertyId)) {
            throw new VyaparException("You can only build on the property you currently landed on.", "MUST_LAND_ON_PROPERTY", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        OwnedProperty ownership = ownedPropertyRepository.findByGameIdAndPropertyId(game.getId(), propertyId)
                .orElseThrow(() -> new VyaparException("Property not owned", "PROPERTY_NOT_OWNED", org.springframework.http.HttpStatus.BAD_REQUEST));

        if (!ownership.getOwnerId().equals(playerId)) {
            throw new VyaparException("You do not own this property", "NOT_PROPERTY_OWNER", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        if (ownership.getMortgaged()) {
            throw new MortgageException("Property is mortgaged");
        }

        if (ownership.getDevelopmentLevel() != 3) {
            throw new VyaparException("You must have exactly 3 houses before building a hotel", "HOTEL_REQUIREMENT_FAILED", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        int cost = config.getHotelPrice();
        if (player.getBalance() < cost) {
            throw new InsufficientBalanceException("Insufficient balance to build hotel (costs ₹" + cost + ")");
        }

        // Deduct cost and set flag
        player.setBalance(player.getBalance() - cost);
        player.setHasBuiltHouseThisTurn(true);
        playerRepository.save(player);

        // Set to hotel (level 4)
        ownership.setDevelopmentLevel(4);
        ownedPropertyRepository.save(ownership);

        // Record transaction
        transactionService.recordTransaction(
                game.getId(),
                playerId,
                TransactionType.HOTEL_BUILT,
                cost,
                player.getUsername() + " built a hotel on " + config.getName() + " for ₹" + cost
        );
    }

    @Transactional
    public void sellHouse(UUID gameId, UUID playerId, Integer propertyId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found"));
        PropertyConfig config = jsonLoaderService.getPropertyConfig(propertyId);
        if (config == null) {
            throw new PropertyNotFoundException("Property not found");
        }

        OwnedProperty ownership = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, propertyId)
                .orElseThrow(() -> new VyaparException("Property not owned", "PROPERTY_NOT_OWNED", org.springframework.http.HttpStatus.BAD_REQUEST));

        if (!ownership.getOwnerId().equals(playerId)) {
            throw new VyaparException("You do not own this property", "NOT_PROPERTY_OWNER", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        if (ownership.getDevelopmentLevel() <= 0 || ownership.getDevelopmentLevel() > 3) {
            throw new VyaparException("No houses to sell", "NO_HOUSES_TO_SELL", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        int refund = (int) (config.getHousePrice() * 0.50);

        player.setBalance(player.getBalance() + refund);
        playerRepository.save(player);

        ownership.setDevelopmentLevel(ownership.getDevelopmentLevel() - 1);
        ownedPropertyRepository.save(ownership);

        transactionService.recordTransaction(
                gameId,
                playerId,
                TransactionType.HOUSE_SOLD,
                refund,
                player.getUsername() + " sold a house on " + config.getName() + " for a refund of ₹" + refund
        );
    }

    @Transactional
    public void sellHotel(UUID gameId, UUID playerId, Integer propertyId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found"));
        PropertyConfig config = jsonLoaderService.getPropertyConfig(propertyId);
        if (config == null) {
            throw new PropertyNotFoundException("Property not found");
        }

        OwnedProperty ownership = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, propertyId)
                .orElseThrow(() -> new VyaparException("Property not owned", "PROPERTY_NOT_OWNED", org.springframework.http.HttpStatus.BAD_REQUEST));

        if (!ownership.getOwnerId().equals(playerId)) {
            throw new VyaparException("You do not own this property", "NOT_PROPERTY_OWNER", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        if (ownership.getDevelopmentLevel() != 4) {
            throw new VyaparException("No hotel exists on this property", "NO_HOTEL_TO_SELL", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        int refund = (int) (config.getHotelPrice() * 0.50);

        player.setBalance(player.getBalance() + refund);
        playerRepository.save(player);

        // Downgrade to 3 houses
        ownership.setDevelopmentLevel(3);
        ownedPropertyRepository.save(ownership);

        transactionService.recordTransaction(
                gameId,
                playerId,
                TransactionType.HOTEL_SOLD,
                refund,
                player.getUsername() + " sold hotel on " + config.getName() + " for a refund of ₹" + refund
        );
    }

    @Transactional
    public void mortgageProperty(UUID gameId, UUID playerId, Integer propertyId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found"));
        PropertyConfig config = jsonLoaderService.getPropertyConfig(propertyId);
        if (config == null) {
            throw new PropertyNotFoundException("Property not found");
        }

        OwnedProperty ownership = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, propertyId)
                .orElseThrow(() -> new VyaparException("Property not owned", "PROPERTY_NOT_OWNED", org.springframework.http.HttpStatus.BAD_REQUEST));

        if (!ownership.getOwnerId().equals(playerId)) {
            throw new VyaparException("You do not own this property", "NOT_PROPERTY_OWNER", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        if (ownership.getMortgaged()) {
            throw new MortgageException("Property already mortgaged");
        }

        if (ownership.getDevelopmentLevel() > 0) {
            throw new MortgageException("Must sell all houses/hotels before mortgaging");
        }

        int value = (int) (config.getPrice() * 0.50);

        player.setBalance(player.getBalance() + value);
        playerRepository.save(player);

        ownership.setMortgaged(true);
        ownedPropertyRepository.save(ownership);

        transactionService.recordTransaction(
                gameId,
                playerId,
                TransactionType.MORTGAGE,
                value,
                player.getUsername() + " mortgaged " + config.getName() + " and received ₹" + value
        );
    }

    @Transactional
    public void unmortgageProperty(UUID gameId, UUID playerId, Integer propertyId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found"));
        PropertyConfig config = jsonLoaderService.getPropertyConfig(propertyId);
        if (config == null) {
            throw new PropertyNotFoundException("Property not found");
        }

        OwnedProperty ownership = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, propertyId)
                .orElseThrow(() -> new VyaparException("Property not owned", "PROPERTY_NOT_OWNED", org.springframework.http.HttpStatus.BAD_REQUEST));

        if (!ownership.getOwnerId().equals(playerId)) {
            throw new VyaparException("You do not own this property", "NOT_PROPERTY_OWNER", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        if (!ownership.getMortgaged()) {
            throw new MortgageException("Property is not mortgaged");
        }

        int mortgageValue = (int) (config.getPrice() * 0.50);
        int cost = (int) (mortgageValue * 1.10); // mortgage value + 10% interest

        if (player.getBalance() < cost) {
            throw new InsufficientBalanceException("Insufficient balance to unmortgage property (costs ₹" + cost + ")");
        }

        player.setBalance(player.getBalance() - cost);
        playerRepository.save(player);

        ownership.setMortgaged(false);
        ownedPropertyRepository.save(ownership);

        transactionService.recordTransaction(
                gameId,
                playerId,
                TransactionType.UNMORTGAGE,
                cost,
                player.getUsername() + " unmortgaged " + config.getName() + " for ₹" + cost
        );
    }

    @Transactional(readOnly = true)
    public int calculateRent(UUID gameId, Integer propertyId, int diceTotal) {
        PropertyConfig config = jsonLoaderService.getPropertyConfig(propertyId);
        if (config == null) {
            return 0;
        }

        Optional<OwnedProperty> ownershipOpt = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, propertyId);
        if (ownershipOpt.isEmpty() || ownershipOpt.get().getMortgaged()) {
            return 0;
        }

        OwnedProperty op = ownershipOpt.get();
        UUID ownerId = op.getOwnerId();

        if ("PROPERTY".equals(config.getType().name())) {
            int level = op.getDevelopmentLevel();
            return config.getRent().get(level);
        } else if ("UTILITY".equals(config.getType().name())) {
            // Check if Railway and Electricity (IDs 21 & 22) are owned by the same player
            boolean ownsRailway = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, 21)
                    .map(prop -> prop.getOwnerId().equals(ownerId) && !prop.getMortgaged())
                    .orElse(false);
            boolean ownsElectricity = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, 22)
                    .map(prop -> prop.getOwnerId().equals(ownerId) && !prop.getMortgaged())
                    .orElse(false);

            boolean paired = ownsRailway && ownsElectricity;

            if (propertyId == 21) {
                // Railway: Dice × 50. If paired: Dice × 100
                return paired ? (diceTotal * 100) : (diceTotal * 50);
            } else if (propertyId == 22) {
                // Electricity: Dice × 25. If paired: Dice × 50
                return paired ? (diceTotal * 50) : (diceTotal * 25);
            }
        } else if ("TRANSPORT".equals(config.getType().name())) {
            if (propertyId == 23 || propertyId == 24) {
                // Airway (23) and Waterway (24)
                boolean ownsAir = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, 23)
                        .map(prop -> prop.getOwnerId().equals(ownerId) && !prop.getMortgaged())
                        .orElse(false);
                boolean ownsWater = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, 24)
                        .map(prop -> prop.getOwnerId().equals(ownerId) && !prop.getMortgaged())
                        .orElse(false);
                boolean paired = ownsAir && ownsWater;

                if (propertyId == 23) {
                    return paired ? 2000 : 1000;
                } else {
                    return paired ? 1000 : 500;
                }
            } else if (propertyId == 25 || propertyId == 26) {
                // Roadway (25) and Bus Bay (26)
                boolean ownsRoad = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, 25)
                        .map(prop -> prop.getOwnerId().equals(ownerId) && !prop.getMortgaged())
                        .orElse(false);
                boolean ownsBus = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, 26)
                        .map(prop -> prop.getOwnerId().equals(ownerId) && !prop.getMortgaged())
                        .orElse(false);
                boolean paired = ownsRoad && ownsBus;

                if (propertyId == 25) {
                    return paired ? 1200 : 600;
                } else {
                    return paired ? 600 : 300;
                }
            }
        }
        return 0;
    }
}
