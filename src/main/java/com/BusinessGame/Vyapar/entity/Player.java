package com.BusinessGame.Vyapar.entity;

import com.BusinessGame.Vyapar.common.enums.PlayerStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "player")
public class Player {
    @Id
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String username;

    @Column(nullable = false)
    private Integer balance;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "number_of_properties", nullable = false)
    private Integer numberOfProperties;

    @Column(name = "consecutive_doubles", nullable = false)
    private Integer consecutiveDoubles;

    @Column(name = "jail_turns", nullable = false)
    private Integer jailTurns;

    @Column(name = "skipped_turns", nullable = false)
    private Integer skippedTurns;

    @Column(nullable = false)
    private Boolean connected;

    @Column(nullable = false)
    private Boolean ready;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PlayerStatus status;

    @Column(name = "has_built_house_this_turn", nullable = false)
    private Boolean hasBuiltHouseThisTurn;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (balance == null) balance = 25000;
        if (position == null) position = 0;
        if (numberOfProperties == null) numberOfProperties = 0;
        if (consecutiveDoubles == null) consecutiveDoubles = 0;
        if (jailTurns == null) jailTurns = 0;
        if (skippedTurns == null) skippedTurns = 0;
        if (connected == null) connected = true;
        if (ready == null) ready = false;
        if (status == null) status = PlayerStatus.ACTIVE;
        if (hasBuiltHouseThisTurn == null) hasBuiltHouseThisTurn = false;
    }
}
