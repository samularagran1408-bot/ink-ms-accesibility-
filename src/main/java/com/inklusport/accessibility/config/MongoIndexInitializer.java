package com.inklusport.accessibility.config;

import com.inklusport.accessibility.model.Notification;
import com.inklusport.accessibility.model.UserPreference;
import com.mongodb.client.MongoCollection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class MongoIndexInitializer implements ApplicationRunner {

    private static final String[] UNUSED_COLLECTIONS = {
            "notification_log",
            "notification_preferences"
    };

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureNotificationIndexes();
        ensurePreferenceIndexes();
        dropUnusedEmptyCollections();
    }

    private void ensureNotificationIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Notification.class);
        recreateIndex(ops, "user_read_idx",
                new Index().on("user_id", Sort.Direction.ASC).on("read", Sort.Direction.ASC).named("user_read_idx"));
        recreateIndex(ops, "user_created_idx",
                new Index().on("user_id", Sort.Direction.ASC).on("created_at", Sort.Direction.DESC).named("user_created_idx"));
        recreateIndex(ops, "expires_at_ttl",
                new Index().on("expires_at", Sort.Direction.ASC).named("expires_at_ttl").expire(0, TimeUnit.SECONDS));
    }

    private void ensurePreferenceIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(UserPreference.class);
        try {
            ops.ensureIndex(new Index().on("userId", Sort.Direction.ASC).unique().named("userId_unique"));
        } catch (Exception ex) {
            log.debug("Índice userId_unique ya existía o no se pudo crear: {}", ex.getMessage());
        }
    }

    private void recreateIndex(IndexOperations ops, String name, Index index) {
        try {
            ops.dropIndex(name);
        } catch (Exception ignored) {
            // el índice todavía no existe
        }
        try {
            ops.ensureIndex(index);
            log.info("Índice Mongo asegurado: {}", name);
        } catch (Exception ex) {
            log.warn("No se pudo crear el índice {}: {}", name, ex.getMessage());
        }
    }

    private void dropUnusedEmptyCollections() {
        for (String name : UNUSED_COLLECTIONS) {
            if (!mongoTemplate.collectionExists(name)) {
                continue;
            }
            MongoCollection<Document> collection = mongoTemplate.getCollection(name);
            if (collection.estimatedDocumentCount() == 0) {
                mongoTemplate.dropCollection(name);
                log.info("Colección Mongo vacía y sin uso eliminada: {}", name);
            }
        }
    }
}
