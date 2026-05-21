package com.gametrend.agent.livetrend.service;

import com.gametrend.agent.livetrend.config.LiveTrendProperties;
import com.gametrend.agent.livetrend.dto.LiveTrendGameResponse;
import com.gametrend.agent.livetrend.dto.LiveTrendRankingResponse;
import com.gametrend.agent.livetrend.dto.LiveTrendRefreshResponse;
import com.gametrend.agent.livetrend.entity.LiveTrendGame;
import com.gametrend.agent.livetrend.repository.LiveTrendGameRepository;
import com.gametrend.agent.trend.service.TrendScoreCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveTrendService {

    private static final int DEFAULT_TOP_LIMIT = 5;
    private static final String ALL_PLATFORMS = "ALL";
    private static final String SIGNAL_STATUS_PARTIAL = "PARTIAL";
    private static final String DATA_ORIGIN_REAL = "REAL";
    private static final String DATA_ORIGIN_FALLBACK = "FALLBACK";
    private static final String DATA_ORIGIN_PARTIAL = "PARTIAL";
    private static final String SORT_TREND_SCORE = "TREND_SCORE";
    private static final String SORT_VIEWER_COUNT = "VIEWER_COUNT";
    private static final String SORT_STREAM_COUNT = "STREAM_COUNT";
    private static final Set<String> FILTERABLE_PLATFORMS = Set.of(
            "TWITCH",
            "CHZZK",
            "SOOP",
            "STEAM"
    );

    private final LiveTrendGameRepository liveTrendGameRepository;
    private final TrendScoreCalculator trendScoreCalculator;
    private final LiveTrendPlatformStatusService platformStatusService;
    private final LiveTrendProperties properties;
    private final List<LiveTrendSignalClient> signalClients;

    public LiveTrendRefreshResponse refreshLiveTrends() {
        LocalDateTime refreshedAt = LocalDateTime.now();
        Map<String, List<LiveGameSignal>> signalsBySource = fetchLiveSignals(refreshedAt);
        List<LiveTrendSeed> seeds = refreshSeeds(signalsBySource);
        List<LiveTrendGameResponse> responses = new ArrayList<>();
        int partialCount = 0;

        try {
            for (LiveTrendSeed seed : seeds) {
                try {
                    RefreshResult result = refreshOne(seed, refreshedAt, signalsBySource);
                    responses.add(result.response());
                    if (result.partial()) {
                        partialCount++;
                    }
                } catch (RuntimeException exception) {
                    partialCount++;
                    log.warn("라이브 트렌드 seed 갱신 실패. source={}, title={}, cause={}",
                            seed.source(),
                            seed.title(),
                            exception.toString()
                    );
                    platformStatusService.markFailure(
                            seed.source(),
                            "%s 수집 실패: %s".formatted(seed.source(), exception.getMessage()),
                            refreshedAt
                    );
                    responses.add(refreshFallback(seed, refreshedAt, exception));
                }
            }

            responses.sort(Comparator.comparing(LiveTrendGameResponse::trendScore).reversed());
            String status = partialCount == 0 ? "SUCCESS" : "PARTIAL_SUCCESS";
            String message = buildRefreshMessage(seeds.size(), responses.size(), partialCount);

            return new LiveTrendRefreshResponse(
                    seeds.size(),
                    responses.size(),
                    partialCount,
                    status,
                    message,
                    refreshedAt,
                    responses
            );
        } catch (RuntimeException exception) {
            String message = "라이브 트렌드 갱신 중 예기치 못한 오류가 발생했습니다: " + exception.getMessage();
            log.warn(message, exception);
            return new LiveTrendRefreshResponse(
                    seeds.size(),
                    responses.size(),
                    Math.max(partialCount, seeds.size() - responses.size()),
                    "FAILED",
                    message,
                    refreshedAt,
                    responses
            );
        }
    }

    public List<LiveTrendGameResponse> findLiveTrendGames() {
        return liveTrendGameRepository.findAllByOrderByTrendScoreDesc()
                .stream()
                .filter(this::shouldExposeGame)
                .map(LiveTrendGameResponse::from)
                .toList();
    }

    public List<LiveTrendGameResponse> findTopLiveTrendGames(int limit) {
        return findTopLiveTrendGames(limit, ALL_PLATFORMS);
    }

    public List<LiveTrendGameResponse> findTopLiveTrendGames(int limit, String platform) {
        int resolvedLimit = Math.max(1, limit);
        String resolvedPlatform = resolvePlatformFilter(platform);
        List<LiveTrendGame> liveTrendGames = ALL_PLATFORMS.equals(resolvedPlatform)
                ? liveTrendGameRepository.findAllByOrderByTrendScoreDesc()
                : liveTrendGameRepository.findBySourceOrderByTrendScoreDesc(resolvedPlatform);

        return liveTrendGames
                .stream()
                .filter(this::shouldExposeGame)
                .limit(resolvedLimit)
                .map(LiveTrendGameResponse::from)
                .toList();
    }

    public List<LiveTrendGameResponse> findTopLiveTrendGames() {
        return findTopLiveTrendGames(DEFAULT_TOP_LIMIT);
    }

    public List<LiveTrendRankingResponse> findRankings(String platform, String sort, int limit) {
        String resolvedPlatform = resolvePlatformFilter(platform);
        String resolvedSort = resolveRankingSort(sort);
        int resolvedLimit = Math.max(1, limit);
        List<LiveTrendGame> liveTrendGames = ALL_PLATFORMS.equals(resolvedPlatform)
                ? liveTrendGameRepository.findAllByOrderByTrendScoreDesc()
                : liveTrendGameRepository.findBySourceOrderByTrendScoreDesc(resolvedPlatform);

        List<LiveTrendGame> sortedGames = liveTrendGames.stream()
                .filter(this::shouldExposeGame)
                .sorted(rankingComparator(resolvedSort))
                .limit(resolvedLimit)
                .toList();

        List<LiveTrendRankingResponse> rankings = new ArrayList<>();
        for (int index = 0; index < sortedGames.size(); index++) {
            rankings.add(LiveTrendRankingResponse.from(index + 1, sortedGames.get(index)));
        }
        return rankings;
    }

    public LiveTrendGameResponse findLiveTrendGame(Long id) {
        return liveTrendGameRepository.findById(id)
                .map(LiveTrendGameResponse::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "라이브 트렌드 게임을 찾을 수 없습니다. id=" + id));
    }

    private RefreshResult refreshOne(
            LiveTrendSeed seed,
            LocalDateTime refreshedAt,
            Map<String, List<LiveGameSignal>> signalsBySource
    ) {
        LiveTrendSignal signal = collectLiveTrendSignal(seed, signalsBySource);
        LiveTrendGameResponse response = saveLiveTrendGame(seed, signal, refreshedAt, signal.partial());
        return new RefreshResult(response, signal.partial());
    }

    private LiveTrendGameResponse refreshFallback(
            LiveTrendSeed seed,
            LocalDateTime refreshedAt,
            RuntimeException exception
    ) {
        LiveTrendSignal fallback = new LiveTrendSignal(
                seed.defaultLiveStreamCount(),
                seed.defaultTotalViewerCount(),
                true,
                "외부 라이브 플랫폼 조회 실패로 fallback 지표를 사용했습니다: " + exception.getClass().getSimpleName()
        );
        return saveLiveTrendGame(seed, fallback, refreshedAt, true);
    }

    private LiveTrendSignal collectLiveTrendSignal(
            LiveTrendSeed seed,
            Map<String, List<LiveGameSignal>> signalsBySource
    ) {
        if (!platformStatusService.isConfigured(seed.source())) {
            return new LiveTrendSignal(
                    seed.defaultLiveStreamCount(),
                    seed.defaultTotalViewerCount(),
                    true,
                    "%s 인증 정보가 없어 fallback snapshot을 사용합니다.".formatted(seed.source())
            );
        }

        Optional<LiveGameSignal> actualSignal = findMatchingSignal(seed, signalsBySource);
        if (actualSignal.isPresent()) {
            LiveGameSignal signal = actualSignal.get();
            return new LiveTrendSignal(
                    signal.liveStreamCount(),
                    signal.totalViewerCount(),
                    false,
                    "%s 실제 API 수집 신호를 반영했습니다. topChannels=[%s], rawMetadata=%s"
                            .formatted(seed.source(), String.join(" | ", signal.topChannels()), signal.rawMetadata())
            );
        }

        return new LiveTrendSignal(
                seed.defaultLiveStreamCount(),
                seed.defaultTotalViewerCount(),
                true,
                "%s 인증 설정은 확인되었지만 이번 refresh에서 seed와 일치하는 실제 라이브 게임 신호가 없어 fallback snapshot을 사용합니다."
                        .formatted(seed.source())
        );
    }

    private Map<String, List<LiveGameSignal>> fetchLiveSignals(LocalDateTime refreshedAt) {
        Map<String, List<LiveGameSignal>> signalsBySource = new LinkedHashMap<>();
        for (LiveTrendSignalClient client : signalClients) {
            if (!platformStatusService.isConfigured(client.source())) {
                continue;
            }
            try {
                List<LiveGameSignal> signals = client.fetchSignals();
                signalsBySource.put(client.source(), signals);
            } catch (RuntimeException exception) {
                log.warn("라이브 트렌드 API 수집 실패. source={}, cause={}", client.source(), exception.toString());
                platformStatusService.markFailure(
                        client.source(),
                        "%s API 수집 실패: %s".formatted(client.source(), exception.getMessage()),
                        refreshedAt
                );
                signalsBySource.put(client.source(), List.of());
            }
        }
        return signalsBySource;
    }

    private List<LiveTrendSeed> refreshSeeds(Map<String, List<LiveGameSignal>> signalsBySource) {
        List<LiveTrendSeed> seeds = new ArrayList<>(defaultSeeds());
        for (List<LiveGameSignal> signals : signalsBySource.values()) {
            signals.stream()
                    .limit(5)
                    .filter(signal -> seeds.stream().noneMatch(seed -> matchesSignal(seed, signal)))
                    .map(this::liveSignalSeed)
                    .forEach(seeds::add);
        }
        return seeds;
    }

    private LiveTrendSeed liveSignalSeed(LiveGameSignal signal) {
        return new LiveTrendSeed(
                signal.source(),
                signal.gameName(),
                "Live Trend",
                "PC",
                signal.sourceKeyword(),
                signal.liveStreamCount(),
                signal.totalViewerCount(),
                76,
                "%s 실제 라이브 API 상위 카테고리에서 생성한 동적 seed입니다.".formatted(signal.source())
        );
    }

    private Optional<LiveGameSignal> findMatchingSignal(
            LiveTrendSeed seed,
            Map<String, List<LiveGameSignal>> signalsBySource
    ) {
        return signalsBySource.getOrDefault(seed.source(), List.of())
                .stream()
                .filter(signal -> matchesSignal(seed, signal))
                .findFirst();
    }

    private boolean matchesSignal(LiveTrendSeed seed, LiveGameSignal signal) {
        String seedTitle = normalizeMatchValue(seed.title());
        String seedKeyword = normalizeMatchValue(seed.sourceKeyword());
        String signalName = normalizeMatchValue(signal.gameName());
        String signalKeyword = normalizeMatchValue(signal.sourceKeyword());
        return sameOrContains(seedTitle, signalName)
                || sameOrContains(seedKeyword, signalName)
                || sameOrContains(seedTitle, signalKeyword)
                || sameOrContains(seedKeyword, signalKeyword);
    }

    private boolean sameOrContains(String left, String right) {
        return !left.isBlank()
                && !right.isBlank()
                && (left.equals(right) || left.contains(right) || right.contains(left));
    }

    private String normalizeMatchValue(String value) {
        if (value == null) {
            return "";
        }
        return value.strip().toLowerCase();
    }

    private LiveTrendGameResponse saveLiveTrendGame(
            LiveTrendSeed seed,
            LiveTrendSignal signal,
            LocalDateTime refreshedAt,
            boolean partial
    ) {
        int viewerScore = trendScoreCalculator.twitchViewerScore(signal.totalViewerCount());
        int streamCountScore = trendScoreCalculator.twitchStreamCountScore(signal.liveStreamCount());
        int streamabilityScore = trendScoreCalculator.streamabilityScore(viewerScore, streamCountScore);
        int marketSignalScore = trendScoreCalculator.marketSignalScore(seed.baseMarketScore(), viewerScore, streamCountScore);
        double trendScore = trendScoreCalculator.trendScore(
                seed.baseMarketScore(),
                viewerScore,
                streamCountScore,
                streamabilityScore
        );

        LiveTrendGame existingGame = liveTrendGameRepository
                .findBySourceAndTitle(seed.source(), seed.title())
                .orElse(null);
        LocalDateTime createdAt = existingGame == null ? refreshedAt : existingGame.getCreatedAt();

        LiveTrendGame liveTrendGame = LiveTrendGame.builder()
                .id(existingGame == null ? null : existingGame.getId())
                .source(seed.source())
                .title(seed.title())
                .genre(seed.genre())
                .platform(seed.platform())
                .sourceKeyword(seed.sourceKeyword())
                .liveStreamCount(signal.liveStreamCount())
                .totalViewerCount(signal.totalViewerCount())
                .viewerScore(viewerScore)
                .streamCountScore(streamCountScore)
                .streamabilityScore(streamabilityScore)
                .marketSignalScore(marketSignalScore)
                .trendScore(trendScore)
                .signalStatus(partial ? "PARTIAL" : "COMPLETE")
                .dataOrigin(partial ? DATA_ORIGIN_FALLBACK : DATA_ORIGIN_REAL)
                .reason(buildReason(seed, signal, trendScore))
                .createdAt(createdAt)
                .updatedAt(refreshedAt)
                .build();

        return LiveTrendGameResponse.from(liveTrendGameRepository.save(liveTrendGame));
    }

    private boolean shouldExposeGame(LiveTrendGame liveTrendGame) {
        if (properties.isExposeFallbackData()) {
            return true;
        }
        if (isFallbackGame(liveTrendGame)) {
            return false;
        }
        return platformStatusService.isConfigured(liveTrendGame.getSource());
    }

    private boolean isFallbackGame(LiveTrendGame liveTrendGame) {
        return SIGNAL_STATUS_PARTIAL.equalsIgnoreCase(nullToEmpty(liveTrendGame.getSignalStatus()))
                || DATA_ORIGIN_FALLBACK.equalsIgnoreCase(nullToEmpty(liveTrendGame.getDataOrigin()))
                || DATA_ORIGIN_PARTIAL.equalsIgnoreCase(nullToEmpty(liveTrendGame.getDataOrigin()));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String buildReason(LiveTrendSeed seed, LiveTrendSignal signal, double trendScore) {
        return "%s 기준 live stream %,d개, viewer %,d명, 장르 신호 %d점을 반영해 live trendScore %.1f점으로 계산했습니다. %s %s"
                .formatted(
                        seed.source(),
                        signal.liveStreamCount(),
                        signal.totalViewerCount(),
                        seed.baseMarketScore(),
                        trendScore,
                        seed.reason(),
                        signal.message()
                );
    }

    private String buildRefreshMessage(int requestedCount, int refreshedCount, int partialCount) {
        if (partialCount == 0) {
            return "라이브 트렌드 데이터 %d개를 정상 갱신했습니다.".formatted(refreshedCount);
        }
        return "라이브 트렌드 데이터 %d개 중 %d개가 fallback 또는 partial 지표로 갱신되었습니다."
                .formatted(requestedCount, partialCount);
    }

    private String resolvePlatformFilter(String platform) {
        if (platform == null || platform.isBlank()) {
            return ALL_PLATFORMS;
        }

        String normalizedPlatform = platform.strip().toUpperCase();
        if (ALL_PLATFORMS.equals(normalizedPlatform)) {
            return ALL_PLATFORMS;
        }
        if (FILTERABLE_PLATFORMS.contains(normalizedPlatform)) {
            return normalizedPlatform;
        }

        throw new ResponseStatusException(
                BAD_REQUEST,
                "지원하지 않는 라이브 트렌드 플랫폼입니다. platform은 ALL, TWITCH, CHZZK, SOOP, STEAM 중 하나여야 합니다."
        );
    }

    private String resolveRankingSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return SORT_TREND_SCORE;
        }
        String normalizedSort = sort.strip().toUpperCase();
        if (SORT_TREND_SCORE.equals(normalizedSort)
                || SORT_VIEWER_COUNT.equals(normalizedSort)
                || SORT_STREAM_COUNT.equals(normalizedSort)) {
            return normalizedSort;
        }
        throw new ResponseStatusException(
                BAD_REQUEST,
                "지원하지 않는 라이브 트렌드 정렬 기준입니다. sort는 TREND_SCORE, VIEWER_COUNT, STREAM_COUNT 중 하나여야 합니다."
        );
    }

    private Comparator<LiveTrendGame> rankingComparator(String sort) {
        return switch (sort) {
            case SORT_VIEWER_COUNT -> Comparator.comparingInt(LiveTrendGame::getTotalViewerCount).reversed()
                    .thenComparing(Comparator.comparingDouble(LiveTrendGame::getTrendScore).reversed())
                    .thenComparing(Comparator.comparingInt(LiveTrendGame::getLiveStreamCount).reversed());
            case SORT_STREAM_COUNT -> Comparator.comparingInt(LiveTrendGame::getLiveStreamCount).reversed()
                    .thenComparing(Comparator.comparingDouble(LiveTrendGame::getTrendScore).reversed())
                    .thenComparing(Comparator.comparingInt(LiveTrendGame::getTotalViewerCount).reversed());
            default -> Comparator.comparingDouble(LiveTrendGame::getTrendScore).reversed()
                    .thenComparing(Comparator.comparingInt(LiveTrendGame::getTotalViewerCount).reversed())
                    .thenComparing(Comparator.comparingInt(LiveTrendGame::getLiveStreamCount).reversed());
        };
    }

    private List<LiveTrendSeed> defaultSeeds() {
        return List.of(
                new LiveTrendSeed("TWITCH", "Counter-Strike 2", "FPS", "PC", "Counter-Strike 2", 1_800, 95_000, 86,
                        "전술 FPS와 e스포츠 시청 수요를 확인하기 위한 라이브 seed입니다."),
                new LiveTrendSeed("TWITCH", "Lethal Company", "Co-op Horror", "PC", "Lethal Company", 420, 18_000, 84,
                        "협동 호러와 스트리머 리액션 적합성을 확인하기 위한 라이브 seed입니다."),
                new LiveTrendSeed("TWITCH", "Minecraft", "Sandbox Survival", "PC", "Minecraft", 2_500, 110_000, 88,
                        "샌드박스, 생존, UGC 방송 수요를 대표하는 라이브 seed입니다."),
                new LiveTrendSeed("CHZZK", "PUBG", "Battle Royale", "PC", "배틀그라운드", 700, 32_000, 78,
                        "국내 FPS/배틀로얄 라이브 반응을 확인하기 위한 라이브 seed입니다."),
                new LiveTrendSeed("CHZZK", "League of Legends", "MOBA", "PC", "리그 오브 레전드", 1_300, 74_000, 90,
                        "국내 라이브 플랫폼에서 안정적인 시청 기반을 가진 경쟁 게임 seed입니다."),
                new LiveTrendSeed("SOOP", "StarCraft", "RTS", "PC", "스타크래프트", 650, 28_000, 75,
                        "SOOP에서 오래 지속되는 e스포츠/레트로 경쟁 신호를 확인하기 위한 seed입니다."),
                new LiveTrendSeed("SOOP", "Sudden Attack", "FPS", "PC", "서든어택", 520, 16_000, 70,
                        "국내 FPS 시청자 기반과 오래된 IP 지속성을 확인하기 위한 seed입니다.")
        );
    }

    private record LiveTrendSeed(
            String source,
            String title,
            String genre,
            String platform,
            String sourceKeyword,
            int defaultLiveStreamCount,
            int defaultTotalViewerCount,
            int baseMarketScore,
            String reason
    ) {
    }

    private record LiveTrendSignal(
            int liveStreamCount,
            int totalViewerCount,
            boolean partial,
            String message
    ) {
    }

    private record RefreshResult(
            LiveTrendGameResponse response,
            boolean partial
    ) {
    }
}
