package com.inklusport.accessibility.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "notification_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    private String id;

    @Indexed(unique = true)
    @NotNull
    private String userId;

    private Map<String, Boolean> channels;
    private Map<String, Boolean> eventTypes;
    private Map<String, Object> quietHours;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}