package com.gametrend.agent.onboarding.dto;

import com.gametrend.agent.gameimage.GameImageResolver;

public record RecommendedConceptResponse(
        String title,
        String genre,
        String reason,
        int streamabilityScore,
        int marketSignalScore,
        int devFeasibilityScore,
        String imageUrl
) {
    public RecommendedConceptResponse(
            String title,
            String genre,
            String reason,
            int streamabilityScore,
            int marketSignalScore,
            int devFeasibilityScore
    ) {
        this(
                title,
                genre,
                reason,
                streamabilityScore,
                marketSignalScore,
                devFeasibilityScore,
                GameImageResolver.resolveImageUrl(title, null, null)
        );
    }
}
