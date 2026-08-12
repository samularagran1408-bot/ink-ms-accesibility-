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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationEmailService notificationEmailService;
    private final UserEmailLookupService userEmailLookupService;

    public NotificationResponse createNotification(String userId, NotificationRequest request) {
        Map<String, Boolean> deliveryStatus = new HashMap<>();
        deliveryStatus.put("push", true);
        deliveryStatus.put("tts", true);
        deliveryStatus.put("email", false);

        /**
         * Canonical key = email cuando se puede resolver (coincide con JWT del front).
         * Así in-app y Gmail llegan al mismo destinatario, sin filtrar por rol/tipo.
         */
        String emailTarget = resolveEmailTarget(userId, request != null ? request.getUserId() : null);
        String storageUserId = emailTarget != null ? emailTarget : (userId != null ? userId.trim() : null);

        Notification notification = Notification.builder()
                .userId(storageUserId)
                .type(request.getType())
                .title(request.getTitle())
                .body(request.getBody())
                .eventId(request.getEventId())
                .priority(request.getPriority() != null ? request.getPriority() : "medium")
                .adaptations(new HashMap<>())
                .deliveryMethods(List.of("push", "email", "tts"))
                .read(false)
                .deliveryStatus(deliveryStatus)
                .scheduledFor(request.getScheduledFor() != null ? request.getScheduledFor() : LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notificación creada para usuario: {} (tipo={})", storageUserId, request.getType());

        if (emailTarget != null) {
            boolean sent = notificationEmailService.sendNotificationEmail(
                    emailTarget,
                    request.getTitle(),
                    request.getBody()
            );
            deliveryStatus.put("email", sent);
            notification.setDeliveryStatus(deliveryStatus);
            notification = notificationRepository.save(notification);
            if (!sent) {
                log.warn(
                        "Notificación {} guardada, pero email no enviado a {} (revisa MAIL_ENABLED/credenciales)",
                        request.getType(),
                        emailTarget
                );
            }
        } else {
            log.warn(
                    "Sin email resoluble para userId={}, request.userId={}; solo queda in-app si el key coincide",
                    userId,
                    request.getUserId()
            );
        }

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

    private String resolveEmailTarget(String headerUserId, String requestUserId) {
        String fromHeader = userEmailLookupService.resolveEmail(headerUserId);
        if (fromHeader != null) {
            return fromHeader;
        }
        return userEmailLookupService.resolveEmail(requestUserId);
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
