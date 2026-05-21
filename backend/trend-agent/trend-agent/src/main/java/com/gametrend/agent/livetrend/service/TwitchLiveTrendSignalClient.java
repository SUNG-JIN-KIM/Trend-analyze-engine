package com.gametrend.agent.livetrend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.gametrend.agent.livetrend.config.LiveTrendProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TwitchLiveTrendSignalClient implements LiveTrendSignalClient {

    private static final String SOURCE = LiveTrendPlatformStatusService.TWITCH;
    private static final String DEFAULT_API_BASE_URL = "https://api.twitch.tv";
    private static final String TOKEN_URL = "https://id.twitch.tv/oauth2/token";
    private static final int STREAM_PAGE_SIZE = 100;
    private static final int BODY_LOG_LIMIT = 500;

    private final LiveTrendProperties properties;
    private final ObjectMapper objectMapper;
    private volatile CachedToken cachedToken;

    @Override
    public String source() {
        return SOURCE;
    }

    @Override
    public List<LiveGameSignal> fetchSignals() {
        LiveTrendProperties.PlatformCredential credential = properties.getPlatforms().getTwitch();
        if (!hasClientCredentials(credential)) {
            return List.of();
        }

        try {
            String accessToken = appAccessToken(credential);
            String responseBody = RestClient.builder()
                    .baseUrl(resolveApiBaseUrl(credential))
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/helix/streams")
                            .queryParam("first", STREAM_PAGE_SIZE)
                            .build())
                    .header("Client-Id", credential.getClientId())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            JsonNode response = parseApiResponse(responseBody);
            return toSignals(response);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Twitch API 호출에 실패했습니다: " + exception.getClass().getSimpleName(), exception);
        }
    }

    List<LiveGameSignal> toSignals(JsonNode response) {
        Map<String, MutableSignal> signals = new LinkedHashMap<>();
        JsonNode data = response == null ? null : response.path("data");
        if (data == null || !data.isArray()) {
            return List.of();
        }

        for (JsonNode stream : data) {
            String gameName = text(stream, "game_name");
            if (gameName.isBlank()) {
                continue;
            }
            MutableSignal signal = signals.computeIfAbsent(normalizeKey(gameName), key -> new MutableSignal(gameName));
            signal.liveStreamCount++;
            signal.totalViewerCount += Math.max(0, stream.path("viewer_count").asInt(0));
            signal.addTopChannel(formatTopChannel(
                    text(stream, "user_name"),
                    text(stream, "title"),
                    text(stream, "started_at")
            ));
        }

        return signals.values()
                .stream()
                .sorted(Comparator.comparingInt(MutableSignal::totalViewerCount).reversed())
                .map(signal -> signal.toLiveGameSignal(SOURCE))
                .toList();
    }

    private String appAccessToken(LiveTrendProperties.PlatformCredential credential) {
        CachedToken token = cachedToken;
        LocalDateTime now = LocalDateTime.now();
        if (token != null && token.expiresAt().isAfter(now.plusSeconds(60))) {
            return token.accessToken();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", credential.getClientId());
        form.add("client_secret", credential.getClientSecret());
        form.add("grant_type", "client_credentials");

        try {
            String responseBody = RestClient.create()
                    .post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);

            JsonNode response = parseTokenResponse(responseBody);
            String accessToken = response == null ? "" : response.path("access_token").asText("");
            int expiresIn = response == null ? 0 : response.path("expires_in").asInt(0);
            if (accessToken.isBlank()) {
                throw new IllegalStateException("Twitch App Access Token 응답에 access_token이 없습니다.");
            }
            CachedToken nextToken = new CachedToken(accessToken, now.plusSeconds(Math.max(60, expiresIn - 60L)));
            cachedToken = nextToken;
            return nextToken.accessToken();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Twitch App Access Token 발급에 실패했습니다: " + exception.getClass().getSimpleName(), exception);
        }
    }

    private JsonNode parseApiResponse(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            log.warn("Twitch API 응답 JSON 파싱 실패. bodyPrefix={}, cause={}",
                    responsePrefix(responseBody),
                    exception.getClass().getSimpleName());
            throw new IllegalStateException("Twitch API 응답 JSON 파싱에 실패했습니다.", exception);
        }
    }

    private JsonNode parseTokenResponse(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Twitch App Access Token 응답 JSON 파싱에 실패했습니다.", exception);
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

    private String formatTopChannel(String channelName, String liveTitle, String startedAt) {
        List<String> parts = new ArrayList<>();
        if (!channelName.isBlank()) {
            parts.add("channelName=" + channelName);
        }
        if (!liveTitle.isBlank()) {
            parts.add("liveTitle=" + liveTitle);
        }
        if (!startedAt.isBlank()) {
            parts.add("openDate=" + startedAt);
        }
        return String.join(", ", parts);
    }

    private record CachedToken(String accessToken, LocalDateTime expiresAt) {
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
                    "twitch.helix.streams"
            );
        }
    }
}
