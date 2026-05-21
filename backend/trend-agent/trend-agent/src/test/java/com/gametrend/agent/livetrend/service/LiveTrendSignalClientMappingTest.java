package com.gametrend.agent.livetrend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.agent.livetrend.config.LiveTrendProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveTrendSignalClientMappingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void chzzkLiveResponse_mapsGameCategoryToLiveGameSignal() throws Exception {
        ChzzkLiveTrendSignalClient client = new ChzzkLiveTrendSignalClient(new LiveTrendProperties(), objectMapper);
        JsonNode response = objectMapper.readTree("""
                {
                  "content": {
                    "data": [
                      {
                        "categoryType": "GAME",
                        "liveCategoryValue": "PUBG",
                        "concurrentUserCount": 100,
                        "channelName": "alpha",
                        "liveTitle": "rank match",
                        "openDate": "2026-05-14T00:00:00Z"
                      },
                      {
                        "categoryType": "GAME",
                        "liveCategoryValue": "PUBG",
                        "concurrentUserCount": 50,
                        "channelName": "beta",
                        "liveTitle": "duo",
                        "openDate": "2026-05-14T00:10:00Z"
                      },
                      {
                        "categoryType": "SPORTS",
                        "liveCategoryValue": "Baseball",
                        "concurrentUserCount": 999
                      }
                    ]
                  }
                }
                """);

        List<LiveGameSignal> signals = client.toSignals(response);

        assertEquals(1, signals.size());
        LiveGameSignal signal = signals.get(0);
        assertEquals("CHZZK", signal.source());
        assertEquals("PUBG", signal.gameName());
        assertEquals("PUBG", signal.sourceKeyword());
        assertEquals(2, signal.liveStreamCount());
        assertEquals(150, signal.totalViewerCount());
        assertTrue(signal.topChannels().get(0).contains("channelName=alpha"));
    }

    @Test
    void twitchStreamsResponse_groupsStreamsByGameName() throws Exception {
        TwitchLiveTrendSignalClient client = new TwitchLiveTrendSignalClient(new LiveTrendProperties(), objectMapper);
        JsonNode response = objectMapper.readTree("""
                {
                  "data": [
                    {
                      "game_name": "Counter-Strike 2",
                      "viewer_count": 700,
                      "user_name": "one",
                      "title": "major",
                      "started_at": "2026-05-14T00:00:00Z"
                    },
                    {
                      "game_name": "Counter-Strike 2",
                      "viewer_count": 300,
                      "user_name": "two",
                      "title": "scrim",
                      "started_at": "2026-05-14T00:05:00Z"
                    }
                  ]
                }
                """);

        List<LiveGameSignal> signals = client.toSignals(response);

        assertEquals(1, signals.size());
        LiveGameSignal signal = signals.get(0);
        assertEquals("TWITCH", signal.source());
        assertEquals("Counter-Strike 2", signal.gameName());
        assertEquals(2, signal.liveStreamCount());
        assertEquals(1_000, signal.totalViewerCount());
        assertTrue(signal.topChannels().get(0).contains("channelName=one"));
    }
}
