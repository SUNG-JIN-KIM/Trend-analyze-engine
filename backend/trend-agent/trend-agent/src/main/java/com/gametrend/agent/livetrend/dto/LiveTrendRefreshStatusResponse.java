package com.gametrend.agent.livetrend.dto;

import com.gametrend.agent.livetrend.config.LiveTrendProperties;
import com.gametrend.agent.livetrend.entity.LiveTrendRefreshStatus;

import java.time.LocalDateTime;
import java.util.List;

public record LiveTrendRefreshStatusResponse(
        boolean schedulerEnabled,
        long refreshIntervalMs,
        boolean refreshOnStartup,
        boolean running,
        LocalDateTime lastRefreshStartedAt,
        LocalDateTime lastRefreshCompletedAt,
        String lastRefreshStatus,
        String lastRefreshMessage,
        LocalDateTime nextRefreshEstimate,
        List<LiveTrendPlatformStatusResponse> platformStatuses
) {

    public static LiveTrendRefreshStatusResponse from(
            LiveTrendRefreshStatus status,
            LiveTrendProperties properties,
            boolean running,
            List<LiveTrendPlatformStatusResponse> platformStatuses
    ) {
        return new LiveTrendRefreshStatusResponse(
                properties.isSchedulerEnabled(),
                properties.getRefreshIntervalMs(),
                properties.isRefreshOnStartup(),
                running,
                status.getLastRefreshStartedAt(),
                status.getLastRefreshCompletedAt(),
                status.getLastRefreshStatus(),
                status.getLastRefreshMessage(),
                status.getNextRefreshEstimate(),
                platformStatuses
        );
    }
}
