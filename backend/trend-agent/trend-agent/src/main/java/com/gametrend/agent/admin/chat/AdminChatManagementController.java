package com.gametrend.agent.admin.chat;

import com.gametrend.agent.admin.chat.dto.AdminChatModerationRequest;
import com.gametrend.agent.admin.chat.dto.AdminChatResponse;
import com.gametrend.agent.admin.common.AdminPageResponse;
import com.gametrend.agent.auth.exception.AuthRequiredException;
import com.gametrend.agent.auth.service.CurrentUser;
import com.gametrend.agent.auth.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/chats")
@RequiredArgsConstructor
public class AdminChatManagementController {

    private final AdminChatManagementService adminChatManagementService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public AdminPageResponse<AdminChatResponse> chats(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean reported,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort
    ) {
        return adminChatManagementService.searchChats(user, keyword, status, reported, page, size, sort);
    }

    @PatchMapping("/{chatId}/hide")
    public AdminChatResponse hide(
            @PathVariable Long chatId,
            @Valid @RequestBody(required = false) AdminChatModerationRequest request
    ) {
        return adminChatManagementService.hide(currentUser().id(), chatId, request == null ? null : request.reason());
    }

    @PatchMapping("/{chatId}/restore")
    public AdminChatResponse restore(
            @PathVariable Long chatId,
            @Valid @RequestBody(required = false) AdminChatModerationRequest request
    ) {
        return adminChatManagementService.restore(currentUser().id(), chatId, request == null ? null : request.reason());
    }

    @DeleteMapping("/{chatId}")
    public AdminChatResponse delete(
            @PathVariable Long chatId,
            @Valid @RequestBody(required = false) AdminChatModerationRequest request
    ) {
        return adminChatManagementService.delete(currentUser().id(), chatId, request == null ? null : request.reason());
    }

    private CurrentUser currentUser() {
        return currentUserService.getCurrentUser()
                .orElseThrow(() -> new AuthRequiredException("관리자 로그인이 필요합니다."));
    }
}
