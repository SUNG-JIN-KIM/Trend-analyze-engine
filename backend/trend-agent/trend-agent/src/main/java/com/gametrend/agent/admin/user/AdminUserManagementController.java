package com.gametrend.agent.admin.user;

import com.gametrend.agent.admin.common.AdminPageResponse;
import com.gametrend.agent.admin.user.dto.AdminUserResponse;
import com.gametrend.agent.admin.user.dto.AdminUserRoleUpdateRequest;
import com.gametrend.agent.admin.user.dto.AdminUserStatusUpdateRequest;
import com.gametrend.agent.auth.exception.AuthRequiredException;
import com.gametrend.agent.auth.service.CurrentUser;
import com.gametrend.agent.auth.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserManagementController {

    private final AdminUserManagementService adminUserManagementService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public AdminPageResponse<AdminUserResponse> users(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort
    ) {
        return adminUserManagementService.searchUsers(email, nickname, phoneNumber, role, status, page, size, sort);
    }

    @GetMapping("/{userId}")
    public AdminUserResponse user(@PathVariable Long userId) {
        return adminUserManagementService.getUser(userId);
    }

    @PatchMapping("/{userId}/status")
    public AdminUserResponse updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusUpdateRequest request
    ) {
        CurrentUser currentUser = currentUser();
        return adminUserManagementService.updateStatus(currentUser.id(), userId, request.status(), request.reason());
    }

    @PatchMapping("/{userId}/role")
    public AdminUserResponse updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserRoleUpdateRequest request
    ) {
        CurrentUser currentUser = currentUser();
        return adminUserManagementService.updateRole(currentUser.id(), userId, request.role(), request.reason());
    }

    private CurrentUser currentUser() {
        return currentUserService.getCurrentUser()
                .orElseThrow(() -> new AuthRequiredException("관리자 로그인이 필요합니다."));
    }
}
