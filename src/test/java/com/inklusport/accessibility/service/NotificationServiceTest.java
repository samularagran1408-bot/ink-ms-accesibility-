package com.inklusport.accessibility.service;

import com.inklusport.accessibility.dto.NotificationRequest;
import com.inklusport.accessibility.dto.NotificationResponse;
import com.inklusport.accessibility.model.Notification;
import com.inklusport.accessibility.model.UserPreference;
import com.inklusport.accessibility.repository.NotificationRepository;
import com.inklusport.accessibility.repository.UserPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationEmailService notificationEmailService;
    @Mock
    private UserEmailLookupService userEmailLookupService;
    @Mock
    private UserPreferenceRepository preferenceRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                notificationEmailService,
                userEmailLookupService,
                preferenceRepository
        );
        lenient().when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId("n-1");
            return notification;
        });
        lenient().when(userEmailLookupService.resolveEmail(any())).thenReturn(null);
    }

    @Test
    void cp1Hu24_incluyeCanalDeVozCuandoTtsEstaActivo() {
        when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(
                UserPreference.builder()
                        .userId("user-1")
                        .notificationsEnabled(true)
                        .ttsEnabled(true)
                        .language("es")
                        .build()
        ));

        NotificationRequest request = sampleRequest("user-1");
        NotificationResponse response = notificationService.createNotification("user-1", request);

        assertThat(response.getDeliveryMethods()).contains("voice", "tts");
        assertThat(response.getAdaptations()).containsEntry("voice", true);
    }

    @Test
    void cp2Hu24_incluyeCanalVisualCuandoNotificacionesEstanActivas() {
        when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(
                UserPreference.builder()
                        .userId("user-1")
                        .notificationsEnabled(true)
                        .ttsEnabled(false)
                        .language("es")
                        .build()
        ));

        NotificationRequest request = sampleRequest("user-1");
        NotificationResponse response = notificationService.createNotification("user-1", request);

        assertThat(response.getDeliveryMethods()).contains("visual", "push");
        assertThat(response.getDeliveryMethods()).doesNotContain("voice");
        assertThat(response.getAdaptations()).containsEntry("visual", true);
        assertThat(response.getAdaptations()).containsEntry("voice", false);
    }

    @Test
    void create_expiraALas4HorasSiNoSeLee() {
        when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        notificationService.createNotification("user-1", sampleRequest("user-1"));

        verify(notificationRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        Notification saved = captor.getAllValues().get(0);
        LocalDateTime now = LocalDateTime.now();
        assertThat(saved.getExpiresAt()).isAfter(now.plusHours(3).plusMinutes(50));
        assertThat(saved.getExpiresAt()).isBefore(now.plusHours(4).plusMinutes(10));
        assertThat(saved.getRead()).isFalse();
    }

    @Test
    void markAsRead_expiraALas2Horas() {
        Notification stored = Notification.builder()
                .id("n-1")
                .userId("user-1")
                .read(false)
                .expiresAt(LocalDateTime.now().plusHours(4))
                .build();
        when(notificationRepository.findById("n-1")).thenReturn(Optional.of(stored));

        notificationService.markAsRead("user-1", "n-1");

        LocalDateTime now = LocalDateTime.now();
        assertThat(stored.getRead()).isTrue();
        assertThat(stored.getExpiresAt()).isAfter(now.plusHours(1).plusMinutes(50));
        assertThat(stored.getExpiresAt()).isBefore(now.plusHours(2).plusMinutes(10));
    }

    private NotificationRequest sampleRequest(String userId) {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setType("system");
        request.setTitle("Aviso de prueba");
        request.setBody("Cuerpo del aviso");
        return request;
    }
}
