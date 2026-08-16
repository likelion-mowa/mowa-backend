package com.mowa.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mowa.backend.common.exception.BusinessException;
import com.mowa.backend.common.exception.ErrorCode;
import com.mowa.backend.dto.walkexperience.CreateWalkExperienceRequest;
import com.mowa.backend.entity.AiGenerationStatus;
import com.mowa.backend.entity.Companion;
import com.mowa.backend.entity.Emotion;
import com.mowa.backend.entity.ExperienceDraft;
import com.mowa.backend.entity.Situation;
import com.mowa.backend.entity.User;
import com.mowa.backend.entity.WalkCandidate;
import com.mowa.backend.entity.WalkExperience;
import com.mowa.backend.repository.ExperienceDraftRepository;
import com.mowa.backend.repository.WalkExperienceRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class WalkExperienceServiceTest {

    private ExperienceDraftRepository experienceDraftRepository;
    private WalkExperienceRepository walkExperienceRepository;
    private WalkExperienceService walkExperienceService;
    private UUID userId;
    private UUID demoSessionId;

    @BeforeEach
    void setUp() {
        experienceDraftRepository = mock(ExperienceDraftRepository.class);
        walkExperienceRepository = mock(WalkExperienceRepository.class);
        walkExperienceService = new WalkExperienceService(experienceDraftRepository, walkExperienceRepository);
        userId = UUID.randomUUID();
        demoSessionId = UUID.randomUUID();
    }

    @Test
    void createsExperienceWithCandidateSnapshotFinalRequestValuesAndDemoSession() {
        UUID draftId = UUID.randomUUID();
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-08-12T13:00:00+09:00");
        OffsetDateTime endedAt = OffsetDateTime.parse("2026-08-12T14:00:00+09:00");
        ExperienceDraft draft = successfulDraft(startedAt, endedAt, 3600, "park");
        CreateWalkExperienceRequest request = request(draftId, List.of(Emotion.CALM), List.of("park"));

        when(experienceDraftRepository.findByIdAndUser_IdAndDemoSessionIdForUpdate(
                draftId,
                userId,
                demoSessionId
        )).thenReturn(Optional.of(draft));
        when(walkExperienceRepository.existsByDraft_Id(draftId)).thenReturn(false);
        when(walkExperienceRepository.saveAndFlush(any(WalkExperience.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        walkExperienceService.create(userId, demoSessionId, request);

        ArgumentCaptor<WalkExperience> captor = ArgumentCaptor.forClass(WalkExperience.class);
        verify(walkExperienceRepository).saveAndFlush(captor.capture());
        WalkExperience saved = captor.getValue();
        assertThat(saved.getDemoSessionId()).isEqualTo(demoSessionId);
        assertThat(saved.getStartedAt()).isEqualTo(startedAt);
        assertThat(saved.getEndedAt()).isEqualTo(endedAt);
        assertThat(saved.getDurationSeconds()).isEqualTo(3600);
        assertThat(saved.getLocationSummary()).isEqualTo("park");
        assertThat(saved.getTitle()).isEqualTo("final title");
        assertThat(saved.getBody()).isEmpty();
        assertThat(saved.getPhotoUrl()).isEqualTo("https://example.com/photo.jpg");
        assertThat(saved.getCompanion()).isEqualTo(Companion.ALONE);
        assertThat(saved.getEmotions()).containsExactly(Emotion.CALM);
        assertThat(saved.getSituation()).isEqualTo(Situation.AFTERNOON);
        assertThat(saved.getTags()).containsExactly("park");
    }

    @Test
    void rejectsDraftThatIsNotOwnedOrNotInSessionAsNotFound() {
        UUID draftId = UUID.randomUUID();
        when(experienceDraftRepository.findByIdAndUser_IdAndDemoSessionIdForUpdate(
                draftId,
                userId,
                demoSessionId
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walkExperienceService.create(userId, demoSessionId, request(draftId, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void rejectsDraftCandidateDemoSessionMismatchAsNotFound() {
        UUID draftId = UUID.randomUUID();
        ExperienceDraft draft = successfulDraft(
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                10,
                null,
                UUID.randomUUID(),
                demoSessionId
        );
        when(experienceDraftRepository.findByIdAndUser_IdAndDemoSessionIdForUpdate(
                draftId,
                userId,
                demoSessionId
        )).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> walkExperienceService.create(userId, demoSessionId, request(draftId, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void rejectsDraftWhenAiStatusIsNotSuccess() {
        UUID draftId = UUID.randomUUID();
        ExperienceDraft draft = draft(
                AiGenerationStatus.PENDING,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                10,
                null,
                demoSessionId,
                demoSessionId
        );
        when(experienceDraftRepository.findByIdAndUser_IdAndDemoSessionIdForUpdate(
                draftId,
                userId,
                demoSessionId
        )).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> walkExperienceService.create(userId, demoSessionId, request(draftId, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void rejectsExistingExperienceForDraftAsConflict() {
        UUID draftId = UUID.randomUUID();
        ExperienceDraft draft = successfulDraft(OffsetDateTime.now(), OffsetDateTime.now(), 10, null);
        when(experienceDraftRepository.findByIdAndUser_IdAndDemoSessionIdForUpdate(
                draftId,
                userId,
                demoSessionId
        )).thenReturn(Optional.of(draft));
        when(walkExperienceRepository.existsByDraft_Id(draftId)).thenReturn(true);

        assertThatThrownBy(() -> walkExperienceService.create(userId, demoSessionId, request(draftId, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void rejectsMissingCandidateDetectedEndAtAsConflict() {
        UUID draftId = UUID.randomUUID();
        ExperienceDraft draft = successfulDraft(OffsetDateTime.now(), null, 10, null);
        stubCreatableDraft(draftId, draft);

        assertThatThrownBy(() -> walkExperienceService.create(userId, demoSessionId, request(draftId, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void rejectsMissingCandidateDurationSecondsAsConflict() {
        UUID draftId = UUID.randomUUID();
        ExperienceDraft draft = successfulDraft(OffsetDateTime.now(), OffsetDateTime.now(), null, null);
        stubCreatableDraft(draftId, draft);

        assertThatThrownBy(() -> walkExperienceService.create(userId, demoSessionId, request(draftId, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void storesNullableEmotionAndTagArraysAsEmptyCollections() {
        UUID draftId = UUID.randomUUID();
        ExperienceDraft draft = successfulDraft(OffsetDateTime.now(), OffsetDateTime.now(), 10, null);
        stubCreatableDraft(draftId, draft);
        when(walkExperienceRepository.saveAndFlush(any(WalkExperience.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        walkExperienceService.create(userId, demoSessionId, request(draftId, null, null));

        ArgumentCaptor<WalkExperience> captor = ArgumentCaptor.forClass(WalkExperience.class);
        verify(walkExperienceRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEmotions()).isEmpty();
        assertThat(captor.getValue().getTags()).isEmpty();
    }

    @Test
    void doesNotTranslateUnclassifiedDataIntegrityViolationAsDuplicateConflict() {
        UUID draftId = UUID.randomUUID();
        ExperienceDraft draft = successfulDraft(OffsetDateTime.now(), OffsetDateTime.now(), 10, null);
        stubCreatableDraft(draftId, draft);
        DataIntegrityViolationException databaseException =
                new DataIntegrityViolationException("unclassified constraint violation");
        when(walkExperienceRepository.saveAndFlush(any(WalkExperience.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> walkExperienceService.create(userId, demoSessionId, request(draftId, null, null)))
                .isSameAs(databaseException);
    }

    @Test
    void rejectsDuplicateEmotionAndTagValuesAsInvalidRequest() {
        UUID draftId = UUID.randomUUID();
        ExperienceDraft draft = successfulDraft(OffsetDateTime.now(), OffsetDateTime.now(), 10, null);
        stubCreatableDraft(draftId, draft);

        assertThatThrownBy(() -> walkExperienceService.create(
                userId,
                demoSessionId,
                request(draftId, List.of(Emotion.CALM, Emotion.CALM), List.of("tag"))
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

        assertThatThrownBy(() -> walkExperienceService.create(
                userId,
                demoSessionId,
                request(draftId, List.of(Emotion.CALM), List.of("tag", "tag"))
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    private void stubCreatableDraft(UUID draftId, ExperienceDraft draft) {
        when(experienceDraftRepository.findByIdAndUser_IdAndDemoSessionIdForUpdate(
                draftId,
                userId,
                demoSessionId
        )).thenReturn(Optional.of(draft));
        when(walkExperienceRepository.existsByDraft_Id(draftId)).thenReturn(false);
    }

    private CreateWalkExperienceRequest request(UUID draftId, List<Emotion> emotions, List<String> tags) {
        return new CreateWalkExperienceRequest(
                draftId,
                "final title",
                "",
                "https://example.com/photo.jpg",
                Companion.ALONE,
                emotions,
                Situation.AFTERNOON,
                tags
        );
    }

    private ExperienceDraft successfulDraft(
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            Integer durationSeconds,
            String locationSummary
    ) {
        return successfulDraft(startedAt, endedAt, durationSeconds, locationSummary, demoSessionId, demoSessionId);
    }

    private ExperienceDraft successfulDraft(
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            Integer durationSeconds,
            String locationSummary,
            UUID draftDemoSessionId,
            UUID candidateDemoSessionId
    ) {
        return draft(
                AiGenerationStatus.SUCCESS,
                startedAt,
                endedAt,
                durationSeconds,
                locationSummary,
                draftDemoSessionId,
                candidateDemoSessionId
        );
    }

    private ExperienceDraft draft(
            AiGenerationStatus status,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            Integer durationSeconds,
            String locationSummary,
            UUID draftDemoSessionId,
            UUID candidateDemoSessionId
    ) {
        User user = mock(User.class);
        WalkCandidate candidate = mock(WalkCandidate.class);
        ExperienceDraft draft = mock(ExperienceDraft.class);
        when(user.getId()).thenReturn(userId);
        when(candidate.getUser()).thenReturn(user);
        when(candidate.getDemoSessionId()).thenReturn(candidateDemoSessionId);
        when(candidate.getDetectedStartAt()).thenReturn(startedAt);
        when(candidate.getDetectedEndAt()).thenReturn(endedAt);
        when(candidate.getDurationSeconds()).thenReturn(durationSeconds);
        when(candidate.getLocationSummary()).thenReturn(locationSummary);
        when(draft.getUser()).thenReturn(user);
        when(draft.getDemoSessionId()).thenReturn(draftDemoSessionId);
        when(draft.getCandidate()).thenReturn(candidate);
        when(draft.getAiGenerationStatus()).thenReturn(status);
        return draft;
    }
}
