package com.mowa.backend.dto.walkexperience;

import com.mowa.backend.entity.Companion;
import com.mowa.backend.entity.Emotion;
import com.mowa.backend.entity.Situation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateWalkExperienceRequest(
        @NotNull UUID draftId,
        @NotBlank @Size(max = 100) String title,
        String body,
        String photoUrl,
        Companion companion,
        List<@NotNull Emotion> emotions,
        Situation situation,
        @Size(max = 10) List<@NotBlank @Size(max = 50) String> tags
) {
}
