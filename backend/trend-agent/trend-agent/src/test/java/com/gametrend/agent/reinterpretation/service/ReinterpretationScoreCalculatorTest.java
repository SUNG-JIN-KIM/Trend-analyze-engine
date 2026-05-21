package com.gametrend.agent.reinterpretation.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinterpretationScoreCalculatorTest {

    private final ReinterpretationScoreCalculator calculator = new ReinterpretationScoreCalculator();

    @Test
    void reinterpretationScore_usesConfiguredWeights() {
        double score = calculator.reinterpretationScore(
                80,
                90,
                70,
                60,
                100,
                50,
                40
        );

        assertEquals(74.5, score);
    }

    @Test
    void interactionFitScore_addsBonusForModernInteractionMechanics() {
        LegacyGameSeed seed = new LegacyGameSeed(
                "Test",
                "STEAM",
                "1",
                1,
                2020,
                List.of("Party"),
                List.of("voice", "chat", "co-op", "deduction"),
                List.of("asymmetric voice deduction reaction"),
                List.of("WEBCAM", "TTS", "STT"),
                80,
                80,
                60,
                80,
                1000,
                0.9
        );

        assertTrue(calculator.interactionFitScore(seed) > 80);
    }

    @Test
    void modernTrendFitScore_returnsFallbackWhenLiveTrendIsEmpty() {
        LegacyGameSeed seed = new LegacyGameSeed(
                "Test",
                "STEAM",
                "1",
                1,
                2020,
                List.of("Horror"),
                List.of("ghost"),
                List.of("co-op investigation"),
                List.of("STT"),
                80,
                80,
                80,
                80,
                1000,
                0.9
        );

        assertEquals(55, calculator.modernTrendFitScore(seed, List.of()));
    }
}
