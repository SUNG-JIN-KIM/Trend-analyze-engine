package com.gametrend.agent.trend.service;

import com.gametrend.agent.infrastructure.steam.SteamReviewSummary;
import org.springframework.stereotype.Component;

@Component
public class TrendScoreCalculator {

    public int steamReviewScore(SteamReviewSummary summary) {
        if (summary.totalReviews() <= 0) {
            return 40;
        }

        double reviewVolumeWeight = reviewVolumeWeight(summary.totalReviews());
        return clamp((int) Math.round(summary.positiveRate() * 100.0 * reviewVolumeWeight));
    }

    public int twitchViewerScore(int viewerCount) {
        return logNormalize(viewerCount, 120_000);
    }

    public int twitchStreamCountScore(int streamCount) {
        return logNormalize(streamCount, 3_000);
    }

    public int streamabilityScore(int twitchViewerScore, int twitchStreamCountScore) {
        return clamp((int) Math.round(twitchViewerScore * 0.55 + twitchStreamCountScore * 0.45));
    }

    public int marketSignalScore(int steamReviewScore, int twitchViewerScore, int twitchStreamCountScore) {
        return clamp((int) Math.round(
                steamReviewScore * 0.60
                        + twitchViewerScore * 0.25
                        + twitchStreamCountScore * 0.15
        ));
    }

    public double trendScore(
            int steamReviewScore,
            int twitchViewerScore,
            int twitchStreamCountScore,
            double internalRecommendationScore
    ) {
        double score = steamReviewScore * 0.35
                + twitchViewerScore * 0.25
                + twitchStreamCountScore * 0.20
                + clamp(internalRecommendationScore) * 0.20;

        return roundToOneDecimal(score);
    }

    private int logNormalize(int value, int maxReference) {
        if (value <= 0) {
            return 0;
        }

        double score = Math.log10(value + 1.0) / Math.log10(maxReference + 1.0) * 100.0;
        return clamp((int) Math.round(score));
    }

    private double reviewVolumeWeight(int totalReviews) {
        if (totalReviews >= 100_000) {
            return 1.0;
        }
        if (totalReviews >= 10_000) {
            return 0.96;
        }
        if (totalReviews >= 1_000) {
            return 0.90;
        }
        if (totalReviews >= 100) {
            return 0.78;
        }
        return 0.60;
    }

    private int clamp(double score) {
        return (int) Math.min(100, Math.max(0, Math.round(score)));
    }

    private double roundToOneDecimal(double score) {
        return Math.round(score * 10.0) / 10.0;
    }
}
