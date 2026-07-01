package com.BusinessGame.Vyapar.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomResponse {
    private UUID roomId;
    private String roomCode;
    private UUID hostId;
    private Integer maxPlayers;
    private Integer currentPlayers;
}
