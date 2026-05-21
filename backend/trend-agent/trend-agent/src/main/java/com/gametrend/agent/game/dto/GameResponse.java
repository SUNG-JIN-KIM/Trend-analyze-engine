package com.gametrend.agent.game.dto;

import com.gametrend.agent.game.entity.Game;

import java.time.LocalDateTime;

public record GameResponse(
        Long id,
        String title,
        String genre,
        String platform,
        String playStyle,
        int streamabilityScore,
        int webcamFitScore,
        int ttsFitScore,
        int sttFitScore,
        int noveltyScore,
        int devFeasibilityScore,
        int marketSignalScore,
        double recommendationScore,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static GameResponse from(Game game) {
        return new GameResponse(
                game.getId(),
                game.getTitle(),
                game.getGenre(),
                game.getPlatform(),
                game.getPlayStyle(),
                game.getStreamabilityScore(),
                game.getWebcamFitScore(),
                game.getTtsFitScore(),
                game.getSttFitScore(),
                game.getNoveltyScore(),
                game.getDevFeasibilityScore(),
                game.getMarketSignalScore(),
                game.getRecommendationScore(),
                game.getReason(),
                game.getCreatedAt(),
                game.getUpdatedAt()
        );
    }
}
