package com.hive.room.dto;

import com.hive.room.RoomMember;
import com.hive.room.RoomRole;
import java.time.Instant;

public record RoomMemberResponse(
        Long userId,
        String username,
        String avatarUrl,
        RoomRole role,
        Instant joinedAt
) {
    public static RoomMemberResponse from(RoomMember member) {
        return new RoomMemberResponse(
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getUser().getAvatarUrl(),
                member.getRole(),
                member.getJoinedAt());
    }
}
