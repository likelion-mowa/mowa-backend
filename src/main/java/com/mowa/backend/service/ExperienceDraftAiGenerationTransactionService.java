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
    public ExperienceDraftAiGenerationInput startGeneration(UUID userId, UUID draftId) {
        ExperienceDraft draft = experienceDraftRepository.findByIdAndUser_IdForUpdate(draftId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        validateGeneratableStatus(draft.getAiGenerationStatus());

        draft.startAiGeneration();
        return ExperienceDraftAiGenerationInput.from(draft);
    }

    @Transactional
    public ExperienceDraftAiGenerationResponse completeGeneration(
            UUID userId,
            UUID draftId,
            ExperienceDraftAiGenerationResult result
    ) {
        ExperienceDraft draft = findDraft(userId, draftId);
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
    public void failGeneration(UUID userId, UUID draftId) {
        experienceDraftRepository.findByIdAndUser_Id(draftId, userId)
                .ifPresent(ExperienceDraft::failAiGeneration);
    }

    private ExperienceDraft findDraft(UUID userId, UUID draftId) {
        return experienceDraftRepository.findByIdAndUser_Id(draftId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private void validateGeneratableStatus(AiGenerationStatus status) {
        if (status != AiGenerationStatus.PENDING && status != AiGenerationStatus.FAILED) {
            throw new BusinessException(ErrorCode.CONFLICT, "AI generation cannot be requested in current status.");
        }
    }
}
