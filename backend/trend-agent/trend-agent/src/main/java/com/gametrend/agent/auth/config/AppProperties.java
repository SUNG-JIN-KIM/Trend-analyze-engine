package com.gametrend.agent.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl
) {

    public AppProperties {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            frontendUrl = "http://localhost:5173";
        }
        frontendUrl = frontendUrl.replaceAll("/+$", "");
    }
}
