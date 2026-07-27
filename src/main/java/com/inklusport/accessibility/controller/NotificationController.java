package com.inklusport.accessibility.controller;

import com.inklusport.accessibility.dto.NotificationRequest;
import com.inklusport.accessibility.dto.NotificationResponse;
import com.inklusport.accessibility.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
//
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

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