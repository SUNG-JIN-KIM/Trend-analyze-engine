package com.gametrend.agent.trend.service;

import com.gametrend.agent.game.entity.Game;
import com.gametrend.agent.game.repository.GameRepository;
import com.gametrend.agent.infrastructure.steam.SteamClient;
import com.gametrend.agent.infrastructure.steam.SteamReviewSummary;
import com.gametrend.agent.trend.dto.TrendGameResponse;
import com.gametrend.agent.trend.dto.TrendRefreshRequest;
import com.gametrend.agent.trend.dto.TrendRefreshResponse;
import com.gametrend.agent.trend.dto.TrendSeedGameRequest;
import com.gametrend.agent.trend.entity.TrendGame;
import com.gametrend.agent.trend.exception.TrendGameNotFoundException;
import com.gametrend.agent.trend.repository.TrendGameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrendGameService {

    private static final int DEFAULT_TOP_LIMIT = 5;

    private final TrendGameRepository trendGameRepository;
    private final SteamClient steamClient;
    private final GameRepository gameRepository;
    private final TrendScoreCalculator trendScoreCalculator;

    public TrendRefreshResponse refresh(TrendRefreshRequest request) {
        List<TrendSeedGame> seeds = resolveSeeds(request);
        LocalDateTime refreshedAt = LocalDateTime.now();
        List<TrendGameResponse> responses = new ArrayList<>();
        int partialCount = 0;

        for (TrendSeedGame seed : seeds) {
            RefreshResult result = refreshOne(seed, refreshedAt);
            if (result.partial()) {
                partialCount++;
            }
            responses.add(result.response());
        }

        responses.sort(Comparator.comparing(TrendGameResponse::trendScore).reversed());

        return new TrendRefreshResponse(
                seeds.size(),
                responses.size(),
                partialCount,
                "트렌드 시그널 갱신이 완료되었습니다. 외부 API 실패 항목은 fallback 값으로 저장했습니다.",
                refreshedAt,
                responses
        );
    }

    public List<TrendGameResponse> findTrendGames() {
        return trendGameRepository.findAllByOrderByTrendScoreDesc()
                .stream()
                .map(TrendGameResponse::from)
                .toList();
    }

    public List<TrendGameResponse> findTopTrendGames() {
        return findTopTrendGames(DEFAULT_TOP_LIMIT);
    }

    public List<TrendGameResponse> findTopTrendGames(int limit) {
        int resolvedLimit = Math.max(1, limit);
        return trendGameRepository.findAllByOrderByTrendScoreDesc()
                .stream()
                .limit(resolvedLimit)
                .map(TrendGameResponse::from)
                .toList();
    }

    public TrendGameResponse findTrendGame(Long id) {
        TrendGame trendGame = trendGameRepository.findById(id)
                .orElseThrow(() -> new TrendGameNotFoundException(id));
        return TrendGameResponse.from(trendGame);
    }

    private RefreshResult refreshOne(TrendSeedGame seed, LocalDateTime refreshedAt) {
        SteamSignal steamSignal = resolveSteamSignal(seed);
        TwitchSignal twitchSignal = resolveTwitchSignal(seed);
        double internalRecommendationScore = resolveInternalRecommendationScore(seed);

        int steamReviewScore = trendScoreCalculator.steamReviewScore(steamSignal.summary());
        int twitchViewerScore = trendScoreCalculator.twitchViewerScore(twitchSignal.totalViewerCount());
        int twitchStreamCountScore = trendScoreCalculator.twitchStreamCountScore(twitchSignal.liveStreamCount());
        int streamabilityScore = trendScoreCalculator.streamabilityScore(twitchViewerScore, twitchStreamCountScore);
        int marketSignalScore = trendScoreCalculator.marketSignalScore(
                steamReviewScore,
                twitchViewerScore,
                twitchStreamCountScore
        );
        double trendScore = trendScoreCalculator.trendScore(
                steamReviewScore,
                twitchViewerScore,
                twitchStreamCountScore,
                internalRecommendationScore
        );

        TrendGame existingGame = trendGameRepository.findByTitle(seed.title()).orElse(null);
        LocalDateTime createdAt = existingGame == null ? refreshedAt : existingGame.getCreatedAt();
        boolean partial = steamSignal.fallback() || twitchSignal.fallback();
        String signalStatus = partial ? "PARTIAL" : "COMPLETE";

        TrendGame trendGame = TrendGame.builder()
                .id(existingGame == null ? null : existingGame.getId())
                .title(seed.title())
                .genre(seed.genre())
                .platform(seed.platform())
                .steamAppId(seed.steamAppId())
                .twitchKeyword(seed.twitchKeyword())
                .steamReviewScore(steamReviewScore)
                .steamTotalReviews(steamSignal.summary().totalReviews())
                .steamPositiveRate(steamSignal.summary().positiveRate())
                .twitchLiveStreamCount(twitchSignal.liveStreamCount())
                .twitchTotalViewerCount(twitchSignal.totalViewerCount())
                .twitchViewerScore(twitchViewerScore)
                .twitchStreamCountScore(twitchStreamCountScore)
                .streamabilityScore(streamabilityScore)
                .marketSignalScore(marketSignalScore)
                .internalRecommendationScore(internalRecommendationScore)
                .trendScore(trendScore)
                .signalStatus(signalStatus)
                .reason(buildReason(seed, steamSignal, twitchSignal, internalRecommendationScore, trendScore))
                .createdAt(createdAt)
                .updatedAt(refreshedAt)
                .build();

        return new RefreshResult(TrendGameResponse.from(trendGameRepository.save(trendGame)), partial);
    }

    private SteamSignal resolveSteamSignal(TrendSeedGame seed) {
        if (seed.steamAppId() == null) {
            return new SteamSignal(fallbackSteamSummary(seed, 0), true, "Steam App ID가 없어 fallback Steam 지표를 사용했습니다.");
        }

        try {
            return new SteamSignal(steamClient.getReviewSummary(seed.steamAppId()), false, "Steam 리뷰 데이터를 조회했습니다.");
        } catch (RuntimeException exception) {
            log.warn("트렌드 Steam 지표 조회 실패. title={}, appId={}, cause={}",
                    seed.title(),
                    seed.steamAppId(),
                    exception.toString()
            );
            return new SteamSignal(
                    fallbackSteamSummary(seed, seed.steamAppId()),
                    true,
                    "Steam 조회 실패로 fallback Steam 지표를 사용했습니다."
            );
        }
    }

    private TwitchSignal resolveTwitchSignal(TrendSeedGame seed) {
        TwitchSignal fallbackSignal = fallbackTwitchSignal(seed);
        return new TwitchSignal(
                fallbackSignal.liveStreamCount(),
                fallbackSignal.totalViewerCount(),
                true,
                "Twitch 실시간 연동은 1차 버전에서 fallback 지표를 사용합니다."
        );
    }

    private double resolveInternalRecommendationScore(TrendSeedGame seed) {
        for (Game game : gameRepository.findAll()) {
            if (isSameTitle(game.getTitle(), seed.title())) {
                return game.getRecommendationScore();
            }
        }
        return seed.defaultInternalRecommendationScore();
    }

    private boolean isSameTitle(String first, String second) {
        return normalize(first).equals(normalize(second));
    }

    private SteamReviewSummary fallbackSteamSummary(TrendSeedGame seed, int appId) {
        int totalReviews = seed.defaultSteamTotalReviews();
        double positiveRate = seed.defaultSteamPositiveRate();
        int totalPositive = (int) Math.round(totalReviews * positiveRate);
        int totalNegative = Math.max(0, totalReviews - totalPositive);

        return new SteamReviewSummary(
                appId,
                "Fallback",
                totalPositive,
                totalNegative,
                totalReviews,
                positiveRate
        );
    }

    private TwitchSignal fallbackTwitchSignal(TrendSeedGame seed) {
        return new TwitchSignal(
                seed.defaultTwitchLiveStreamCount(),
                seed.defaultTwitchTotalViewerCount(),
                true,
                "Fallback Twitch signal"
        );
    }

    private String buildReason(
            TrendSeedGame seed,
            SteamSignal steamSignal,
            TwitchSignal twitchSignal,
            double internalRecommendationScore,
            double trendScore
    ) {
        return "%s Steam 리뷰 점수 %d점(총 %,d건, 긍정 %.1f%%), Twitch fallback 시청자 %,d명/방송 %,d개, 내부 추천 점수 %.1f점을 반영해 trendScore %.1f점으로 계산했습니다. %s %s"
                .formatted(
                        seed.reason(),
                        trendScoreCalculator.steamReviewScore(steamSignal.summary()),
                        steamSignal.summary().totalReviews(),
                        steamSignal.summary().positiveRate() * 100.0,
                        twitchSignal.totalViewerCount(),
                        twitchSignal.liveStreamCount(),
                        internalRecommendationScore,
                        trendScore,
                        steamSignal.message(),
                        twitchSignal.message()
                );
    }

    private List<TrendSeedGame> resolveSeeds(TrendRefreshRequest request) {
        if (request == null || request.games() == null || request.games().isEmpty()) {
            return defaultSeeds();
        }

        return request.games()
                .stream()
                .filter(seed -> seed != null && seed.title() != null && !seed.title().isBlank())
                .map(this::toSeed)
                .toList();
    }

    private TrendSeedGame toSeed(TrendSeedGameRequest request) {
        TrendSeedGame fallback = defaultSeeds().stream()
                .filter(seed -> isSameTitle(seed.title(), request.title()))
                .findFirst()
                .orElse(defaultCustomSeed(request.title()));

        return new TrendSeedGame(
                request.title().strip(),
                displayValue(request.genre(), fallback.genre()),
                displayValue(request.platform(), fallback.platform()),
                request.steamAppId() == null ? fallback.steamAppId() : request.steamAppId(),
                displayValue(request.twitchKeyword(), fallback.twitchKeyword()),
                fallback.defaultSteamTotalReviews(),
                fallback.defaultSteamPositiveRate(),
                fallback.defaultTwitchLiveStreamCount(),
                fallback.defaultTwitchTotalViewerCount(),
                fallback.defaultInternalRecommendationScore(),
                fallback.reason()
        );
    }

    private TrendSeedGame defaultCustomSeed(String title) {
        return new TrendSeedGame(
                title.strip(),
                "Unknown",
                "PC",
                null,
                title.strip(),
                2_000,
                0.82,
                80,
                2_500,
                65.0,
                "요청으로 전달된 커스텀 게임입니다."
        );
    }

    private List<TrendSeedGame> defaultSeeds() {
        return List.of(
                new TrendSeedGame(
                        "Counter-Strike 2",
                        "FPS",
                        "PC",
                        730,
                        "Counter-Strike 2",
                        8_400_000,
                        0.87,
                        1_800,
                        95_000,
                        82.0,
                        "전술 슈팅 장르의 강한 e스포츠/라이브 시청 기반을 대표하는 seed입니다."
                ),
                new TrendSeedGame(
                        "PUBG",
                        "Battle Royale",
                        "PC",
                        578080,
                        "PUBG",
                        2_400_000,
                        0.58,
                        850,
                        42_000,
                        74.0,
                        "배틀로얄 장르의 지속 경쟁력과 방송 클립성을 확인하기 위한 seed입니다."
                ),
                new TrendSeedGame(
                        "Minecraft",
                        "Sandbox Survival",
                        "PC",
                        null,
                        "Minecraft",
                        500_000,
                        0.95,
                        2_500,
                        110_000,
                        88.0,
                        "샌드박스, 생존, UGC 트렌드를 대표하는 seed입니다."
                ),
                new TrendSeedGame(
                        "Lethal Company",
                        "Co-op Horror",
                        "PC",
                        1966720,
                        "Lethal Company",
                        390_000,
                        0.96,
                        420,
                        18_000,
                        84.0,
                        "협동 호러와 스트리머 리액션을 결합한 최근 성공 사례 seed입니다."
                ),
                new TrendSeedGame(
                        "Palworld",
                        "Survival Craft",
                        "PC",
                        1623730,
                        "Palworld",
                        330_000,
                        0.93,
                        500,
                        22_000,
                        80.0,
                        "수집, 생존, 제작 루프가 결합된 대중적 트렌드 seed입니다."
                ),
                new TrendSeedGame(
                        "Helldivers 2",
                        "Co-op Shooter",
                        "PC",
                        553850,
                        "Helldivers 2",
                        310_000,
                        0.82,
                        450,
                        20_000,
                        78.0,
                        "협동 슈팅과 커뮤니티 이벤트형 운영을 보기 위한 seed입니다."
                ),
                new TrendSeedGame(
                        "Among Us",
                        "Social Deduction",
                        "PC",
                        945360,
                        "Among Us",
                        600_000,
                        0.92,
                        350,
                        15_000,
                        76.0,
                        "소셜 추리, 파티, 방송 친화 게임의 대표 seed입니다."
                )
        );
    }

    private String displayValue(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.strip();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase().strip();
    }

    private record TrendSeedGame(
            String title,
            String genre,
            String platform,
            Integer steamAppId,
            String twitchKeyword,
            int defaultSteamTotalReviews,
            double defaultSteamPositiveRate,
            int defaultTwitchLiveStreamCount,
            int defaultTwitchTotalViewerCount,
            double defaultInternalRecommendationScore,
            String reason
    ) {
    }

    private record SteamSignal(
            SteamReviewSummary summary,
            boolean fallback,
            String message
    ) {
    }

    private record TwitchSignal(
            int liveStreamCount,
            int totalViewerCount,
            boolean fallback,
            String message
    ) {
    }

    private record RefreshResult(
            TrendGameResponse response,
            boolean partial
    ) {
    }
}
