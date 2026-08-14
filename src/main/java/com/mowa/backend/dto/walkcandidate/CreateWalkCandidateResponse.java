package com.mowa.backend.dto.walkcandidate;

import com.mowa.backend.entity.CandidateStatus;
import com.mowa.backend.entity.WalkCandidate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateWalkCandidateResponse(
        UUID candidateId,
        OffsetDateTime detectedStartAt,
        String locationSummary,
        CandidateStatus status
) {

    public static CreateWalkCandidateResponse from(WalkCandidate candidate) {
        return new CreateWalkCandidateResponse(
                candidate.getId(),
                candidate.getDetectedStartAt(),
                candidate.getLocationSummary(),
                candidate.getStatus()
        );
    }
}
