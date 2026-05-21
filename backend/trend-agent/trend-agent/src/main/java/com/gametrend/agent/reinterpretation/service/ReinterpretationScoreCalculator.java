package com.gametrend.agent.reinterpretation.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class ReinterpretationScoreCalculator {

    public int legacyPopularityScore(int reviewCount) {
        if (reviewCount <= 0) {
            return 45;
        }
        return clamp((int) Math.round(35 + Math.log10(reviewCount) * 11));
    }

    public int reviewSentimentScore(double positiveReviewRate) {
        return clamp((int) Math.round(positiveReviewRate * 100));
    }

    public int interactionFitScore(LegacyGameSeed seed) {
        int score = seed.interactionFitSeedScore();
        String joined = String.join(" ", seed.mechanics()) + " " + String.join(" ", seed.tags());
        String normalized = joined.toLowerCase(Locale.ROOT);
        score += keywordBonus(normalized, "voice", 5);
        score += keywordBonus(normalized, "co-op", 4);
        score += keywordBonus(normalized, "deduction", 5);
        score += keywordBonus(normalized, "horror", 4);
        score += keywordBonus(normalized, "party", 4);
        score += keywordBonus(normalized, "reaction", 5);
        score += keywordBonus(normalized, "chat", 4);
        score += keywordBonus(normalized, "asymmetric", 4);
        return clamp(score);
    }

    public int modernTrendFitScore(LegacyGameSeed seed, List<String> liveTrendTokens) {
        if (liveTrendTokens == null || liveTrendTokens.isEmpty()) {
            return 55;
        }
        int matches = 0;
        for (String token : seedTokens(seed)) {
            if (liveTrendTokens.stream().anyMatch(live -> live.contains(token) || token.contains(live))) {
                matches++;
            }
        }
        return clamp(50 + matches * 8);
    }

    public double reinterpretationScore(
            int legacyPopularityScore,
            int reviewSentimentScore,
            int mechanicUniquenessScore,
            int streamabilityScore,
            int interactionFitScore,
            int modernTrendFitScore,
            int devFeasibilityScore
    ) {
        double score = legacyPopularityScore * 0.20
                + reviewSentimentScore * 0.15
                + mechanicUniquenessScore * 0.20
                + streamabilityScore * 0.15
                + interactionFitScore * 0.15
                + modernTrendFitScore * 0.10
                + devFeasibilityScore * 0.05;
        return Math.round(score * 10.0) / 10.0;
    }

    List<String> seedTokens(LegacyGameSeed seed) {
        return java.util.stream.Stream.of(seed.genres(), seed.tags(), seed.mechanics())
                .flatMap(List::stream)
                .map(value -> value.toLowerCase(Locale.ROOT).strip())
                .filter(value -> !value.isBlank())
                .toList();
    }

    private int keywordBonus(String value, String keyword, int bonus) {
        return value.contains(keyword) ? bonus : 0;
    }

    private int clamp(int value) {
        return Math.min(100, Math.max(0, value));
    }
}
