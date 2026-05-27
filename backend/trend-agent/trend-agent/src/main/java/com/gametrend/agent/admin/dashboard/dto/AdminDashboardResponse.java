package com.gametrend.agent.admin.dashboard.dto;

import com.gametrend.agent.youtube.dto.GameYoutubeTrendScoreResponse;
import com.gametrend.agent.youtube.dto.YoutubeCollectLogResponse;

import java.util.List;

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
        long hiddenConversationCount,
        long youtubeVideoCount,
        long todayYoutubeCollectCount,
        long youtubeCollectSuccessCount,
        long youtubeCollectFailureCount,
        String topYoutubeGameKeyword,
        String latestYoutubeCollectKeyword,
        List<YoutubeCollectLogResponse> recentYoutubeCollectLogs,
        List<GameYoutubeTrendScoreResponse> topYoutubeGames
) {
}
