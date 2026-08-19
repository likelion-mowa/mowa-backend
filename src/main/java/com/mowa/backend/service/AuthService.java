package com.mowa.backend.service;

import com.mowa.backend.common.exception.BusinessException;
import com.mowa.backend.common.exception.ErrorCode;
import com.mowa.backend.dto.auth.LoginRequest;
import com.mowa.backend.dto.auth.LoginResponse;
import com.mowa.backend.entity.User;
import com.mowa.backend.repository.UserRepository;
import com.mowa.backend.security.JwtTokenProvider;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final DemoSessionDataService demoSessionDataService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            DemoSessionDataService demoSessionDataService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.demoSessionDataService = demoSessionDataService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        UUID demoSessionId = UUID.randomUUID();
        if (demoSessionDataService.supports(user)) {
            demoSessionDataService.initializeDefaultDataIfDemoUser(user, demoSessionId);
        }

        return new LoginResponse(jwtTokenProvider.createAccessToken(user.getId(), demoSessionId));
    }
}
