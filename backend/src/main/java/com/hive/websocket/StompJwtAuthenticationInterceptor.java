package com.hive.websocket;

import com.hive.security.AuthenticatedUser;
import com.hive.security.JwtService;
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
public class StompJwtAuthenticationInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public StompJwtAuthenticationInterceptor(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(message);
        }
        if (StompCommand.CONNECT != accessor.getCommand()) {
            return message;
        }

        String authorization = firstHeader(accessor, "Authorization", "authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new WebSocketAccessDeniedException("A Bearer access token is required in the STOMP CONNECT frame");
        }

        try {
            String subject = jwtService.extractUsername(authorization.substring(7).trim());
            UserDetails user = userDetailsService.loadUserByUsername(subject);
            var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            accessor.setUser(authentication);
            accessor.setLeaveMutable(true);
            return message;
        } catch (Exception exception) {
            throw new WebSocketAccessDeniedException("Invalid or expired access token");
        }
    }

    private String firstHeader(StompHeaderAccessor accessor, String... names) {
        for (String name : names) {
            List<String> values = accessor.getNativeHeader(name);
            if (values != null && !values.isEmpty()) {
                return values.getFirst();
            }
        }
        return null;
    }
}
