package com.hive.websocket;

import com.hive.room.RoomPermissionService;
import com.hive.security.AuthenticatedUser;
import java.security.Principal;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class StompAuthorizationInterceptor implements ChannelInterceptor {

    private final StompJwtAuthenticationInterceptor authenticationInterceptor;
    private final RoomPermissionService permissionService;

    public StompAuthorizationInterceptor(StompJwtAuthenticationInterceptor authenticationInterceptor,
                                         RoomPermissionService permissionService) {
        this.authenticationInterceptor = authenticationInterceptor;
        this.permissionService = permissionService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        Message<?> authenticatedMessage = authenticationInterceptor.preSend(message, channel);
        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(
                authenticatedMessage, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(authenticatedMessage);
        }
        StompCommand command = accessor.getCommand();

        if (command == null || command == StompCommand.CONNECT || command == StompCommand.DISCONNECT) {
            return authenticatedMessage;
        }
        if (command != StompCommand.SEND && command != StompCommand.SUBSCRIBE) {
            return authenticatedMessage;
        }

        AuthenticatedUser user = authenticatedUser(accessor.getUser());
        String destination = accessor.getDestination();
        RoomDestination.Match match = RoomDestination.parse(destination);
        if (match == null) {
            throw new WebSocketAccessDeniedException("Destination is not allowed");
        }

        if (command == StompCommand.SEND && !match.applicationDestination()) {
            throw new WebSocketAccessDeniedException("Clients cannot publish directly to broker topics");
        }
        if (command == StompCommand.SUBSCRIBE && match.applicationDestination()) {
            throw new WebSocketAccessDeniedException("Clients cannot subscribe to application destinations");
        }

        try {
            if (match.requiresEditPermission()) {
                permissionService.requireEditor(match.roomId(), user.id());
            } else {
                permissionService.requireMember(match.roomId(), user.id());
            }
        } catch (RuntimeException exception) {
            throw new WebSocketAccessDeniedException(exception.getMessage());
        }
        return authenticatedMessage;
    }

    private AuthenticatedUser authenticatedUser(Principal principal) {
        if (!(principal instanceof Authentication authentication)
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new WebSocketAccessDeniedException("Authentication is required");
        }
        return user;
    }
}
