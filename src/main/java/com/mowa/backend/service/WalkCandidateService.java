package com.mowa.backend.service;

import com.mowa.backend.common.exception.BusinessException;
import com.mowa.backend.common.exception.ErrorCode;
import com.mowa.backend.dto.walkcandidate.CreateWalkCandidateRequest;
import com.mowa.backend.dto.walkcandidate.CreateWalkCandidateResponse;
import com.mowa.backend.dto.walkcandidate.UpdateWalkCandidateRequest;
import com.mowa.backend.dto.walkcandidate.WalkCandidateResponse;
import com.mowa.backend.entity.CandidateStatus;
import com.mowa.backend.entity.User;
import com.mowa.backend.entity.WalkCandidate;
import com.mowa.backend.repository.UserRepository;
import com.mowa.backend.repository.WalkCandidateRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalkCandidateService {

    private final WalkCandidateRepository walkCandidateRepository;
    private final UserRepository userRepository;

    public WalkCandidateService(
            WalkCandidateRepository walkCandidateRepository,
            UserRepository userRepository
    ) {
        this.walkCandidateRepository = walkCandidateRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CreateWalkCandidateResponse create(UUID userId, CreateWalkCandidateRequest request) {
        User user = findUser(userId);
        WalkCandidate candidate = WalkCandidate.create(
                user,
                request.detectedStartAt(),
                request.locationSummary()
        );

        return CreateWalkCandidateResponse.from(walkCandidateRepository.save(candidate));
    }

    @Transactional(readOnly = true)
    public WalkCandidateResponse get(UUID userId, UUID candidateId) {
        return WalkCandidateResponse.from(findCandidate(userId, candidateId));
    }

    @Transactional
    public WalkCandidateResponse update(UUID userId, UUID candidateId, UpdateWalkCandidateRequest request) {
        WalkCandidate candidate = findCandidate(userId, candidateId);

        if (request.hasDetectedEndAt()) {
            validateDetectedEndAt(candidate.getDetectedStartAt(), request.getDetectedEndAt());
            candidate.updateDetectedEndAt(request.getDetectedEndAt());
        }

        if (request.hasDurationSeconds()) {
            candidate.updateDurationSeconds(request.getDurationSeconds());
        }

        if (request.hasLocationSummary()) {
            candidate.updateLocationSummary(request.getLocationSummary());
        }

        if (request.hasStatus()) {
            validateStatusTransition(candidate.getStatus(), request.getStatus());
            candidate.updateStatus(request.getStatus());
        }

        return WalkCandidateResponse.from(candidate);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private WalkCandidate findCandidate(UUID userId, UUID candidateId) {
        return walkCandidateRepository.findByIdAndUser_Id(candidateId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private void validateDetectedEndAt(OffsetDateTime detectedStartAt, OffsetDateTime detectedEndAt) {
        if (detectedEndAt != null && detectedEndAt.isBefore(detectedStartAt)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "detectedEndAt must not be earlier than detectedStartAt."
            );
        }
    }

    private void validateStatusTransition(CandidateStatus currentStatus, CandidateStatus nextStatus) {
        if (nextStatus == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "status must not be null.");
        }

        boolean allowed = (currentStatus == CandidateStatus.DETECTED && nextStatus == CandidateStatus.SUGGESTED)
                || (currentStatus == CandidateStatus.SUGGESTED && nextStatus == CandidateStatus.RECORDING)
                || (currentStatus == CandidateStatus.SUGGESTED && nextStatus == CandidateStatus.SKIPPED);

        if (!allowed) {
            throw new BusinessException(ErrorCode.CONFLICT, "Candidate status transition is not allowed.");
        }
    }
}
