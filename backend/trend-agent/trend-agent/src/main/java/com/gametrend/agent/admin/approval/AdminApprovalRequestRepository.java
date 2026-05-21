package com.gametrend.agent.admin.approval;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AdminApprovalRequestRepository extends CrudRepository<AdminApprovalRequest, Long> {

    Optional<AdminApprovalRequest> findByTokenHash(String tokenHash);

    @Query("""
            SELECT *
            FROM admin_approval_requests
            WHERE user_id = :userId
              AND status = 'PENDING'
              AND used_at IS NULL
              AND token_expires_at > :now
            ORDER BY requested_at DESC
            LIMIT 1
            """)
    Optional<AdminApprovalRequest> findActivePendingByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );
}
