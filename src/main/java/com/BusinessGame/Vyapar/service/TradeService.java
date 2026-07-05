package com.BusinessGame.Vyapar.service;

import com.BusinessGame.Vyapar.common.enums.*;
import com.BusinessGame.Vyapar.common.exception.*;
import com.BusinessGame.Vyapar.config.model.PropertyConfig;
import com.BusinessGame.Vyapar.dto.TradeOfferResponse;
import com.BusinessGame.Vyapar.dto.TradeProposalRequest;
import com.BusinessGame.Vyapar.entity.*;
import com.BusinessGame.Vyapar.repository.*;
import com.BusinessGame.Vyapar.websocket.GameEvent;
import com.BusinessGame.Vyapar.websocket.GameEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TradeService {

    private final TradeOfferRepository tradeOfferRepository;
    private final TradeOfferPropertyRepository tradeOfferPropertyRepository;
    private final PlayerRepository playerRepository;
    private final OwnedPropertyRepository ownedPropertyRepository;
    private final GameRepository gameRepository;
    private final JsonLoaderService jsonLoaderService;
    private final TransactionService transactionService;
    private final GameEventPublisher eventPublisher;

    public TradeService(TradeOfferRepository tradeOfferRepository,
                        TradeOfferPropertyRepository tradeOfferPropertyRepository,
                        PlayerRepository playerRepository,
                        OwnedPropertyRepository ownedPropertyRepository,
                        GameRepository gameRepository,
                        JsonLoaderService jsonLoaderService,
                        TransactionService transactionService,
                        GameEventPublisher eventPublisher) {
        this.tradeOfferRepository = tradeOfferRepository;
        this.tradeOfferPropertyRepository = tradeOfferPropertyRepository;
        this.playerRepository = playerRepository;
        this.ownedPropertyRepository = ownedPropertyRepository;
        this.gameRepository = gameRepository;
        this.jsonLoaderService = jsonLoaderService;
        this.transactionService = transactionService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TradeOfferResponse proposeTrade(UUID gameId, UUID proposerPlayerId, TradeProposalRequest request) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        if (game.getStatus() != GameStatus.STARTED) {
            throw new VyaparException("Game is not active", "GAME_NOT_ACTIVE", HttpStatus.BAD_REQUEST);
        }

        Player proposer = playerRepository.findById(proposerPlayerId)
                .orElseThrow(() -> new PlayerNotFoundException("Proposer player not found"));

        Player receiver = playerRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new PlayerNotFoundException("Receiver player not found"));

        if (proposerPlayerId.equals(request.getReceiverId())) {
            throw new VyaparException("You cannot trade with yourself", "INVALID_TRADE_PARTNERS", HttpStatus.BAD_REQUEST);
        }

        if (request.getOfferedCash() < 0 || request.getRequestedCash() < 0) {
            throw new VyaparException("Trade cash amount cannot be negative", "INVALID_CASH_AMOUNT", HttpStatus.BAD_REQUEST);
        }

        if (proposer.getBalance() < request.getOfferedCash()) {
            throw new InsufficientBalanceException("Proposer has insufficient balance");
        }

        // Limit to only one active pending trade proposal at a time
        List<TradeOffer> pendingTrades = tradeOfferRepository.findByGameIdAndProposerIdAndStatus(gameId, proposerPlayerId, TradeStatus.PENDING);
        if (!pendingTrades.isEmpty()) {
            throw new VyaparException("You already have an active pending trade proposal. Cancel or resolve it before making a new one.", "PENDING_TRADE_EXISTS", HttpStatus.BAD_REQUEST);
        }

        // Validate property ownership & improvements (Option B)
        validatePropertyOwnership(gameId, proposerPlayerId, request.getOfferedProperties());
        validatePropertyOwnership(gameId, request.getReceiverId(), request.getRequestedProperties());
        
        validateNoDevelopmentsInColorGroup(gameId, request.getOfferedProperties());
        validateNoDevelopmentsInColorGroup(gameId, request.getRequestedProperties());

        // Create Trade Offer
        TradeOffer tradeOffer = new TradeOffer();
        tradeOffer.setGameId(gameId);
        tradeOffer.setProposerId(proposerPlayerId);
        tradeOffer.setReceiverId(request.getReceiverId());
        tradeOffer.setOfferedCash(request.getOfferedCash());
        tradeOffer.setRequestedCash(request.getRequestedCash());
        tradeOffer.setStatus(TradeStatus.PENDING);
        tradeOffer = tradeOfferRepository.save(tradeOffer);

        // Save Trade Offer Properties
        UUID tradeOfferId = tradeOffer.getId();
        if (request.getOfferedProperties() != null) {
            for (Integer propId : request.getOfferedProperties()) {
                tradeOfferPropertyRepository.save(new TradeOfferProperty(tradeOfferId, propId, true));
            }
        }
        if (request.getRequestedProperties() != null) {
            for (Integer propId : request.getRequestedProperties()) {
                tradeOfferPropertyRepository.save(new TradeOfferProperty(tradeOfferId, propId, false));
            }
        }

        TradeOfferResponse response = mapToResponse(tradeOffer);

        // Broadcast event
        eventPublisher.publish(gameId, GameEvent.of(
                EventType.TRADE_PROPOSED,
                gameId,
                Map.of(
                        "tradeId", tradeOffer.getId(),
                        "proposerId", proposerPlayerId,
                        "proposerName", proposer.getUsername(),
                        "receiverId", receiver.getId(),
                        "receiverName", receiver.getUsername(),
                        "offeredCash", tradeOffer.getOfferedCash(),
                        "requestedCash", tradeOffer.getRequestedCash()
                ),
                game.getVersion()
        ));

        return response;
    }

    @Transactional(readOnly = true)
    public List<TradeOfferResponse> getPendingTrades(UUID gameId, UUID playerId) {
        // Return active pending trades involving the player as receiver or proposer
        return tradeOfferRepository.findByGameIdAndStatus(gameId, TradeStatus.PENDING).stream()
                .filter(t -> t.getProposerId().equals(playerId) || t.getReceiverId().equals(playerId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void acceptTrade(UUID gameId, UUID receiverPlayerId, UUID tradeId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        TradeOffer trade = tradeOfferRepository.findById(tradeId)
                .orElseThrow(() -> new VyaparException("Trade offer not found", "TRADE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (trade.getStatus() != TradeStatus.PENDING) {
            throw new VyaparException("Trade is no longer pending", "TRADE_NOT_PENDING", HttpStatus.BAD_REQUEST);
        }

        if (!trade.getReceiverId().equals(receiverPlayerId)) {
            throw new VyaparException("You cannot accept this trade offer", "UNAUTHORIZED_TRADE_ACCEPTANCE", HttpStatus.FORBIDDEN);
        }

        Player proposer = playerRepository.findById(trade.getProposerId())
                .orElseThrow(() -> new PlayerNotFoundException("Proposer not found"));

        Player receiver = playerRepository.findById(trade.getReceiverId())
                .orElseThrow(() -> new PlayerNotFoundException("Receiver not found"));

        // Fetch properties
        List<Integer> offeredProperties = tradeOfferPropertyRepository.findByTradeOfferId(trade.getId()).stream()
                .filter(TradeOfferProperty::getIsOffered)
                .map(TradeOfferProperty::getPropertyId)
                .collect(Collectors.toList());

        List<Integer> requestedProperties = tradeOfferPropertyRepository.findByTradeOfferId(trade.getId()).stream()
                .filter(p -> !p.getIsOffered())
                .map(TradeOfferProperty::getPropertyId)
                .collect(Collectors.toList());

        // Re-validate ownership and developments (Option B)
        validatePropertyOwnership(gameId, trade.getProposerId(), offeredProperties);
        validatePropertyOwnership(gameId, trade.getReceiverId(), requestedProperties);

        validateNoDevelopmentsInColorGroup(gameId, offeredProperties);
        validateNoDevelopmentsInColorGroup(gameId, requestedProperties);

        // Execute Cash Transfer
        proposer.setBalance(proposer.getBalance() - trade.getOfferedCash() + trade.getRequestedCash());
        receiver.setBalance(receiver.getBalance() - trade.getRequestedCash() + trade.getOfferedCash());

        // Execute Property Transfer
        for (Integer propId : offeredProperties) {
            OwnedProperty op = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, propId)
                    .orElseThrow(() -> new PropertyNotFoundException("Property not owned: " + propId));
            op.setOwnerId(receiver.getId());
            ownedPropertyRepository.save(op);

            transactionService.recordTransaction(
                    gameId,
                    proposer.getId(),
                    TransactionType.TRADE,
                    0,
                    proposer.getUsername() + " traded property " + jsonLoaderService.getPropertyConfig(propId).getName() + " to " + receiver.getUsername()
            );
        }

        for (Integer propId : requestedProperties) {
            OwnedProperty op = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, propId)
                    .orElseThrow(() -> new PropertyNotFoundException("Property not owned: " + propId));
            op.setOwnerId(proposer.getId());
            ownedPropertyRepository.save(op);

            transactionService.recordTransaction(
                    gameId,
                    receiver.getId(),
                    TransactionType.TRADE,
                    0,
                    receiver.getUsername() + " traded property " + jsonLoaderService.getPropertyConfig(propId).getName() + " to " + proposer.getUsername()
            );
        }

        // Record cash transactions if applicable
        if (trade.getOfferedCash() > 0) {
            transactionService.recordTransaction(
                    gameId,
                    proposer.getId(),
                    TransactionType.TRADE,
                    trade.getOfferedCash(),
                    proposer.getUsername() + " transferred ₹" + trade.getOfferedCash() + " to " + receiver.getUsername() + " as part of trade"
            );
        }

        if (trade.getRequestedCash() > 0) {
            transactionService.recordTransaction(
                    gameId,
                    receiver.getId(),
                    TransactionType.TRADE,
                    trade.getRequestedCash(),
                    receiver.getUsername() + " transferred ₹" + trade.getRequestedCash() + " to " + proposer.getUsername() + " as part of trade"
            );
        }

        // Update player property count fields
        proposer.setNumberOfProperties(proposer.getNumberOfProperties() - offeredProperties.size() + requestedProperties.size());
        receiver.setNumberOfProperties(receiver.getNumberOfProperties() - requestedProperties.size() + offeredProperties.size());

        playerRepository.save(proposer);
        playerRepository.save(receiver);

        updatePlayerRecoveryStatus(game, proposer);
        updatePlayerRecoveryStatus(game, receiver);

        // Mark trade as accepted
        trade.setStatus(TradeStatus.ACCEPTED);
        tradeOfferRepository.save(trade);

        // Auto-cancellation of conflicting pending trades
        cancelConflictingTrades(gameId, offeredProperties, requestedProperties, game.getVersion());

        // Broadcast events
        eventPublisher.publish(gameId, GameEvent.of(
                EventType.TRADE_ACCEPTED,
                gameId,
                Map.of(
                        "tradeId", trade.getId(),
                        "proposerId", proposer.getId(),
                        "receiverId", receiver.getId()
                ),
                game.getVersion()
        ));
        
        eventPublisher.publish(gameId, GameEvent.of(
                EventType.MONEY_UPDATED,
                gameId,
                Map.of("message", "Balances updated after trade execution"),
                game.getVersion()
        ));

        eventPublisher.publish(gameId, GameEvent.of(
                EventType.PROPERTY_UPDATED,
                gameId,
                Map.of("message", "Property ownership updated after trade execution"),
                game.getVersion()
        ));
    }

    @Transactional
    public void rejectTrade(UUID gameId, UUID receiverPlayerId, UUID tradeId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        TradeOffer trade = tradeOfferRepository.findById(tradeId)
                .orElseThrow(() -> new VyaparException("Trade offer not found", "TRADE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (trade.getStatus() != TradeStatus.PENDING) {
            throw new VyaparException("Trade is no longer pending", "TRADE_NOT_PENDING", HttpStatus.BAD_REQUEST);
        }

        if (!trade.getReceiverId().equals(receiverPlayerId)) {
            throw new VyaparException("You cannot reject this trade offer", "UNAUTHORIZED_TRADE_REJECTION", HttpStatus.FORBIDDEN);
        }

        trade.setStatus(TradeStatus.REJECTED);
        tradeOfferRepository.save(trade);

        eventPublisher.publish(gameId, GameEvent.of(
                EventType.TRADE_REJECTED,
                gameId,
                Map.of("tradeId", tradeId),
                game.getVersion()
        ));
    }

    @Transactional
    public void cancelTrade(UUID gameId, UUID proposerPlayerId, UUID tradeId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        TradeOffer trade = tradeOfferRepository.findById(tradeId)
                .orElseThrow(() -> new VyaparException("Trade offer not found", "TRADE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (trade.getStatus() != TradeStatus.PENDING) {
            throw new VyaparException("Trade is no longer pending", "TRADE_NOT_PENDING", HttpStatus.BAD_REQUEST);
        }

        if (!trade.getProposerId().equals(proposerPlayerId)) {
            throw new VyaparException("You cannot cancel this trade offer", "UNAUTHORIZED_TRADE_CANCELLATION", HttpStatus.FORBIDDEN);
        }

        trade.setStatus(TradeStatus.CANCELLED);
        tradeOfferRepository.save(trade);

        eventPublisher.publish(gameId, GameEvent.of(
                EventType.TRADE_CANCELLED,
                gameId,
                Map.of("tradeId", tradeId),
                game.getVersion()
        ));
    }

    private void validatePropertyOwnership(UUID gameId, UUID playerId, List<Integer> propertyIds) {
        if (propertyIds == null) return;
        for (Integer propId : propertyIds) {
            OwnedProperty op = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, propId)
                    .orElseThrow(() -> new PropertyNotFoundException("Property not owned by anyone: " + propId));
            if (!op.getOwnerId().equals(playerId)) {
                throw new VyaparException("Property ID " + propId + " is not owned by the expected player.", "PROPERTY_NOT_OWNED_BY_PLAYER", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private void validateNoDevelopmentsInColorGroup(UUID gameId, List<Integer> propertyIds) {
        if (propertyIds == null) return;
        for (Integer propId : propertyIds) {
            PropertyConfig config = jsonLoaderService.getPropertyConfig(propId);
            if (config == null) continue;

            if ("PROPERTY".equals(config.getType().name())) {
                String group = config.getGroup();
                if (group != null) {
                    Optional<OwnedProperty> originalOwnership = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, propId);
                    if (originalOwnership.isEmpty()) continue;
                    UUID ownerId = originalOwnership.get().getOwnerId();

                    List<PropertyConfig> groupConfigs = jsonLoaderService.getAllPropertyConfigs().stream()
                            .filter(p -> group.equals(p.getGroup()))
                            .collect(Collectors.toList());

                    for (PropertyConfig gp : groupConfigs) {
                        Optional<OwnedProperty> gpOwnership = ownedPropertyRepository.findByGameIdAndPropertyId(gameId, gp.getId());
                        if (gpOwnership.isPresent() 
                                && gpOwnership.get().getOwnerId().equals(ownerId) 
                                && gpOwnership.get().getDevelopmentLevel() > 0) {
                            throw new VyaparException("Cannot trade property in color group " + group + " because you have improvements (houses/hotels) built on " + gp.getName() + ".", "COLOR_GROUP_HAS_DEVELOPMENTS", HttpStatus.BAD_REQUEST);
                        }
                    }
                }
            }
        }
    }

    private void cancelConflictingTrades(UUID gameId, List<Integer> offeredProperties, List<Integer> requestedProperties, Long gameVersion) {
        Set<Integer> transferredProperties = new HashSet<>();
        if (offeredProperties != null) transferredProperties.addAll(offeredProperties);
        if (requestedProperties != null) transferredProperties.addAll(requestedProperties);

        if (transferredProperties.isEmpty()) return;

        List<TradeOffer> pendingTrades = tradeOfferRepository.findByGameIdAndStatus(gameId, TradeStatus.PENDING);
        for (TradeOffer pendingTrade : pendingTrades) {
            List<TradeOfferProperty> tradeProps = tradeOfferPropertyRepository.findByTradeOfferId(pendingTrade.getId());
            boolean conflicts = tradeProps.stream()
                    .anyMatch(tp -> transferredProperties.contains(tp.getPropertyId()));

            if (conflicts) {
                pendingTrade.setStatus(TradeStatus.CANCELLED);
                tradeOfferRepository.save(pendingTrade);

                eventPublisher.publish(gameId, GameEvent.of(
                        EventType.TRADE_CANCELLED,
                        gameId,
                        Map.of(
                                "tradeId", pendingTrade.getId(),
                                "reason", "One or more properties in this trade were transferred in another accepted trade."
                        ),
                        gameVersion
                ));
            }
        }
    }

    private TradeOfferResponse mapToResponse(TradeOffer trade) {
        Player proposer = playerRepository.findById(trade.getProposerId()).orElse(null);
        Player receiver = playerRepository.findById(trade.getReceiverId()).orElse(null);

        List<Integer> offered = tradeOfferPropertyRepository.findByTradeOfferId(trade.getId()).stream()
                .filter(TradeOfferProperty::getIsOffered)
                .map(TradeOfferProperty::getPropertyId)
                .collect(Collectors.toList());

        List<Integer> requested = tradeOfferPropertyRepository.findByTradeOfferId(trade.getId()).stream()
                .filter(p -> !p.getIsOffered())
                .map(TradeOfferProperty::getPropertyId)
                .collect(Collectors.toList());

        return new TradeOfferResponse(
                trade.getId(),
                trade.getGameId(),
                trade.getProposerId(),
                proposer != null ? proposer.getUsername() : "Unknown",
                trade.getReceiverId(),
                receiver != null ? receiver.getUsername() : "Unknown",
                offered,
                requested,
                trade.getOfferedCash(),
                trade.getRequestedCash(),
                trade.getStatus(),
                trade.getCreatedAt()
        );
    }

    private void updatePlayerRecoveryStatus(Game game, Player player) {
        if (player.getBalance() < 0) {
            if (player.getStatus() == PlayerStatus.ACTIVE) {
                player.setStatus(PlayerStatus.RECOVERY);
                playerRepository.save(player);
                game.setPendingAction(PendingAction.RECOVERY);
                gameRepository.save(game);

                transactionService.recordTransaction(
                        game.getId(),
                        player.getId(),
                        TransactionType.BANKRUPTCY,
                        0,
                        player.getUsername() + " entered Recovery Mode due to negative balance (₹" + player.getBalance() + ")"
                );

                eventPublisher.publish(game.getId(), GameEvent.of(
                        EventType.RECOVERY_STARTED, 
                        game.getId(), 
                        Map.of("playerId", player.getId()), 
                        game.getVersion()
                ));
            }
        } else {
            if (player.getStatus() == PlayerStatus.RECOVERY) {
                player.setStatus(PlayerStatus.ACTIVE);
                playerRepository.save(player);
                game.setPendingAction(PendingAction.NONE);
                gameRepository.save(game);

                transactionService.recordTransaction(
                        game.getId(),
                        player.getId(),
                        TransactionType.START_REWARD,
                        0,
                        player.getUsername() + " recovered and exited Recovery Mode (₹" + player.getBalance() + ")"
                );

                eventPublisher.publish(game.getId(), GameEvent.of(
                        EventType.RECOVERY_COMPLETED, 
                        game.getId(), 
                        Map.of("playerId", player.getId()), 
                        game.getVersion()
                ));
            }
        }
    }
}
