package com.gametrend.agent.livetrend.service;

import com.gametrend.agent.infrastructure.steam.SteamProperties;
import com.gametrend.agent.livetrend.config.LiveTrendProperties;
import com.gametrend.agent.livetrend.dto.LiveTrendGameResponse;
import com.gametrend.agent.livetrend.dto.LiveTrendPlatformStatusResponse;
import com.gametrend.agent.livetrend.dto.LiveTrendRefreshResponse;
import com.gametrend.agent.livetrend.entity.LiveTrendGame;
import com.gametrend.agent.livetrend.entity.LiveTrendPlatformStatus;
import com.gametrend.agent.livetrend.entity.LiveTrendRefreshStatus;
import com.gametrend.agent.livetrend.repository.LiveTrendGameRepository;
import com.gametrend.agent.livetrend.repository.LiveTrendPlatformStatusRepository;
import com.gametrend.agent.livetrend.repository.LiveTrendRefreshStatusRepository;
import com.gametrend.agent.trend.service.TrendScoreCalculator;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

class LiveTrendServiceTest {

    @Test
    void refreshLiveTrends_savesSeedSnapshotsAndUpdatesStatusWithoutExternalApi() {
        InMemoryLiveTrendGameRepository gameRepository = new InMemoryLiveTrendGameRepository();
        InMemoryLiveTrendRefreshStatusRepository statusRepository = new InMemoryLiveTrendRefreshStatusRepository();
        InMemoryLiveTrendPlatformStatusRepository platformStatusRepository = new InMemoryLiveTrendPlatformStatusRepository();
        LiveTrendRefreshCoordinator coordinator = refreshCoordinator(gameRepository, statusRepository, platformStatusRepository);

        LiveTrendRefreshResponse response = coordinator.refreshLiveTrends();

        assertEquals("PARTIAL_SUCCESS", response.status());
        assertEquals(7, response.requestedCount());
        assertEquals(7, response.refreshedCount());
        assertEquals(7, gameRepository.count());
        assertTrue(response.partialCount() > 0);
        assertTrue(response.games().stream().allMatch(game -> game.trendScore() >= 0 && game.trendScore() <= 100));
        assertEquals("PARTIAL_SUCCESS", statusRepository.currentStatus().getLastRefreshStatus());
        assertFalse(statusRepository.currentStatus().isRunning());
        assertNotNull(statusRepository.currentStatus().getNextRefreshEstimate());
        assertNotNull(statusRepository.currentStatus().getLastRefreshStartedAt());
        assertNotNull(statusRepository.currentStatus().getLastRefreshCompletedAt());
        assertEquals("PARTIAL", platformStatusRepository.findByPlatform("TWITCH").orElseThrow().getStatus());
        assertEquals("PUBLIC_OR_FALLBACK", platformStatusRepository.findByPlatform("STEAM").orElseThrow().getStatus());
    }

    @Test
    void refreshLiveTrends_skipsWhenRefreshIsAlreadyRunning() {
        InMemoryLiveTrendGameRepository gameRepository = new InMemoryLiveTrendGameRepository();
        InMemoryLiveTrendRefreshStatusRepository statusRepository = new InMemoryLiveTrendRefreshStatusRepository();
        InMemoryLiveTrendPlatformStatusRepository platformStatusRepository = new InMemoryLiveTrendPlatformStatusRepository();
        LiveTrendProperties properties = liveTrendProperties(false);
        LiveTrendPlatformStatusService platformStatusService = platformStatusService(platformStatusRepository, properties);
        LiveTrendRefreshStatusManager statusManager = statusManager(statusRepository, platformStatusRepository, properties, platformStatusService);
        LiveTrendService service = liveTrendService(gameRepository, platformStatusService, properties);
        LiveTrendRefreshCoordinator coordinator = new LiveTrendRefreshCoordinator(service, statusManager, platformStatusService);
        assertTrue(statusManager.tryStart());

        LiveTrendRefreshResponse response = coordinator.refreshLiveTrends();

        assertEquals("SKIPPED", response.status());
        assertEquals(0, response.refreshedCount());
        assertEquals("RUNNING", statusRepository.currentStatus().getLastRefreshStatus());
        assertTrue(statusRepository.currentStatus().isRunning());

        statusManager.complete("SUCCESS", "테스트 종료");
    }

