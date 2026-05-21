package com.gametrend.agent.livetrend.repository;

import com.gametrend.agent.livetrend.entity.LiveTrendRefreshStatus;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LiveTrendRefreshStatusRepository extends CrudRepository<LiveTrendRefreshStatus, Long> {

    @Modifying
    @Query("""
            UPDATE live_trend_refresh_status
               SET running = :running,
                   last_refresh_started_at = :lastRefreshStartedAt,
                   last_refresh_completed_at = :lastRefreshCompletedAt,
                   last_refresh_status = :lastRefreshStatus,
                   last_refresh_message = :lastRefreshMessage,
                   next_refresh_estimate = :nextRefreshEstimate,
                   updated_at = :updatedAt
             WHERE id = :id
            """)
    int updateStatusRow(
            @Param("id") Long id,
            @Param("running") boolean running,
            @Param("lastRefreshStartedAt") LocalDateTime lastRefreshStartedAt,
            @Param("lastRefreshCompletedAt") LocalDateTime lastRefreshCompletedAt,
            @Param("lastRefreshStatus") String lastRefreshStatus,
            @Param("lastRefreshMessage") String lastRefreshMessage,
            @Param("nextRefreshEstimate") LocalDateTime nextRefreshEstimate,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying
    @Query("""
            INSERT INTO live_trend_refresh_status (
                id,
                running,
                last_refresh_started_at,
                last_refresh_completed_at,
                last_refresh_status,
                last_refresh_message,
                next_refresh_estimate,
                updated_at
            ) VALUES (
                :id,
                :running,
                :lastRefreshStartedAt,
                :lastRefreshCompletedAt,
                :lastRefreshStatus,
                :lastRefreshMessage,
                :nextRefreshEstimate,
                :updatedAt
            )
            """)
    int insertStatusRow(
            @Param("id") Long id,
            @Param("running") boolean running,
            @Param("lastRefreshStartedAt") LocalDateTime lastRefreshStartedAt,
            @Param("lastRefreshCompletedAt") LocalDateTime lastRefreshCompletedAt,
            @Param("lastRefreshStatus") String lastRefreshStatus,
            @Param("lastRefreshMessage") String lastRefreshMessage,
            @Param("nextRefreshEstimate") LocalDateTime nextRefreshEstimate,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
