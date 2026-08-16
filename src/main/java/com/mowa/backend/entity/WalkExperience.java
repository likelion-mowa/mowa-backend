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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "walk_experiences",
        indexes = @Index(
                name = "idx_walk_experiences_user_demo_session_started_at",
                columnList = "user_id, demo_session_id, started_at"
        )
)
public class WalkExperience extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "demo_session_id", nullable = false)
    private UUID demoSessionId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_id", nullable = false, unique = true)
    private ExperienceDraft draft;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private OffsetDateTime endedAt;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "location_summary", length = 255)
    private String locationSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "companion", length = 30)
    private Companion companion;

    @Enumerated(EnumType.STRING)
    @Column(name = "situation", length = 30)
    private Situation situation;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "walk_experience_emotions",
            joinColumns = @JoinColumn(name = "experience_id", nullable = false)
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "emotion", nullable = false, length = 30)
    private Set<Emotion> emotions = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "walk_experience_tags",
            joinColumns = @JoinColumn(name = "experience_id", nullable = false)
    )
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new HashSet<>();

    protected WalkExperience() {
    }

    public static WalkExperience create(
            User user,
            ExperienceDraft draft,
            WalkCandidate candidate,
            String title,
            String body,
            String photoUrl,
            Companion companion,
            Set<Emotion> emotions,
            Situation situation,
            Set<String> tags
    ) {
        WalkExperience experience = new WalkExperience();
        experience.user = user;
        experience.demoSessionId = draft.getDemoSessionId();
        experience.draft = draft;
        experience.title = title;
        experience.body = body;
        experience.photoUrl = photoUrl;
        experience.startedAt = candidate.getDetectedStartAt();
        experience.endedAt = candidate.getDetectedEndAt();
        experience.durationSeconds = candidate.getDurationSeconds();
        experience.locationSummary = candidate.getLocationSummary();
        experience.companion = companion;
        experience.situation = situation;
        if (emotions != null) {
            experience.emotions.addAll(emotions);
        }
        if (tags != null) {
            experience.tags.addAll(tags);
        }
        return experience;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateBody(String body) {
        this.body = body;
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
        this.emotions.addAll(emotions);
    }

    public void replaceTags(Set<String> tags) {
        this.tags.clear();
        this.tags.addAll(tags);
    }

    public void softDelete(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public UUID getDemoSessionId() {
        return demoSessionId;
    }

    public ExperienceDraft getDraft() {
        return draft;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getEndedAt() {
        return endedAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public String getLocationSummary() {
        return locationSummary;
    }

    public Companion getCompanion() {
        return companion;
    }

    public Situation getSituation() {
        return situation;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public Set<Emotion> getEmotions() {
        return emotions;
    }

    public Set<String> getTags() {
        return tags;
    }
}
