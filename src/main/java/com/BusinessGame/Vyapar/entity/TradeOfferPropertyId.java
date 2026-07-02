package com.BusinessGame.Vyapar.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeOfferPropertyId implements Serializable {
    private UUID tradeOfferId;
    private Integer propertyId;
}