    @Test
    void refreshLiveTrends_updatesRefreshAndPlatformStatusesAfterManualRun() {
        InMemoryLiveTrendGameRepository gameRepository = new InMemoryLiveTrendGameRepository();
        InMemoryLiveTrendRefreshStatusRepository statusRepository = new InMemoryLiveTrendRefreshStatusRepository();
        InMemoryLiveTrendPlatformStatusRepository platformStatusRepository = new InMemoryLiveTrendPlatformStatusRepository();
        LiveTrendProperties properties = liveTrendProperties(false);
        properties.getPlatforms().getTwitch().setClientId("tw-client-id");
        properties.getPlatforms().getTwitch().setClientSecret("tw-client-secret");
        properties.getPlatforms().getChzzk().setClientId("chzzk-client-id");
        properties.getPlatforms().getChzzk().setClientSecret("chzzk-client-secret");
        LiveTrendPlatformStatusService platformStatusService = platformStatusService(platformStatusRepository, properties);
        LiveTrendRefreshStatusManager statusManager = statusManager(statusRepository, platformStatusRepository, properties, platformStatusService);
        LiveTrendService service = liveTrendService(
                gameRepository,
                platformStatusService,
                properties,
                List.of(
                        fakeSignalClient("TWITCH", "Counter-Strike 2", 12, 1_200),
                        fakeSignalClient("CHZZK", "PUBG", 8, 900)
                )
        );
        LiveTrendRefreshCoordinator coordinator = new LiveTrendRefreshCoordinator(service, statusManager, platformStatusService);

        LiveTrendRefreshResponse response = coordinator.refreshLiveTrends();

        assertEquals("PARTIAL_SUCCESS", response.status());
        assertEquals("PARTIAL_SUCCESS", statusRepository.currentStatus().getLastRefreshStatus());
        assertFalse(statusRepository.currentStatus().isRunning());
        assertNotNull(statusRepository.currentStatus().getLastRefreshStartedAt());
        assertNotNull(statusRepository.currentStatus().getLastRefreshCompletedAt());

        LiveTrendPlatformStatus twitchStatus = platformStatusRepository.findByPlatform("TWITCH").orElseThrow();
        LiveTrendPlatformStatus chzzkStatus = platformStatusRepository.findByPlatform("CHZZK").orElseThrow();
        LiveTrendPlatformStatus steamStatus = platformStatusRepository.findByPlatform("STEAM").orElseThrow();
        LiveTrendPlatformStatus soopStatus = platformStatusRepository.findByPlatform("SOOP").orElseThrow();

        assertEquals("SUCCESS", twitchStatus.getStatus());
        assertNotNull(twitchStatus.getLastSuccessAt());
        assertEquals("SUCCESS", chzzkStatus.getStatus());
        assertNotNull(chzzkStatus.getLastSuccessAt());
        assertEquals("PUBLIC_OR_FALLBACK", steamStatus.getStatus());
        assertTrue(steamStatus.getMessage().contains("공개 API"));
        assertEquals("MISSING_CREDENTIALS", soopStatus.getStatus());
    }

    @Test
    void refreshCoordinator_treatsSteamNotUsedAsPartialSuccess() {
        InMemoryLiveTrendGameRepository gameRepository = new InMemoryLiveTrendGameRepository();
        InMemoryLiveTrendRefreshStatusRepository statusRepository = new InMemoryLiveTrendRefreshStatusRepository();
        InMemoryLiveTrendPlatformStatusRepository platformStatusRepository = new InMemoryLiveTrendPlatformStatusRepository();
        LiveTrendProperties properties = liveTrendProperties(false);
        properties.getPlatforms().getTwitch().setClientId("tw-client-id");
        properties.getPlatforms().getTwitch().setClientSecret("tw-client-secret");
        properties.getPlatforms().getChzzk().setClientId("chzzk-client-id");
        properties.getPlatforms().getChzzk().setClientSecret("chzzk-client-secret");
        properties.getPlatforms().getSoop().setClientId("soop-client-id");
        properties.getPlatforms().getSoop().setClientSecret("soop-client-secret");
        LiveTrendPlatformStatusService platformStatusService = platformStatusService(platformStatusRepository, properties);
        LiveTrendRefreshStatusManager statusManager = statusManager(statusRepository, platformStatusRepository, properties, platformStatusService);
        LiveTrendService service = liveTrendService(
                gameRepository,
                platformStatusService,
                properties,
                List.of(
                        fakeSignalClient("TWITCH", "Counter-Strike 2", 12, 1_200),
                        fakeSignalClient("CHZZK", "PUBG", 8, 900)
                )
        );
        LiveTrendRefreshCoordinator coordinator = new LiveTrendRefreshCoordinator(service, statusManager, platformStatusService);

        LiveTrendRefreshResponse response = coordinator.refreshLiveTrends();

        assertEquals("PARTIAL_SUCCESS", response.status());
        assertEquals("PARTIAL_SUCCESS", statusRepository.currentStatus().getLastRefreshStatus());
        assertEquals("SUCCESS", platformStatusRepository.findByPlatform("TWITCH").orElseThrow().getStatus());
        assertEquals("SUCCESS", platformStatusRepository.findByPlatform("CHZZK").orElseThrow().getStatus());
        assertEquals("PARTIAL", platformStatusRepository.findByPlatform("SOOP").orElseThrow().getStatus());
        assertEquals("PUBLIC_OR_FALLBACK", platformStatusRepository.findByPlatform("STEAM").orElseThrow().getStatus());
    }

