package com.gametrend.agent.conversation.controller;

import com.gametrend.agent.auth.exception.AuthRequiredException;
import com.gametrend.agent.auth.service.CurrentUserService;
import com.gametrend.agent.conversation.dto.ConversationCreateRequest;
import com.gametrend.agent.conversation.dto.ConversationDetailResponse;
import com.gametrend.agent.conversation.dto.ConversationResponse;
import com.gametrend.agent.conversation.dto.ConversationUpdateRequest;
import com.gametrend.agent.conversation.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/conversations", "/api/my/conversations", "/my/conversations"})
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<ConversationResponse> findConversations() {
        return conversationService.findConversations(currentUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse create(@Valid @RequestBody(required = false) ConversationCreateRequest request) {
        return conversationService.create(
                currentUserId(),
                request == null ? new ConversationCreateRequest(null) : request
        );
    }

    @GetMapping("/{conversationId}")
    public ConversationDetailResponse findConversation(@PathVariable Long conversationId) {
        return conversationService.findConversation(conversationId, currentUserId());
    }

    @PatchMapping("/{conversationId}")
    public ConversationResponse update(
            @PathVariable Long conversationId,
            @Valid @RequestBody ConversationUpdateRequest request
    ) {
        return conversationService.update(conversationId, currentUserId(), request);
    }

    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long conversationId) {
        conversationService.delete(conversationId, currentUserId());
    }

    private Long currentUserId() {
        return currentUserService.getCurrentUser()
                .map(user -> user.id())
                .orElseThrow(() -> new AuthRequiredException("대화 기록은 로그인 후 사용할 수 있습니다."));
    }
}
