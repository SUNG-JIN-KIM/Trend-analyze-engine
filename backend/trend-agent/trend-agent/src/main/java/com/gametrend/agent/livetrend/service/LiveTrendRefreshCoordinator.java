package com.gametrend.agent.livetrend.service;

import com.gametrend.agent.livetrend.dto.LiveTrendRefreshResponse;
import com.gametrend.agent.livetrend.dto.LiveTrendRefreshStatusResponse;
import com.gametrend.agent.livetrend.entity.LiveTrendPlatformStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveTrendRefreshCoordinator {

    private static final String SKIPPED = "SKIPPED";

    private final LiveTrendService liveTrendService;
    private final LiveTrendRefreshStatusManager statusManager;
    private final LiveTrendPlatformStatusService platformStatusService;

    public LiveTrendRefreshResponse refreshLiveTrends() {
        log.info("live-trends refresh 시작 요청을 받았습니다.");
        if (!statusManager.tryStart()) {
            log.info("live-trends refresh 시작 실패. 이전 refresh가 아직 실행 중입니다.");
            return new LiveTrendRefreshResponse(
                    0,
                    0,
                    0,
                    SKIPPED,
                    "이전 라이브 트렌드 갱신이 아직 실행 중이라 이번 실행은 건너뜁니다.",
                    LocalDateTime.now(),
                    List.of()
            );
        }

        try {
            platformStatusService.prepareForRefresh();
            LiveTrendRefreshResponse response = liveTrendService.refreshLiveTrends();
            log.info(
                    "live-trends service refresh 결과. status={}, message={}, refreshedCount={}, partialCount={}",
                    response.status(),
                    safeForLog(response.message()),
                    response.refreshedCount(),
                    response.partialCount()
            );
            List<LiveTrendPlatformStatus> platformStatuses = platformStatusService.updateAfterRefresh(
                    response.games(),
                    response.refreshedAt()
            );
            String finalStatus = platformStatusService.resolveOverallRefreshStatus(response.status(), platformStatuses);
            String finalMessage = platformStatusService.appendPlatformSummary(response.message(), platformStatuses);
            log.info(
                    "live-trends statusManager.complete 호출 직전. finalStatus={}, finalMessage={}",
                    finalStatus,
                    safeForLog(finalMessage)
            );
            statusManager.complete(finalStatus, finalMessage);
            LiveTrendRefreshStatusResponse currentStatus = statusManager.getStatus();
            log.info(
                    "live-trends statusManager.complete 호출 직후 현재 status. lastRefreshStatus={}, running={}, lastRefreshStartedAt={}, lastRefreshCompletedAt={}",
                    currentStatus.lastRefreshStatus(),
                    currentStatus.running(),
                    currentStatus.lastRefreshStartedAt(),
                    currentStatus.lastRefreshCompletedAt()
            );
            return withStatus(response, finalStatus, finalMessage);
        } catch (RuntimeException exception) {
            String message = "라이브 트렌드 갱신 중 예기치 못한 오류가 발생했습니다: " + exception.getMessage();
            log.warn(message, exception);
            statusManager.fail(message);
            return new LiveTrendRefreshResponse(
                    0,
                    0,
                    0,
                    "FAILED",
                    message,
                    LocalDateTime.now(),
                    List.of()
            );
        }
    }

    private LiveTrendRefreshResponse withStatus(
            LiveTrendRefreshResponse response,
            String status,
            String message
    ) {
        return new LiveTrendRefreshResponse(
                response.requestedCount(),
                response.refreshedCount(),
                response.partialCount(),
                status,
                message,
                response.refreshedAt(),
                response.games()
        );
    }

    private String safeForLog(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = value
                .replaceAll("(?i)(client[_-]?secret|authorization|access[_-]?token|refresh[_-]?token|api[_-]?key)(\\s*[=:]\\s*)[^,\\s&]+", "$1$2***")
                .replaceAll("(?i)bearer\\s+[a-z0-9._\\-]+", "Bearer ***");
        if (sanitized.length() <= 500) {
            return sanitized;
        }
        return sanitized.substring(0, 500) + "...";
    }
}
