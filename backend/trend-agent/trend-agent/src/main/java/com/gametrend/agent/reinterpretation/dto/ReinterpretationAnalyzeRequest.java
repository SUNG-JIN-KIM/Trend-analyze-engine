package com.gametrend.agent.reinterpretation.dto;

import java.util.List;

public record ReinterpretationAnalyzeRequest(
        String message,
        String targetPlatform,
        List<String> preferredInteractionFeatures,
        Integer limit
) {
}
