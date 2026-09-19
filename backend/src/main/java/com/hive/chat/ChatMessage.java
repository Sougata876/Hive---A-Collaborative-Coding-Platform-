package com.hive.chat;

import com.hive.room.Room;
import com.hive.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_messages_room_sent_id", columnList = "room_id, sent_at, id"),
        @Index(name = "idx_chat_messages_sender_id", columnList = "sender_id")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, updatable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, updatable = false)
    private User sender;

    @Column(nullable = false, length = 2000, updatable = false)
    private String content;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    protected ChatMessage() {
    }

    public ChatMessage(Room room, User sender, String content, Instant sentAt) {
        this.room = Objects.requireNonNull(room);
        this.sender = Objects.requireNonNull(sender);
        this.content = Objects.requireNonNull(content);
        this.sentAt = Objects.requireNonNull(sentAt);
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public User getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