    @Test
    void refreshCoordinator_updatesExistingRefreshStatusRowWithoutDuplicateInsert() {
        InMemoryLiveTrendGameRepository gameRepository = new InMemoryLiveTrendGameRepository();
        InMemoryLiveTrendRefreshStatusRepository statusRepository = new InMemoryLiveTrendRefreshStatusRepository();
        InMemoryLiveTrendPlatformStatusRepository platformStatusRepository = new InMemoryLiveTrendPlatformStatusRepository();
        LocalDateTime previousTime = LocalDateTime.now().minusMinutes(5);
        statusRepository.seedStatus(LiveTrendRefreshStatus.builder()
                .id(1L)
                .running(false)
                .lastRefreshStartedAt(previousTime)
                .lastRefreshCompletedAt(previousTime)
                .lastRefreshStatus("NEVER_RUN")
                .lastRefreshMessage("테스트 기존 상태")
                .updatedAt(previousTime)
                .build());
        LiveTrendRefreshCoordinator coordinator = refreshCoordinator(
                gameRepository,
                statusRepository,
                platformStatusRepository
        );

        coordinator.refreshLiveTrends();
        coordinator.refreshLiveTrends();

        assertEquals(1L, statusRepository.count());
        assertEquals(1L, statusRepository.currentStatus().getId());
        assertFalse(statusRepository.currentStatus().getLastRefreshStatus().equals("NEVER_RUN"));
        assertNotNull(statusRepository.currentStatus().getLastRefreshCompletedAt());
    }

    @Test
    void getStatus_recoversFromSavedGamesWhenRefreshBypassesCoordinator() {
        InMemoryLiveTrendGameRepository gameRepository = new InMemoryLiveTrendGameRepository();
        InMemoryLiveTrendRefreshStatusRepository statusRepository = new InMemoryLiveTrendRefreshStatusRepository();
        InMemoryLiveTrendPlatformStatusRepository platformStatusRepository = new InMemoryLiveTrendPlatformStatusRepository();
        LiveTrendProperties properties = liveTrendProperties(false);
        properties.getPlatforms().getTwitch().setClientId("tw-client-id");
        properties.getPlatforms().getTwitch().setClientSecret("tw-client-secret");
        properties.getPlatforms().getChzzk().setClientId("chzzk-client-id");
        properties.getPlatforms().getChzzk().setClientSecret("chzzk-client-secret");
        LiveTrendPlatformStatusService platformStatusService = platformStatusService(platformStatusRepository, properties);
        LiveTrendRefreshStatusManager statusManager = statusManager(
                gameRepository,
                statusRepository,
                platformStatusRepository,
                properties,
                platformStatusService
        );
        LiveTrendService service = liveTrendService(
                gameRepository,
                platformStatusService,
                properties,
                List.of(
                        fakeSignalClient("TWITCH", "Counter-Strike 2", 12, 1_200),
                        fakeSignalClient("CHZZK", "PUBG", 8, 900)
                )
        );

        service.refreshLiveTrends();
        var status = statusManager.getStatus();

        assertFalse(status.lastRefreshStatus().equals("NEVER_RUN"));
        assertNotNull(status.lastRefreshStartedAt());
        assertNotNull(status.lastRefreshCompletedAt());
        assertTrue(status.platformStatuses().stream().anyMatch(platformStatus -> platformStatus.platform().equals("TWITCH")
                && platformStatus.status().equals("SUCCESS")));
        assertTrue(status.platformStatuses().stream().anyMatch(platformStatus -> platformStatus.platform().equals("CHZZK")
                && platformStatus.status().equals("SUCCESS")));
        assertTrue(status.platformStatuses().stream().anyMatch(platformStatus -> platformStatus.platform().equals("STEAM")
                && !platformStatus.status().equals("NEVER_RUN")));
        assertTrue(status.platformStatuses().stream().anyMatch(platformStatus -> platformStatus.platform().equals("SOOP")
                && platformStatus.status().equals("MISSING_CREDENTIALS")));
    }

    @Test
    void getStatus_reconcilesNeverRunPlatformStatusesFromDatabaseSnapshot() {
        InMemoryLiveTrendGameRepository gameRepository = new InMemoryLiveTrendGameRepository();
        gameRepository.save(liveTrendGame("Twitch Real", "TWITCH", 91.0, "COMPLETE", "REAL"));
        gameRepository.save(liveTrendGame("Chzzk Real", "CHZZK", 88.0, "COMPLETE", "REAL"));
        InMemoryLiveTrendRefreshStatusRepository statusRepository = new InMemoryLiveTrendRefreshStatusRepository();
        InMemoryLiveTrendPlatformStatusRepository platformStatusRepository = new InMemoryLiveTrendPlatformStatusRepository();
        LiveTrendProperties properties = liveTrendProperties(false);
        properties.getPlatforms().getTwitch().setClientId("tw-client-id");
        properties.getPlatforms().getTwitch().setClientSecret("tw-client-secret");
        properties.getPlatforms().getChzzk().setClientId("chzzk-client-id");
        properties.getPlatforms().getChzzk().setClientSecret("chzzk-client-secret");
        LiveTrendPlatformStatusService platformStatusService = platformStatusService(platformStatusRepository, properties);
        LiveTrendRefreshStatusManager statusManager = statusManager(
                gameRepository,
                statusRepository,
                platformStatusRepository,
                properties,
                platformStatusService
        );

        var status = statusManager.getStatus();

        assertEquals("SUCCESS", status.lastRefreshStatus());
        assertEquals("저장된 최신 라이브 트렌드 데이터를 기준으로 상태를 보정했습니다.", status.lastRefreshMessage());
        assertNotNull(status.lastRefreshCompletedAt());
        assertTrue(status.platformStatuses().stream().anyMatch(platformStatus -> platformStatus.platform().equals("TWITCH")
                && platformStatus.status().equals("SUCCESS")));
        assertTrue(status.platformStatuses().stream().anyMatch(platformStatus -> platformStatus.platform().equals("CHZZK")
                && platformStatus.status().equals("SUCCESS")));
        assertTrue(status.platformStatuses().stream().anyMatch(platformStatus -> platformStatus.platform().equals("STEAM")
                && platformStatus.status().equals("PUBLIC_OR_FALLBACK")));
        assertTrue(status.platformStatuses().stream().anyMatch(platformStatus -> platformStatus.platform().equals("SOOP")
                && platformStatus.status().equals("MISSING_CREDENTIALS")));
    }

