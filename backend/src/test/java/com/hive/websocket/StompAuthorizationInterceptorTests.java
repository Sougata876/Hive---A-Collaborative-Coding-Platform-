package com.hive.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.hive.room.RoomPermissionService;
import com.hive.security.AuthenticatedUser;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class StompAuthorizationInterceptorTests {

    RoomPermissionService permissionService;
    StompJwtAuthenticationInterceptor authenticationInterceptor;
    StompAuthorizationInterceptor interceptor;
    MessageChannel channel;
    AuthenticatedUser user;

    @BeforeEach
    void setUp() {
        permissionService = mock(RoomPermissionService.class);
        authenticationInterceptor = mock(StompJwtAuthenticationInterceptor.class);
        interceptor = new StompAuthorizationInterceptor(authenticationInterceptor, permissionService);
        channel = mock(MessageChannel.class);
        user = new AuthenticatedUser(7L, "alice", "hash");
        org.mockito.Mockito.doAnswer(invocation -> invocation.getArgument(0))
                .when(authenticationInterceptor).preSend(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void editMessagesRequireEditorPermission() {
        Message<?> message = message(StompCommand.SEND, "/app/room/42/edit", user);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
        verify(permissionService).requireEditor(42L, 7L);
    }

    @Test
    void syncMessagesRequireMembershipOnly() {
        Message<?> message = message(StompCommand.SEND, "/app/room/42/sync", user);

        interceptor.preSend(message, channel);

        verify(permissionService).requireMember(42L, 7L);
    }

    @Test
    void chatAndSubscriptionsRequireMembership() {
        Message<?> chat = message(StompCommand.SEND, "/app/room/42/chat", user);
        interceptor.preSend(chat, channel);
        verify(permissionService).requireMember(42L, 7L);

        Message<?> subscription = message(StompCommand.SUBSCRIBE, "/topic/room/42/code", user);
        interceptor.preSend(subscription, channel);
        verify(permissionService, org.mockito.Mockito.times(2)).requireMember(42L, 7L);
    }

    @Test
    void directTopicPublishingAndUnknownDestinationsAreRejected() {
        Message<?> topicSend = message(StompCommand.SEND, "/topic/room/42/code", user);
        assertThatThrownBy(() -> interceptor.preSend(topicSend, channel))
                .isInstanceOf(WebSocketAccessDeniedException.class);

        Message<?> unknown = message(StompCommand.SEND, "/app/admin/delete", user);
        assertThatThrownBy(() -> interceptor.preSend(unknown, channel))
                .isInstanceOf(WebSocketAccessDeniedException.class);
    }

    @Test
    void unauthenticatedMessagesAreRejected() {
        Message<?> message = message(StompCommand.SUBSCRIBE, "/topic/room/42/chat", null);
        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(WebSocketAccessDeniedException.class);
    }

    private Message<?> message(StompCommand command, String destination, AuthenticatedUser principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        if (principal != null) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
