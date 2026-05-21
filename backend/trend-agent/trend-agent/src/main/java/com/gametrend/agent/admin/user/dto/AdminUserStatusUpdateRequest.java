package com.gametrend.agent.admin.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserStatusUpdateRequest(
        @NotBlank
        @Size(max = 40)
        String status,

        @Size(max = 1000)
        String reason
) {
}
