package com.gametrend.agent.youtube.dto;

public record YoutubeDashboardSummaryResponse(
        long totalVideoCount,
        long todayCollectCount,
        long successCollectCount,
        long failureCollectCount,
        String topGameKeyword,
        String latestCollectKeyword
) {
}
