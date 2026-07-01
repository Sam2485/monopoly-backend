package com.BusinessGame.Vyapar.websocket;

import com.BusinessGame.Vyapar.common.enums.EventType;
import com.BusinessGame.Vyapar.common.exception.VyaparException;
import com.BusinessGame.Vyapar.dto.ActionRequest;
import com.BusinessGame.Vyapar.dto.SocketActionMessage;
import com.BusinessGame.Vyapar.service.GameEngineFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
public class GameWebSocketController {

    private final GameEngineFacade gameEngineFacade;
    private final GameEventPublisher eventPublisher;

    public GameWebSocketController(GameEngineFacade gameEngineFacade, GameEventPublisher eventPublisher) {
        this.gameEngineFacade = gameEngineFacade;
        this.eventPublisher = eventPublisher;
    }

    @MessageMapping("/game/action")
    public void processAction(@Payload SocketActionMessage message) {
        log.info("Received WebSocket command: {}", message);
        try {
            ActionRequest request = new ActionRequest(message.getAction(), message.getPropertyId());
            gameEngineFacade.performAction(message.getGameId(), message.getPlayerId(), request);
        } catch (VyaparException ex) {
            log.warn("WebSocket action failed: {}", ex.getMessage());
            // Publish error event back to the game topic
            GameEvent errorEvent = GameEvent.of(
                    EventType.ERROR,
                    message.getGameId(),
                    Map.of(
                            "code", ex.getErrorCode(),
                            "message", ex.getMessage()
                    ),
                    0L
            );
            eventPublisher.publish(message.getGameId(), errorEvent);
        } catch (Exception ex) {
            log.error("WebSocket action error", ex);
            GameEvent errorEvent = GameEvent.of(
                    EventType.ERROR,
                    message.getGameId(),
                    Map.of(
                            "code", "INTERNAL_SERVER_ERROR",
                            "message", ex.getMessage()
                    ),
                    0L
            );
            eventPublisher.publish(message.getGameId(), errorEvent);
        }
    }
}
