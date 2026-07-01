package com.BusinessGame.Vyapar.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerLobbyResponse {
    private UUID playerId;
    private String username;
    private String avatar;
    private boolean ready;
    private boolean host;
}
