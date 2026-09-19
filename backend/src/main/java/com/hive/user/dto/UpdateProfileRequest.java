package com.hive.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "must contain only letters, numbers, and underscores")
        String username,

        @Email
        @Size(max = 254)
        String email,

        @Size(max = 2048)
        @Pattern(regexp = "^https?://\\S+$", message = "must be a valid HTTP or HTTPS URL")
        String avatarUrl
) {
}
