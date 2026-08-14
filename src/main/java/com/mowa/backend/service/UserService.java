package com.mowa.backend.service;

import com.mowa.backend.common.exception.BusinessException;
import com.mowa.backend.common.exception.ErrorCode;
import com.mowa.backend.dto.user.UpdateUserMeRequest;
import com.mowa.backend.dto.user.UserMeResponse;
import com.mowa.backend.entity.User;
import com.mowa.backend.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserMeResponse getMe(UUID userId) {
        return UserMeResponse.from(findUser(userId));
    }

    @Transactional
    public UserMeResponse updateNickname(UUID userId, UpdateUserMeRequest request) {
        User user = findUser(userId);
        user.updateNickname(request.nickname());
        return UserMeResponse.from(user);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
