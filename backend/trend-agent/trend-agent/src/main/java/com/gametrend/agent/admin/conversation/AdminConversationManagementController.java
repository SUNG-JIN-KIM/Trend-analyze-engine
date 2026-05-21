package com.gametrend.agent.admin.conversation;

import com.gametrend.agent.admin.common.AdminPageResponse;
import com.gametrend.agent.admin.conversation.dto.AdminConversationModerationRequest;
import com.gametrend.agent.admin.conversation.dto.AdminConversationResponse;
import com.gametrend.agent.auth.exception.AuthRequiredException;
import com.gametrend.agent.auth.service.CurrentUserService;
import com.gametrend.agent.conversation.dto.ConversationDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/conversations", "/admin/conversations"})
@RequiredArgsConstructor
public class AdminConversationManagementController {

    private final AdminConversationManagementService adminConversationManagementService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public AdminPageResponse<AdminConversationResponse> searchConversations(
            String user,
            String keyword,
            String status,
            Integer page,
            Integer size,
            String sort
    ) {
        return adminConversationManagementService.searchConversations(
                user,
                keyword,
                status,
                page == null ? 0 : page,
                size == null ? 20 : size,
                sort
        );
    }

    @GetMapping("/{conversationId}")
    public ConversationDetailResponse getConversation(@PathVariable Long conversationId) {
        return adminConversationManagementService.getConversation(conversationId);
    }

    @PatchMapping("/{conversationId}/hide")
    public AdminConversationResponse hide(
            @PathVariable Long conversationId,
            @Valid @RequestBody(required = false) AdminConversationModerationRequest request
    ) {
        return adminConversationManagementService.hide(currentUserId(), conversationId, reason(request));
    }

    @PatchMapping("/{conversationId}/restore")
    public AdminConversationResponse restore(
            @PathVariable Long conversationId,
            @Valid @RequestBody(required = false) AdminConversationModerationRequest request
    ) {
        return adminConversationManagementService.restore(currentUserId(), conversationId, reason(request));
    }

    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.OK)
    public AdminConversationResponse softDelete(
            @PathVariable Long conversationId,
            @Valid @RequestBody(required = false) AdminConversationModerationRequest request
    ) {
        return adminConversationManagementService.softDelete(currentUserId(), conversationId, reason(request));
    }

    private Long currentUserId() {
        return currentUserService.getCurrentUser()
                .map(user -> user.id())
                .orElseThrow(() -> new AuthRequiredException("관리자 로그인이 필요합니다."));
    }

    private String reason(AdminConversationModerationRequest request) {
        return request == null ? null : request.reason();
    }
}
