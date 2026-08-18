package com.mowa.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mowa.backend.common.exception.BusinessException;
import com.mowa.backend.common.exception.ErrorCode;
import com.mowa.backend.dto.walkexperience.UpdateWalkExperienceRequest;
import com.mowa.backend.dto.walkexperience.WalkExperienceDetailResponse;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WalkExperienceUpdateServiceTest {

    private static final OffsetDateTime STARTED_AT = OffsetDateTime.parse("2026-08-15T10:00:00+09:00");
    private static final OffsetDateTime ENDED_AT = OffsetDateTime.parse("2026-08-15T11:00:00+09:00");

    private WalkExperienceRepository repository;
    private ExperienceDraftAiClient aiClient;
    private WalkExperienceService service;
    private UUID userId;
    private UUID demoSessionId;
    private UUID experienceId;
    private UUID draftId;
    private WalkExperience experience;

    @BeforeEach
    void setUp() {
        repository = mock(WalkExperienceRepository.class);
        aiClient = mock(ExperienceDraftAiClient.class);
        service = new WalkExperienceService(mock(ExperienceDraftRepository.class), repository, aiClient);
        userId = UUID.randomUUID();
        demoSessionId = UUID.randomUUID();
        experienceId = UUID.randomUUID();
        draftId = UUID.randomUUID();
        experience = experience();
        when(aiClient.generate(any(ExperienceDraftAiGenerationInput.class)))
                .thenReturn(new ExperienceDraftAiGenerationResult(
                        "AI title",
                        "AI body",
                        List.of("ai-tag", "walk")
                ));
        when(repository.findActiveByIdAndUserIdAndDemoSessionId(experienceId, userId, demoSessionId))
                .thenReturn(Optional.of(experience));
    }

    @Test
    void updatesOwnedActiveExperienceAndKeepsSnapshot() {
        UpdateWalkExperienceRequest request = new UpdateWalkExperienceRequest();
        request.setTitle("changed");
        request.setBody("changed body");
        request.setPhotoUrl("changed photo");
        request.setCompanion(Companion.PET);
        request.setEmotions(List.of(Emotion.PENSIVE, Emotion.CALM));
        request.setSituation(Situation.EVENING);
        request.setTags(List.of("changed", "#raw"));

        WalkExperienceDetailResponse response = service.update(userId, demoSessionId, experienceId, request);

        verify(repository).findActiveByIdAndUserIdAndDemoSessionId(experienceId, userId, demoSessionId);
        assertThat(response.title()).isEqualTo("AI title");
        assertThat(response.body()).isEqualTo("AI body");
        assertThat(response.photoUrl()).isEqualTo("changed photo");
        assertThat(response.companion()).isEqualTo(Companion.PET);
        assertThat(response.emotions()).containsExactly(Emotion.CALM, Emotion.PENSIVE);
        assertThat(response.situation()).isEqualTo(Situation.EVENING);
        assertThat(response.tags()).containsExactlyInAnyOrder("ai-tag", "walk");
        assertThat(response.startedAt()).isEqualTo(STARTED_AT);
        assertThat(response.endedAt()).isEqualTo(ENDED_AT);
        assertThat(response.durationSeconds()).isEqualTo(3600);
        assertThat(response.locationSummary()).isEqualTo("location");
        verify(aiClient).generate(any(ExperienceDraftAiGenerationInput.class));
    }

    @Test
    void omittedFieldsRemainUnchanged() {
        WalkExperienceDetailResponse response = service.update(
                userId,
                demoSessionId,
                experienceId,
                new UpdateWalkExperienceRequest()
        );

        assertThat(response.title()).isEqualTo("original");
        assertThat(response.body()).isEqualTo("body");
        assertThat(response.photoUrl()).isEqualTo("photo");
        assertThat(response.companion()).isEqualTo(Companion.ALONE);
        assertThat(response.emotions()).containsExactly(Emotion.HAPPY);
        assertThat(response.situation()).isEqualTo(Situation.MORNING);
        assertThat(response.tags()).containsExactly("tag");
        verify(aiClient, never()).generate(any());
    }

    @Test
    void nullableFieldsCanBeClearedAndBodyCanBeEmpty() {
        UpdateWalkExperienceRequest request = new UpdateWalkExperienceRequest();
        request.setBody(null);
        request.setPhotoUrl(null);
        request.setCompanion(null);
        request.setSituation(null);

        WalkExperienceDetailResponse cleared = service.update(userId, demoSessionId, experienceId, request);

        assertThat(cleared.body()).isEqualTo("AI body");
        assertThat(cleared.photoUrl()).isNull();
        assertThat(cleared.companion()).isNull();
        assertThat(cleared.situation()).isNull();

        UpdateWalkExperienceRequest emptyBody = new UpdateWalkExperienceRequest();
        emptyBody.setBody("");
        assertThat(service.update(userId, demoSessionId, experienceId, emptyBody).body()).isEmpty();
    }

    @Test
    void titleAcceptsOneHundredCharacters() {
        UpdateWalkExperienceRequest request = new UpdateWalkExperienceRequest();
        request.setTitle("a".repeat(100));

        assertThat(service.update(userId, demoSessionId, experienceId, request).title()).hasSize(100);
    }

    @Test
    void rejectsInvalidTitles() {
        assertInvalid(requestWithTitle(null));
        assertInvalid(requestWithTitle(""));
        assertInvalid(requestWithTitle("   "));
        assertInvalid(requestWithTitle("a".repeat(101)));
    }

    @Test
    void emotionsCanBeClearedOrReplaced() {
        UpdateWalkExperienceRequest clear = new UpdateWalkExperienceRequest();
        clear.setEmotions(List.of());
        assertThat(service.update(userId, demoSessionId, experienceId, clear).emotions()).isEmpty();

        UpdateWalkExperienceRequest replace = new UpdateWalkExperienceRequest();
        replace.setEmotions(List.of(Emotion.TIRED, Emotion.CALM));
        assertThat(service.update(userId, demoSessionId, experienceId, replace).emotions())
                .containsExactly(Emotion.CALM, Emotion.TIRED);
    }

    @Test
    void rejectsInvalidEmotionArrays() {
        UpdateWalkExperienceRequest nullArray = new UpdateWalkExperienceRequest();
        nullArray.setEmotions(null);
        assertInvalid(nullArray);

        UpdateWalkExperienceRequest nullElement = new UpdateWalkExperienceRequest();
        List<Emotion> emotions = new ArrayList<>();
        emotions.add(null);
        nullElement.setEmotions(emotions);
        assertInvalid(nullElement);

        UpdateWalkExperienceRequest duplicate = new UpdateWalkExperienceRequest();
        duplicate.setEmotions(List.of(Emotion.CALM, Emotion.CALM));
        assertInvalid(duplicate);
    }

    @Test
    void tagsCanBeClearedOrReplacedWithoutNormalization() {
        UpdateWalkExperienceRequest clear = new UpdateWalkExperienceRequest();
        clear.setTags(List.of());
        assertThat(service.update(userId, demoSessionId, experienceId, clear).tags()).isEmpty();

        UpdateWalkExperienceRequest replace = new UpdateWalkExperienceRequest();
        replace.setTags(List.of(" Tag ", "tag", "#tag"));
        assertThat(service.update(userId, demoSessionId, experienceId, replace).tags())
                .containsExactlyInAnyOrder(" Tag ", "tag", "#tag");
    }

    @Test
    void tagsAcceptDocumentedBoundaries() {
        UpdateWalkExperienceRequest request = new UpdateWalkExperienceRequest();
        request.setTags(List.of(
                "a".repeat(50), "2", "3", "4", "5", "6", "7", "8", "9", "10"
        ));

        assertThat(service.update(userId, demoSessionId, experienceId, request).tags()).hasSize(10);
    }

    @Test
    void rejectsInvalidTags() {
        assertInvalid(requestWithTags(null));
        assertInvalid(requestWithTags(listWithNull()));
        assertInvalid(requestWithTags(List.of("")));
        assertInvalid(requestWithTags(List.of("   ")));
        assertInvalid(requestWithTags(List.of("same", "same")));
        assertInvalid(requestWithTags(List.of("a".repeat(51))));
        assertInvalid(requestWithTags(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")));
    }

    @Test
    void regeneratesOnceWhenSituationActuallyChanges() {
        UpdateWalkExperienceRequest request = new UpdateWalkExperienceRequest();
        request.setSituation(Situation.EVENING);

        WalkExperienceDetailResponse response = service.update(userId, demoSessionId, experienceId, request);

        verify(aiClient, times(1)).generate(any());
        assertThat(response.title()).isEqualTo("AI title");
        assertThat(response.body()).isEqualTo("AI body");
        assertThat(response.tags()).containsExactlyInAnyOrder("ai-tag", "walk");
    }

    @Test
    void regeneratesOnlyOnceWhenCompanionAndSituationBothChange() {
        UpdateWalkExperienceRequest request = new UpdateWalkExperienceRequest();
        request.setCompanion(Companion.PET);
        request.setSituation(Situation.EVENING);

        service.update(userId, demoSessionId, experienceId, request);

        verify(aiClient, times(1)).generate(any());
    }

    @Test
    void doesNotRegenerateForExplicitlyUnchangedCompanionAndSituation() {
        UpdateWalkExperienceRequest request = new UpdateWalkExperienceRequest();
        request.setCompanion(Companion.ALONE);
        request.setSituation(Situation.MORNING);

        service.update(userId, demoSessionId, experienceId, request);

        verify(aiClient, never()).generate(any());
    }

    @Test
    void doesNotRegenerateWhenOnlyTitleBodyAndTagsChange() {
        UpdateWalkExperienceRequest request = new UpdateWalkExperienceRequest();
        request.setTitle("manual title");
        request.setBody("manual body");
        request.setTags(List.of("manual"));

        WalkExperienceDetailResponse response = service.update(userId, demoSessionId, experienceId, request);

        verify(aiClient, never()).generate(any());
        assertThat(response.title()).isEqualTo("manual title");
        assertThat(response.body()).isEqualTo("manual body");
        assertThat(response.tags()).containsExactly("manual");
    }

    @Test
    void explicitNullCompanionIsAnActualChangeAndRegenerates() {
        UpdateWalkExperienceRequest request = new UpdateWalkExperienceRequest();
        request.setCompanion(null);

        WalkExperienceDetailResponse response = service.update(userId, demoSessionId, experienceId, request);

        verify(aiClient).generate(any());
        assertThat(response.companion()).isNull();
    }

    @Test
    void nullToNonNullSituationIsAnActualChangeAndRegenerates() {
        experience = experience(Companion.ALONE, null);
        when(repository.findActiveByIdAndUserIdAndDemoSessionId(experienceId, userId, demoSessionId))
                .thenReturn(Optional.of(experience));
        UpdateWalkExperienceRequest request = new UpdateWalkExperienceRequest();
        request.setSituation(Situation.AFTERNOON);

        service.update(userId, demoSessionId, experienceId, request);

        verify(aiClient).generate(any());
    }

    @Test
    void aiInputUsesFinalPatchedExperienceSnapshot() {
        UpdateWalkExperienceRequest request = new UpdateWalkExperienceRequest();
        request.setPhotoUrl(null);
        request.setCompanion(Companion.PET);
        request.setEmotions(List.of(Emotion.TIRED, Emotion.CALM));
        request.setSituation(Situation.EVENING);

        service.update(userId, demoSessionId, experienceId, request);

        ArgumentCaptor<ExperienceDraftAiGenerationInput> captor =
                ArgumentCaptor.forClass(ExperienceDraftAiGenerationInput.class);
        verify(aiClient).generate(captor.capture());
        ExperienceDraftAiGenerationInput input = captor.getValue();
        assertThat(input.draftId()).isEqualTo(draftId);
        assertThat(input.hasPhoto()).isFalse();
        assertThat(input.companion()).isEqualTo(Companion.PET);
        assertThat(input.emotions()).containsExactly(Emotion.CALM, Emotion.TIRED);
        assertThat(input.situation()).isEqualTo(Situation.EVENING);
        assertThat(input.detectedStartAt()).isEqualTo(STARTED_AT);
        assertThat(input.detectedEndAt()).isEqualTo(ENDED_AT);
        assertThat(input.durationSeconds()).isEqualTo(3600);
        assertThat(input.locationSummary()).isEqualTo("location");
    }

    @Test
    void aiResultOverridesManualTitleBodyAndTagsWhenTriggerChanges() {
        UpdateWalkExperienceRequest request = new UpdateWalkExperienceRequest();
        request.setCompanion(Companion.PET);
        request.setTitle("manual title");
        request.setBody("manual body");
        request.setTags(List.of("manual"));

        WalkExperienceDetailResponse response = service.update(userId, demoSessionId, experienceId, request);

        assertThat(response.title()).isEqualTo("AI title");
        assertThat(response.body()).isEqualTo("AI body");
        assertThat(response.tags()).containsExactlyInAnyOrder("ai-tag", "walk");
    }

    @Test
    void translatesAiFailureToInternalServerError() {
        when(aiClient.generate(any())).thenThrow(new IllegalStateException("provider failed"));
        UpdateWalkExperienceRequest request = new UpdateWalkExperienceRequest();
        request.setCompanion(Companion.PET);

        assertThatThrownBy(() -> service.update(userId, demoSessionId, experienceId, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    @Test
    void rejectsInvalidAiTagsAsAiGenerationFailure() {
        when(aiClient.generate(any())).thenReturn(new ExperienceDraftAiGenerationResult(
                "AI title",
                "AI body",
                List.of("duplicate", "duplicate")
        ));
        UpdateWalkExperienceRequest request = new UpdateWalkExperienceRequest();
        request.setSituation(Situation.EVENING);

        assertThatThrownBy(() -> service.update(userId, demoSessionId, experienceId, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    @Test
    void missingOtherOwnedOtherSessionAndDeletedExperiencesAreAllNotFound() {
        when(repository.findActiveByIdAndUserIdAndDemoSessionId(experienceId, userId, demoSessionId))
                .thenReturn(Optional.empty());

        for (String ignored : List.of("missing", "other-owned", "other-session", "deleted")) {
            assertThatThrownBy(() -> service.update(
                    userId,
                    demoSessionId,
                    experienceId,
                    new UpdateWalkExperienceRequest()
            )).isInstanceOfSatisfying(BusinessException.class,
                    exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    private void assertInvalid(UpdateWalkExperienceRequest request) {
        assertThatThrownBy(() -> service.update(userId, demoSessionId, experienceId, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    private UpdateWalkExperienceRequest requestWithTitle(String title) {
        UpdateWalkExperienceRequest request = new UpdateWalkExperienceRequest();
        request.setTitle(title);
        return request;
    }

    private UpdateWalkExperienceRequest requestWithTags(List<String> tags) {
        UpdateWalkExperienceRequest request = new UpdateWalkExperienceRequest();
        request.setTags(tags);
        return request;
    }

    private List<String> listWithNull() {
        List<String> tags = new ArrayList<>();
        tags.add(null);
        return tags;
    }

    private WalkExperience experience() {
        return experience(Companion.ALONE, Situation.MORNING);
    }

    private WalkExperience experience(Companion companion, Situation situation) {
        User user = mock(User.class);
        ExperienceDraft draft = mock(ExperienceDraft.class);
        WalkCandidate candidate = mock(WalkCandidate.class);
        when(draft.getId()).thenReturn(draftId);
        when(draft.getDemoSessionId()).thenReturn(demoSessionId);
        when(candidate.getDetectedStartAt()).thenReturn(STARTED_AT);
        when(candidate.getDetectedEndAt()).thenReturn(ENDED_AT);
        when(candidate.getDurationSeconds()).thenReturn(3600);
        when(candidate.getLocationSummary()).thenReturn("location");
        return WalkExperience.create(
                user,
                draft,
                candidate,
                "original",
                "body",
                "photo",
                companion,
                Set.of(Emotion.HAPPY),
                situation,
                new LinkedHashSet<>(List.of("tag"))
        );
    }
}
