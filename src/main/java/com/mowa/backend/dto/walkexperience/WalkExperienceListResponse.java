package com.mowa.backend.dto.walkexperience;

import com.mowa.backend.entity.Companion;
import com.mowa.backend.entity.Emotion;
import com.mowa.backend.entity.Situation;
import com.mowa.backend.entity.WalkExperience;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record WalkExperienceListResponse(
        UUID experienceId,
        String photoUrl,
        String title,
        OffsetDateTime startedAt,
        Integer durationSeconds,
        String locationSummary,
        Companion companion,
        List<Emotion> emotions,
        Situation situation,
        List<String> tags
) {

    public static WalkExperienceListResponse from(WalkExperience experience) {
        return new WalkExperienceListResponse(
                experience.getId(),
                experience.getPhotoUrl(),
                experience.getTitle(),
                experience.getStartedAt(),
                experience.getDurationSeconds(),
                experience.getLocationSummary(),
                experience.getCompanion(),
                experience.getEmotions().stream()
                        .sorted(Comparator.comparingInt(Enum::ordinal))
                        .toList(),
                experience.getSituation(),
                List.copyOf(experience.getTags())
        );
    }
}
