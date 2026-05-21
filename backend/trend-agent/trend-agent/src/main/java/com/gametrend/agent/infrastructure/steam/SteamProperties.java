package com.gametrend.agent.infrastructure.steam;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "steam")
public record SteamProperties(
        String baseUrl,
        long timeoutMs,
        String apiKey
) {

    public SteamProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://store.steampowered.com";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 10_000L;
        }
        if (apiKey == null) {
            apiKey = "";
        }
    }
}
