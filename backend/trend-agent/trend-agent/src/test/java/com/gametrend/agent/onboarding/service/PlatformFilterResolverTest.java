package com.gametrend.agent.onboarding.service;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlatformFilterResolverTest {

    private final PlatformFilterResolver resolver = new PlatformFilterResolver();

    @Test
    void resolve_detectsChzzkTwitchSoopAndSteamKeywords() {
        assertEquals(Optional.of("CHZZK"), resolver.resolve("치지직 기준으로 요즘 인기 있는 게임 알려줘"));
        assertEquals(Optional.of("TWITCH"), resolver.resolve("Twitch 기준으로 다시 분석해줘"));
        assertEquals(Optional.of("SOOP"), resolver.resolve("아프리카 기준으로 방송 반응 좋은 게임 알려줘"));
        assertEquals(Optional.of("STEAM"), resolver.resolve("스팀 기준으로 다시 봐줘"));
    }

    @Test
    void resolve_returnsEmptyWhenNoPlatformOrAllPlatformRequested() {
        assertEquals(Optional.empty(), resolver.resolve("요즘 어떤 게임이 인기 있어?"));
        assertEquals(Optional.empty(), resolver.resolve("전체 기준으로 다시 봐줘"));
    }
}
