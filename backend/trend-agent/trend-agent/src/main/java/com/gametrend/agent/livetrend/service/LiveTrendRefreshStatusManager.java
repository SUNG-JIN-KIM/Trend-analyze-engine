package com.gametrend.agent.livetrend.service;

import com.gametrend.agent.livetrend.config.LiveTrendProperties;
import com.gametrend.agent.livetrend.dto.LiveTrendGameResponse;
import com.gametrend.agent.livetrend.dto.LiveTrendRefreshStatusResponse;
import com.gametrend.agent.livetrend.entity.LiveTrendGame;
import com.gametrend.agent.livetrend.entity.LiveTrendRefreshStatus;
import com.gametrend.agent.livetrend.repository.LiveTrendGameRepository;
import com.gametrend.agent.livetrend.repository.LiveTrendRefreshStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveTrendRefreshStatusManager {

    private static final Long STATUS_ID = 1L;
    private static final String NEVER_RUN = "NEVER_RUN";
    private static final String RUNNING = "RUNNING";
    private static final String SKIPPED = "SKIPPED";
    private static final String SUCCESS = "SUCCESS";
    private static final String PARTIAL_SUCCESS = "PARTIAL_SUCCESS";
    private static final String SIGNAL_STATUS_PARTIAL = "PARTIAL";
    private static final String SIGNAL_STATUS_COMPLETE = "COMPLETE";
    private static final String DATA_ORIGIN_REAL = "REAL";
    private static final String DATA_ORIGIN_FALLBACK = "FALLBACK";
    private static final String DATA_ORIGIN_PARTIAL = "PARTIAL";
    private static final String RECOVERED_FROM_SNAPSHOT_MESSAGE =
            "저장된 최신 라이브 트렌드 데이터를 기준으로 상태를 보정했습니다.";
    private static final List<String> SNAPSHOT_SOURCES = List.of(
            LiveTrendPlatformStatusService.TWITCH,
            LiveTrendPlatformStatusService.CHZZK,
            LiveTrendPlatformStatusService.SOOP,
            LiveTrendPlatformStatusService.STEAM
    );

    private final LiveTrendRefreshStatusRepository repository;
    private final LiveTrendProperties properties;
    private final LiveTrendPlatformStatusService platformStatusService;
    private final LiveTrendGameRepository liveTrendGameRepository;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public boolean tryStart() {
        if (!running.compareAndSet(false, true)) {
            markAlreadyRunning("이전 라이브 트렌드 갱신이 아직 실행 중이라 이번 실행은 건너뜁니다.");
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LiveTrendRefreshStatus previous = findStatus().orElse(defaultStatus(now));
        LiveTrendRefreshStatus nextStatus = LiveTrendRefreshStatus.builder()
                .id(STATUS_ID)
                .running(true)
                .lastRefreshStartedAt(now)
                .lastRefreshCompletedAt(previous.getLastRefreshCompletedAt())
                .lastRefreshStatus(RUNNING)
                .lastRefreshMessage("라이브 트렌드 갱신 중입니다.")
                .nextRefreshEstimate(estimateNextRefresh(now))
                .updatedAt(now)
                .build();
        saveStatus(nextStatus);

        return true;
    }

    public void complete(String status, String message) {
        LocalDateTime now = LocalDateTime.now();
        LiveTrendRefreshStatus previous = findStatus().orElse(defaultStatus(now));
        running.set(false);
        LiveTrendRefreshStatus nextStatus = LiveTrendRefreshStatus.builder()
                .id(STATUS_ID)
                .running(false)
                .lastRefreshStartedAt(previous.getLastRefreshStartedAt() == null ? now : previous.getLastRefreshStartedAt())
                .lastRefreshCompletedAt(now)
                .lastRefreshStatus(status)
                .lastRefreshMessage(message)
                .nextRefreshEstimate(estimateNextRefresh(now))
                .updatedAt(now)
                .build();

        log.info(
                "live-trends refresh status 저장 시도. lastRefreshStatus={}, lastRefreshStartedAt={}, lastRefreshCompletedAt={}",
                nextStatus.getLastRefreshStatus(),
                nextStatus.getLastRefreshStartedAt(),
                nextStatus.getLastRefreshCompletedAt()
        );
        LiveTrendRefreshStatus savedStatus = saveStatus(nextStatus);
        log.info("live-trends refresh status 저장 완료. savedStatus={}", summarizeStatus(savedStatus));
    }

    public void fail(String message) {
        complete("FAILED", message);
    }

    public LiveTrendRefreshStatusResponse getStatus() {
        LocalDateTime now = LocalDateTime.now();
        removeUnexpectedStatusRows();
        Optional<LiveTrendRefreshStatus> savedStatus = findStatus();
        boolean useDefaultStatus = savedStatus.isEmpty();
        LiveTrendRefreshStatus baseStatus = savedStatus.orElseGet(() -> defaultStatus(now));
        long liveTrendGameRowCount = liveTrendGameRepository.count();
        long refreshStatusRowCount = repository.count();

        log.info(
                "live-trends status 조회 시작. findStatusPresent={}, useDefaultStatus={}, liveTrendGameRows={}, refreshStatusRows={}, beforeStatus={}",
                savedStatus.isPresent(),
                useDefaultStatus,
                liveTrendGameRowCount,
                refreshStatusRowCount,
                summarizeStatus(baseStatus)
        );

        LiveTrendRefreshStatus status = reconcileStatusWithSavedGames(baseStatus, now);
        log.info("live-trends status 조회 보정 완료. afterStatus={}", summarizeStatus(status));

        return LiveTrendRefreshStatusResponse.from(
                status,
                properties,
                running.get(),
                platformStatusService.getPlatformStatuses()
        );
    }

    private Optional<LiveTrendRefreshStatus> findStatus() {
        return repository.findById(STATUS_ID);
    }

    private void removeUnexpectedStatusRows() {
        List<Long> removedIds = new ArrayList<>();
        for (LiveTrendRefreshStatus status : repository.findAll()) {
            if (status.getId() != null && !STATUS_ID.equals(status.getId())) {
                removedIds.add(status.getId());
                repository.deleteById(status.getId());
            }
        }
        if (!removedIds.isEmpty()) {
            log.warn("live-trends refresh status 테이블에서 STATUS_ID={} 외 row를 정리했습니다. removedIds={}", STATUS_ID, removedIds);
        }
    }

    private LiveTrendRefreshStatus reconcileStatusWithSavedGames(
            LiveTrendRefreshStatus status,
            LocalDateTime now
    ) {
        long liveTrendGameRowCount = liveTrendGameRepository.count();
        Optional<LiveTrendGame> latestSavedGame = liveTrendGameRepository.findTopByOrderByUpdatedAtDesc();
        List<LiveTrendGame> savedGames = List.of();
        if (latestSavedGame.isEmpty() && liveTrendGameRowCount > 0) {
            savedGames = liveTrendGameRepository.findAllByOrderByTrendScoreDesc();
            latestSavedGame = savedGames.stream()
                    .max((left, right) -> timestampOrNow(left, now).compareTo(timestampOrNow(right, now)));
            log.warn(
                    "live-trends status DB 최신 조회가 비어 있습니다. count={}이므로 전체 snapshot fallback을 사용합니다. fallbackFound={}",
                    liveTrendGameRowCount,
                    latestSavedGame.isPresent()
            );
        }

        log.info(
                "live-trends status DB 보정 시작. latestSavedGamePresent={}, liveTrendGameRows={}",
                latestSavedGame.isPresent(),
                liveTrendGameRowCount
        );

        if (latestSavedGame.isEmpty()) {
            return status;
        }

        LocalDateTime refreshedAt = timestampOrNow(latestSavedGame.orElseThrow(), now);
        log.info("live-trends status 최신 snapshot. game={}", summarizeGame(latestSavedGame.orElseThrow()));

        List<LiveTrendGame> statusSnapshotGames = findLatestStatusSnapshotGames();
        if (statusSnapshotGames.isEmpty() && liveTrendGameRowCount > 0) {
            if (savedGames.isEmpty()) {
                savedGames = liveTrendGameRepository.findAllByOrderByTrendScoreDesc();
            }
            statusSnapshotGames = savedGames;
            log.warn(
                    "live-trends source별 최신 snapshot 조회가 비어 있습니다. 전체 snapshot {}개로 플랫폼 status를 보정합니다.",
                    statusSnapshotGames.size()
            );
        }
        log.info("live-trends source별 status snapshot 개수={}", statusSnapshotGames.size());

        List<LiveTrendGameResponse> gameResponses = statusSnapshotGames.stream()
                .map(LiveTrendGameResponse::from)
                .toList();
        platformStatusService.updateAfterRefresh(gameResponses, refreshedAt);

        if (!NEVER_RUN.equals(status.getLastRefreshStatus())) {
            return status;
        }

        if (savedGames.isEmpty()) {
            savedGames = liveTrendGameRepository.findAllByOrderByTrendScoreDesc();
        }
        String recoveredStatus = hasPartialGame(savedGames) ? PARTIAL_SUCCESS : SUCCESS;
        log.info(
                "live-trends status NEVER_RUN 보정 수행. savedGames={}, recoveredStatus={}, repositorySave=true",
                savedGames.size(),
                recoveredStatus
        );

        LiveTrendRefreshStatus recovered = LiveTrendRefreshStatus.builder()
                .id(STATUS_ID)
                .running(false)
                .lastRefreshStartedAt(refreshedAt)
                .lastRefreshCompletedAt(refreshedAt)
                .lastRefreshStatus(recoveredStatus)
                .lastRefreshMessage(RECOVERED_FROM_SNAPSHOT_MESSAGE)
                .nextRefreshEstimate(estimateNextRefresh(now))
                .updatedAt(now)
                .build();
        LiveTrendRefreshStatus savedRecoveredStatus = saveStatus(recovered);
        log.info("live-trends status NEVER_RUN 보정 저장 완료. savedStatus={}", summarizeStatus(savedRecoveredStatus));
        return savedRecoveredStatus;
    }

    private List<LiveTrendGame> findLatestStatusSnapshotGames() {
        return SNAPSHOT_SOURCES.stream()
                .map(this::findLatestStatusSnapshotGame)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<LiveTrendGame> findLatestStatusSnapshotGame(String source) {
        Optional<LiveTrendGame> realGame =
                liveTrendGameRepository.findFirstBySourceAndDataOriginOrderByUpdatedAtDesc(source, DATA_ORIGIN_REAL);
        if (realGame.isPresent()) {
            return realGame;
        }

        Optional<LiveTrendGame> completeGame =
                liveTrendGameRepository.findFirstBySourceAndSignalStatusOrderByUpdatedAtDesc(source, SIGNAL_STATUS_COMPLETE);
        if (completeGame.isPresent()) {
            return completeGame;
        }

        return liveTrendGameRepository.findTopBySourceOrderByUpdatedAtDesc(source);
    }

    private LocalDateTime timestampOrNow(LiveTrendGame game, LocalDateTime now) {
        return game.getUpdatedAt() == null ? now : game.getUpdatedAt();
    }

    private boolean hasPartialGame(List<LiveTrendGame> games) {
        return games.stream()
                .anyMatch(game -> SIGNAL_STATUS_PARTIAL.equalsIgnoreCase(nullToEmpty(game.getSignalStatus()))
                        || DATA_ORIGIN_FALLBACK.equalsIgnoreCase(nullToEmpty(game.getDataOrigin()))
                        || DATA_ORIGIN_PARTIAL.equalsIgnoreCase(nullToEmpty(game.getDataOrigin())));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void markAlreadyRunning(String message) {
        LocalDateTime now = LocalDateTime.now();
        LiveTrendRefreshStatus previous = findStatus().orElse(defaultStatus(now));
        saveStatus(LiveTrendRefreshStatus.builder()
                .id(STATUS_ID)
                .running(true)
                .lastRefreshStartedAt(previous.getLastRefreshStartedAt())
                .lastRefreshCompletedAt(previous.getLastRefreshCompletedAt())
                .lastRefreshStatus(RUNNING)
                .lastRefreshMessage(message)
                .nextRefreshEstimate(estimateNextRefresh(now))
                .updatedAt(now)
                .build());
    }

    private LiveTrendRefreshStatus defaultStatus(LocalDateTime now) {
        return LiveTrendRefreshStatus.builder()
                .id(STATUS_ID)
                .running(false)
                .lastRefreshStatus(NEVER_RUN)
                .lastRefreshMessage("아직 라이브 트렌드 자동 갱신이 실행되지 않았습니다.")
                .nextRefreshEstimate(properties.isSchedulerEnabled() ? estimateNextRefresh(now) : null)
                .updatedAt(now)
                .build();
    }

    private LocalDateTime estimateNextRefresh(LocalDateTime baseTime) {
        if (!properties.isSchedulerEnabled()) {
            return null;
        }
        return baseTime.plusNanos(properties.getRefreshIntervalMs() * 1_000_000L);
    }

    private LiveTrendRefreshStatus saveStatus(LiveTrendRefreshStatus status) {
        removeUnexpectedStatusRows();
        int updatedRows = updateStatus(status);
        if (updatedRows == 0) {
            try {
                insertStatus(status);
            } catch (DuplicateKeyException exception) {
                log.warn("live-trends refresh status id={} insert 충돌이 발생해 update로 재시도합니다.", STATUS_ID);
                updateStatus(status);
            }
        }
        return repository.findById(STATUS_ID).orElse(status);
    }

    private int updateStatus(LiveTrendRefreshStatus status) {
        return repository.updateStatusRow(
                STATUS_ID,
                status.isRunning(),
                status.getLastRefreshStartedAt(),
                status.getLastRefreshCompletedAt(),
                status.getLastRefreshStatus(),
                status.getLastRefreshMessage(),
                status.getNextRefreshEstimate(),
                status.getUpdatedAt()
        );
    }

    private void insertStatus(LiveTrendRefreshStatus status) {
        repository.insertStatusRow(
                STATUS_ID,
                status.isRunning(),
                status.getLastRefreshStartedAt(),
                status.getLastRefreshCompletedAt(),
                status.getLastRefreshStatus(),
                status.getLastRefreshMessage(),
                status.getNextRefreshEstimate(),
                status.getUpdatedAt()
        );
    }

    private String summarizeStatus(LiveTrendRefreshStatus status) {
        if (status == null) {
            return "null";
        }
        return "id=%s, running=%s, lastRefreshStatus=%s, lastRefreshStartedAt=%s, lastRefreshCompletedAt=%s, updatedAt=%s"
                .formatted(
                        status.getId(),
                        status.isRunning(),
                        status.getLastRefreshStatus(),
                        status.getLastRefreshStartedAt(),
                        status.getLastRefreshCompletedAt(),
                        status.getUpdatedAt()
                );
    }

    private String summarizeGame(LiveTrendGame game) {
        if (game == null) {
            return "null";
        }
        return "id=%s, source=%s, title=%s, dataOrigin=%s, signalStatus=%s, updatedAt=%s"
                .formatted(
                        game.getId(),
                        game.getSource(),
                        game.getTitle(),
                        game.getDataOrigin(),
                        game.getSignalStatus(),
                        game.getUpdatedAt()
                );
    }
}
