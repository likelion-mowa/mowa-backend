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

    @BeforeEach
    void setUp() {
        experienceDraftRepository = mock(ExperienceDraftRepository.class);
        walkExperienceRepository = mock(WalkExperienceRepository.class);
        walkExperienceService = new WalkExperienceService(experienceDraftRepository, walkExperienceRepository);
    }

    @Test
    void createsExperienceWithCandidateSnapshotAndFinalRequestValues() {
        UUID userId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-08-12T13:00:00+09:00");
        OffsetDateTime endedAt = OffsetDateTime.parse("2026-08-12T14:00:00+09:00");
        ExperienceDraft draft = successfulDraft(userId, startedAt, endedAt, 3600, "망원동");
        CreateWalkExperienceRequest request = request(draftId, List.of(Emotion.CALM), List.of("망원동"));

        when(experienceDraftRepository.findByIdAndUser_IdForUpdate(draftId, userId))
                .thenReturn(Optional.of(draft));
        when(walkExperienceRepository.existsByDraft_Id(draftId)).thenReturn(false);
        when(walkExperienceRepository.saveAndFlush(any(WalkExperience.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        walkExperienceService.create(userId, request);

        ArgumentCaptor<WalkExperience> captor = ArgumentCaptor.forClass(WalkExperience.class);
        verify(walkExperienceRepository).saveAndFlush(captor.capture());
        WalkExperience saved = captor.getValue();
        assertThat(saved.getStartedAt()).isEqualTo(startedAt);
        assertThat(saved.getEndedAt()).isEqualTo(endedAt);
        assertThat(saved.getDurationSeconds()).isEqualTo(3600);
        assertThat(saved.getLocationSummary()).isEqualTo("망원동");
        assertThat(saved.getTitle()).isEqualTo("최종 제목");
        assertThat(saved.getBody()).isEmpty();
        assertThat(saved.getPhotoUrl()).isEqualTo("https://example.com/photo.jpg");
        assertThat(saved.getCompanion()).isEqualTo(Companion.ALONE);
        assertThat(saved.getEmotions()).containsExactly(Emotion.CALM);
        assertThat(saved.getSituation()).isEqualTo(Situation.AFTERNOON);
        assertThat(saved.getTags()).containsExactly("망원동");
    }

    @Test
    void rejectsDraftThatIsNotOwnedAsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        when(experienceDraftRepository.findByIdAndUser_IdForUpdate(draftId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> walkExperienceService.create(userId, request(draftId, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void rejectsDraftWhenAiStatusIsNotSuccess() {
        UUID userId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        ExperienceDraft draft = draft(userId, AiGenerationStatus.PENDING,
                OffsetDateTime.now(), OffsetDateTime.now(), 10, null);
        when(experienceDraftRepository.findByIdAndUser_IdForUpdate(draftId, userId))
                .thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> walkExperienceService.create(userId, request(draftId, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void rejectsExistingExperienceForDraftAsConflict() {
        UUID userId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        ExperienceDraft draft = successfulDraft(
                userId, OffsetDateTime.now(), OffsetDateTime.now(), 10, null);
        when(experienceDraftRepository.findByIdAndUser_IdForUpdate(draftId, userId))
                .thenReturn(Optional.of(draft));
        when(walkExperienceRepository.existsByDraft_Id(draftId)).thenReturn(true);

        assertThatThrownBy(() -> walkExperienceService.create(userId, request(draftId, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void rejectsMissingCandidateDetectedEndAtAsConflict() {
        UUID userId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        ExperienceDraft draft = successfulDraft(userId, OffsetDateTime.now(), null, 10, null);
        stubCreatableDraft(userId, draftId, draft);

        assertThatThrownBy(() -> walkExperienceService.create(userId, request(draftId, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void rejectsMissingCandidateDurationSecondsAsConflict() {
        UUID userId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        ExperienceDraft draft = successfulDraft(
                userId, OffsetDateTime.now(), OffsetDateTime.now(), null, null);
        stubCreatableDraft(userId, draftId, draft);

        assertThatThrownBy(() -> walkExperienceService.create(userId, request(draftId, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void storesNullableEmotionAndTagArraysAsEmptyCollections() {
        UUID userId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        ExperienceDraft draft = successfulDraft(
                userId, OffsetDateTime.now(), OffsetDateTime.now(), 10, null);
        stubCreatableDraft(userId, draftId, draft);
        when(walkExperienceRepository.saveAndFlush(any(WalkExperience.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        walkExperienceService.create(userId, request(draftId, null, null));

        ArgumentCaptor<WalkExperience> captor = ArgumentCaptor.forClass(WalkExperience.class);
        verify(walkExperienceRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEmotions()).isEmpty();
        assertThat(captor.getValue().getTags()).isEmpty();
    }

    @Test
    void doesNotTranslateUnclassifiedDataIntegrityViolationAsDuplicateConflict() {
        UUID userId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        ExperienceDraft draft = successfulDraft(
                userId, OffsetDateTime.now(), OffsetDateTime.now(), 10, null);
        stubCreatableDraft(userId, draftId, draft);
        DataIntegrityViolationException databaseException =
                new DataIntegrityViolationException("unclassified constraint violation");
        when(walkExperienceRepository.saveAndFlush(any(WalkExperience.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> walkExperienceService.create(userId, request(draftId, null, null)))
                .isSameAs(databaseException);
    }

    @Test
    void rejectsDuplicateEmotionAndTagValuesAsInvalidRequest() {
        UUID userId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        ExperienceDraft draft = successfulDraft(
                userId, OffsetDateTime.now(), OffsetDateTime.now(), 10, null);
        when(experienceDraftRepository.findByIdAndUser_IdForUpdate(draftId, userId))
                .thenReturn(Optional.of(draft));
        when(walkExperienceRepository.existsByDraft_Id(draftId)).thenReturn(false);

        assertThatThrownBy(() -> walkExperienceService.create(
                userId,
                request(draftId, List.of(Emotion.CALM, Emotion.CALM), List.of("태그"))
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

        assertThatThrownBy(() -> walkExperienceService.create(
                userId,
                request(draftId, List.of(Emotion.CALM), List.of("태그", "태그"))
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    private void stubCreatableDraft(UUID userId, UUID draftId, ExperienceDraft draft) {
        when(experienceDraftRepository.findByIdAndUser_IdForUpdate(draftId, userId))
                .thenReturn(Optional.of(draft));
        when(walkExperienceRepository.existsByDraft_Id(draftId)).thenReturn(false);
    }

    private CreateWalkExperienceRequest request(UUID draftId, List<Emotion> emotions, List<String> tags) {
        return new CreateWalkExperienceRequest(
                draftId,
                "최종 제목",
                "",
                "https://example.com/photo.jpg",
                Companion.ALONE,
                emotions,
                Situation.AFTERNOON,
                tags
        );
    }

    private ExperienceDraft successfulDraft(
            UUID userId,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            Integer durationSeconds,
            String locationSummary
    ) {
        return draft(userId, AiGenerationStatus.SUCCESS, startedAt, endedAt, durationSeconds, locationSummary);
    }

    private ExperienceDraft draft(
            UUID userId,
            AiGenerationStatus status,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            Integer durationSeconds,
            String locationSummary
    ) {
        User user = mock(User.class);
        WalkCandidate candidate = mock(WalkCandidate.class);
        ExperienceDraft draft = mock(ExperienceDraft.class);
        when(user.getId()).thenReturn(userId);
        when(candidate.getUser()).thenReturn(user);
        when(candidate.getDetectedStartAt()).thenReturn(startedAt);
        when(candidate.getDetectedEndAt()).thenReturn(endedAt);
        when(candidate.getDurationSeconds()).thenReturn(durationSeconds);
        when(candidate.getLocationSummary()).thenReturn(locationSummary);
        when(draft.getUser()).thenReturn(user);
        when(draft.getCandidate()).thenReturn(candidate);
        when(draft.getAiGenerationStatus()).thenReturn(status);
        return draft;
    }
}
