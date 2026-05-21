package com.gametrend.agent.admin.audit;

import org.springframework.data.repository.CrudRepository;

public interface AdminAuditLogRepository extends CrudRepository<AdminAuditLog, Long> {
}
