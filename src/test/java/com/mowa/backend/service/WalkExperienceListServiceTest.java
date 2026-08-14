package com.mowa.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mowa.backend.common.exception.BusinessException;
import com.mowa.backend.common.exception.ErrorCode;
import com.mowa.backend.dto.walkexperience.WalkExperienceListResponse;
import com.mowa.backend.entity.Companion;
import com.mowa.backend.entity.Emotion;
import com.mowa.backend.entity.Situation;
import com.mowa.backend.entity.WalkExperience;
import com.mowa.backend.repository.ExperienceDraftRepository;
import com.mowa.backend.repository.WalkExperienceRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WalkExperienceListServiceTest {

    private WalkExperienceRepository walkExperienceRepository;
    private WalkExperienceService walkExperienceService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        walkExperienceRepository = mock(WalkExperienceRepository.class);
        walkExperienceService = new WalkExperienceService(
                mock(ExperienceDraftRepository.class),
                walkExperienceRepository
        );
        userId = UUID.randomUUID();
    }

    @Test
    void getsAllExperiencesWithoutQueryParameters() {
        when(walkExperienceRepository.findAllActiveByUserId(userId)).thenReturn(List.of());

        List<WalkExperienceListResponse> result = walkExperienceService.getAll(userId, null, null, null);

        assertThat(result).isEmpty();
        verify(walkExperienceRepository).findAllActiveByUserId(userId);
    }

    @Test
    void getsDateRangeUsingInclusiveKstStartAndExclusiveDayAfterEnd() {
        LocalDate date = LocalDate.of(2026, 8, 11);
        when(walkExperienceRepository.findAllActiveByUserIdAndStartedAtRange(
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of());

        walkExperienceService.getAll(userId, date, date, null);

        ArgumentCaptor<OffsetDateTime> startCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> endCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(walkExperienceRepository).findAllActiveByUserIdAndStartedAtRange(
                org.mockito.ArgumentMatchers.eq(userId),
                startCaptor.capture(),
                endCaptor.capture()
        );
        assertThat(startCaptor.getValue()).isEqualTo(OffsetDateTime.parse("2026-08-11T00:00:00+09:00"));
        assertThat(endCaptor.getValue()).isEqualTo(OffsetDateTime.parse("2026-08-12T00:00:00+09:00"));
    }

    @Test
    void rejectsFromWithoutTo() {
        assertInvalidRequest(LocalDate.of(2026, 8, 1), null, null);
    }

    @Test
    void rejectsToWithoutFrom() {
        assertInvalidRequest(null, LocalDate.of(2026, 8, 1), null);
    }

    @Test
    void rejectsFromAfterTo() {
        assertInvalidRequest(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 1), null);
    }

    @Test
    void rejectsDateRangeCombinedWithTag() {
        assertInvalidRequest(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2), "망원동");
    }

    @Test
    void getsExperiencesByExactTag() {
        when(walkExperienceRepository.findAllActiveByUserIdAndTag(userId, "#망원동"))
                .thenReturn(List.of());

        walkExperienceService.getAll(userId, null, null, "#망원동");

        verify(walkExperienceRepository).findAllActiveByUserIdAndTag(userId, "#망원동");
    }

    @Test
    void preservesRepositoryOrderAndMapsOnlyListResponseFields() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        WalkExperience first = experience(firstId, Set.of(Emotion.PENSIVE, Emotion.CALM), "두번째", "첫번째");
        WalkExperience second = experience(secondId, Set.of(), new String[0]);
        when(walkExperienceRepository.findAllActiveByUserId(userId)).thenReturn(List.of(first, second));

        List<WalkExperienceListResponse> result = walkExperienceService.getAll(userId, null, null, null);

        assertThat(result).extracting(WalkExperienceListResponse::experienceId)
                .containsExactly(firstId, secondId);
        WalkExperienceListResponse response = result.getFirst();
        assertThat(response.photoUrl()).isEqualTo("https://example.com/photo.jpg");
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.startedAt()).isEqualTo(OffsetDateTime.parse("2026-08-11T13:00:00+09:00"));
        assertThat(response.durationSeconds()).isEqualTo(3600);
        assertThat(response.locationSummary()).isEqualTo("망원동");
        assertThat(response.companion()).isEqualTo(Companion.ALONE);
        assertThat(response.emotions()).containsExactly(Emotion.CALM, Emotion.PENSIVE);
        assertThat(response.situation()).isEqualTo(Situation.AFTERNOON);
        assertThat(response.tags()).containsExactly("두번째", "첫번째");
        assertThat(result.get(1).emotions()).isEmpty();
        assertThat(result.get(1).tags()).isEmpty();
    }

    private void assertInvalidRequest(LocalDate from, LocalDate to, String tag) {
        assertThatThrownBy(() -> walkExperienceService.getAll(userId, from, to, tag))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verifyNoInteractions(walkExperienceRepository);
    }

    private WalkExperience experience(UUID id, Set<Emotion> emotions, String... tags) {
        WalkExperience experience = mock(WalkExperience.class);
        when(experience.getId()).thenReturn(id);
        when(experience.getPhotoUrl()).thenReturn("https://example.com/photo.jpg");
        when(experience.getTitle()).thenReturn("제목");
        when(experience.getStartedAt()).thenReturn(OffsetDateTime.parse("2026-08-11T13:00:00+09:00"));
        when(experience.getDurationSeconds()).thenReturn(3600);
        when(experience.getLocationSummary()).thenReturn("망원동");
        when(experience.getCompanion()).thenReturn(Companion.ALONE);
        when(experience.getEmotions()).thenReturn(emotions);
        when(experience.getSituation()).thenReturn(Situation.AFTERNOON);
        when(experience.getTags()).thenReturn(new LinkedHashSet<>(List.of(tags)));
        return experience;
    }
}
