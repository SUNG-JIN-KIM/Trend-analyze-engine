package com.gametrend.agent.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ConfigurationProperties(prefix = "sms")
public record SmsProperties(
        String provider,
        String apiKey,
        String apiSecret,
        String serviceId,
        String fromNumber,
        String baseUrl
) {

    public SmsProperties {
        provider = normalize(provider, "mock");
        apiKey = blankToNull(apiKey);
        apiSecret = blankToNull(apiSecret);
        serviceId = blankToNull(serviceId);
        fromNumber = blankToNull(fromNumber);
        baseUrl = normalizeBaseUrl(baseUrl);
    }

    public boolean isMockProvider() {
        return "mock".equals(provider) || "local".equals(provider) || "dev".equals(provider);
    }

    public boolean isNaverSensProvider() {
        return "naver-sens".equals(provider) || "sens".equals(provider) || "naver".equals(provider);
    }

    public List<String> missingNaverSensSettings() {
        List<String> missing = new ArrayList<>();
        if (apiKey == null || apiSecret == null || serviceId == null || fromNumber == null) {
            if (apiKey == null) {
                missing.add("SMS_API_KEY");
            }
            if (apiSecret == null) {
                missing.add("SMS_API_SECRET");
            }
            if (serviceId == null) {
                missing.add("SMS_SERVICE_ID");
            }
            if (fromNumber == null) {
                missing.add("SMS_FROM_NUMBER");
            }
        }
        return missing;
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return "https://sens.apigw.ntruss.com";
        }
        return normalized.replaceAll("/+$", "");
    }
}
