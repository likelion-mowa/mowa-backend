package com.mowa.backend.service;

import com.mowa.backend.common.exception.BusinessException;
import com.mowa.backend.common.exception.ErrorCode;
import com.mowa.backend.dto.experiencedraft.ExperienceDraftAiGenerationResponse;
import com.mowa.backend.entity.AiGenerationStatus;
import com.mowa.backend.entity.ExperienceDraft;
import com.mowa.backend.repository.ExperienceDraftRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ExperienceDraftAiGenerationTransactionService {

    private final ExperienceDraftRepository experienceDraftRepository;

    ExperienceDraftAiGenerationTransactionService(ExperienceDraftRepository experienceDraftRepository) {
        this.experienceDraftRepository = experienceDraftRepository;
    }

    @Transactional
    public ExperienceDraftAiGenerationInput startGeneration(UUID userId, UUID demoSessionId, UUID draftId) {
        ExperienceDraft draft = experienceDraftRepository
                .findByIdAndUser_IdAndDemoSessionIdForUpdate(draftId, userId, demoSessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        validateGeneratableStatus(draft.getAiGenerationStatus());

        draft.startAiGeneration();
        return ExperienceDraftAiGenerationInput.from(draft);
    }

    @Transactional
    public ExperienceDraftAiGenerationResponse completeGeneration(
            UUID userId,
            UUID demoSessionId,
            UUID draftId,
            ExperienceDraftAiGenerationResult result
    ) {
        ExperienceDraft draft = findDraft(userId, demoSessionId, draftId);
        draft.completeAiGeneration(result.aiTitle(), result.aiBody());
        return new ExperienceDraftAiGenerationResponse(
                draft.getId(),
                draft.getAiTitle(),
                draft.getAiBody(),
                result.suggestedTags(),
                draft.getAiGenerationStatus()
        );
    }

    @Transactional
    public void failGeneration(UUID userId, UUID demoSessionId, UUID draftId) {
        experienceDraftRepository.findByIdAndUser_IdAndDemoSessionId(draftId, userId, demoSessionId)
                .ifPresent(ExperienceDraft::failAiGeneration);
    }

    private ExperienceDraft findDraft(UUID userId, UUID demoSessionId, UUID draftId) {
        return experienceDraftRepository.findByIdAndUser_IdAndDemoSessionId(draftId, userId, demoSessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private void validateGeneratableStatus(AiGenerationStatus status) {
        if (status != AiGenerationStatus.PENDING && status != AiGenerationStatus.FAILED) {
            throw new BusinessException(ErrorCode.CONFLICT, "AI generation cannot be requested in current status.");
        }
    }
}
