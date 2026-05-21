package com.gametrend.agent.livetrend.scheduler;

import com.gametrend.agent.livetrend.config.LiveTrendProperties;
import com.gametrend.agent.livetrend.service.LiveTrendRefreshCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveTrendRefreshScheduler {

    private final LiveTrendProperties properties;
    private final LiveTrendRefreshCoordinator refreshCoordinator;

    @Scheduled(fixedDelayString = "${live-trends.refresh-interval-ms:1800000}")
    public void refreshOnSchedule() {
        if (!properties.isSchedulerEnabled()) {
            return;
        }

        try {
            refreshCoordinator.refreshLiveTrends();
        } catch (RuntimeException exception) {
            log.warn("라이브 트렌드 스케줄 갱신 실패. scheduler는 계속 유지됩니다. cause={}", exception.toString(), exception);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        if (!properties.isSchedulerEnabled() || !properties.isRefreshOnStartup()) {
            return;
        }

        try {
            refreshCoordinator.refreshLiveTrends();
        } catch (RuntimeException exception) {
            log.warn("앱 시작 시 라이브 트렌드 갱신 실패. cause={}", exception.toString(), exception);
        }
    }
}
