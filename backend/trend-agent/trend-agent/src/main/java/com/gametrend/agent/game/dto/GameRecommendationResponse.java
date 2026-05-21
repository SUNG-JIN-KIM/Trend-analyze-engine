package com.gametrend.agent.game.dto;

import com.gametrend.agent.game.entity.Game;

public record GameRecommendationResponse(
        int rank,
        Long id,
        String title,
        String genre,
        String platform,
        String playStyle,
        double recommendationScore,
        String summaryReason
) {

    public static GameRecommendationResponse of(int rank, Game game) {
        return new GameRecommendationResponse(
                rank,
                game.getId(),
                game.getTitle(),
                game.getGenre(),
                game.getPlatform(),
                game.getPlayStyle(),
                game.getRecommendationScore(),
                game.getReason()
        );
    }
}
