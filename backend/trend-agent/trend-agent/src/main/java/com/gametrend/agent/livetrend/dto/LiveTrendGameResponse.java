package com.gametrend.agent.livetrend.dto;

import com.gametrend.agent.gameimage.GameImageResolver;
import com.gametrend.agent.livetrend.entity.LiveTrendGame;

import java.time.LocalDateTime;

public record LiveTrendGameResponse(
        Long id,
        String source,
        String title,
        String genre,
        String platform,
        String sourceKeyword,
        int liveStreamCount,
        int totalViewerCount,
        int viewerScore,
        int streamCountScore,
        int streamabilityScore,
        int marketSignalScore,
        double trendScore,
        String signalStatus,
        String dataOrigin,
        String reason,
        LocalDateTime updatedAt,
        String imageUrl
) {

    public LiveTrendGameResponse(
            Long id,
            String source,
            String title,
            String genre,
            String platform,
            String sourceKeyword,
            int liveStreamCount,
            int totalViewerCount,
            int viewerScore,
            int streamCountScore,
            int streamabilityScore,
            int marketSignalScore,
            double trendScore,
            String signalStatus,
            String dataOrigin,
            String reason,
            LocalDateTime updatedAt
    ) {
        this(
                id,
                source,
                title,
                genre,
                platform,
                sourceKeyword,
                liveStreamCount,
                totalViewerCount,
                viewerScore,
                streamCountScore,
                streamabilityScore,
                marketSignalScore,
                trendScore,
                signalStatus,
                dataOrigin,
                reason,
                updatedAt,
                GameImageResolver.resolveImageUrl(title, sourceKeyword, null)
        );
    }

    public static LiveTrendGameResponse from(LiveTrendGame liveTrendGame) {
        return new LiveTrendGameResponse(
                liveTrendGame.getId(),
                liveTrendGame.getSource(),
                liveTrendGame.getTitle(),
                liveTrendGame.getGenre(),
                liveTrendGame.getPlatform(),
                liveTrendGame.getSourceKeyword(),
                liveTrendGame.getLiveStreamCount(),
                liveTrendGame.getTotalViewerCount(),
                liveTrendGame.getViewerScore(),
                liveTrendGame.getStreamCountScore(),
                liveTrendGame.getStreamabilityScore(),
                liveTrendGame.getMarketSignalScore(),
                liveTrendGame.getTrendScore(),
                liveTrendGame.getSignalStatus(),
                liveTrendGame.getDataOrigin(),
                liveTrendGame.getReason(),
                liveTrendGame.getUpdatedAt()
        );
    }
}
