package com.inklusport.accessibility.controller;

import com.inklusport.accessibility.dto.NotificationRequest;
import com.inklusport.accessibility.dto.NotificationResponse;
import com.inklusport.accessibility.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
//
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(@AuthenticationPrincipal String userId) {
        if (userId == null || userId.isBlank() || "anonymousUser".equals(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(notificationService.subscribe(userId));
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    @GetMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal String userId, @PathVariable String notificationId) {
        notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal String userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createNotificationByAdmin(
            @Valid @RequestBody NotificationRequest request) {
        
        log.info("Admin creando notificación para usuario: {}", request.getUserId());
        
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setUserId(request.getUserId());
        notificationRequest.setType(request.getType() != null ? request.getType() : "system");
        notificationRequest.setTitle(request.getTitle());
        notificationRequest.setBody(request.getBody());
        notificationRequest.setPriority(request.getPriority() != null ? request.getPriority() : "medium");
        
        notificationService.createNotification(request.getUserId(), notificationRequest);
        
        return ResponseEntity.ok(Map.of(
            "message", "Notificación enviada al usuario: " + request.getUserId(),
            "status", "success"
        ));
    }

    @PostMapping("/internal/create")
    public ResponseEntity<?> createNotificationInternal(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody NotificationRequest request) {
        
        String finalUserId = userId != null ? userId : request.getUserId();
        
        log.info("Notificación interna - Usuario: {}, Título: {}", finalUserId, request.getTitle());
        
        if (finalUserId == null) {
            log.error("No se pudo determinar el userId");
            return ResponseEntity.badRequest().build();
        }
        
        notificationService.createNotification(finalUserId, request);
        return ResponseEntity.ok().build();
    }
}