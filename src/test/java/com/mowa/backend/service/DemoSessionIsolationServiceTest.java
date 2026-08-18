package com.mowa.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mowa.backend.common.exception.BusinessException;
import com.mowa.backend.common.exception.ErrorCode;
import com.mowa.backend.dto.experiencedraft.UpdateExperienceDraftRequest;
import com.mowa.backend.dto.walkexperience.UpdateWalkExperienceRequest;
import com.mowa.backend.repository.ExperienceDraftRepository;
import com.mowa.backend.repository.UserRepository;
import com.mowa.backend.repository.WalkCandidateRepository;
import com.mowa.backend.repository.WalkExperienceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DemoSessionIsolationServiceTest {

    @Test
    void sessionBCannotReadSessionACandidateForSameUser() {
        WalkCandidateRepository walkCandidateRepository = mock(WalkCandidateRepository.class);
        WalkCandidateService service = new WalkCandidateService(
                walkCandidateRepository,
                mock(UserRepository.class)
        );
        UUID userId = UUID.randomUUID();
        UUID sessionB = UUID.randomUUID();
        UUID candidateA = UUID.randomUUID();
        when(walkCandidateRepository.findByIdAndUser_IdAndDemoSessionId(candidateA, userId, sessionB))
                .thenReturn(Optional.empty());

        assertNotFound(() -> service.get(userId, sessionB, candidateA));
        verify(walkCandidateRepository).findByIdAndUser_IdAndDemoSessionId(candidateA, userId, sessionB);
    }

    @Test
    void sessionBCannotAccessSessionADraftForUpdateOrAiGenerationForSameUser() {
        ExperienceDraftRepository draftRepository = mock(ExperienceDraftRepository.class);
        UUID userId = UUID.randomUUID();
        UUID sessionB = UUID.randomUUID();
        UUID draftA = UUID.randomUUID();
        ExperienceDraftService draftService = new ExperienceDraftService(
                draftRepository,
                mock(WalkCandidateRepository.class),
                mock(ExperienceDraftAiGenerationTransactionService.class),
                mock(ExperienceDraftAiClient.class)
        );
        ExperienceDraftAiGenerationTransactionService aiTransactionService =
                new ExperienceDraftAiGenerationTransactionService(draftRepository);
        when(draftRepository.findByIdAndUser_IdAndDemoSessionId(draftA, userId, sessionB))
                .thenReturn(Optional.empty());
        when(draftRepository.findByIdAndUser_IdAndDemoSessionIdForUpdate(draftA, userId, sessionB))
                .thenReturn(Optional.empty());

        assertNotFound(() -> draftService.update(userId, sessionB, draftA, new UpdateExperienceDraftRequest()));
        assertNotFound(() -> aiTransactionService.startGeneration(userId, sessionB, draftA));
    }

    @Test
    void sessionBCannotCreateExperienceFromSessionADraftForSameUser() {
        ExperienceDraftRepository draftRepository = mock(ExperienceDraftRepository.class);
        WalkExperienceService service = new WalkExperienceService(
                draftRepository,
                mock(WalkExperienceRepository.class),
                mock(ExperienceDraftAiClient.class)
        );
        UUID userId = UUID.randomUUID();
        UUID sessionB = UUID.randomUUID();
        UUID draftA = UUID.randomUUID();
        when(draftRepository.findByIdAndUser_IdAndDemoSessionIdForUpdate(draftA, userId, sessionB))
                .thenReturn(Optional.empty());

        assertNotFound(() -> service.create(
                userId,
                sessionB,
                TestWalkExperienceRequests.validCreateRequest(draftA)
        ));
    }

    @Test
    void sessionBCannotListReadUpdateOrDeleteSessionAExperienceForSameUser() {
        WalkExperienceRepository experienceRepository = mock(WalkExperienceRepository.class);
        WalkExperienceService service = new WalkExperienceService(
                mock(ExperienceDraftRepository.class),
                experienceRepository,
                mock(ExperienceDraftAiClient.class)
        );
        UUID userId = UUID.randomUUID();
        UUID sessionB = UUID.randomUUID();
        UUID experienceA = UUID.randomUUID();
        when(experienceRepository.findAllActiveByUserIdAndDemoSessionId(userId, sessionB))
                .thenReturn(List.of());
        when(experienceRepository.findActiveByIdAndUserIdAndDemoSessionId(experienceA, userId, sessionB))
                .thenReturn(Optional.empty());

        assertThat(service.getAll(userId, sessionB, null, null, null)).isEmpty();
        assertNotFound(() -> service.get(userId, sessionB, experienceA));
        assertNotFound(() -> service.update(userId, sessionB, experienceA, new UpdateWalkExperienceRequest()));
        assertNotFound(() -> service.delete(userId, sessionB, experienceA));
        verify(experienceRepository).findAllActiveByUserIdAndDemoSessionId(userId, sessionB);
    }

    private void assertNotFound(ThrowingCall call) {
        assertThatThrownBy(call::execute)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void execute();
    }
}
