package com.BusinessGame.Vyapar.dto;

import com.BusinessGame.Vyapar.common.enums.ActionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocketActionMessage {
    private UUID gameId;
    private UUID playerId;
    private ActionType action;
    private Integer propertyId;
}
