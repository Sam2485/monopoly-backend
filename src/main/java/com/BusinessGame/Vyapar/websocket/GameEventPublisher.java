package com.BusinessGame.Vyapar.websocket;

import com.BusinessGame.Vyapar.common.enums.EventType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GameEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public GameEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(UUID gameId, GameEvent event) {
        // Send to general game topic
        String topic = "/topic/game/" + gameId;
        messagingTemplate.convertAndSend(topic, event);

        // Also duplicate to sub-topics as per specifications
        if (event.getEventType() != null) {
            switch (event.getEventType()) {
                case PLAYER_JOINED, PLAYER_LEFT, HOST_CHANGED, PLAYER_SENT_TO_JAIL, PLAYER_RELEASED, PLAYER_BANKRUPT ->
                        messagingTemplate.convertAndSend(topic + "/players", event);
                case PROPERTY_PURCHASED, PROPERTY_UPDATED, PROPERTY_MORTGAGED, PROPERTY_UNMORTGAGED, HOUSE_BUILT, HOTEL_BUILT ->
                        messagingTemplate.convertAndSend(topic + "/properties", event);
                case MONEY_UPDATED, RENT_PAID ->
                        messagingTemplate.convertAndSend(topic + "/money", event);
                default -> {}
            }
        }
    }

    public void publishRoomUpdate(UUID roomId, Object roomPayload) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId, roomPayload);
    }
}
