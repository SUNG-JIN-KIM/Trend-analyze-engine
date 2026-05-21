package com.gametrend.agent.steam.service;

import com.gametrend.agent.game.dto.GameCreateRequest;
import com.gametrend.agent.game.dto.GameResponse;
import com.gametrend.agent.game.service.GameService;
import com.gametrend.agent.infrastructure.steam.SteamClient;
import com.gametrend.agent.infrastructure.steam.SteamReviewSummary;
import com.gametrend.agent.steam.dto.SteamImportRequest;
import com.gametrend.agent.steam.dto.SteamImportResponse;
import com.gametrend.agent.steam.dto.SteamReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SteamImportService {

    private final SteamClient steamClient;
    private final GameService gameService;

    public SteamReviewResponse getReviewSummary(int appId) {
        SteamReviewSummary summary = steamClient.getReviewSummary(appId);
        return SteamReviewResponse.of(summary, calculateMarketSignalScore(summary));
    }

    public SteamImportResponse importGame(SteamImportRequest request) {
        SteamReviewSummary summary = steamClient.getReviewSummary(request.appId());
        int marketSignalScore = calculateMarketSignalScore(summary);
        SteamReviewResponse steamReview = SteamReviewResponse.of(summary, marketSignalScore);

        GameCreateRequest gameRequest = new GameCreateRequest(
                request.title(),
                request.genre(),
                request.platform(),
                request.playStyle(),
                resolveScore(request.streamabilityScore(), defaultStreamabilityScore(marketSignalScore)),
                resolveScore(request.webcamFitScore(), 50),
                resolveScore(request.ttsFitScore(), 50),
                resolveScore(request.sttFitScore(), 50),
                resolveScore(request.noveltyScore(), defaultNoveltyScore(marketSignalScore)),
                resolveScore(request.devFeasibilityScore(), 65),
                marketSignalScore,
                buildReason(request.reason(), summary, marketSignalScore)
        );

        GameResponse game = gameService.createGame(gameRequest);
        return new SteamImportResponse(game, steamReview);
    }

    private int calculateMarketSignalScore(SteamReviewSummary summary) {
        if (summary.totalReviews() <= 0) {
            return 40;
        }

        double volumeWeight = reviewVolumeWeight(summary.totalReviews());
        return clamp((int) Math.round(summary.positiveRate() * 100.0 * volumeWeight));
    }

    private double reviewVolumeWeight(int totalReviews) {
        if (totalReviews >= 10_000) {
            return 1.0;
        }
        if (totalReviews >= 1_000) {
            return 0.95;
        }
        if (totalReviews >= 100) {
            return 0.85;
        }
        if (totalReviews >= 20) {
            return 0.70;
        }
        return 0.50;
    }

    private int defaultStreamabilityScore(int marketSignalScore) {
        return clamp((int) Math.round(55 + marketSignalScore * 0.35));
    }

    private int defaultNoveltyScore(int marketSignalScore) {
        return clamp((int) Math.round(45 + marketSignalScore * 0.30));
    }

    private int resolveScore(Integer requestedScore, int defaultScore) {
        if (requestedScore == null) {
            return defaultScore;
        }
        return clamp(requestedScore);
    }

    private int clamp(int score) {
        return Math.min(100, Math.max(0, score));
    }

    private String buildReason(String requestedReason, SteamReviewSummary summary, int marketSignalScore) {
        String steamReason = "Steam 리뷰 요약: %s, 긍정 %d건 / 부정 %d건, 총 리뷰 %d건, 긍정 비율 %.1f%%, marketSignalScore %d점."
                .formatted(
                        summary.reviewScoreDesc(),
                        summary.totalPositive(),
                        summary.totalNegative(),
                        summary.totalReviews(),
                        summary.positiveRate() * 100.0,
                        marketSignalScore
                );

        if (requestedReason == null || requestedReason.isBlank()) {
            return steamReason;
        }
        return "%s %s".formatted(requestedReason.strip(), steamReason);
    }
}
