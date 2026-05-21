package com.gametrend.agent.infrastructure.steam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class PublicSteamReviewClient implements SteamClient {

    private final RestClient steamRestClient;

    public PublicSteamReviewClient(@Qualifier("steamRestClient") RestClient steamRestClient) {
        this.steamRestClient = steamRestClient;
    }

    @Override
    public SteamReviewSummary getReviewSummary(int appId) {
        try {
            SteamReviewApiResponse response = steamRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/appreviews/{appId}")
                            .queryParam("json", 1)
                            .queryParam("language", "all")
                            .queryParam("purchase_type", "all")
                            .queryParam("num_per_page", 0)
                            .build(appId)
                    )
                    .retrieve()
                    .body(SteamReviewApiResponse.class);

            return toSummary(appId, response);
        } catch (RestClientException ex) {
            log.warn("Steam 리뷰 조회 실패: appId={}, message={}", appId, ex.getMessage());
            throw new SteamClientException("Steam 리뷰 데이터를 조회하지 못했습니다.", ex);
        }
    }

    private SteamReviewSummary toSummary(int appId, SteamReviewApiResponse response) {
        if (response == null || response.querySummary() == null) {
            throw new SteamClientException("Steam 리뷰 응답이 비어 있습니다.");
        }

        QuerySummary querySummary = response.querySummary();
        int totalPositive = Math.max(querySummary.totalPositive(), 0);
        int totalNegative = Math.max(querySummary.totalNegative(), 0);
        int totalReviews = Math.max(querySummary.totalReviews(), totalPositive + totalNegative);
        double positiveRate = totalPositive + totalNegative == 0
                ? 0.0
                : (double) totalPositive / (totalPositive + totalNegative);

        return new SteamReviewSummary(
                appId,
                resolveReviewScoreDesc(querySummary.reviewScoreDesc()),
                totalPositive,
                totalNegative,
                totalReviews,
                Math.round(positiveRate * 1000.0) / 1000.0
        );
    }

    private String resolveReviewScoreDesc(String reviewScoreDesc) {
        if (reviewScoreDesc == null || reviewScoreDesc.isBlank()) {
            return "No review summary";
        }
        return reviewScoreDesc;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SteamReviewApiResponse(
            @JsonProperty("query_summary")
            QuerySummary querySummary
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record QuerySummary(
            @JsonProperty("review_score_desc")
            String reviewScoreDesc,
            @JsonProperty("total_positive")
            int totalPositive,
            @JsonProperty("total_negative")
            int totalNegative,
            @JsonProperty("total_reviews")
            int totalReviews
    ) {
    }
}
