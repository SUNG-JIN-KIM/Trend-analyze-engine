package com.gametrend.agent.livetrend.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table("live_trend_refresh_status")
public class LiveTrendRefreshStatus {

    @Id
    private Long id;

    private boolean running;
    private LocalDateTime lastRefreshStartedAt;
    private LocalDateTime lastRefreshCompletedAt;
    private String lastRefreshStatus;
    private String lastRefreshMessage;
    private LocalDateTime nextRefreshEstimate;
    private LocalDateTime updatedAt;
}
