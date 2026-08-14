package com.mowa.backend.dto.user;

import com.mowa.backend.entity.User;
import java.util.UUID;

public record UserMeResponse(
        UUID userId,
        String loginId,
        String nickname
) {

    public static UserMeResponse from(User user) {
        return new UserMeResponse(user.getId(), user.getLoginId(), user.getNickname());
    }
}
