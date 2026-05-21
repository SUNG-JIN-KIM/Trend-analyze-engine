package com.gametrend.agent.auth.phone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PhoneCodeSendRequest(
        @NotBlank
        @Size(max = 30)
        String phoneNumber
) {
}
