package com.gametrend.agent.youtube.dto;

import java.util.List;

public record YoutubeCommentReactionSummaryResponse(
        int positiveMentionCount,
        int negativeMentionCount,
        List<YoutubeKeywordStatResponse> topPositiveKeywords,
        List<YoutubeKeywordStatResponse> topNegativeKeywords,
        String summary
) {
}
