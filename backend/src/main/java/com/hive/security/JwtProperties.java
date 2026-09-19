package com.hive.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hive.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {
    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("hive.jwt.issuer must be configured");
        }
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32 ) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        }
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalArgumentException("hive.jwt.access-token-ttl must be positive");
        }
        if (refreshTokenTtl == null || refreshTokenTtl.isNegative() || refreshTokenTtl.isZero()) {
            throw new IllegalArgumentException("hive.jwt.refresh-token-ttl must be positive");
        }
    }
}
