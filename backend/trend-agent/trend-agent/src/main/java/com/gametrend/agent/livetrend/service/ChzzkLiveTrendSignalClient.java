package com.gametrend.agent.livetrend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.agent.livetrend.config.LiveTrendProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChzzkLiveTrendSignalClient implements LiveTrendSignalClient {

    private static final String SOURCE = LiveTrendPlatformStatusService.CHZZK;
    private static final String DEFAULT_API_BASE_URL = "https://openapi.chzzk.naver.com";
    private static final int LIVE_LIST_SIZE = 20;
    private static final int BODY_LOG_LIMIT = 500;

    private final LiveTrendProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public String source() {
        return SOURCE;
    }

    @Override
    public List<LiveGameSignal> fetchSignals() {
        LiveTrendProperties.PlatformCredential credential = properties.getPlatforms().getChzzk();
        if (!hasClientCredentials(credential)) {
            return List.of();
        }

        try {
            String responseBody = RestClient.builder()
                    .baseUrl(resolveApiBaseUrl(credential))
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/open/v1/lives")
                            .queryParam("size", LIVE_LIST_SIZE)
                            .build())
                    .header("Client-Id", credential.getClientId())
                    .header("Client-Secret", credential.getClientSecret())
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(String.class);

            JsonNode response = parseApiResponse(responseBody);
            return toSignals(response);
        } catch (RestClientException exception) {
            throw new IllegalStateException("CHZZK API 호출에 실패했습니다: " + exception.getClass().getSimpleName(), exception);
        }
    }

    private JsonNode parseApiResponse(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            log.warn("CHZZK API 응답 JSON 파싱 실패. bodyPrefix={}, cause={}",
                    responsePrefix(responseBody),
                    exception.getClass().getSimpleName());
            throw new IllegalStateException("CHZZK API 응답 JSON 파싱에 실패했습니다.", exception);
        }
    }

    private String responsePrefix(String responseBody) {
        if (responseBody == null) {
            return "";
        }
        String normalized = responseBody.replaceAll("\\s+", " ").strip();
        if (normalized.length() <= BODY_LOG_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, BODY_LOG_LIMIT);
    }

    List<LiveGameSignal> toSignals(JsonNode response) {
        JsonNode data = liveData(response);
        if (data == null || !data.isArray()) {
            return List.of();
        }

        Map<String, MutableSignal> gameSignals = new LinkedHashMap<>();
        for (JsonNode live : data) {
            if (!"GAME".equalsIgnoreCase(text(live, "categoryType"))) {
                continue;
            }

            String gameName = text(live, "liveCategoryValue");
            if (gameName.isBlank()) {
                continue;
            }

            MutableSignal signal = gameSignals.computeIfAbsent(normalizeKey(gameName), key -> new MutableSignal(gameName));
            signal.liveStreamCount++;
            signal.totalViewerCount += Math.max(0, live.path("concurrentUserCount").asInt(0));
            signal.addTopChannel(formatTopChannel(
                    text(live, "channelName"),
                    text(live, "liveTitle"),
                    text(live, "openDate")
            ));
        }

        return gameSignals.values()
                .stream()
                .sorted(Comparator.comparingInt(MutableSignal::totalViewerCount).reversed())
                .map(signal -> signal.toLiveGameSignal(SOURCE))
                .toList();
    }

    private JsonNode liveData(JsonNode response) {
        if (response == null) {
            return null;
        }
        JsonNode content = response.has("content") ? response.path("content") : response;
        return content.path("data");
    }

    private String resolveApiBaseUrl(LiveTrendProperties.PlatformCredential credential) {
        if (hasText(credential.getApiBaseUrl())) {
            return credential.getApiBaseUrl();
        }
        return DEFAULT_API_BASE_URL;
    }

    private boolean hasClientCredentials(LiveTrendProperties.PlatformCredential credential) {
        return hasText(credential.getClientId()) && hasText(credential.getClientSecret());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String text(JsonNode node, String field) {
        return node == null ? "" : node.path(field).asText("");
    }

    private String normalizeKey(String value) {
        return value.strip().toLowerCase();
    }

    private String formatTopChannel(String channelName, String liveTitle, String openDate) {
        List<String> parts = new ArrayList<>();
        if (!channelName.isBlank()) {
            parts.add("channelName=" + channelName);
        }
        if (!liveTitle.isBlank()) {
            parts.add("liveTitle=" + liveTitle);
        }
        if (!openDate.isBlank()) {
            parts.add("openDate=" + openDate);
        }
        return String.join(", ", parts);
    }

    private static class MutableSignal {

        private static final int TOP_CHANNEL_LIMIT = 3;

        private final String gameName;
        private int liveStreamCount;
        private int totalViewerCount;
        private final List<String> topChannels = new ArrayList<>();

        private MutableSignal(String gameName) {
            this.gameName = gameName;
        }

        private int totalViewerCount() {
            return totalViewerCount;
        }

        private void addTopChannel(String topChannel) {
            if (!topChannel.isBlank() && topChannels.size() < TOP_CHANNEL_LIMIT) {
                topChannels.add(topChannel);
            }
        }

        private LiveGameSignal toLiveGameSignal(String source) {
            return new LiveGameSignal(
                    source,
                    gameName,
                    gameName,
                    liveStreamCount,
                    totalViewerCount,
                    List.copyOf(topChannels),
                    "chzzk.open.v1.lives"
            );
        }
    }
}
