package com.mowa.backend.controller;

import com.mowa.backend.common.response.ApiResponse;
import com.mowa.backend.dto.experiencedraft.CreateExperienceDraftRequest;
import com.mowa.backend.dto.experiencedraft.ExperienceDraftResponse;
import com.mowa.backend.dto.experiencedraft.UpdateExperienceDraftRequest;
import com.mowa.backend.security.AuthenticatedUser;
import com.mowa.backend.service.ExperienceDraftService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ExperienceDraftController {

    private final ExperienceDraftService experienceDraftService;

    public ExperienceDraftController(ExperienceDraftService experienceDraftService) {
        this.experienceDraftService = experienceDraftService;
    }

    @PostMapping("/walk-candidates/{candidateId}/experience-drafts")
    public ResponseEntity<ApiResponse<ExperienceDraftResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID candidateId,
            @Valid @RequestBody CreateExperienceDraftRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                experienceDraftService.create(authenticatedUser.userId(), candidateId, request)
        ));
    }

    @PatchMapping("/experience-drafts/{draftId}")
    public ResponseEntity<ApiResponse<ExperienceDraftResponse>> update(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID draftId,
            @Valid @RequestBody UpdateExperienceDraftRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                experienceDraftService.update(authenticatedUser.userId(), draftId, request)
        ));
    }
}
