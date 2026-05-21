package com.gametrend.agent.onboarding.service;

import com.gametrend.agent.onboarding.dto.AgentQueryConditionResponse;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class AgentQueryConditionResolver {

    private final PlatformFilterResolver platformFilterResolver;
    private final NonGameCategoryFilter nonGameCategoryFilter;

    public AgentQueryConditionResolver(
            PlatformFilterResolver platformFilterResolver,
            NonGameCategoryFilter nonGameCategoryFilter
    ) {
        this.platformFilterResolver = platformFilterResolver;
        this.nonGameCategoryFilter = nonGameCategoryFilter;
    }

    public AgentQueryConditionResponse resolve(String message) {
        return resolve(message, List.of());
    }

    public AgentQueryConditionResponse resolve(String message, List<String> preferredFeatures) {
        String normalizedMessage = normalize(message);
        List<String> interactionFeatures = resolveInteractionFeatures(normalizedMessage, preferredFeatures);
        return new AgentQueryConditionResponse(
                platformFilterResolver.resolve(message).orElse("ALL"),
                resolveSortMetric(normalizedMessage),
                resolveAnalysisPurpose(normalizedMessage, interactionFeatures),
                interactionFeatures,
                !nonGameCategoryFilter.allowsNonGameCategories(message),
                message
        );
    }

    public AgentQueryConditionResponse resolveFollowUp(
            String message,
            List<String> preferredFeatures,
            String parentMessage,
            List<String> parentPreferredFeatures
    ) {
        AgentQueryConditionResponse currentCondition = resolve(message, preferredFeatures);
        if (parentMessage == null || parentMessage.isBlank()) {
            return currentCondition;
        }

        String normalizedMessage = normalize(message);
        if (!isAmbiguousFollowUp(normalizedMessage)) {
            return currentCondition;
        }

        AgentQueryConditionResponse parentCondition = resolve(parentMessage, parentPreferredFeatures);
        return new AgentQueryConditionResponse(
                currentCondition.platformFilter(),
                currentCondition.sortMetric(),
                parentCondition.analysisPurpose(),
                currentCondition.interactionFeatures(),
                currentCondition.excludeNonGameCategories(),
                currentCondition.originalMessage()
        );
    }

    private String resolveSortMetric(String normalizedMessage) {
        if (containsAny(normalizedMessage, "시청자", "많이 보는", "조회", "viewer")) {
            return "VIEWER_COUNT";
        }
        if (containsAny(normalizedMessage, "확산", "여러 스트리머", "스트리머 확산")) {
            return "STREAMER_SPREAD";
        }
        if (containsAny(normalizedMessage, "방송 수", "방송수", "스트리머들이 많이", "많이 하는", "stream count")) {
            return "STREAM_COUNT";
        }
        if (containsAny(normalizedMessage, "시장", "가능성", "개발자", "상업성")) {
            return "MARKET_SIGNAL";
        }
        if (containsAny(normalizedMessage, "트렌드", "인기", "핫한", "요즘 뜨는")) {
            return "TREND_SCORE";
        }
        return "TREND_SCORE";
    }

    private String resolveAnalysisPurpose(String normalizedMessage, List<String> interactionFeatures) {
        if (isHelp(normalizedMessage)) {
            return "HELP";
        }
        if (isGreeting(normalizedMessage)) {
            return "GREETING";
        }
        if (isSmallTalk(normalizedMessage)) {
            return "SMALL_TALK";
        }
        if (isOutOfScope(normalizedMessage)) {
            return "OUT_OF_SCOPE";
        }
        if (isUserGameRecommendation(normalizedMessage)) {
            return "USER_GAME_RECOMMENDATION";
        }
        if (isGameReinterpretation(normalizedMessage)) {
            return "GAME_REINTERPRETATION";
        }
        if (!interactionFeatures.isEmpty() || containsAny(normalizedMessage, "웹캠", "tts", "stt", "음성", "카메라", "반응형")) {
            return "INTERACTION_GAME_IDEA";
        }
        if (isDeveloperMarketAnalysis(normalizedMessage)) {
            return "DEVELOPER_MARKET_ANALYSIS";
        }
        if (isStreamingFitAnalysis(normalizedMessage) && !isTrendAnalysis(normalizedMessage)) {
            return "STREAMING_FIT_ANALYSIS";
        }
        if (isTrendAnalysis(normalizedMessage)) {
            return "TREND_ANALYSIS";
        }
        return "TREND_ANALYSIS";
    }

    private List<String> resolveInteractionFeatures(String normalizedMessage, List<String> preferredFeatures) {
        Set<String> features = new LinkedHashSet<>();
        if (containsAny(normalizedMessage, "웹캠", "카메라", "얼굴", "webcam")) {
            features.add("WEBCAM");
        }
        if (containsAny(normalizedMessage, "tts", "음성 출력", "채팅 읽기")) {
            features.add("TTS");
        }
        if (containsAny(normalizedMessage, "stt", "음성 인식", "말로")) {
            features.add("STT");
        }
        if (preferredFeatures != null) {
            preferredFeatures.stream()
                    .map(this::normalize)
                    .forEach(feature -> {
                        if (containsAny(feature, "webcam", "웹캠", "카메라")) {
                            features.add("WEBCAM");
                        }
                        if (containsAny(feature, "tts")) {
                            features.add("TTS");
                        }
                        if (containsAny(feature, "stt")) {
                            features.add("STT");
                        }
                    });
        }
        return List.copyOf(features);
    }

    private boolean isAmbiguousFollowUp(String normalizedMessage) {
        if (normalizedMessage.isBlank()) {
            return false;
        }
        if (hasExplicitAnalysisPurpose(normalizedMessage)) {
            return false;
        }
        return normalizedMessage.length() <= 80
                && containsAny(
                normalizedMessage,
                "그럼",
                "그걸",
                "그거",
                "다시",
                "더 자세히",
                "왜",
                "기준",
                "알려줘",
                "분석해줘",
                "했는데",
                "한다고",
                "말했",
                "아니",
                "친구랑",
                "친구와",
                "같이",
                "함께"
        );
    }

    private boolean hasExplicitAnalysisPurpose(String normalizedMessage) {
        return isHelp(normalizedMessage)
                || isGreeting(normalizedMessage)
                || isSmallTalk(normalizedMessage)
                || isOutOfScope(normalizedMessage)
                || isUserGameRecommendation(normalizedMessage)
                || isGameReinterpretation(normalizedMessage)
                || isDeveloperMarketAnalysis(normalizedMessage)
                || containsAny(normalizedMessage, "웹캠", "tts", "stt", "음성", "카메라")
                || isExplicitStreamingFitAnalysis(normalizedMessage)
                || isTrendAnalysis(normalizedMessage);
    }

    private boolean isUserGameRecommendation(String normalizedMessage) {
        return containsAny(
                normalizedMessage,
                "나한테",
                "내가",
                "할만한 게임",
                "할 만한 게임",
                "할만 게임",
                "게임 추천",
                "추천해줘",
                "뭐가 좋을까",
                "뭐 하면 좋을까",
                "플레이할",
                "친구랑 할",
                "친구랑 한다고",
                "친구와 할",
                "같이 할",
                "함께 할",
                "혼자 할",
                "요즘 할만"
        ) || isFriendPlayRequest(normalizedMessage);
    }

    private boolean isFriendPlayRequest(String normalizedMessage) {
        return containsAny(
                normalizedMessage,
                "친구랑",
                "친구와",
                "친구들이랑",
                "같이",
                "함께",
                "둘이",
                "여럿",
                "협동",
                "멀티",
                "co-op",
                "coop"
        ) && containsAny(
                normalizedMessage,
                "게임",
                "추천",
                "할",
                "한다",
                "한다고",
                "했는데",
                "플레이",
                "즐길"
        );
    }

    private boolean isGreeting(String normalizedMessage) {
        String compact = normalizedMessage.replace(" ", "");
        return containsAny(compact, "안녕", "안녕하세요", "반가워", "반갑습니다");
    }

    private boolean isHelp(String normalizedMessage) {
        String compact = normalizedMessage.replace(" ", "");
        return containsAny(
                compact,
                "뭐할수있어",
                "뭐할수있니",
                "무엇을할수있어",
                "너는뭐야",
                "도움말",
                "사용법알려줘",
                "사용법",
                "help"
        );
    }

    private boolean isSmallTalk(String normalizedMessage) {
        String compact = normalizedMessage.replace(" ", "");
        return containsAny(compact, "뭐해", "뭐하니", "잘지내", "심심해");
    }

    private boolean isOutOfScope(String normalizedMessage) {
        if (normalizedMessage.isBlank() || isGameDomainMessage(normalizedMessage)) {
            return false;
        }
        return containsAny(
                normalizedMessage,
                "날씨",
                "점심",
                "저녁",
                "요리",
                "주식",
                "코인",
                "부동산",
                "여행",
                "영화 추천",
                "수학 문제"
        );
    }

    private boolean isGameDomainMessage(String normalizedMessage) {
        return containsAny(
                normalizedMessage,
                "게임",
                "트렌드",
                "인기",
                "방송",
                "스트리머",
                "트위치",
                "치지직",
                "soop",
                "스팀",
                "개발",
                "장르",
                "웹캠",
                "tts",
                "stt",
                "추천",
                "플레이",
                "친구랑",
                "친구와",
                "같이",
                "함께",
                "협동"
        );
    }

    private boolean isGameReinterpretation(String normalizedMessage) {
        return containsAny(
                normalizedMessage,
                "과거 게임",
                "예전 게임",
                "옛날 게임",
                "재해석",
                "다시 만들",
                "리메이크",
                "레트로",
                "지금 다시",
                "이전에 있었던 게임",
                "웹캠 tts stt로 재해석"
        );
    }

    private boolean isDeveloperMarketAnalysis(String normalizedMessage) {
        return containsAny(
                normalizedMessage,
                "개발",
                "만들고 싶은데",
                "개발하면",
                "기획",
                "장르",
                "mvp",
                "프로토타입",
                "시장성",
                "개발자 관점",
                "개발자"
        );
    }

    private boolean isStreamingFitAnalysis(String normalizedMessage) {
        return containsAny(
                normalizedMessage,
                "방송",
                "스트리머",
                "시청자 반응",
                "라이브",
                "트위치",
                "치지직",
                "soop",
                "숲"
        );
    }

    private boolean isExplicitStreamingFitAnalysis(String normalizedMessage) {
        return containsAny(
                normalizedMessage,
                "방송",
                "스트리머",
                "시청자 반응",
                "라이브"
        );
    }

    private boolean isTrendAnalysis(String normalizedMessage) {
        return containsAny(normalizedMessage, "트렌드", "인기", "핫한", "요즘 뜨는", "요즘 인기", "현재 인기");
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).strip();
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
