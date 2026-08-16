package com.mowa.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mowa.backend.common.exception.BusinessException;
import com.mowa.backend.common.exception.ErrorCode;
import com.mowa.backend.dto.walkcandidate.CreateWalkCandidateRequest;
import com.mowa.backend.entity.User;
import com.mowa.backend.entity.WalkCandidate;
import com.mowa.backend.repository.UserRepository;
import com.mowa.backend.repository.WalkCandidateRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WalkCandidateDemoSessionServiceTest {

    @Test
    void createStoresCurrentDemoSessionFromAuthentication() {
        WalkCandidateRepository walkCandidateRepository = mock(WalkCandidateRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        WalkCandidateService service = new WalkCandidateService(walkCandidateRepository, userRepository);
        UUID userId = UUID.randomUUID();
        UUID demoSessionId = UUID.randomUUID();
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(walkCandidateRepository.save(any(WalkCandidate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(
                userId,
                demoSessionId,
                new CreateWalkCandidateRequest(OffsetDateTime.parse("2026-08-15T10:00:00+09:00"), "park")
        );

        ArgumentCaptor<WalkCandidate> captor = ArgumentCaptor.forClass(WalkCandidate.class);
        verify(walkCandidateRepository).save(captor.capture());
        assertThat(captor.getValue().getDemoSessionId()).isEqualTo(demoSessionId);
    }

    @Test
    void getUsesUserIdAndDemoSessionIdForOwnership() {
        WalkCandidateRepository walkCandidateRepository = mock(WalkCandidateRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        WalkCandidateService service = new WalkCandidateService(walkCandidateRepository, userRepository);
        UUID userId = UUID.randomUUID();
        UUID demoSessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        when(walkCandidateRepository.findByIdAndUser_IdAndDemoSessionId(candidateId, userId, demoSessionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(userId, demoSessionId, candidateId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }
}
