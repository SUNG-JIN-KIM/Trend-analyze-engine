package com.gametrend.agent.livetrend.dto;

import com.gametrend.agent.gameimage.GameImageResolver;
import com.gametrend.agent.livetrend.entity.LiveTrendGame;

import java.time.LocalDateTime;

public record LiveTrendRankingResponse(
        int rank,
        String title,
        String source,
        String genre,
        int liveStreamCount,
        int totalViewerCount,
        double trendScore,
        int viewerScore,
        int streamCountScore,
        String signalStatus,
        String dataOrigin,
        String reason,
        LocalDateTime updatedAt,
        String imageUrl
) {

    public LiveTrendRankingResponse(
            int rank,
            String title,
            String source,
            String genre,
            int liveStreamCount,
            int totalViewerCount,
            double trendScore,
            int viewerScore,
            int streamCountScore,
            String signalStatus,
            String dataOrigin,
            String reason,
            LocalDateTime updatedAt
    ) {
        this(
                rank,
                title,
                source,
                genre,
                liveStreamCount,
                totalViewerCount,
                trendScore,
                viewerScore,
                streamCountScore,
                signalStatus,
                dataOrigin,
                reason,
                updatedAt,
                GameImageResolver.resolveImageUrl(title, null, null)
        );
    }

    public static LiveTrendRankingResponse from(int rank, LiveTrendGame game) {
        return new LiveTrendRankingResponse(
                rank,
                game.getTitle(),
                game.getSource(),
                game.getGenre(),
                game.getLiveStreamCount(),
                game.getTotalViewerCount(),
                game.getTrendScore(),
                game.getViewerScore(),
                game.getStreamCountScore(),
                game.getSignalStatus(),
                game.getDataOrigin(),
                game.getReason(),
                game.getUpdatedAt(),
                GameImageResolver.resolveImageUrl(game.getTitle(), game.getSourceKeyword(), null)
        );
    }
}
