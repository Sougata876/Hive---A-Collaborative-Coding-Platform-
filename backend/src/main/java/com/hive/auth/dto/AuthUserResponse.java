package com.hive.auth.dto;

import com.hive.user.User;

public record AuthUserResponse(Long id, String username, String email, String avatarUrl) {

    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getAvatarUrl());
    }
}
