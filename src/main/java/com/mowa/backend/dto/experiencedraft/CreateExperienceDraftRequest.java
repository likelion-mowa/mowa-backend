package com.mowa.backend.dto.experiencedraft;

import com.mowa.backend.entity.Companion;
import com.mowa.backend.entity.Emotion;
import com.mowa.backend.entity.Situation;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateExperienceDraftRequest(
        String photoUrl,
        Companion companion,
        List<@NotNull Emotion> emotions,
        Situation situation
) {
}
