package com.gametrend.agent.youtube.dto;

public record YoutubeCollectResponse(
        String keyword,
        String status,
        String message,
        GameYoutubeTrendScoreResponse score
) {
}
