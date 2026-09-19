package com.hive.code;

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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "code_snapshots",
        indexes = @Index(name = "idx_code_snapshots_room_version", columnList = "room_id, version"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_code_snapshots_room_version", columnNames = {"room_id", "version"}))
public class CodeSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, updatable = false)
    private Room room;

    @Lob
    @Column(nullable = false, updatable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private int version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saved_by", nullable = false, updatable = false)
    private User savedBy;

    @Column(name = "saved_at", nullable = false, updatable = false)
    private Instant savedAt;

    protected CodeSnapshot() {
    }

    public CodeSnapshot(Room room, String content, int version, User savedBy, Instant savedAt) {
        this.room = Objects.requireNonNull(room);
        this.content = Objects.requireNonNull(content);
        this.version = version;
        this.savedBy = Objects.requireNonNull(savedBy);
        this.savedAt = Objects.requireNonNull(savedAt);
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public String getContent() {
        return content;
    }

    public int getVersion() {
        return version;
    }

    public User getSavedBy() {
        return savedBy;
    }

    public Instant getSavedAt() {
        return savedAt;
    }
}
