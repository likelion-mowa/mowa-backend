package com.mowa.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mowa.backend.common.exception.BusinessException;
import com.mowa.backend.common.exception.ErrorCode;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WalkExperienceDeleteServiceTest {

    private WalkExperienceRepository walkExperienceRepository;
    private ExperienceDraftRepository experienceDraftRepository;
    private WalkExperienceService service;
    private UUID userId;
    private UUID demoSessionId;
    private UUID experienceId;

    @BeforeEach
    void setUp() {
        walkExperienceRepository = mock(WalkExperienceRepository.class);
        experienceDraftRepository = mock(ExperienceDraftRepository.class);
        service = new WalkExperienceService(
                experienceDraftRepository,
                walkExperienceRepository,
                mock(ExperienceDraftAiClient.class)
        );
        userId = UUID.randomUUID();
        demoSessionId = UUID.randomUUID();
        experienceId = UUID.randomUUID();
    }

    @Test
    void softDeletesOwnedActiveExperienceAndPreservesAllOtherData() {
        User user = mock(User.class);
        ExperienceDraft draft = mock(ExperienceDraft.class);
        WalkCandidate candidate = candidate();
        WalkExperience experience = WalkExperience.create(
                user,
                draft,
                candidate,
                "title",
                "body",
                "photo",
                Companion.ALONE,
                Set.of(Emotion.CALM),
                Situation.MORNING,
                Set.of("tag")
        );
        when(walkExperienceRepository.findActiveByIdAndUserIdAndDemoSessionId(experienceId, userId, demoSessionId))
                .thenReturn(Optional.of(experience));
        OffsetDateTime before = OffsetDateTime.now();

        service.delete(userId, demoSessionId, experienceId);

        OffsetDateTime after = OffsetDateTime.now();
        verify(walkExperienceRepository).findActiveByIdAndUserIdAndDemoSessionId(
                experienceId,
                userId,
                demoSessionId
        );
        assertThat(experience.getDeletedAt()).isBetween(before, after);
        assertThat(experience.getUser()).isSameAs(user);
        assertThat(experience.getDraft()).isSameAs(draft);
        assertThat(experience.getTitle()).isEqualTo("title");
        assertThat(experience.getBody()).isEqualTo("body");
        assertThat(experience.getPhotoUrl()).isEqualTo("photo");
        assertThat(experience.getStartedAt()).isEqualTo(candidate.getDetectedStartAt());
        assertThat(experience.getEndedAt()).isEqualTo(candidate.getDetectedEndAt());
        assertThat(experience.getDurationSeconds()).isEqualTo(candidate.getDurationSeconds());
        assertThat(experience.getLocationSummary()).isEqualTo(candidate.getLocationSummary());
        assertThat(experience.getCompanion()).isEqualTo(Companion.ALONE);
        assertThat(experience.getEmotions()).containsExactly(Emotion.CALM);
        assertThat(experience.getSituation()).isEqualTo(Situation.MORNING);
        assertThat(experience.getTags()).containsExactly("tag");
        verify(walkExperienceRepository, never()).delete(any(WalkExperience.class));
        verify(walkExperienceRepository, never()).deleteById(any(UUID.class));
        verify(walkExperienceRepository, never()).save(any(WalkExperience.class));
        verifyNoInteractions(experienceDraftRepository);
    }

    @Test
    void missingExperienceIsNotFound() {
        assertNotFoundForEmptyRepositoryResult();
    }

    @Test
    void otherUsersExperienceIsNotFound() {
        assertNotFoundForEmptyRepositoryResult();
    }

    @Test
    void alreadySoftDeletedExperienceIsNotFound() {
        assertNotFoundForEmptyRepositoryResult();
    }

    @Test
    void entityOnlyExposesSoftDeleteAndNoRestoreOrHardDeleteOperation() {
        assertThat(WalkExperience.class.getDeclaredMethods())
                .extracting(method -> method.getName())
                .contains("softDelete")
                .doesNotContain("restore", "hardDelete");
    }

    private void assertNotFoundForEmptyRepositoryResult() {
        when(walkExperienceRepository.findActiveByIdAndUserIdAndDemoSessionId(experienceId, userId, demoSessionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(userId, demoSessionId, experienceId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(walkExperienceRepository, never()).delete(any(WalkExperience.class));
        verify(walkExperienceRepository, never()).deleteById(any(UUID.class));
        verify(walkExperienceRepository, never()).save(any(WalkExperience.class));
        verifyNoInteractions(experienceDraftRepository);
    }

    private WalkCandidate candidate() {
        WalkCandidate candidate = mock(WalkCandidate.class);
        when(candidate.getDetectedStartAt()).thenReturn(OffsetDateTime.parse("2026-08-15T10:00:00+09:00"));
        when(candidate.getDetectedEndAt()).thenReturn(OffsetDateTime.parse("2026-08-15T11:00:00+09:00"));
        when(candidate.getDurationSeconds()).thenReturn(3600);
        when(candidate.getLocationSummary()).thenReturn("location");
        return candidate;
    }
}
