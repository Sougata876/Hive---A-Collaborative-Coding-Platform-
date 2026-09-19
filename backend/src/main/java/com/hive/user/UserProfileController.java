package com.hive.user;

import com.hive.security.AuthenticatedUser;
import com.hive.user.dto.UpdateProfileRequest;
import com.hive.user.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public UserProfileResponse getProfile(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userProfileService.getProfile(principal.id());
    }

    @PutMapping
    public UserProfileResponse updateProfile(@AuthenticationPrincipal AuthenticatedUser principal,
                                             @Valid @RequestBody UpdateProfileRequest request) {
        return userProfileService.updateProfile(principal.id(), request);
    }
}
