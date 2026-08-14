package com.mowa.backend.dto.walkexperience;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mowa.backend.entity.Companion;
import com.mowa.backend.entity.Emotion;
import com.mowa.backend.entity.Situation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UpdateWalkExperienceRequest {

    private final Set<String> updatedFields = new HashSet<>();

    @Size(max = 100)
    private String title;

    private String body;

    private String photoUrl;

    private Companion companion;

    private List<@NotNull Emotion> emotions;

    private Situation situation;

    @Size(max = 10)
    private List<@NotNull @NotBlank @Size(max = 50) String> tags;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        updatedFields.add("title");
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        updatedFields.add("body");
        this.body = body;
    }

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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        updatedFields.add("tags");
        this.tags = tags;
    }

    @JsonIgnore
    public boolean hasTitle() {
        return updatedFields.contains("title");
    }

    @JsonIgnore
    public boolean hasBody() {
        return updatedFields.contains("body");
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

    @JsonIgnore
    public boolean hasTags() {
        return updatedFields.contains("tags");
    }
}
