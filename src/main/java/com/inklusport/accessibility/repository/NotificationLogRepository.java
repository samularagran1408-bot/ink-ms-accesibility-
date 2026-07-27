package com.inklusport.accessibility.repository;

import com.inklusport.accessibility.model.NotificationLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {
    List<NotificationLog> findByUserIdOrderBySentAtDesc(String userId);
    List<NotificationLog> findByNotificationId(String notificationId);
}