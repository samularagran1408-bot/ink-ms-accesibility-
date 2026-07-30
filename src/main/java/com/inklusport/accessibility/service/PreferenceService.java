package com.inklusport.accessibility.service;

import com.inklusport.accessibility.dto.PreferenceRequest;
import com.inklusport.accessibility.dto.PreferenceResponse;
import com.inklusport.accessibility.model.UserPreference;
import com.inklusport.accessibility.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreferenceService {

    private final UserPreferenceRepository preferenceRepository;

    public PreferenceResponse getPreferences(String userId) {
        UserPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
        return convertToResponse(preference);
    }

    public PreferenceResponse updatePreferences(String userId, PreferenceRequest request) {
        UserPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        if (request.getDisabilityType() != null) preference.setDisabilityType(request.getDisabilityType());
        if (request.getLanguage() != null) preference.setLanguage(request.getLanguage());
        if (request.getHighContrast() != null) preference.setHighContrast(request.getHighContrast());
        if (request.getFontSize() != null) preference.setFontSize(request.getFontSize());
        if (request.getScreenReader() != null) preference.setScreenReader(request.getScreenReader());
        if (request.getReducedMotion() != null) preference.setReducedMotion(request.getReducedMotion());
        if (request.getKeyboardNavigation() != null) preference.setKeyboardNavigation(request.getKeyboardNavigation());
        if (request.getReaderMode() != null) preference.setReaderMode(request.getReaderMode());
        if (request.getNotificationsEnabled() != null) preference.setNotificationsEnabled(request.getNotificationsEnabled());
        if (request.getVoiceCommandsEnabled() != null) preference.setVoiceCommandsEnabled(request.getVoiceCommandsEnabled());
        if (request.getTtsEnabled() != null) preference.setTtsEnabled(request.getTtsEnabled());
        if (request.getVoiceLanguage() != null) preference.setVoiceLanguage(normalizeVoiceLanguage(request.getVoiceLanguage()));

        preference = preferenceRepository.save(preference);
        log.info("Preferencias actualizadas para usuario: {}", userId);

        return convertToResponse(preference);
    }

    private UserPreference createDefaultPreferences(String userId) {
        UserPreference preference = UserPreference.builder()
                .userId(userId)
                .language("es")
                .highContrast(false)
                .fontSize("medium")
                .screenReader(false)
                .reducedMotion(false)
                .keyboardNavigation(true)
                .readerMode(false)
                .notificationsEnabled(true)
                .voiceCommandsEnabled(true)
                .ttsEnabled(true)
                .voiceLanguage("es-ES")
                .notificationPreferences(new HashMap<>())
                .trainingPreferences(new HashMap<>())
                .build();
        
        preference.getNotificationPreferences().put("email", true);
        preference.getNotificationPreferences().put("push", true);
        
        return preferenceRepository.save(preference);
    }

    private PreferenceResponse convertToResponse(UserPreference preference) {
        return PreferenceResponse.builder()
                .userId(preference.getUserId())
                .disabilityType(preference.getDisabilityType())
                .language(preference.getLanguage())
                .highContrast(preference.getHighContrast())
                .fontSize(preference.getFontSize())
                .screenReader(preference.getScreenReader())
                .reducedMotion(preference.getReducedMotion())
                .keyboardNavigation(preference.getKeyboardNavigation())
                .readerMode(preference.getReaderMode())
                .notificationsEnabled(preference.getNotificationsEnabled())
                .voiceCommandsEnabled(Boolean.TRUE.equals(preference.getVoiceCommandsEnabled()))
                .ttsEnabled(preference.getTtsEnabled() == null || preference.getTtsEnabled())
                .voiceLanguage(preference.getVoiceLanguage() != null ? preference.getVoiceLanguage() : "es-ES")
                .notificationPreferences(preference.getNotificationPreferences())
                .trainingPreferences(preference.getTrainingPreferences())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    private String normalizeVoiceLanguage(String language) {
        if ("es".equalsIgnoreCase(language)) return "es-ES";
        if ("en".equalsIgnoreCase(language)) return "en-US";
        return language;
    }
}