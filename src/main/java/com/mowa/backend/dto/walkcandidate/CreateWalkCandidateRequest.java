package com.mowa.backend.dto.walkcandidate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record CreateWalkCandidateRequest(
        @NotNull
        OffsetDateTime detectedStartAt,

        @Size(max = 255)
        String locationSummary
) {
}
