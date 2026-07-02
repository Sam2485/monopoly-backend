package com.BusinessGame.Vyapar.repository;

import com.BusinessGame.Vyapar.entity.TradeOfferProperty;
import com.BusinessGame.Vyapar.entity.TradeOfferPropertyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeOfferPropertyRepository extends JpaRepository<TradeOfferProperty, TradeOfferPropertyId> {
    List<TradeOfferProperty> findByTradeOfferId(UUID tradeOfferId);
    List<TradeOfferProperty> findByPropertyId(Integer propertyId);
    void deleteByTradeOfferId(UUID tradeOfferId);
}
