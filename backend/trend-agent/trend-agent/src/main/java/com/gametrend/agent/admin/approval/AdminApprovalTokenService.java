package com.gametrend.agent.admin.approval;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AdminApprovalTokenService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder tokenEncoder = Base64.getUrlEncoder().withoutPadding();

    public String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return tokenEncoder.encodeToString(bytes);
    }

    public String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw AdminApprovalException.invalidToken();
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return tokenEncoder.encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("관리자 승인 토큰 해시 생성에 실패했습니다.", ex);
        }
    }
}
