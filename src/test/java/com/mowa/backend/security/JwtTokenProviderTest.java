package com.mowa.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    @Test
    void accessTokenCarriesUserIdAndDemoSessionId() {
        JwtTokenProvider provider = new JwtTokenProvider(properties());
        UUID userId = UUID.randomUUID();
        UUID demoSessionId = UUID.randomUUID();

        String token = provider.createAccessToken(userId, demoSessionId);

        assertThat(provider.isValid(token)).isTrue();
        AuthenticatedUser authenticatedUser = provider.getAuthenticatedUser(token);
        assertThat(authenticatedUser.userId()).isEqualTo(userId);
        assertThat(authenticatedUser.demoSessionId()).isEqualTo(demoSessionId);
    }

    @Test
    void sameUserCanReceiveDifferentDemoSessionsAcrossLogins() {
        JwtTokenProvider provider = new JwtTokenProvider(properties());
        UUID userId = UUID.randomUUID();

        String first = provider.createAccessToken(userId, UUID.randomUUID());
        String second = provider.createAccessToken(userId, UUID.randomUUID());

        assertThat(provider.getAuthenticatedUser(first).userId()).isEqualTo(userId);
        assertThat(provider.getAuthenticatedUser(second).userId()).isEqualTo(userId);
        assertThat(provider.getAuthenticatedUser(first).demoSessionId())
                .isNotEqualTo(provider.getAuthenticatedUser(second).demoSessionId());
    }

    private JwtProperties properties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789abcdef0123456789abcdef");
        properties.setAccessTokenExpirationMs(3_600_000L);
        return properties;
    }
}
