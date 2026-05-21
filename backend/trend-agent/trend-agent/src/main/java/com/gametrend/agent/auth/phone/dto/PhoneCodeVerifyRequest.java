package com.gametrend.agent.auth.phone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PhoneCodeVerifyRequest(
        @NotBlank
        @Size(max = 30)
        String phoneNumber,

        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "인증번호는 6자리 숫자여야 합니다.")
        String code
) {
}
