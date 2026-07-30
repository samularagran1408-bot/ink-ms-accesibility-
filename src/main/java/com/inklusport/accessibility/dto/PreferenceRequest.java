package com.inklusport.accessibility.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PreferenceRequest {

    private String disabilityType;

    @Pattern(regexp = "es|en", message = "Idioma debe ser 'es' o 'en'")
    private String language;

    private Boolean highContrast;

    @Pattern(regexp = "small|medium|large|xlarge", message = "Tamaño de fuente inválido")
    private String fontSize;

    private Boolean screenReader;
    private Boolean reducedMotion;
    private Boolean keyboardNavigation;
    private Boolean readerMode;
    private Boolean notificationsEnabled;
    private Boolean voiceCommandsEnabled;
    private Boolean ttsEnabled;

    @Pattern(regexp = "es-ES|en-US|es|en", message = "Idioma de voz inválido")
    private String voiceLanguage;
}