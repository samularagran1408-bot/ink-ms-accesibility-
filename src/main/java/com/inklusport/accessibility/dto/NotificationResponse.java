package com.inklusport.accessibility.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private String userId;
    private String type;
    private String title;
    private String body;
    private String eventId;
    private String priority;
    private Map<String, Object> adaptations;
    private List<String> deliveryMethods;
    private Boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledFor;
}