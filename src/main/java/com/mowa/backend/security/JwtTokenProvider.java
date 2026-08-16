package com.mowa.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class JwtTokenProvider {

    private static final String DEMO_SESSION_ID_CLAIM = "demoSessionId";

    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Autowired
    public JwtTokenProvider(JwtProperties jwtProperties) {
        this(jwtProperties, Clock.systemUTC());
    }

    JwtTokenProvider(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    public String createAccessToken(UUID userId, UUID demoSessionId) {
        Instant issuedAt = Instant.now(clock);
        Instant expiration = issuedAt.plusMillis(getAccessTokenExpirationMs());

        return Jwts.builder()
                .subject(userId.toString())
                .claim(DEMO_SESSION_ID_CLAIM, demoSessionId.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public AuthenticatedUser getAuthenticatedUser(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("JWT subject is empty.");
        }

        String demoSessionId = claims.get(DEMO_SESSION_ID_CLAIM, String.class);
        if (demoSessionId == null || demoSessionId.isBlank()) {
            throw new IllegalArgumentException("JWT demoSessionId claim is empty.");
        }

        return new AuthenticatedUser(UUID.fromString(subject), UUID.fromString(demoSessionId));
    }

    public boolean isValid(String token) {
        try {
            getAuthenticatedUser(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private SecretKey getSigningKey() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured.");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private long getAccessTokenExpirationMs() {
        Long expirationMs = jwtProperties.getAccessTokenExpirationMs();
        if (expirationMs == null || expirationMs <= 0) {
            throw new IllegalStateException("JWT access token expiration is not configured.");
        }
        return expirationMs;
    }
}
