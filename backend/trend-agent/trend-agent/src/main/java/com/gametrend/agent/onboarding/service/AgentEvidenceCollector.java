package com.gametrend.agent.onboarding.service;

import com.gametrend.agent.gameimage.GameImageResolver;
import com.gametrend.agent.livetrend.dto.LiveTrendGameResponse;
import com.gametrend.agent.livetrend.service.LiveTrendService;
import com.gametrend.agent.onboarding.dto.AgentEvidenceBundle;
import com.gametrend.agent.onboarding.dto.AgentPlan;
import com.gametrend.agent.onboarding.dto.AgentQueryConditionResponse;
import com.gametrend.agent.onboarding.dto.EvidenceCardResponse;
import com.gametrend.agent.reinterpretation.dto.ReinterpretationCandidateResponse;
import com.gametrend.agent.reinterpretation.service.ReinterpretationCandidateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class AgentEvidenceCollector {

    private final LiveTrendService liveTrendService;
    private final ReinterpretationCandidateService reinterpretationCandidateService;
    private final NonGameCategoryFilter nonGameCategoryFilter;

    public AgentEvidenceCollector(
            LiveTrendService liveTrendService,
            ReinterpretationCandidateService reinterpretationCandidateService,
            NonGameCategoryFilter nonGameCategoryFilter
    ) {
        this.liveTrendService = liveTrendService;
        this.reinterpretationCandidateService = reinterpretationCandidateService;
        this.nonGameCategoryFilter = nonGameCategoryFilter;
    }

    public AgentEvidenceBundle collect(
            AgentPlan plan,
            AgentQueryConditionResponse queryCondition,
            String userMessage
    ) {
        if (plan == null) {
            return AgentEvidenceBundle.empty();
        }

        List<LiveTrendGameResponse> liveTrendGames = collectLiveTrendGames(plan, queryCondition, userMessage);
        List<ReinterpretationCandidateResponse> reinterpretationCandidates = collectReinterpretationCandidates(plan, queryCondition);
        List<EvidenceCardResponse> evidenceCards = new ArrayList<>();
        evidenceCards.addAll(toLiveTrendEvidenceCards(plan, liveTrendGames));
        evidenceCards.addAll(toReinterpretationEvidenceCards(reinterpretationCandidates));
        if (evidenceCards.isEmpty() && plan.needsClarification() && !plan.needsGameRecommendation()) {
            evidenceCards.add(new EvidenceCardResponse(
                    "추가 정보 필요",
                    "GENERAL_CONTEXT",
                    "질문이 조금 넓거나 모호해서 가능한 범위의 임시 답변과 함께 추가 질문을 제안합니다.",
                    null, null, null, null, null, null,
                    plan.reasoningSummary(),
                    null, null, null, null, null, null,
                    "GENERAL_CONTEXT", "GENERAL_CONTEXT",
                    null, null, null, null, null, null, null, null, null
            ));
        }

        return new AgentEvidenceBundle(liveTrendGames, reinterpretationCandidates, List.copyOf(evidenceCards));
    }

    private List<LiveTrendGameResponse> collectLiveTrendGames(
            AgentPlan plan,
            AgentQueryConditionResponse queryCondition,
            String userMessage
    ) {
        if (!plan.needsLiveTrend() && !plan.needsGameRecommendation()) {
            return List.of();
        }
        try {
            String platform = plan.platformFilter() == null || plan.platformFilter().isBlank()
                    ? "ALL"
                    : plan.platformFilter();

            List<LiveTrendGameResponse> games = liveTrendService.findTopLiveTrendGames(50, platform)
                    .stream()
                    .filter(game -> !queryCondition.excludeNonGameCategories()
                            || nonGameCategoryFilter.shouldInclude(game, userMessage))
                    // ✅ 핵심 수정: 장르 필터 적용
                    // 기존: 장르 무관하게 트렌드 점수 상위 5개 반환 → "fps 추천해줘"에 Minecraft 포함
                    // 개선: genreFilter가 있으면 해당 장르만, 없으면 전체
                    .filter(game -> matchesGenre(game, plan.genreFilter()))
                    .filter(game -> matchesRequestedPlayMode(game, userMessage, plan))
                    .sorted(liveTrendComparator(plan.sortMetric()))
                    .limit(5)
                    .toList();

            // 장르가 명시된 플레이 추천은 조건이 맞는 후보가 없을 때 일반 인기 게임으로 새지 않게 막습니다.
            // 예: "친구랑 할 게임"에 MOBA/도박 카테고리가 섞이면 추천 의도가 흐려집니다.
            if (games.isEmpty()
                    && plan.needsGameRecommendation()
                    && plan.genreFilter() != null
                    && !plan.genreFilter().isBlank()) {
                log.info("플레이 추천 장르 필터({}) 결과가 없어 전체 트렌드 fallback을 건너뜁니다.", plan.genreFilter());
                return List.of();
            }

            // 장르 필터 결과가 비었을 때 fallback: 필터 없이 전체에서 재조회
            // 트렌드 분석/시장 분석은 데이터 부족 안내보다 전체 흐름을 보조 근거로 보여주는 편이 나을 수 있습니다.
            if (games.isEmpty() && plan.genreFilter() != null && !plan.genreFilter().isBlank()) {
                log.info("장르 필터({}) 결과 없음. 전체 트렌드로 fallback합니다.", plan.genreFilter());
                games = liveTrendService.findTopLiveTrendGames(50, platform)
                        .stream()
                        .filter(game -> !queryCondition.excludeNonGameCategories()
                                || nonGameCategoryFilter.shouldInclude(game, userMessage))
                        .sorted(liveTrendComparator(plan.sortMetric()))
                        .limit(5)
                        .toList();
            }

            return games;
        } catch (RuntimeException ex) {
            log.warn("Agent evidence liveTrend 조회 실패. cause={}", ex.toString());
            return List.of();
        }
    }

    /**
     * ✅ 신규: 장르 매칭 로직
     *
     * genreFilter가 null이거나 비어있으면 → 모든 게임 통과 (필터 없음)
     * genreFilter가 있으면 → 게임 title 또는 genre 필드가 해당 장르 키워드를 포함하면 통과
     *
     * 예시:
     *   genreFilter="FPS" → Counter-Strike 2(genre=FPS) ✅ / Minecraft(genre=Sandbox) ❌
     *   genreFilter="HORROR" → Lethal Company(genre=Horror) ✅ / Minecraft ❌
     *   genreFilter=null → Minecraft ✅ Counter-Strike 2 ✅ (전체 통과)
     */
    private boolean matchesGenre(LiveTrendGameResponse game, String genreFilter) {
        if (genreFilter == null || genreFilter.isBlank()) {
            return true; // 장르 필터 없으면 전체 통과
        }

        String filter = genreFilter.toLowerCase(Locale.ROOT);
        String title = game.title() == null ? "" : game.title().toLowerCase(Locale.ROOT);
        String genre = game.genre() == null ? "" : game.genre().toLowerCase(Locale.ROOT);

        return switch (filter) {
            case "fps" -> containsAny(title, "counter-strike", "cs2", "pubg", "배틀그라운드",
                    "valorant", "overwatch", "apex", "rainbow six", "서든어택", "sudden attack",
                    "helldivers", "call of duty", "cod")
                    || containsAny(genre, "fps", "shooter", "battle royale", "tactical", "first-person");

            case "horror" -> containsAny(title, "lethal company", "phasmophobia", "dead by daylight",
                    "resident evil", "five nights", "fnaf", "little nightmares")
                    || containsAny(genre, "horror", "survival horror", "psychological");

            case "party" -> containsAny(title, "among us", "fall guys", "pummel party",
                    "jackbox", "goose goose duck", "it takes two", "overcooked", "plateup",
                    "unrailed", "lethal company", "phasmophobia", "content warning",
                    "minecraft", "valheim", "palworld")
                    || containsAny(genre, "party", "social deduction", "co-op", "coop",
                    "cooperative", "multiplayer party", "survival", "craft", "sandbox");

            case "survival", "survival_roguelike" -> containsAny(title, "minecraft", "valheim",
                    "rust", "ark", "the forest", "sons of the forest", "palworld", "subnautica")
                    || containsAny(genre, "survival", "craft", "roguelike", "open world survival");

            case "puzzle", "puzzle_adventure" -> containsAny(genre, "puzzle", "adventure",
                    "point and click", "narrative");

            case "moba" -> containsAny(title, "league of legends", "lol", "dota", "heroes of the storm")
                    || containsAny(genre, "moba", "multiplayer online battle");

            case "rpg" -> containsAny(title, "elden ring", "baldur's gate", "path of exile",
                    "lost ark", "final fantasy", "diablo", "monster hunter", "cyberpunk",
                    "persona", "witcher", "skyrim")
                    || containsAny(genre, "rpg", "role-playing", "action rpg", "jrpg", "mmorpg");

            case "sports" -> containsAny(title, "fifa", "fc 25", "nba 2k", "eFootball", "rocket league")
                    || containsAny(genre, "sports", "racing", "football", "soccer");

            // 기타 장르: title/genre에 필터 문자열이 포함되면 통과
            default -> title.contains(filter) || genre.contains(filter);
        };
    }

    private boolean matchesRequestedPlayMode(
            LiveTrendGameResponse game,
            String userMessage,
            AgentPlan plan
    ) {
        if (plan == null || !plan.needsGameRecommendation()) {
            return true;
        }
        if (isSoloPlayRequest(userMessage)) {
            return matchesSoloFriendlyGame(game);
        }
        if (isFriendPlayRequest(userMessage)) {
            return matchesFriendFriendlyGame(game);
        }
        return true;
    }

    private boolean matchesSoloFriendlyGame(LiveTrendGameResponse game) {
        String title = normalize(game.title());
        String genre = normalize(game.genre());
        String keyword = normalize(game.sourceKeyword());
        if (containsAny(title, "valorant", "counter-strike", "dota", "league of legends", "lol")
                || containsAny(keyword, "valorant", "counter-strike", "dota", "league of legends", "lol")
                || containsAny(genre, "moba", "battle royale", "tactical fps", "competitive")) {
            return false;
        }
        return containsAny(title, "doom", "titanfall", "metro", "borderlands", "cyberpunk",
                "elden ring", "baldur", "witcher", "skyrim", "persona", "diablo",
                "path of exile", "monster hunter", "hades", "subnautica", "resident evil")
                || containsAny(genre, "single", "single-player", "single player", "rpg", "role-playing",
                "action rpg", "jrpg", "mmorpg", "adventure", "survival horror", "roguelike",
                "soulslike", "open world", "sandbox survival", "puzzle");
    }

    private boolean matchesFriendFriendlyGame(LiveTrendGameResponse game) {
        String title = normalize(game.title());
        String genre = normalize(game.genre());
        String keyword = normalize(game.sourceKeyword());
        return containsAny(title, "lethal", "phasmophobia", "content warning", "among us",
                "fall guys", "pummel party", "jackbox", "goose goose duck", "it takes two",
                "overcooked", "plateup", "unrailed", "minecraft", "valheim", "palworld",
                "monster hunter")
                || containsAny(keyword, "lethal", "phasmophobia", "content warning", "among us",
                "fall guys", "pummel party", "jackbox", "goose goose duck", "it takes two",
                "overcooked", "plateup", "unrailed", "minecraft", "valheim", "palworld",
                "monster hunter")
                || containsAny(genre, "party", "social", "co-op", "coop", "cooperative",
                "multiplayer party", "social deduction", "survival", "craft", "sandbox", "action rpg");
    }

    private boolean isSoloPlayRequest(String message) {
        return containsAny(normalize(message), "혼자", "솔로", "1인", "싱글", "single", "singleplayer", "single-player", "solo");
    }

    private boolean isFriendPlayRequest(String message) {
        String normalized = normalize(message);
        return containsAny(normalized, "친구랑", "친구와", "친구", "같이", "함께", "둘이", "여럿", "협동", "멀티", "co-op", "coop")
                && containsAny(normalized, "게임", "추천", "할", "한다", "한다고", "했는데", "플레이", "즐길");
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).strip();
    }

    private boolean containsAny(String value, String... keywords) {
        if (value == null || keywords == null) return false;
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && value.contains(keyword.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private List<ReinterpretationCandidateResponse> collectReinterpretationCandidates(
            AgentPlan plan,
            AgentQueryConditionResponse queryCondition
    ) {
        if (!plan.needsReinterpretation()) {
            return List.of();
        }
        try {
            return reinterpretationCandidateService.findCandidates(queryCondition, 5);
        } catch (RuntimeException ex) {
            log.warn("Agent evidence 재해석 후보 조회 실패. cause={}", ex.toString());
            return List.of();
        }
    }

    private List<EvidenceCardResponse> toLiveTrendEvidenceCards(
            AgentPlan plan,
            List<LiveTrendGameResponse> games
    ) {
        String evidenceType = plan.needsGameRecommendation() ? "GAME_RECOMMENDATION" : "LIVE_TREND";
        return games.stream()
                .limit(3)
                .map(game -> new EvidenceCardResponse(
                        game.title(),
                        evidenceType,
                        "%s 기준으로 수집된 라이브 트렌드 근거입니다.".formatted(game.source()),
                        game.trendScore(),
                        null,
                        "TWITCH".equalsIgnoreCase(game.source()) ? game.totalViewerCount() : null,
                        "TWITCH".equalsIgnoreCase(game.source()) ? game.liveStreamCount() : null,
                        game.streamabilityScore(),
                        game.marketSignalScore(),
                        game.reason(),
                        game.source(),
                        game.genre(),
                        game.totalViewerCount(),
                        game.liveStreamCount(),
                        game.signalStatus(),
                        game.dataOrigin(),
                        evidenceType,
                        evidenceType,
                        null, null, null, null, null, null, null, null, null,
                        game.imageUrl()
                ))
                .toList();
    }

    private List<EvidenceCardResponse> toReinterpretationEvidenceCards(
            List<ReinterpretationCandidateResponse> candidates
    ) {
        return candidates.stream()
                .limit(3)
                .map(candidate -> new EvidenceCardResponse(
                        candidate.title(),
                        "REINTERPRETATION_CANDIDATE",
                        candidate.reinterpretationConcept(),
                        candidate.reinterpretationScore(),
                        null, null, null,
                        candidate.streamabilityScore(),
                        candidate.modernTrendFitScore(),
                        candidate.reason(),
                        candidate.source(),
                        String.join(", ", candidate.genres()),
                        null, null, null,
                        candidate.dataOrigin(),
                        "REINTERPRETATION",
                        "REINTERPRETATION_CANDIDATE",
                        String.join(", ", candidate.genres()),
                        candidate.reinterpretationConcept(),
                        candidate.reinterpretationScore(),
                        candidate.legacyPopularityScore(),
                        candidate.reviewSentimentScore(),
                        candidate.mechanicUniquenessScore(),
                        candidate.interactionFitScore(),
                        candidate.modernTrendFitScore(),
                        candidate.devFeasibilityScore(),
                        GameImageResolver.resolveImageUrl(candidate.title(), null, candidate.steamAppId())
                ))
                .toList();
    }

    private Comparator<LiveTrendGameResponse> liveTrendComparator(String sortMetric) {
        return switch (sortMetric == null ? "" : sortMetric) {
            case "VIEWER_COUNT" -> Comparator.comparingInt(LiveTrendGameResponse::totalViewerCount).reversed()
                    .thenComparing(Comparator.comparingDouble(LiveTrendGameResponse::trendScore).reversed());
            case "STREAM_COUNT", "STREAMER_SPREAD" -> Comparator.comparingInt(LiveTrendGameResponse::liveStreamCount).reversed()
                    .thenComparing(Comparator.comparingDouble(LiveTrendGameResponse::trendScore).reversed());
            case "MARKET_SIGNAL" -> Comparator.comparingInt(LiveTrendGameResponse::marketSignalScore).reversed()
                    .thenComparing(Comparator.comparingDouble(LiveTrendGameResponse::trendScore).reversed());
            default -> Comparator.comparingDouble(LiveTrendGameResponse::trendScore).reversed()
                    .thenComparing(Comparator.comparingInt(LiveTrendGameResponse::totalViewerCount).reversed())
                    .thenComparing(Comparator.comparingInt(LiveTrendGameResponse::liveStreamCount).reversed());
        };
    }
}
