package com.hive.chat;

import com.hive.chat.dto.ChatHistoryResponse;
import com.hive.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
public class ChatRestController {

    private final ChatService chatService;

    public ChatRestController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public ChatHistoryResponse history(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestParam(required = false) Long beforeId) {
        return chatService.history(roomId, principal.id(), size, beforeId);
    }
}
