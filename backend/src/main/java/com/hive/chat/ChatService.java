package com.hive.chat;

import com.hive.chat.dto.ChatHistoryResponse;
import com.hive.chat.dto.ChatMessageResponse;
import com.hive.chat.dto.SendChatMessageRequest;
import com.hive.common.InvalidRequestException;
import com.hive.room.RoomMember;
import com.hive.room.RoomPermissionService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int DEFAULT_HISTORY_SIZE = 50;
    private static final int MAX_HISTORY_SIZE = 100;

    private final ChatMessageRepository messageRepository;
    private final RoomPermissionService permissionService;
    private final Clock clock;

    public ChatService(ChatMessageRepository messageRepository,
                       RoomPermissionService permissionService,
                       Clock clock) {
        this.messageRepository = messageRepository;
        this.permissionService = permissionService;
        this.clock = clock;
    }

    @Transactional
    public ChatMessageResponse send(Long roomId, Long userId, SendChatMessageRequest request) {
        RoomMember member = permissionService.requireMember(roomId, userId);
        String content = normalizeContent(request == null ? null : request.content());
        // Align with the TIMESTAMP(6) column so in-memory cursors match persisted values exactly.
        Instant sentAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
        ChatMessage message = messageRepository.save(new ChatMessage(
                member.getRoom(), member.getUser(), content, sentAt));
        return ChatMessageResponse.from(message);
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse history(Long roomId, Long userId, Integer requestedSize, Long beforeId) {
        permissionService.requireMember(roomId, userId);
        int size = validateSize(requestedSize);
        PageRequest page = PageRequest.of(0, size);

        Slice<ChatMessage> slice;
        if (beforeId == null) {
            slice = messageRepository.findLatest(roomId, page);
        } else {
            if (beforeId <= 0) {
                throw new InvalidRequestException("beforeId must be positive");
            }
            ChatMessage cursor = messageRepository.findByIdAndRoomId(beforeId, roomId)
                    .orElseThrow(() -> new InvalidRequestException("Invalid message cursor"));
            slice = messageRepository.findBefore(roomId, cursor.getSentAt(), cursor.getId(), page);
        }

        List<ChatMessage> chronological = new ArrayList<>(slice.getContent());
        Collections.reverse(chronological);
        List<ChatMessageResponse> messages = chronological.stream()
                .map(ChatMessageResponse::from)
                .toList();
        Long nextBeforeId = slice.hasNext() && !messages.isEmpty() ? messages.getFirst().id() : null;
        return new ChatHistoryResponse(messages, slice.hasNext(), nextBeforeId);
    }

    private int validateSize(Integer requestedSize) {
        int size = requestedSize == null ? DEFAULT_HISTORY_SIZE : requestedSize;
        if (size < 1 || size > MAX_HISTORY_SIZE) {
            throw new InvalidRequestException("size must be between 1 and 100");
        }
        return size;
    }

    private String normalizeContent(String rawContent) {
        if (rawContent == null) {
            throw new InvalidRequestException("Message content is required");
        }
        if (rawContent.length() > MAX_CONTENT_LENGTH) {
            throw new InvalidRequestException("Message content must not exceed 2000 characters");
        }
        String content = rawContent.strip();
        if (content.isEmpty() || content.codePoints().allMatch(Character::isWhitespace)) {
            throw new InvalidRequestException("Message content must not be blank");
        }
        boolean containsDisallowedControl = content.codePoints()
                .anyMatch(codePoint -> Character.isISOControl(codePoint)
                        && codePoint != '\t' && codePoint != '\n' && codePoint != '\r');
        if (containsDisallowedControl) {
            throw new InvalidRequestException("Message content contains unsupported control characters");
        }
        return content;
    }
}
