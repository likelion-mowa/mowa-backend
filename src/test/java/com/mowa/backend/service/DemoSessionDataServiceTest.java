package com.mowa.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mowa.backend.entity.AiGenerationStatus;
import com.mowa.backend.entity.CandidateStatus;
import com.mowa.backend.entity.Companion;
import com.mowa.backend.entity.Emotion;
import com.mowa.backend.entity.ExperienceDraft;
import com.mowa.backend.entity.Situation;
import com.mowa.backend.entity.User;
import com.mowa.backend.entity.WalkCandidate;
import com.mowa.backend.entity.WalkExperience;
import com.mowa.backend.repository.ExperienceDraftRepository;
import com.mowa.backend.repository.WalkCandidateRepository;
import com.mowa.backend.repository.WalkExperienceRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class DemoSessionDataServiceTest {

    private WalkCandidateRepository walkCandidateRepository;
    private ExperienceDraftRepository experienceDraftRepository;
    private WalkExperienceRepository walkExperienceRepository;
    private DemoSessionDataService service;

    @BeforeEach
    void setUp() {
        walkCandidateRepository = mock(WalkCandidateRepository.class);
        experienceDraftRepository = mock(ExperienceDraftRepository.class);
        walkExperienceRepository = mock(WalkExperienceRepository.class);
        service = new DemoSessionDataService(
                walkCandidateRepository,
                experienceDraftRepository,
                walkExperienceRepository
        );
    }

    @Test
    void supportsOnlyMowaDemoLoginId() {
        assertThat(service.supports(user(UUID.randomUUID(), "mowa01"))).isTrue();
        assertThat(service.supports(user(UUID.randomUUID(), "normal01"))).isFalse();
    }

    @Test
    void copiesTemplateCandidateDraftExperienceCollectionsAndRelationshipsToNewSession() {
        UUID userId = UUID.randomUUID();
        UUID newSessionId = UUID.randomUUID();
        User user = user(userId, "mowa01");
        WalkExperience template = templateExperience(user);

        when(walkCandidateRepository.existsByUser_IdAndDemoSessionId(userId, newSessionId)).thenReturn(false);
        when(walkExperienceRepository.existsByUser_IdAndDemoSessionId(userId, newSessionId)).thenReturn(false);
        when(walkExperienceRepository.findAllActiveTemplatesByUserIdAndDemoSessionId(
                userId,
                DemoSessionDataService.DEMO_TEMPLATE_SESSION_ID
        )).thenReturn(List.of(template));
        when(walkCandidateRepository.save(any(WalkCandidate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(experienceDraftRepository.save(any(ExperienceDraft.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(walkExperienceRepository.save(any(WalkExperience.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.initializeDefaultDataIfDemoUser(user, newSessionId);

        ArgumentCaptor<WalkCandidate> candidateCaptor = ArgumentCaptor.forClass(WalkCandidate.class);
        ArgumentCaptor<ExperienceDraft> draftCaptor = ArgumentCaptor.forClass(ExperienceDraft.class);
        ArgumentCaptor<WalkExperience> experienceCaptor = ArgumentCaptor.forClass(WalkExperience.class);
        verify(walkCandidateRepository).save(candidateCaptor.capture());
        verify(experienceDraftRepository).save(draftCaptor.capture());
        verify(walkExperienceRepository).save(experienceCaptor.capture());

        WalkCandidate copiedCandidate = candidateCaptor.getValue();
        ExperienceDraft copiedDraft = draftCaptor.getValue();
        WalkExperience copiedExperience = experienceCaptor.getValue();
        ExperienceDraft templateDraft = template.getDraft();
        WalkCandidate templateCandidate = templateDraft.getCandidate();

        assertThat(copiedCandidate).isNotSameAs(templateCandidate);
        assertThat(copiedDraft).isNotSameAs(templateDraft);
        assertThat(copiedExperience).isNotSameAs(template);

        assertThat(copiedCandidate.getUser()).isSameAs(user);
        assertThat(copiedDraft.getUser()).isSameAs(user);
        assertThat(copiedExperience.getUser()).isSameAs(user);
        assertThat(copiedCandidate.getDemoSessionId()).isEqualTo(newSessionId);
        assertThat(copiedDraft.getDemoSessionId()).isEqualTo(newSessionId);
        assertThat(copiedExperience.getDemoSessionId()).isEqualTo(newSessionId);

        assertThat(copiedDraft.getCandidate()).isSameAs(copiedCandidate);
        assertThat(copiedExperience.getDraft()).isSameAs(copiedDraft);
        assertThat(copiedExperience.getDraft()).isNotSameAs(templateDraft);
        assertThat(copiedDraft.getCandidate()).isNotSameAs(templateCandidate);

        assertThat(copiedCandidate.getDetectedStartAt()).isEqualTo(templateCandidate.getDetectedStartAt());
        assertThat(copiedCandidate.getDetectedEndAt()).isEqualTo(templateCandidate.getDetectedEndAt());
        assertThat(copiedCandidate.getDurationSeconds()).isEqualTo(templateCandidate.getDurationSeconds());
        assertThat(copiedCandidate.getLocationSummary()).isEqualTo(templateCandidate.getLocationSummary());
        assertThat(copiedCandidate.getStatus()).isEqualTo(templateCandidate.getStatus());

        assertThat(copiedDraft.getPhotoUrl()).isEqualTo(templateDraft.getPhotoUrl());
        assertThat(copiedDraft.getCompanion()).isEqualTo(templateDraft.getCompanion());
        assertThat(copiedDraft.getSituation()).isEqualTo(templateDraft.getSituation());
        assertThat(copiedDraft.getAiTitle()).isEqualTo(templateDraft.getAiTitle());
        assertThat(copiedDraft.getAiBody()).isEqualTo(templateDraft.getAiBody());
        assertThat(copiedDraft.getAiGenerationStatus()).isEqualTo(AiGenerationStatus.SUCCESS);
        assertThat(copiedDraft.getEmotions()).containsExactlyInAnyOrderElementsOf(templateDraft.getEmotions());
        assertThat(copiedDraft.getEmotions()).isNotSameAs(templateDraft.getEmotions());

        assertThat(copiedExperience.getTitle()).isEqualTo(template.getTitle());
        assertThat(copiedExperience.getBody()).isEqualTo(template.getBody());
        assertThat(copiedExperience.getPhotoUrl()).isEqualTo(template.getPhotoUrl());
        assertThat(copiedExperience.getStartedAt()).isEqualTo(template.getStartedAt());
        assertThat(copiedExperience.getEndedAt()).isEqualTo(template.getEndedAt());
        assertThat(copiedExperience.getDurationSeconds()).isEqualTo(template.getDurationSeconds());
        assertThat(copiedExperience.getLocationSummary()).isEqualTo(template.getLocationSummary());
        assertThat(copiedExperience.getCompanion()).isEqualTo(template.getCompanion());
        assertThat(copiedExperience.getSituation()).isEqualTo(template.getSituation());
        assertThat(copiedExperience.getEmotions()).containsExactlyInAnyOrderElementsOf(template.getEmotions());
        assertThat(copiedExperience.getTags()).containsExactlyInAnyOrderElementsOf(template.getTags());
        assertThat(copiedExperience.getEmotions()).isNotSameAs(template.getEmotions());
        assertThat(copiedExperience.getTags()).isNotSameAs(template.getTags());
    }

    @Test
    void eachInitializationCreatesIndependentCopiesForDifferentSessions() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "mowa01");
        WalkExperience template = templateExperience(user);
        UUID firstSession = UUID.randomUUID();
        UUID secondSession = UUID.randomUUID();

        when(walkCandidateRepository.existsByUser_IdAndDemoSessionId(eq(userId), any(UUID.class))).thenReturn(false);
        when(walkExperienceRepository.existsByUser_IdAndDemoSessionId(eq(userId), any(UUID.class))).thenReturn(false);
        when(walkExperienceRepository.findAllActiveTemplatesByUserIdAndDemoSessionId(
                userId,
                DemoSessionDataService.DEMO_TEMPLATE_SESSION_ID
        )).thenReturn(List.of(template));
        when(walkCandidateRepository.save(any(WalkCandidate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(experienceDraftRepository.save(any(ExperienceDraft.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(walkExperienceRepository.save(any(WalkExperience.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.initializeDefaultDataIfDemoUser(user, firstSession);
        service.initializeDefaultDataIfDemoUser(user, secondSession);

        ArgumentCaptor<WalkExperience> experiences = ArgumentCaptor.forClass(WalkExperience.class);
        verify(walkExperienceRepository, org.mockito.Mockito.times(2)).save(experiences.capture());
        assertThat(experiences.getAllValues().get(0).getDemoSessionId()).isEqualTo(firstSession);
        assertThat(experiences.getAllValues().get(1).getDemoSessionId()).isEqualTo(secondSession);
        assertThat(experiences.getAllValues().get(0)).isNotSameAs(experiences.getAllValues().get(1));
        assertThat(experiences.getAllValues().get(0).getDraft())
                .isNotSameAs(experiences.getAllValues().get(1).getDraft());
    }

    @Test
    void throwsWhenDemoTemplateDataIsMissing() {
        UUID userId = UUID.randomUUID();
        UUID newSessionId = UUID.randomUUID();
        User user = user(userId, "mowa01");

        when(walkCandidateRepository.existsByUser_IdAndDemoSessionId(userId, newSessionId)).thenReturn(false);
        when(walkExperienceRepository.existsByUser_IdAndDemoSessionId(userId, newSessionId)).thenReturn(false);
        when(walkExperienceRepository.findAllActiveTemplatesByUserIdAndDemoSessionId(
                userId,
                DemoSessionDataService.DEMO_TEMPLATE_SESSION_ID
        )).thenReturn(List.of());

        assertThatThrownBy(() -> service.initializeDefaultDataIfDemoUser(user, newSessionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Demo template data is not initialized");

        verify(walkCandidateRepository, never()).save(any(WalkCandidate.class));
        verify(experienceDraftRepository, never()).save(any(ExperienceDraft.class));
        verify(walkExperienceRepository, never()).save(any(WalkExperience.class));
    }

    @Test
    void skipsWhenNewSessionAlreadyHasData() {
        UUID userId = UUID.randomUUID();
        UUID newSessionId = UUID.randomUUID();
        User user = user(userId, "mowa01");
        when(walkCandidateRepository.existsByUser_IdAndDemoSessionId(userId, newSessionId)).thenReturn(true);

        service.initializeDefaultDataIfDemoUser(user, newSessionId);

        verify(walkExperienceRepository, never())
                .findAllActiveTemplatesByUserIdAndDemoSessionId(any(UUID.class), any(UUID.class));
        verify(walkCandidateRepository, never()).save(any(WalkCandidate.class));
        verify(experienceDraftRepository, never()).save(any(ExperienceDraft.class));
        verify(walkExperienceRepository, never()).save(any(WalkExperience.class));
    }

    private WalkExperience templateExperience(User user) {
        WalkCandidate candidate = WalkCandidate.create(
                user,
                DemoSessionDataService.DEMO_TEMPLATE_SESSION_ID,
                OffsetDateTime.parse("2026-08-12T13:00:00+09:00"),
                "Seoul Forest"
        );
        candidate.updateDetectedEndAt(OffsetDateTime.parse("2026-08-12T14:00:00+09:00"));
        candidate.updateDurationSeconds(3600);
        candidate.updateStatus(CandidateStatus.RECORDING);
        ReflectionTestUtils.setField(candidate, "id", UUID.randomUUID());

        ExperienceDraft draft = ExperienceDraft.create(
                user,
                candidate,
                "https://res.cloudinary.com/demo/image/upload/sample.jpg",
                Companion.ALONE,
                Set.of(Emotion.CALM, Emotion.HAPPY),
                Situation.AFTERNOON
        );
        draft.completeAiGeneration("AI title", "AI body");
        ReflectionTestUtils.setField(draft, "id", UUID.randomUUID());

        WalkExperience experience = WalkExperience.create(
                user,
                draft,
                candidate,
                "Final title",
                "Final body",
                draft.getPhotoUrl(),
                draft.getCompanion(),
                Set.of(Emotion.CALM),
                draft.getSituation(),
                Set.of("forest", "demo")
        );
        ReflectionTestUtils.setField(experience, "id", UUID.randomUUID());
        return experience;
    }

    private User user(UUID id, String loginId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getLoginId()).thenReturn(loginId);
        return user;
    }
}
