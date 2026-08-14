package com.mowa.backend.dto.walkcandidate;

import com.mowa.backend.entity.CandidateStatus;
import com.mowa.backend.entity.WalkCandidate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WalkCandidateResponse(
        UUID candidateId,
        OffsetDateTime detectedStartAt,
        OffsetDateTime detectedEndAt,
        Integer durationSeconds,
        String locationSummary,
        CandidateStatus status
) {

    public static WalkCandidateResponse from(WalkCandidate candidate) {
        return new WalkCandidateResponse(
                candidate.getId(),
                candidate.getDetectedStartAt(),
                candidate.getDetectedEndAt(),
                candidate.getDurationSeconds(),
                candidate.getLocationSummary(),
                candidate.getStatus()
        );
    }
}
