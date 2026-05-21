package com.gametrend.agent.auth.phone;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PhoneVerificationRepository extends CrudRepository<PhoneVerification, Long> {

    @Query("""
            SELECT *
            FROM phone_verifications
            WHERE phone_number = :phoneNumber
              AND purpose = :purpose
            ORDER BY created_at DESC
            LIMIT 1
            """)
    Optional<PhoneVerification> findLatestByPhoneNumberAndPurpose(
            @Param("phoneNumber") String phoneNumber,
            @Param("purpose") PhoneVerificationPurpose purpose
    );

    @Query("""
            SELECT COALESCE(SUM(resend_count + 1), 0)
            FROM phone_verifications
            WHERE phone_number = :phoneNumber
              AND purpose = :purpose
              AND created_at >= :since
            """)
    long countSendAttemptsSince(
            @Param("phoneNumber") String phoneNumber,
            @Param("purpose") PhoneVerificationPurpose purpose,
            @Param("since") LocalDateTime since
    );
}
