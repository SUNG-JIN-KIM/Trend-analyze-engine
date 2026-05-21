package com.gametrend.agent.auth.phone.dto;

import java.time.LocalDateTime;

public record PhoneVerificationResponse(
        String phoneNumber,
        boolean verified,
        LocalDateTime expiresAt,
        LocalDateTime resendAvailableAt,
        String message
) {
}
