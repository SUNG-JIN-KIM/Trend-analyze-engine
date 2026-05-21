package com.gametrend.agent.trend.service;

import com.gametrend.agent.game.entity.Game;
import com.gametrend.agent.game.repository.GameRepository;
import com.gametrend.agent.infrastructure.steam.SteamClient;
import com.gametrend.agent.infrastructure.steam.SteamReviewSummary;
import com.gametrend.agent.trend.dto.TrendGameResponse;
import com.gametrend.agent.trend.dto.TrendRefreshResponse;
import com.gametrend.agent.trend.entity.TrendGame;
import com.gametrend.agent.trend.repository.TrendGameRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrendGameServiceTest {

    @Test
    void refresh_usesDefaultSeedsAndSurvivesSteamFailures() {
        InMemoryTrendGameRepository trendGameRepository = new InMemoryTrendGameRepository();
        GameRepository gameRepository = gameRepositoryWith(List.of(game("Counter-Strike 2", 93.0)));
        SteamClient steamClient = appId -> {
            if (appId == 730) {
                return new SteamReviewSummary(
                        appId,
                        "Very Positive",
                        900_000,
                        100_000,
                        1_000_000,
                        0.90
                );
            }
            throw new RuntimeException("테스트용 Steam 실패");
        };
        TrendGameService trendGameService = new TrendGameService(
                trendGameRepository.asRepository(),
                steamClient,
                gameRepository,
                new TrendScoreCalculator()
        );

        TrendRefreshResponse response = trendGameService.refresh(null);

        assertEquals(7, response.requestedCount());
        assertEquals(7, response.refreshedCount());
        assertEquals(7, response.games().size());
        assertTrue(response.partialCount() > 0);
        assertEquals(7, trendGameRepository.count());
        assertTrue(response.games().stream().allMatch(game -> game.trendScore() >= 0 && game.trendScore() <= 100));
        assertTrue(response.games().stream().anyMatch(game -> game.title().equals("Counter-Strike 2")
                && game.internalRecommendationScore() == 93.0));
    }

    @Test
    void findTopTrendGames_returnsHighestTrendScoreFirst() {
        InMemoryTrendGameRepository trendGameRepository = new InMemoryTrendGameRepository();
        trendGameRepository.save(trendGame("Low Signal", 40.0));
        trendGameRepository.save(trendGame("High Signal", 88.0));
        TrendGameService trendGameService = new TrendGameService(
                trendGameRepository.asRepository(),
                appId -> {
                    throw new RuntimeException("사용하지 않음");
                },
                gameRepositoryWith(List.of()),
                new TrendScoreCalculator()
        );

        List<TrendGameResponse> topGames = trendGameService.findTopTrendGames(1);

        assertEquals(1, topGames.size());
        assertEquals("High Signal", topGames.get(0).title());
    }

    private Game game(String title, double recommendationScore) {
        LocalDateTime now = LocalDateTime.now();
        return Game.builder()
                .title(title)
                .genre("FPS")
                .platform("PC")
                .playStyle("Competitive")
                .streamabilityScore(90)
                .webcamFitScore(50)
                .ttsFitScore(50)
                .sttFitScore(50)
                .noveltyScore(70)
                .devFeasibilityScore(60)
                .marketSignalScore(90)
                .recommendationScore(recommendationScore)
                .reason("테스트 게임")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private GameRepository gameRepositoryWith(List<Game> games) {
        return (GameRepository) Proxy.newProxyInstance(
                GameRepository.class.getClassLoader(),
                new Class<?>[]{GameRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findAll" -> games;
                    case "toString" -> "InMemoryGameRepository";
                    default -> throw new UnsupportedOperationException(
                            "테스트에서 사용하지 않는 메서드입니다: " + method.getName()
                    );
                }
        );
    }

    private TrendGame trendGame(String title, double trendScore) {
        LocalDateTime now = LocalDateTime.now();
        return TrendGame.builder()
                .title(title)
                .genre("Test")
                .platform("PC")
                .steamReviewScore(50)
                .steamTotalReviews(100)
                .steamPositiveRate(0.8)
                .twitchLiveStreamCount(10)
                .twitchTotalViewerCount(500)
                .twitchViewerScore(30)
                .twitchStreamCountScore(20)
                .streamabilityScore(25)
                .marketSignalScore(45)
                .internalRecommendationScore(50)
                .trendScore(trendScore)
                .signalStatus("PARTIAL")
                .reason("테스트")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static class InMemoryTrendGameRepository {

        private final List<TrendGame> trendGames = new ArrayList<>();
        private long nextId = 1L;

        TrendGameRepository asRepository() {
            return (TrendGameRepository) Proxy.newProxyInstance(
                    TrendGameRepository.class.getClassLoader(),
                    new Class<?>[]{TrendGameRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "save" -> save((TrendGame) args[0]);
                        case "findByTitle" -> findByTitle((String) args[0]);
                        case "findAllByOrderByTrendScoreDesc" -> findAllByOrderByTrendScoreDesc();
                        case "findById" -> findById((Long) args[0]);
                        case "toString" -> "InMemoryTrendGameRepository";
                        default -> throw new UnsupportedOperationException(
                                "테스트에서 사용하지 않는 메서드입니다: " + method.getName()
                        );
                    }
            );
        }

        int count() {
            return trendGames.size();
        }

        private TrendGame save(TrendGame trendGame) {
            TrendGame savedTrendGame = copyWithId(
                    trendGame,
                    trendGame.getId() == null ? nextId++ : trendGame.getId()
            );
            trendGames.removeIf(existingGame -> existingGame.getId().equals(savedTrendGame.getId()));
            trendGames.add(savedTrendGame);
            return savedTrendGame;
        }

        private Optional<TrendGame> findByTitle(String title) {
            return trendGames.stream()
                    .filter(trendGame -> trendGame.getTitle().equals(title))
                    .findFirst();
        }

        private List<TrendGame> findAllByOrderByTrendScoreDesc() {
            return trendGames.stream()
                    .sorted(Comparator.comparing(TrendGame::getTrendScore).reversed())
                    .toList();
        }

        private Optional<TrendGame> findById(Long id) {
            return trendGames.stream()
                    .filter(trendGame -> trendGame.getId().equals(id))
                    .findFirst();
        }

        private TrendGame copyWithId(TrendGame trendGame, Long id) {
            assertNotNull(trendGame.getCreatedAt());
            assertNotNull(trendGame.getUpdatedAt());
            return TrendGame.builder()
                    .id(id)
                    .title(trendGame.getTitle())
                    .genre(trendGame.getGenre())
                    .platform(trendGame.getPlatform())
                    .steamAppId(trendGame.getSteamAppId())
                    .twitchKeyword(trendGame.getTwitchKeyword())
                    .steamReviewScore(trendGame.getSteamReviewScore())
                    .steamTotalReviews(trendGame.getSteamTotalReviews())
                    .steamPositiveRate(trendGame.getSteamPositiveRate())
                    .twitchLiveStreamCount(trendGame.getTwitchLiveStreamCount())
                    .twitchTotalViewerCount(trendGame.getTwitchTotalViewerCount())
                    .twitchViewerScore(trendGame.getTwitchViewerScore())
                    .twitchStreamCountScore(trendGame.getTwitchStreamCountScore())
                    .streamabilityScore(trendGame.getStreamabilityScore())
                    .marketSignalScore(trendGame.getMarketSignalScore())
                    .internalRecommendationScore(trendGame.getInternalRecommendationScore())
                    .trendScore(trendGame.getTrendScore())
                    .signalStatus(trendGame.getSignalStatus())
                    .reason(trendGame.getReason())
                    .createdAt(trendGame.getCreatedAt())
                    .updatedAt(trendGame.getUpdatedAt())
                    .build();
        }
    }
}
