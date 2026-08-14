package com.mowa.backend.dto.walkexperience;

import com.mowa.backend.entity.WalkExperience;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateWalkExperienceResponse(UUID experienceId, UUID draftId, OffsetDateTime createdAt) {

    public static CreateWalkExperienceResponse from(WalkExperience experience) {
        return new CreateWalkExperienceResponse(
                experience.getId(),
                experience.getDraft().getId(),
                experience.getCreatedAt()
        );
    }
}