    @Test
    void findTopLiveTrendGames_returnsHighestTrendScoreFirst() {
        InMemoryLiveTrendGameRepository gameRepository = new InMemoryLiveTrendGameRepository();
        gameRepository.save(liveTrendGame("Low Signal", 40.0));
        gameRepository.save(liveTrendGame("High Signal", 88.0));
        LiveTrendService service = liveTrendService(
                gameRepository,
                new InMemoryLiveTrendRefreshStatusRepository(),
                new InMemoryLiveTrendPlatformStatusRepository(),
                true
        );

        List<LiveTrendGameResponse> topGames = service.findTopLiveTrendGames(1);

        assertEquals(1, topGames.size());
        assertEquals("High Signal", topGames.get(0).title());
    }

    @Test
    void findTopLiveTrendGames_filtersBySourcePlatform() {
        InMemoryLiveTrendGameRepository gameRepository = new InMemoryLiveTrendGameRepository();
        gameRepository.save(liveTrendGame("Twitch Game", "TWITCH", 88.0));
        gameRepository.save(liveTrendGame("Chzzk Game", "CHZZK", 91.0));
        gameRepository.save(liveTrendGame("Soop Game", "SOOP", 72.0));
        LiveTrendService service = liveTrendService(
                gameRepository,
                new InMemoryLiveTrendRefreshStatusRepository(),
                new InMemoryLiveTrendPlatformStatusRepository(),
                true
        );

        List<LiveTrendGameResponse> soopGames = service.findTopLiveTrendGames(10, "soop");
        List<LiveTrendGameResponse> allGames = service.findTopLiveTrendGames(10, "ALL");

        assertEquals(1, soopGames.size());
        assertEquals("SOOP", soopGames.get(0).source());
        assertEquals("Soop Game", soopGames.get(0).title());
        assertEquals(3, allGames.size());
    }

    @Test
    void findTopLiveTrendGames_hidesFallbackDataByDefault() {
        InMemoryLiveTrendGameRepository gameRepository = new InMemoryLiveTrendGameRepository();
        gameRepository.save(liveTrendGame("Soop Fallback", "SOOP", 72.0, "PARTIAL", "FALLBACK"));
        gameRepository.save(liveTrendGame("Steam Real", "STEAM", 80.0, "COMPLETE", "REAL"));
        LiveTrendService service = liveTrendService(
                gameRepository,
                new InMemoryLiveTrendRefreshStatusRepository(),
                new InMemoryLiveTrendPlatformStatusRepository(),
                false
        );

        List<LiveTrendGameResponse> soopGames = service.findTopLiveTrendGames(10, "SOOP");
        List<LiveTrendGameResponse> allGames = service.findTopLiveTrendGames(10);

        assertEquals(0, soopGames.size());
        assertEquals(1, allGames.size());
        assertEquals("Steam Real", allGames.get(0).title());
    }

    @Test
    void findTopLiveTrendGames_canExposeFallbackDataForDemo() {
        InMemoryLiveTrendGameRepository gameRepository = new InMemoryLiveTrendGameRepository();
        gameRepository.save(liveTrendGame("Soop Fallback", "SOOP", 72.0, "PARTIAL", "FALLBACK"));
        LiveTrendService service = liveTrendService(
                gameRepository,
                new InMemoryLiveTrendRefreshStatusRepository(),
                new InMemoryLiveTrendPlatformStatusRepository(),
                true
        );

        List<LiveTrendGameResponse> soopGames = service.findTopLiveTrendGames(10, "SOOP");

        assertEquals(1, soopGames.size());
        assertEquals("FALLBACK", soopGames.get(0).dataOrigin());
    }

