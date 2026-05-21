package com.gametrend.agent.game.repository;

import com.gametrend.agent.game.domain.Game;
import java.util.Optional;

public interface GameRepository {
    Optional<Game> findById(Long id);
}