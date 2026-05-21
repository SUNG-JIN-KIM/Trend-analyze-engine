package com.gametrend.agent.livetrend.repository;

import com.gametrend.agent.livetrend.entity.LiveTrendGame;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface LiveTrendGameRepository extends CrudRepository<LiveTrendGame, Long> {

    List<LiveTrendGame> findAllByOrderByTrendScoreDesc();

    List<LiveTrendGame> findBySourceOrderByTrendScoreDesc(String source);

    Optional<LiveTrendGame> findBySourceAndTitle(String source, String title);

    Optional<LiveTrendGame> findTopByOrderByUpdatedAtDesc();

    Optional<LiveTrendGame> findTopBySourceOrderByUpdatedAtDesc(String source);

    Optional<LiveTrendGame> findFirstBySourceAndDataOriginOrderByUpdatedAtDesc(String source, String dataOrigin);

    Optional<LiveTrendGame> findFirstBySourceAndSignalStatusOrderByUpdatedAtDesc(String source, String signalStatus);
}
