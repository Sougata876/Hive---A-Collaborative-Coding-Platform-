package com.hive.websocket.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hive.room.RoomPermissionService;
import com.hive.security.AuthenticatedUser;
import com.hive.user.User;
import com.hive.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class PresenceServiceTests {

    RoomPermissionService permissionService;
    UserRepository userRepository;
    SimpMessagingTemplate messagingTemplate;
    PresenceService service;
    AuthenticatedUser principal;

    @BeforeEach
    void setUp() {
        permissionService = mock(RoomPermissionService.class);
        userRepository = mock(UserRepository.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-11T12:00:00Z"), ZoneOffset.UTC);
        service = new PresenceService(permissionService, userRepository, messagingTemplate, clock);
        principal = new AuthenticatedUser(5L, "alice", "hash");
        when(userRepository.findById(5L)).thenReturn(Optional.of(
                new User("alice", "alice@example.com", "hash", clock.instant())));
    }

    @Test
    void tracksMultipleSessionsAndRemovesUserAfterLastDisconnect() {
        service.join(9L, principal, "session-1");
        service.join(9L, principal, "session-2");

        verify(permissionService, org.mockito.Mockito.times(2)).requireMember(9L, 5L);
        assertThat(service.snapshot(9L).users()).hasSize(1);

        service.disconnect("session-1");
        assertThat(service.snapshot(9L).users()).hasSize(1);

        service.disconnect("session-2");
        assertThat(service.snapshot(9L).users()).isEmpty();
    }

    @Test
    void oneSessionCanBePresentInSeveralRoomsIndependently() {
        service.join(41L, principal, "session-1");
        service.join(42L, principal, "session-1");

        assertThat(service.snapshot(41L).users()).hasSize(1);
        assertThat(service.snapshot(42L).users()).hasSize(1);

        service.leave(42L, "session-1");

        assertThat(service.snapshot(41L).users()).hasSize(1);
        assertThat(service.snapshot(42L).users()).isEmpty();
    }

    @Test
    void disconnectClearsEveryRoomTheSessionJoined() {
        service.join(41L, principal, "session-1");
        service.join(42L, principal, "session-1");

        service.disconnect("session-1");

        assertThat(service.snapshot(41L).users()).isEmpty();
        assertThat(service.snapshot(42L).users()).isEmpty();
        verify(messagingTemplate, org.mockito.Mockito.times(2)).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/room/41/presence"),
                org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    void leavingARoomTheSessionNeverJoinedChangesNothing() {
        service.join(41L, principal, "session-1");

        service.leave(99L, "session-1");

        assertThat(service.snapshot(41L).users()).hasSize(1);
        assertThat(service.snapshot(99L).users()).isEmpty();
    }
}
