package com.gametrend.agent.youtube.dto;

public record YoutubeCommentCollectResponse(
        String keyword,
        String status,
        String message,
        int targetVideoCount,
        int collectedCommentCount,
        YoutubeCommentReactionSummaryResponse reactionSummary
) {
}
