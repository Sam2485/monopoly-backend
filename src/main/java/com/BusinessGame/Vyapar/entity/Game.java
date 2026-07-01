package com.BusinessGame.Vyapar.entity;

import com.BusinessGame.Vyapar.common.enums.GameStatus;
import com.BusinessGame.Vyapar.common.enums.PendingAction;
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
@Table(name = "game")
public class Game {
    @Id
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "current_turn_player_id")
    private UUID currentTurnPlayerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pending_action", length = 50)
    private PendingAction pendingAction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStatus status;

    @Column(name = "winner_id")
    private UUID winnerId;

    @Column(name = "has_rolled")
    private Boolean hasRolled;

    @Version
    private Long version;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (hasRolled == null) {
            hasRolled = false;
        }
        startedAt = LocalDateTime.now();
    }
}
