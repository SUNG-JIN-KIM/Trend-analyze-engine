package com.gametrend.agent.game.service;

import com.gametrend.agent.game.dto.GameCreateRequest;
import com.gametrend.agent.game.dto.GameRecommendationResponse;
import com.gametrend.agent.game.dto.GameResponse;
import com.gametrend.agent.game.entity.Game;
import com.gametrend.agent.game.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameServiceTest {

    private InMemoryGameRepository fakeRepository;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        fakeRepository = new InMemoryGameRepository();
        gameService = new GameService(fakeRepository.asRepository());
    }

    @Test
    void createGame_savesGameAndCalculatesRecommendationScore() {
        GameResponse response = gameService.createGame(newRequest(
                "Rhythm Talk",
                100,
                80,
                60,
                40,
                20,
                50,
                70
        ));

        assertNotNull(response.id());
        assertEquals("Rhythm Talk", response.title());
        assertEquals(62.0, response.recommendationScore());
        assertEquals("테스트용 추천 근거", response.reason());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());

        Game savedGame = fakeRepository.findById(response.id()).orElseThrow();
        assertEquals("Rhythm Talk", savedGame.getTitle());
        assertEquals(62.0, savedGame.getRecommendationScore());
        assertEquals(1, fakeRepository.count());
    }

    @Test
    void findGames_returnsRegisteredGames() {
        gameService.createGame(newRequest("Voice Party", 70, 70, 70, 70, 70, 70, 70));
        gameService.createGame(newRequest("Camera Quest", 80, 80, 80, 80, 80, 80, 80));

        List<GameResponse> responses = gameService.findGames();

        assertEquals(2, responses.size());
        List<String> titles = responses.stream()
                .map(GameResponse::title)
                .toList();
        assertTrue(titles.containsAll(List.of("Voice Party", "Camera Quest")));
    }

    @Test
    void recommendGames_returnsGamesOrderedByRecommendationScoreDesc() {
        gameService.createGame(newRequest("Low Fit Game", 20, 20, 20, 20, 20, 20, 20));
        gameService.createGame(newRequest("Mid Fit Game", 60, 60, 60, 60, 60, 60, 60));
        gameService.createGame(newRequest("High Fit Game", 90, 90, 90, 90, 90, 90, 90));

        List<GameRecommendationResponse> recommendations = gameService.recommendGames();

        assertEquals(3, recommendations.size());
        assertEquals(1, recommendations.get(0).rank());
        assertEquals("High Fit Game", recommendations.get(0).title());
        assertEquals(90.0, recommendations.get(0).recommendationScore());
        assertEquals(2, recommendations.get(1).rank());
        assertEquals("Mid Fit Game", recommendations.get(1).title());
        assertEquals(60.0, recommendations.get(1).recommendationScore());
        assertEquals(3, recommendations.get(2).rank());
        assertEquals("Low Fit Game", recommendations.get(2).title());
    }

    private GameCreateRequest newRequest(
            String title,
            int streamabilityScore,
            int webcamFitScore,
            int ttsFitScore,
            int sttFitScore,
            int noveltyScore,
            int devFeasibilityScore,
            int marketSignalScore
    ) {
        return new GameCreateRequest(
                title,
                "Party",
                "PC",
                "Co-op",
                streamabilityScore,
                webcamFitScore,
                ttsFitScore,
                sttFitScore,
                noveltyScore,
                devFeasibilityScore,
                marketSignalScore,
                "테스트용 추천 근거"
        );
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
                        case "deleteAll" -> {
                            games.clear();
                            yield null;
                        }
                        case "toString" -> "InMemoryGameRepository";
                        default -> throw new UnsupportedOperationException(
                                "테스트에서 사용하지 않는 메서드입니다: " + method.getName()
                        );
                    }
            );
        }

        Optional<Game> findById(Long id) {
            return games.stream()
                    .filter(game -> game.getId().equals(id))
                    .findFirst();
        }

        long count() {
            return games.size();
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
