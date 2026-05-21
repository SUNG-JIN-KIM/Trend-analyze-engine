package com.gametrend.agent.reinterpretation.controller;

import com.gametrend.agent.reinterpretation.dto.ReinterpretationAnalyzeRequest;
import com.gametrend.agent.reinterpretation.dto.ReinterpretationAnalyzeResponse;
import com.gametrend.agent.reinterpretation.dto.ReinterpretationCandidateResponse;
import com.gametrend.agent.reinterpretation.service.ReinterpretationAnalysisService;
import com.gametrend.agent.reinterpretation.service.ReinterpretationCandidateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reinterpretation")
public class ReinterpretationController {

    private final ReinterpretationCandidateService candidateService;
    private final ReinterpretationAnalysisService analysisService;

    public ReinterpretationController(
            ReinterpretationCandidateService candidateService,
            ReinterpretationAnalysisService analysisService
    ) {
        this.candidateService = candidateService;
        this.analysisService = analysisService;
    }

    @GetMapping("/candidates")
    public List<ReinterpretationCandidateResponse> findCandidates(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return candidateService.findCandidates(limit);
    }

    @PostMapping("/analyze")
    public ReinterpretationAnalyzeResponse analyze(
            @RequestBody ReinterpretationAnalyzeRequest request
    ) {
        return analysisService.analyze(request);
    }
}
