package com.gametrend.agent.admin.user.dto;

import com.gametrend.agent.auth.entity.UserAccount;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        String nickname,
        String phoneNumber,
        boolean phoneVerified,
        String role,
        String status,
        String authType,
        LocalDateTime lastLoginAt,
        int failedLoginCount,
        LocalDateTime lockedUntil,
        LocalDateTime chatRestrictedUntil,
        String chatRestrictionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AdminUserResponse from(UserAccount user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getPhoneNumber(),
                user.isPhoneVerified(),
                user.getRole() == null ? null : user.getRole().name(),
                user.getStatus() == null ? "ACTIVE" : user.getStatus().name(),
                user.getAuthType() == null ? "LOCAL" : user.getAuthType().name(),
                user.getLastLoginAt(),
                user.getFailedLoginCount(),
                user.getLockedUntil(),
                user.getChatRestrictedUntil(),
                user.getChatRestrictionReason(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
