package com.mowa.backend.service;

import com.mowa.backend.entity.Companion;
import com.mowa.backend.entity.Emotion;
import com.mowa.backend.entity.ExperienceDraft;
import com.mowa.backend.entity.Situation;
import com.mowa.backend.entity.WalkCandidate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

record ExperienceDraftAiGenerationInput(
        UUID draftId,
        boolean hasPhoto,
        Companion companion,
        List<Emotion> emotions,
        Situation situation,
        OffsetDateTime detectedStartAt,
        OffsetDateTime detectedEndAt,
        Integer durationSeconds,
        String locationSummary
) {

    static ExperienceDraftAiGenerationInput from(ExperienceDraft draft) {
        WalkCandidate candidate = draft.getCandidate();
        return new ExperienceDraftAiGenerationInput(
                draft.getId(),
                draft.getPhotoUrl() != null && !draft.getPhotoUrl().isBlank(),
                draft.getCompanion(),
                draft.getEmotions().stream()
                        .sorted(Comparator.comparingInt(Enum::ordinal))
                        .toList(),
                draft.getSituation(),
                candidate.getDetectedStartAt(),
                candidate.getDetectedEndAt(),
                candidate.getDurationSeconds(),
                candidate.getLocationSummary()
        );
    }
}
