package com.BusinessGame.Vyapar.dto;

import com.BusinessGame.Vyapar.common.enums.PlayerStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerResponse {
    private UUID playerId;
    private String username;
    private int balance;
    private int boardPosition;
    private int numberOfProperties;
    private PlayerStatus status;
    private boolean connected;
    private boolean hasBuiltHouseThisTurn;
}
