package com.gametrend.agent.livetrend.service;

import com.gametrend.agent.infrastructure.steam.SteamProperties;
import com.gametrend.agent.livetrend.config.LiveTrendProperties;
import com.gametrend.agent.livetrend.dto.LiveTrendGameResponse;
import com.gametrend.agent.livetrend.dto.LiveTrendPlatformStatusResponse;
import com.gametrend.agent.livetrend.entity.LiveTrendPlatformStatus;
import com.gametrend.agent.livetrend.repository.LiveTrendPlatformStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LiveTrendPlatformStatusService {

    public static final String TWITCH = "TWITCH";
    public static final String CHZZK = "CHZZK";
    public static final String SOOP = "SOOP";
    public static final String STEAM = "STEAM";

    private static final List<String> PLATFORMS = List.of(TWITCH, CHZZK, SOOP, STEAM);
    private static final String NEVER_RUN = "NEVER_RUN";
    private static final String MISSING_CREDENTIALS = "MISSING_CREDENTIALS";
    private static final String SUCCESS = "SUCCESS";
    private static final String PARTIAL = "PARTIAL";
    private static final String FAILED = "FAILED";
    private static final String SKIPPED = "SKIPPED";
    private static final String PUBLIC_OR_FALLBACK = "PUBLIC_OR_FALLBACK";
    private static final String NOT_USED_IN_THIS_REFRESH = "NOT_USED_IN_THIS_REFRESH";
    private static final String PARTIAL_SUCCESS = "PARTIAL_SUCCESS";
    private static final String DATA_ORIGIN_REAL = "REAL";
    private static final String SIGNAL_STATUS_COMPLETE = "COMPLETE";

    private final LiveTrendPlatformStatusRepository repository;
    private final LiveTrendProperties properties;
    private final SteamProperties steamProperties;

    public List<LiveTrendPlatformStatusResponse> getPlatformStatuses() {
        return PLATFORMS.stream()
                .map(platform -> LiveTrendPlatformStatusResponse.from(ensureStatus(platform)))
                .toList();
    }

    public void prepareForRefresh() {
        for (String platform : PLATFORMS) {
            LiveTrendPlatformStatus status = ensureStatus(platform);
            if (!isConfigured(platform) && !MISSING_CREDENTIALS.equals(status.getStatus())) {
                markMissingCredentials(platform);
            }
        }
    }

    public boolean isConfigured(String platform) {
        return switch (normalize(platform)) {
            case TWITCH -> hasClientCredentials(properties.getPlatforms().getTwitch());
            case CHZZK -> hasClientCredentials(properties.getPlatforms().getChzzk());
            case SOOP -> hasClientCredentials(properties.getPlatforms().getSoop());
            case STEAM -> properties.getPlatforms().getSteam().isUsePublicApi()
                    || hasText(properties.getPlatforms().getSteam().getApiKey())
                    || hasText(steamProperties.apiKey());
            default -> false;
        };
    }

    public LiveTrendPlatformStatus markMissingCredentials(String platform) {
        LocalDateTime now = LocalDateTime.now();
        LiveTrendPlatformStatus previous = findStatus(platform).orElse(defaultStatus(platform, now));
        return save(
                previous,
                false,
                MISSING_CREDENTIALS,
                buildConfigurationStatusMessage(platform, false),
                previous.getLastSuccessAt(),
                previous.getLastFailureAt(),
                now
        );
    }

    public LiveTrendPlatformStatus markSuccess(String platform, String message, LocalDateTime refreshedAt) {
        LiveTrendPlatformStatus previous = findStatus(platform).orElse(defaultStatus(platform, refreshedAt));
        return save(previous, true, SUCCESS, appendConfigurationSummary(platform, message), refreshedAt, previous.getLastFailureAt(), refreshedAt);
    }

    public LiveTrendPlatformStatus markPartial(String platform, String message, LocalDateTime refreshedAt) {
        LiveTrendPlatformStatus previous = findStatus(platform).orElse(defaultStatus(platform, refreshedAt));
        return save(previous, true, PARTIAL, appendConfigurationSummary(platform, message), refreshedAt, previous.getLastFailureAt(), refreshedAt);
    }

    public LiveTrendPlatformStatus markFailure(String platform, String message, LocalDateTime failedAt) {
        LiveTrendPlatformStatus previous = findStatus(platform).orElse(defaultStatus(platform, failedAt));
        return save(previous, isConfigured(platform), FAILED, appendConfigurationSummary(platform, message), previous.getLastSuccessAt(), failedAt, failedAt);
    }

    public LiveTrendPlatformStatus markPublicOrFallback(String platform, String message, LocalDateTime refreshedAt) {
        LiveTrendPlatformStatus previous = findStatus(platform).orElse(defaultStatus(platform, refreshedAt));
        return save(previous, true, PUBLIC_OR_FALLBACK, appendConfigurationSummary(platform, message), refreshedAt, previous.getLastFailureAt(), refreshedAt);
    }

    public LiveTrendPlatformStatus markNotUsedInThisRefresh(String platform, String message, LocalDateTime refreshedAt) {
        LiveTrendPlatformStatus previous = findStatus(platform).orElse(defaultStatus(platform, refreshedAt));
        return save(previous, true, NOT_USED_IN_THIS_REFRESH, appendConfigurationSummary(platform, message), previous.getLastSuccessAt(), previous.getLastFailureAt(), refreshedAt);
    }

    public void markAllSkipped(String message) {
        LocalDateTime now = LocalDateTime.now();
        for (String platform : PLATFORMS) {
            if (!isConfigured(platform)) {
                markMissingCredentials(platform);
                continue;
            }
            markSkipped(platform, message, now);
        }
    }

    public List<LiveTrendPlatformStatus> updateAfterRefresh(
            List<LiveTrendGameResponse> games,
            LocalDateTime refreshedAt
    ) {
        LocalDateTime statusTime = refreshedAt == null ? LocalDateTime.now() : refreshedAt;
        List<LiveTrendPlatformStatus> updatedStatuses = new ArrayList<>();
        updatedStatuses.add(updateLivePlatformAfterRefresh(TWITCH, games, statusTime));
        updatedStatuses.add(updateLivePlatformAfterRefresh(CHZZK, games, statusTime));
        updatedStatuses.add(updateLivePlatformAfterRefresh(SOOP, games, statusTime));
        updatedStatuses.add(updateSteamAfterRefresh(games, statusTime));
        return updatedStatuses;
    }

    public String resolveOverallRefreshStatus(String refreshStatus, List<LiveTrendPlatformStatus> platformStatuses) {
        if (FAILED.equals(refreshStatus) || SKIPPED.equals(refreshStatus)) {
            return refreshStatus;
        }
        boolean hasSuccess = platformStatuses.stream()
                .anyMatch(status -> SUCCESS.equals(status.getStatus()));
        boolean hasNonSuccess = platformStatuses.stream()
                .anyMatch(status -> !SUCCESS.equals(status.getStatus()));
        if (hasSuccess && hasNonSuccess) {
            return PARTIAL_SUCCESS;
        }
        if (hasNonSuccess && "SUCCESS".equals(refreshStatus)) {
            return PARTIAL_SUCCESS;
        }
        return refreshStatus;
    }

    public String appendPlatformSummary(String message, List<LiveTrendPlatformStatus> platformStatuses) {
        List<String> notableStatuses = platformStatuses.stream()
                .filter(status -> !SUCCESS.equals(status.getStatus()))
                .map(status -> "%s=%s".formatted(status.getPlatform(), status.getStatus()))
                .toList();
        if (notableStatuses.isEmpty()) {
            return message;
        }
        return "%s 플랫폼 상태: %s.".formatted(message, String.join(", ", notableStatuses));
    }

    private LiveTrendPlatformStatus markSkipped(String platform, String message, LocalDateTime skippedAt) {
        LiveTrendPlatformStatus previous = findStatus(platform).orElse(defaultStatus(platform, skippedAt));
        return save(previous, true, SKIPPED, appendConfigurationSummary(platform, message), previous.getLastSuccessAt(), previous.getLastFailureAt(), skippedAt);
    }

    private LiveTrendPlatformStatus updateLivePlatformAfterRefresh(
            String platform,
            List<LiveTrendGameResponse> games,
            LocalDateTime refreshedAt
    ) {
        String normalizedPlatform = normalize(platform);
        LiveTrendPlatformStatus previous = ensureStatus(normalizedPlatform);
        if (failedDuringRefresh(previous, refreshedAt)) {
            return previous;
        }

        List<LiveTrendGameResponse> platformGames = games.stream()
                .filter(game -> normalizedPlatform.equals(normalize(game.source())))
                .toList();
        LocalDateTime platformRefreshedAt = latestUpdatedAt(platformGames).orElse(refreshedAt);

        if (!platformGames.isEmpty() && hasCompleteOrRealSignal(platformGames)) {
            return markSuccess(
                    normalizedPlatform,
                    "%s refresh 결과 실제 라이브 트렌드 데이터가 확인되었습니다.".formatted(normalizedPlatform),
                    platformRefreshedAt
            );
        }
        if (!platformGames.isEmpty()) {
            if (SOOP.equals(normalizedPlatform) && !isConfigured(normalizedPlatform)) {
                return markMissingCredentials(normalizedPlatform);
            }
            return markPartial(
                    normalizedPlatform,
                    "%s refresh 결과 fallback 또는 partial 라이브 트렌드 데이터만 확인되었습니다.".formatted(normalizedPlatform),
                    platformRefreshedAt
            );
        }

        if (!isConfigured(normalizedPlatform)) {
            return markMissingCredentials(normalizedPlatform);
        }

        return markSkipped(
                normalizedPlatform,
                "%s는 이번 라이브 트렌드 refresh 대상 seed가 없어 수집을 건너뛰었습니다.".formatted(normalizedPlatform),
                refreshedAt
        );
    }

    private LiveTrendPlatformStatus updateSteamAfterRefresh(
            List<LiveTrendGameResponse> games,
            LocalDateTime refreshedAt
    ) {
        if (!isConfigured(STEAM)) {
            return markMissingCredentials(STEAM);
        }

        List<LiveTrendGameResponse> steamGames = games.stream()
                .filter(game -> STEAM.equals(normalize(game.source())))
                .toList();
        LocalDateTime steamRefreshedAt = latestUpdatedAt(steamGames).orElse(refreshedAt);
        if (steamGames.isEmpty()) {
            if (properties.getPlatforms().getSteam().isUsePublicApi()) {
                return markPublicOrFallback(
                        STEAM,
                        "Steam 공개 API 사용 설정이 켜져 있지만 현재 live-trends refresh는 Steam 데이터를 직접 수집하지 않아 공개 API 또는 fallback 상태로 표시합니다.",
                        refreshedAt
                );
            }
            return markNotUsedInThisRefresh(
                    STEAM,
                    "현재 live-trends refresh는 Steam 리뷰/마켓 신호를 직접 조회하지 않고 라이브 플랫폼 seed 점수만 갱신합니다.",
                    refreshedAt
            );
        }
        if (hasCompleteOrRealSignal(steamGames)) {
            return markSuccess(
                    STEAM,
                    "Steam refresh 결과 실제 리뷰/마켓 신호가 확인되었습니다.",
                    steamRefreshedAt
            );
        }
        return markPublicOrFallback(
                STEAM,
                "Steam API Key 없이 공개 API 또는 fallback 기반 Steam 신호를 사용했습니다.",
                steamRefreshedAt
        );
    }

    private Optional<LocalDateTime> latestUpdatedAt(List<LiveTrendGameResponse> games) {
        return games.stream()
                .map(LiveTrendGameResponse::updatedAt)
                .filter(updatedAt -> updatedAt != null)
                .max(LocalDateTime::compareTo);
    }

    private boolean failedDuringRefresh(LiveTrendPlatformStatus status, LocalDateTime refreshedAt) {
        return FAILED.equals(status.getStatus())
                && status.getLastFailureAt() != null
                && !status.getLastFailureAt().isBefore(refreshedAt);
    }

    private boolean hasCompleteOrRealSignal(List<LiveTrendGameResponse> games) {
        return games.stream()
                .anyMatch(game -> DATA_ORIGIN_REAL.equalsIgnoreCase(nullToEmpty(game.dataOrigin()))
                        || SIGNAL_STATUS_COMPLETE.equalsIgnoreCase(nullToEmpty(game.signalStatus())));
    }

    private LiveTrendPlatformStatus ensureStatus(String platform) {
        String normalizedPlatform = normalize(platform);
        return findStatus(normalizedPlatform)
                .map(this::syncConfiguration)
                .orElseGet(() -> {
            LocalDateTime now = LocalDateTime.now();
            LiveTrendPlatformStatus status = defaultStatus(normalizedPlatform, now);
            return repository.save(status);
        });
    }

    private LiveTrendPlatformStatus syncConfiguration(LiveTrendPlatformStatus status) {
        boolean configured = isConfigured(status.getPlatform());
        String configurationMessage = buildConfigurationStatusMessage(status.getPlatform(), configured);
        if (status.isConfigured() == configured) {
            if ((NEVER_RUN.equals(status.getStatus()) || MISSING_CREDENTIALS.equals(status.getStatus()))
                    && !configurationMessage.equals(status.getMessage())) {
                return repository.save(LiveTrendPlatformStatus.builder()
                        .id(status.getId())
                        .platform(status.getPlatform())
                        .configured(configured)
                        .status(status.getStatus())
                        .message(configurationMessage)
                        .lastSuccessAt(status.getLastSuccessAt())
                        .lastFailureAt(status.getLastFailureAt())
                        .updatedAt(LocalDateTime.now())
                        .build());
            }
            return status;
        }

        LocalDateTime now = LocalDateTime.now();
        if (!isInitialStatus(status.getStatus())) {
            return repository.save(LiveTrendPlatformStatus.builder()
                    .id(status.getId())
                    .platform(status.getPlatform())
                    .configured(configured)
                    .status(status.getStatus())
                    .message(status.getMessage())
                    .lastSuccessAt(status.getLastSuccessAt())
                    .lastFailureAt(status.getLastFailureAt())
                    .updatedAt(now)
                    .build());
        }

        String nextStatus = configured ? NEVER_RUN : MISSING_CREDENTIALS;

        LiveTrendPlatformStatus syncedStatus = LiveTrendPlatformStatus.builder()
                .id(status.getId())
                .platform(status.getPlatform())
                .configured(configured)
                .status(nextStatus)
                .message(configurationMessage)
                .lastSuccessAt(status.getLastSuccessAt())
                .lastFailureAt(status.getLastFailureAt())
                .updatedAt(now)
                .build();
        return repository.save(syncedStatus);
    }

    private boolean isInitialStatus(String status) {
        return NEVER_RUN.equals(status) || MISSING_CREDENTIALS.equals(status);
    }

    private Optional<LiveTrendPlatformStatus> findStatus(String platform) {
        return repository.findByPlatform(normalize(platform));
    }

    private LiveTrendPlatformStatus defaultStatus(String platform, LocalDateTime now) {
        String normalizedPlatform = normalize(platform);
        boolean configured = isConfigured(normalizedPlatform);
        return LiveTrendPlatformStatus.builder()
                .id(platformId(normalizedPlatform))
                .newEntity(true)
                .platform(normalizedPlatform)
                .configured(configured)
                .status(configured ? NEVER_RUN : MISSING_CREDENTIALS)
                .message(buildConfigurationStatusMessage(normalizedPlatform, configured))
                .updatedAt(now)
                .build();
    }

    private LiveTrendPlatformStatus save(
            LiveTrendPlatformStatus previous,
            boolean configured,
            String status,
            String message,
            LocalDateTime lastSuccessAt,
            LocalDateTime lastFailureAt,
            LocalDateTime updatedAt
    ) {
        return repository.save(LiveTrendPlatformStatus.builder()
                .id(previous.getId() == null ? platformId(previous.getPlatform()) : previous.getId())
                .newEntity(previous.isNew())
                .platform(previous.getPlatform())
                .configured(configured)
                .status(status)
                .message(message)
                .lastSuccessAt(lastSuccessAt)
                .lastFailureAt(lastFailureAt)
                .updatedAt(updatedAt)
                .build());
    }

    private boolean hasClientCredentials(LiveTrendProperties.PlatformCredential credential) {
        return hasText(credential.getClientId()) && hasText(credential.getClientSecret());
    }

    private String buildConfigurationStatusMessage(String platform, boolean configured) {
        String normalizedPlatform = normalize(platform);
        if (configured) {
            return "%s 인증 설정이 확인되었습니다. 아직 수집은 실행되지 않았습니다. 설정 확인: %s"
                    .formatted(normalizedPlatform, configurationSummary(normalizedPlatform));
        }
        return "%s 인증 정보가 충분하지 않습니다. 설정 확인: %s"
                .formatted(normalizedPlatform, configurationSummary(normalizedPlatform));
    }

    private String appendConfigurationSummary(String platform, String message) {
        return "%s 설정 확인: %s".formatted(message, configurationSummary(platform));
    }

    private String configurationSummary(String platform) {
        return switch (normalize(platform)) {
            case TWITCH -> credentialSummary(properties.getPlatforms().getTwitch());
            case CHZZK -> credentialSummary(properties.getPlatforms().getChzzk());
            case SOOP -> credentialSummary(properties.getPlatforms().getSoop());
            case STEAM -> "usePublicApi=%s, apiKey=%s, platformApiKey=%s"
                    .formatted(properties.getPlatforms().getSteam().isUsePublicApi(),
                            hasText(steamProperties.apiKey()),
                            hasText(properties.getPlatforms().getSteam().getApiKey()));
            default -> "unknownPlatform=true";
        };
    }

    private String credentialSummary(LiveTrendProperties.PlatformCredential credential) {
        return "clientId=%s, clientSecret=%s, accessToken=%s, refreshToken=%s, apiKey=%s, apiBaseUrl=%s, redirectUri=%s"
                .formatted(hasText(credential.getClientId()),
                        hasText(credential.getClientSecret()),
                        hasText(credential.getAccessToken()),
                        hasText(credential.getRefreshToken()),
                        hasText(credential.getApiKey()),
                        hasText(credential.getApiBaseUrl()),
                        hasText(credential.getRedirectUri()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Long platformId(String platform) {
        return switch (normalize(platform)) {
            case TWITCH -> 1L;
            case CHZZK -> 2L;
            case SOOP -> 3L;
            case STEAM -> 4L;
            default -> throw new IllegalArgumentException("지원하지 않는 라이브 트렌드 플랫폼입니다: " + platform);
        };
    }

    private String normalize(String platform) {
        if (platform == null) {
            return "";
        }
        return platform.strip().toUpperCase();
    }
}
