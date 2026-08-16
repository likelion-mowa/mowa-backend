package com.mowa.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mowa.backend.common.exception.BusinessException;
import com.mowa.backend.common.exception.ErrorCode;
import com.mowa.backend.repository.ExperienceDraftRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExperienceDraftAiGenerationDemoSessionTest {

    @Test
    void startGenerationWithOtherSessionDraftReturnsNotFound() {
        ExperienceDraftRepository repository = mock(ExperienceDraftRepository.class);
        ExperienceDraftAiGenerationTransactionService service =
                new ExperienceDraftAiGenerationTransactionService(repository);
        UUID userId = UUID.randomUUID();
        UUID demoSessionId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        when(repository.findByIdAndUser_IdAndDemoSessionIdForUpdate(draftId, userId, demoSessionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startGeneration(userId, demoSessionId, draftId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }
}
