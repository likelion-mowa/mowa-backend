package com.mowa.backend.controller;

import com.mowa.backend.common.response.ApiResponse;
import com.mowa.backend.dto.walkexperience.CreateWalkExperienceRequest;
import com.mowa.backend.dto.walkexperience.CreateWalkExperienceResponse;
import com.mowa.backend.security.AuthenticatedUser;
import com.mowa.backend.service.WalkExperienceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/walk-experiences")
public class WalkExperienceController {

    private final WalkExperienceService walkExperienceService;

    public WalkExperienceController(WalkExperienceService walkExperienceService) {
        this.walkExperienceService = walkExperienceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateWalkExperienceResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateWalkExperienceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                walkExperienceService.create(authenticatedUser.userId(), request)
        ));
    }
}
