package com.hive.websocket.presence;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class PresenceDisconnectListener {

    private final PresenceService presenceService;

    public PresenceDisconnectListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        presenceService.disconnect(event.getSessionId());
    }
}
