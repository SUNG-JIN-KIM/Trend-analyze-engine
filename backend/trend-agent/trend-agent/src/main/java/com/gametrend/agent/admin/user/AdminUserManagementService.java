package com.gametrend.agent.admin.user;

import com.gametrend.agent.admin.audit.AdminAuditService;
import com.gametrend.agent.admin.common.AdminManagementException;
import com.gametrend.agent.admin.common.AdminPageResponse;
import com.gametrend.agent.admin.user.dto.AdminUserResponse;
import com.gametrend.agent.auth.entity.UserAccount;
import com.gametrend.agent.auth.entity.UserRole;
import com.gametrend.agent.auth.entity.UserStatus;
import com.gametrend.agent.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class AdminUserManagementService {

    private final UserRepository userRepository;
    private final AdminAuditService adminAuditService;

    public AdminPageResponse<AdminUserResponse> searchUsers(
            String email,
            String nickname,
            String phoneNumber,
            String role,
            String status,
            int page,
            int size,
            String sort
    ) {
        var users = StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .filter(user -> containsIgnoreCase(user.getEmail(), email))
                .filter(user -> containsIgnoreCase(user.getNickname(), nickname))
                .filter(user -> containsIgnoreCase(user.getPhoneNumber(), phoneNumber))
                .filter(user -> matchesEnum(user.getRole(), role))
                .filter(user -> matchesEnum(user.getStatus() == null ? UserStatus.ACTIVE : user.getStatus(), status))
                .sorted(userComparator(sort))
                .map(AdminUserResponse::from)
                .toList();

        return AdminPageResponse.of(users, page, size);
    }

    public AdminUserResponse getUser(Long userId) {
        return AdminUserResponse.from(loadUser(userId));
    }

    @Transactional
    public AdminUserResponse updateStatus(
            Long actorUserId,
            Long targetUserId,
            String status,
            String reason
    ) {
        UserAccount actor = loadUser(actorUserId);
        UserAccount target = loadUser(targetUserId);
        if (target.getRole() == UserRole.OWNER && actor.getRole() != UserRole.OWNER) {
            throw AdminManagementException.forbidden("OWNER 계정 상태는 OWNER만 변경할 수 있습니다.");
        }

        UserStatus nextStatus = parseStatus(status);
        UserAccount saved = userRepository.save(target.toBuilder()
                .status(nextStatus)
                .updatedAt(LocalDateTime.now())
                .build());

        adminAuditService.log(
                actor.getId(),
                nextStatus == UserStatus.SUSPENDED ? "USER_SUSPENDED" : "USER_STATUS_CHANGED",
                "USER",
                target.getId(),
                "status=%s,reason=%s".formatted(nextStatus.name(), normalize(reason))
        );

        return AdminUserResponse.from(saved);
    }

    @Transactional
    public AdminUserResponse updateRole(
            Long actorUserId,
            Long targetUserId,
            String role,
            String reason
    ) {
        UserAccount actor = loadUser(actorUserId);
        UserAccount target = loadUser(targetUserId);
        UserRole nextRole = parseRole(role);

        if (actor.getRole() != UserRole.OWNER) {
            throw AdminManagementException.forbidden("권한 변경은 OWNER만 수행할 수 있습니다.");
        }
        if (target.getRole() == UserRole.OWNER && !target.getId().equals(actor.getId())) {
            throw AdminManagementException.forbidden("다른 OWNER 계정의 권한은 변경할 수 없습니다.");
        }
        if (target.getId().equals(actor.getId()) && target.getRole() != nextRole) {
            throw AdminManagementException.forbidden("자기 자신의 권한은 변경할 수 없습니다.");
        }

        UserAccount saved = userRepository.save(target.toBuilder()
                .role(nextRole)
                .updatedAt(LocalDateTime.now())
                .build());

        adminAuditService.log(
                actor.getId(),
                "USER_ROLE_CHANGED",
                "USER",
                target.getId(),
                "from=%s,to=%s,reason=%s".formatted(target.getRole(), nextRole, normalize(reason))
        );

        return AdminUserResponse.from(saved);
    }

    private UserAccount loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> AdminManagementException.notFound("사용자"));
    }

    private boolean containsIgnoreCase(String actual, String expected) {
        String expectedValue = normalize(expected);
        if (expectedValue == null) {
            return true;
        }
        return actual != null && actual.toLowerCase(Locale.ROOT).contains(expectedValue.toLowerCase(Locale.ROOT));
    }

    private boolean matchesEnum(Enum<?> actual, String expected) {
        String expectedValue = normalize(expected);
        if (expectedValue == null || "ALL".equalsIgnoreCase(expectedValue)) {
            return true;
        }
        return actual != null && actual.name().equalsIgnoreCase(expectedValue);
    }

    private Comparator<UserAccount> userComparator(String sort) {
        String value = normalize(sort);
        Comparator<UserAccount> byIdDesc = Comparator.comparing(UserAccount::getId, Comparator.nullsLast(Long::compareTo)).reversed();
        if ("email".equalsIgnoreCase(value)) {
            return Comparator.comparing(UserAccount::getEmail, Comparator.nullsLast(String::compareToIgnoreCase));
        }
        if ("lastLoginAt".equalsIgnoreCase(value)) {
            return Comparator.comparing(UserAccount::getLastLoginAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed();
        }
        return Comparator.comparing(UserAccount::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed()
                .thenComparing(byIdDesc);
    }

    private UserStatus parseStatus(String status) {
        try {
            return UserStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw AdminManagementException.invalidRequest("지원하지 않는 사용자 상태입니다.");
        }
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role.toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw AdminManagementException.invalidRequest("지원하지 않는 사용자 권한입니다.");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
