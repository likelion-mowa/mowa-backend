package com.mowa.backend.service;

import java.util.List;

record ExperienceDraftAiGenerationResult(
        String aiTitle,
        String aiBody,
        List<String> suggestedTags
) {
}
