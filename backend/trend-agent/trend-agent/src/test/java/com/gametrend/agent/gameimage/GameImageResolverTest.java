package com.gametrend.agent.gameimage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameImageResolverTest {

    @Test
    void resolveImageUrl_usesSeedMappingFirst() {
        String imageUrl = GameImageResolver.resolveImageUrl("Counter-Strike 2", null, null);

        assertEquals("https://cdn.akamai.steamstatic.com/steam/apps/730/header.jpg", imageUrl);
    }

    @Test
    void resolveImageUrl_usesSteamAppIdWhenSeedIsMissing() {
        String imageUrl = GameImageResolver.resolveImageUrl("Unknown Steam Game", null, 12345);

        assertEquals("https://cdn.akamai.steamstatic.com/steam/apps/12345/header.jpg", imageUrl);
    }

    @Test
    void resolveImageUrl_returnsPlaceholderWhenNoSignalExists() {
        String imageUrl = GameImageResolver.resolveImageUrl("Unknown Game", null, null);

        assertTrue(imageUrl.contains("placehold.co"));
    }
}
