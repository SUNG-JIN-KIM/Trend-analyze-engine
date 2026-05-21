package com.gametrend.agent.reinterpretation.repository;

import com.gametrend.agent.reinterpretation.entity.LegacyGame;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface LegacyGameRepository extends CrudRepository<LegacyGame, Long> {

    List<LegacyGame> findAllByOrderByTitleAsc();

    Optional<LegacyGame> findBySourceAndSourceGameId(String source, String sourceGameId);

}
