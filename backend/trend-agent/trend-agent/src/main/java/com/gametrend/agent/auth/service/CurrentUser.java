package com.gametrend.agent.auth.service;

public record CurrentUser(
        Long id,
        String email,
        String nickname,
        String role,
        String status,
        String authType,
        String provider,
        String profileImageUrl,
        boolean emailVerified,
        String phoneNumber,
        boolean phoneVerified
) {
}