    @Test
    void findTopLiveTrendGames_throwsBadRequestForInvalidPlatform() {
        LiveTrendService service = liveTrendService(
                new InMemoryLiveTrendGameRepository(),
                new InMemoryLiveTrendRefreshStatusRepository(),
                new InMemoryLiveTrendPlatformStatusRepository()
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.findTopLiveTrendGames(10, "YOUTUBE")
        );

        assertEquals(BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void getStatus_returnsPlatformStatusesEvenBeforeRefresh() {
        InMemoryLiveTrendRefreshStatusRepository statusRepository = new InMemoryLiveTrendRefreshStatusRepository();
        InMemoryLiveTrendPlatformStatusRepository platformStatusRepository = new InMemoryLiveTrendPlatformStatusRepository();
        LiveTrendRefreshStatusManager statusManager = statusManager(statusRepository, platformStatusRepository);

        List<LiveTrendPlatformStatusResponse> platformStatuses = statusManager.getStatus().platformStatuses();

        assertEquals(4, platformStatuses.size());
        assertTrue(platformStatuses.stream().anyMatch(status -> status.platform().equals("TWITCH")
                && !status.configured()
                && status.status().equals("MISSING_CREDENTIALS")));
        assertTrue(platformStatuses.stream().anyMatch(status -> status.platform().equals("STEAM")
                && status.configured()
                && status.status().equals("NEVER_RUN")));
    }

    @Test
    void getPlatformStatuses_reportsCredentialPresenceWithoutSecretValues() {
        InMemoryLiveTrendPlatformStatusRepository platformStatusRepository = new InMemoryLiveTrendPlatformStatusRepository();
        LiveTrendProperties properties = liveTrendProperties(false);
        properties.getPlatforms().getTwitch().setClientId("tw-client-id");
        properties.getPlatforms().getTwitch().setClientSecret("tw-client-secret");
        properties.getPlatforms().getChzzk().setClientId("chzzk-client-id");
        properties.getPlatforms().getChzzk().setApiBaseUrl("https://openapi.chzzk.naver.com");
        LiveTrendPlatformStatusService service = platformStatusService(platformStatusRepository, properties);

        List<LiveTrendPlatformStatusResponse> platformStatuses = service.getPlatformStatuses();

        LiveTrendPlatformStatusResponse twitchStatus = platformStatuses.stream()
                .filter(status -> status.platform().equals("TWITCH"))
                .findFirst()
                .orElseThrow();
        LiveTrendPlatformStatusResponse chzzkStatus = platformStatuses.stream()
                .filter(status -> status.platform().equals("CHZZK"))
                .findFirst()
                .orElseThrow();

        assertTrue(twitchStatus.configured());
        assertEquals("NEVER_RUN", twitchStatus.status());
        assertTrue(twitchStatus.message().contains("clientId=true"));
        assertTrue(twitchStatus.message().contains("clientSecret=true"));
        assertFalse(twitchStatus.message().contains("tw-client-secret"));

        assertFalse(chzzkStatus.configured());
        assertEquals("MISSING_CREDENTIALS", chzzkStatus.status());
        assertTrue(chzzkStatus.message().contains("clientId=true"));
        assertTrue(chzzkStatus.message().contains("clientSecret=false"));
        assertTrue(chzzkStatus.message().contains("apiBaseUrl=true"));
        assertFalse(chzzkStatus.message().contains("chzzk-client-id"));
    }

    private LiveTrendService liveTrendService(
            InMemoryLiveTrendGameRepository gameRepository,
            InMemoryLiveTrendRefreshStatusRepository statusRepository,
            InMemoryLiveTrendPlatformStatusRepository platformStatusRepository
    ) {
        return liveTrendService(gameRepository, statusRepository, platformStatusRepository, false);
    }

    private LiveTrendService liveTrendService(
            InMemoryLiveTrendGameRepository gameRepository,
            InMemoryLiveTrendRefreshStatusRepository statusRepository,
            InMemoryLiveTrendPlatformStatusRepository platformStatusRepository,
            boolean exposeFallbackData
    ) {
        LiveTrendProperties properties = liveTrendProperties(exposeFallbackData);
        LiveTrendPlatformStatusService platformStatusService = platformStatusService(platformStatusRepository, properties);
        return liveTrendService(gameRepository, platformStatusService, properties);
    }

    private LiveTrendService liveTrendService(
            InMemoryLiveTrendGameRepository gameRepository,
            LiveTrendPlatformStatusService platformStatusService,
            LiveTrendProperties properties
    ) {
        return liveTrendService(gameRepository, platformStatusService, properties, List.of());
    }

    private LiveTrendService liveTrendService(
            InMemoryLiveTrendGameRepository gameRepository,
            LiveTrendPlatformStatusService platformStatusService,
            LiveTrendProperties properties,
            List<LiveTrendSignalClient> signalClients
    ) {
        return new LiveTrendService(
                gameRepository.asRepository(),
                new TrendScoreCalculator(),
                platformStatusService,
                properties,
                signalClients
        );
    }

    private LiveTrendRefreshCoordinator refreshCoordinator(
            InMemoryLiveTrendGameRepository gameRepository,
            InMemoryLiveTrendRefreshStatusRepository statusRepository,
            InMemoryLiveTrendPlatformStatusRepository platformStatusRepository
    ) {
        LiveTrendProperties properties = liveTrendProperties(false);
        LiveTrendPlatformStatusService platformStatusService = platformStatusService(platformStatusRepository, properties);
        LiveTrendRefreshStatusManager statusManager = statusManager(
                gameRepository,
                statusRepository,
                platformStatusRepository,
                properties,
                platformStatusService
        );
        LiveTrendService service = liveTrendService(gameRepository, platformStatusService, properties);
        return new LiveTrendRefreshCoordinator(service, statusManager, platformStatusService);
    }

    private LiveTrendRefreshStatusManager statusManager(
            InMemoryLiveTrendRefreshStatusRepository statusRepository,
            InMemoryLiveTrendPlatformStatusRepository platformStatusRepository
    ) {
        LiveTrendProperties properties = liveTrendProperties(false);
        return statusManager(statusRepository, platformStatusRepository, properties);
    }

    private LiveTrendRefreshStatusManager statusManager(
            InMemoryLiveTrendRefreshStatusRepository statusRepository,
            InMemoryLiveTrendPlatformStatusRepository platformStatusRepository,
            LiveTrendProperties properties
    ) {
        return statusManager(
                statusRepository,
                platformStatusRepository,
                properties,
                platformStatusService(platformStatusRepository, properties)
        );
    }

    private LiveTrendRefreshStatusManager statusManager(
            InMemoryLiveTrendRefreshStatusRepository statusRepository,
            InMemoryLiveTrendPlatformStatusRepository platformStatusRepository,
            LiveTrendProperties properties,
            LiveTrendPlatformStatusService platformStatusService
    ) {
        return statusManager(
                new InMemoryLiveTrendGameRepository(),
                statusRepository,
                platformStatusRepository,
                properties,
                platformStatusService
        );
    }

    private LiveTrendRefreshStatusManager statusManager(
            InMemoryLiveTrendGameRepository gameRepository,
            InMemoryLiveTrendRefreshStatusRepository statusRepository,
            InMemoryLiveTrendPlatformStatusRepository platformStatusRepository,
            LiveTrendProperties properties,
            LiveTrendPlatformStatusService platformStatusService
    ) {
        return new LiveTrendRefreshStatusManager(
                statusRepository.asRepository(),
                properties,
                platformStatusService,
                gameRepository.asRepository()
        );
    }

    private LiveTrendPlatformStatusService platformStatusService(
            InMemoryLiveTrendPlatformStatusRepository platformStatusRepository
    ) {
        LiveTrendProperties properties = liveTrendProperties(false);
        return platformStatusService(platformStatusRepository, properties);
    }

    private LiveTrendPlatformStatusService platformStatusService(
            InMemoryLiveTrendPlatformStatusRepository platformStatusRepository,
            LiveTrendProperties properties
    ) {
        return new LiveTrendPlatformStatusService(
                platformStatusRepository.asRepository(),
                properties,
                new SteamProperties("https://store.steampowered.com", 10_000L, "")
        );
    }

    private LiveTrendProperties liveTrendProperties(boolean exposeFallbackData) {
        LiveTrendProperties properties = new LiveTrendProperties();
        properties.setSchedulerEnabled(true);
        properties.setRefreshIntervalMs(1_800_000L);
        properties.setRefreshOnStartup(false);
        properties.setExposeFallbackData(exposeFallbackData);
        return properties;
    }

    private LiveTrendGame liveTrendGame(String title, double trendScore) {
        return liveTrendGame(title, "STEAM", trendScore);
    }

    private LiveTrendGame liveTrendGame(String title, String source, double trendScore) {
        return liveTrendGame(title, source, trendScore, "COMPLETE", "REAL");
    }

    private LiveTrendGame liveTrendGame(
            String title,
            String source,
            double trendScore,
            String signalStatus,
            String dataOrigin
    ) {
        LocalDateTime now = LocalDateTime.now();
        return LiveTrendGame.builder()
                .source(source)
                .title(title)
                .genre("Test")
                .platform("PC")
                .sourceKeyword(title)
                .liveStreamCount(10)
                .totalViewerCount(500)
                .viewerScore(30)
                .streamCountScore(20)
                .streamabilityScore(25)
                .marketSignalScore(45)
                .trendScore(trendScore)
                .signalStatus(signalStatus)
                .dataOrigin(dataOrigin)
                .reason("테스트")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private LiveTrendSignalClient fakeSignalClient(
            String source,
            String gameName,
            int liveStreamCount,
            int totalViewerCount
    ) {
        return new LiveTrendSignalClient() {

            @Override
            public String source() {
                return source;
            }

            @Override
            public List<LiveGameSignal> fetchSignals() {
                return List.of(new LiveGameSignal(
                        source,
                        gameName,
                        gameName,
                        liveStreamCount,
                        totalViewerCount,
                        List.of("channelName=test-channel, liveTitle=test-live, openDate=2026-05-14T00:00:00Z"),
                        "test"
                ));
            }
        };
    }

    private static class InMemoryLiveTrendGameRepository {

        private final List<LiveTrendGame> liveTrendGames = new ArrayList<>();
        private long nextId = 1L;

        LiveTrendGameRepository asRepository() {
            return (LiveTrendGameRepository) Proxy.newProxyInstance(
                    LiveTrendGameRepository.class.getClassLoader(),
                    new Class<?>[]{LiveTrendGameRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "save" -> save((LiveTrendGame) args[0]);
                        case "findBySourceAndTitle" -> findBySourceAndTitle((String) args[0], (String) args[1]);
                        case "findBySourceOrderByTrendScoreDesc" -> findBySourceOrderByTrendScoreDesc((String) args[0]);
                        case "findAllByOrderByTrendScoreDesc" -> findAllByOrderByTrendScoreDesc();
                        case "findTopByOrderByUpdatedAtDesc" -> findTopByOrderByUpdatedAtDesc();
                        case "findTopBySourceOrderByUpdatedAtDesc" -> findTopBySourceOrderByUpdatedAtDesc((String) args[0]);
                        case "findFirstBySourceAndDataOriginOrderByUpdatedAtDesc" ->
                                findFirstBySourceAndDataOriginOrderByUpdatedAtDesc((String) args[0], (String) args[1]);
                        case "findFirstBySourceAndSignalStatusOrderByUpdatedAtDesc" ->
                                findFirstBySourceAndSignalStatusOrderByUpdatedAtDesc((String) args[0], (String) args[1]);
                        case "findById" -> findById((Long) args[0]);
                        case "count" -> (long) count();
                        case "toString" -> "InMemoryLiveTrendGameRepository";
                        default -> throw new UnsupportedOperationException(
                                "테스트에서 사용하지 않는 메서드입니다: " + method.getName()
                        );
                    }
            );
        }

        int count() {
            return liveTrendGames.size();
        }

        private LiveTrendGame save(LiveTrendGame liveTrendGame) {
            LiveTrendGame savedLiveTrendGame = copyWithId(
                    liveTrendGame,
                    liveTrendGame.getId() == null ? nextId++ : liveTrendGame.getId()
            );
            liveTrendGames.removeIf(existingGame -> existingGame.getId().equals(savedLiveTrendGame.getId()));
            liveTrendGames.add(savedLiveTrendGame);
            return savedLiveTrendGame;
        }

        private Optional<LiveTrendGame> findBySourceAndTitle(String source, String title) {
            return liveTrendGames.stream()
                    .filter(game -> game.getSource().equals(source) && game.getTitle().equals(title))
                    .findFirst();
        }

        private List<LiveTrendGame> findAllByOrderByTrendScoreDesc() {
            return liveTrendGames.stream()
                    .sorted(Comparator.comparing(LiveTrendGame::getTrendScore).reversed())
                    .toList();
        }

        private List<LiveTrendGame> findBySourceOrderByTrendScoreDesc(String source) {
            return liveTrendGames.stream()
                    .filter(game -> game.getSource().equals(source))
                    .sorted(Comparator.comparing(LiveTrendGame::getTrendScore).reversed())
                    .toList();
        }

        private Optional<LiveTrendGame> findTopByOrderByUpdatedAtDesc() {
            return liveTrendGames.stream()
                    .max(Comparator.comparing(this::updatedAtOrMin));
        }

        private Optional<LiveTrendGame> findTopBySourceOrderByUpdatedAtDesc(String source) {
            return liveTrendGames.stream()
                    .filter(game -> game.getSource().equals(source))
                    .max(Comparator.comparing(this::updatedAtOrMin));
        }

        private Optional<LiveTrendGame> findFirstBySourceAndDataOriginOrderByUpdatedAtDesc(String source, String dataOrigin) {
            return liveTrendGames.stream()
                    .filter(game -> game.getSource().equals(source))
                    .filter(game -> dataOrigin.equals(game.getDataOrigin()))
                    .max(Comparator.comparing(this::updatedAtOrMin));
        }

        private Optional<LiveTrendGame> findFirstBySourceAndSignalStatusOrderByUpdatedAtDesc(String source, String signalStatus) {
            return liveTrendGames.stream()
                    .filter(game -> game.getSource().equals(source))
                    .filter(game -> signalStatus.equals(game.getSignalStatus()))
                    .max(Comparator.comparing(this::updatedAtOrMin));
        }

        private Optional<LiveTrendGame> findById(Long id) {
            return liveTrendGames.stream()
                    .filter(game -> game.getId().equals(id))
                    .findFirst();
        }

        private LocalDateTime updatedAtOrMin(LiveTrendGame game) {
            return game.getUpdatedAt() == null ? LocalDateTime.MIN : game.getUpdatedAt();
        }

        private LiveTrendGame copyWithId(LiveTrendGame liveTrendGame, Long id) {
            assertNotNull(liveTrendGame.getCreatedAt());
            assertNotNull(liveTrendGame.getUpdatedAt());
            return LiveTrendGame.builder()
                    .id(id)
                    .source(liveTrendGame.getSource())
                    .title(liveTrendGame.getTitle())
                    .genre(liveTrendGame.getGenre())
                    .platform(liveTrendGame.getPlatform())
                    .sourceKeyword(liveTrendGame.getSourceKeyword())
                    .liveStreamCount(liveTrendGame.getLiveStreamCount())
                    .totalViewerCount(liveTrendGame.getTotalViewerCount())
                    .viewerScore(liveTrendGame.getViewerScore())
                    .streamCountScore(liveTrendGame.getStreamCountScore())
                    .streamabilityScore(liveTrendGame.getStreamabilityScore())
                    .marketSignalScore(liveTrendGame.getMarketSignalScore())
                    .trendScore(liveTrendGame.getTrendScore())
                    .signalStatus(liveTrendGame.getSignalStatus())
                    .dataOrigin(liveTrendGame.getDataOrigin())
                    .reason(liveTrendGame.getReason())
                    .createdAt(liveTrendGame.getCreatedAt())
                    .updatedAt(liveTrendGame.getUpdatedAt())
                    .build();
        }
    }

    private static class InMemoryLiveTrendRefreshStatusRepository {

        private LiveTrendRefreshStatus status;

        LiveTrendRefreshStatusRepository asRepository() {
            return (LiveTrendRefreshStatusRepository) Proxy.newProxyInstance(
                    LiveTrendRefreshStatusRepository.class.getClassLoader(),
                    new Class<?>[]{LiveTrendRefreshStatusRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "save" -> save((LiveTrendRefreshStatus) args[0]);
                        case "findById" -> findById((Long) args[0]);
                        case "findAll" -> findAll();
                        case "deleteById" -> {
                            deleteById((Long) args[0]);
                            yield null;
                        }
                        case "count" -> status == null ? 0L : 1L;
                        case "updateStatusRow" -> updateStatusRow(args);
                        case "insertStatusRow" -> insertStatusRow(args);
                        case "toString" -> "InMemoryLiveTrendRefreshStatusRepository";
                        default -> throw new UnsupportedOperationException(
                                "테스트에서 사용하지 않는 메서드입니다: " + method.getName()
                        );
                    }
            );
        }

        LiveTrendRefreshStatus currentStatus() {
            assertNotNull(status);
            return status;
        }

        long count() {
            return status == null ? 0L : 1L;
        }

        void seedStatus(LiveTrendRefreshStatus status) {
            this.status = status;
        }

        private LiveTrendRefreshStatus save(LiveTrendRefreshStatus status) {
            this.status = status;
            return status;
        }

        private int updateStatusRow(Object[] args) {
            Long id = (Long) args[0];
            if (status == null || !status.getId().equals(id)) {
                return 0;
            }
            status = statusFromArgs(args);
            return 1;
        }

        private int insertStatusRow(Object[] args) {
            Long id = (Long) args[0];
            if (status != null && status.getId().equals(id)) {
                throw new DuplicateKeyException("duplicate id=" + id);
            }
            status = statusFromArgs(args);
            return 1;
        }

        private Optional<LiveTrendRefreshStatus> findById(Long id) {
            return Optional.ofNullable(status)
                    .filter(savedStatus -> savedStatus.getId().equals(id));
        }

        private List<LiveTrendRefreshStatus> findAll() {
            if (status == null) {
                return List.of();
            }
            return List.of(status);
        }

        private void deleteById(Long id) {
            if (status != null && status.getId().equals(id)) {
                status = null;
            }
        }

        private LiveTrendRefreshStatus statusFromArgs(Object[] args) {
            return LiveTrendRefreshStatus.builder()
                    .id((Long) args[0])
                    .running((boolean) args[1])
                    .lastRefreshStartedAt((LocalDateTime) args[2])
                    .lastRefreshCompletedAt((LocalDateTime) args[3])
                    .lastRefreshStatus((String) args[4])
                    .lastRefreshMessage((String) args[5])
                    .nextRefreshEstimate((LocalDateTime) args[6])
                    .updatedAt((LocalDateTime) args[7])
                    .build();
        }
    }

    private static class InMemoryLiveTrendPlatformStatusRepository {

        private final List<LiveTrendPlatformStatus> statuses = new ArrayList<>();

        LiveTrendPlatformStatusRepository asRepository() {
            return (LiveTrendPlatformStatusRepository) Proxy.newProxyInstance(
                    LiveTrendPlatformStatusRepository.class.getClassLoader(),
                    new Class<?>[]{LiveTrendPlatformStatusRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "save" -> save((LiveTrendPlatformStatus) args[0]);
                        case "findById" -> findById((Long) args[0]);
                        case "findByPlatform" -> findByPlatform((String) args[0]);
                        case "toString" -> "InMemoryLiveTrendPlatformStatusRepository";
                        default -> throw new UnsupportedOperationException(
                                "테스트에서 사용하지 않는 메서드입니다: " + method.getName()
                        );
                    }
            );
        }

        Optional<LiveTrendPlatformStatus> findByPlatform(String platform) {
            return statuses.stream()
                    .filter(status -> status.getPlatform().equals(platform))
                    .findFirst();
        }

        private LiveTrendPlatformStatus save(LiveTrendPlatformStatus status) {
            statuses.removeIf(savedStatus -> savedStatus.getId().equals(status.getId()));
            statuses.add(status);
            return status;
        }

        private Optional<LiveTrendPlatformStatus> findById(Long id) {
            return statuses.stream()
                    .filter(status -> status.getId().equals(id))
                    .findFirst();
        }
    }
}
