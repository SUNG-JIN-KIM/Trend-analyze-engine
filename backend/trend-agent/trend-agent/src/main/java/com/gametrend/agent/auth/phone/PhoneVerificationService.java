package com.gametrend.agent.auth.phone;

import com.gametrend.agent.auth.config.PhoneVerificationProperties;
import com.gametrend.agent.auth.phone.dto.PhoneVerificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private static final PhoneVerificationPurpose REGISTER_PURPOSE = PhoneVerificationPurpose.REGISTER;

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhoneVerificationProperties properties;
    private final SmsSender smsSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public PhoneVerificationResponse sendRegisterCode(String phoneNumber) {
        String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);
        LocalDateTime now = LocalDateTime.now();
        enforceDailySendLimit(normalizedPhoneNumber, now);

        PhoneVerification verification = phoneVerificationRepository
                .findLatestByPhoneNumberAndPurpose(normalizedPhoneNumber, REGISTER_PURPOSE)
                .map(latest -> prepareResendOrExpire(latest, now))
                .orElseGet(() -> createNewVerification(normalizedPhoneNumber, now));

        String code = generateCode();
        PhoneVerification saved = phoneVerificationRepository.save(verification.toBuilder()
                .codeHash(passwordEncoder.encode(code))
                .status(PhoneVerificationStatus.PENDING)
                .attemptCount(0)
                .lastSentAt(now)
                .expiresAt(now.plusMinutes(properties.codeExpirationMinutes()))
                .verifiedAt(null)
                .updatedAt(now)
                .build());

        smsSender.sendVerificationCode(normalizedPhoneNumber, code);

        return new PhoneVerificationResponse(
                normalizedPhoneNumber,
                false,
                saved.getExpiresAt(),
                saved.getLastSentAt().plusSeconds(properties.resendCooldownSeconds()),
                "인증번호를 발송했습니다."
        );
    }

    @Transactional
    public PhoneVerificationResponse verifyRegisterCode(String phoneNumber, String code) {
        String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);
        PhoneVerification saved = verifyCode(normalizedPhoneNumber, code, LocalDateTime.now());
        return new PhoneVerificationResponse(
                normalizedPhoneNumber,
                true,
                saved.getExpiresAt(),
                saved.getLastSentAt().plusSeconds(properties.resendCooldownSeconds()),
                "전화번호 인증이 완료되었습니다."
        );
    }

    @Transactional
    public String verifyRegistrationForSignup(String phoneNumber, String code) {
        String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);
        verifyCode(normalizedPhoneNumber, code, LocalDateTime.now());
        return normalizedPhoneNumber;
    }

    private PhoneVerification verifyCode(String phoneNumber, String code, LocalDateTime now) {
        PhoneVerification verification = phoneVerificationRepository
                .findLatestByPhoneNumberAndPurpose(phoneNumber, REGISTER_PURPOSE)
                .orElseThrow(PhoneVerificationException::verificationNotFound);

        if (verification.getExpiresAt().isBefore(now)) {
            expireVerification(verification, now);
            throw PhoneVerificationException.expired();
        }

        if (verification.getStatus() == PhoneVerificationStatus.EXPIRED) {
            throw PhoneVerificationException.expired();
        }

        if (verification.getStatus() == PhoneVerificationStatus.VERIFIED) {
            if (!passwordEncoder.matches(code, verification.getCodeHash())) {
                throw PhoneVerificationException.invalidCode();
            }
            return verification;
        }

        if (verification.getAttemptCount() >= properties.maxAttempts()) {
            throw PhoneVerificationException.maxAttemptsExceeded();
        }

        if (!passwordEncoder.matches(code, verification.getCodeHash())) {
            phoneVerificationRepository.save(verification.toBuilder()
                    .attemptCount(verification.getAttemptCount() + 1)
                    .updatedAt(now)
                    .build());
            throw PhoneVerificationException.invalidCode();
        }

        return phoneVerificationRepository.save(verification.toBuilder()
                .status(PhoneVerificationStatus.VERIFIED)
                .verifiedAt(now)
                .updatedAt(now)
                .build());
    }

    private PhoneVerification prepareResendOrExpire(PhoneVerification latest, LocalDateTime now) {
        if (latest.getStatus() != PhoneVerificationStatus.PENDING) {
            return createNewVerification(latest.getPhoneNumber(), now);
        }
        if (latest.getExpiresAt().isBefore(now)) {
            expireVerification(latest, now);
            return createNewVerification(latest.getPhoneNumber(), now);
        }

        LocalDateTime resendAvailableAt = latest.getLastSentAt().plusSeconds(properties.resendCooldownSeconds());
        if (resendAvailableAt.isAfter(now)) {
            long waitSeconds = Duration.between(now, resendAvailableAt).toSeconds();
            throw PhoneVerificationException.resendTooSoon(waitSeconds);
        }
        if (latest.getResendCount() >= properties.maxResendCount()) {
            throw PhoneVerificationException.maxResendExceeded();
        }

        return latest.toBuilder()
                .resendCount(latest.getResendCount() + 1)
                .build();
    }

    private void expireVerification(PhoneVerification verification, LocalDateTime now) {
        phoneVerificationRepository.save(verification.toBuilder()
                .status(PhoneVerificationStatus.EXPIRED)
                .updatedAt(now)
                .build());
    }

    private PhoneVerification createNewVerification(String phoneNumber, LocalDateTime now) {
        return PhoneVerification.builder()
                .phoneNumber(phoneNumber)
                .purpose(REGISTER_PURPOSE)
                .status(PhoneVerificationStatus.PENDING)
                .attemptCount(0)
                .resendCount(0)
                .lastSentAt(now)
                .expiresAt(now.plusMinutes(properties.codeExpirationMinutes()))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void enforceDailySendLimit(String phoneNumber, LocalDateTime now) {
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        long sendAttempts = phoneVerificationRepository.countSendAttemptsSince(
                phoneNumber,
                REGISTER_PURPOSE,
                todayStart
        );
        if (sendAttempts >= properties.dailyRequestLimit()) {
            throw PhoneVerificationException.dailyRequestLimitExceeded();
        }
    }

    private String generateCode() {
        return "%06d".formatted(secureRandom.nextInt(1_000_000));
    }

    public String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            throw PhoneVerificationException.invalidPhoneNumber();
        }
        String normalized = phoneNumber.strip()
                .replaceAll("[\\s\\-().]", "");
        if (!normalized.matches("^\\+?\\d{9,15}$")) {
            throw PhoneVerificationException.invalidPhoneNumber();
        }
        return normalized;
    }
}
