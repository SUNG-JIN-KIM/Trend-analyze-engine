package com.gametrend.agent.onboarding.exception;

public class OnboardingHistoryNotFoundException extends RuntimeException {

    public OnboardingHistoryNotFoundException(Long id) {
        super("온보딩 분석 이력을 찾을 수 없습니다. id=" + id);
    }
}
