package com.gametrend.agent.trend.dto;

import com.gametrend.agent.trend.entity.TrendGame;

import java.time.LocalDateTime;

public record TrendGameResponse(
        Long id,
        String title,
        String genre,
        String platform,
        Integer steamAppId,
        int steamReviewScore,
        int steamTotalReviews,
        double steamPositiveRate,
        int twitchLiveStreamCount,
        int twitchTotalViewerCount,
        int twitchViewerScore,
        int twitchStreamCountScore,
        int streamabilityScore,
        int marketSignalScore,
        double internalRecommendationScore,
        double trendScore,
        String signalStatus,
        String reason,
        LocalDateTime updatedAt
) {

    public static TrendGameResponse from(TrendGame trendGame) {
        return new TrendGameResponse(
                trendGame.getId(),
                trendGame.getTitle(),
                trendGame.getGenre(),
                trendGame.getPlatform(),
                trendGame.getSteamAppId(),
                trendGame.getSteamReviewScore(),
                trendGame.getSteamTotalReviews(),
                trendGame.getSteamPositiveRate(),
                trendGame.getTwitchLiveStreamCount(),
                trendGame.getTwitchTotalViewerCount(),
                trendGame.getTwitchViewerScore(),
                trendGame.getTwitchStreamCountScore(),
                trendGame.getStreamabilityScore(),
                trendGame.getMarketSignalScore(),
                trendGame.getInternalRecommendationScore(),
                trendGame.getTrendScore(),
                trendGame.getSignalStatus(),
                trendGame.getReason(),
                trendGame.getUpdatedAt()
        );
    }
}
