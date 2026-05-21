package com.gametrend.agent.livetrend.service;

import java.util.List;

public record LiveGameSignal(
        String source,
        String gameName,
        String sourceKeyword,
        int liveStreamCount,
        int totalViewerCount,
        List<String> topChannels,
        String rawMetadata
) {
}
