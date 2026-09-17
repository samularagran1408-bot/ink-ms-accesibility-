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

/**
 * Gestiona las preferencias de accesibilidad del usuario.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PreferenceService {

    private final UserPreferenceRepository preferenceRepository;

    /** Obtiene las preferencias del usuario con idioma por defecto. */
    public PreferenceResponse getPreferences(String userId) {
        return getPreferences(userId, null);
    }

    /** Obtiene las preferencias o valores por defecto según Accept-Language. */
    public PreferenceResponse getPreferences(String userId, String acceptLanguage) {
        return preferenceRepository.findByUserId(userId)
                .map(this::convertToResponse)
                .orElseGet(() -> convertToResponse(buildDefaultPreferences(userId, acceptLanguage)));
    }

    /** Actualiza las preferencias enviadas por el usuario. */
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
        if (request.getWeeklyReportEmailEnabled() != null) {
            preference.setWeeklyReportEmailEnabled(request.getWeeklyReportEmailEnabled());
        }

        syncAlertChannels(preference);

        preference = preferenceRepository.save(preference);
        log.info("Preferencias actualizadas para usuario: {}", userId);

        return convertToResponse(preference);
    }

    /** Crea y guarda preferencias por defecto del usuario. */
    UserPreference createDefaultPreferences(String userId, String acceptLanguage) {
        return preferenceRepository.save(buildDefaultPreferences(userId, acceptLanguage));
    }

    /** Construye preferencias por defecto sin persistirlas. */
    private UserPreference buildDefaultPreferences(String userId, String acceptLanguage) {
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
                .weeklyReportEmailEnabled(false)
                .notificationPreferences(defaultAlertChannels())
                .trainingPreferences(new HashMap<>())
                .build();

        return preference;
    }

    /** Convierte el modelo de preferencias a DTO de respuesta. */
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
                .weeklyReportEmailEnabled(Boolean.TRUE.equals(preference.getWeeklyReportEmailEnabled()))
                .notificationPreferences(channels)
                .trainingPreferences(preference.getTrainingPreferences())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    /** Sincroniza los canales de alerta con las flags del usuario. */
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

    /** Devuelve el mapa inicial de canales de alerta. */
    static Map<String, Boolean> defaultAlertChannels() {
        Map<String, Boolean> channels = new HashMap<>();
        channels.put("email", true);
        channels.put("push", true);
        channels.put("visual", true);
        channels.put("voice", true);
        channels.put("tts", true);
        return channels;
    }

    /** Extrae el idioma de UI desde Accept-Language. */
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

    /** Normaliza el idioma de interfaz a es o en. */
    static String normalizeUiLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "es";
        }
        return language.trim().toLowerCase(Locale.ROOT).startsWith("en") ? "en" : "es";
    }

    /** Normaliza el código de idioma de voz. */
    private String normalizeVoiceLanguage(String language) {
        if ("es".equalsIgnoreCase(language)) return "es-ES";
        if ("en".equalsIgnoreCase(language)) return "en-US";
        return language;
    }

    /** Normaliza el método de check-in a qr o form. */
    private String normalizeAttendanceCheckInMethod(String value) {
        if ("form".equalsIgnoreCase(value)) {
            return "form";
        }
        return "qr";
    }
}
