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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "rooms", uniqueConstraints =
        @UniqueConstraint(name = "uk_rooms_invite_code", columnNames = "invite_code"))
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgrammingLanguage language;

    @Column(name = "invite_code", nullable = false, length = 32)
    private String inviteCode;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Room() {
    }

    public Room(String name, User owner, ProgrammingLanguage language, String inviteCode, Instant createdAt) {
        this.name = Objects.requireNonNull(name);
        this.owner = Objects.requireNonNull(owner);
        this.language = Objects.requireNonNull(language);
        this.inviteCode = Objects.requireNonNull(inviteCode);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User getOwner() {
        return owner;
    }

    public ProgrammingLanguage getLanguage() {
        return language;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void deactivate() {
        active = false;
    }
}
