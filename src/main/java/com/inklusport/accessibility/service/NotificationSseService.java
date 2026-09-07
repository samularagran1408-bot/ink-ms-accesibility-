package com.inklusport.accessibility.service;

import com.inklusport.accessibility.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Empuja notificaciones in-app a los clientes conectados (SSE).
 * La clave es el email del JWT, que coincide con el userId de almacenamiento.
 */
@Service
@Slf4j
public class NotificationSseService {

    private static final long NEVER_TIMEOUT = 0L;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /** Abre una conexión SSE para recibir notificaciones. */
    public SseEmitter subscribe(String userId) {
        String key = key(userId);
        SseEmitter emitter = new SseEmitter(NEVER_TIMEOUT);
        emitters.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(key, emitter));
        emitter.onTimeout(() -> remove(key, emitter));
        emitter.onError(error -> remove(key, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception e) {
            remove(key, emitter);
        }
        log.debug("SSE notificaciones: cliente conectado para {}", key);
        return emitter;
    }

    /** Empuja la notificación a los clientes SSE conectados. */
    public void push(String userId, NotificationResponse notification) {
        String key = key(userId);
        if (key.isEmpty() || notification == null) {
            return;
        }
        CopyOnWriteArrayList<SseEmitter> live = emitters.get(key);
        if (live == null || live.isEmpty()) {
            return;
        }
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : live) {
            try {
                emitter.send(SseEmitter.event()
                        .id(notification.getId())
                        .name("notification")
                        .data(notification, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        dead.forEach(emitter -> remove(key, emitter));
    }

    /** Envía un ping periódico para detectar conexiones muertas. */
    @Scheduled(fixedRate = 15_000)
    public void heartbeat() {
        for (Map.Entry<String, CopyOnWriteArrayList<SseEmitter>> entry : emitters.entrySet()) {
            List<SseEmitter> dead = new ArrayList<>();
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    dead.add(emitter);
                }
            }
            dead.forEach(emitter -> remove(entry.getKey(), emitter));
        }
    }

    /** Quita un emisor y cierra su conexión. */
    private void remove(String key, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> live = emitters.get(key);
        if (live == null) {
            return;
        }
        live.remove(emitter);
        if (live.isEmpty()) {
            emitters.remove(key, live);
        }
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // ya cerrado
        }
    }

    /** Normaliza la clave de usuario para el mapa SSE. */
    private static String key(String userId) {
        return userId == null ? "" : userId.trim().toLowerCase();
    }
}
