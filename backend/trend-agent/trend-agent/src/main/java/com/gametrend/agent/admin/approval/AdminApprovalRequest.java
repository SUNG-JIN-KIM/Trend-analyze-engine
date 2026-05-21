package com.gametrend.agent.admin.approval;

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
@Table("admin_approval_requests")
public class AdminApprovalRequest {

    @Id
    private Long id;

    private Long userId;
    private String requesterEmail;
    private String requesterNickname;
    private String requesterPhoneNumber;
    private AdminApprovalStatus status;
    private String tokenHash;
    private LocalDateTime tokenExpiresAt;
    private String approvalEmailSentTo;
    private String requestReason;
    private String decisionReason;
    private Long approvedByUserId;
    private Long rejectedByUserId;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
