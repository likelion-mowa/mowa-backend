package com.mowa.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mowa.backend.dto.auth.LoginRequest;
import com.mowa.backend.dto.auth.LoginResponse;
import com.mowa.backend.entity.User;
import com.mowa.backend.repository.UserRepository;
import com.mowa.backend.security.JwtTokenProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private DemoSessionDataService demoSessionDataService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        demoSessionDataService = mock(DemoSessionDataService.class);
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtTokenProvider,
                demoSessionDataService
        );
    }

    @Test
    void demoUserLoginCreatesNewSessionInitializesDefaultDataAndIssuesTokenWithSameSession() {
        User user = user(UUID.randomUUID(), "mowa01");
        when(userRepository.findByLoginId("mowa01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(demoSessionDataService.supports(user)).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(eq(user.getId()), any(UUID.class))).thenReturn("access-token");

        LoginResponse response = authService.login(new LoginRequest("mowa01", "password"));

        ArgumentCaptor<UUID> initializedSession = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<UUID> tokenSession = ArgumentCaptor.forClass(UUID.class);
        verify(demoSessionDataService).initializeDefaultDataIfDemoUser(eq(user), initializedSession.capture());
        verify(jwtTokenProvider).createAccessToken(eq(user.getId()), tokenSession.capture());
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(initializedSession.getValue()).isNotNull();
        assertThat(tokenSession.getValue()).isEqualTo(initializedSession.getValue());
    }

    @Test
    void demoUserReceivesDifferentDemoSessionsAcrossLogins() {
        User user = user(UUID.randomUUID(), "mowa01");
        when(userRepository.findByLoginId("mowa01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(demoSessionDataService.supports(user)).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(eq(user.getId()), any(UUID.class)))
                .thenReturn("first-token", "second-token");

        authService.login(new LoginRequest("mowa01", "password"));
        authService.login(new LoginRequest("mowa01", "password"));

        ArgumentCaptor<UUID> sessions = ArgumentCaptor.forClass(UUID.class);
        verify(demoSessionDataService, org.mockito.Mockito.times(2))
                .initializeDefaultDataIfDemoUser(eq(user), sessions.capture());
        assertThat(sessions.getAllValues()).hasSize(2);
        assertThat(sessions.getAllValues().get(0)).isNotEqualTo(sessions.getAllValues().get(1));
    }

    @Test
    void normalUserLoginDoesNotInitializeDemoDefaultData() {
        User user = user(UUID.randomUUID(), "normal01");
        when(userRepository.findByLoginId("normal01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(demoSessionDataService.supports(user)).thenReturn(false);
        when(jwtTokenProvider.createAccessToken(eq(user.getId()), any(UUID.class))).thenReturn("access-token");

        LoginResponse response = authService.login(new LoginRequest("normal01", "password"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(demoSessionDataService).supports(user);
        verify(demoSessionDataService, never()).initializeDefaultDataIfDemoUser(eq(user), any(UUID.class));
    }

    private User user(UUID id, String loginId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getLoginId()).thenReturn(loginId);
        when(user.getPasswordHash()).thenReturn("encoded");
        return user;
    }
}
