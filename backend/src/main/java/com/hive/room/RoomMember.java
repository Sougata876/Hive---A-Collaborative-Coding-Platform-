package com.hive.room;

import com.hive.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "room_members",
        indexes = @Index(name = "idx_room_members_user_id", columnList = "user_id"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_members_room_user", columnNames = {"room_id", "user_id"}))
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected RoomMember() {
    }

    public RoomMember(Room room, User user, RoomRole role, Instant joinedAt) {
        this.room = Objects.requireNonNull(room);
        this.user = Objects.requireNonNull(user);
        this.role = Objects.requireNonNull(role);
        this.joinedAt = Objects.requireNonNull(joinedAt);
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public User getUser() {
        return user;
    }

    public RoomRole getRole() {
        return role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void changeRole(RoomRole role) {
        if (this.role == RoomRole.OWNER || role == RoomRole.OWNER) {
            throw new IllegalArgumentException("Room ownership cannot be changed through member role updates");
        }
        this.role = Objects.requireNonNull(role);
    }
}
