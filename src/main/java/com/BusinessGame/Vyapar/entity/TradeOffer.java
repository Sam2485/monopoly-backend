package com.BusinessGame.Vyapar.entity;

import com.BusinessGame.Vyapar.common.enums.TradeStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "trade_offer")
public class TradeOffer {
    @Id
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "proposer_id", nullable = false)
    private UUID proposerId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Column(name = "offered_cash", nullable = false)
    private Integer offeredCash;

    @Column(name = "requested_cash", nullable = false)
    private Integer requestedCash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TradeStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
