package com.inklusport.accessibility.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class PreferenceResponse {
    private String userId;
    private String disabilityType;
    private String language;
    private Boolean highContrast;
    private String fontSize;
    private Boolean screenReader;
    private Boolean reducedMotion;
    private Boolean keyboardNavigation;
    private Boolean readerMode;
    private Boolean notificationsEnabled;
    private Boolean voiceCommandsEnabled;
    private Boolean ttsEnabled;
    private String voiceLanguage;
    private String attendanceCheckInMethod;
    private Map<String, Boolean> notificationPreferences;
    private Map<String, Object> trainingPreferences;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}