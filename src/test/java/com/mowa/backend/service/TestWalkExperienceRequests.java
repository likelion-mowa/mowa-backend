package com.mowa.backend.service;

import com.mowa.backend.dto.walkexperience.CreateWalkExperienceRequest;
import com.mowa.backend.entity.Companion;
import com.mowa.backend.entity.Emotion;
import com.mowa.backend.entity.Situation;
import java.util.List;
import java.util.UUID;

final class TestWalkExperienceRequests {

    private TestWalkExperienceRequests() {
    }

    static CreateWalkExperienceRequest validCreateRequest(UUID draftId) {
        return new CreateWalkExperienceRequest(
                draftId,
                "final title",
                "body",
                "https://example.com/photo.jpg",
                Companion.ALONE,
                List.of(Emotion.CALM),
                Situation.AFTERNOON,
                List.of("tag")
        );
    }
}
