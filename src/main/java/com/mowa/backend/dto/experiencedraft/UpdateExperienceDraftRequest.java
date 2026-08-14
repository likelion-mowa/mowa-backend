package com.mowa.backend.dto.experiencedraft;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mowa.backend.entity.Companion;
import com.mowa.backend.entity.Emotion;
import com.mowa.backend.entity.Situation;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UpdateExperienceDraftRequest {

    private final Set<String> updatedFields = new HashSet<>();

    private String photoUrl;

    private Companion companion;

    private List<@NotNull Emotion> emotions;

    private Situation situation;

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        updatedFields.add("photoUrl");
        this.photoUrl = photoUrl;
    }

    public Companion getCompanion() {
        return companion;
    }

    public void setCompanion(Companion companion) {
        updatedFields.add("companion");
        this.companion = companion;
    }

    public List<Emotion> getEmotions() {
        return emotions;
    }

    public void setEmotions(List<Emotion> emotions) {
        updatedFields.add("emotions");
        this.emotions = emotions;
    }

    public Situation getSituation() {
        return situation;
    }

    public void setSituation(Situation situation) {
        updatedFields.add("situation");
        this.situation = situation;
    }

    @JsonIgnore
    public boolean hasPhotoUrl() {
        return updatedFields.contains("photoUrl");
    }

    @JsonIgnore
    public boolean hasCompanion() {
        return updatedFields.contains("companion");
    }

    @JsonIgnore
    public boolean hasEmotions() {
        return updatedFields.contains("emotions");
    }

    @JsonIgnore
    public boolean hasSituation() {
        return updatedFields.contains("situation");
    }
}
