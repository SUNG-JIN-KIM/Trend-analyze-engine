package com.gametrend.agent.report.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ReportRequest(
        @Min(1) @Max(20)
        Integer recommendationLimit
) {
}
