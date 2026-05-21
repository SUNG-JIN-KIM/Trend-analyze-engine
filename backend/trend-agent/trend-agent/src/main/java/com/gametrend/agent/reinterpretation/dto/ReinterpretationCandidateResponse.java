package com.gametrend.agent.reinterpretation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReinterpretationCandidateResponse(
        Long id,
        Long legacyGameId,
        String title,
        String source,
        String sourceGameId,
        Integer steamAppId,
        int releaseYear,
        List<String> genres,
        List<String> tags,
        List<String> mechanics,
        List<String> interactionHints,
        int legacyPopularityScore,
        int reviewSentimentScore,
        int mechanicUniquenessScore,
        int streamabilityScore,
        int interactionFitScore,
        int modernTrendFitScore,
        int devFeasibilityScore,
        double reinterpretationScore,
        String reinterpretationConcept,
        String reason,
        String dataOrigin,
        int reviewCount,
        double positiveReviewRate,
        List<String> matchedLiveTrendSources,
        LocalDateTime updatedAt
) {
}
