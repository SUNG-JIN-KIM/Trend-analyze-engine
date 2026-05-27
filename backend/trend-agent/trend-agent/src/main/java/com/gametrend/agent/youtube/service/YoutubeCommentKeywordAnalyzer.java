package com.gametrend.agent.youtube.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class YoutubeCommentKeywordAnalyzer {

    private static final List<String> POSITIVE_KEYWORDS = List.of(
            "재밌", "재미", "좋", "추천", "갓겜", "최고", "기대", "멋", "awesome", "good", "great", "fun", "love", "best"
    );
    private static final List<String> NEGATIVE_KEYWORDS = List.of(
            "별로", "노잼", "망", "싫", "아쉽", "실망", "버그", "비추", "bad", "boring", "hate", "bug", "worst"
    );

    public Map<KeywordKey, KeywordStatDraft> analyze(String text) {
        Map<KeywordKey, KeywordStatDraft> result = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return result;
        }

        String normalized = text.toLowerCase(Locale.ROOT);
        addMatches(result, normalized, text, "POSITIVE", POSITIVE_KEYWORDS);
        addMatches(result, normalized, text, "NEGATIVE", NEGATIVE_KEYWORDS);
        return result;
    }

    private void addMatches(
            Map<KeywordKey, KeywordStatDraft> result,
            String normalized,
            String originalText,
            String sentiment,
            List<String> keywords
    ) {
        for (String keyword : keywords) {
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                KeywordKey key = new KeywordKey(keyword, sentiment);
                result.merge(
                        key,
                        new KeywordStatDraft(keyword, sentiment, 1, limit(originalText, 500)),
                        (left, right) -> new KeywordStatDraft(
                                left.keyword(),
                                left.sentiment(),
                                left.count() + right.count(),
                                left.sampleText()
                        )
                );
            }
        }
    }

    private String limit(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record KeywordKey(String keyword, String sentiment) {
    }

    public record KeywordStatDraft(
            String keyword,
            String sentiment,
            int count,
            String sampleText
    ) {
    }
}
