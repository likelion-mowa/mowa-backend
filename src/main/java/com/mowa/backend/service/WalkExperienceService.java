package com.mowa.backend.service;

import com.mowa.backend.common.exception.BusinessException;
import com.mowa.backend.common.exception.ErrorCode;
import com.mowa.backend.dto.walkexperience.CreateWalkExperienceRequest;
import com.mowa.backend.dto.walkexperience.CreateWalkExperienceResponse;
import com.mowa.backend.entity.AiGenerationStatus;
import com.mowa.backend.entity.Emotion;
import com.mowa.backend.entity.ExperienceDraft;
import com.mowa.backend.entity.WalkCandidate;
import com.mowa.backend.entity.WalkExperience;
import com.mowa.backend.repository.ExperienceDraftRepository;
import com.mowa.backend.repository.WalkExperienceRepository;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalkExperienceService {

    private final ExperienceDraftRepository experienceDraftRepository;
    private final WalkExperienceRepository walkExperienceRepository;

    public WalkExperienceService(
            ExperienceDraftRepository experienceDraftRepository,
            WalkExperienceRepository walkExperienceRepository
    ) {
        this.experienceDraftRepository = experienceDraftRepository;
        this.walkExperienceRepository = walkExperienceRepository;
    }

    @Transactional
    public CreateWalkExperienceResponse create(UUID userId, CreateWalkExperienceRequest request) {
        ExperienceDraft draft = experienceDraftRepository
                .findByIdAndUser_IdForUpdate(request.draftId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        validateDraft(draft, userId);
        if (walkExperienceRepository.existsByDraft_Id(request.draftId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "WalkExperience already exists for draft.");
        }

        WalkCandidate candidate = draft.getCandidate();
        validateCandidate(candidate);

        WalkExperience experience = WalkExperience.create(
                draft.getUser(),
                draft,
                candidate,
                request.title(),
                request.body(),
                request.photoUrl(),
                request.companion(),
                toEmotionSet(request.emotions()),
                request.situation(),
                toTagSet(request.tags())
        );

        return CreateWalkExperienceResponse.from(walkExperienceRepository.saveAndFlush(experience));
    }

    private void validateDraft(ExperienceDraft draft, UUID userId) {
        if (!draft.getUser().getId().equals(userId)
                || !draft.getCandidate().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (draft.getAiGenerationStatus() != AiGenerationStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.CONFLICT, "ExperienceDraft AI status must be SUCCESS.");
        }
    }

    private void validateCandidate(WalkCandidate candidate) {
        if (candidate.getDetectedEndAt() == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "Candidate detectedEndAt is required.");
        }
        if (candidate.getDurationSeconds() == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "Candidate durationSeconds is required.");
        }
    }

    private Set<Emotion> toEmotionSet(List<Emotion> emotions) {
        if (emotions == null) {
            return EnumSet.noneOf(Emotion.class);
        }
        Set<Emotion> result = EnumSet.noneOf(Emotion.class);
        for (Emotion emotion : emotions) {
            if (emotion == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "emotions must not contain null.");
            }
            if (!result.add(emotion)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "emotions must not contain duplicates.");
            }
        }
        return result;
    }

    private Set<String> toTagSet(List<String> tags) {
        if (tags == null) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "tags must not contain blank values.");
            }
            if (tag.length() > 50) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "tags must not exceed 50 characters.");
            }
            if (!result.add(tag)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "tags must not contain duplicates.");
            }
        }
        return result;
    }
}
