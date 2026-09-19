package com.hive.room.dto;

import com.hive.room.ProgrammingLanguage;
import com.hive.room.Room;
import com.hive.room.RoomRole;
import java.time.Instant;

public record RoomResponse(
        Long id,
        String name,
        Long ownerId,
        ProgrammingLanguage language,
        RoomRole role,
        boolean active,
        Instant createdAt,
        String inviteCode
) {
    public static RoomResponse from(Room room, RoomRole role, boolean includeInviteCode) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getOwner().getId(),
                room.getLanguage(),
                role,
                room.isActive(),
                room.getCreatedAt(),
                includeInviteCode ? room.getInviteCode() : null);
    }
}
