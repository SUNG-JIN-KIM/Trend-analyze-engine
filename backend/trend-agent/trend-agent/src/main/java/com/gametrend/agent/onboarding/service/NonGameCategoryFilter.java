package com.gametrend.agent.onboarding.service;

import com.gametrend.agent.livetrend.dto.LiveTrendGameResponse;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class NonGameCategoryFilter {

    private static final Set<String> NON_GAME_CATEGORIES = Set.of(
            "just chatting",
            "talk shows & podcasts",
            "music",
            "irl",
            "sports",
            "asmr",
            "slots",
            "casino",
            "casino & gambling",
            "gambling",
            "poker",
            "blackjack",
            "sports betting",
            "슬롯",
            "카지노",
            "도박"
    );

    public boolean shouldInclude(LiveTrendGameResponse game, String userMessage) {
        if (allowsNonGameCategories(userMessage)) {
            return true;
        }
        return !isNonGameCategory(game.title())
                && !isNonGameCategory(game.sourceKeyword())
                && !isNonGameCategory(game.genre());
    }

    public boolean allowsNonGameCategories(String userMessage) {
        String normalizedMessage = normalize(userMessage);
        return containsAny(
                normalizedMessage,
                "방송 카테고리",
                "잡담 카테고리",
                "비게임 카테고리",
                "just chatting",
                "talk shows",
                "podcasts",
                "irl",
                "asmr"
        );
    }

    private boolean isNonGameCategory(String value) {
        return NON_GAME_CATEGORIES.contains(normalize(value));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .strip();
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
