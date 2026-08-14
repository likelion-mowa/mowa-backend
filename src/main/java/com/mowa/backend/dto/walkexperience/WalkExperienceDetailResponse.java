package com.mowa.backend.dto.walkexperience;

import com.mowa.backend.entity.Companion;
import com.mowa.backend.entity.Emotion;
import com.mowa.backend.entity.Situation;
import com.mowa.backend.entity.WalkExperience;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record WalkExperienceDetailResponse(
        UUID experienceId,
        String title,
        String body,
        String photoUrl,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        Integer durationSeconds,
        String locationSummary,
        Companion companion,
        List<Emotion> emotions,
        Situation situation,
        List<String> tags
) {

    public static WalkExperienceDetailResponse from(WalkExperience experience) {
        return new WalkExperienceDetailResponse(
                experience.getId(),
                experience.getTitle(),
                experience.getBody(),
                experience.getPhotoUrl(),
                experience.getStartedAt(),
                experience.getEndedAt(),
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
