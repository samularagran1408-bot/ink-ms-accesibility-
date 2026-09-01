package com.inklusport.accessibility.service;

import com.inklusport.accessibility.dto.NotificationRequest;
import com.inklusport.accessibility.dto.NotificationResponse;
import com.inklusport.accessibility.model.Notification;
import com.inklusport.accessibility.model.UserPreference;
import com.inklusport.accessibility.repository.NotificationRepository;
import com.inklusport.accessibility.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final int MAX_NOTIFICATIONS = 50;
    private static final int MAX_UNREAD = 20;
    static final int HOURS_IF_UNREAD = 4;
    static final int HOURS_AFTER_READ = 2;

    private final NotificationRepository notificationRepository;
    private final NotificationEmailService notificationEmailService;
    private final UserEmailLookupService userEmailLookupService;
    private final UserPreferenceRepository preferenceRepository;

    public NotificationResponse createNotification(String userId, NotificationRequest request) {
        /**
         * Canonical key = email cuando se puede resolver (coincide con JWT del front).
         * Así in-app y Gmail llegan al mismo destinatario, sin filtrar por rol/tipo.
         */
        String emailTarget = resolveEmailTarget(userId, request != null ? request.getUserId() : null);
        String storageUserId = emailTarget != null ? emailTarget : (userId != null ? userId.trim() : null);

        AlertChannels channels = resolveAlertChannels(storageUserId, userId);

        Map<String, Boolean> deliveryStatus = new HashMap<>();
        deliveryStatus.put("push", channels.visual);
        deliveryStatus.put("visual", channels.visual);
        deliveryStatus.put("tts", channels.voice);
        deliveryStatus.put("voice", channels.voice);
        deliveryStatus.put("email", false);

        List<String> deliveryMethods = new ArrayList<>();
        if (channels.visual) {
            deliveryMethods.add("push");
            deliveryMethods.add("visual");
        }
        if (channels.voice) {
            deliveryMethods.add("tts");
            deliveryMethods.add("voice");
        }
        deliveryMethods.add("email");

        Map<String, Object> adaptations = new HashMap<>();
        adaptations.put("visual", channels.visual);
        adaptations.put("voice", channels.voice);
        adaptations.put("screenReader", channels.screenReader);
        adaptations.put("highContrast", channels.highContrast);
        adaptations.put("language", channels.language);

        Notification notification = Notification.builder()
                .userId(storageUserId)
                .type(request.getType())
                .title(request.getTitle())
                .body(request.getBody())
                .eventId(request.getEventId())
                .priority(request.getPriority() != null ? request.getPriority() : "medium")
                .adaptations(adaptations)
                .deliveryMethods(deliveryMethods)
                .read(false)
                .deliveryStatus(deliveryStatus)
                .createdAt(LocalDateTime.now())
                .scheduledFor(request.getScheduledFor() != null ? request.getScheduledFor() : LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(HOURS_IF_UNREAD))
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
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, MAX_NOTIFICATIONS)).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, PageRequest.of(0, MAX_UNREAD)).stream()
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

        LocalDateTime now = LocalDateTime.now();
        notification.setRead(true);
        notification.setReadAt(now);
        notification.setExpiresAt(now.plusHours(HOURS_AFTER_READ));
        notificationRepository.save(notification);
        log.info("Notificación marcada como leída: {}", notificationId);
    }

    public void markAllAsRead(String userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndReadFalse(userId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = now.plusHours(HOURS_AFTER_READ);
        notifications.forEach(n -> {
            n.setRead(true);
            n.setReadAt(now);
            n.setExpiresAt(expires);
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

    private AlertChannels resolveAlertChannels(String... candidateUserIds) {
        UserPreference prefs = findPreference(candidateUserIds).orElse(null);
        boolean visual = prefs == null || prefs.getNotificationsEnabled() == null || prefs.getNotificationsEnabled();
        boolean voice = visual && (prefs == null || prefs.getTtsEnabled() == null || prefs.getTtsEnabled());
        boolean screenReader = prefs != null && Boolean.TRUE.equals(prefs.getScreenReader());
        boolean highContrast = prefs != null && Boolean.TRUE.equals(prefs.getHighContrast());
        String language = prefs != null && prefs.getLanguage() != null ? prefs.getLanguage() : "es";
        return new AlertChannels(visual, voice, screenReader, highContrast, language);
    }

    private Optional<UserPreference> findPreference(String... candidateUserIds) {
        if (candidateUserIds == null) {
            return Optional.empty();
        }
        for (String candidate : candidateUserIds) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            Optional<UserPreference> found = preferenceRepository.findByUserId(candidate.trim());
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private record AlertChannels(
            boolean visual,
            boolean voice,
            boolean screenReader,
            boolean highContrast,
            String language
    ) {}

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
