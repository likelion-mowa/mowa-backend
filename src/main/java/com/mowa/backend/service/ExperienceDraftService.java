package com.mowa.backend.service;

import com.mowa.backend.common.exception.BusinessException;
import com.mowa.backend.common.exception.ErrorCode;
import com.mowa.backend.dto.experiencedraft.CreateExperienceDraftRequest;
import com.mowa.backend.dto.experiencedraft.ExperienceDraftResponse;
import com.mowa.backend.dto.experiencedraft.UpdateExperienceDraftRequest;
import com.mowa.backend.entity.AiGenerationStatus;
import com.mowa.backend.entity.CandidateStatus;
import com.mowa.backend.entity.Emotion;
import com.mowa.backend.entity.ExperienceDraft;
import com.mowa.backend.entity.WalkCandidate;
import com.mowa.backend.repository.ExperienceDraftRepository;
import com.mowa.backend.repository.WalkCandidateRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExperienceDraftService {

    private final ExperienceDraftRepository experienceDraftRepository;
    private final WalkCandidateRepository walkCandidateRepository;

    public ExperienceDraftService(
            ExperienceDraftRepository experienceDraftRepository,
            WalkCandidateRepository walkCandidateRepository
    ) {
        this.experienceDraftRepository = experienceDraftRepository;
        this.walkCandidateRepository = walkCandidateRepository;
    }

    @Transactional
    public ExperienceDraftResponse create(UUID userId, UUID candidateId, CreateExperienceDraftRequest request) {
        WalkCandidate candidate = findCandidate(userId, candidateId);

        if (candidate.getStatus() != CandidateStatus.RECORDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "Candidate status must be RECORDING.");
        }

        if (experienceDraftRepository.existsByCandidate_Id(candidateId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "ExperienceDraft already exists for candidate.");
        }

        ExperienceDraft draft = ExperienceDraft.create(
                candidate.getUser(),
                candidate,
                request.photoUrl(),
                request.companion(),
                toEmotionSet(request.emotions(), false),
                request.situation()
        );

        try {
            return ExperienceDraftResponse.from(experienceDraftRepository.saveAndFlush(draft));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "ExperienceDraft already exists for candidate.");
        }
    }

    @Transactional
    public ExperienceDraftResponse update(UUID userId, UUID draftId, UpdateExperienceDraftRequest request) {
        ExperienceDraft draft = findDraft(userId, draftId);
        validateUpdatableStatus(draft.getAiGenerationStatus());

        if (request.hasPhotoUrl()) {
            draft.updatePhotoUrl(request.getPhotoUrl());
        }

        if (request.hasCompanion()) {
            draft.updateCompanion(request.getCompanion());
        }

        if (request.hasEmotions()) {
            draft.replaceEmotions(toEmotionSet(request.getEmotions(), true));
        }

        if (request.hasSituation()) {
            draft.updateSituation(request.getSituation());
        }

        return ExperienceDraftResponse.from(draft);
    }

    private WalkCandidate findCandidate(UUID userId, UUID candidateId) {
        return walkCandidateRepository.findByIdAndUser_Id(candidateId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private ExperienceDraft findDraft(UUID userId, UUID draftId) {
        return experienceDraftRepository.findByIdAndUser_Id(draftId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private void validateUpdatableStatus(AiGenerationStatus status) {
        if (status != AiGenerationStatus.PENDING && status != AiGenerationStatus.FAILED) {
            throw new BusinessException(ErrorCode.CONFLICT, "ExperienceDraft cannot be updated in current AI status.");
        }
    }

    private Set<Emotion> toEmotionSet(List<Emotion> emotions, boolean rejectNullList) {
        if (emotions == null) {
            if (rejectNullList) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "emotions must not be null.");
            }
            return EnumSet.noneOf(Emotion.class);
        }

        Set<Emotion> emotionSet = EnumSet.noneOf(Emotion.class);
        for (Emotion emotion : emotions) {
            if (emotion == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "emotions must not contain null.");
            }
            if (!emotionSet.add(emotion)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "emotions must not contain duplicates.");
            }
        }
        return emotionSet;
    }
}
