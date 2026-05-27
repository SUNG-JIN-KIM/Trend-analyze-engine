package com.gametrend.agent.youtube.controller;

import com.gametrend.agent.admin.common.AdminPageResponse;
import com.gametrend.agent.youtube.dto.YoutubeCommentCollectResponse;
import com.gametrend.agent.youtube.dto.YoutubeCollectLogResponse;
import com.gametrend.agent.youtube.dto.YoutubeCollectResponse;
import com.gametrend.agent.youtube.dto.YoutubeDashboardResponse;
import com.gametrend.agent.youtube.dto.YoutubeKeywordStatResponse;
import com.gametrend.agent.youtube.dto.YoutubeVideoResponse;
import com.gametrend.agent.youtube.service.YoutubeTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/youtube")
@RequiredArgsConstructor
public class AdminYoutubeController {

    private final YoutubeTrendService youtubeTrendService;

    @PostMapping("/collect")
    public YoutubeCollectResponse collect(@RequestParam("keyword") String keyword) {
        return youtubeTrendService.collect(keyword);
    }

    @PostMapping("/comments/collect")
    public YoutubeCommentCollectResponse collectComments(@RequestParam("keyword") String keyword) {
        return youtubeTrendService.collectComments(keyword);
    }

    @GetMapping("/dashboard")
    public YoutubeDashboardResponse getDashboard() {
        return youtubeTrendService.getDashboard();
    }

    @GetMapping("/logs")
    public AdminPageResponse<YoutubeCollectLogResponse> findLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return youtubeTrendService.findLogPage(keyword, status, page, size);
    }

    @GetMapping("/videos")
    public AdminPageResponse<YoutubeVideoResponse> findVideos(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String channelTitle,
            @RequestParam(defaultValue = "viewCount") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return youtubeTrendService.findVideoPage(title, keyword, channelTitle, sort, page, size);
    }

    @GetMapping("/keywords")
    public AdminPageResponse<YoutubeKeywordStatResponse> findKeywordStats(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sentiment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return youtubeTrendService.findKeywordStats(keyword, sentiment, page, size);
    }
}
