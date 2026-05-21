package com.gametrend.agent.admin.audit;

import com.gametrend.agent.admin.audit.dto.AdminAuditLogResponse;
import com.gametrend.agent.admin.common.AdminPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private final AdminAuditLogRepository adminAuditLogRepository;

    public AdminPageResponse<AdminAuditLogResponse> searchLogs(
            String action,
            String targetType,
            String search,
            int page,
            int size
    ) {
        var logs = StreamSupport.stream(adminAuditLogRepository.findAll().spliterator(), false)
                .filter(log -> matches(action, log.getAction()))
                .filter(log -> matches(targetType, log.getTargetType()))
                .filter(log -> matchesSearch(log, search))
                .sorted(Comparator.comparing(AdminAuditLog::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .map(AdminAuditLogResponse::from)
                .toList();

        return AdminPageResponse.of(logs, page, size);
    }

    private boolean matches(String expected, String actual) {
        String value = normalize(expected);
        return value == null || "ALL".equalsIgnoreCase(value) || (actual != null && actual.equalsIgnoreCase(value));
    }

    private boolean matchesSearch(AdminAuditLog log, String search) {
        String value = normalize(search);
        if (value == null) {
            return true;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return contains(log.getAction(), lower)
                || contains(log.getTargetType(), lower)
                || contains(log.getDetail(), lower)
                || (log.getTargetId() != null && String.valueOf(log.getTargetId()).equals(value))
                || (log.getAdminUserId() != null && String.valueOf(log.getAdminUserId()).equals(value));
    }

    private boolean contains(String actual, String lower) {
        return actual != null && actual.toLowerCase(Locale.ROOT).contains(lower);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
