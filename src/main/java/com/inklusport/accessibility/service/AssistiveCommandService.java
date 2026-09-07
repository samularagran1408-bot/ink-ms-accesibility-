package com.inklusport.accessibility.service;

import com.inklusport.accessibility.dto.CommandDefinition;
import com.inklusport.accessibility.dto.CommandLogRequest;
import com.inklusport.accessibility.dto.InterpretRequest;
import com.inklusport.accessibility.dto.InterpretResponse;
import com.inklusport.accessibility.model.CommandLog;
import com.inklusport.accessibility.repository.CommandLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Interpreta comandos de voz y registra su uso.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssistiveCommandService {

    private final AssistiveCommandCatalog catalog;
    private final CommandLogRepository commandLogRepository;

    /** Devuelve el catálogo de comandos de voz disponibles. */
    public List<CommandDefinition> voiceCommands() {
        return catalog.all();
    }

    /** Interpreta el texto de voz y opcionalmente lo registra. */
    public InterpretResponse interpretVoice(String userId, InterpretRequest request) {
        Optional<AssistiveCommandCatalog.Match> match = catalog.matchVoice(request.getInput());
        InterpretResponse response = match
                .map(m -> toResponse(m.command(), m.confidence(), "voice"))
                .orElseGet(() -> InterpretResponse.builder()
                        .matched(false)
                        .modality("voice")
                        .confidence(0.0)
                        .feedback("No reconocí ese comando. Di «ayuda» para escuchar las opciones.")
                        .build());

        if (Boolean.TRUE.equals(request.getLog())) {
            persist(userId, "voice", request.getInput(), response, false);
        }
        return response;
    }

    /** Guarda un registro explícito de comando ejecutado. */
    public void logCommand(String userId, CommandLogRequest request) {
        commandLogRepository.save(CommandLog.builder()
                .userId(resolveUser(userId))
                .modality(request.getModality())
                .input(request.getInput())
                .action(request.getAction())
                .route(request.getRoute())
                .confidence(request.getConfidence())
                .executed(Boolean.TRUE.equals(request.getExecuted()))
                .build());
    }

    /** Lista los últimos comandos del usuario. */
    public List<CommandLog> recent(String userId) {
        return commandLogRepository.findTop20ByUserIdOrderByCreatedAtDesc(resolveUser(userId));
    }

    /** Construye la respuesta de un comando reconocido. */
    private InterpretResponse toResponse(CommandDefinition cmd, double confidence, String modality) {
        return InterpretResponse.builder()
                .matched(true)
                .action(cmd.getAction())
                .label(cmd.getLabel())
                .route(cmd.getRoute())
                .confidence(confidence)
                .feedback(cmd.getLabel())
                .modality(modality)
                .build();
    }

    /** Persiste el intento de comando sin fallar al caller. */
    private void persist(String userId, String modality, String input, InterpretResponse response, boolean executed) {
        try {
            commandLogRepository.save(CommandLog.builder()
                    .userId(resolveUser(userId))
                    .modality(modality)
                    .input(input)
                    .action(response.getAction())
                    .route(response.getRoute())
                    .confidence(response.getConfidence())
                    .executed(executed)
                    .build());
        } catch (Exception e) {
            log.warn("No se pudo guardar el log de comando: {}", e.getMessage());
        }
    }

    /** Normaliza el identificador de usuario o usa anónimo. */
    private String resolveUser(String userId) {
        return (userId == null || userId.isBlank()) ? "anonymous" : userId;
    }
}
