package com.mowa.backend.dto.walkcandidate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mowa.backend.entity.CandidateStatus;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

public class UpdateWalkCandidateRequest {

    private final Set<String> updatedFields = new HashSet<>();

    private OffsetDateTime detectedEndAt;

    @PositiveOrZero
    private Integer durationSeconds;

    @Size(max = 255)
    private String locationSummary;

    private CandidateStatus status;

    public OffsetDateTime getDetectedEndAt() {
        return detectedEndAt;
    }

    public void setDetectedEndAt(OffsetDateTime detectedEndAt) {
        updatedFields.add("detectedEndAt");
        this.detectedEndAt = detectedEndAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        updatedFields.add("durationSeconds");
        this.durationSeconds = durationSeconds;
    }

    public String getLocationSummary() {
        return locationSummary;
    }

    public void setLocationSummary(String locationSummary) {
        updatedFields.add("locationSummary");
        this.locationSummary = locationSummary;
    }

    public CandidateStatus getStatus() {
        return status;
    }

    public void setStatus(CandidateStatus status) {
        updatedFields.add("status");
        this.status = status;
    }

    @JsonIgnore
    public boolean hasDetectedEndAt() {
        return updatedFields.contains("detectedEndAt");
    }

    @JsonIgnore
    public boolean hasDurationSeconds() {
        return updatedFields.contains("durationSeconds");
    }

    @JsonIgnore
    public boolean hasLocationSummary() {
        return updatedFields.contains("locationSummary");
    }

    @JsonIgnore
    public boolean hasStatus() {
        return updatedFields.contains("status");
    }
}
