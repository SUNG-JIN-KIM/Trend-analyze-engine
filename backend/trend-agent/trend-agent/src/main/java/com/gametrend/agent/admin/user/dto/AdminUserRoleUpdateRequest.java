package com.gametrend.agent.admin.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserRoleUpdateRequest(
        @NotBlank
        @Size(max = 40)
        String role,

        @Size(max = 1000)
        String reason
) {
}
