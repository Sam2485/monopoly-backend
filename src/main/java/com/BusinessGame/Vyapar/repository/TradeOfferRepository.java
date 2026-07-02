package com.BusinessGame.Vyapar.repository;

import com.BusinessGame.Vyapar.common.enums.TradeStatus;
import com.BusinessGame.Vyapar.entity.TradeOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeOfferRepository extends JpaRepository<TradeOffer, UUID> {
    List<TradeOffer> findByGameId(UUID gameId);
    List<TradeOffer> findByGameIdAndStatus(UUID gameId, TradeStatus status);
    List<TradeOffer> findByGameIdAndReceiverIdAndStatus(UUID gameId, UUID receiverId, TradeStatus status);
    List<TradeOffer> findByGameIdAndProposerIdAndStatus(UUID gameId, UUID proposerId, TradeStatus status);
}
