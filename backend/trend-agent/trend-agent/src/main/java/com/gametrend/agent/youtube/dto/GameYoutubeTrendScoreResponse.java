package com.gametrend.agent.youtube.dto;

import com.gametrend.agent.youtube.entity.GameYoutubeTrendScore;

import java.time.LocalDateTime;

public record GameYoutubeTrendScoreResponse(
        Long gameId,
        String keyword,
        String gameTitle,
        long totalViewCount,
        double averageViewCount,
        long totalLikeCount,
        long totalCommentCount,
        double averageEngagementRate,
        int videoCount,
        double viewScore,
        double engagementScore,
        double volumeScore,
        double youtubeInterestScore,
        LocalDateTime collectedAt
) {

    public static GameYoutubeTrendScoreResponse from(GameYoutubeTrendScore score) {
        return new GameYoutubeTrendScoreResponse(
                score.getGameId(),
                score.getKeyword(),
                score.getGameTitle(),
                score.getTotalViewCount(),
                score.getAverageViewCount(),
                score.getTotalLikeCount(),
                score.getTotalCommentCount(),
                score.getAverageEngagementRate(),
                score.getVideoCount(),
                score.getViewScore(),
                score.getEngagementScore(),
                score.getVolumeScore(),
                score.getYoutubeInterestScore(),
                score.getCollectedAt()
        );
    }
}
