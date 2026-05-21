package com.gametrend.agent.auth.phone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.agent.auth.config.SmsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class NaverSensSmsSender implements SmsSender {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SmsProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public void sendVerificationCode(String phoneNumber, String code) {
        List<String> missingSettings = properties.missingNaverSensSettings();
        if (!missingSettings.isEmpty()) {
            log.warn("Naver SENS SMS 설정 누락. missing={}", missingSettings);
            throw PhoneVerificationException.smsConfigMissing();
        }

        String path = "/sms/v2/services/%s/messages".formatted(properties.serviceId());
        String timestamp = String.valueOf(System.currentTimeMillis());
        String body = buildRequestBody(phoneNumber, code);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.baseUrl() + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("x-ncp-apigw-timestamp", timestamp)
                .header("x-ncp-iam-access-key", properties.apiKey())
                .header("x-ncp-apigw-signature-v2", signature("POST", path, timestamp))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Naver SENS SMS 발송 실패. statusCode={}, body={}", response.statusCode(), response.body());
                throw PhoneVerificationException.smsSendFailed();
            }
        } catch (IOException ex) {
            log.warn("Naver SENS SMS 발송 중 I/O 오류가 발생했습니다.", ex);
            throw PhoneVerificationException.smsSendFailed();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw PhoneVerificationException.smsSendFailed();
        }
    }

    private String buildRequestBody(String phoneNumber, String code) {
        Map<String, Object> payload = Map.of(
                "type", "SMS",
                "contentType", "COMM",
                "countryCode", "82",
                "from", normalizeKoreanPhoneNumber(properties.fromNumber()),
                "content", "[Game Trend] 인증번호는 %s입니다. 제한 시간 안에 입력해주세요.".formatted(code),
                "messages", List.of(Map.of("to", normalizeKoreanPhoneNumber(phoneNumber)))
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("SMS 요청 본문 생성에 실패했습니다.", ex);
        }
    }

    private String signature(String method, String path, String timestamp) {
        try {
            String message = method + " " + path + "\n" + timestamp + "\n" + properties.apiKey();
            SecretKeySpec signingKey = new SecretKeySpec(
                    properties.apiSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(signingKey);
            return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Naver SENS SMS 서명 생성에 실패했습니다.", ex);
        }
    }

    private String normalizeKoreanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw PhoneVerificationException.invalidPhoneNumber();
        }
        String digits = phoneNumber.strip().replaceAll("[^0-9]", "");
        if (digits.startsWith("82") && digits.length() > 10) {
            digits = "0" + digits.substring(2);
        }
        if (!digits.matches("^0\\d{8,10}$")) {
            throw PhoneVerificationException.invalidPhoneNumber();
        }
        return digits;
    }
}
