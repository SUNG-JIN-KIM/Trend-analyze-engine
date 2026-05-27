package com.gametrend.agent.youtube.dto;

import java.util.List;

public record YoutubeDashboardResponse(
        List<YoutubeCollectLogResponse> recentLogs,
        List<GameYoutubeTrendScoreResponse> topGames
) {
}
