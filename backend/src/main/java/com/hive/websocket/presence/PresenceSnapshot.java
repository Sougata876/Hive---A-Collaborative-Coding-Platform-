package com.hive.websocket.presence;

import java.util.List;

public record PresenceSnapshot(Long roomId, List<PresenceUser> users) {
}
