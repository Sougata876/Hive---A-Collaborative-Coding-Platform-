package com.hive.user.dto;

import com.hive.user.User;
import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String avatarUrl,
        Instant createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getCreatedAt());
    }
}
