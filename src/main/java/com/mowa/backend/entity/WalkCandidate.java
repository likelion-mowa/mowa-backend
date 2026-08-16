package com.mowa.backend.entity;

import com.mowa.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "walk_candidates")
public class WalkCandidate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "demo_session_id", nullable = false)
    private UUID demoSessionId;

    @Column(name = "detected_start_at", nullable = false)
    private OffsetDateTime detectedStartAt;

    @Column(name = "detected_end_at")
    private OffsetDateTime detectedEndAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "location_summary", length = 255)
    private String locationSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CandidateStatus status = CandidateStatus.DETECTED;

    protected WalkCandidate() {
    }

    public static WalkCandidate create(
            User user,
            UUID demoSessionId,
            OffsetDateTime detectedStartAt,
            String locationSummary
    ) {
        WalkCandidate candidate = new WalkCandidate();
        candidate.user = user;
        candidate.demoSessionId = demoSessionId;
        candidate.detectedStartAt = detectedStartAt;
        candidate.locationSummary = locationSummary;
        candidate.status = CandidateStatus.DETECTED;
        return candidate;
    }

    public void updateDetectedEndAt(OffsetDateTime detectedEndAt) {
        this.detectedEndAt = detectedEndAt;
    }

    public void updateDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public void updateLocationSummary(String locationSummary) {
        this.locationSummary = locationSummary;
    }

    public void updateStatus(CandidateStatus status) {
        this.status = status;
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

    public OffsetDateTime getDetectedStartAt() {
        return detectedStartAt;
    }

    public OffsetDateTime getDetectedEndAt() {
        return detectedEndAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public String getLocationSummary() {
        return locationSummary;
    }

    public CandidateStatus getStatus() {
        return status;
    }
}
