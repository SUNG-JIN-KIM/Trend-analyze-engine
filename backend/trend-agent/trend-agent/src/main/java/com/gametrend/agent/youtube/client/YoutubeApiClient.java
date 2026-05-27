package com.gametrend.agent.youtube.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.gametrend.agent.youtube.config.YoutubeProperties;
import com.gametrend.agent.youtube.entity.YoutubeComment;
import com.gametrend.agent.youtube.entity.YoutubeVideo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class YoutubeApiClient {

    private final WebClient.Builder webClientBuilder;
    private final YoutubeProperties properties;

    public List<String> searchVideoIds(String keyword) {
        JsonNode root = webClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("part", "snippet")
                        .queryParam("type", "video")
                        .queryParam("q", keyword)
                        .queryParam("maxResults", properties.maxResults())
                        .queryParam("regionCode", properties.regionCode())
                        .queryParam("relevanceLanguage", properties.relevanceLanguage())
                        .queryParam("key", properties.key())
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        List<String> videoIds = new ArrayList<>();
        for (JsonNode item : items(root)) {
            String videoId = text(item.path("id").path("videoId"));
            if (!videoId.isBlank()) {
                videoIds.add(videoId);
            }
        }
        return videoIds;
    }

    public List<YoutubeVideo> findVideos(String keyword, List<String> videoIds, LocalDateTime collectedAt) {
        if (videoIds.isEmpty()) {
            return List.of();
        }
        JsonNode root = webClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/videos")
                        .queryParam("part", "snippet,statistics,contentDetails")
                        .queryParam("id", String.join(",", videoIds))
                        .queryParam("key", properties.key())
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        List<YoutubeVideo> videos = new ArrayList<>();
        for (JsonNode item : items(root)) {
            JsonNode snippet = item.path("snippet");
            JsonNode statistics = item.path("statistics");
            JsonNode thumbnails = snippet.path("thumbnails");
            videos.add(YoutubeVideo.builder()
                    .videoId(text(item.path("id")))
                    .gameKeyword(keyword)
                    .keyword(keyword)
                    .title(limit(text(snippet.path("title")), 500))
                    .description(text(snippet.path("description")))
                    .publishedAt(toLocalDateTime(text(snippet.path("publishedAt"))))
                    .channelId(text(snippet.path("channelId")))
                    .channelTitle(limit(text(snippet.path("channelTitle")), 300))
                    .thumbnailUrl(resolveThumbnail(thumbnails))
                    .viewCount(number(statistics.path("viewCount")))
                    .likeCount(number(statistics.path("likeCount")))
                    .commentCount(number(statistics.path("commentCount")))
                    .durationSeconds(parseDurationSeconds(text(item.path("contentDetails").path("duration"))))
                    .collectedAt(collectedAt)
                    .updatedAt(collectedAt)
                    .build());
        }
        return videos;
    }

    public List<YoutubeComment> findTopLevelComments(
            YoutubeVideo video,
            String gameKeyword,
            int maxResults,
            LocalDateTime collectedAt
    ) {
        JsonNode root = webClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/commentThreads")
                        .queryParam("part", "snippet")
                        .queryParam("videoId", video.getVideoId())
                        .queryParam("maxResults", Math.max(1, Math.min(100, maxResults)))
                        .queryParam("order", "relevance")
                        .queryParam("textFormat", "plainText")
                        .queryParam("key", properties.key())
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(ex -> Mono.empty())
                .block();

        List<YoutubeComment> comments = new ArrayList<>();
        for (JsonNode item : items(root)) {
            JsonNode snippet = item.path("snippet").path("topLevelComment").path("snippet");
            comments.add(YoutubeComment.builder()
                    .commentId(text(item.path("id")))
                    .videoId(video.getVideoId())
                    .gameKeyword(gameKeyword)
                    .authorName(limit(text(snippet.path("authorDisplayName")), 300))
                    .text(text(snippet.path("textDisplay")))
                    .likeCount(number(snippet.path("likeCount")))
                    .publishedAt(toLocalDateTime(text(snippet.path("publishedAt"))))
                    .collectedAt(collectedAt)
                    .build());
        }
        return comments;
    }

    private WebClient webClient() {
        return webClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    private Iterable<JsonNode> items(JsonNode root) {
        if (root == null || !root.has("items") || !root.path("items").isArray()) {
            return List.of();
        }
        return root.path("items");
    }

    private String resolveThumbnail(JsonNode thumbnails) {
        for (String key : List.of("maxres", "standard", "high", "medium", "default")) {
            String url = text(thumbnails.path(key).path("url"));
            if (!url.isBlank()) {
                return url;
            }
        }
        return "";
    }

    private LocalDateTime toLocalDateTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.parse(text), ZoneId.systemDefault());
    }

    private long parseDurationSeconds(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        try {
            return Duration.parse(text).toSeconds();
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private String text(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? "" : node.asText("");
    }

    private long number(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return 0;
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        String text = node.asText("");
        if (text.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
