package com.hive.chat;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByIdAndRoomId(Long id, Long roomId);

    @EntityGraph(attributePaths = "sender")
    @Query("""
            SELECT message
            FROM ChatMessage message
            WHERE message.room.id = :roomId
            ORDER BY message.sentAt DESC, message.id DESC
            """)
    Slice<ChatMessage> findLatest(@Param("roomId") Long roomId, Pageable pageable);

    @EntityGraph(attributePaths = "sender")
    @Query("""
            SELECT message
            FROM ChatMessage message
            WHERE message.room.id = :roomId
              AND (message.sentAt < :sentAt
                   OR (message.sentAt = :sentAt AND message.id < :messageId))
            ORDER BY message.sentAt DESC, message.id DESC
            """)
    Slice<ChatMessage> findBefore(
            @Param("roomId") Long roomId,
            @Param("sentAt") Instant sentAt,
            @Param("messageId") Long messageId,
            Pageable pageable);
}
