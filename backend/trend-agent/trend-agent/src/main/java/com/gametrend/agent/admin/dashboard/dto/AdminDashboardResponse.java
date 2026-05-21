package com.gametrend.agent.admin.dashboard.dto;

public record AdminDashboardResponse(
        long totalUserCount,
        long userCount,
        long adminCount,
        long ownerCount,
        long pendingApprovalCount,
        long todaySignupCount,
        long recentLoginCount,
        long chatReportCount,
        long hiddenChatCount,
        long totalConversationCount,
        long reportedConversationCount,
        long hiddenConversationCount
) {
}
