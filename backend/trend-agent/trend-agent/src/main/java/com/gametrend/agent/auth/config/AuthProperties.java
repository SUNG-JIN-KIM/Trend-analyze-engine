package com.gametrend.agent.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        String jwtSecret,
        long jwtExpirationMs
) {

    public AuthProperties {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            jwtSecret = "local-dev-secret-change-me";
        }
        if (jwtExpirationMs <= 0) {
            jwtExpirationMs = 86_400_000L;
        }
    }
}
