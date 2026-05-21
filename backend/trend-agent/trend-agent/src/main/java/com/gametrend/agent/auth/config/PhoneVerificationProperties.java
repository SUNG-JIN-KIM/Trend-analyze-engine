package com.gametrend.agent.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.phone")
public record PhoneVerificationProperties(
        long codeExpirationMinutes,
        long resendCooldownSeconds,
        int maxAttempts,
        int maxResendCount,
        int dailyRequestLimit
) {

    public PhoneVerificationProperties {
        if (codeExpirationMinutes <= 0) {
            codeExpirationMinutes = 5;
        }
        if (resendCooldownSeconds <= 0) {
            resendCooldownSeconds = 60;
        }
        if (maxAttempts <= 0) {
            maxAttempts = 5;
        }
        if (maxResendCount <= 0) {
            maxResendCount = 5;
        }
        if (dailyRequestLimit <= 0) {
            dailyRequestLimit = 5;
        }
    }
}
