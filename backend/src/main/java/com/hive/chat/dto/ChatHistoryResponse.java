package com.hive.chat.dto;

import java.util.List;

public record ChatHistoryResponse(
        List<ChatMessageResponse> messages,
        boolean hasMore,
        Long nextBeforeId
) {
    public ChatHistoryResponse {
        messages = List.copyOf(messages);
    }
}
