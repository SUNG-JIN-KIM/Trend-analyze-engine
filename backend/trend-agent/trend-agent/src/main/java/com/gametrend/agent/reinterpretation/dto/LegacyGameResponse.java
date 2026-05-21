package com.gametrend.agent.reinterpretation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record LegacyGameResponse(
        Long id,
        String title,
        String source,
        String sourceGameId,
        Integer steamAppId,
        int releaseYear,
        List<String> genres,
        List<String> tags,
        List<String> mechanics,
        List<String> interactionHints,
        int devFeasibilityScore,
        int reviewCount,
        double positiveReviewRate,
        int legacyPopularityScore,
        int reviewSentimentScore,
        String dataOrigin,
        String reason,
        LocalDateTime updatedAt
) {
}
