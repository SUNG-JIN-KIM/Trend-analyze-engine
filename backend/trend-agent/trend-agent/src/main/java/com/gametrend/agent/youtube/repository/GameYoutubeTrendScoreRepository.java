package com.gametrend.agent.youtube.repository;

import com.gametrend.agent.youtube.entity.GameYoutubeTrendScore;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameYoutubeTrendScoreRepository extends CrudRepository<GameYoutubeTrendScore, Long> {

    @Query("""
            SELECT *
            FROM game_youtube_trend_scores
            WHERE LOWER(keyword) = LOWER(:keyword)
            LIMIT 1
            """)
    Optional<GameYoutubeTrendScore> findByKeywordIgnoreCase(@Param("keyword") String keyword);

    @Query("""
            SELECT *
            FROM game_youtube_trend_scores
            WHERE game_id = :gameId
            ORDER BY collected_at DESC
            LIMIT 1
            """)
    Optional<GameYoutubeTrendScore> findLatestByGameId(@Param("gameId") Long gameId);

    @Query("""
            SELECT *
            FROM game_youtube_trend_scores
            ORDER BY youtube_interest_score DESC, collected_at DESC
            LIMIT :limit
            """)
    List<GameYoutubeTrendScore> findTopScores(@Param("limit") int limit);
}
