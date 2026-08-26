package com.inklusport.accessibility.controller;

import com.inklusport.accessibility.dto.PreferenceRequest;
import com.inklusport.accessibility.dto.PreferenceResponse;
import com.inklusport.accessibility.service.PreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<PreferenceResponse> getPreferences(
            @AuthenticationPrincipal String userId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return ResponseEntity.ok(preferenceService.getPreferences(userId, acceptLanguage));
    }

    @PutMapping
    public ResponseEntity<PreferenceResponse> updatePreferences(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody PreferenceRequest request) {
        return ResponseEntity.ok(preferenceService.updatePreferences(userId, request));
    }
}