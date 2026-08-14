package com.mowa.backend.dto.experiencedraft;

import com.mowa.backend.entity.AiGenerationStatus;
import com.mowa.backend.entity.Companion;
import com.mowa.backend.entity.Emotion;
import com.mowa.backend.entity.ExperienceDraft;
import com.mowa.backend.entity.Situation;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record ExperienceDraftResponse(
        UUID draftId,
        UUID candidateId,
        String photoUrl,
        Companion companion,
        List<Emotion> emotions,
        Situation situation,
        AiGenerationStatus aiGenerationStatus
) {

    public static ExperienceDraftResponse from(ExperienceDraft draft) {
        return new ExperienceDraftResponse(
                draft.getId(),
                draft.getCandidate().getId(),
                draft.getPhotoUrl(),
                draft.getCompanion(),
                draft.getEmotions().stream()
                        .sorted(Comparator.comparingInt(Enum::ordinal))
                        .toList(),
                draft.getSituation(),
                draft.getAiGenerationStatus()
        );
    }
}
