package com.gametrend.agent.auth.phone;

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
@Table("phone_verifications")
public class PhoneVerification {

    @Id
    private Long id;

    private String phoneNumber;
    private String codeHash;
    private PhoneVerificationPurpose purpose;
    private PhoneVerificationStatus status;
    private int attemptCount;
    private int resendCount;
    private LocalDateTime lastSentAt;
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
