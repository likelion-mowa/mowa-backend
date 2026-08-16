package com.mowa.backend.service;

import com.mowa.backend.common.exception.BusinessException;
import com.mowa.backend.common.exception.ErrorCode;
import com.mowa.backend.dto.walkexperience.CreateWalkExperienceRequest;
import com.mowa.backend.dto.walkexperience.CreateWalkExperienceResponse;
import com.mowa.backend.dto.walkexperience.WalkExperienceListResponse;
import com.mowa.backend.dto.walkexperience.WalkExperienceDetailResponse;
import com.mowa.backend.dto.walkexperience.UpdateWalkExperienceRequest;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalkExperienceService {

    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

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
    public CreateWalkExperienceResponse create(
            UUID userId,
            UUID demoSessionId,
            CreateWalkExperienceRequest request
    ) {
        ExperienceDraft draft = experienceDraftRepository
                .findByIdAndUser_IdAndDemoSessionIdForUpdate(request.draftId(), userId, demoSessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        validateDraft(draft, userId, demoSessionId);
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

    @Transactional(readOnly = true)
    public List<WalkExperienceListResponse> getAll(
            UUID userId,
            UUID demoSessionId,
            LocalDate from,
            LocalDate to,
            String tag
    ) {
        validateQueryParameters(from, to, tag);

        List<WalkExperience> experiences;
        if (tag != null) {
            experiences = walkExperienceRepository
                    .findAllActiveByUserIdAndDemoSessionIdAndTag(userId, demoSessionId, tag);
        } else if (from != null) {
            OffsetDateTime startInclusive = from.atStartOfDay(SERVICE_ZONE_ID).toOffsetDateTime();
            OffsetDateTime endExclusive = to.plusDays(1).atStartOfDay(SERVICE_ZONE_ID).toOffsetDateTime();
            experiences = walkExperienceRepository.findAllActiveByUserIdAndDemoSessionIdAndStartedAtRange(
                    userId,
                    demoSessionId,
                    startInclusive,
                    endExclusive
            );
        } else {
            experiences = walkExperienceRepository.findAllActiveByUserIdAndDemoSessionId(userId, demoSessionId);
        }

        return experiences.stream()
                .map(WalkExperienceListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WalkExperienceDetailResponse get(UUID userId, UUID demoSessionId, UUID experienceId) {
        WalkExperience experience = walkExperienceRepository
                .findActiveByIdAndUserIdAndDemoSessionId(experienceId, userId, demoSessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return WalkExperienceDetailResponse.from(experience);
    }

    @Transactional
    public WalkExperienceDetailResponse update(
            UUID userId,
            UUID demoSessionId,
            UUID experienceId,
            UpdateWalkExperienceRequest request
    ) {
        WalkExperience experience = walkExperienceRepository
                .findActiveByIdAndUserIdAndDemoSessionId(experienceId, userId, demoSessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (request.hasTitle()) {
            validateTitle(request.getTitle());
            experience.updateTitle(request.getTitle());
        }
        if (request.hasBody()) {
            experience.updateBody(request.getBody());
        }
        if (request.hasPhotoUrl()) {
            experience.updatePhotoUrl(request.getPhotoUrl());
        }
        if (request.hasCompanion()) {
            experience.updateCompanion(request.getCompanion());
        }
        if (request.hasEmotions()) {
            experience.replaceEmotions(toEmotionSetForUpdate(request.getEmotions()));
        }
        if (request.hasSituation()) {
            experience.updateSituation(request.getSituation());
        }
        if (request.hasTags()) {
            experience.replaceTags(toTagSetForUpdate(request.getTags()));
        }

        return WalkExperienceDetailResponse.from(experience);
    }

    @Transactional
    public void delete(UUID userId, UUID demoSessionId, UUID experienceId) {
        WalkExperience experience = walkExperienceRepository
                .findActiveByIdAndUserIdAndDemoSessionId(experienceId, userId, demoSessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        experience.softDelete(OffsetDateTime.now());
    }

    private void validateQueryParameters(LocalDate from, LocalDate to, String tag) {
        boolean hasFrom = from != null;
        boolean hasTo = to != null;
        boolean hasTag = tag != null;

        if (hasTag && (hasFrom || hasTo)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Date range and tag cannot be used together.");
        }
        if (hasFrom != hasTo) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "from and to must be provided together.");
        }
        if (hasFrom && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "from must not be after to.");
        }
    }

    private void validateDraft(ExperienceDraft draft, UUID userId, UUID demoSessionId) {
        if (!draft.getUser().getId().equals(userId)
                || !draft.getCandidate().getUser().getId().equals(userId)
                || !draft.getDemoSessionId().equals(demoSessionId)
                || !draft.getCandidate().getDemoSessionId().equals(demoSessionId)) {
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

    private Set<Emotion> toEmotionSetForUpdate(List<Emotion> emotions) {
        if (emotions == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "emotions must not be null.");
        }
        return toEmotionSet(emotions);
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

    private Set<String> toTagSetForUpdate(List<String> tags) {
        if (tags == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "tags must not be null.");
        }
        if (tags.size() > 10) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "tags must not exceed 10 items.");
        }
        return toTagSet(tags);
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "title must not be blank.");
        }
        if (title.length() > 100) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "title must not exceed 100 characters.");
        }
    }
}
