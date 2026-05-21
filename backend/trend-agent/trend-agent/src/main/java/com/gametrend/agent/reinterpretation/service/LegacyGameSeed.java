package com.gametrend.agent.reinterpretation.service;

import java.util.List;

record LegacyGameSeed(
        String title,
        String source,
        String sourceGameId,
        Integer steamAppId,
        int releaseYear,
        List<String> genres,
        List<String> tags,
        List<String> mechanics,
        List<String> interactionHints,
        int mechanicUniquenessScore,
        int streamabilityScore,
        int interactionFitSeedScore,
        int devFeasibilityScore,
        int fallbackReviewCount,
        double fallbackPositiveReviewRate
) {
}
