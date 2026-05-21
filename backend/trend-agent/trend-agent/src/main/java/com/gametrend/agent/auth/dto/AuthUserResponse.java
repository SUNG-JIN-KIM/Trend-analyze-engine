package com.gametrend.agent.auth.dto;

import com.gametrend.agent.auth.entity.UserAccount;
import com.gametrend.agent.auth.service.CurrentUser;

public record AuthUserResponse(
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

    public static AuthUserResponse from(UserAccount user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name(),
                user.getStatus() == null ? "ACTIVE" : user.getStatus().name(),
                user.getAuthType() == null ? "LOCAL" : user.getAuthType().name(),
                user.getProvider(),
                user.getProfileImageUrl(),
                user.isEmailVerified(),
                user.getPhoneNumber(),
                user.isPhoneVerified()
        );
    }

    public static AuthUserResponse from(CurrentUser user) {
        return new AuthUserResponse(
                user.id(),
                user.email(),
                user.nickname(),
                user.role(),
                user.status(),
                user.authType(),
                user.provider(),
                user.profileImageUrl(),
                user.emailVerified(),
                user.phoneNumber(),
                user.phoneVerified()
        );
    }
}
