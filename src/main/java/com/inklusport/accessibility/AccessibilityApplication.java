package com.inklusport.accessibility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/** Este microservicio funciona */

@SpringBootApplication
@EnableMongoAuditing
public class AccessibilityApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccessibilityApplication.class, args);
    }
}