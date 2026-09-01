package com.inklusport.accessibility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Este microservicio funciona */

@SpringBootApplication
@EnableMongoAuditing
@EnableAsync
@EnableScheduling
public class AccessibilityApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccessibilityApplication.class, args);
    }
}