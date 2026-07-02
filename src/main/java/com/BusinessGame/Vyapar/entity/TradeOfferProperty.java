package com.BusinessGame.Vyapar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "trade_offer_property")
@IdClass(TradeOfferPropertyId.class)
public class TradeOfferProperty {
    @Id
    @Column(name = "trade_offer_id")
    private UUID tradeOfferId;

    @Id
    @Column(name = "property_id")
    private Integer propertyId;

    @Column(name = "is_offered", nullable = false)
    private Boolean isOffered;
}
