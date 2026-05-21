package com.gametrend.agent.report.dto;

import java.time.LocalDateTime;

public record ReportResponse(
        String title,
        String draft,
        LocalDateTime generatedAt
) {
}
