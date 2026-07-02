package com.BusinessGame.Vyapar.dto;

import com.BusinessGame.Vyapar.common.enums.TradeStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeOfferResponse {
    private UUID tradeId;
    private UUID gameId;
    private UUID proposerId;
    private String proposerName;
    private UUID receiverId;
    private String receiverName;
    private List<Integer> offeredProperties;
    private List<Integer> requestedProperties;
    private Integer offeredCash;
    private Integer requestedCash;
    private TradeStatus status;
    private LocalDateTime createdAt;
}
