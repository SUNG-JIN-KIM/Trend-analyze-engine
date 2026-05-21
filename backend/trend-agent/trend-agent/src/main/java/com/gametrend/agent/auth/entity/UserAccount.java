package com.gametrend.agent.auth.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table("users")
public class UserAccount {

    @Id
    private Long id;

    private String email;
    private String passwordHash;
    private String nickname;
    private UserRole role;
    private String phoneNumber;
    private boolean phoneVerified;
    private UserStatus status;
    private String provider;
    private String providerId;
    private String profileImageUrl;
    private boolean emailVerified;
    private AuthType authType;
    private LocalDateTime lastLoginAt;
    private int failedLoginCount;
    private LocalDateTime lockedUntil;
    private LocalDateTime chatRestrictedUntil;
    private String chatRestrictionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
