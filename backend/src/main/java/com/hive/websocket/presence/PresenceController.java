package com.hive.websocket.presence;

import com.hive.security.AuthenticatedUser;
import com.hive.websocket.WebSocketAccessDeniedException;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @MessageMapping("/room/{roomId}/presence")
    public void presence(@DestinationVariable Long roomId, @Payload PresenceCommand command,
                         Principal principal, StompHeaderAccessor accessor) {
        AuthenticatedUser user = authenticatedUser(principal);
        String sessionId = accessor.getSessionId();
        if (command == null || command.action() == null) {
            throw new WebSocketAccessDeniedException("Presence action must be join or leave");
        }
        if ("join".equalsIgnoreCase(command.action())) {
            presenceService.join(roomId, user, sessionId);
        } else if ("leave".equalsIgnoreCase(command.action())) {
            presenceService.leave(roomId, sessionId);
        } else {
            throw new WebSocketAccessDeniedException("Presence action must be join or leave");
        }
    }

    private AuthenticatedUser authenticatedUser(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new WebSocketAccessDeniedException("Authentication is required");
    }
}
