package com.mowa.backend.service;

import com.mowa.backend.entity.ExperienceDraft;
import com.mowa.backend.entity.User;
import com.mowa.backend.entity.WalkCandidate;
import com.mowa.backend.entity.WalkExperience;
import com.mowa.backend.repository.ExperienceDraftRepository;
import com.mowa.backend.repository.WalkCandidateRepository;
import com.mowa.backend.repository.WalkExperienceRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoSessionDataService {

    static final String DEMO_LOGIN_ID = "mowa01";
    static final UUID DEMO_TEMPLATE_SESSION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final WalkCandidateRepository walkCandidateRepository;
    private final ExperienceDraftRepository experienceDraftRepository;
    private final WalkExperienceRepository walkExperienceRepository;

    public DemoSessionDataService(
            WalkCandidateRepository walkCandidateRepository,
            ExperienceDraftRepository experienceDraftRepository,
            WalkExperienceRepository walkExperienceRepository
    ) {
        this.walkCandidateRepository = walkCandidateRepository;
        this.experienceDraftRepository = experienceDraftRepository;
        this.walkExperienceRepository = walkExperienceRepository;
    }

    public boolean supports(User user) {
        return DEMO_LOGIN_ID.equals(user.getLoginId());
    }

    @Transactional
    public void initializeDefaultDataIfDemoUser(User user, UUID newDemoSessionId) {
        if (!supports(user)) {
            return;
        }
        if (walkCandidateRepository.existsByUser_IdAndDemoSessionId(user.getId(), newDemoSessionId)
                || walkExperienceRepository.existsByUser_IdAndDemoSessionId(user.getId(), newDemoSessionId)) {
            return;
        }

        List<WalkExperience> templates = walkExperienceRepository.findAllActiveTemplatesByUserIdAndDemoSessionId(
                user.getId(),
                DEMO_TEMPLATE_SESSION_ID
        );
        if (templates.isEmpty()) {
            throw new IllegalStateException("Demo template data is not initialized");
        }

        Map<UUID, WalkCandidate> copiedCandidates = new HashMap<>();
        Map<UUID, ExperienceDraft> copiedDrafts = new HashMap<>();

        for (WalkExperience templateExperience : templates) {
            ExperienceDraft templateDraft = templateExperience.getDraft();
            WalkCandidate templateCandidate = templateDraft.getCandidate();

            WalkCandidate copiedCandidate = copiedCandidates.computeIfAbsent(
                    templateCandidate.getId(),
                    ignored -> copyCandidate(user, newDemoSessionId, templateCandidate)
            );
            ExperienceDraft copiedDraft = copiedDrafts.computeIfAbsent(
                    templateDraft.getId(),
                    ignored -> copyDraft(user, copiedCandidate, templateDraft)
            );

            walkExperienceRepository.save(copyExperience(user, copiedDraft, copiedCandidate, templateExperience));
        }
    }

    private WalkCandidate copyCandidate(User user, UUID newDemoSessionId, WalkCandidate source) {
        WalkCandidate copied = WalkCandidate.create(
                user,
                newDemoSessionId,
                source.getDetectedStartAt(),
                source.getLocationSummary()
        );
        copied.updateDetectedEndAt(source.getDetectedEndAt());
        copied.updateDurationSeconds(source.getDurationSeconds());
        copied.updateStatus(source.getStatus());
        return walkCandidateRepository.save(copied);
    }

    private ExperienceDraft copyDraft(User user, WalkCandidate copiedCandidate, ExperienceDraft source) {
        ExperienceDraft copied = ExperienceDraft.create(
                user,
                copiedCandidate,
                source.getPhotoUrl(),
                source.getCompanion(),
                new HashSet<>(source.getEmotions()),
                source.getSituation()
        );
        copied.copyAiGeneration(source.getAiTitle(), source.getAiBody(), source.getAiGenerationStatus());
        return experienceDraftRepository.save(copied);
    }

    private WalkExperience copyExperience(
            User user,
            ExperienceDraft copiedDraft,
            WalkCandidate copiedCandidate,
            WalkExperience source
    ) {
        return WalkExperience.create(
                user,
                copiedDraft,
                copiedCandidate,
                source.getTitle(),
                source.getBody(),
                source.getPhotoUrl(),
                source.getCompanion(),
                new HashSet<>(source.getEmotions()),
                source.getSituation(),
                new HashSet<>(source.getTags())
        );
    }
}
