package com.gametrend.agent.report.controller;

import com.gametrend.agent.report.dto.ReportRequest;
import com.gametrend.agent.report.dto.ReportResponse;
import com.gametrend.agent.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/draft")
    public ReportResponse generateDraft(@Valid @RequestBody(required = false) ReportRequest request) {
        return reportService.generateDraft(request);
    }
}
