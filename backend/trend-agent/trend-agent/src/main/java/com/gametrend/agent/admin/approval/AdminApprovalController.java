package com.gametrend.agent.admin.approval;

import com.gametrend.agent.admin.approval.dto.AdminApprovalDecisionResponse;
import com.gametrend.agent.admin.approval.dto.AdminApprovalRequestCreateRequest;
import com.gametrend.agent.admin.approval.dto.AdminApprovalRequestResponse;
import com.gametrend.agent.auth.exception.AuthRequiredException;
import com.gametrend.agent.auth.service.CurrentUser;
import com.gametrend.agent.auth.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminApprovalController {

    private final AdminApprovalService adminApprovalService;
    private final CurrentUserService currentUserService;

    @PostMapping("/api/admin-approval/request")
    public AdminApprovalRequestResponse requestApproval(
            @Valid @RequestBody AdminApprovalRequestCreateRequest request
    ) {
        CurrentUser currentUser = currentUserService.getCurrentUser()
                .orElseThrow(() -> new AuthRequiredException("관리자 권한 신청은 로그인 후 사용할 수 있습니다."));
        return adminApprovalService.requestApproval(currentUser.id(), request.reason());
    }

    @GetMapping("/admin/approval/approve")
    public AdminApprovalDecisionResponse approve(@RequestParam String token) {
        return adminApprovalService.approve(token);
    }

    @GetMapping("/admin/approval/reject")
    public AdminApprovalDecisionResponse reject(
            @RequestParam String token,
            @RequestParam(required = false) String reason
    ) {
        return adminApprovalService.reject(token, reason);
    }
}
