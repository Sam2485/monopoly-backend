package com.BusinessGame.Vyapar.dto;

import com.BusinessGame.Vyapar.common.enums.TransactionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private UUID transactionId;
    private TransactionType type;
    private Integer amount;
    private String description;
    private LocalDateTime createdAt;
}
