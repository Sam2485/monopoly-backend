package com.BusinessGame.Vyapar.websocket;

import com.BusinessGame.Vyapar.common.enums.EventType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameEvent {
    private EventType eventType;
    private UUID gameId;
    private Object payload;
    private Long version;
    private LocalDateTime timestamp = LocalDateTime.now();

    public static GameEvent of(EventType eventType, UUID gameId, Object payload, Long version) {
        return new GameEvent(eventType, gameId, payload, version, LocalDateTime.now());
    }
}
