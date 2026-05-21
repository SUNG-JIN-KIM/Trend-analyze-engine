package com.gametrend.agent.admin.approval;

import com.gametrend.agent.admin.approval.dto.AdminApprovalAdminResponse;
import com.gametrend.agent.admin.approval.dto.AdminApprovalDecisionRequest;
import com.gametrend.agent.admin.common.AdminPageResponse;
import com.gametrend.agent.auth.exception.AuthRequiredException;
import com.gametrend.agent.auth.service.CurrentUser;
import com.gametrend.agent.auth.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/approval-requests")
@RequiredArgsConstructor
public class AdminApprovalManagementController {

    private final AdminApprovalManagementService adminApprovalManagementService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public AdminPageResponse<AdminApprovalAdminResponse> requests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "requestedAt") String sort
    ) {
        return adminApprovalManagementService.searchRequests(status, search, page, size, sort);
    }

    @PostMapping("/{requestId}/approve")
    public AdminApprovalAdminResponse approve(@PathVariable Long requestId) {
        CurrentUser currentUser = currentUser();
        return adminApprovalManagementService.approve(currentUser.id(), requestId);
    }

    @PostMapping("/{requestId}/reject")
    public AdminApprovalAdminResponse reject(
            @PathVariable Long requestId,
            @Valid @RequestBody(required = false) AdminApprovalDecisionRequest request
    ) {
        CurrentUser currentUser = currentUser();
        return adminApprovalManagementService.reject(
                currentUser.id(),
                requestId,
                request == null ? null : request.reason()
        );
    }

    private CurrentUser currentUser() {
        return currentUserService.getCurrentUser()
                .orElseThrow(() -> new AuthRequiredException("관리자 로그인이 필요합니다."));
    }
}
