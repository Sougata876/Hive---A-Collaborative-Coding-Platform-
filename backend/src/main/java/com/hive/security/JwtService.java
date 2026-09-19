package com.hive.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public AccessToken generateAccessToken(AuthenticatedUser user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        String token = Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.id().toString())
                .claim("username", user.username())
                .claim("type", "access")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .id(UUID.randomUUID().toString())
                .signWith(signingKey)
                .compact();
        return new AccessToken(token, expiresAt);
    }

    public String extractUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!"access".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Unexpected token type");
        }
        return claims.getSubject();
    }

    public record AccessToken(String value, Instant expiresAt) {
    }
}
