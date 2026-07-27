package com.inklusport.accessibility.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationRequest {

    @NotBlank(message = "El ID del usuario destino es obligatorio")
    private String userId;

    @NotBlank(message = "El tipo es obligatorio")
    private String type;

    @NotBlank(message = "El título es obligatorio")
    private String title;

    @NotBlank(message = "El cuerpo es obligatorio")
    private String body;

    private String eventId;
    private String priority;
    private LocalDateTime scheduledFor;
}