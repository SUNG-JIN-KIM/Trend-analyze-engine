package com.gametrend.agent.youtube.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "youtube.api")
public record YoutubeProperties(
        String key,
        String baseUrl,
        int maxResults,
        String regionCode,
        String relevanceLanguage,
        long collectCooldownMinutes
) {

    public YoutubeProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://www.googleapis.com/youtube/v3";
        }
        if (maxResults <= 0) {
            maxResults = 10;
        }
        if (maxResults > 50) {
            maxResults = 50;
        }
        if (regionCode == null || regionCode.isBlank()) {
            regionCode = "KR";
        }
        if (relevanceLanguage == null || relevanceLanguage.isBlank()) {
            relevanceLanguage = "ko";
        }
        if (collectCooldownMinutes <= 0) {
            collectCooldownMinutes = 360;
        }
    }

    public boolean hasApiKey() {
        return key != null && !key.isBlank();
    }
}
