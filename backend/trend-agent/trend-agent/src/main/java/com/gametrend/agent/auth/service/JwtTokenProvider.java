package com.gametrend.agent.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.agent.auth.config.AuthProperties;
import com.gametrend.agent.auth.entity.UserAccount;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class JwtTokenProvider {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper;
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder decoder = Base64.getUrlDecoder();

    public JwtTokenProvider(AuthProperties authProperties, ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
    }

    public String createAccessToken(UserAccount user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(authProperties.jwtExpirationMs());
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getId());
        payload.put("email", user.getEmail());
        payload.put("nickname", user.getNickname());
        payload.put("role", user.getRole().name());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String signingInput = encodeJson(header) + "." + encodeJson(payload);
        return signingInput + "." + sign(signingInput);
    }

    public Optional<JwtClaims> parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        String signingInput = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(signingInput), parts[2])) {
            return Optional.empty();
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(
                    new String(decoder.decode(parts[1]), StandardCharsets.UTF_8),
                    MAP_TYPE
            );
            long expiresAt = toLong(payload.get("exp"));
            if (expiresAt <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(new JwtClaims(
                    toLong(payload.get("sub")),
                    stringValue(payload.get("email")),
                    stringValue(payload.get("nickname")),
                    stringValue(payload.get("role"))
            ));
        } catch (IllegalArgumentException | JsonProcessingException ex) {
            return Optional.empty();
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return encoder.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JWT payload 직렬화에 실패했습니다.", ex);
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(authProperties.jwtSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return encoder.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("JWT 서명 생성에 실패했습니다.", ex);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        throw new IllegalArgumentException("JWT claim number conversion failed.");
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public record JwtClaims(
            long userId,
            String email,
            String nickname,
            String role
    ) {
    }
}
