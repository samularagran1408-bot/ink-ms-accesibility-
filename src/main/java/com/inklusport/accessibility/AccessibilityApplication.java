package com.inklusport.accessibility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/** Este microservicio funciona */

@SpringBootApplication
@EnableMongoAuditing
@EnableAsync
public class AccessibilityApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccessibilityApplication.class, args);
    }
}