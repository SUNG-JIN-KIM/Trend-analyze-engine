package com.gametrend.agent.trend.service;

import com.gametrend.agent.infrastructure.steam.SteamReviewSummary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrendScoreCalculatorTest {

    private final TrendScoreCalculator calculator = new TrendScoreCalculator();

    @Test
    void steamReviewScore_reflectsPositiveRateAndReviewVolume() {
        SteamReviewSummary strongSignal = new SteamReviewSummary(
                730,
                "Very Positive",
                900_000,
                100_000,
                1_000_000,
                0.90
        );
        SteamReviewSummary weakVolume = new SteamReviewSummary(
                1,
                "Positive",
                18,
                2,
                20,
                0.90
        );

        assertTrue(calculator.steamReviewScore(strongSignal) > calculator.steamReviewScore(weakVolume));
        assertEquals(90, calculator.steamReviewScore(strongSignal));
    }

    @Test
    void trendScore_usesWeightedSignals() {
        double trendScore = calculator.trendScore(90, 80, 70, 60);

        assertEquals(77.5, trendScore);
    }
}
