package com.gametrend.agent.steam.service;

import com.gametrend.agent.game.dto.GameResponse;
import com.gametrend.agent.game.entity.Game;
import com.gametrend.agent.game.repository.GameRepository;
import com.gametrend.agent.game.service.GameService;
import com.gametrend.agent.infrastructure.steam.SteamClient;
import com.gametrend.agent.infrastructure.steam.SteamReviewSummary;
import com.gametrend.agent.steam.dto.SteamImportRequest;
import com.gametrend.agent.steam.dto.SteamImportResponse;
import com.gametrend.agent.steam.dto.SteamReviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SteamImportServiceTest {

    private FakeSteamClient steamClient;
    private InMemoryGameRepository gameRepository;
    private SteamImportService steamImportService;

    @BeforeEach
    void setUp() {
        steamClient = new FakeSteamClient(new SteamReviewSummary(
                620,
                "Very Positive",
                900,
                100,
                1_000,
                0.9
        ));
        gameRepository = new InMemoryGameRepository();
        GameService gameService = new GameService(gameRepository.asRepository());
        steamImportService = new SteamImportService(steamClient, gameService);
    }

    @Test
    void getReviewSummary_convertsSteamReviewToMarketSignalScore() {
        SteamReviewResponse response = steamImportService.getReviewSummary(620);

        assertEquals(620, response.appId());
        assertEquals("Very Positive", response.reviewScoreDesc());
        assertEquals(1_000, response.totalReviews());
        assertEquals(0.9, response.positiveRate());
        assertEquals(86, response.marketSignalScore());
    }

    @Test
    void importGame_usesSteamMarketSignalAndSafeDefaultScores() {
        SteamImportResponse response = steamImportService.importGame(new SteamImportRequest(
                620,
                "Portal 2",
                "Puzzle",
                "PC",
                "Co-op",
                null,
                null,
                80,
                null,
                null,
                null,
                "협동 퍼즐과 방송 반응이 잘 어울립니다."
        ));

        GameResponse game = response.game();

        assertEquals("Portal 2", game.title());
        assertEquals(86, game.marketSignalScore());
        assertEquals(85, game.streamabilityScore());
        assertEquals(50, game.webcamFitScore());
        assertEquals(80, game.ttsFitScore());
        assertEquals(50, game.sttFitScore());
        assertEquals(71, game.noveltyScore());
        assertEquals(65, game.devFeasibilityScore());
        assertTrue(game.reason().contains("Steam 리뷰 요약"));
        assertTrue(game.reason().contains("Very Positive"));
        assertEquals(1, gameRepository.count());
    }

    @Test
    void importGame_usesConservativeMarketSignalWhenThereAreNoReviews() {
        steamClient.setSummary(new SteamReviewSummary(
                123,
                "No user reviews",
                0,
                0,
                0,
                0.0
        ));

        SteamImportResponse response = steamImportService.importGame(new SteamImportRequest(
                123,
                "Unknown Indie",
                "Adventure",
                "PC",
                "Solo",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertEquals(40, response.steamReview().marketSignalScore());
        assertEquals(40, response.game().marketSignalScore());
        assertTrue(response.game().reason().contains("총 리뷰 0건"));
    }

    private static class FakeSteamClient implements SteamClient {

        private SteamReviewSummary summary;

        FakeSteamClient(SteamReviewSummary summary) {
            this.summary = summary;
        }

        void setSummary(SteamReviewSummary summary) {
            this.summary = summary;
        }

        @Override
        public SteamReviewSummary getReviewSummary(int appId) {
            return summary;
        }
    }

    private static class InMemoryGameRepository {

        private final List<Game> games = new ArrayList<>();
        private long nextId = 1L;

        GameRepository asRepository() {
            return (GameRepository) Proxy.newProxyInstance(
                    GameRepository.class.getClassLoader(),
                    new Class<?>[]{GameRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "save" -> save((Game) args[0]);
                        case "findAll" -> List.copyOf(games);
                        case "findAllByOrderByRecommendationScoreDesc" -> findAllByOrderByRecommendationScoreDesc();
                        case "findById" -> findById((Long) args[0]);
                        case "count" -> count();
                        case "toString" -> "InMemoryGameRepository";
                        default -> throw new UnsupportedOperationException(
                                "테스트에서 사용하지 않는 메서드입니다: " + method.getName()
                        );
                    }
            );
        }

        long count() {
            return games.size();
        }

        private Optional<Game> findById(Long id) {
            return games.stream()
                    .filter(game -> game.getId().equals(id))
                    .findFirst();
        }

        private Game save(Game game) {
            Game savedGame = copyWithId(game, game.getId() == null ? nextId++ : game.getId());
            games.removeIf(existingGame -> existingGame.getId().equals(savedGame.getId()));
            games.add(savedGame);
            return savedGame;
        }

        private List<Game> findAllByOrderByRecommendationScoreDesc() {
            return games.stream()
                    .sorted(Comparator.comparingDouble(Game::getRecommendationScore).reversed())
                    .toList();
        }

        private Game copyWithId(Game game, Long id) {
            return Game.builder()
                    .id(id)
                    .title(game.getTitle())
                    .genre(game.getGenre())
                    .platform(game.getPlatform())
                    .playStyle(game.getPlayStyle())
                    .streamabilityScore(game.getStreamabilityScore())
                    .webcamFitScore(game.getWebcamFitScore())
                    .ttsFitScore(game.getTtsFitScore())
                    .sttFitScore(game.getSttFitScore())
                    .noveltyScore(game.getNoveltyScore())
                    .devFeasibilityScore(game.getDevFeasibilityScore())
                    .marketSignalScore(game.getMarketSignalScore())
                    .recommendationScore(game.getRecommendationScore())
                    .reason(game.getReason())
                    .createdAt(game.getCreatedAt())
                    .updatedAt(game.getUpdatedAt())
                    .build();
        }
    }
}
