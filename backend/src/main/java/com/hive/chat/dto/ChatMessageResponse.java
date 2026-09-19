package com.hive.chat.dto;

import com.hive.chat.ChatMessage;
import java.time.Instant;

public record ChatMessageResponse(
        Long id,
        Long roomId,
        Long userId,
        String username,
        String avatarUrl,
        String content,
        Instant sentAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRoom().getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                message.getSender().getAvatarUrl(),
                message.getContent(),
                message.getSentAt());
    }
}
