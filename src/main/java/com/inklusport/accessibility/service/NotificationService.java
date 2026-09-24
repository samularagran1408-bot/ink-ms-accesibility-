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

/**
 * Crea, consulta y marca notificaciones in-app del usuario.
 */
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

    /** Crea la notificación y envía el email si aplica. */
    public NotificationResponse createNotification(String userId, NotificationRequest request) {
        /**
         * Canonical key = email cuando se puede resolver (coincide con JWT del front).
         * Así in-app y Gmail llegan al mismo destinatario, sin filtrar por rol/tipo.
         */
        String emailTarget = resolveEmailTarget(userId, request != null ? request.getUserId() : null);
        String storageUserId = emailTarget != null ? emailTarget : (userId != null ? userId.trim() : null);

        AlertChannels channels = resolveAlertChannels(storageUserId, userId);
        LocalizedContent content = localize(request, channels.language());

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
                .title(content.title())
                .body(content.body())
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
                    content.title(),
                    content.body()
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

    /** Lista las notificaciones recientes del usuario. */
    public List<NotificationResponse> getUserNotifications(String userId) {
        String language = findPreference(userId).map(UserPreference::getLanguage).orElse("es");
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, MAX_NOTIFICATIONS)).stream()
            .map(notification -> convertToResponse(notification, language))
                .collect(Collectors.toList());
    }

    /** Lista solo las notificaciones no leídas del usuario. */
    public List<NotificationResponse> getUnreadNotifications(String userId) {
        String language = findPreference(userId).map(UserPreference::getLanguage).orElse("es");
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, PageRequest.of(0, MAX_UNREAD)).stream()
            .map(notification -> convertToResponse(notification, language))
                .collect(Collectors.toList());
    }

    /** Marca una notificación como leída y ajusta su caducidad. */
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

    /** Marca todas las notificaciones pendientes como leídas. */
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

    /** Cuenta las notificaciones no leídas del usuario. */
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /** Resuelve el email destino desde el header o el request. */
    private String resolveEmailTarget(String headerUserId, String requestUserId) {
        String fromHeader = userEmailLookupService.resolveEmail(headerUserId);
        if (fromHeader != null) {
            return fromHeader;
        }
        return userEmailLookupService.resolveEmail(requestUserId);
    }

    /** Determina los canales de alerta según las preferencias. */
    private AlertChannels resolveAlertChannels(String... candidateUserIds) {
        UserPreference prefs = findPreference(candidateUserIds).orElse(null);
        boolean visual = prefs == null || prefs.getNotificationsEnabled() == null || prefs.getNotificationsEnabled();
        boolean voice = visual && (prefs == null || prefs.getTtsEnabled() == null || prefs.getTtsEnabled());
        boolean screenReader = prefs != null && Boolean.TRUE.equals(prefs.getScreenReader());
        boolean highContrast = prefs != null && Boolean.TRUE.equals(prefs.getHighContrast());
        String language = prefs != null && prefs.getLanguage() != null ? prefs.getLanguage() : "es";
        return new AlertChannels(visual, voice, screenReader, highContrast, language);
    }

    /** Busca preferencias por el primer identificador que exista. */
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

    /** Canales y adaptaciones de alerta para una notificación. */
    private record AlertChannels(
            boolean visual,
            boolean voice,
            boolean screenReader,
            boolean highContrast,
            String language
    ) {}

    private record LocalizedContent(String title, String body) {}

    private LocalizedContent localize(NotificationRequest request, String language) {
        if (!"en".equalsIgnoreCase(language)) {
            return new LocalizedContent(request.getTitle(), request.getBody());
        }

        String type = request.getType() == null ? "" : request.getType().toLowerCase();
        String title = request.getTitle();
        String body = request.getBody();
        switch (type) {
            case "attendance_confirmed" -> {
                title = "Attendance confirmed";
                body = "Your check-in at " + valueAfter(body, "Tu check-in en ", " was recorded.");
            }
            case "attendance_checkin" -> {
                title = "New check-in";
                body = body.replace(" registró asistencia en ", " checked in at ");
            }
            case "event_cancelled", "admin_event_cancelled" -> {
                title = "Event cancelled";
                body = body.replace("El evento ", "The event ").replace(" fue cancelado.", " was cancelled.");
            }
            case "role_request_approved" -> {
                title = "Role request approved";
                body = body.replace("Tu solicitud del rol ", "Your request for the ")
                        .replace(" fue aprobada.", " was approved.")
                        .replace(" Recarga la página para que se cargue tu nuevo rol.", " Reload the page to load your new role.");
            }
            case "role_request_rejected" -> {
                title = "Role request rejected";
                body = body.replace("Tu solicitud del rol ", "Your request for the ")
                        .replace(" fue rechazada.", " was rejected.")
                        .replace(" Conservas el rol Usuario.", " You keep the User role.")
                        .replace(" Si crees que es un error, contacta a un administrador.", " If you think this is an error, contact an administrator.");
            }
            case "admin_user_registered" -> {
                title = "New user registered";
                body = body.replace("El usuario ", "User ").replace(" se registró por primera vez.", " registered for the first time.");
            }
            case "admin_role_request" -> {
                title = "New role request";
                body = body.replace("El usuario ", "User ")
                        .replace(" solicitó el rol ", " requested the ")
                        .replace(". Revísala en Solicitudes de rol.", ". Review it in Role requests.");
            }
            default -> {
                return new LocalizedContent(title, body);
            }
        }
        return new LocalizedContent(title, body);
    }

    private String valueAfter(String value, String prefix, String suffix) {
        if (value == null || !value.startsWith(prefix)) {
            return value == null ? "the event" : value;
        }
        String result = value.substring(prefix.length());
        int suffixIndex = result.indexOf(suffix);
        return suffixIndex >= 0 ? result.substring(0, suffixIndex) : result;
    }

    /** Convierte el modelo de notificación a DTO de respuesta. */
    private NotificationResponse convertToResponse(Notification notification) {
        return convertToResponse(notification, "es");
    }

    private NotificationResponse convertToResponse(Notification notification, String language) {
        NotificationRequest request = new NotificationRequest();
        request.setType(notification.getType());
        request.setTitle(notification.getTitle());
        request.setBody(notification.getBody());
        LocalizedContent content = localize(request, language);
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(content.title())
                .body(content.body())
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
