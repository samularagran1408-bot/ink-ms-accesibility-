package com.inklusport.accessibility.service;

import com.inklusport.accessibility.dto.PreferenceRequest;
import com.inklusport.accessibility.dto.PreferenceResponse;
import com.inklusport.accessibility.model.UserPreference;
import com.inklusport.accessibility.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreferenceService {

    private final UserPreferenceRepository preferenceRepository;

    public PreferenceResponse getPreferences(String userId) {
        return getPreferences(userId, null);
    }

    public PreferenceResponse getPreferences(String userId, String acceptLanguage) {
        UserPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId, acceptLanguage));
        return convertToResponse(preference);
    }

    public PreferenceResponse updatePreferences(String userId, PreferenceRequest request) {
        UserPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId, request.getLanguage()));

        if (request.getDisabilityType() != null) preference.setDisabilityType(request.getDisabilityType());
        if (request.getLanguage() != null) {
            String uiLanguage = normalizeUiLanguage(request.getLanguage());
            preference.setLanguage(uiLanguage);
            if (request.getFollowSystemLanguage() == null) {
                preference.setFollowSystemLanguage(false);
            }
            if (request.getVoiceLanguage() == null) {
                preference.setVoiceLanguage("en".equals(uiLanguage) ? "en-US" : "es-ES");
            }
        }
        if (request.getFollowSystemLanguage() != null) {
            preference.setFollowSystemLanguage(request.getFollowSystemLanguage());
        }
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
        if (request.getAttendanceCheckInMethod() != null) {
            preference.setAttendanceCheckInMethod(normalizeAttendanceCheckInMethod(request.getAttendanceCheckInMethod()));
        }

        syncAlertChannels(preference);

        preference = preferenceRepository.save(preference);
        log.info("Preferencias actualizadas para usuario: {}", userId);

        return convertToResponse(preference);
    }

    UserPreference createDefaultPreferences(String userId, String acceptLanguage) {
        String language = normalizeUiLanguage(languageFromAccept(acceptLanguage));
        UserPreference preference = UserPreference.builder()
                .userId(userId)
                .language(language)
                .followSystemLanguage(true)
                .highContrast(false)
                .fontSize("medium")
                .screenReader(false)
                .reducedMotion(false)
                .keyboardNavigation(true)
                .readerMode(false)
                .notificationsEnabled(true)
                .voiceCommandsEnabled(true)
                .ttsEnabled(true)
                .voiceLanguage(language.startsWith("en") ? "en-US" : "es-ES")
                .attendanceCheckInMethod("qr")
                .notificationPreferences(defaultAlertChannels())
                .trainingPreferences(new HashMap<>())
                .build();

        return preferenceRepository.save(preference);
    }

    private PreferenceResponse convertToResponse(UserPreference preference) {
        Map<String, Boolean> channels = preference.getNotificationPreferences() != null
                ? preference.getNotificationPreferences()
                : defaultAlertChannels();
        return PreferenceResponse.builder()
                .userId(preference.getUserId())
                .disabilityType(preference.getDisabilityType())
                .language(normalizeUiLanguage(preference.getLanguage()))
                .followSystemLanguage(Boolean.TRUE.equals(preference.getFollowSystemLanguage()))
                .highContrast(Boolean.TRUE.equals(preference.getHighContrast()))
                .fontSize(preference.getFontSize() != null ? preference.getFontSize() : "medium")
                .screenReader(Boolean.TRUE.equals(preference.getScreenReader()))
                .reducedMotion(Boolean.TRUE.equals(preference.getReducedMotion()))
                .keyboardNavigation(preference.getKeyboardNavigation() == null || preference.getKeyboardNavigation())
                .readerMode(Boolean.TRUE.equals(preference.getReaderMode()))
                .notificationsEnabled(preference.getNotificationsEnabled() == null || preference.getNotificationsEnabled())
                .voiceCommandsEnabled(Boolean.TRUE.equals(preference.getVoiceCommandsEnabled()))
                .ttsEnabled(preference.getTtsEnabled() == null || preference.getTtsEnabled())
                .voiceLanguage(preference.getVoiceLanguage() != null ? preference.getVoiceLanguage() : "es-ES")
                .attendanceCheckInMethod(normalizeAttendanceCheckInMethod(preference.getAttendanceCheckInMethod()))
                .notificationPreferences(channels)
                .trainingPreferences(preference.getTrainingPreferences())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    private void syncAlertChannels(UserPreference preference) {
        Map<String, Boolean> channels = preference.getNotificationPreferences() != null
                ? new HashMap<>(preference.getNotificationPreferences())
                : defaultAlertChannels();
        boolean visual = preference.getNotificationsEnabled() == null || preference.getNotificationsEnabled();
        boolean voice = visual && (preference.getTtsEnabled() == null || preference.getTtsEnabled());
        channels.put("visual", visual);
        channels.put("push", visual);
        channels.put("voice", voice);
        channels.put("tts", voice);
        channels.putIfAbsent("email", true);
        preference.setNotificationPreferences(channels);
    }

    static Map<String, Boolean> defaultAlertChannels() {
        Map<String, Boolean> channels = new HashMap<>();
        channels.put("email", true);
        channels.put("push", true);
        channels.put("visual", true);
        channels.put("voice", true);
        channels.put("tts", true);
        return channels;
    }

    static String languageFromAccept(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return "es";
        }
        String first = acceptLanguage.split(",")[0].trim().toLowerCase(Locale.ROOT);
        if (first.startsWith("en")) {
            return "en";
        }
        return "es";
    }

    static String normalizeUiLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "es";
        }
        return language.trim().toLowerCase(Locale.ROOT).startsWith("en") ? "en" : "es";
    }

    private String normalizeVoiceLanguage(String language) {
        if ("es".equalsIgnoreCase(language)) return "es-ES";
        if ("en".equalsIgnoreCase(language)) return "en-US";
        return language;
    }

    private String normalizeAttendanceCheckInMethod(String value) {
        if ("form".equalsIgnoreCase(value)) {
            return "form";
        }
        return "qr";
    }
}
