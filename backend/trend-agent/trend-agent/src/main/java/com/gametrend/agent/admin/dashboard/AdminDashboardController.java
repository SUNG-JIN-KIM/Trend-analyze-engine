package com.gametrend.agent.admin.dashboard;

import com.gametrend.agent.admin.dashboard.dto.AdminDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping
    public AdminDashboardResponse dashboard() {
        return adminDashboardService.getDashboard();
    }
}
