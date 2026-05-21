package com.gametrend.agent.steam.dto;

import com.gametrend.agent.infrastructure.steam.SteamReviewSummary;

public record SteamReviewResponse(
        int appId,
        String reviewScoreDesc,
        int totalPositive,
        int totalNegative,
        int totalReviews,
        double positiveRate,
        int marketSignalScore
) {

    public static SteamReviewResponse of(SteamReviewSummary summary, int marketSignalScore) {
        return new SteamReviewResponse(
                summary.appId(),
                summary.reviewScoreDesc(),
                summary.totalPositive(),
                summary.totalNegative(),
                summary.totalReviews(),
                summary.positiveRate(),
                marketSignalScore
        );
    }
}
