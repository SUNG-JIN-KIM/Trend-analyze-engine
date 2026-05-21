package com.gametrend.agent.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.agent.auth.exception.AuthRequiredException;
import com.gametrend.agent.auth.service.CurrentUserService;
import com.gametrend.agent.conversation.entity.Conversation;
import com.gametrend.agent.conversation.service.ConversationService;
import com.gametrend.agent.infrastructure.llm.LlmCallException;
import com.gametrend.agent.infrastructure.llm.LlmClient;
import com.gametrend.agent.livetrend.dto.LiveTrendGameResponse;
import com.gametrend.agent.livetrend.service.LiveTrendService;
import com.gametrend.agent.onboarding.dto.OnboardingAnalyzeRequest;
import com.gametrend.agent.onboarding.dto.OnboardingAnalyzeResponse;
import com.gametrend.agent.onboarding.dto.OnboardingHistoryDetailResponse;
import com.gametrend.agent.onboarding.dto.OnboardingHistoryItemResponse;
import com.gametrend.agent.onboarding.entity.OnboardingAnalysisHistory;
import com.gametrend.agent.onboarding.repository.OnboardingAnalysisHistoryRepository;
import com.gametrend.agent.trend.dto.TrendGameResponse;
import com.gametrend.agent.trend.service.TrendGameService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnboardingServiceTest {

    @Test
    void analyze_returnsLlmReportWhenLlmSucceeds() {
        CountingLlmClient llmClient = new CountingLlmClient("## 분석 요약\nLLM 리포트입니다.");
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService onboardingService = newService(llmClient, historyRepository);

        OnboardingAnalyzeResponse response = onboardingService.analyze(request());

        assertEquals(1L, response.historyId());
        assertEquals("## 분석 요약\nLLM 리포트입니다.", response.report());
        assertEquals("FEATURE_BASED_IDEA", response.intent());
        assertTrue(response.answer().contains("WEBCAM"));
        assertEquals(2, response.evidenceCards().size());
        assertTrue(response.detectedKeywords().contains("스트리머/방송/Twitch/YouTube/시청자 참여"));
        assertTrue(response.followUpQuestions().contains("프로토타입 수준으로 다시 분석해줘"));
        assertEquals(3, response.recommendedConcepts().size());
        assertEquals(1, historyRepository.count());
        assertEquals(1, llmClient.callCount());
        assertTrue(llmClient.lastSystemPrompt().contains("GEMMA4 E2B"));
        assertTrue(llmClient.lastUserPrompt().contains("스트리머들이 하기 좋고"));
        assertTrue(llmClient.lastUserPrompt().contains("PC"));
        assertTrue(llmClient.lastUserPrompt().contains("small"));
        assertTrue(llmClient.lastUserPrompt().contains("webcam"));
        assertTrue(llmClient.lastUserPrompt().contains("3 months"));
        assertTrue(llmClient.lastUserPrompt().contains("추출된 장르/의도"));
        assertTrue(llmClient.lastUserPrompt().contains("사용자 질문에 대한 직접 답변 초안"));
        assertTrue(llmClient.lastUserPrompt().contains("추천 컨셉 후보"));
        assertTrue(llmClient.lastUserPrompt().contains("근거 카드 데이터"));
        assertTrue(llmClient.lastUserPrompt().contains("Steam 리뷰"));
        assertTrue(llmClient.lastUserPrompt().contains("Twitch/YouTube"));
    }

    @Test
    void analyze_returnsFallbackReportWhenLlmFails() {
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("테스트 예외"));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService onboardingService = newService(llmClient, historyRepository);

        OnboardingAnalyzeResponse response = onboardingService.analyze(request());

        assertEquals(1L, response.historyId());
        assertEquals(3, response.recommendedConcepts().size());
        assertEquals(2, response.evidenceCards().size());
        assertTrue(response.summary().contains("기능 기반"));
        assertTrue(response.report().contains("## 분석 요약"));
        assertTrue(response.report().contains("## 근거 카드"));
        assertTrue(response.report().contains(response.answer()));
        assertTrue(response.report().contains("fallback 분석 결과"));
        assertTrue(response.report().contains("Reaction Party Challenge")
                || response.report().contains("Chat TTS Party Room")
                || response.report().contains("Webcam Rhythm Battle"));
        assertEquals(1, historyRepository.count());
        assertEquals(1, llmClient.callCount());
    }

    @Test
    void analyze_greetingReturnsShortAnswerWithoutCallingAnalysisLlm() {
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("분석 LLM이 호출되면 안 됩니다."));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService onboardingService = newService(llmClient, historyRepository);

        OnboardingAnalyzeResponse response = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "안녕",
                null,
                null,
                List.of(),
                null
        ));

        assertEquals("GREETING", response.intent());
        assertEquals("GREETING", response.agentPlan().intent());
        assertEquals("SHORT", response.agentPlan().responseDepth());
        assertTrue(response.answer().contains("안녕하세요"));
        assertTrue(response.evidenceCards().isEmpty());
        assertEquals(1, historyRepository.count());
        assertEquals(0, llmClient.callCount());
    }

    @Test
    void analyze_helpReturnsCapabilityGuideWithoutCallingAnalysisLlm() {
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("분석 LLM이 호출되면 안 됩니다."));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService onboardingService = newService(llmClient, historyRepository);

        OnboardingAnalyzeResponse response = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "뭐 할 수 있어?",
                null,
                null,
                List.of(),
                null
        ));

        assertEquals("HELP", response.intent());
        assertEquals("HELP", response.queryCondition().analysisPurpose());
        assertTrue(response.answer().contains("게임 트렌드 분석 Agent"));
        assertTrue(response.followUpQuestions().contains("요즘 할만한 게임 추천해줘"));
        assertEquals(0, llmClient.callCount());
    }

    @Test
    void analyze_guestPublicQuestionDoesNotPersistConversationMemoryOrHistory() {
        SecurityContextHolder.clearContext();
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("테스트 예외"));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        ThrowingConversationService conversationService = new ThrowingConversationService();
        OnboardingService onboardingService = authenticatedAwareService(
                llmClient,
                historyRepository,
                conversationService
        );

        OnboardingAnalyzeResponse response = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "요즘 할만한 게임 추천해줘",
                null,
                null,
                List.of(),
                null
        ));

        assertEquals("GAME_RECOMMENDATION", response.intent());
        assertNull(response.historyId());
        assertNull(response.parentHistoryId());
        assertNull(response.conversationId());
        assertNull(response.memorySummary());
        assertTrue(response.sessionId().startsWith("agent-session-"));
        assertEquals(0, historyRepository.count());
        assertFalse(conversationService.resolveCalled());
        assertFalse(conversationService.appendCalled());
    }

    @Test
    void analyze_guestConversationIdRequiresLoginWithoutPersisting() {
        SecurityContextHolder.clearContext();
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("호출되면 안 됩니다."));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        ThrowingConversationService conversationService = new ThrowingConversationService();
        OnboardingService onboardingService = authenticatedAwareService(
                llmClient,
                historyRepository,
                conversationService
        );

        AuthRequiredException exception = assertThrows(AuthRequiredException.class, () -> onboardingService.analyze(
                new OnboardingAnalyzeRequest(
                        "요즘 할만한 게임 추천해줘",
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        "123"
                )
        ));

        assertTrue(exception.getMessage().contains("로그인"));
        assertEquals(0, historyRepository.count());
        assertFalse(conversationService.resolveCalled());
        assertFalse(conversationService.appendCalled());
        assertEquals(0, llmClient.callCount());
    }

    @Test
    void analyze_returnsDifferentFallbackConceptsByUserIntent() {
        InMemoryHistoryRepository fpsHistoryRepository = new InMemoryHistoryRepository();
        OnboardingService fpsService = newService(
                new CountingLlmClient(new LlmCallException("테스트 예외")),
                fpsHistoryRepository
        );
        InMemoryHistoryRepository partyHistoryRepository = new InMemoryHistoryRepository();
        OnboardingService partyService = newService(
                new CountingLlmClient(new LlmCallException("테스트 예외")),
                partyHistoryRepository
        );

        OnboardingAnalyzeResponse fpsResponse = fpsService.analyze(new OnboardingAnalyzeRequest(
                "FPS 게임을 만들고 싶은데 가능성 있어보여?",
                "PC",
                "solo",
                List.of(),
                "6 months"
        ));
        OnboardingAnalyzeResponse partyResponse = partyService.analyze(new OnboardingAnalyzeRequest(
                "웹캠, TTS, STT를 쓰는 파티 게임을 만들고 싶어요.",
                "PC",
                "small",
                List.of("webcam", "tts", "stt"),
                "3 months"
        ));

        List<String> fpsTitles = fpsResponse.recommendedConcepts()
                .stream()
                .map(concept -> concept.title())
                .toList();
        List<String> partyTitles = partyResponse.recommendedConcepts()
                .stream()
                .map(concept -> concept.title())
                .toList();

        assertTrue(fpsTitles.contains("Tactical Extraction Lite")
                || fpsTitles.contains("Streamer Squad Arena")
                || fpsTitles.contains("Voice Command Survival FPS"));
        assertTrue(partyTitles.contains("Reaction Party Challenge")
                || partyTitles.contains("Chat TTS Party Room")
                || partyTitles.contains("Webcam Rhythm Battle"));
        assertNotEquals(fpsTitles, partyTitles);
        assertNotEquals(fpsResponse.answer(), partyResponse.answer());
        assertTrue(fpsResponse.summary().contains("FPS"));
        assertTrue(partyResponse.summary().contains("WEBCAM"));
        assertTrue(fpsResponse.report().contains("FPS/슈팅"));
        assertTrue(partyResponse.report().contains("파티/협동"));
        assertEquals("DEVELOPMENT_FEASIBILITY", fpsResponse.intent());
        assertEquals("FEATURE_BASED_IDEA", partyResponse.intent());
        assertNotEquals(fpsResponse.intent(), partyResponse.intent());
    }

    @Test
    void analyze_classifiesRecommendationTrendAndSpecificGameQuestions() {
        OnboardingService recommendationService = newService(
                new CountingLlmClient(new LlmCallException("테스트 예외")),
                new InMemoryHistoryRepository()
        );
        OnboardingService trendService = newService(
                new CountingLlmClient(new LlmCallException("테스트 예외")),
                new InMemoryHistoryRepository()
        );
        OnboardingService specificGameService = newService(
                new CountingLlmClient(new LlmCallException("테스트 예외")),
                new InMemoryHistoryRepository()
        );

        OnboardingAnalyzeResponse recommendation = recommendationService.analyze(new OnboardingAnalyzeRequest(
                "나 할만한 게임 추천해줘",
                null,
                null,
                List.of(),
                null
        ));
        OnboardingAnalyzeResponse trend = trendService.analyze(new OnboardingAnalyzeRequest(
                "요즘 어떤 게임이 인기 있어?",
                "PC",
                null,
                List.of(),
                null
        ));
        OnboardingAnalyzeResponse specificGame = specificGameService.analyze(new OnboardingAnalyzeRequest(
                "요즘 배그는 어떤 거 같아?",
                "PC",
                null,
                List.of(),
                null
        ));

        assertEquals("GAME_RECOMMENDATION", recommendation.intent());
        assertEquals("TREND_ANALYSIS", trend.intent());
        assertEquals("SPECIFIC_GAME_ANALYSIS", specificGame.intent());
        assertTrue(recommendation.answer().contains("추천"));
        assertTrue(trend.answer().contains("인기 흐름"));
        assertTrue(specificGame.answer().contains("인기 요인"));
        assertTrue(recommendation.followUpQuestions().contains("혼자 할 게임 기준으로 추천해줘"));
        assertTrue(recommendation.recommendedConcepts().stream()
                .anyMatch(concept -> concept.title().equals("Monster Hunter Wilds")
                        || concept.title().equals("Minecraft")
                        || concept.title().equals("Lethal Company")));
        assertTrue(trend.recommendedConcepts().stream().anyMatch(concept -> concept.genre().equals("Trend Analysis")));
        assertTrue(specificGame.recommendedConcepts().stream().anyMatch(concept -> concept.genre().equals("Specific Game Analysis")));
        assertNotEquals(recommendation.recommendedConcepts().get(0).title(), trend.recommendedConcepts().get(0).title());
        assertNotEquals(trend.recommendedConcepts().get(0).title(), specificGame.recommendedConcepts().get(0).title());
    }

    @Test
    void analyze_detectsYoutubeTwitchStreamingKeywords() {
        OnboardingService onboardingService = newService(
                new CountingLlmClient(new LlmCallException("테스트 예외")),
                new InMemoryHistoryRepository()
        );

        OnboardingAnalyzeResponse response = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "트위치랑 유튜브 방송에서 반응 잘 나오는 게임 알려줘",
                "PC",
                null,
                List.of(),
                null
        ));

        assertEquals("STREAMING_FIT_ANALYSIS", response.intent());
        assertTrue(response.detectedKeywords().contains("스트리머/방송/Twitch/YouTube/시청자 참여"));
        assertTrue(response.answer().contains("스트리머용"));
        assertTrue(response.recommendedConcepts().stream()
                .anyMatch(concept -> concept.genre().equals("Streaming Fit")));
    }

    @Test
    void analyze_trendQuestionReturnsEvidenceCardsFromTrendSignalsEvenWhenLlmFails() {
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("테스트 예외"));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService onboardingService = new OnboardingService(
                llmClient,
                historyRepository.asRepository(),
                new FakeTrendGameService(List.of(
                        trendGame("Lethal Company", "Co-op Horror", 91.0, 88, 12_500, 93, 89),
                        trendGame("Counter-Strike 2", "FPS", 87.5, 90, 95_000, 84, 92)
                )),
                new ObjectMapper()
        );

        OnboardingAnalyzeResponse response = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "요즘 어떤 게임이 인기 있어?",
                "PC",
                null,
                List.of(),
                null
        ));

        assertEquals("TREND_ANALYSIS", response.intent());
        assertEquals(2, response.evidenceCards().size());
        assertEquals("TREND_GAME", response.evidenceCards().get(0).type());
        assertEquals("Lethal Company", response.evidenceCards().get(0).title());
        assertEquals(91.0, response.evidenceCards().get(0).trendScore());
        assertEquals(88, response.evidenceCards().get(0).steamReviewScore());
        assertEquals(12_500, response.evidenceCards().get(0).twitchViewerCount());
        assertTrue(response.report().contains("## 근거 카드"));
        assertTrue(response.report().contains("Lethal Company"));
    }

    @Test
    void analyze_llmPromptIncludesEvidenceCardsWhenTrendSignalsExist() {
        CountingLlmClient llmClient = new CountingLlmClient("LLM 리포트");
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService onboardingService = new OnboardingService(
                llmClient,
                historyRepository.asRepository(),
                new FakeTrendGameService(List.of(
                        trendGame("PUBG", "Battle Royale", 82.0, 70, 42_000, 86, 78)
                )),
                new ObjectMapper()
        );

        OnboardingAnalyzeResponse response = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "요즘 FPS 가능성 있어?",
                "PC",
                "small",
                List.of(),
                "6 months"
        ));

        assertEquals("DEVELOPMENT_FEASIBILITY", response.intent());
        assertTrue(response.evidenceCards().stream().anyMatch(card -> card.title().equals("PUBG")));
        assertTrue(llmClient.lastUserPrompt().contains("근거 카드 데이터"));
        assertTrue(llmClient.lastUserPrompt().contains("PUBG"));
        assertTrue(llmClient.lastUserPrompt().contains("trendScore=82.0"));
    }

    @Test
    void analyze_trendQuestionUsesLiveTrendSignalsForEvidencePromptAndFallback() {
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("테스트 예외"));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService onboardingService = new OnboardingService(
                llmClient,
                historyRepository.asRepository(),
                new FakeTrendGameService(List.of()),
                new FakeLiveTrendService(List.of(
                        liveTrendGame("Counter-Strike 2", "TWITCH", "FPS", 97_000, 1_800, 94.5, "COMPLETE", "REAL"),
                        liveTrendGame("PUBG", "CHZZK", "Battle Royale", 32_000, 700, 82.0, "PARTIAL", "FALLBACK")
                )),
                new ObjectMapper()
        );

        OnboardingAnalyzeResponse response = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "요즘 어떤 게임이 인기 있어?",
                "PC",
                null,
                List.of(),
                null
        ));

        assertEquals("TREND_ANALYSIS", response.intent());
        assertEquals("LIVE_TREND_GAME", response.evidenceCards().get(0).type());
        assertEquals("Counter-Strike 2", response.evidenceCards().get(0).title());
        assertEquals("TWITCH", response.evidenceCards().get(0).source());
        assertEquals(97_000, response.evidenceCards().get(0).totalViewerCount());
        assertEquals(1_800, response.evidenceCards().get(0).liveStreamCount());
        assertEquals("REAL", response.evidenceCards().get(0).dataOrigin());
        assertTrue(response.answer().contains("현재 수집된 라이브 트렌드 중 트렌드 점수 기준"));
        assertTrue(response.answer().contains("실제 수집 데이터"));
        assertTrue(response.report().contains("liveTrendCandidates"));
        assertTrue(response.report().contains("Counter-Strike 2"));
        assertTrue(llmClient.lastUserPrompt().contains("liveTrendCandidates"));
        assertTrue(llmClient.lastUserPrompt().contains("현재 수집된 라이브 트렌드 기준"));
    }

    @Test
    void analyze_streamingFitUsesLiveTrendSignals() {
        CountingLlmClient llmClient = new CountingLlmClient("LLM 리포트");
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService onboardingService = new OnboardingService(
                llmClient,
                historyRepository.asRepository(),
                new FakeTrendGameService(List.of()),
                new FakeLiveTrendService(List.of(
                        liveTrendGame("Lethal Company", "TWITCH", "Co-op Horror", 18_000, 420, 90.0, "COMPLETE", "REAL")
                )),
                new ObjectMapper()
        );

        OnboardingAnalyzeResponse response = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "스트리머들이 많이 하는 게임 알려줘",
                "PC",
                null,
                List.of(),
                null
        ));

        assertEquals("STREAMING_FIT_ANALYSIS", response.intent());
        assertTrue(response.evidenceCards().stream().anyMatch(card -> card.type().equals("LIVE_STREAMING_SIGNAL")));
        assertTrue(response.summary().contains("현재 수집된 라이브 트렌드 중 방송 수 기준"));
        assertTrue(llmClient.lastUserPrompt().contains("Lethal Company"));
    }

    @Test
    void analyze_popularGameRecommendationUsesLiveTrendSignals() {
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("테스트 예외"));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService onboardingService = new OnboardingService(
                llmClient,
                historyRepository.asRepository(),
                new FakeTrendGameService(List.of()),
                new FakeLiveTrendService(List.of(
                        liveTrendGame("Monster Hunter Wilds", "TWITCH", "Action RPG", 88_000, 1_200, 92.0, "COMPLETE", "REAL"),
                        liveTrendGame("Eternal Return", "CHZZK", "MOBA", 24_000, 530, 78.5, "PARTIAL", "FALLBACK")
                )),
                new ObjectMapper()
        );

        OnboardingAnalyzeResponse response = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "요즘 할만한 인기 게임 추천해줘",
                "PC",
                null,
                List.of(),
                null
        ));

        assertEquals("GAME_RECOMMENDATION", response.intent());
        assertEquals("LIVE_TREND_RECOMMENDATION", response.evidenceCards().get(0).type());
        assertEquals("Monster Hunter Wilds", response.evidenceCards().get(0).title());
        assertEquals("TWITCH", response.evidenceCards().get(0).source());
        assertEquals(88_000, response.evidenceCards().get(0).totalViewerCount());
        assertTrue(response.answer().contains("현재 수집된 라이브 트렌드 중 트렌드 점수 기준"));
        assertTrue(response.answer().contains("Twitch 기준"));
        assertTrue(response.report().contains("Monster Hunter Wilds"));
        assertTrue(response.report().contains("부분 수집 데이터는 보조 신호"));
        assertTrue(response.followUpQuestions().contains("Twitch 기준으로 다시 분석해줘"));
        assertTrue(response.followUpQuestions().contains("시청자 수보다 방송 수 중심으로 다시 분석해줘"));
        assertTrue(llmClient.lastUserPrompt().contains("Twitch 기준"));
        assertTrue(llmClient.lastUserPrompt().contains("실제 수집 데이터 기준"));
    }

    @Test
    void analyze_soloFpsRecommendationUsesSoloFriendlyCandidatesInsteadOfCompetitiveLiveTrend() {
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("테스트 예외"));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService onboardingService = new OnboardingService(
                llmClient,
                historyRepository.asRepository(),
                new FakeTrendGameService(List.of()),
                new FakeLiveTrendService(List.of(
                        liveTrendGame("VALORANT", "TWITCH", "FPS", 80_000, 1_600, 91.0, "COMPLETE", "REAL"),
                        liveTrendGame("Counter-Strike 2", "TWITCH", "Tactical FPS", 97_000, 1_800, 94.5, "COMPLETE", "REAL")
                )),
                new ObjectMapper()
        );

        OnboardingAnalyzeResponse response = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "fps 게임으로 추천해줘 혼자 할거야",
                "PC",
                null,
                List.of(),
                null
        ));

        assertEquals("GAME_RECOMMENDATION", response.intent());
        assertTrue(response.answer().contains("혼자 할 기준"));
        assertTrue(response.answer().contains("DOOM Eternal") || response.answer().contains("Titanfall 2"));
        assertTrue(response.evidenceCards().stream().noneMatch(card -> "VALORANT".equals(card.title())));
        assertTrue(response.evidenceCards().stream().noneMatch(card -> "Counter-Strike 2".equals(card.title())));
        assertTrue(response.recommendedConcepts().stream()
                .anyMatch(concept -> concept.title().equals("DOOM Eternal") || concept.title().equals("Titanfall 2")));
        assertTrue(!response.answer().contains("혼자 할지, 친구"));
    }

    @Test
    void analyze_rpgRecommendationDoesNotFallBackToUnrelatedLiveTrendGames() {
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("테스트 예외"));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService onboardingService = new OnboardingService(
                llmClient,
                historyRepository.asRepository(),
                new FakeTrendGameService(List.of()),
                new FakeLiveTrendService(List.of(
                        liveTrendGame("Dota 2", "TWITCH", "MOBA", 88_000, 1_200, 92.0, "COMPLETE", "REAL"),
                        liveTrendGame("Minecraft", "TWITCH", "Sandbox Survival", 60_000, 900, 86.0, "COMPLETE", "REAL"),
                        liveTrendGame("VALORANT", "TWITCH", "FPS", 80_000, 1_600, 91.0, "COMPLETE", "REAL")
                )),
                new ObjectMapper()
        );

        OnboardingAnalyzeResponse response = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "RPG 게임도 추천해줘",
                "PC",
                null,
                List.of(),
                null
        ));

        assertEquals("GAME_RECOMMENDATION", response.intent());
        assertTrue(response.summary().contains("RPG"));
        assertTrue(response.evidenceCards().stream().noneMatch(card -> "Dota 2".equals(card.title())));
        assertTrue(response.evidenceCards().stream().noneMatch(card -> "Minecraft".equals(card.title())));
        assertTrue(response.evidenceCards().stream().noneMatch(card -> "VALORANT".equals(card.title())));
        assertTrue(response.recommendedConcepts().stream()
                .anyMatch(concept -> concept.title().equals("Monster Hunter Wilds")
                        || concept.title().equals("Baldur's Gate 3")
                        || concept.title().equals("Path of Exile 2")));
    }

    @Test
    void analyze_chzzkPlatformQuestionUsesOnlyChzzkLiveTrendSignals() {
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("테스트 예외"));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        FakeLiveTrendService liveTrendService = new FakeLiveTrendService(List.of(
                liveTrendGame("Just Chatting", "TWITCH", "Just Chatting", 120_000, 3_000, 99.0, "COMPLETE", "REAL"),
                liveTrendGame("Eternal Return", "CHZZK", "MOBA", 24_000, 530, 78.5, "COMPLETE", "REAL")
        ));
        OnboardingService onboardingService = new OnboardingService(
                llmClient,
                historyRepository.asRepository(),
                new FakeTrendGameService(List.of()),
                liveTrendService,
                new ObjectMapper()
        );

        OnboardingAnalyzeResponse response = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "치지직 기준으로 요즘 인기 있는 게임 알려줘",
                "PC",
                null,
                List.of(),
                null
        ));

        assertEquals("CHZZK", liveTrendService.lastPlatform());
        assertEquals("TREND_ANALYSIS", response.intent());
        assertEquals("CHZZK", response.queryCondition().platformFilter());
        assertEquals("TREND_SCORE", response.queryCondition().sortMetric());
        assertTrue(response.evidenceCards().stream().allMatch(card -> "CHZZK".equals(card.source())));
        assertTrue(response.answer().contains("CHZZK 라이브 트렌드"));
        assertTrue(response.report().contains("selectedPlatform=CHZZK"));
        assertTrue(response.report().contains("Eternal Return"));
        assertTrue(response.evidenceCards().stream().noneMatch(card -> "TWITCH".equals(card.source())));
    }

    @Test
    void analyze_latestFollowUpPlatformOverridesPreviousPlatformContext() {
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("테스트 예외"));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        FakeLiveTrendService liveTrendService = new FakeLiveTrendService(List.of(
                liveTrendGame("Counter-Strike 2", "TWITCH", "FPS", 97_000, 1_800, 94.5, "COMPLETE", "REAL"),
                liveTrendGame("Eternal Return", "CHZZK", "MOBA", 24_000, 530, 78.5, "COMPLETE", "REAL")
        ));
        OnboardingService onboardingService = new OnboardingService(
                llmClient,
                historyRepository.asRepository(),
                new FakeTrendGameService(List.of()),
                liveTrendService,
                new ObjectMapper()
        );

        OnboardingAnalyzeResponse first = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "트위치 기준으로 요즘 인기 있는 게임 알려줘",
                "PC",
                null,
                List.of(),
                null
        ));
        OnboardingAnalyzeResponse followUp = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "치지직 기준으로 알려줘",
                null,
                null,
                List.of(),
                null,
                first.historyId(),
                first.conversationId()
        ));

        assertEquals("CHZZK", liveTrendService.lastPlatform());
        assertEquals("CHZZK", followUp.queryCondition().platformFilter());
        assertTrue(followUp.answer().contains("CHZZK 라이브 트렌드"));
        assertTrue(followUp.evidenceCards().stream().allMatch(card -> "CHZZK".equals(card.source())));
        assertTrue(followUp.report().contains("selectedPlatform=CHZZK"));
    }

    @Test
    void analyze_unfilteredTrendQuestionUsesAllPlatformsAndExcludesNonGameCategories() {
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("테스트 예외"));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        FakeLiveTrendService liveTrendService = new FakeLiveTrendService(List.of(
                liveTrendGame("Just Chatting", "TWITCH", "Just Chatting", 180_000, 5_000, 99.9, "COMPLETE", "REAL"),
                liveTrendGame("Valorant", "TWITCH", "FPS", 80_000, 1_600, 91.0, "COMPLETE", "REAL"),
                liveTrendGame("Eternal Return", "CHZZK", "MOBA", 24_000, 530, 78.5, "COMPLETE", "REAL")
        ));
        OnboardingService onboardingService = new OnboardingService(
                llmClient,
                historyRepository.asRepository(),
                new FakeTrendGameService(List.of()),
                liveTrendService,
                new ObjectMapper()
        );

        OnboardingAnalyzeResponse response = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "요즘 어떤 게임이 인기 있어?",
                "PC",
                null,
                List.of(),
                null
        ));

        assertEquals("ALL", liveTrendService.lastPlatform());
        assertEquals("ALL", response.queryCondition().platformFilter());
        assertTrue(response.evidenceCards().stream().noneMatch(card -> "Just Chatting".equals(card.title())));
        assertEquals("Valorant", response.evidenceCards().get(0).title());
        assertTrue(response.report().contains("selectedPlatform=ALL"));
    }

    @Test
    void analyze_viewerCountSortMetricOrdersLiveTrendEvidenceByViewerCount() {
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("테스트 예외"));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        FakeLiveTrendService liveTrendService = new FakeLiveTrendService(List.of(
                liveTrendGame("High Trend Low Viewer", "TWITCH", "FPS", 10_000, 900, 99.0, "COMPLETE", "REAL"),
                liveTrendGame("High Viewer", "CHZZK", "MOBA", 120_000, 400, 80.0, "COMPLETE", "REAL")
        ));
        OnboardingService onboardingService = new OnboardingService(
                llmClient,
                historyRepository.asRepository(),
                new FakeTrendGameService(List.of()),
                liveTrendService,
                new ObjectMapper()
        );

        OnboardingAnalyzeResponse response = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "시청자 수 기준으로 인기 있는 게임 알려줘",
                "PC",
                null,
                List.of(),
                null
        ));

        assertEquals("VIEWER_COUNT", response.queryCondition().sortMetric());
        assertEquals("High Viewer", response.evidenceCards().get(0).title());
        assertTrue(response.answer().contains("시청자 수 기준"));
        assertTrue(response.report().contains("sortMetric=VIEWER_COUNT"));
    }

    @Test
    void analyze_followUpQuestionUsesParentHistoryContextWhenMessageIsShort() {
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("테스트 예외"));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService onboardingService = newService(llmClient, historyRepository);

        OnboardingAnalyzeResponse first = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "FPS 게임 만들면 가능성 있어?",
                "PC",
                "small",
                List.of(),
                "6 months"
        ));

        OnboardingAnalyzeResponse followUp = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "혼자 개발할 예정이야",
                null,
                null,
                List.of(),
                null,
                first.historyId(),
                null
        ));

        assertEquals(first.historyId(), followUp.parentHistoryId());
        assertEquals(first.conversationId(), followUp.conversationId());
        assertEquals("DEVELOPMENT_FEASIBILITY", followUp.intent());
        assertTrue(followUp.summary().contains("FPS"));
        assertTrue(followUp.report().contains("## 이전 분석 맥락"));
        assertTrue(followUp.report().contains("FPS 게임 만들면 가능성 있어?"));
        assertTrue(followUp.recommendedConcepts().stream()
                .anyMatch(concept -> concept.title().contains("Tactical Extraction Lite")
                        || concept.title().contains("Streamer Squad Arena")
                        || concept.title().contains("Voice Command Survival FPS")));

        OnboardingHistoryDetailResponse detail = onboardingService.findHistory(followUp.historyId());
        assertEquals(first.historyId(), detail.parentHistoryId());
        assertEquals(first.conversationId(), detail.conversationId());
        assertEquals("solo", detail.teamSize());
        assertEquals("PC", detail.targetPlatform());
    }

    @Test
    void analyze_followUpPrototypeRequestChangesSummaryAnswerAndConcepts() {
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("테스트 예외"));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService onboardingService = newService(llmClient, historyRepository);

        OnboardingAnalyzeResponse first = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "FPS 게임 만들면 가능성 있어?",
                "PC",
                "small",
                List.of(),
                "6 months"
        ));

        OnboardingAnalyzeResponse followUp = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "프로토타입 수준으로 다시 분석해줘",
                null,
                null,
                List.of(),
                null,
                first.historyId(),
                null
        ));

        assertEquals(first.historyId(), followUp.parentHistoryId());
        assertEquals("DEVELOPMENT_FEASIBILITY", followUp.intent());
        assertNotEquals(first.summary(), followUp.summary());
        assertNotEquals(first.answer(), followUp.answer());
        assertTrue(followUp.summary().contains("프로토타입"));
        assertTrue(followUp.answer().contains("프로토타입 수준"));
        assertTrue(followUp.recommendedConcepts().stream()
                .anyMatch(concept -> concept.title().contains("Prototype Slice")));
        assertTrue(followUp.followUpQuestions().contains("출시 가능한 MVP 기준으로 다시 분석해줘"));
        assertTrue(followUp.report().contains("현재 분석 관점: 프로토타입 범위"));
    }

    @Test
    void analyze_followUpScopeDecisionComparesPrototypeAndReleaseMvp() {
        CountingLlmClient llmClient = new CountingLlmClient(new LlmCallException("테스트 예외"));
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService onboardingService = newService(llmClient, historyRepository);

        OnboardingAnalyzeResponse first = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "FPS 게임 만들면 가능성 있어?",
                "PC",
                "small",
                List.of(),
                "6 months"
        ));

        OnboardingAnalyzeResponse followUp = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "프로토타입 수준인지, 출시 가능한 MVP 수준인지 정해졌나요?",
                null,
                null,
                List.of(),
                null,
                first.historyId(),
                null
        ));

        assertEquals(first.historyId(), followUp.parentHistoryId());
        assertEquals("DEVELOPMENT_FEASIBILITY", followUp.intent());
        assertNotEquals(first.summary(), followUp.summary());
        assertTrue(followUp.summary().contains("프로토타입과 출시 MVP"));
        assertTrue(followUp.answer().contains("프로토타입을 먼저 추천"));
        assertTrue(followUp.answer().contains("출시 MVP"));
        assertTrue(followUp.recommendedConcepts().stream()
                .anyMatch(concept -> concept.title().contains("Scope Decision")));
        assertTrue(followUp.report().contains("현재 분석 관점: 프로토타입/MVP 범위 선택"));
    }

    @Test
    void analyze_llmPromptIncludesParentHistoryContext() {
        CountingLlmClient llmClient = new CountingLlmClient("LLM 후속 리포트");
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService onboardingService = newService(llmClient, historyRepository);

        OnboardingAnalyzeResponse first = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "웹캠이랑 TTS 활용한 게임 만들고 싶어",
                "PC",
                "small",
                List.of("webcam", "tts"),
                "3 months"
        ));

        OnboardingAnalyzeResponse followUp = onboardingService.analyze(new OnboardingAnalyzeRequest(
                "스트리머용으로 생각 중이야",
                null,
                null,
                List.of(),
                null,
                first.historyId(),
                null
        ));

        assertEquals(first.historyId(), followUp.parentHistoryId());
        assertTrue(llmClient.lastUserPrompt().contains("이전 분석 맥락"));
        assertTrue(llmClient.lastUserPrompt().contains("웹캠이랑 TTS 활용한 게임 만들고 싶어"));
        assertTrue(llmClient.lastUserPrompt().contains("현재 후속 질문"));
        assertTrue(llmClient.lastUserPrompt().contains("스트리머용으로 생각 중이야"));
        assertTrue(llmClient.lastUserPrompt().contains("webcam"));
        assertTrue(llmClient.lastUserPrompt().contains("tts"));
    }

    @Test
    void findHistory_returnsSavedAnalyzeResult() {
        CountingLlmClient llmClient = new CountingLlmClient("## 분석 요약\n저장된 리포트입니다.");
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService onboardingService = newService(llmClient, historyRepository);

        OnboardingAnalyzeResponse analyzeResponse = onboardingService.analyze(request());

        OnboardingHistoryDetailResponse detail = onboardingService.findHistory(analyzeResponse.historyId());

        assertEquals(analyzeResponse.historyId(), detail.id());
        assertEquals("PC", detail.targetPlatform());
        assertEquals(List.of("webcam", "tts", "stt"), detail.preferredFeatures());
        assertEquals(3, detail.recommendedConcepts().size());
        assertEquals("## 분석 요약\n저장된 리포트입니다.", detail.report());
    }

    @Test
    void findHistories_returnsSavedResultsInRecentOrderAndDeleteRemovesHistory() {
        InMemoryHistoryRepository historyRepository = new InMemoryHistoryRepository();
        OnboardingService firstService = newService(new CountingLlmClient("첫 번째 리포트"), historyRepository);
        OnboardingService secondService = newService(new CountingLlmClient("두 번째 리포트"), historyRepository);

        OnboardingAnalyzeResponse first = firstService.analyze(request());
        OnboardingAnalyzeResponse second = secondService.analyze(request());

        List<OnboardingHistoryItemResponse> histories = secondService.findHistories();

        assertEquals(2, histories.size());
        assertEquals(second.historyId(), histories.get(0).id());
        assertEquals(first.historyId(), histories.get(1).id());
        assertEquals(3, histories.get(0).recommendedConceptCount());

        secondService.deleteHistory(first.historyId());

        assertEquals(1, historyRepository.count());
        assertEquals(second.historyId(), secondService.findHistories().get(0).id());
    }

    private OnboardingService newService(CountingLlmClient llmClient, InMemoryHistoryRepository historyRepository) {
        return new OnboardingService(llmClient, historyRepository.asRepository(), new ObjectMapper());
    }

    private OnboardingService authenticatedAwareService(
            CountingLlmClient llmClient,
            InMemoryHistoryRepository historyRepository,
            ConversationService conversationService
    ) {
        PlatformFilterResolver platformFilterResolver = new PlatformFilterResolver();
        NonGameCategoryFilter nonGameCategoryFilter = new NonGameCategoryFilter();
        return new OnboardingService(
                llmClient,
                historyRepository.asRepository(),
                null,
                null,
                platformFilterResolver,
                nonGameCategoryFilter,
                new AgentQueryConditionResolver(platformFilterResolver, nonGameCategoryFilter),
                null,
                null,
                null,
                null,
                null,
                new CurrentUserService(),
                conversationService,
                new ObjectMapper()
        );
    }

    private TrendGameResponse trendGame(
            String title,
            String genre,
            double trendScore,
            int steamReviewScore,
            int twitchViewerCount,
            int streamabilityScore,
            int marketSignalScore
    ) {
        return new TrendGameResponse(
                1L,
                title,
                genre,
                "PC",
                null,
                steamReviewScore,
                10_000,
                0.88,
                320,
                twitchViewerCount,
                80,
                70,
                streamabilityScore,
                marketSignalScore,
                76.0,
                trendScore,
                "COMPLETE",
                "테스트 트렌드 근거입니다.",
                LocalDateTime.now()
        );
    }

    private LiveTrendGameResponse liveTrendGame(
            String title,
            String source,
            String genre,
            int totalViewerCount,
            int liveStreamCount,
            double trendScore,
            String signalStatus,
            String dataOrigin
    ) {
        return new LiveTrendGameResponse(
                1L,
                source,
                title,
                genre,
                "PC",
                title,
                liveStreamCount,
                totalViewerCount,
                88,
                84,
                91,
                86,
                trendScore,
                signalStatus,
                dataOrigin,
                "테스트 라이브 트렌드 근거입니다.",
                LocalDateTime.now()
        );
    }

    private OnboardingAnalyzeRequest request() {
        return new OnboardingAnalyzeRequest(
                "요즘 스트리머들이 하기 좋고 시청자 반응이 잘 나오는 게임을 만들고 싶어요.",
                "PC",
                "small",
                List.of("webcam", "tts", "stt"),
                "3 months"
        );
    }

    private static class CountingLlmClient implements LlmClient {

        private final String response;
        private final RuntimeException exception;
        private int callCount;
        private String lastSystemPrompt;
        private String lastUserPrompt;

        CountingLlmClient(String response) {
            this.response = response;
            this.exception = null;
        }

        CountingLlmClient(RuntimeException exception) {
            this.response = null;
            this.exception = exception;
        }

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            callCount++;
            lastSystemPrompt = systemPrompt;
            lastUserPrompt = userPrompt;
            if (exception != null) {
                throw exception;
            }
            return response;
        }

        int callCount() {
            return callCount;
        }

        String lastSystemPrompt() {
            return lastSystemPrompt;
        }

        String lastUserPrompt() {
            return lastUserPrompt;
        }
    }

    private static class FakeLiveTrendService extends LiveTrendService {

        private final List<LiveTrendGameResponse> games;
        private String lastPlatform;

        FakeLiveTrendService(List<LiveTrendGameResponse> games) {
            super(null, null, null, null, List.of());
            this.games = games;
        }

        @Override
        public List<LiveTrendGameResponse> findTopLiveTrendGames(int limit) {
            lastPlatform = "ALL";
            return games.stream()
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<LiveTrendGameResponse> findTopLiveTrendGames(int limit, String platform) {
            lastPlatform = platform;
            return games.stream()
                    .filter(game -> platform == null
                            || platform.isBlank()
                            || "ALL".equalsIgnoreCase(platform)
                            || platform.equalsIgnoreCase(game.source()))
                    .limit(limit)
                    .toList();
        }

        String lastPlatform() {
            return lastPlatform;
        }
    }

    private static class ThrowingConversationService extends ConversationService {

        private boolean resolveCalled;
        private boolean appendCalled;

        ThrowingConversationService() {
            super(null, null, new ObjectMapper());
        }

        @Override
        public Conversation resolveForAnalyze(Long userId, Long conversationId, String requestedSessionId, String message) {
            resolveCalled = true;
            throw new AssertionError("비로그인 분석에서는 Conversation을 생성하거나 조회하면 안 됩니다.");
        }

        @Override
        public void appendExchange(
                Conversation conversation,
                String userMessage,
                String assistantAnswer,
                String intent,
                List<com.gametrend.agent.onboarding.dto.EvidenceCardResponse> evidenceCards
        ) {
            appendCalled = true;
            throw new AssertionError("비로그인 분석에서는 ConversationMessage를 저장하면 안 됩니다.");
        }

        boolean resolveCalled() {
            return resolveCalled;
        }

        boolean appendCalled() {
            return appendCalled;
        }
    }

    private static class InMemoryHistoryRepository {

        private final List<OnboardingAnalysisHistory> histories = new ArrayList<>();
        private long nextId = 1L;

        OnboardingAnalysisHistoryRepository asRepository() {
            return (OnboardingAnalysisHistoryRepository) Proxy.newProxyInstance(
                    OnboardingAnalysisHistoryRepository.class.getClassLoader(),
                    new Class<?>[]{OnboardingAnalysisHistoryRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "save" -> save((OnboardingAnalysisHistory) args[0]);
                        case "findAllByOrderByCreatedAtDesc" -> findAllByOrderByCreatedAtDesc();
                        case "findById" -> findById((Long) args[0]);
                        case "findFirstByConversationIdOrderByCreatedAtDesc" -> findFirstByConversationIdOrderByCreatedAtDesc((String) args[0]);
                        case "existsById" -> existsById((Long) args[0]);
                        case "deleteById" -> {
                            deleteById((Long) args[0]);
                            yield null;
                        }
                        case "toString" -> "InMemoryHistoryRepository";
                        default -> throw new UnsupportedOperationException(
                                "테스트에서 사용하지 않는 메서드입니다: " + method.getName()
                        );
                    }
            );
        }

        int count() {
            return histories.size();
        }

        private OnboardingAnalysisHistory save(OnboardingAnalysisHistory history) {
            OnboardingAnalysisHistory savedHistory = copyWithId(
                    history,
                    history.getId() == null ? nextId++ : history.getId()
            );
            histories.removeIf(existingHistory -> existingHistory.getId().equals(savedHistory.getId()));
            histories.add(savedHistory);
            return savedHistory;
        }

        private List<OnboardingAnalysisHistory> findAllByOrderByCreatedAtDesc() {
            return histories.stream()
                    .sorted(Comparator.comparing(OnboardingAnalysisHistory::getCreatedAt).reversed())
                    .toList();
        }

        private Optional<OnboardingAnalysisHistory> findById(Long id) {
            return histories.stream()
                    .filter(history -> history.getId().equals(id))
                    .findFirst();
        }

        private Optional<OnboardingAnalysisHistory> findFirstByConversationIdOrderByCreatedAtDesc(String conversationId) {
            return histories.stream()
                    .filter(history -> conversationId.equals(history.getConversationId()))
                    .sorted(Comparator.comparing(OnboardingAnalysisHistory::getCreatedAt).reversed())
                    .findFirst();
        }

        private boolean existsById(Long id) {
            return histories.stream().anyMatch(history -> history.getId().equals(id));
        }

        private void deleteById(Long id) {
            histories.removeIf(history -> history.getId().equals(id));
        }

        private OnboardingAnalysisHistory copyWithId(OnboardingAnalysisHistory history, Long id) {
            return OnboardingAnalysisHistory.builder()
                    .id(id)
                    .parentHistoryId(history.getParentHistoryId())
                    .conversationId(history.getConversationId())
                    .message(history.getMessage())
                    .targetPlatform(history.getTargetPlatform())
                    .teamSize(history.getTeamSize())
                    .preferredFeaturesJson(history.getPreferredFeaturesJson())
                    .developmentPeriod(history.getDevelopmentPeriod())
                    .summary(history.getSummary())
                    .recommendedConceptsJson(history.getRecommendedConceptsJson())
                    .report(history.getReport())
                    .createdAt(history.getCreatedAt())
                    .build();
        }
    }

    private static class FakeTrendGameService extends TrendGameService {

        private final List<TrendGameResponse> trendGames;

        FakeTrendGameService(List<TrendGameResponse> trendGames) {
            super(null, null, null, null);
            this.trendGames = trendGames;
        }

        @Override
        public List<TrendGameResponse> findTrendGames() {
            return trendGames;
        }
    }
}
