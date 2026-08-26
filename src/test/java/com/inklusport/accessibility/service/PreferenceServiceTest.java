package com.inklusport.accessibility.service;

import com.inklusport.accessibility.dto.PreferenceRequest;
import com.inklusport.accessibility.dto.PreferenceResponse;
import com.inklusport.accessibility.model.UserPreference;
import com.inklusport.accessibility.repository.UserPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {

    @Mock
    private UserPreferenceRepository preferenceRepository;

    private PreferenceService preferenceService;

    @BeforeEach
    void setUp() {
        preferenceService = new PreferenceService(preferenceRepository);
        lenient().when(preferenceRepository.save(any(UserPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void cp1Hu20_guardaFuenteYModoLector() {
        when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        PreferenceRequest request = new PreferenceRequest();
        request.setFontSize("xlarge");
        request.setReaderMode(true);
        request.setScreenReader(true);

        PreferenceResponse response = preferenceService.updatePreferences("user-1", request);

        assertThat(response.getFontSize()).isEqualTo("xlarge");
        assertThat(response.getReaderMode()).isTrue();
        assertThat(response.getScreenReader()).isTrue();
    }

    @Test
    void cp2Hu20_guardaContrasteManual() {
        when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        PreferenceRequest request = new PreferenceRequest();
        request.setHighContrast(true);

        PreferenceResponse response = preferenceService.updatePreferences("user-1", request);

        assertThat(response.getHighContrast()).isTrue();
    }

    @Test
    void cp1Hu21_cambiaIdiomaAInglesYDejaDeSeguirElSistema() {
        when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        PreferenceRequest request = new PreferenceRequest();
        request.setLanguage("en");

        PreferenceResponse response = preferenceService.updatePreferences("user-1", request);

        assertThat(response.getLanguage()).isEqualTo("en");
        assertThat(response.getFollowSystemLanguage()).isFalse();
        assertThat(response.getVoiceLanguage()).isEqualTo("en-US");
    }

    @Test
    void cp2Hu21_retornaAEspanol() {
        UserPreference stored = UserPreference.builder()
                .userId("user-1")
                .language("en")
                .followSystemLanguage(false)
                .fontSize("medium")
                .build();
        when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(stored));

        PreferenceRequest request = new PreferenceRequest();
        request.setLanguage("es");

        PreferenceResponse response = preferenceService.updatePreferences("user-1", request);

        assertThat(response.getLanguage()).isEqualTo("es");
        assertThat(response.getFollowSystemLanguage()).isFalse();
    }

    @Test
    void cp1Hu22_detectaIdiomaDelSistemaEnAcceptLanguage() {
        when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        PreferenceResponse response = preferenceService.getPreferences("user-1", "en-US,en;q=0.9");

        assertThat(response.getLanguage()).isEqualTo("en");
        assertThat(response.getFollowSystemLanguage()).isTrue();
        assertThat(response.getVoiceLanguage()).isEqualTo("en-US");
    }

    @Test
    void cp2Hu22_permiteSeguirElSistemaEnTiempoReal() {
        UserPreference stored = UserPreference.builder()
                .userId("user-1")
                .language("es")
                .followSystemLanguage(false)
                .fontSize("medium")
                .build();
        when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(stored));

        PreferenceRequest request = new PreferenceRequest();
        request.setFollowSystemLanguage(true);
        request.setLanguage("en");

        PreferenceResponse response = preferenceService.updatePreferences("user-1", request);

        assertThat(response.getFollowSystemLanguage()).isTrue();
        assertThat(response.getLanguage()).isEqualTo("en");
    }

    @Test
    void cpHu23_persistePorUsuarioTrasCerrarSesionOCambiarDispositivo() {
        when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        PreferenceRequest request = new PreferenceRequest();
        request.setLanguage("en");
        request.setHighContrast(true);
        request.setFontSize("large");
        preferenceService.updatePreferences("user-1", request);

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(preferenceRepository, atLeastOnce()).save(captor.capture());
        UserPreference saved = captor.getValue();

        when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.of(saved));

        PreferenceResponse afterRelogin = preferenceService.getPreferences("user-1");
        assertThat(afterRelogin.getLanguage()).isEqualTo("en");
        assertThat(afterRelogin.getHighContrast()).isTrue();
        assertThat(afterRelogin.getFontSize()).isEqualTo("large");
        assertThat(afterRelogin.getUserId()).isEqualTo("user-1");
    }

    @Test
    void cpHu24_canalesDeAlertaVisualYVoz() {
        when(preferenceRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        PreferenceRequest request = new PreferenceRequest();
        request.setNotificationsEnabled(true);
        request.setTtsEnabled(true);
        PreferenceResponse response = preferenceService.updatePreferences("user-1", request);

        assertThat(response.getNotificationPreferences()).containsEntry("visual", true);
        assertThat(response.getNotificationPreferences()).containsEntry("voice", true);
        assertThat(response.getTtsEnabled()).isTrue();
        assertThat(response.getNotificationsEnabled()).isTrue();
    }

    @Test
    void languageFromAccept_priorizaIngles() {
        assertThat(PreferenceService.languageFromAccept("en-GB,es;q=0.8")).isEqualTo("en");
        assertThat(PreferenceService.languageFromAccept("es-CO")).isEqualTo("es");
        assertThat(PreferenceService.languageFromAccept(null)).isEqualTo("es");
    }
}
