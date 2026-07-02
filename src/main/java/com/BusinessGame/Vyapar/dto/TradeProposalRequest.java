package com.BusinessGame.Vyapar.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeProposalRequest {
    private UUID receiverId;
    private List<Integer> offeredProperties;
    private List<Integer> requestedProperties;
    private Integer offeredCash;
    private Integer requestedCash;
}
