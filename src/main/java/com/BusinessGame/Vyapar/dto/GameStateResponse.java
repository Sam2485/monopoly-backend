package com.BusinessGame.Vyapar.dto;

import com.BusinessGame.Vyapar.common.enums.GameStatus;
import com.BusinessGame.Vyapar.common.enums.PendingAction;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStateResponse {
    private UUID gameId;
    private GameStatus status;
    private UUID currentTurnPlayerId;
    private List<PlayerResponse> players;
    private List<PropertyResponse> properties;
    private DiceResponse dice;
    private PendingAction pendingAction;
}
