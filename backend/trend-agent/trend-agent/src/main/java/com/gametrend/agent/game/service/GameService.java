package com.gametrend.agent.game.service;

import com.gametrend.agent.game.dto.GameCreateRequest;
import com.gametrend.agent.game.dto.GameRecommendationResponse;
import com.gametrend.agent.game.dto.GameResponse;
import com.gametrend.agent.game.entity.Game;
import com.gametrend.agent.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;

    public GameResponse createGame(GameCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        double recommendationScore = calculateRecommendationScore(request);

        Game game = Game.builder()
                .title(request.title())
                .genre(request.genre())
                .platform(request.platform())
                .playStyle(request.playStyle())
                .streamabilityScore(request.streamabilityScore())
                .webcamFitScore(request.webcamFitScore())
                .ttsFitScore(request.ttsFitScore())
                .sttFitScore(request.sttFitScore())
                .noveltyScore(request.noveltyScore())
                .devFeasibilityScore(request.devFeasibilityScore())
                .marketSignalScore(request.marketSignalScore())
                .recommendationScore(recommendationScore)
                .reason(request.reason())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return GameResponse.from(gameRepository.save(game));
    }

    public List<GameResponse> findGames() {
        List<GameResponse> responses = new ArrayList<>();
        gameRepository.findAll().forEach(game -> responses.add(GameResponse.from(game)));
        return responses;
    }

    public List<GameRecommendationResponse> recommendGames() {
        List<Game> games = gameRepository.findAllByOrderByRecommendationScoreDesc();

        return IntStream.range(0, games.size())
                .mapToObj(index -> GameRecommendationResponse.of(index + 1, games.get(index)))
                .toList();
    }

    private double calculateRecommendationScore(GameCreateRequest request) {
        double score = request.streamabilityScore() * 0.20
                + request.webcamFitScore() * 0.15
                + request.ttsFitScore() * 0.15
                + request.sttFitScore() * 0.15
                + request.noveltyScore() * 0.15
                + request.devFeasibilityScore() * 0.10
                + request.marketSignalScore() * 0.10;

        return Math.round(score * 10.0) / 10.0;
    }
}
