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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId("n-1");
            return notification;
        });
        when(userEmailLookupService.resolveEmail(any())).thenReturn(null);
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

    private NotificationRequest sampleRequest(String userId) {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setType("system");
        request.setTitle("Aviso de prueba");
        request.setBody("Cuerpo del aviso");
        return request;
    }
}
