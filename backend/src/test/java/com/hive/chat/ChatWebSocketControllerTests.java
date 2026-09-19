package com.hive.chat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hive.chat.dto.ChatMessageResponse;
import com.hive.chat.dto.SendChatMessageRequest;
import com.hive.security.AuthenticatedUser;
import com.hive.websocket.WebSocketAccessDeniedException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class ChatWebSocketControllerTests {

    ChatService chatService;
    SimpMessagingTemplate messagingTemplate;
    ChatWebSocketController controller;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        controller = new ChatWebSocketController(chatService, messagingTemplate);
    }

    @Test
    void persistsUsingPrincipalBeforeBroadcastingCanonicalResponse() {
        AuthenticatedUser user = new AuthenticatedUser(7L, "alice", "hash");
        var principal = new UsernamePasswordAuthenticationToken(user, null, List.of());
        var request = new SendChatMessageRequest("hello");
        var response = new ChatMessageResponse(
                11L, 42L, 7L, "alice", null, "hello", Instant.parse("2026-09-12T12:00:00Z"));
        when(chatService.send(42L, 7L, request)).thenReturn(response);

        controller.send(42L, request, principal);

        InOrder order = inOrder(chatService, messagingTemplate);
        order.verify(chatService).send(42L, 7L, request);
        order.verify(messagingTemplate).convertAndSend("/topic/room/42/chat", response);
    }

    @Test
    void failedPersistenceDoesNotBroadcast() {
        AuthenticatedUser user = new AuthenticatedUser(7L, "alice", "hash");
        var principal = new UsernamePasswordAuthenticationToken(user, null, List.of());
        var request = new SendChatMessageRequest("hello");
        when(chatService.send(42L, 7L, request)).thenThrow(new IllegalStateException("commit failed"));

        assertThatThrownBy(() -> controller.send(42L, request, principal))
                .isInstanceOf(IllegalStateException.class);
        org.mockito.Mockito.verifyNoInteractions(messagingTemplate);
    }

    @Test
    void persistedMessageSurvivesBroadcastFailure() {
        AuthenticatedUser user = new AuthenticatedUser(7L, "alice", "hash");
        var principal = new UsernamePasswordAuthenticationToken(user, null, List.of());
        var request = new SendChatMessageRequest("hello");
        var response = new ChatMessageResponse(
                11L, 42L, 7L, "alice", null, "hello", Instant.parse("2026-09-12T12:00:00Z"));
        when(chatService.send(42L, 7L, request)).thenReturn(response);
        org.mockito.Mockito.doThrow(new IllegalStateException("broker down"))
                .when(messagingTemplate).convertAndSend(
                        org.mockito.ArgumentMatchers.eq("/topic/room/42/chat"),
                        org.mockito.ArgumentMatchers.<Object>any());

        controller.send(42L, request, principal);

        org.mockito.Mockito.verify(chatService).send(42L, 7L, request);
    }

    @Test
    void unauthenticatedPrincipalIsRejected() {
        assertThatThrownBy(() -> controller.send(
                42L, new SendChatMessageRequest("hello"), () -> "anonymous"))
                .isInstanceOf(WebSocketAccessDeniedException.class);
    }
}
