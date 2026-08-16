package com.mowa.backend.controller;

import com.mowa.backend.common.response.ApiResponse;
import com.mowa.backend.dto.walkexperience.CreateWalkExperienceRequest;
import com.mowa.backend.dto.walkexperience.CreateWalkExperienceResponse;
import com.mowa.backend.dto.walkexperience.WalkExperienceListResponse;
import com.mowa.backend.dto.walkexperience.WalkExperienceDetailResponse;
import com.mowa.backend.dto.walkexperience.UpdateWalkExperienceRequest;
import com.mowa.backend.security.AuthenticatedUser;
import com.mowa.backend.service.WalkExperienceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
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
                walkExperienceService.create(
                        authenticatedUser.userId(),
                        authenticatedUser.demoSessionId(),
                        request
                )
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
                walkExperienceService.getAll(
                        authenticatedUser.userId(),
                        authenticatedUser.demoSessionId(),
                        from,
                        to,
                        tag
                )
        ));
    }

    @GetMapping("/{experienceId}")
    public ResponseEntity<ApiResponse<WalkExperienceDetailResponse>> get(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID experienceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                walkExperienceService.get(
                        authenticatedUser.userId(),
                        authenticatedUser.demoSessionId(),
                        experienceId
                )
        ));
    }

    @PatchMapping("/{experienceId}")
    public ResponseEntity<ApiResponse<WalkExperienceDetailResponse>> update(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID experienceId,
            @Valid @RequestBody UpdateWalkExperienceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                walkExperienceService.update(
                        authenticatedUser.userId(),
                        authenticatedUser.demoSessionId(),
                        experienceId,
                        request
                )
        ));
    }

    @DeleteMapping("/{experienceId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID experienceId
    ) {
        walkExperienceService.delete(
                authenticatedUser.userId(),
                authenticatedUser.demoSessionId(),
                experienceId
        );
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
