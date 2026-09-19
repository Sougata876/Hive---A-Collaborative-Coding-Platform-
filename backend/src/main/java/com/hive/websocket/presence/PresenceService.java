package com.hive.websocket.presence;

import com.hive.common.ResourceNotFoundException;
import com.hive.room.RoomPermissionService;
import com.hive.security.AuthenticatedUser;
import com.hive.user.User;
import com.hive.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Tracks who is currently present in each room. Presence is keyed by
 * ({@code sessionId}, {@code roomId}) so a single STOMP session can be present in several rooms at
 * once without one room's join or leave corrupting another's state.
 */
@Service
public class PresenceService {

    private final Map<Long, Map<Long, UserPresence>> roomUsers = new HashMap<>();
    private final Map<String, Map<Long, Long>> sessionRooms = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final RoomPermissionService permissionService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    public PresenceService(RoomPermissionService permissionService, UserRepository userRepository,
                           SimpMessagingTemplate messagingTemplate, Clock clock) {
        this.permissionService = permissionService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.clock = clock;
    }

    public void join(Long roomId, AuthenticatedUser principal, String sessionId) {
        permissionService.requireMember(roomId, principal.id());
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // The authenticated principal is the authoritative identity; the entity only supplies display data.
        Long userId = principal.id();
        Instant now = clock.instant();

        lock.lock();
        try {
            Map<Long, Long> rooms = sessionRooms.computeIfAbsent(sessionId, ignored -> new LinkedHashMap<>());
            Long previousUserId = rooms.put(roomId, userId);
            if (previousUserId != null && !previousUserId.equals(userId)) {
                removeSessionFromRoom(roomId, previousUserId, sessionId);
            }
            UserPresence presence = roomUsers
                    .computeIfAbsent(roomId, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(userId, ignored -> new UserPresence(
                            userId, user.getUsername(), user.getAvatarUrl(), now));
            presence.sessionIds().add(sessionId);
        } finally {
            lock.unlock();
        }
        broadcast(roomId);
    }

    /** Removes this session from a single room, leaving its other rooms untouched. */
    public void leave(Long roomId, String sessionId) {
        boolean changed = false;
        lock.lock();
        try {
            Map<Long, Long> rooms = sessionRooms.get(sessionId);
            if (rooms != null) {
                Long userId = rooms.remove(roomId);
                if (userId != null) {
                    removeSessionFromRoom(roomId, userId, sessionId);
                    changed = true;
                }
                if (rooms.isEmpty()) {
                    sessionRooms.remove(sessionId);
                }
            }
        } finally {
            lock.unlock();
        }
        if (changed) {
            broadcast(roomId);
        }
    }

    /** Removes a disconnected session from every room it was present in. */
    public void disconnect(String sessionId) {
        List<Long> changedRooms = new ArrayList<>();
        lock.lock();
        try {
            Map<Long, Long> rooms = sessionRooms.remove(sessionId);
            if (rooms != null) {
                rooms.forEach((roomId, userId) -> {
                    removeSessionFromRoom(roomId, userId, sessionId);
                    changedRooms.add(roomId);
                });
            }
        } finally {
            lock.unlock();
        }
        changedRooms.forEach(this::broadcast);
    }

    public PresenceSnapshot snapshot(Long roomId) {
        lock.lock();
        try {
            Map<Long, UserPresence> users = roomUsers.getOrDefault(roomId, Map.of());
            List<PresenceUser> result = users.values().stream()
                    .map(user -> new PresenceUser(
                            user.userId(), user.username(), user.avatarUrl(), user.connectedAt()))
                    .sorted(Comparator.comparing(PresenceUser::username, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            return new PresenceSnapshot(roomId, result);
        } finally {
            lock.unlock();
        }
    }

    private void removeSessionFromRoom(Long roomId, Long userId, String sessionId) {
        Map<Long, UserPresence> users = roomUsers.get(roomId);
        if (users == null) {
            return;
        }
        UserPresence user = users.get(userId);
        if (user != null) {
            user.sessionIds().remove(sessionId);
            if (user.sessionIds().isEmpty()) {
                users.remove(userId);
            }
        }
        if (users.isEmpty()) {
            roomUsers.remove(roomId);
        }
    }

    private void broadcast(Long roomId) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/presence", snapshot(roomId));
    }

    private record UserPresence(Long userId, String username, String avatarUrl,
                                Instant connectedAt, Set<String> sessionIds) {
        UserPresence(Long userId, String username, String avatarUrl, Instant connectedAt) {
            this(userId, username, avatarUrl, connectedAt, new LinkedHashSet<>());
        }
    }
}
