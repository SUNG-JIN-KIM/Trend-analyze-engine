package com.gametrend.agent.onboarding.dto;

import com.gametrend.agent.gameimage.GameImageResolver;

public record EvidenceCardResponse(
        String title,
        String type,
        String description,
        Double trendScore,
        Integer steamReviewScore,
        Integer twitchViewerCount,
        Integer twitchLiveStreamCount,
        Integer streamabilityScore,
        Integer marketSignalScore,
        String reason,
        String source,
        String genre,
        Integer totalViewerCount,
        Integer liveStreamCount,
        String signalStatus,
        String dataOrigin,
        String evidenceType,
        String category,
        String originalGenre,
        String reinterpretationConcept,
        Double reinterpretationScore,
        Integer legacyPopularityScore,
        Integer reviewSentimentScore,
        Integer mechanicUniquenessScore,
        Integer interactionFitScore,
        Integer modernTrendFitScore,
        Integer devFeasibilityScore,
        String imageUrl
) {
    public EvidenceCardResponse(
            String title,
            String type,
            String description,
            Double trendScore,
            Integer steamReviewScore,
            Integer twitchViewerCount,
            Integer twitchLiveStreamCount,
            Integer streamabilityScore,
            Integer marketSignalScore,
            String reason,
            String source,
            String genre,
            Integer totalViewerCount,
            Integer liveStreamCount,
            String signalStatus,
            String dataOrigin,
            String evidenceType,
            String category,
            String originalGenre,
            String reinterpretationConcept,
            Double reinterpretationScore,
            Integer legacyPopularityScore,
            Integer reviewSentimentScore,
            Integer mechanicUniquenessScore,
            Integer interactionFitScore,
            Integer modernTrendFitScore,
            Integer devFeasibilityScore
    ) {
        this(
                title,
                type,
                description,
                trendScore,
                steamReviewScore,
                twitchViewerCount,
                twitchLiveStreamCount,
                streamabilityScore,
                marketSignalScore,
                reason,
                source,
                genre,
                totalViewerCount,
                liveStreamCount,
                signalStatus,
                dataOrigin,
                evidenceType,
                category,
                originalGenre,
                reinterpretationConcept,
                reinterpretationScore,
                legacyPopularityScore,
                reviewSentimentScore,
                mechanicUniquenessScore,
                interactionFitScore,
                modernTrendFitScore,
                devFeasibilityScore,
                GameImageResolver.resolveImageUrl(title, null, null)
        );
    }

    public EvidenceCardResponse(
            String title,
            String type,
            String description,
            Double trendScore,
            Integer steamReviewScore,
            Integer twitchViewerCount,
            Integer twitchLiveStreamCount,
            Integer streamabilityScore,
            Integer marketSignalScore,
            String reason,
            String source,
            String genre,
            Integer totalViewerCount,
            Integer liveStreamCount,
            String signalStatus,
            String dataOrigin
    ) {
        this(
                title,
                type,
                description,
                trendScore,
                steamReviewScore,
                twitchViewerCount,
                twitchLiveStreamCount,
                streamabilityScore,
                marketSignalScore,
                reason,
                source,
                genre,
                totalViewerCount,
                liveStreamCount,
                signalStatus,
                dataOrigin,
                GameImageResolver.resolveImageUrl(title, null, null)
        );
    }

    public EvidenceCardResponse(
            String title,
            String type,
            String description,
            Double trendScore,
            Integer steamReviewScore,
            Integer twitchViewerCount,
            Integer twitchLiveStreamCount,
            Integer streamabilityScore,
            Integer marketSignalScore,
            String reason,
            String source,
            String genre,
            Integer totalViewerCount,
            Integer liveStreamCount,
            String signalStatus,
            String dataOrigin,
            String imageUrl
    ) {
        this(
                title,
                type,
                description,
                trendScore,
                steamReviewScore,
                twitchViewerCount,
                twitchLiveStreamCount,
                streamabilityScore,
                marketSignalScore,
                reason,
                source,
                genre,
                totalViewerCount,
                liveStreamCount,
                signalStatus,
                dataOrigin,
                inferEvidenceType(type),
                type,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                imageUrl == null || imageUrl.isBlank()
                        ? GameImageResolver.resolveImageUrl(title, null, null)
                        : imageUrl
        );
    }

    public EvidenceCardResponse(
            String title,
            String type,
            String description,
            Double trendScore,
            Integer steamReviewScore,
            Integer twitchViewerCount,
            Integer twitchLiveStreamCount,
            Integer streamabilityScore,
            Integer marketSignalScore,
            String reason
    ) {
        this(
                title,
                type,
                description,
                trendScore,
                steamReviewScore,
                twitchViewerCount,
                twitchLiveStreamCount,
                streamabilityScore,
                marketSignalScore,
                reason,
                GameImageResolver.resolveImageUrl(title, null, null)
        );
    }

    public EvidenceCardResponse(
            String title,
            String type,
            String description,
            Double trendScore,
            Integer steamReviewScore,
            Integer twitchViewerCount,
            Integer twitchLiveStreamCount,
            Integer streamabilityScore,
            Integer marketSignalScore,
            String reason,
            String imageUrl
    ) {
        this(
                title,
                type,
                description,
                trendScore,
                steamReviewScore,
                twitchViewerCount,
                twitchLiveStreamCount,
                streamabilityScore,
                marketSignalScore,
                reason,
                null,
                null,
                null,
                null,
                null,
                null,
                inferEvidenceType(type),
                type,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                imageUrl == null || imageUrl.isBlank()
                        ? GameImageResolver.resolveImageUrl(title, null, null)
                        : imageUrl
        );
    }

    private static String inferEvidenceType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String normalizedType = type.toUpperCase();
        if (normalizedType.contains("REINTERPRETATION")) {
            return "REINTERPRETATION";
        }
        if (normalizedType.contains("LIVE")) {
            return "LIVE_TREND";
        }
        return "TREND";
    }
}
