package com.mowa.backend.entity;

import com.mowa.backend.common.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "experience_drafts")
public class ExperienceDraft extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private WalkCandidate candidate;

    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "companion", length = 30)
    private Companion companion;

    @Enumerated(EnumType.STRING)
    @Column(name = "situation", length = 30)
    private Situation situation;

    @Column(name = "ai_title", length = 100)
    private String aiTitle;

    @Column(name = "ai_body", columnDefinition = "TEXT")
    private String aiBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_generation_status", nullable = false, length = 20)
    private AiGenerationStatus aiGenerationStatus = AiGenerationStatus.PENDING;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "experience_draft_emotions",
            joinColumns = @JoinColumn(name = "draft_id", nullable = false)
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "emotion", nullable = false, length = 30)
    private Set<Emotion> emotions = new HashSet<>();

    protected ExperienceDraft() {
    }

    public static ExperienceDraft create(
            User user,
            WalkCandidate candidate,
            String photoUrl,
            Companion companion,
            Set<Emotion> emotions,
            Situation situation
    ) {
        ExperienceDraft draft = new ExperienceDraft();
        draft.user = user;
        draft.candidate = candidate;
        draft.photoUrl = photoUrl;
        draft.companion = companion;
        draft.situation = situation;
        draft.aiGenerationStatus = AiGenerationStatus.PENDING;
        draft.replaceEmotions(emotions);
        return draft;
    }

    public void updatePhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void updateCompanion(Companion companion) {
        this.companion = companion;
    }

    public void updateSituation(Situation situation) {
        this.situation = situation;
    }

    public void replaceEmotions(Set<Emotion> emotions) {
        this.emotions.clear();
        if (emotions != null) {
            this.emotions.addAll(emotions);
        }
    }

    public void startAiGeneration() {
        this.aiGenerationStatus = AiGenerationStatus.GENERATING;
    }

    public void completeAiGeneration(String aiTitle, String aiBody) {
        this.aiTitle = aiTitle;
        this.aiBody = aiBody;
        this.aiGenerationStatus = AiGenerationStatus.SUCCESS;
    }

    public void failAiGeneration() {
        this.aiGenerationStatus = AiGenerationStatus.FAILED;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public WalkCandidate getCandidate() {
        return candidate;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public Companion getCompanion() {
        return companion;
    }

    public Situation getSituation() {
        return situation;
    }

    public String getAiTitle() {
        return aiTitle;
    }

    public String getAiBody() {
        return aiBody;
    }

    public AiGenerationStatus getAiGenerationStatus() {
        return aiGenerationStatus;
    }

    public Set<Emotion> getEmotions() {
        return emotions;
    }
}
