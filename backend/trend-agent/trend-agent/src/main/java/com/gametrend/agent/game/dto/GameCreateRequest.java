package com.gametrend.agent.game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GameCreateRequest(
        @NotBlank @Size(max = 200)
        String title,

        @NotBlank @Size(max = 100)
        String genre,

        @NotBlank @Size(max = 100)
        String platform,

        @NotBlank @Size(max = 100)
        String playStyle,

        @Min(0) @Max(100)
        int streamabilityScore,

        @Min(0) @Max(100)
        int webcamFitScore,

        @Min(0) @Max(100)
        int ttsFitScore,

        @Min(0) @Max(100)
        int sttFitScore,

        @Min(0) @Max(100)
        int noveltyScore,

        @Min(0) @Max(100)
        int devFeasibilityScore,

        @Min(0) @Max(100)
        int marketSignalScore,

        @Size(max = 2000)
        String reason
) {
}
