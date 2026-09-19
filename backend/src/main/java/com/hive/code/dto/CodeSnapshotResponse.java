package com.hive.code.dto;

import com.hive.code.CodeSnapshot;
import java.time.Instant;

public record CodeSnapshotResponse(
        Long id,
        Long roomId,
        String content,
        int version,
        Long savedByUserId,
        String savedByUsername,
        Instant savedAt
) {
    public static CodeSnapshotResponse from(CodeSnapshot snapshot) {
        return new CodeSnapshotResponse(
                snapshot.getId(),
                snapshot.getRoom().getId(),
                snapshot.getContent(),
                snapshot.getVersion(),
                snapshot.getSavedBy().getId(),
                snapshot.getSavedBy().getUsername(),
                snapshot.getSavedAt());
    }

    public static CodeSnapshotResponse empty(Long roomId) {
        return new CodeSnapshotResponse(null, roomId, "", 0, null, null, null);
    }
}
