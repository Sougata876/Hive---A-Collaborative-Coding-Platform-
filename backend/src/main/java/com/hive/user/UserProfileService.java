package com.hive.user;

import com.hive.common.ConflictException;
import com.hive.common.InvalidRequestException;
import com.hive.common.ResourceNotFoundException;
import com.hive.user.dto.UpdateProfileRequest;
import com.hive.user.dto.UserProfileResponse;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        return UserProfileResponse.from(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        String username = request.username() == null ? user.getUsername() : request.username().trim();
        String email = request.email() == null
                ? user.getEmail()
                : request.email().trim().toLowerCase(Locale.ROOT);
        String avatarUrl = request.avatarUrl() == null ? user.getAvatarUrl() : normalizeAvatarUrl(request.avatarUrl());

        if (username.isBlank()) {
            throw new InvalidRequestException("Username must not be blank");
        }
        if (email.isBlank()) {
            throw new InvalidRequestException("Email must not be blank");
        }
        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(username, userId)) {
            throw new ConflictException("Username is already in use");
        }
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new ConflictException("Email is already in use");
        }

        user.updateProfile(username, email, avatarUrl);
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Username or email is already in use");
        }
        return UserProfileResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String normalizeAvatarUrl(String avatarUrl) {
        String normalized = avatarUrl.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
