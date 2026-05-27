package com.gametrend.agent.youtube.repository;

import com.gametrend.agent.youtube.entity.YoutubeCollectLog;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface YoutubeCollectLogRepository extends CrudRepository<YoutubeCollectLog, Long> {

    @Query("""
            SELECT *
            FROM youtube_collect_logs
            ORDER BY started_at DESC
            LIMIT :limit
            """)
    List<YoutubeCollectLog> findRecent(@Param("limit") int limit);

    @Query("""
            SELECT *
            FROM youtube_collect_logs
            WHERE started_at >= :start
            ORDER BY started_at DESC
            """)
    List<YoutubeCollectLog> findAllSince(@Param("start") LocalDateTime start);

    @Query("""
            SELECT *
            FROM youtube_collect_logs
            WHERE LOWER(keyword) = LOWER(:keyword)
              AND status = 'SUCCESS'
              AND completed_at >= :cutoff
            ORDER BY completed_at DESC
            LIMIT 1
            """)
    Optional<YoutubeCollectLog> findRecentSuccess(
            @Param("keyword") String keyword,
            @Param("cutoff") LocalDateTime cutoff
    );
}
