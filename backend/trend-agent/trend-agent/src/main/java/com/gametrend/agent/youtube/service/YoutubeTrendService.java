package com.gametrend.agent.youtube.service;

import com.gametrend.agent.game.repository.GameRepository;
import com.gametrend.agent.admin.common.AdminPageResponse;
import com.gametrend.agent.youtube.client.YoutubeApiClient;
import com.gametrend.agent.youtube.config.YoutubeProperties;
import com.gametrend.agent.youtube.dto.GameYoutubeTrendScoreResponse;
import com.gametrend.agent.youtube.dto.YoutubeCommentCollectResponse;
import com.gametrend.agent.youtube.dto.YoutubeCommentReactionSummaryResponse;
import com.gametrend.agent.youtube.dto.YoutubeCollectLogResponse;
import com.gametrend.agent.youtube.dto.YoutubeCollectResponse;
import com.gametrend.agent.youtube.dto.YoutubeDashboardResponse;
import com.gametrend.agent.youtube.dto.YoutubeDashboardSummaryResponse;
import com.gametrend.agent.youtube.dto.YoutubeTrendResponse;
import com.gametrend.agent.youtube.dto.YoutubeVideoResponse;
import com.gametrend.agent.youtube.entity.GameYoutubeTrendScore;
import com.gametrend.agent.youtube.entity.YoutubeComment;
import com.gametrend.agent.youtube.entity.YoutubeCollectLog;
import com.gametrend.agent.youtube.entity.YoutubeKeywordStat;
import com.gametrend.agent.youtube.entity.YoutubeVideo;
import com.gametrend.agent.youtube.repository.GameYoutubeTrendScoreRepository;
import com.gametrend.agent.youtube.repository.YoutubeCommentRepository;
import com.gametrend.agent.youtube.repository.YoutubeCollectLogRepository;
import com.gametrend.agent.youtube.repository.YoutubeKeywordStatRepository;
import com.gametrend.agent.youtube.repository.YoutubeVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class YoutubeTrendService {

    private final YoutubeProperties properties;
    private final YoutubeApiClient youtubeApiClient;
    private final YoutubeVideoRepository youtubeVideoRepository;
    private final YoutubeCommentRepository youtubeCommentRepository;
    private final YoutubeCollectLogRepository youtubeCollectLogRepository;
    private final YoutubeKeywordStatRepository youtubeKeywordStatRepository;
    private final GameYoutubeTrendScoreRepository gameYoutubeTrendScoreRepository;
    private final GameRepository gameRepository;
    private final YoutubeTrendScoreCalculator scoreCalculator;
    private final YoutubeCommentKeywordAnalyzer commentKeywordAnalyzer;

    @Transactional
    public YoutubeCollectResponse collect(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cooldownCutoff = now.minusMinutes(properties.collectCooldownMinutes());
        var recentSuccess = youtubeCollectLogRepository.findRecentSuccess(normalizedKeyword, cooldownCutoff);
        if (recentSuccess.isPresent()) {
            GameYoutubeTrendScore score = findScoreByKeyword(normalizedKeyword);
            return new YoutubeCollectResponse(
                    normalizedKeyword,
                    "SKIPPED",
                    "최근 수집 결과를 재사용했습니다.",
                    GameYoutubeTrendScoreResponse.from(score)
            );
        }

        if (!properties.hasApiKey()) {
            saveLog(normalizedKeyword, "FAILED", "YOUTUBE_API_KEY가 설정되지 않았습니다.", 0, 0, now, LocalDateTime.now());
            throw new YoutubeTrendException("YOUTUBE_API_KEY가 설정되지 않았습니다.");
        }

        try {
            List<String> videoIds = youtubeApiClient.searchVideoIds(normalizedKeyword);
            List<YoutubeVideo> videos = youtubeApiClient.findVideos(normalizedKeyword, videoIds, now);

            youtubeVideoRepository.saveAll(videos.stream().map(this::withExistingVideoId).toList());

            GameYoutubeTrendScore savedScore = saveScore(normalizedKeyword, videos, now);
            saveLog(normalizedKeyword, "SUCCESS", "YouTube 데이터 수집을 완료했습니다.", videos.size(), 1, now, LocalDateTime.now());
            return new YoutubeCollectResponse(
                    normalizedKeyword,
                    "SUCCESS",
                    "YouTube 데이터 수집을 완료했습니다.",
                    GameYoutubeTrendScoreResponse.from(savedScore)
            );
        } catch (RuntimeException ex) {
            saveLog(normalizedKeyword, "FAILED", limit(ex.getMessage(), 1000), 0, 0, now, LocalDateTime.now());
            throw new YoutubeTrendException("YouTube 데이터 수집에 실패했습니다.", ex);
        }
    }

    public List<YoutubeVideoResponse> findVideos(String keyword) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : normalizeKeyword(keyword);
        List<YoutubeVideo> videos = normalizedKeyword == null
                ? StreamSupport.stream(youtubeVideoRepository.findAll().spliterator(), false)
                        .sorted((a, b) -> Long.compare(b.getViewCount(), a.getViewCount()))
                        .toList()
                : youtubeVideoRepository.findByKeywordOrderByViewCountDesc(normalizedKeyword);
        return videos.stream()
                .map(YoutubeVideoResponse::from)
                .toList();
    }

    @Transactional
    public YoutubeCommentCollectResponse collectComments(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        LocalDateTime now = LocalDateTime.now();
        if (!properties.hasApiKey()) {
            saveLog(normalizedKeyword, "FAILED", "YOUTUBE_API_KEY가 설정되지 않았습니다.", 0, 0, now, LocalDateTime.now());
            throw new YoutubeTrendException("YOUTUBE_API_KEY가 설정되지 않았습니다.");
        }

        List<YoutubeVideo> targetVideos = youtubeVideoRepository.findByKeywordOrderByViewCountDesc(normalizedKeyword)
                .stream()
                .limit(5)
                .toList();
        if (targetVideos.isEmpty()) {
            throw new YoutubeTrendException("댓글을 수집할 YouTube 영상이 없습니다. 먼저 영상 수집을 실행해주세요.");
        }

        List<YoutubeComment> collectedComments = targetVideos.stream()
                .flatMap(video -> youtubeApiClient.findTopLevelComments(video, normalizedKeyword, 20, now).stream())
                .map(this::withExistingCommentId)
                .toList();
        youtubeCommentRepository.saveAll(collectedComments);

        updateKeywordStats(normalizedKeyword, collectedComments, now);
        YoutubeCommentReactionSummaryResponse summary = buildReactionSummary(normalizedKeyword);
        saveLog(
                normalizedKeyword,
                "SUCCESS",
                "YouTube 댓글 수집과 키워드 분석을 완료했습니다.",
                collectedComments.size(),
                summary.positiveMentionCount() + summary.negativeMentionCount(),
                now,
                LocalDateTime.now()
        );
        return new YoutubeCommentCollectResponse(
                normalizedKeyword,
                "SUCCESS",
                "YouTube 댓글 수집과 키워드 분석을 완료했습니다.",
                targetVideos.size(),
                collectedComments.size(),
                summary
        );
    }

    public AdminPageResponse<YoutubeVideoResponse> findVideoPage(
            String title,
            String keyword,
            String channelTitle,
            String sort,
            int page,
            int size
    ) {
        Comparator<YoutubeVideo> comparator = switch (sort == null ? "" : sort) {
            case "commentCount" -> Comparator.comparingLong(YoutubeVideo::getCommentCount).reversed();
            case "publishedAt" -> Comparator.comparing(
                    YoutubeVideo::getPublishedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ).reversed();
            case "collectedAt" -> Comparator.comparing(
                    YoutubeVideo::getCollectedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ).reversed();
            default -> Comparator.comparingLong(YoutubeVideo::getViewCount).reversed();
        };

        List<YoutubeVideoResponse> filtered = StreamSupport.stream(youtubeVideoRepository.findAll().spliterator(), false)
                .filter(video -> containsIgnoreCase(video.getTitle(), title))
                .filter(video -> containsIgnoreCase(video.getGameKeyword(), keyword))
                .filter(video -> containsIgnoreCase(video.getChannelTitle(), channelTitle))
                .sorted(comparator)
                .map(YoutubeVideoResponse::from)
                .toList();
        return AdminPageResponse.of(filtered, page, size);
    }

    public List<YoutubeCollectLogResponse> findLogs() {
        return youtubeCollectLogRepository.findRecent(50).stream()
                .map(YoutubeCollectLogResponse::from)
                .toList();
    }

    public AdminPageResponse<YoutubeCollectLogResponse> findLogPage(
            String keyword,
            String status,
            int page,
            int size
    ) {
        List<YoutubeCollectLogResponse> filtered = StreamSupport.stream(youtubeCollectLogRepository.findAll().spliterator(), false)
                .filter(log -> containsIgnoreCase(log.getKeyword(), keyword))
                .filter(log -> status == null || status.isBlank() || log.getStatus().equalsIgnoreCase(status))
                .sorted(Comparator.comparing(
                        YoutubeCollectLog::getStartedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .map(YoutubeCollectLogResponse::from)
                .toList();
        return AdminPageResponse.of(filtered, page, size);
    }

    public YoutubeDashboardSummaryResponse getDashboardSummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<YoutubeCollectLog> todayLogs = youtubeCollectLogRepository.findAllSince(todayStart);
        List<YoutubeCollectLog> allLogs = StreamSupport.stream(youtubeCollectLogRepository.findAll().spliterator(), false)
                .toList();
        String topGameKeyword = gameYoutubeTrendScoreRepository.findTopScores(1).stream()
                .findFirst()
                .map(GameYoutubeTrendScore::getKeyword)
                .orElse("-");
        String latestCollectKeyword = allLogs.stream()
                .max(Comparator.comparing(YoutubeCollectLog::getStartedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(YoutubeCollectLog::getKeyword)
                .orElse("-");

        return new YoutubeDashboardSummaryResponse(
                youtubeVideoRepository.count(),
                todayLogs.size(),
                allLogs.stream().filter(log -> "SUCCESS".equalsIgnoreCase(log.getStatus())).count(),
                allLogs.stream().filter(log -> "FAILED".equalsIgnoreCase(log.getStatus())).count(),
                topGameKeyword,
                latestCollectKeyword
        );
    }

    public YoutubeDashboardResponse getDashboard() {
        return new YoutubeDashboardResponse(
                youtubeCollectLogRepository.findRecent(10).stream()
                        .map(YoutubeCollectLogResponse::from)
                        .toList(),
                findTopGames(10)
        );
    }

    public YoutubeTrendResponse findTrend(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        GameYoutubeTrendScore score = findScoreByKeyword(normalizedKeyword);
        return new YoutubeTrendResponse(
                GameYoutubeTrendScoreResponse.from(score),
                findVideos(normalizedKeyword),
                buildReactionSummary(normalizedKeyword)
        );
    }

    public List<GameYoutubeTrendScoreResponse> findTopGames(int limit) {
        int safeLimit = Math.max(1, Math.min(50, limit));
        return gameYoutubeTrendScoreRepository.findTopScores(safeLimit).stream()
                .map(GameYoutubeTrendScoreResponse::from)
                .toList();
    }

    public GameYoutubeTrendScoreResponse findGameTrend(Long gameId) {
        return gameYoutubeTrendScoreRepository.findLatestByGameId(gameId)
                .or(() -> gameRepository.findById(gameId)
                        .flatMap(game -> gameYoutubeTrendScoreRepository.findByKeywordIgnoreCase(game.getTitle())))
                .map(GameYoutubeTrendScoreResponse::from)
                .orElseThrow(() -> new YoutubeTrendException("해당 게임의 YouTube 트렌드 점수가 없습니다."));
    }

    private GameYoutubeTrendScore saveScore(
            String keyword,
            List<YoutubeVideo> videos,
            LocalDateTime now
    ) {
        YoutubeTrendScoreCalculator.Score score = scoreCalculator.calculate(videos, now);
        long totalViews = videos.stream().mapToLong(YoutubeVideo::getViewCount).sum();
        long totalLikes = videos.stream().mapToLong(YoutubeVideo::getLikeCount).sum();
        long totalComments = videos.stream().mapToLong(YoutubeVideo::getCommentCount).sum();
        double averageViews = videos.isEmpty() ? 0 : totalViews / (double) videos.size();

        Long matchedGameId = gameRepository.findAllByOrderByRecommendationScoreDesc().stream()
                .filter(game -> game.getTitle().equalsIgnoreCase(keyword))
                .map(com.gametrend.agent.game.entity.Game::getId)
                .findFirst()
                .orElse(null);

        GameYoutubeTrendScore existing = gameYoutubeTrendScoreRepository.findByKeywordIgnoreCase(keyword).orElse(null);
        return gameYoutubeTrendScoreRepository.save(GameYoutubeTrendScore.builder()
                .id(existing == null ? null : existing.getId())
                .gameId(matchedGameId)
                .keyword(keyword)
                .gameTitle(keyword)
                .totalViewCount(totalViews)
                .averageViewCount(Math.round(averageViews * 10.0) / 10.0)
                .totalLikeCount(totalLikes)
                .totalCommentCount(totalComments)
                .averageEngagementRate(score.averageEngagementRate())
                .videoCount(videos.size())
                .viewScore(score.viewScore())
                .engagementScore(score.engagementScore())
                .volumeScore(score.volumeScore())
                .youtubeInterestScore(score.youtubeInterestScore())
                .collectedAt(now)
                .updatedAt(now)
                .build());
    }

    private GameYoutubeTrendScore findScoreByKeyword(String keyword) {
        return gameYoutubeTrendScoreRepository.findByKeywordIgnoreCase(keyword)
                .orElseThrow(() -> new YoutubeTrendException("수집된 YouTube 트렌드 점수가 없습니다."));
    }

    private void saveLog(
            String keyword,
            String status,
            String message,
            int videoCount,
            int scoreCount,
            LocalDateTime startedAt,
            LocalDateTime completedAt
    ) {
        youtubeCollectLogRepository.save(YoutubeCollectLog.builder()
                .keyword(keyword)
                .status(status)
                .message(message)
                .videoCount(videoCount)
                .scoreCount(scoreCount)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .createdAt(startedAt)
                .build());
    }

    private YoutubeVideo withExistingVideoId(YoutubeVideo video) {
        return youtubeVideoRepository.findByVideoId(video.getVideoId())
                .map(existing -> video.toBuilder().id(existing.getId()).build())
                .orElse(video);
    }

    private YoutubeComment withExistingCommentId(YoutubeComment comment) {
        return youtubeCommentRepository.findByCommentId(comment.getCommentId())
                .map(existing -> comment.toBuilder().id(existing.getId()).build())
                .orElse(comment);
    }

    private void updateKeywordStats(String gameKeyword, List<YoutubeComment> comments, LocalDateTime now) {
        Map<YoutubeCommentKeywordAnalyzer.KeywordKey, YoutubeCommentKeywordAnalyzer.KeywordStatDraft> drafts = new LinkedHashMap<>();
        for (YoutubeComment comment : comments) {
            commentKeywordAnalyzer.analyze(comment.getText()).forEach((key, draft) -> drafts.merge(
                    key,
                    draft,
                    (left, right) -> new YoutubeCommentKeywordAnalyzer.KeywordStatDraft(
                            left.keyword(),
                            left.sentiment(),
                            left.count() + right.count(),
                            left.sampleText()
                    )
            ));
        }

        for (YoutubeCommentKeywordAnalyzer.KeywordStatDraft draft : drafts.values()) {
            YoutubeKeywordStat existing = youtubeKeywordStatRepository
                    .findStat(gameKeyword, draft.keyword(), draft.sentiment())
                    .orElse(null);
            youtubeKeywordStatRepository.save(YoutubeKeywordStat.builder()
                    .id(existing == null ? null : existing.getId())
                    .gameKeyword(gameKeyword)
                    .statKeyword(draft.keyword())
                    .sentiment(draft.sentiment())
                    .mentionCount((existing == null ? 0 : existing.getMentionCount()) + draft.count())
                    .sampleText(existing == null || existing.getSampleText() == null ? draft.sampleText() : existing.getSampleText())
                    .collectedAt(existing == null ? now : existing.getCollectedAt())
                    .updatedAt(now)
                    .build());
        }
    }

    public AdminPageResponse<com.gametrend.agent.youtube.dto.YoutubeKeywordStatResponse> findKeywordStats(
            String keyword,
            String sentiment,
            int page,
            int size
    ) {
        List<com.gametrend.agent.youtube.dto.YoutubeKeywordStatResponse> filtered = StreamSupport
                .stream(youtubeKeywordStatRepository.findAll().spliterator(), false)
                .filter(stat -> containsIgnoreCase(stat.getGameKeyword(), keyword))
                .filter(stat -> sentiment == null || sentiment.isBlank() || stat.getSentiment().equalsIgnoreCase(sentiment))
                .sorted(Comparator.comparingInt(YoutubeKeywordStat::getMentionCount).reversed())
                .map(com.gametrend.agent.youtube.dto.YoutubeKeywordStatResponse::from)
                .toList();
        return AdminPageResponse.of(filtered, page, size);
    }

    private YoutubeCommentReactionSummaryResponse buildReactionSummary(String keyword) {
        List<com.gametrend.agent.youtube.dto.YoutubeKeywordStatResponse> stats = youtubeKeywordStatRepository
                .findByGameKeyword(keyword)
                .stream()
                .map(com.gametrend.agent.youtube.dto.YoutubeKeywordStatResponse::from)
                .toList();
        List<com.gametrend.agent.youtube.dto.YoutubeKeywordStatResponse> positives = stats.stream()
                .filter(stat -> "POSITIVE".equals(stat.sentiment()))
                .limit(5)
                .toList();
        List<com.gametrend.agent.youtube.dto.YoutubeKeywordStatResponse> negatives = stats.stream()
                .filter(stat -> "NEGATIVE".equals(stat.sentiment()))
                .limit(5)
                .toList();
        int positiveCount = positives.stream().mapToInt(com.gametrend.agent.youtube.dto.YoutubeKeywordStatResponse::mentionCount).sum();
        int negativeCount = negatives.stream().mapToInt(com.gametrend.agent.youtube.dto.YoutubeKeywordStatResponse::mentionCount).sum();
        String summary = positiveCount == 0 && negativeCount == 0
                ? "아직 댓글 키워드 분석 결과가 없습니다."
                : positiveCount >= negativeCount
                        ? "댓글 반응은 긍정 키워드가 더 많이 감지되었습니다."
                        : "댓글 반응은 부정 키워드가 더 많이 감지되었습니다.";
        return new YoutubeCommentReactionSummaryResponse(
                positiveCount,
                negativeCount,
                positives,
                negatives,
                summary
        );
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new YoutubeTrendException("게임 키워드를 입력해주세요.");
        }
        return keyword.strip().toLowerCase(Locale.ROOT);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.strip().toLowerCase(Locale.ROOT));
    }

    private String limit(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
