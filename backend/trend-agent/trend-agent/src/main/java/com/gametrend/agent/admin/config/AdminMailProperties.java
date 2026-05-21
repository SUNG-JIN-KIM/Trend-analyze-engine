package com.gametrend.agent.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin.mail")
public record AdminMailProperties(
        boolean enabled,
        String from
) {

    public AdminMailProperties {
        if (from != null && from.isBlank()) {
            from = null;
        }
    }
}
