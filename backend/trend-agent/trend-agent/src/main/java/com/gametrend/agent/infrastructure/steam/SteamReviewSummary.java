package com.gametrend.agent.infrastructure.steam;

public record SteamReviewSummary(
        int appId,
        String reviewScoreDesc,
        int totalPositive,
        int totalNegative,
        int totalReviews,
        double positiveRate
) {
}
