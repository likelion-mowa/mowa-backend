package com.mowa.backend.controller;

import com.mowa.backend.common.response.ApiResponse;
import com.mowa.backend.dto.walkexperience.CreateWalkExperienceRequest;
import com.mowa.backend.dto.walkexperience.CreateWalkExperienceResponse;
import com.mowa.backend.dto.walkexperience.WalkExperienceListResponse;
import com.mowa.backend.security.AuthenticatedUser;
import com.mowa.backend.service.WalkExperienceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<WalkExperienceListResponse>>> getAll(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String tag
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                walkExperienceService.getAll(authenticatedUser.userId(), from, to, tag)
        ));
    }
}
