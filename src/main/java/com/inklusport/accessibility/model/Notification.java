package com.inklusport.accessibility.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "user_read_idx", def = "{'userId': 1, 'read': 1}")
public class Notification {

    @Id
    private String id;

    @Field("user_id") 
    @Indexed
    private String userId;

    private String type;  

    @Field("title")
    private String title;

    @Field("body")
    private String body;

    @Field("event_id")
    private String eventId;

    private String priority;

    private Map<String, Object> adaptations;
    private List<String> deliveryMethods;

    @Field("read")
    private Boolean read;

    @Field("read_at")
    private LocalDateTime readAt;

    private Map<String, Boolean> deliveryStatus;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("scheduled_for")
    private LocalDateTime scheduledFor;

    @Field("expires_at")
    private LocalDateTime expiresAt;
}