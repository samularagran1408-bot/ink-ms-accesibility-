package com.inklusport.accessibility.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "user_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    @Id
    private String id;

    @NotNull
    @Indexed(unique = true)
    private String userId;

    private String disabilityType;
    private String language;
    /** Si es true, el cliente debe seguir el idioma del sistema operativo. */
    private Boolean followSystemLanguage;
    private Boolean highContrast;
    private String fontSize;
    private Boolean screenReader;
    private Boolean reducedMotion;
    private Boolean keyboardNavigation;
    private Boolean readerMode;
    private Boolean notificationsEnabled;

    /** Comandos por voz (Web Speech / STT). */
    private Boolean voiceCommandsEnabled;
    /** Feedback hablado (TTS) al ejecutar un comando. */
    private Boolean ttsEnabled;
    /** Idioma del reconocedor de voz (p. ej. es-ES, en-US). */
    private String voiceLanguage;

    /** Cómo confirma asistencia el atleta: {@code qr} (escaneo instantáneo) o {@code form} (encuesta). */
    private String attendanceCheckInMethod;

    private Map<String, Boolean> notificationPreferences;
    private Map<String, Object> trainingPreferences;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}