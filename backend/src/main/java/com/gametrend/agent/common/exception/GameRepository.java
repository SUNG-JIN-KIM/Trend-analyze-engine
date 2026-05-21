package com.gametrend.agent.common.exception;

import com.gametrend.agent.domain.Game;
import java.util.List;

public interface GameRepository {
    void save(Game game);
    Game findById(String id);
    List<Game> findAll();
}