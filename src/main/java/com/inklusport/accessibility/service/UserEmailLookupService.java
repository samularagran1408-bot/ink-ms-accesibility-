package com.inklusport.accessibility.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Resuelve UUID de usuario a email real (users-ms) para in-app + Gmail.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserEmailLookupService {

    private final RestTemplate restTemplate;

    @Value("${users.service.url:http://localhost:3002}")
    private String usersServiceUrl;

    public String resolveEmail(String userIdOrEmail) {
        if (userIdOrEmail == null || userIdOrEmail.isBlank()) {
            return null;
        }
        String value = userIdOrEmail.trim();
        if (value.contains("@")) {
            return value;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> user = restTemplate.getForObject(
                    usersServiceUrl + "/api/internal/users/{id}",
                    Map.class,
                    value
            );
            if (user == null) {
                return null;
            }
            Object email = user.get("email");
            if (email == null) {
                return null;
            }
            String resolved = String.valueOf(email).trim();
            if (resolved.contains("@") && !resolved.startsWith("no-disponible")) {
                return resolved;
            }
        } catch (Exception e) {
            log.warn("No se pudo resolver email para {}: {}", value, e.getMessage());
        }
        return null;
    }
}
