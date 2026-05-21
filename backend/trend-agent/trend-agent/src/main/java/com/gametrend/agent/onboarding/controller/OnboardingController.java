package com.gametrend.agent.onboarding.controller;

import com.gametrend.agent.auth.exception.AuthRequiredException;
import com.gametrend.agent.auth.service.CurrentUser;
import com.gametrend.agent.auth.service.CurrentUserService;
import com.gametrend.agent.onboarding.dto.OnboardingAnalyzeRequest;
import com.gametrend.agent.onboarding.dto.OnboardingAnalyzeResponse;
import com.gametrend.agent.onboarding.dto.OnboardingHistoryDetailResponse;
import com.gametrend.agent.onboarding.dto.OnboardingHistoryItemResponse;
import com.gametrend.agent.onboarding.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final CurrentUserService currentUserService;

    @PostMapping("/analyze")
    public OnboardingAnalyzeResponse analyze(@Valid @RequestBody OnboardingAnalyzeRequest request) {
        return onboardingService.analyze(request);
    }

    @GetMapping("/history")
    public List<OnboardingHistoryItemResponse> findHistories() {
        CurrentUser currentUser = currentUser();
        return onboardingService.findHistories(currentUser.id(), isAdmin(currentUser));
    }

    @GetMapping("/history/{id}")
    public OnboardingHistoryDetailResponse findHistory(@PathVariable Long id) {
        CurrentUser currentUser = currentUser();
        return onboardingService.findHistory(id, currentUser.id(), isAdmin(currentUser));
    }

    @DeleteMapping("/history/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHistory(@PathVariable Long id) {
        CurrentUser currentUser = currentUser();
        onboardingService.deleteHistory(id, currentUser.id(), isAdmin(currentUser));
    }

    private CurrentUser currentUser() {
        return currentUserService.getCurrentUser()
                .orElseThrow(() -> new AuthRequiredException("대화 기록은 로그인 후 사용할 수 있습니다."));
    }

    private boolean isAdmin(CurrentUser currentUser) {
        String role = currentUser.role();
        return "ADMIN".equalsIgnoreCase(role) || "OWNER".equalsIgnoreCase(role);
    }
}
