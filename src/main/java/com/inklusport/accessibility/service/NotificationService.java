package com.inklusport.accessibility.service;

import com.inklusport.accessibility.dto.NotificationRequest;
import com.inklusport.accessibility.dto.NotificationResponse;
import com.inklusport.accessibility.model.Notification;
import com.inklusport.accessibility.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationResponse createNotification(String userId, NotificationRequest request) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(request.getType())
                .title(request.getTitle())
                .body(request.getBody())
                .eventId(request.getEventId())
                .priority(request.getPriority() != null ? request.getPriority() : "medium")
                .adaptations(new HashMap<>())
                .deliveryMethods(List.of("push", "email"))
                .read(false)
                .deliveryStatus(new HashMap<>())
                .scheduledFor(request.getScheduledFor() != null ? request.getScheduledFor() : LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notificación creada para usuario: {}", userId);

        return convertToResponse(notification);
    }

    public List<NotificationResponse> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndReadFalse(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(String userId, String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("No autorizado");
        }

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
        log.info("Notificación marcada como leída: {}", notificationId);
    }

    public void markAllAsRead(String userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndReadFalse(userId);
        notifications.forEach(n -> {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
        });
        notificationRepository.saveAll(notifications);
        log.info("Todas las notificaciones marcadas como leídas para usuario: {}", userId);
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    private NotificationResponse convertToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .eventId(notification.getEventId())
                .priority(notification.getPriority())
                .adaptations(notification.getAdaptations())
                .deliveryMethods(notification.getDeliveryMethods())
                .read(notification.getRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .scheduledFor(notification.getScheduledFor())
                .build();
    }
}