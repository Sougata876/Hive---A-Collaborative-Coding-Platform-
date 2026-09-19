package com.hive.security;

import com.hive.user.User;
import com.hive.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class HiveUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public HiveUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String subject) {
        User user;
        try {
            Long userId = Long.parseLong(subject);
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        } catch (NumberFormatException exception) {
            user = userRepository.findByUsernameIgnoreCase(subject)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        }

        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getPasswordHash());
    }
}
