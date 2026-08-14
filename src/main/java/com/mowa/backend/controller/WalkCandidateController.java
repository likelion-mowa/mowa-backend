package com.mowa.backend.controller;

import com.mowa.backend.common.response.ApiResponse;
import com.mowa.backend.dto.walkcandidate.CreateWalkCandidateRequest;
import com.mowa.backend.dto.walkcandidate.CreateWalkCandidateResponse;
import com.mowa.backend.dto.walkcandidate.UpdateWalkCandidateRequest;
import com.mowa.backend.dto.walkcandidate.WalkCandidateResponse;
import com.mowa.backend.security.AuthenticatedUser;
import com.mowa.backend.service.WalkCandidateService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/walk-candidates")
public class WalkCandidateController {

    private final WalkCandidateService walkCandidateService;

    public WalkCandidateController(WalkCandidateService walkCandidateService) {
        this.walkCandidateService = walkCandidateService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateWalkCandidateResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateWalkCandidateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(walkCandidateService.create(authenticatedUser.userId(), request)));
    }

    @GetMapping("/{candidateId}")
    public ResponseEntity<ApiResponse<WalkCandidateResponse>> get(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID candidateId
    ) {
        return ResponseEntity.ok(ApiResponse.success(walkCandidateService.get(authenticatedUser.userId(), candidateId)));
    }

    @PatchMapping("/{candidateId}")
    public ResponseEntity<ApiResponse<WalkCandidateResponse>> update(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID candidateId,
            @Valid @RequestBody UpdateWalkCandidateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                walkCandidateService.update(authenticatedUser.userId(), candidateId, request)
        ));
    }
}
