package com.gametrend.agent.admin.audit.dto;

import com.gametrend.agent.admin.audit.AdminAuditLog;

import java.time.LocalDateTime;

public record AdminAuditLogResponse(
        Long id,
        Long adminUserId,
        String action,
        String targetType,
        Long targetId,
        String ipAddress,
        String userAgent,
        String detail,
        LocalDateTime createdAt
) {

    public static AdminAuditLogResponse from(AdminAuditLog log) {
        return new AdminAuditLogResponse(
                log.getId(),
                log.getAdminUserId(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getDetail(),
                log.getCreatedAt()
        );
    }
}
