package com.BusinessGame.Vyapar.service;

import com.BusinessGame.Vyapar.common.enums.*;
import com.BusinessGame.Vyapar.common.exception.*;
import com.BusinessGame.Vyapar.config.model.BoardTile;
import com.BusinessGame.Vyapar.config.model.CardConfig;
import com.BusinessGame.Vyapar.config.model.PropertyConfig;
import com.BusinessGame.Vyapar.dto.ActionRequest;
import com.BusinessGame.Vyapar.dto.DiceResponse;
import com.BusinessGame.Vyapar.dto.GameStateResponse;
import com.BusinessGame.Vyapar.entity.Game;
import com.BusinessGame.Vyapar.entity.OwnedProperty;
import com.BusinessGame.Vyapar.entity.Player;
import com.BusinessGame.Vyapar.repository.GameRepository;
import com.BusinessGame.Vyapar.repository.OwnedPropertyRepository;
import com.BusinessGame.Vyapar.repository.PlayerRepository;
import com.BusinessGame.Vyapar.websocket.GameEvent;
import com.BusinessGame.Vyapar.websocket.GameEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class GameEngineFacade {

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final OwnedPropertyRepository ownedPropertyRepository;
    private final DiceService diceService;
    private final PropertyService propertyService;
    private final TaxService taxService;
    private final CardService cardService;
    private final FinancialService financialService;
    private final TurnService turnService;
    private final TransactionService transactionService;
    private final GameEventPublisher eventPublisher;
    private final JsonLoaderService jsonLoaderService;
    private final GameService gameService;

    public GameEngineFacade(GameRepository gameRepository,
                            PlayerRepository playerRepository,
                            OwnedPropertyRepository ownedPropertyRepository,
                            DiceService diceService,
                            PropertyService propertyService,
                            TaxService taxService,
                            CardService cardService,
                            FinancialService financialService,
                            TurnService turnService,
                            TransactionService transactionService,
                            GameEventPublisher eventPublisher,
                            JsonLoaderService jsonLoaderService,
                            GameService gameService) {
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.ownedPropertyRepository = ownedPropertyRepository;
        this.diceService = diceService;
        this.propertyService = propertyService;
        this.taxService = taxService;
        this.cardService = cardService;
        this.financialService = financialService;
        this.turnService = turnService;
        this.transactionService = transactionService;
        this.eventPublisher = eventPublisher;
        this.jsonLoaderService = jsonLoaderService;
        this.gameService = gameService;
    }

    @Transactional
    public GameStateResponse performAction(UUID gameId, UUID playerId, ActionRequest request) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found"));

        if (game.getStatus() == GameStatus.FINISHED) {
            throw new VyaparException("Game already finished", "GAME_ALREADY_FINISHED", HttpStatus.BAD_REQUEST);
        }

        // Verify turn: turn holder must be current player
        boolean isTurn = game.getCurrentTurnPlayerId().equals(playerId);
        if (!isTurn && player.getStatus() != PlayerStatus.RECOVERY) {
            throw new InvalidTurnException("It is not your turn");
        }

        ActionType action = request.getAction();
        log.info("Processing action {} for player {} in game {}", action, player.getUsername(), gameId);

        switch (action) {
            case ROLL_DICE:
                handleRollDice(game, player);
                break;
            case BUY_PROPERTY:
                handleBuyProperty(game, player, request.getPropertyId());
                break;
            case SKIP_PROPERTY:
                handleSkipProperty(game, player);
                break;
            case BUILD_HOUSE:
                handleBuildHouse(game, player, request.getPropertyId());
                break;
            case BUILD_HOTEL:
                handleBuildHotel(game, player, request.getPropertyId());
                break;
            case SELL_HOUSE:
                handleSellHouse(game, player, request.getPropertyId());
                break;
            case SELL_HOTEL:
                handleSellHotel(game, player, request.getPropertyId());
                break;
            case MORTGAGE:
                handleMortgage(game, player, request.getPropertyId());
                break;
            case UNMORTGAGE:
                handleUnmortgage(game, player, request.getPropertyId());
                break;
            case PAY_BAIL:
                handlePayBail(game, player);
                break;
            case END_TURN:
                handleEndTurn(game, player);
                break;
            default:
                throw new IllegalArgumentException("Unknown action type: " + action);
        }

        // Return latest state
        return gameService.getGameState(gameId);
    }

    private void handleRollDice(Game game, Player player) {
        if (player.getStatus() == PlayerStatus.IN_JAIL) {
            throw new VyaparException("Cannot roll dice while in Jail. Please pay bail or end turn to skip.", "PLAYER_IN_JAIL", HttpStatus.BAD_REQUEST);
        }
        if (game.getPendingAction() != PendingAction.NONE) {
            throw new VyaparException("Must resolve pending action first: " + game.getPendingAction(), "PENDING_ACTION_ACTIVE", HttpStatus.BAD_REQUEST);
        }
        if (game.getHasRolled()) {
            throw new InvalidTurnException("You have already rolled the dice this turn.");
        }

        DiceResponse dice = diceService.rollDice();
        game.setHasRolled(true);

        if (dice.isDouble()) {
            player.setConsecutiveDoubles(player.getConsecutiveDoubles() + 1);
            if (player.getConsecutiveDoubles() == 3) {
                // Sent to jail
                player.setPosition(9);
                player.setStatus(PlayerStatus.IN_JAIL);
                player.setJailTurns(0);
                player.setConsecutiveDoubles(0);
                playerRepository.save(player);

                transactionService.recordTransaction(
                        game.getId(),
                        player.getId(),
                        TransactionType.BAIL_PAYMENT,
                        0,
                        player.getUsername() + " rolled 3 consecutive doubles and was sent to Jail."
                );

                // Broadcast Dice Rolled Event immediately for 3 doubles
                eventPublisher.publish(game.getId(), GameEvent.of(EventType.DICE_ROLLED, game.getId(), Map.of(
                        "playerId", player.getId(),
                        "diceOne", dice.getDiceOne(),
                        "diceTwo", dice.getDiceTwo(),
                        "total", dice.getTotal(),
                        "isDouble", dice.isDouble()
                ), game.getVersion()));

                eventPublisher.publish(game.getId(), GameEvent.of(EventType.PLAYER_SENT_TO_JAIL, game.getId(), Map.of(
                        "playerId", player.getId(),
                        "reason", "THREE_DOUBLES"
                ), game.getVersion()));

                // Auto end turn since player is jailed
                autoEndTurnToNextPlayer(game, player);
                return;
            }
        } else {
            player.setConsecutiveDoubles(0);
        }

        int oldPos = player.getPosition();
        int newPos = (oldPos + dice.getTotal()) % 36;

        // Check passing START
        if (newPos < oldPos || newPos == 0) {
            int reward = jsonLoaderService.getGameRules().getPassStartReward();
            player.setBalance(player.getBalance() + reward);
            transactionService.recordTransaction(
                    game.getId(),
                    player.getId(),
                    TransactionType.START_REWARD,
                    reward,
                    player.getUsername() + " passed START and collected ₹" + reward + "."
            );
            eventPublisher.publish(game.getId(), GameEvent.of(EventType.MONEY_UPDATED, game.getId(), Map.of(
                    "playerId", player.getId(),
                    "balance", player.getBalance()
            ), game.getVersion()));
        }

        player.setPosition(newPos);
        playerRepository.save(player);

        // Execute Landed Tile Logic first (updates pendingAction and balance in DB)
        executeTileLanding(game, player, newPos, dice.getTotal());

        // Broadcast Dice Rolled Event
        eventPublisher.publish(game.getId(), GameEvent.of(EventType.DICE_ROLLED, game.getId(), Map.of(
                "playerId", player.getId(),
                "diceOne", dice.getDiceOne(),
                "diceTwo", dice.getDiceTwo(),
                "total", dice.getTotal(),
                "isDouble", dice.isDouble()
        ), game.getVersion()));

        // Broadcast player movement
        eventPublisher.publish(game.getId(), GameEvent.of(EventType.PLAYER_MOVED, game.getId(), Map.of(
                "playerId", player.getId(),
                "from", oldPos,
                "to", newPos
        ), game.getVersion()));
    }

    private void executeTileLanding(Game game, Player player, int position, int diceTotal) {
        BoardTile tile = jsonLoaderService.getBoardTile(position);
        log.info("Player {} landed on tile {} ({})", player.getUsername(), position, tile.getType());

        switch (tile.getType()) {
            case PROPERTY:
            case UTILITY:
            case TRANSPORT:
                int propId = tile.getPropertyId();
                Optional<OwnedProperty> existingOwner = ownedPropertyRepository.findByGameIdAndPropertyId(game.getId(), propId);
                if (existingOwner.isEmpty()) {
                    // Purchase available
                    game.setPendingAction(PendingAction.BUY_PROPERTY);
                    gameRepository.save(game);
                } else {
                    // Pay rent
                    OwnedProperty op = existingOwner.get();
                    if (!op.getOwnerId().equals(player.getId()) && !op.getMortgaged()) {
                        int rent = propertyService.calculateRent(game.getId(), propId, diceTotal);
                        if (rent > 0) {
                            Player owner = playerRepository.findById(op.getOwnerId()).orElseThrow();
                            player.setBalance(player.getBalance() - rent);
                            owner.setBalance(owner.getBalance() + rent);

                            playerRepository.save(player);
                            playerRepository.save(owner);

                            transactionService.recordTransaction(
                                    game.getId(),
                                    player.getId(),
                                    TransactionType.PROPERTY_RENT,
                                    rent,
                                    player.getUsername() + " paid rent of ₹" + rent + " to " + owner.getUsername() + " for property " + jsonLoaderService.getPropertyConfig(propId).getName()
                            );

                            eventPublisher.publish(game.getId(), GameEvent.of(EventType.RENT_PAID, game.getId(), Map.of(
                                    "fromPlayer", player.getId(),
                                    "toPlayer", owner.getId(),
                                    "amount", rent
                            ), game.getVersion()));

                            eventPublisher.publish(game.getId(), GameEvent.of(EventType.MONEY_UPDATED, game.getId(), Map.of(
                                    "playerId", player.getId(),
                                    "balance", player.getBalance()
                            ), game.getVersion()));

                            eventPublisher.publish(game.getId(), GameEvent.of(EventType.MONEY_UPDATED, game.getId(), Map.of(
                                    "playerId", owner.getId(),
                                    "balance", owner.getBalance()
                            ), game.getVersion()));

                            checkNegativeBalance(game, player);
                        }
                    }
                }
                break;
            case INCOME_TAX:
                taxService.payIncomeTax(player);
                eventPublisher.publish(game.getId(), GameEvent.of(EventType.MONEY_UPDATED, game.getId(), Map.of(
                        "playerId", player.getId(),
                        "balance", player.getBalance()
                ), game.getVersion()));
                checkNegativeBalance(game, player);
                break;
            case WEALTH_TAX:
                taxService.payWealthTax(player);
                eventPublisher.publish(game.getId(), GameEvent.of(EventType.MONEY_UPDATED, game.getId(), Map.of(
                        "playerId", player.getId(),
                        "balance", player.getBalance()
                ), game.getVersion()));
                checkNegativeBalance(game, player);
                break;
            case CHANCE:
                CardConfig chanceCard = cardService.drawChanceCard(player, diceTotal);
                eventPublisher.publish(game.getId(), GameEvent.of(EventType.PROPERTY_UPDATED, game.getId(), Map.of(
                        "card", chanceCard,
                        "playerId", player.getId(),
                        "balance", player.getBalance(),
                        "position", player.getPosition(),
                        "status", player.getStatus().name()
                ), game.getVersion()));
                checkNegativeBalance(game, player);
                break;
            case COMMUNITY_CHEST:
                CardConfig chestCard = cardService.drawCommunityCard(player, diceTotal);
                eventPublisher.publish(game.getId(), GameEvent.of(EventType.PROPERTY_UPDATED, game.getId(), Map.of(
                        "card", chestCard,
                        "playerId", player.getId(),
                        "balance", player.getBalance(),
                        "position", player.getPosition(),
                        "status", player.getStatus().name()
                ), game.getVersion()));
                checkNegativeBalance(game, player);
                break;
            case REST_ROOM:
                player.setSkippedTurns(1);
                playerRepository.save(player);
                transactionService.recordTransaction(
                        game.getId(),
                        player.getId(),
                        TransactionType.CLUB_PAYMENT,
                        0,
                        player.getUsername() + " landed on Rest Room. Next turn will be skipped."
                );
                break;
            case CLUB:
                player.setBalance(player.getBalance() - 100);
                playerRepository.save(player);
                transactionService.recordTransaction(
                        game.getId(),
                        player.getId(),
                        TransactionType.CLUB_PAYMENT,
                        100,
                        player.getUsername() + " paid Club fee of ₹100."
                );
                eventPublisher.publish(game.getId(), GameEvent.of(EventType.MONEY_UPDATED, game.getId(), Map.of(
                        "playerId", player.getId(),
                        "balance", player.getBalance()
                ), game.getVersion()));
                checkNegativeBalance(game, player);
                break;
            case JAIL:
                // Just visiting
                break;
            default:
                break;
        }
        gameRepository.save(game);
    }

    private void checkNegativeBalance(Game game, Player player) {
        if (player.getBalance() < 0) {
            financialService.enterRecovery(game, player);
            eventPublisher.publish(game.getId(), GameEvent.of(EventType.RECOVERY_STARTED, game.getId(), Map.of(
                    "playerId", player.getId()
            ), game.getVersion()));
        }
    }

    private void handleBuyProperty(Game game, Player player, Integer propertyId) {
        if (game.getPendingAction() != PendingAction.BUY_PROPERTY) {
            throw new VyaparException("No property available to buy", "NO_PROPERTY_TO_BUY", HttpStatus.BAD_REQUEST);
        }
        propertyService.buyProperty(game.getId(), player.getId(), propertyId);
        game.setPendingAction(PendingAction.NONE);
        gameRepository.save(game);

        eventPublisher.publish(game.getId(), GameEvent.of(EventType.PROPERTY_PURCHASED, game.getId(), Map.of(
                "propertyId", propertyId,
                "ownerId", player.getId()
        ), game.getVersion()));

        eventPublisher.publish(game.getId(), GameEvent.of(EventType.MONEY_UPDATED, game.getId(), Map.of(
                "playerId", player.getId(),
                "balance", player.getBalance()
        ), game.getVersion()));
    }

    private void handleSkipProperty(Game game, Player player) {
        if (game.getPendingAction() != PendingAction.BUY_PROPERTY) {
            throw new VyaparException("No property purchase decision pending", "NO_DECISION_PENDING", HttpStatus.BAD_REQUEST);
        }
        game.setPendingAction(PendingAction.NONE);
        gameRepository.save(game);

        transactionService.recordTransaction(
                game.getId(),
                player.getId(),
                TransactionType.PROPERTY_PURCHASE,
                0,
                player.getUsername() + " skipped property purchase."
        );

        eventPublisher.publish(game.getId(), GameEvent.of(EventType.PROPERTY_UPDATED, game.getId(), Map.of(
                "playerId", player.getId(),
                "message", player.getUsername() + " skipped property purchase."
        ), game.getVersion()));
    }

    private void handleBuildHouse(Game game, Player player, Integer propertyId) {
        propertyService.buildHouse(game, player.getId(), propertyId);

        Optional<OwnedProperty> op = ownedPropertyRepository.findByGameIdAndPropertyId(game.getId(), propertyId);
        op.ifPresent(ownedProperty -> eventPublisher.publish(game.getId(), GameEvent.of(EventType.HOUSE_BUILT, game.getId(), Map.of(
                "propertyId", propertyId,
                "level", ownedProperty.getDevelopmentLevel()
        ), game.getVersion())));

        eventPublisher.publish(game.getId(), GameEvent.of(EventType.MONEY_UPDATED, game.getId(), Map.of(
                "playerId", player.getId(),
                "balance", player.getBalance()
        ), game.getVersion()));
    }

    private void handleBuildHotel(Game game, Player player, Integer propertyId) {
        propertyService.buildHotel(game, player.getId(), propertyId);

        eventPublisher.publish(game.getId(), GameEvent.of(EventType.HOTEL_BUILT, game.getId(), Map.of(
                "propertyId", propertyId
        ), game.getVersion()));

        eventPublisher.publish(game.getId(), GameEvent.of(EventType.MONEY_UPDATED, game.getId(), Map.of(
                "playerId", player.getId(),
                "balance", player.getBalance()
        ), game.getVersion()));
    }

    private void handleSellHouse(Game game, Player player, Integer propertyId) {
        propertyService.sellHouse(game.getId(), player.getId(), propertyId);

        Optional<OwnedProperty> op = ownedPropertyRepository.findByGameIdAndPropertyId(game.getId(), propertyId);
        op.ifPresent(ownedProperty -> eventPublisher.publish(game.getId(), GameEvent.of(EventType.HOUSE_BUILT, game.getId(), Map.of(
                "propertyId", propertyId,
                "level", ownedProperty.getDevelopmentLevel()
        ), game.getVersion())));

        eventPublisher.publish(game.getId(), GameEvent.of(EventType.MONEY_UPDATED, game.getId(), Map.of(
                "playerId", player.getId(),
                "balance", player.getBalance()
        ), game.getVersion()));

        if (player.getStatus() == PlayerStatus.RECOVERY) {
            boolean exited = financialService.checkAndExitRecovery(game, player);
            if (exited) {
                eventPublisher.publish(game.getId(), GameEvent.of(EventType.RECOVERY_COMPLETED, game.getId(), Map.of(
                        "playerId", player.getId()
                ), game.getVersion()));
            }
        }
    }

    private void handleSellHotel(Game game, Player player, Integer propertyId) {
        propertyService.sellHotel(game.getId(), player.getId(), propertyId);

        eventPublisher.publish(game.getId(), GameEvent.of(EventType.HOUSE_BUILT, game.getId(), Map.of(
                "propertyId", propertyId,
                "level", 3 // hotel downgraded to 3 houses
        ), game.getVersion()));

        eventPublisher.publish(game.getId(), GameEvent.of(EventType.MONEY_UPDATED, game.getId(), Map.of(
                "playerId", player.getId(),
                "balance", player.getBalance()
        ), game.getVersion()));

        if (player.getStatus() == PlayerStatus.RECOVERY) {
            boolean exited = financialService.checkAndExitRecovery(game, player);
            if (exited) {
                eventPublisher.publish(game.getId(), GameEvent.of(EventType.RECOVERY_COMPLETED, game.getId(), Map.of(
                        "playerId", player.getId()
                ), game.getVersion()));
            }
        }
    }

    private void handleMortgage(Game game, Player player, Integer propertyId) {
        propertyService.mortgageProperty(game.getId(), player.getId(), propertyId);

        eventPublisher.publish(game.getId(), GameEvent.of(EventType.PROPERTY_MORTGAGED, game.getId(), Map.of(
                "propertyId", propertyId
        ), game.getVersion()));

        eventPublisher.publish(game.getId(), GameEvent.of(EventType.MONEY_UPDATED, game.getId(), Map.of(
                "playerId", player.getId(),
                "balance", player.getBalance()
        ), game.getVersion()));

        if (player.getStatus() == PlayerStatus.RECOVERY) {
            boolean exited = financialService.checkAndExitRecovery(game, player);
            if (exited) {
                eventPublisher.publish(game.getId(), GameEvent.of(EventType.RECOVERY_COMPLETED, game.getId(), Map.of(
                        "playerId", player.getId()
                ), game.getVersion()));
            }
        }
    }

    private void handleUnmortgage(Game game, Player player, Integer propertyId) {
        propertyService.unmortgageProperty(game.getId(), player.getId(), propertyId);

        eventPublisher.publish(game.getId(), GameEvent.of(EventType.PROPERTY_UNMORTGAGED, game.getId(), Map.of(
                "propertyId", propertyId
        ), game.getVersion()));

        eventPublisher.publish(game.getId(), GameEvent.of(EventType.MONEY_UPDATED, game.getId(), Map.of(
                "playerId", player.getId(),
                "balance", player.getBalance()
        ), game.getVersion()));
    }

    private void handlePayBail(Game game, Player player) {
        if (player.getStatus() != PlayerStatus.IN_JAIL) {
            throw new VyaparException("Player is not in jail", "PLAYER_NOT_IN_JAIL", HttpStatus.BAD_REQUEST);
        }

        if (player.getBalance() < 500) {
            throw new InsufficientBalanceException("Insufficient balance to pay bail of ₹500");
        }

        player.setBalance(player.getBalance() - 500);
        player.setStatus(PlayerStatus.ACTIVE);
        player.setJailTurns(0);
        playerRepository.save(player);

        transactionService.recordTransaction(
                game.getId(),
                player.getId(),
                TransactionType.BAIL_PAYMENT,
                500,
                player.getUsername() + " paid ₹500 bail and was released from Jail."
        );

        eventPublisher.publish(game.getId(), GameEvent.of(EventType.PLAYER_RELEASED, game.getId(), Map.of(
                "playerId", player.getId()
        ), game.getVersion()));

        eventPublisher.publish(game.getId(), GameEvent.of(EventType.MONEY_UPDATED, game.getId(), Map.of(
                "playerId", player.getId(),
                "balance", player.getBalance()
        ), game.getVersion()));
    }

    private void handleEndTurn(Game game, Player player) {
        if (game.getPendingAction() != PendingAction.NONE) {
            throw new VyaparException("Cannot end turn while decision is pending: " + game.getPendingAction(), "PENDING_ACTION_ACTIVE", HttpStatus.BAD_REQUEST);
        }

        if (player.getStatus() == PlayerStatus.RECOVERY) {
            if (player.getBalance() < 0) {
                // If they have no assets to sell/mortgage, declare bankruptcy
                long ownedProps = ownedPropertyRepository.countByGameIdAndOwnerId(game.getId(), player.getId());
                if (ownedProps == 0) {
                    financialService.declareBankruptcy(game, player);
                    eventPublisher.publish(game.getId(), GameEvent.of(EventType.PLAYER_BANKRUPT, game.getId(), Map.of(
                            "playerId", player.getId()
                    ), game.getVersion()));

                    // Check if game finished
                    if (game.getStatus() == GameStatus.FINISHED) {
                        eventPublisher.publish(game.getId(), GameEvent.of(EventType.GAME_FINISHED, game.getId(), Map.of(
                                "winnerId", game.getWinnerId()
                        ), game.getVersion()));
                        return;
                    }
                } else {
                    throw new VyaparException("Must resolve negative balance before ending turn (sell assets or mortgage properties)", "RECOVERY_MODE_ACTIVE", HttpStatus.BAD_REQUEST);
                }
            } else {
                financialService.checkAndExitRecovery(game, player);
                eventPublisher.publish(game.getId(), GameEvent.of(EventType.RECOVERY_COMPLETED, game.getId(), Map.of(
                        "playerId", player.getId()
                ), game.getVersion()));
            }
        }

        // Check if player has consecutive doubles (gets extra turn)
        if (player.getConsecutiveDoubles() > 0 && player.getStatus() != PlayerStatus.IN_JAIL && player.getStatus() != PlayerStatus.BANKRUPT) {
            // Extra turn awarded
            game.setHasRolled(false);
            player.setHasBuiltHouseThisTurn(false);
            playerRepository.save(player);
            gameRepository.save(game);

            transactionService.recordTransaction(
                    game.getId(),
                    player.getId(),
                    TransactionType.START_REWARD,
                    0,
                    player.getUsername() + " gets an extra turn due to rolling a double!"
            );

            eventPublisher.publish(game.getId(), GameEvent.of(EventType.TURN_CHANGED, game.getId(), Map.of(
                    "currentPlayerId", player.getId()
            ), game.getVersion()));
        } else {
            // Standard next player turn
            autoEndTurnToNextPlayer(game, player);
        }
    }

    private void autoEndTurnToNextPlayer(Game game, Player currentPlayer) {
        currentPlayer.setHasBuiltHouseThisTurn(false);
        // Increment jail turns if ended turn while in jail
        if (currentPlayer.getStatus() == PlayerStatus.IN_JAIL) {
            currentPlayer.setJailTurns(currentPlayer.getJailTurns() + 1);
            if (currentPlayer.getJailTurns() >= 3) {
                // Force pay bail
                currentPlayer.setBalance(currentPlayer.getBalance() - 500);
                currentPlayer.setStatus(PlayerStatus.ACTIVE);
                currentPlayer.setJailTurns(0);
                playerRepository.save(currentPlayer);

                transactionService.recordTransaction(
                        game.getId(),
                        currentPlayer.getId(),
                        TransactionType.BAIL_PAYMENT,
                        500,
                        currentPlayer.getUsername() + " was in jail for 3 turns. Forced to pay ₹500 and released."
                );

                eventPublisher.publish(game.getId(), GameEvent.of(EventType.PLAYER_RELEASED, game.getId(), Map.of(
                        "playerId", currentPlayer.getId()
                ), game.getVersion()));

                eventPublisher.publish(game.getId(), GameEvent.of(EventType.MONEY_UPDATED, game.getId(), Map.of(
                        "playerId", currentPlayer.getId(),
                        "balance", currentPlayer.getBalance()
                ), game.getVersion()));
            } else {
                playerRepository.save(currentPlayer);
            }
        } else {
            playerRepository.save(currentPlayer);
        }

        game.setHasRolled(false);
        Player nextPlayer = turnService.determineNextPlayer(game);
        if (nextPlayer != null) {
            nextPlayer.setHasBuiltHouseThisTurn(false);
            playerRepository.save(nextPlayer);

            game.setCurrentTurnPlayerId(nextPlayer.getId());
            gameRepository.save(game);

            eventPublisher.publish(game.getId(), GameEvent.of(EventType.TURN_CHANGED, game.getId(), Map.of(
                    "currentPlayerId", nextPlayer.getId()
            ), game.getVersion()));
        }
    }
}
