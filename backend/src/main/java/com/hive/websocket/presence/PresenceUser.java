package com.hive.websocket.presence;

import java.time.Instant;

public record PresenceUser(
        Long userId,
        String username,
        String avatarUrl,
        Instant connectedAt
) {
}
