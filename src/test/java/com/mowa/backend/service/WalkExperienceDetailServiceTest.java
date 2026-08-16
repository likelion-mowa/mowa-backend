package com.mowa.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mowa.backend.common.exception.BusinessException;
import com.mowa.backend.common.exception.ErrorCode;
import com.mowa.backend.dto.walkexperience.WalkExperienceDetailResponse;
import com.mowa.backend.entity.Companion;
import com.mowa.backend.entity.Emotion;
import com.mowa.backend.entity.Situation;
import com.mowa.backend.entity.WalkExperience;
import com.mowa.backend.repository.ExperienceDraftRepository;
import com.mowa.backend.repository.WalkExperienceRepository;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WalkExperienceDetailServiceTest {

    private WalkExperienceRepository walkExperienceRepository;
    private WalkExperienceService walkExperienceService;
    private UUID userId;
    private UUID demoSessionId;
    private UUID experienceId;

    @BeforeEach
    void setUp() {
        walkExperienceRepository = mock(WalkExperienceRepository.class);
        walkExperienceService = new WalkExperienceService(
                mock(ExperienceDraftRepository.class),
                walkExperienceRepository
        );
        userId = UUID.randomUUID();
        demoSessionId = UUID.randomUUID();
        experienceId = UUID.randomUUID();
    }

    @Test
    void getsOwnedActiveExperienceWithDetailFields() {
        WalkExperience experience = detailExperience(
                Set.of(Emotion.PENSIVE, Emotion.CALM),
                "두번째",
                "첫번째"
        );
        when(walkExperienceRepository.findActiveByIdAndUserIdAndDemoSessionId(experienceId, userId, demoSessionId))
                .thenReturn(Optional.of(experience));

        WalkExperienceDetailResponse response = walkExperienceService.get(userId, demoSessionId, experienceId);

        verify(walkExperienceRepository).findActiveByIdAndUserIdAndDemoSessionId(
                experienceId,
                userId,
                demoSessionId
        );
        assertThat(response.experienceId()).isEqualTo(experienceId);
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.body()).isEqualTo("본문");
        assertThat(response.photoUrl()).isEqualTo("https://example.com/photo.jpg");
        assertThat(response.startedAt()).isEqualTo(OffsetDateTime.parse("2026-08-11T13:00:00+09:00"));
        assertThat(response.endedAt()).isEqualTo(OffsetDateTime.parse("2026-08-11T14:00:00+09:00"));
        assertThat(response.durationSeconds()).isEqualTo(3600);
        assertThat(response.locationSummary()).isEqualTo("망원동");
        assertThat(response.companion()).isEqualTo(Companion.ALONE);
        assertThat(response.emotions()).containsExactly(Emotion.CALM, Emotion.PENSIVE);
        assertThat(response.situation()).isEqualTo(Situation.AFTERNOON);
        assertThat(response.tags()).containsExactly("두번째", "첫번째");
    }

    @Test
    void returnsNotFoundWhenExperienceDoesNotExist() {
        assertNotFoundForEmptyRepositoryResult();
    }

    @Test
    void returnsNotFoundWhenExperienceBelongsToAnotherUser() {
        assertNotFoundForEmptyRepositoryResult();
    }

    @Test
    void returnsNotFoundWhenExperienceIsSoftDeleted() {
        assertNotFoundForEmptyRepositoryResult();
    }

    @Test
    void returnsEmptyEmotionAndTagArrays() {
        WalkExperience experience = detailExperience(Set.of());
        when(walkExperienceRepository.findActiveByIdAndUserIdAndDemoSessionId(experienceId, userId, demoSessionId))
                .thenReturn(Optional.of(experience));

        WalkExperienceDetailResponse response = walkExperienceService.get(userId, demoSessionId, experienceId);

        assertThat(response.emotions()).isEmpty();
        assertThat(response.tags()).isEmpty();
    }

    @Test
    void detailResponseContainsOnlySpecifiedFields() {
        assertThat(Arrays.stream(WalkExperienceDetailResponse.class.getRecordComponents())
                .map(component -> component.getName()))
                .containsExactly(
                        "experienceId",
                        "title",
                        "body",
                        "photoUrl",
                        "startedAt",
                        "endedAt",
                        "durationSeconds",
                        "locationSummary",
                        "companion",
                        "emotions",
                        "situation",
                        "tags"
                )
                .doesNotContain("draftId", "userId", "createdAt", "updatedAt", "deletedAt");
    }

    private void assertNotFoundForEmptyRepositoryResult() {
        when(walkExperienceRepository.findActiveByIdAndUserIdAndDemoSessionId(experienceId, userId, demoSessionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> walkExperienceService.get(userId, demoSessionId, experienceId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    private WalkExperience detailExperience(Set<Emotion> emotions, String... tags) {
        WalkExperience experience = mock(WalkExperience.class);
        when(experience.getId()).thenReturn(experienceId);
        when(experience.getTitle()).thenReturn("제목");
        when(experience.getBody()).thenReturn("본문");
        when(experience.getPhotoUrl()).thenReturn("https://example.com/photo.jpg");
        when(experience.getStartedAt()).thenReturn(OffsetDateTime.parse("2026-08-11T13:00:00+09:00"));
        when(experience.getEndedAt()).thenReturn(OffsetDateTime.parse("2026-08-11T14:00:00+09:00"));
        when(experience.getDurationSeconds()).thenReturn(3600);
        when(experience.getLocationSummary()).thenReturn("망원동");
        when(experience.getCompanion()).thenReturn(Companion.ALONE);
        when(experience.getEmotions()).thenReturn(emotions);
        when(experience.getSituation()).thenReturn(Situation.AFTERNOON);
        when(experience.getTags()).thenReturn(new LinkedHashSet<>(Arrays.asList(tags)));
        return experience;
    }
}
