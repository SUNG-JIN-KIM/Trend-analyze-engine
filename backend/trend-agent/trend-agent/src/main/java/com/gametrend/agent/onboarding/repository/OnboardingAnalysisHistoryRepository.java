package com.gametrend.agent.onboarding.repository;

import com.gametrend.agent.onboarding.entity.OnboardingAnalysisHistory;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface OnboardingAnalysisHistoryRepository extends CrudRepository<OnboardingAnalysisHistory, Long> {

    List<OnboardingAnalysisHistory> findAllByOrderByCreatedAtDesc();

    List<OnboardingAnalysisHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<OnboardingAnalysisHistory> findByIdAndUserId(Long id, Long userId);

    Optional<OnboardingAnalysisHistory> findFirstByConversationIdOrderByCreatedAtDesc(String conversationId);

    Optional<OnboardingAnalysisHistory> findFirstByConversationIdAndUserIdOrderByCreatedAtDesc(
            String conversationId,
            Long userId
    );
}
