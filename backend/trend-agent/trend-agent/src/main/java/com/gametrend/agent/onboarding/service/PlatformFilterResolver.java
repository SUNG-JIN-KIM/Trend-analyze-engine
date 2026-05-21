package com.gametrend.agent.onboarding.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
public class PlatformFilterResolver {

    public Optional<String> resolve(String message) {
        String normalizedMessage = normalize(message);
        if (normalizedMessage.isBlank() || asksForAllPlatforms(normalizedMessage)) {
            return Optional.empty();
        }
        if (containsAny(normalizedMessage, "트위치", "twitch")) {
            return Optional.of("TWITCH");
        }
        if (containsAny(normalizedMessage, "치지직", "chzzk")) {
            return Optional.of("CHZZK");
        }
        if (containsAny(normalizedMessage, "숲", "soop", "아프리카")) {
            return Optional.of("SOOP");
        }
        if (containsAny(normalizedMessage, "스팀", "steam")) {
            return Optional.of("STEAM");
        }
        return Optional.empty();
    }

    private boolean asksForAllPlatforms(String normalizedMessage) {
        return containsAny(
                normalizedMessage,
                "전체 기준",
                "전체로",
                "전체 데이터",
                "전체 라이브",
                "all platform",
                "all platforms",
                "all source",
                "all sources"
        );
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
