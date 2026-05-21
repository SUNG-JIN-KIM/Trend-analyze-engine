package com.gametrend.agent.admin.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;

    public void log(
            Long adminUserId,
            String action,
            String targetType,
            Long targetId,
            String detail
    ) {
        adminAuditLogRepository.save(AdminAuditLog.builder()
                .adminUserId(adminUserId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .detail(detail)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
