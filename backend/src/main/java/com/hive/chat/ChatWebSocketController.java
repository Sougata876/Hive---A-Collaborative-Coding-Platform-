package com.hive.chat;

import com.hive.chat.dto.ChatMessageResponse;
import com.hive.chat.dto.SendChatMessageRequest;
import com.hive.security.AuthenticatedUser;
import com.hive.websocket.WebSocketAccessDeniedException;
import jakarta.validation.Valid;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/room/{roomId}/chat")
    public void send(@DestinationVariable Long roomId,
                     @Valid @Payload SendChatMessageRequest request,
                     Principal principal) {
        AuthenticatedUser user = authenticatedUser(principal);
        // The service transaction commits before this returns, so a rolled-back message is never broadcast.
        ChatMessageResponse response = chatService.send(roomId, user.id(), request);
        try {
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/chat", response);
        } catch (RuntimeException exception) {
            // The message is already durable; clients recover it through the REST history endpoint.
            log.error("Chat message {} in room {} was saved but could not be broadcast",
                    response.id(), roomId, exception);
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
