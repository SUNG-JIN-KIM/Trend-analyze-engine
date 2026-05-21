package com.gametrend.agent.trend.dto;

public record TrendSeedGameRequest(
        String title,
        String genre,
        String platform,
        Integer steamAppId,
        String twitchKeyword
) {
}
