package com.hive.code.dto;

import java.time.Instant;

/** A Yjs payload as broadcast to room subscribers, annotated with the authenticated origin. */
public record YjsUpdateBroadcast(
        Long roomId,
        Long userId,
        String username,
        String update,
        Instant sentAt,
        String kind,
        Long target
) {
    /** The common case: an incremental document update from a peer, addressed to the whole room. */
    public YjsUpdateBroadcast(Long roomId, Long userId, String username, String update, Instant sentAt) {
        this(roomId, userId, username, update, sentAt, "update", null);
    }
}
