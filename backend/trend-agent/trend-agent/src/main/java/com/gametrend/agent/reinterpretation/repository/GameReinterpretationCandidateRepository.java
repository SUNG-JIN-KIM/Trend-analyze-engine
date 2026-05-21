package com.gametrend.agent.reinterpretation.repository;

import com.gametrend.agent.reinterpretation.entity.GameReinterpretationCandidate;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface GameReinterpretationCandidateRepository extends CrudRepository<GameReinterpretationCandidate, Long> {

    List<GameReinterpretationCandidate> findAllByOrderByReinterpretationScoreDesc();

    Optional<GameReinterpretationCandidate> findByLegacyGameId(Long legacyGameId);
}
