package com.hive.auth;

import com.hive.auth.dto.AuthResponse;
import com.hive.auth.dto.AuthUserResponse;
import com.hive.auth.dto.LoginRequest;
import com.hive.auth.dto.RegisterRequest;
import com.hive.common.ConflictException;
import com.hive.common.InvalidRefreshTokenException;
import com.hive.security.AuthenticatedUser;
import com.hive.security.JwtProperties;
import com.hive.security.JwtService;
import com.hive.user.User;
import com.hive.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final int REFRESH_TOKEN_BYTES = 48;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       JwtService jwtService, JwtProperties jwtProperties, Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username is already in use");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already in use");
        }

        User user;
        try {
            user = userRepository.saveAndFlush(new User(
                    username,
                    email,
                    passwordEncoder.encode(request.password()),
                    clock.instant()));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Username or email is already in use");
        }
        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        User user = identifier.contains("@")
                ? userRepository.findByEmailIgnoreCase(identifier).orElseThrow(this::badCredentials)
                : userRepository.findByUsernameIgnoreCase(identifier).orElseThrow(this::badCredentials);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.password()));
        } catch (AuthenticationException exception) {
            throw badCredentials();
        }
        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse refresh(String rawToken) {
        Instant now = clock.instant();
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!storedToken.isUsableAt(now)) {
            throw new InvalidRefreshTokenException();
        }

        storedToken.revoke(now);
        return issueTokenPair(storedToken.getUser());
    }

    private AuthResponse issueTokenPair(User user) {
        var principal = new AuthenticatedUser(user.getId(), user.getUsername(), user.getPasswordHash());
        JwtService.AccessToken accessToken = jwtService.generateAccessToken(principal);
        String rawRefreshToken = generateRefreshToken();
        Instant refreshExpiresAt = clock.instant().plus(jwtProperties.refreshTokenTtl());
        refreshTokenRepository.save(new RefreshToken(
                user, hash(rawRefreshToken), refreshExpiresAt, clock.instant()));

        return new AuthResponse(
                "Bearer",
                accessToken.value(),
                accessToken.expiresAt(),
                rawRefreshToken,
                refreshExpiresAt,
                AuthUserResponse.from(user));
    }

    private String generateRefreshToken() {
        byte[] token = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private org.springframework.security.authentication.BadCredentialsException badCredentials() {
        return new org.springframework.security.authentication.BadCredentialsException("Invalid username/email or password");
    }
}
