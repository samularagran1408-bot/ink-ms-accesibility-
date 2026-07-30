package com.inklusport.accessibility.repository;

import com.inklusport.accessibility.model.CommandLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommandLogRepository extends MongoRepository<CommandLog, String> {
    List<CommandLog> findTop20ByUserIdOrderByCreatedAtDesc(String userId);
}
