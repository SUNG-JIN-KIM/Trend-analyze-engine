package com.gametrend.agent.admin.audit;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table("admin_audit_logs")
public class AdminAuditLog {

    @Id
    private Long id;

    private Long adminUserId;
    private String action;
    private String targetType;
    private Long targetId;
    private String ipAddress;
    private String userAgent;
    private String detail;
    private LocalDateTime createdAt;
}
