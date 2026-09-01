package com.inklusport.accessibility.service;

import com.inklusport.accessibility.model.CommandLog;
import com.inklusport.accessibility.model.Notification;
import com.mongodb.client.result.DeleteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Limpia datos efímeros. No toca preferencias ni perfiles.
 * Notificaciones: 4 h si no se leyeron, 2 h después de marcarlas leídas.
 * Logs de comandos de voz: 4 h.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRetentionService {

    private final MongoTemplate mongoTemplate;

    @Scheduled(initialDelay = 15_000, fixedDelay = 900_000)
    public void purgeExpired() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime unreadCutoff = now.minusHours(NotificationService.HOURS_IF_UNREAD);
        LocalDateTime readCutoff = now.minusHours(NotificationService.HOURS_AFTER_READ);

        DeleteResult unread = mongoTemplate.remove(
                Query.query(new Criteria().andOperator(
                        Criteria.where("read").ne(true),
                        Criteria.where("created_at").lt(unreadCutoff)
                )),
                Notification.class
        );
        DeleteResult read = mongoTemplate.remove(
                Query.query(new Criteria().andOperator(
                        Criteria.where("read").is(true),
                        new Criteria().orOperator(
                                Criteria.where("read_at").lt(readCutoff),
                                new Criteria().andOperator(
                                        Criteria.where("read_at").exists(false),
                                        Criteria.where("created_at").lt(readCutoff)
                                )
                        )
                )),
                Notification.class
        );
        DeleteResult ttl = mongoTemplate.remove(
                Query.query(Criteria.where("expires_at").lt(now)),
                Notification.class
        );
        DeleteResult commands = mongoTemplate.remove(
                Query.query(new Criteria().orOperator(
                        Criteria.where("createdAt").lt(unreadCutoff),
                        Criteria.where("created_at").lt(unreadCutoff)
                )),
                CommandLog.class
        );

        long notifications = unread.getDeletedCount() + read.getDeletedCount() + ttl.getDeletedCount();
        long logs = commands.getDeletedCount();
        if (notifications + logs > 0) {
            log.info(
                    "Retención: {} notificaciones ({} no leídas, {} leídas, {} TTL) y {} command_logs",
                    notifications,
                    unread.getDeletedCount(),
                    read.getDeletedCount(),
                    ttl.getDeletedCount(),
                    logs
            );
        }
    }
}
