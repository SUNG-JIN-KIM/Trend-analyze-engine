package com.gametrend.agent.youtube.repository;

import com.gametrend.agent.youtube.entity.YoutubeKeywordStat;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface YoutubeKeywordStatRepository extends CrudRepository<YoutubeKeywordStat, Long> {

    @Query("""
            SELECT *
            FROM youtube_keyword_stats
            WHERE LOWER(game_keyword) = LOWER(:gameKeyword)
              AND LOWER(stat_keyword) = LOWER(:statKeyword)
              AND sentiment = :sentiment
            LIMIT 1
            """)
    Optional<YoutubeKeywordStat> findStat(
            @Param("gameKeyword") String gameKeyword,
            @Param("statKeyword") String statKeyword,
            @Param("sentiment") String sentiment
    );

    @Query("""
            SELECT *
            FROM youtube_keyword_stats
            WHERE LOWER(game_keyword) = LOWER(:gameKeyword)
            ORDER BY mention_count DESC, updated_at DESC
            """)
    List<YoutubeKeywordStat> findByGameKeyword(@Param("gameKeyword") String gameKeyword);
}
