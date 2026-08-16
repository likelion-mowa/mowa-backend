package com.mowa.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mowa.backend.common.exception.BusinessException;
import com.mowa.backend.common.exception.ErrorCode;
import com.mowa.backend.dto.experiencedraft.CreateExperienceDraftRequest;
import com.mowa.backend.entity.CandidateStatus;
import com.mowa.backend.entity.ExperienceDraft;
import com.mowa.backend.entity.User;
import com.mowa.backend.entity.WalkCandidate;
import com.mowa.backend.repository.ExperienceDraftRepository;
import com.mowa.backend.repository.WalkCandidateRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ExperienceDraftDemoSessionServiceTest {

    @Test
    void createCopiesDemoSessionFromCandidate() {
        ExperienceDraftRepository experienceDraftRepository = mock(ExperienceDraftRepository.class);
        WalkCandidateRepository walkCandidateRepository = mock(WalkCandidateRepository.class);
        ExperienceDraftService service = new ExperienceDraftService(
                experienceDraftRepository,
                walkCandidateRepository,
                mock(ExperienceDraftAiGenerationTransactionService.class),
                mock(ExperienceDraftAiClient.class)
        );
        UUID userId = UUID.randomUUID();
        UUID demoSessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        WalkCandidate candidate = mock(WalkCandidate.class);
        User user = mock(User.class);
        when(candidate.getUser()).thenReturn(user);
        when(candidate.getDemoSessionId()).thenReturn(demoSessionId);
        when(candidate.getStatus()).thenReturn(CandidateStatus.RECORDING);
        when(walkCandidateRepository.findByIdAndUser_IdAndDemoSessionId(candidateId, userId, demoSessionId))
                .thenReturn(Optional.of(candidate));
        when(experienceDraftRepository.existsByCandidate_Id(candidateId)).thenReturn(false);
        when(experienceDraftRepository.saveAndFlush(any(ExperienceDraft.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(userId, demoSessionId, candidateId, new CreateExperienceDraftRequest(
                "https://example.com/photo.jpg",
                null,
                null,
                null
        ));

        ArgumentCaptor<ExperienceDraft> captor = ArgumentCaptor.forClass(ExperienceDraft.class);
        verify(experienceDraftRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getDemoSessionId()).isEqualTo(demoSessionId);
    }

    @Test
    void createWithOtherSessionCandidateReturnsNotFound() {
        ExperienceDraftRepository experienceDraftRepository = mock(ExperienceDraftRepository.class);
        WalkCandidateRepository walkCandidateRepository = mock(WalkCandidateRepository.class);
        ExperienceDraftService service = new ExperienceDraftService(
                experienceDraftRepository,
                walkCandidateRepository,
                mock(ExperienceDraftAiGenerationTransactionService.class),
                mock(ExperienceDraftAiClient.class)
        );
        UUID userId = UUID.randomUUID();
        UUID demoSessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        when(walkCandidateRepository.findByIdAndUser_IdAndDemoSessionId(candidateId, userId, demoSessionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                userId,
                demoSessionId,
                candidateId,
                new CreateExperienceDraftRequest(null, null, null, null)
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }
}
