package com.inklusport.accessibility.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InterpretRequest {

    /** Transcripción de voz o id de gesto detectado en el cliente. */
    @NotBlank(message = "El campo input es obligatorio")
    private String input;

    /** Idioma preferido (es | en). Opcional. */
    private String language;

    /** Si true, persiste el intento en el historial del usuario. */
    private Boolean log = true;
}
