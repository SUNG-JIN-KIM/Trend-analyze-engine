package com.gametrend.agent.steam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SteamImportRequest(
        @NotNull @Positive
        Integer appId,

        @NotBlank @Size(max = 200)
        String title,

        @NotBlank @Size(max = 100)
        String genre,

        @NotBlank @Size(max = 100)
        String platform,

        @NotBlank @Size(max = 100)
        String playStyle,

        @Min(0) @Max(100)
        Integer streamabilityScore,

        @Min(0) @Max(100)
        Integer webcamFitScore,

        @Min(0) @Max(100)
        Integer ttsFitScore,

        @Min(0) @Max(100)
        Integer sttFitScore,

        @Min(0) @Max(100)
        Integer noveltyScore,

        @Min(0) @Max(100)
        Integer devFeasibilityScore,

        @Size(max = 1500)
        String reason
) {
}
