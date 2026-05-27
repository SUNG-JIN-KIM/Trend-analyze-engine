package com.gametrend.agent.youtube.dto;

import java.util.List;

public record YoutubeTrendResponse(
        GameYoutubeTrendScoreResponse score,
        List<YoutubeVideoResponse> videos,
        YoutubeCommentReactionSummaryResponse commentReactionSummary
) {
}
