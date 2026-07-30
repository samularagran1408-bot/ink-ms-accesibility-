package com.inklusport.accessibility.service;

import com.inklusport.accessibility.dto.CommandDefinition;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Catálogo de comandos de navegación y accesibilidad para voz.
 */
@Component
public class AssistiveCommandCatalog {

    private final List<CommandDefinition> commands = List.of(
            CommandDefinition.builder()
                    .action("navigate.home")
                    .label("Ir al inicio")
                    .description("Abre el panel principal")
                    .route("/home")
                    .phrases(List.of(
                            "ir a inicio", "ir al inicio", "abrir inicio", "abrir home",
                            "ir a home", "página principal", "pantalla principal", "inicio"
                    ))
                    .build(),
            CommandDefinition.builder()
                    .action("navigate.events")
                    .label("Ir a eventos")
                    .description("Lista de eventos y competencias")
                    .route("/events")
                    .phrases(List.of(
                            "ir a eventos", "abrir eventos", "ver eventos", "eventos",
                            "competencias", "mostrar eventos"
                    ))
                    .build(),
            CommandDefinition.builder()
                    .action("navigate.calendar")
                    .label("Ir al calendario")
                    .description("Calendario de actividades")
                    .route("/calendar")
                    .phrases(List.of(
                            "ir a calendario", "abrir calendario", "ver calendario", "calendario"
                    ))
                    .build(),
            CommandDefinition.builder()
                    .action("navigate.progress")
                    .label("Ir a mi progreso")
                    .description("Estadísticas y avance")
                    .route("/progress")
                    .phrases(List.of(
                            "ir a progreso", "mi progreso", "ver progreso", "estadísticas",
                            "mis estadisticas", "rendimiento"
                    ))
                    .build(),
            CommandDefinition.builder()
                    .action("navigate.profile")
                    .label("Ir al perfil")
                    .description("Datos de la cuenta")
                    .route("/profile")
                    .phrases(List.of(
                            "ir a perfil", "abrir perfil", "mi perfil", "ver perfil", "perfil"
                    ))
                    .build(),
            CommandDefinition.builder()
                    .action("navigate.accessibility")
                    .label("Ir a accesibilidad")
                    .description("Preferencias de accesibilidad")
                    .route("/accessibility")
                    .phrases(List.of(
                            "ir a accesibilidad", "abrir accesibilidad", "accesibilidad",
                            "configuración de accesibilidad", "ajustes de accesibilidad"
                    ))
                    .build(),
            CommandDefinition.builder()
                    .action("navigate.ai")
                    .label("Abrir asistente IA")
                    .description("Chat del asistente")
                    .route("/ai-assistant")
                    .phrases(List.of(
                            "abrir asistente", "asistente ia", "asistente de ia",
                            "hablar con el asistente", "inteligencia artificial", "chat ia"
                    ))
                    .build(),
            CommandDefinition.builder()
                    .action("a11y.high_contrast")
                    .label("Alternar alto contraste")
                    .description("Activa o desactiva el alto contraste")
                    .route(null)
                    .phrases(List.of(
                            "alto contraste", "activar contraste", "desactivar contraste",
                            "contraste alto", "modo contraste"
                    ))
                    .build(),
            CommandDefinition.builder()
                    .action("a11y.increase_font")
                    .label("Aumentar texto")
                    .description("Incrementa el tamaño de fuente")
                    .route(null)
                    .phrases(List.of(
                            "aumentar texto", "texto más grande", "letra más grande",
                            "aumentar fuente", "agrandar texto"
                    ))
                    .build(),
            CommandDefinition.builder()
                    .action("a11y.decrease_font")
                    .label("Reducir texto")
                    .description("Reduce el tamaño de fuente")
                    .route(null)
                    .phrases(List.of(
                            "reducir texto", "texto más pequeño", "letra más pequeña",
                            "disminuir fuente", "achicar texto"
                    ))
                    .build(),
            CommandDefinition.builder()
                    .action("assist.stop")
                    .label("Detener escucha")
                    .description("Cierra el micrófono")
                    .route(null)
                    .phrases(List.of(
                            "detener", "parar", "cancelar", "detener escucha",
                            "apagar micrófono", "callar", "stop"
                    ))
                    .build(),
            CommandDefinition.builder()
                    .action("assist.help")
                    .label("Ayuda de comandos")
                    .description("Lee los comandos disponibles")
                    .route(null)
                    .phrases(List.of(
                            "ayuda", "qué puedo decir", "comandos disponibles",
                            "ayuda de voz"
                    ))
                    .build()
    );

    public List<CommandDefinition> all() {
        return commands;
    }

    public Optional<Match> matchVoice(String transcript) {
        String normalized = normalize(transcript);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        Match best = null;
        for (CommandDefinition cmd : commands) {
            for (String phrase : cmd.getPhrases()) {
                String p = normalize(phrase);
                if (p.isBlank()) continue;
                double score;
                if (normalized.equals(p)) {
                    score = 1.0;
                } else if (normalized.contains(p)) {
                    score = 0.82 + Math.min(0.15, p.length() / 80.0);
                } else if (p.contains(normalized) && normalized.length() >= 4) {
                    score = 0.7;
                } else {
                    continue;
                }
                if (best == null || score > best.confidence()) {
                    best = new Match(cmd, score);
                }
            }
        }
        return Optional.ofNullable(best);
    }

    public static String normalize(String raw) {
        if (raw == null) return "";
        String lower = raw.toLowerCase(Locale.ROOT).trim();
        String noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return noAccents.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    public record Match(CommandDefinition command, double confidence) {}
}
