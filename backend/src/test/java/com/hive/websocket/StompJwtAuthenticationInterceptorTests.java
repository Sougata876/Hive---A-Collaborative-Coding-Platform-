package com.hive.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hive.security.AuthenticatedUser;
import com.hive.security.JwtService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;

class StompJwtAuthenticationInterceptorTests {

    JwtService jwtService;
    UserDetailsService userDetailsService;
    StompJwtAuthenticationInterceptor interceptor;
    MessageChannel channel;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userDetailsService = mock(UserDetailsService.class);
        interceptor = new StompJwtAuthenticationInterceptor(jwtService, userDetailsService);
        channel = mock(MessageChannel.class);
    }

    @Test
    void connectAuthenticatesBearerTokenFromStompHeaders() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "alice", "hash");
        when(jwtService.extractUsername("valid-token")).thenReturn("1");
        when(userDetailsService.loadUserByUsername("1")).thenReturn(user);
        Message<?> connect = connectMessage("Bearer valid-token");

        Message<?> result = interceptor.preSend(connect, channel);
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);

        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("alice");
    }

    @Test
    void connectWithoutTokenIsRejected() {
        Message<?> connect = connectMessage(null);
        assertThatThrownBy(() -> interceptor.preSend(connect, channel))
                .isInstanceOf(WebSocketAccessDeniedException.class)
                .hasMessageContaining("Bearer");
    }

    @Test
    void invalidTokenIsRejected() {
        when(jwtService.extractUsername("bad-token")).thenThrow(new IllegalArgumentException("bad"));
        Message<?> connect = connectMessage("Bearer bad-token");
        assertThatThrownBy(() -> interceptor.preSend(connect, channel))
                .isInstanceOf(WebSocketAccessDeniedException.class)
                .hasMessage("Invalid or expired access token");
    }

    private Message<?> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
