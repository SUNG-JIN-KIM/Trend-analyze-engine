package com.gametrend.agent.admin.approval.dto;

import jakarta.validation.constraints.Size;

public record AdminApprovalRequestCreateRequest(
        @Size(max = 1000)
        String reason
) {
}
