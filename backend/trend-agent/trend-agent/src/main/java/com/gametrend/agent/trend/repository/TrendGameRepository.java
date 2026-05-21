package com.gametrend.agent.trend.repository;

import com.gametrend.agent.trend.entity.TrendGame;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface TrendGameRepository extends CrudRepository<TrendGame, Long> {

    List<TrendGame> findAllByOrderByTrendScoreDesc();

    Optional<TrendGame> findByTitle(String title);
}
