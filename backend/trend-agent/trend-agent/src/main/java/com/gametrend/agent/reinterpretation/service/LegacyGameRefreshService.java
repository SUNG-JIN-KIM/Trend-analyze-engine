package com.gametrend.agent.reinterpretation.service;

import com.gametrend.agent.infrastructure.steam.SteamClient;
import com.gametrend.agent.infrastructure.steam.SteamClientException;
import com.gametrend.agent.infrastructure.steam.SteamReviewSummary;
import com.gametrend.agent.livetrend.dto.LiveTrendGameResponse;
import com.gametrend.agent.livetrend.service.LiveTrendService;
import com.gametrend.agent.reinterpretation.dto.LegacyGameRefreshResponse;
import com.gametrend.agent.reinterpretation.entity.GameReinterpretationCandidate;
import com.gametrend.agent.reinterpretation.entity.LegacyGame;
import com.gametrend.agent.reinterpretation.repository.GameReinterpretationCandidateRepository;
import com.gametrend.agent.reinterpretation.repository.LegacyGameRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class LegacyGameRefreshService {

    private final LegacyGameSeedService seedService;
    private final LegacyGameRepository legacyGameRepository;
    private final GameReinterpretationCandidateRepository candidateRepository;
    private final SteamClient steamClient;
    private final LiveTrendService liveTrendService;
    private final ReinterpretationScoreCalculator scoreCalculator;
    private final ReinterpretationMapper mapper;

    public LegacyGameRefreshService(
            LegacyGameSeedService seedService,
            LegacyGameRepository legacyGameRepository,
            GameReinterpretationCandidateRepository candidateRepository,
            SteamClient steamClient,
            LiveTrendService liveTrendService,
            ReinterpretationScoreCalculator scoreCalculator,
            ReinterpretationMapper mapper
    ) {
        this.seedService = seedService;
        this.legacyGameRepository = legacyGameRepository;
        this.candidateRepository = candidateRepository;
        this.steamClient = steamClient;
        this.liveTrendService = liveTrendService;
        this.scoreCalculator = scoreCalculator;
        this.mapper = mapper;
    }

    public LegacyGameRefreshResponse refresh() {
        LocalDateTime now = LocalDateTime.now();
        List<LegacyGameSeed> seeds = seedService.seeds();
        List<LiveTrendGameResponse> liveTrendGames = findLiveTrendGames();
        List<String> liveTrendTokens = liveTrendTokens(liveTrendGames);
        int fallbackCount = 0;

        for (LegacyGameSeed seed : seeds) {
            ReviewSignal reviewSignal = fetchReviewSignal(seed);
            if (!"REAL".equals(reviewSignal.dataOrigin())) {
                fallbackCount++;
            }
            LegacyGame legacyGame = saveLegacyGame(seed, reviewSignal, now);
            saveCandidate(seed, legacyGame, reviewSignal, liveTrendTokens, liveTrendGames, now);
        }

        String status = fallbackCount == 0 ? "SUCCESS" : "PARTIAL_SUCCESS";
        return new LegacyGameRefreshResponse(
                seeds.size(),
                seeds.size(),
                fallbackCount,
                status,
                "과거 게임 seed %d개를 갱신했습니다. fallback=%d".formatted(seeds.size(), fallbackCount),
                now
        );
    }

    public void ensureSeedFallbacks() {
        if (legacyGameRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<LiveTrendGameResponse> liveTrendGames = findLiveTrendGames();
        List<String> liveTrendTokens = liveTrendTokens(liveTrendGames);
        for (LegacyGameSeed seed : seedService.seeds()) {
            ReviewSignal fallback = fallbackReviewSignal(seed);
            LegacyGame legacyGame = saveLegacyGame(seed, fallback, now);
            saveCandidate(seed, legacyGame, fallback, liveTrendTokens, liveTrendGames, now);
        }
    }

    private ReviewSignal fetchReviewSignal(LegacyGameSeed seed) {
        if (seed.steamAppId() == null) {
            return fallbackReviewSignal(seed);
        }
        try {
            SteamReviewSummary summary = steamClient.getReviewSummary(seed.steamAppId());
            return new ReviewSignal(
                    summary.totalReviews(),
                    summary.positiveRate(),
                    scoreCalculator.legacyPopularityScore(summary.totalReviews()),
                    scoreCalculator.reviewSentimentScore(summary.positiveRate()),
                    "REAL",
                    "Steam appreviews 공개 API 기준 리뷰 신호입니다. reviewScore=%s".formatted(summary.reviewScoreDesc())
            );
        } catch (SteamClientException ex) {
            log.warn("과거 게임 Steam 리뷰 조회 실패. title={}, steamAppId={}, cause={}",
                    seed.title(), seed.steamAppId(), ex.toString());
            return fallbackReviewSignal(seed);
        }
    }

    private ReviewSignal fallbackReviewSignal(LegacyGameSeed seed) {
        return new ReviewSignal(
                seed.fallbackReviewCount(),
                seed.fallbackPositiveReviewRate(),
                scoreCalculator.legacyPopularityScore(seed.fallbackReviewCount()),
                scoreCalculator.reviewSentimentScore(seed.fallbackPositiveReviewRate()),
                "FALLBACK",
                "Steam 리뷰 API 실패 또는 미사용 상태라 seed fallback 리뷰 신호를 사용했습니다."
        );
    }

    private LegacyGame saveLegacyGame(LegacyGameSeed seed, ReviewSignal signal, LocalDateTime now) {
        LegacyGame existing = legacyGameRepository.findBySourceAndSourceGameId(seed.source(), seed.sourceGameId())
                .orElse(null);
        LocalDateTime createdAt = existing == null ? now : existing.getCreatedAt();
        return legacyGameRepository.save(LegacyGame.builder()
                .id(existing == null ? null : existing.getId())
                .title(seed.title())
                .source(seed.source())
                .sourceGameId(seed.sourceGameId())
                .steamAppId(seed.steamAppId())
                .releaseYear(seed.releaseYear())
                .genresJson(mapper.writeList(seed.genres()))
                .tagsJson(mapper.writeList(seed.tags()))
                .mechanicsJson(mapper.writeList(seed.mechanics()))
                .interactionHintsJson(mapper.writeList(seed.interactionHints()))
                .mechanicUniquenessScore(seed.mechanicUniquenessScore())
                .streamabilityScore(seed.streamabilityScore())
                .interactionFitSeedScore(seed.interactionFitSeedScore())
                .devFeasibilityScore(seed.devFeasibilityScore())
                .reviewCount(signal.reviewCount())
                .positiveReviewRate(signal.positiveReviewRate())
                .legacyPopularityScore(signal.legacyPopularityScore())
                .reviewSentimentScore(signal.reviewSentimentScore())
                .dataOrigin(signal.dataOrigin())
                .reason(signal.reason())
                .createdAt(createdAt)
                .updatedAt(now)
                .build());
    }

    private void saveCandidate(
            LegacyGameSeed seed,
            LegacyGame legacyGame,
            ReviewSignal signal,
            List<String> liveTrendTokens,
            List<LiveTrendGameResponse> liveTrendGames,
            LocalDateTime now
    ) {
        int interactionFitScore = scoreCalculator.interactionFitScore(seed);
        int modernTrendFitScore = scoreCalculator.modernTrendFitScore(seed, liveTrendTokens);
        double reinterpretationScore = scoreCalculator.reinterpretationScore(
                signal.legacyPopularityScore(),
                signal.reviewSentimentScore(),
                seed.mechanicUniquenessScore(),
                seed.streamabilityScore(),
                interactionFitScore,
                modernTrendFitScore,
                seed.devFeasibilityScore()
        );
        GameReinterpretationCandidate existing = candidateRepository.findByLegacyGameId(legacyGame.getId()).orElse(null);
        LocalDateTime createdAt = existing == null ? now : existing.getCreatedAt();
        candidateRepository.save(GameReinterpretationCandidate.builder()
                .id(existing == null ? null : existing.getId())
                .legacyGameId(legacyGame.getId())
                .title(seed.title())
                .source(seed.source())
                .sourceGameId(seed.sourceGameId())
                .steamAppId(seed.steamAppId())
                .releaseYear(seed.releaseYear())
                .genresJson(mapper.writeList(seed.genres()))
                .tagsJson(mapper.writeList(seed.tags()))
                .mechanicsJson(mapper.writeList(seed.mechanics()))
                .interactionHintsJson(mapper.writeList(seed.interactionHints()))
                .legacyPopularityScore(signal.legacyPopularityScore())
                .reviewSentimentScore(signal.reviewSentimentScore())
                .mechanicUniquenessScore(seed.mechanicUniquenessScore())
                .streamabilityScore(seed.streamabilityScore())
                .interactionFitScore(interactionFitScore)
                .modernTrendFitScore(modernTrendFitScore)
                .devFeasibilityScore(seed.devFeasibilityScore())
                .reinterpretationScore(reinterpretationScore)
                .reinterpretationConcept(buildConcept(seed))
                .reason(buildReason(seed, modernTrendFitScore, signal.dataOrigin()))
                .dataOrigin(signal.dataOrigin())
                .reviewCount(signal.reviewCount())
                .positiveReviewRate(signal.positiveReviewRate())
                .matchedLiveTrendSourcesJson(mapper.writeList(matchedLiveTrendSources(seed, liveTrendGames)))
                .createdAt(createdAt)
                .updatedAt(now)
                .build());
    }

    private List<LiveTrendGameResponse> findLiveTrendGames() {
        try {
            return liveTrendService.findTopLiveTrendGames(30);
        } catch (RuntimeException ex) {
            log.warn("재해석 후보 liveTrend 조회 실패. fallback modernTrendFitScore를 사용합니다. cause={}", ex.toString());
            return List.of();
        }
    }

    private List<String> liveTrendTokens(List<LiveTrendGameResponse> games) {
        return games.stream()
                .flatMap(game -> java.util.stream.Stream.of(game.title(), game.genre(), game.sourceKeyword()))
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT).strip())
                .toList();
    }

    private List<String> matchedLiveTrendSources(LegacyGameSeed seed, List<LiveTrendGameResponse> games) {
        List<String> seedTokens = scoreCalculator.seedTokens(seed);
        return games.stream()
                .filter(game -> {
                    String haystack = "%s %s %s".formatted(game.title(), game.genre(), game.sourceKeyword())
                            .toLowerCase(Locale.ROOT);
                    return seedTokens.stream().anyMatch(haystack::contains);
                })
                .limit(5)
                .map(game -> "%s:%s".formatted(game.source(), game.title()))
                .toList();
    }

    private String buildConcept(LegacyGameSeed seed) {
        String text = String.join(" ", seed.mechanics()).toLowerCase(Locale.ROOT);
        if (text.contains("horror") || text.contains("ghost")) {
            return "웹캠 반응형 협동 공포 게임";
        }
        if (text.contains("deduction") || text.contains("voting")) {
            return "시청자 참여형 음성 추리 게임";
        }
        if (text.contains("sandbox") || text.contains("roleplay")) {
            return "TTS 기반 혼란형 샌드박스 파티 시뮬레이션";
        }
        if (text.contains("narrative") || text.contains("narrator")) {
            return "TTS/STT 기반 반응형 내러티브 게임";
        }
        return "현대 스트리밍 인터랙션을 결합한 재해석 게임";
    }

    private String buildReason(LegacyGameSeed seed, int modernTrendFitScore, String dataOrigin) {
        return "%s는 %s 메커니즘이 강하고 %s 기능과 결합하기 좋습니다. 현재 liveTrend 적합도는 %d점이며 리뷰 신호는 %s 기준입니다."
                .formatted(
                        seed.title(),
                        String.join(", ", seed.mechanics()),
                        String.join("/", seed.interactionHints()),
                        modernTrendFitScore,
                        dataOrigin
                );
    }

    private record ReviewSignal(
            int reviewCount,
            double positiveReviewRate,
            int legacyPopularityScore,
            int reviewSentimentScore,
            String dataOrigin,
            String reason
    ) {
    }
}
