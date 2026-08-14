package com.mowa.backend.dto.experiencedraft;

import com.mowa.backend.entity.AiGenerationStatus;
import java.util.List;
import java.util.UUID;

public record ExperienceDraftAiGenerationResponse(
        UUID draftId,
        String aiTitle,
        String aiBody,
        List<String> suggestedTags,
        AiGenerationStatus aiGenerationStatus
) {
}
