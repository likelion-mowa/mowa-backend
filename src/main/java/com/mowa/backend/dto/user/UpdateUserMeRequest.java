package com.mowa.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserMeRequest(
        @NotNull
        @NotBlank
        @Size(max = 30)
        String nickname
) {
}
