package com.gametrend.agent.onboarding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.agent.auth.exception.AuthRequiredException;
import com.gametrend.agent.auth.service.CurrentUser;
import com.gametrend.agent.auth.service.CurrentUserService;
import com.gametrend.agent.conversation.entity.Conversation;
import com.gametrend.agent.conversation.service.ConversationService;
import com.gametrend.agent.gameimage.GameImageResolver;
import com.gametrend.agent.infrastructure.llm.LlmClient;
import com.gametrend.agent.livetrend.dto.LiveTrendGameResponse;
import com.gametrend.agent.livetrend.service.LiveTrendService;
import com.gametrend.agent.onboarding.dto.AgentAnswerDraft;
import com.gametrend.agent.onboarding.dto.AgentEvidenceBundle;
import com.gametrend.agent.onboarding.dto.AgentPlan;
import com.gametrend.agent.onboarding.dto.AgentPlanningContext;
import com.gametrend.agent.onboarding.dto.AgentQueryConditionResponse;
import com.gametrend.agent.onboarding.dto.ConversationMemorySummaryResponse;
import com.gametrend.agent.onboarding.dto.ConversationMemoryUpdateContext;
import com.gametrend.agent.onboarding.dto.EvidenceCardResponse;
import com.gametrend.agent.onboarding.dto.OnboardingAnalyzeRequest;
import com.gametrend.agent.onboarding.dto.OnboardingAnalyzeResponse;
import com.gametrend.agent.onboarding.dto.OnboardingHistoryDetailResponse;
import com.gametrend.agent.onboarding.dto.OnboardingHistoryItemResponse;
import com.gametrend.agent.onboarding.dto.RecommendedConceptResponse;
import com.gametrend.agent.onboarding.entity.OnboardingAnalysisHistory;
import com.gametrend.agent.onboarding.exception.OnboardingHistoryNotFoundException;
import com.gametrend.agent.onboarding.repository.OnboardingAnalysisHistoryRepository;
import com.gametrend.agent.reinterpretation.dto.ReinterpretationCandidateResponse;
import com.gametrend.agent.reinterpretation.service.ReinterpretationCandidateService;
import com.gametrend.agent.trend.dto.TrendGameResponse;
import com.gametrend.agent.trend.service.TrendGameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OnboardingService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private static final TypeReference<List<RecommendedConceptResponse>> RECOMMENDED_CONCEPT_LIST_TYPE =
            new TypeReference<>() {
            };

    private static final String SYSTEM_PROMPT = """
            당신은 GEMMA4 E2B 기반 게임 트렌드 분석 Agent입니다.
            사용자의 자연어 질문을 읽고 게임 추천, 시장 트렌드 분석, 특정 게임 인기 요인 분석,
            게임 개발 가능성 분석, 스트리밍 적합성 분석, 인터랙션 기반 아이디어 추천을 수행합니다.
            응답은 반드시 한국어로 작성합니다.
            마크다운 형식으로 작성하되, 다음 섹션을 포함합니다.
              1. 분석 요약
              2. 사용자 질문에 대한 직접 답변
              3. 추천 방향 또는 컨셉
              4. 시장성, 인기도, 스트리밍 적합성
              5. 다음에 확인할 정보
            개발 질문인 경우에는 구현 난이도와 MVP 범위를 함께 설명합니다.
            플레이어 추천 질문인 경우에는 취향을 단정하지 말고 추천 기준과 추가 질문을 제시합니다.
            과장된 마케팅 문구는 피하고, 사용자의 원문 질문과 입력 조건을 반드시 반영합니다.
            """;

    private final LlmClient llmClient;
    private final OnboardingAnalysisHistoryRepository historyRepository;
    private final TrendGameService trendGameService;
    private final LiveTrendService liveTrendService;
    private final PlatformFilterResolver platformFilterResolver;
    private final NonGameCategoryFilter nonGameCategoryFilter;
    private final AgentQueryConditionResolver queryConditionResolver;
    private final ReinterpretationCandidateService reinterpretationCandidateService;
    private final AgentPlanner agentPlanner;
    private final AgentEvidenceCollector agentEvidenceCollector;
    private final AgentAnswerSynthesizer agentAnswerSynthesizer;
    private final ConversationMemoryService conversationMemoryService;
    private final CurrentUserService currentUserService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    @Autowired
    public OnboardingService(
            LlmClient llmClient,
            OnboardingAnalysisHistoryRepository historyRepository,
            TrendGameService trendGameService,
            LiveTrendService liveTrendService,
            PlatformFilterResolver platformFilterResolver,
            NonGameCategoryFilter nonGameCategoryFilter,
            AgentQueryConditionResolver queryConditionResolver,
            ReinterpretationCandidateService reinterpretationCandidateService,
            AgentPlanner agentPlanner,
            AgentEvidenceCollector agentEvidenceCollector,
            AgentAnswerSynthesizer agentAnswerSynthesizer,
            ConversationMemoryService conversationMemoryService,
            CurrentUserService currentUserService,
            ConversationService conversationService,
            ObjectMapper objectMapper
    ) {
        this.llmClient = llmClient;
        this.historyRepository = historyRepository;
        this.trendGameService = trendGameService;
        this.liveTrendService = liveTrendService;
        this.platformFilterResolver = platformFilterResolver;
        this.nonGameCategoryFilter = nonGameCategoryFilter;
        this.queryConditionResolver = queryConditionResolver;
        this.reinterpretationCandidateService = reinterpretationCandidateService;
        this.agentPlanner = agentPlanner;
        this.agentEvidenceCollector = agentEvidenceCollector;
        this.agentAnswerSynthesizer = agentAnswerSynthesizer;
        this.conversationMemoryService = conversationMemoryService;
        this.currentUserService = currentUserService;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
    }

    OnboardingService(
            LlmClient llmClient,
            OnboardingAnalysisHistoryRepository historyRepository,
            TrendGameService trendGameService,
            LiveTrendService liveTrendService,
            ObjectMapper objectMapper
    ) {
        this(
                llmClient,
                historyRepository,
                trendGameService,
                liveTrendService,
                new PlatformFilterResolver(),
                new NonGameCategoryFilter(),
                new AgentQueryConditionResolver(new PlatformFilterResolver(), new NonGameCategoryFilter()),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                objectMapper
        );
    }

    OnboardingService(
            LlmClient llmClient,
            OnboardingAnalysisHistoryRepository historyRepository,
            TrendGameService trendGameService,
            ObjectMapper objectMapper
    ) {
        this(llmClient, historyRepository, trendGameService, null, objectMapper);
    }

    OnboardingService(
            LlmClient llmClient,
            OnboardingAnalysisHistoryRepository historyRepository,
            ObjectMapper objectMapper
    ) {
        this(llmClient, historyRepository, null, null, objectMapper);
    }

    public OnboardingAnalyzeResponse analyze(OnboardingAnalyzeRequest request) {
        PersistentConversation persistentConversation = resolvePersistentConversation(request);
        OnboardingAnalyzeRequest requestWithConversation = requestWithConversation(request, persistentConversation.conversationKey());
        requireLoginForRestrictedGuestMessage(
                requestWithConversation.message(),
                requestWithConversation.preferredFeatures()
        );
        ConversationContext conversationContext = resolveConversationContext(requestWithConversation);
        OnboardingAnalyzeRequest analysisRequest = buildContextAwareRequest(requestWithConversation, conversationContext);
        String conversationId = resolveConversationId(requestWithConversation, conversationContext);
        String sessionId = resolveSessionId(requestWithConversation, conversationId, persistentConversation);
        ConversationMemorySummaryResponse memoryBefore = findOrCreateMemory(sessionId, persistentConversation.conversationId());
        FollowUpFocus followUpFocus = detectFollowUpFocus(request, conversationContext);
        AgentPlanningContext planningContext = toAgentPlanningContext(conversationContext, memoryBefore);
        AgentPlan agentPlan = planAgent(request, planningContext);
        AgentQueryConditionResponse queryCondition = queryConditionFromPlan(request, agentPlan, conversationContext);
        if (isConversationalOnlyPlan(agentPlan)) {
            return buildConversationalResponse(
                    request,
                    requestWithConversation,
                    conversationContext,
                    persistentConversation,
                    conversationId,
                    sessionId,
                    queryCondition,
                    agentPlan
            );
        }
        Optional<String> selectedPlatform = selectedPlatform(queryCondition);
        requireLoginForRestrictedAnalysis(agentPlan, queryCondition);

        OnboardingIntent latestIntent = applyAgentPlanGenre(
                preserveContextGenreForFollowUp(
                        analyzeIntent(buildLatestIntentRequest(request, analysisRequest)),
                        conversationContext,
                        request.message()
                ),
                agentPlan
        );
        OnboardingIntent intent = alignIntentWithQueryCondition(latestIntent, queryCondition);
        AgentEvidenceBundle agentEvidence = collectAgentEvidence(agentPlan, queryCondition, request.message());
        List<TrendGameResponse> trendSignals = resolveTrendSignals(intent);
        List<LiveTrendGameResponse> liveTrendSignals = agentEvidence.liveTrendGames().isEmpty()
                ? resolveLiveTrendSignals(request, analysisRequest, intent, queryCondition)
                : agentEvidence.liveTrendGames();
        List<ReinterpretationCandidateResponse> reinterpretationCandidates = agentEvidence.reinterpretationCandidates().isEmpty()
                ? resolveReinterpretationCandidates(intent, queryCondition)
                : agentEvidence.reinterpretationCandidates();
        List<RecommendedConceptResponse> concepts = buildFallbackConcepts(analysisRequest, intent, followUpFocus, conversationContext);
        List<EvidenceCardResponse> evidenceCards = !agentEvidence.evidenceCards().isEmpty()
                ? agentEvidence.evidenceCards()
                : usesReinterpretationCandidates(intent, queryCondition)
                ? buildReinterpretationEvidenceCards(reinterpretationCandidates)
                : buildEvidenceCards(intent, concepts, trendSignals, liveTrendSignals, selectedPlatform, queryCondition);
        String ruleSummary = usesReinterpretationCandidates(intent, queryCondition)
                ? buildReinterpretationSummary(queryCondition, reinterpretationCandidates)
                : buildSummary(request, analysisRequest, conversationContext, intent, trendSignals, liveTrendSignals, followUpFocus, selectedPlatform, queryCondition);
        String ruleAnswer = usesReinterpretationCandidates(intent, queryCondition)
                ? buildReinterpretationAnswer(queryCondition, reinterpretationCandidates)
                : buildAnswer(request, analysisRequest, conversationContext, intent, concepts, trendSignals, liveTrendSignals, followUpFocus, selectedPlatform, queryCondition);
        AgentAnswerDraft synthesizedAnswer = synthesizeAgentAnswer(
                request.message(),
                planningContext,
                agentPlan,
                new AgentEvidenceBundle(liveTrendSignals, reinterpretationCandidates, evidenceCards),
                ruleSummary,
                ruleAnswer
        );
        String summary = synthesizedAnswer == null ? ruleSummary : synthesizedAnswer.summary();
        String answer = synthesizedAnswer == null ? ruleAnswer : synthesizedAnswer.answer();
        String report = synthesizedAnswer == null
                ? buildReportWithLlmOrFallback(
                request,
                analysisRequest,
                conversationContext,
                followUpFocus,
                intent,
                summary,
                answer,
                concepts,
                trendSignals,
                liveTrendSignals,
                reinterpretationCandidates,
                selectedPlatform,
                queryCondition,
                evidenceCards
        )
                : synthesizedAnswer.report();
        List<String> followUpQuestions = synthesizedAnswer == null
                ? buildFollowUpQuestions(intent, followUpFocus, conversationContext, liveTrendSignals)
                : synthesizedAnswer.followUpQuestions();
        OnboardingAnalysisHistory history = saveHistoryIfAllowed(
                persistentConversation,
                request,
                analysisRequest,
                conversationContext,
                conversationId,
                summary,
                concepts,
                report
        );
        ConversationMemorySummaryResponse memoryAfter = updateMemory(
                sessionId,
                persistentConversation.conversationId(),
                request.message(),
                agentPlan,
                evidenceCards,
                summary,
                answer
        );
        persistConversationMessages(persistentConversation, request.message(), answer, intent.questionIntent().name(), evidenceCards);

        return new OnboardingAnalyzeResponse(
                history == null ? null : history.getId(),
                summary,
                concepts,
                answer,
                report,
                intent.questionIntent().name(),
                intent.detectedKeywords(),
                followUpQuestions,
                evidenceCards,
                history == null ? null : history.getParentHistoryId(),
                history == null ? persistentConversation.conversationKey() : history.getConversationId(),
                queryCondition,
                agentPlan,
                sessionId,
                memoryAfter
        );
    }

    public List<OnboardingHistoryItemResponse> findHistories(Long currentUserId, boolean admin) {
        List<OnboardingAnalysisHistory> histories = admin
                ? historyRepository.findAllByOrderByCreatedAtDesc()
                : historyRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);
        return histories
                .stream()
                .map(this::toHistoryItemResponse)
                .toList();
    }

    public List<OnboardingHistoryItemResponse> findHistories() {
        return findHistories(null, true);
    }

    public OnboardingHistoryDetailResponse findHistory(Long id, Long currentUserId, boolean admin) {
        OnboardingAnalysisHistory history = loadHistoryForViewer(id, currentUserId, admin);

        return toHistoryDetailResponse(history);
    }

    public OnboardingHistoryDetailResponse findHistory(Long id) {
        return findHistory(id, null, true);
    }

    public void deleteHistory(Long id, Long currentUserId, boolean admin) {
        loadHistoryForViewer(id, currentUserId, admin);
        historyRepository.deleteById(id);
    }

    public void deleteHistory(Long id) {
        deleteHistory(id, null, true);
    }

    private OnboardingAnalysisHistory loadHistoryForViewer(Long id, Long currentUserId, boolean admin) {
        if (admin) {
            return historyRepository.findById(id)
                    .orElseThrow(() -> new OnboardingHistoryNotFoundException(id));
        }
        return historyRepository.findByIdAndUserId(id, currentUserId)
                .orElseThrow(() -> new OnboardingHistoryNotFoundException(id));
    }

    private OnboardingAnalysisHistory loadHistoryForCurrentUser(Long id) {
        if (currentUserService == null) {
            return historyRepository.findById(id)
                    .orElseThrow(() -> new OnboardingHistoryNotFoundException(id));
        }
        Optional<CurrentUser> currentUser = currentUserService.getCurrentUser();
        if (currentUser.isEmpty()) {
            throw new AuthRequiredException("저장된 분석 이력을 이어가려면 로그인이 필요합니다.");
        }
        if (isAdmin(currentUser.get())) {
            return historyRepository.findById(id)
                    .orElseThrow(() -> new OnboardingHistoryNotFoundException(id));
        }
        return historyRepository.findByIdAndUserId(id, currentUser.get().id())
                .orElseThrow(() -> new OnboardingHistoryNotFoundException(id));
    }

    private Optional<OnboardingAnalysisHistory> findLatestConversationHistoryForCurrentUser(String conversationId) {
        if (currentUserService == null) {
            return historyRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId);
        }
        Optional<CurrentUser> currentUser = currentUserService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return Optional.empty();
        }
        if (isAdmin(currentUser.get())) {
            return historyRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId);
        }
        return historyRepository.findFirstByConversationIdAndUserIdOrderByCreatedAtDesc(conversationId, currentUser.get().id());
    }

    private ConversationContext resolveConversationContext(OnboardingAnalyzeRequest request) {
        if (request.parentHistoryId() != null) {
            OnboardingAnalysisHistory parentHistory = loadHistoryForCurrentUser(request.parentHistoryId());
            return toConversationContext(parentHistory);
        }

        String conversationId = stripToNull(request.conversationId());
        if (conversationId == null) {
            return ConversationContext.empty();
        }

        return findLatestConversationHistoryForCurrentUser(conversationId)
                .map(this::toConversationContext)
                .orElseGet(ConversationContext::empty);
    }

    private ConversationContext toConversationContext(OnboardingAnalysisHistory history) {
        return new ConversationContext(
                history.getId(),
                history.getConversationId(),
                history.getMessage(),
                history.getTargetPlatform(),
                history.getTeamSize(),
                readPreferredFeatures(history.getPreferredFeaturesJson()),
                history.getDevelopmentPeriod(),
                history.getSummary(),
                readRecommendedConcepts(history.getRecommendedConceptsJson()),
                history.getReport()
        );
    }

    private OnboardingAnalyzeRequest buildContextAwareRequest(
            OnboardingAnalyzeRequest request,
            ConversationContext conversationContext
    ) {
        return new OnboardingAnalyzeRequest(
                buildContextAwareMessage(request, conversationContext),
                resolveEffectivePlatform(request, conversationContext),
                resolveEffectiveTeamSize(request, conversationContext),
                resolveEffectiveFeatures(request, conversationContext),
                resolveEffectiveDevelopmentPeriod(request, conversationContext),
                request.parentHistoryId(),
                resolveConversationId(request, conversationContext),
                request.sessionId(),
                null
        );
    }

    private OnboardingAnalyzeRequest buildLatestIntentRequest(
            OnboardingAnalyzeRequest request,
            OnboardingAnalyzeRequest analysisRequest
    ) {
        return new OnboardingAnalyzeRequest(
                request.message(),
                analysisRequest.targetPlatform(),
                analysisRequest.teamSize(),
                analysisRequest.preferredFeatures(),
                analysisRequest.developmentPeriod(),
                request.parentHistoryId(),
                request.conversationId(),
                request.sessionId(),
                null
        );
    }

    private AgentQueryConditionResponse resolveQueryCondition(
            OnboardingAnalyzeRequest request,
            ConversationContext conversationContext
    ) {
        if (!conversationContext.hasHistory()) {
            return queryConditionResolver.resolve(request.message(), request.preferredFeatures());
        }

        return queryConditionResolver.resolveFollowUp(
                request.message(),
                request.preferredFeatures(),
                conversationContext.message(),
                conversationContext.preferredFeatures()
        );
    }

    private String resolveSessionId(
            OnboardingAnalyzeRequest request,
            String conversationId,
            PersistentConversation persistentConversation
    ) {
        if (persistentConversation.sessionId() != null) {
            return persistentConversation.sessionId();
        }
        if (persistentConversation.hasConversation() && conversationMemoryService != null) {
            return conversationMemoryService.resolveSessionId(request.sessionId());
        }
        String requestedSessionId = stripToNull(request.sessionId());
        if (requestedSessionId != null) {
            return requestedSessionId;
        }
        return "agent-session-" + UUID.randomUUID();
    }

    private ConversationMemorySummaryResponse findOrCreateMemory(String sessionId, Long conversationId) {
        if (conversationMemoryService == null || conversationId == null) {
            return null;
        }
        return conversationMemoryService.findOrCreate(conversationId, sessionId);
    }

    private ConversationMemorySummaryResponse updateMemory(
            String sessionId,
            Long conversationId,
            String message,
            AgentPlan agentPlan,
            List<EvidenceCardResponse> evidenceCards,
            String summary,
            String answer
    ) {
        if (conversationMemoryService == null || agentPlan == null || conversationId == null) {
            return null;
        }
        return conversationMemoryService.update(new ConversationMemoryUpdateContext(
                sessionId,
                conversationId,
                message,
                agentPlan,
                evidenceCards,
                summary,
                answer
        ));
    }

    private PersistentConversation resolvePersistentConversation(OnboardingAnalyzeRequest request) {
        if (conversationService == null || currentUserService == null) {
            if (stripToNull(request.conversationId()) != null && looksNumeric(request.conversationId())) {
                throw new AuthRequiredException("저장된 대화를 이어가려면 로그인이 필요합니다.");
            }
            return PersistentConversation.none();
        }
        Optional<CurrentUser> currentUser = currentUserService.getCurrentUser();
        Long requestedConversationId = parseConversationId(request.conversationId());
        if (currentUser.isEmpty()) {
            if (stripToNull(request.conversationId()) != null) {
                throw new AuthRequiredException("저장된 대화를 이어가려면 로그인이 필요합니다.");
            }
            if (request.parentHistoryId() != null) {
                throw new AuthRequiredException("저장된 분석 이력을 이어가려면 로그인이 필요합니다.");
            }
            return PersistentConversation.none();
        }
        Conversation conversation = conversationService.resolveForAnalyze(
                currentUser.get().id(),
                requestedConversationId,
                request.sessionId(),
                request.message()
        );
        return PersistentConversation.of(conversation);
    }

    private OnboardingAnalyzeRequest requestWithConversation(
            OnboardingAnalyzeRequest request,
            String conversationId
    ) {
        if (conversationId == null || conversationId.isBlank()) {
            return request;
        }
        return new OnboardingAnalyzeRequest(
                request.message(),
                request.targetPlatform(),
                request.teamSize(),
                request.preferredFeatures(),
                request.developmentPeriod(),
                request.parentHistoryId(),
                conversationId,
                request.sessionId(),
                null
        );
    }

    private void persistConversationMessages(
            PersistentConversation persistentConversation,
            String userMessage,
            String answer,
            String intent,
            List<EvidenceCardResponse> evidenceCards
    ) {
        if (!persistentConversation.hasConversation() || conversationService == null) {
            return;
        }
        conversationService.appendExchange(
                persistentConversation.conversation(),
                userMessage,
                answer,
                intent,
                evidenceCards
        );
    }

    private Long parseConversationId(String conversationId) {
        String value = stripToNull(conversationId);
        if (value == null || !looksNumeric(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean looksNumeric(String value) {
        String normalized = stripToNull(value);
        return normalized != null && normalized.chars().allMatch(Character::isDigit);
    }

    private void requireLoginForRestrictedAnalysis(
            AgentPlan agentPlan,
            AgentQueryConditionResponse queryCondition
    ) {
        if (!requiresAuthenticatedUser(agentPlan, queryCondition)) {
            return;
        }
        if (currentUserService == null || currentUserService.isLoggedIn()) {
            return;
        }
        throw new AuthRequiredException(
                "개발자 분석, 과거 게임 재해석, 웹캠/TTS/STT 게임 아이디어 분석은 로그인 후 사용할 수 있습니다."
        );
    }

    private void requireLoginForRestrictedGuestMessage(
            String message,
            List<String> preferredFeatures
    ) {
        if (currentUserService == null || currentUserService.isLoggedIn()) {
            return;
        }
        if (!isRestrictedGuestMessage(message, preferredFeatures)) {
            return;
        }
        throw new AuthRequiredException(
                "개발자 분석, 과거 게임 재해석, 웹캠/TTS/STT 게임 아이디어 분석은 로그인 후 사용할 수 있습니다."
        );
    }

    private boolean requiresAuthenticatedUser(
            AgentPlan agentPlan,
            AgentQueryConditionResponse queryCondition
    ) {
        if (agentPlan != null) {
            if (matchesRestrictedPurpose(agentPlan.intent())
                    || matchesRestrictedPurpose(agentPlan.analysisPurpose())
                    || agentPlan.needsReinterpretation()
                    || hasInteractionIdeaFeatures(agentPlan.interactionFeatures())) {
                return true;
            }
        }
        if (queryCondition == null) {
            return false;
        }
        return matchesRestrictedPurpose(queryCondition.analysisPurpose())
                || hasInteractionIdeaFeatures(queryCondition.interactionFeatures());
    }

    private boolean matchesRestrictedPurpose(String value) {
        return containsAny(
                normalize(value),
                "developer_market_analysis",
                "game_reinterpretation",
                "interaction_game_idea",
                "development_feasibility",
                "feature_based_idea"
        );
    }

    private boolean hasInteractionIdeaFeatures(List<String> interactionFeatures) {
        if (interactionFeatures == null || interactionFeatures.isEmpty()) {
            return false;
        }
        return interactionFeatures.stream()
                .filter(feature -> feature != null && !feature.isBlank())
                .map(this::normalize)
                .anyMatch(feature -> containsAny(feature, "webcam", "tts", "stt", "웹캠", "카메라", "음성"));
    }

    private boolean isRestrictedGuestMessage(String message, List<String> preferredFeatures) {
        String normalizedMessage = normalize(message);
        boolean developmentQuestion = containsAny(
                normalizedMessage,
                "개발",
                "개발자",
                "만들고 싶은",
                "만들고 싶",
                "만들면",
                "만들 수",
                "만들만",
                "만들 만",
                "기획",
                "시장성",
                "상업성",
                "수익성",
                "프로토타입",
                "mvp",
                "출시",
                "구현",
                "개발 가능",
                "feasibility",
                "prototype",
                "release"
        );
        boolean reinterpretationQuestion = containsAny(
                normalizedMessage,
                "과거 게임",
                "예전 게임",
                "옛날 게임",
                "재해석",
                "다시 만들",
                "리메이크",
                "레트로",
                "현대화"
        );
        boolean interactionIdeaQuestion = containsAny(
                normalizedMessage,
                "웹캠",
                "webcam",
                "tts",
                "stt",
                "음성 인식",
                "마이크",
                "시청자 참여형",
                "채팅으로 조작",
                "채팅 참여",
                "카메라"
        );
        return developmentQuestion
                || reinterpretationQuestion
                || interactionIdeaQuestion
                || hasInteractionIdeaFeatures(preferredFeatures);
    }

    private AgentPlanningContext toAgentPlanningContext(
            ConversationContext conversationContext,
            ConversationMemorySummaryResponse memorySummary
    ) {
        if (conversationContext == null || !conversationContext.hasHistory()) {
            return new AgentPlanningContext(null, null, null, List.of(), memorySummary);
        }
        return new AgentPlanningContext(
                conversationContext.message(),
                conversationContext.summary(),
                conversationContext.report(),
                conversationContext.recommendedConcepts() == null
                        ? List.of()
                        : conversationContext.recommendedConcepts().stream()
                        .map(RecommendedConceptResponse::title)
                        .toList(),
                memorySummary
        );
    }

    private AgentPlan planAgent(OnboardingAnalyzeRequest request, AgentPlanningContext planningContext) {
        if (agentPlanner != null) {
            return agentPlanner.plan(request.message(), request.preferredFeatures(), planningContext);
        }
        AgentQueryConditionResponse fallbackCondition = queryConditionResolver.resolveFollowUp(
                request.message(),
                request.preferredFeatures(),
                planningContext.previousMessage(),
                List.of()
        );
        return fallbackAgentPlan(request.message(), fallbackCondition, planningContext);
    }

    private AgentQueryConditionResponse queryConditionFromPlan(
            OnboardingAnalyzeRequest request,
            AgentPlan agentPlan,
            ConversationContext conversationContext
    ) {
        if (agentPlan == null) {
            return resolveQueryCondition(request, conversationContext);
        }
        return new AgentQueryConditionResponse(
                displayValue(agentPlan.platformFilter(), "ALL"),
                displayValue(agentPlan.sortMetric(), "TREND_SCORE"),
                displayValue(agentPlan.analysisPurpose(), agentPlan.intent()),
                agentPlan.interactionFeatures() == null ? List.of() : agentPlan.interactionFeatures(),
                !nonGameCategoryFilter.allowsNonGameCategories(request.message()),
                request.message()
        );
    }

    private AgentEvidenceBundle collectAgentEvidence(
            AgentPlan agentPlan,
            AgentQueryConditionResponse queryCondition,
            String userMessage
    ) {
        if (agentEvidenceCollector == null) {
            return AgentEvidenceBundle.empty();
        }
        return agentEvidenceCollector.collect(agentPlan, queryCondition, userMessage);
    }

    private AgentAnswerDraft synthesizeAgentAnswer(
            String message,
            AgentPlanningContext planningContext,
            AgentPlan agentPlan,
            AgentEvidenceBundle evidence,
            String fallbackSummary,
            String fallbackAnswer
    ) {
        if (agentAnswerSynthesizer == null || agentPlan == null) {
            return null;
        }
        return agentAnswerSynthesizer.synthesize(message, planningContext, agentPlan, evidence, fallbackSummary, fallbackAnswer);
    }

    private boolean isConversationalOnlyPlan(AgentPlan agentPlan) {
        if (agentPlan == null) {
            return false;
        }
        return isConversationalIntent(agentPlan.intent()) || isConversationalIntent(agentPlan.analysisPurpose());
    }

    private boolean isConversationalIntent(String intent) {
        return switch (normalize(intent).toUpperCase(Locale.ROOT)) {
            case "SMALL_TALK", "GREETING", "HELP", "OUT_OF_SCOPE" -> true;
            default -> false;
        };
    }

    private OnboardingAnalyzeResponse buildConversationalResponse(
            OnboardingAnalyzeRequest request,
            OnboardingAnalyzeRequest analysisRequest,
            ConversationContext conversationContext,
            PersistentConversation persistentConversation,
            String conversationId,
            String sessionId,
            AgentQueryConditionResponse queryCondition,
            AgentPlan agentPlan
    ) {
        String intent = conversationalIntent(agentPlan);
        String summary = conversationalSummary(intent);
        String answer = conversationalAnswer(intent);
        String report = conversationalReport(summary, answer);
        List<String> detectedKeywords = conversationalDetectedKeywords(intent);
        List<String> followUpQuestions = conversationalFollowUpQuestions(intent);
        List<RecommendedConceptResponse> concepts = List.of();
        List<EvidenceCardResponse> evidenceCards = List.of();
        OnboardingAnalysisHistory history = saveHistoryIfAllowed(
                persistentConversation,
                request,
                analysisRequest,
                conversationContext,
                conversationId,
                summary,
                concepts,
                report
        );
        ConversationMemorySummaryResponse memoryAfter = updateMemory(
                sessionId,
                persistentConversation.conversationId(),
                request.message(),
                agentPlan,
                evidenceCards,
                summary,
                answer
        );
        persistConversationMessages(persistentConversation, request.message(), answer, intent, evidenceCards);

        return new OnboardingAnalyzeResponse(
                history == null ? null : history.getId(),
                summary,
                concepts,
                answer,
                report,
                intent,
                detectedKeywords,
                followUpQuestions,
                evidenceCards,
                history == null ? null : history.getParentHistoryId(),
                history == null ? persistentConversation.conversationKey() : history.getConversationId(),
                queryCondition,
                agentPlan,
                sessionId,
                memoryAfter
        );
    }

    private OnboardingAnalysisHistory saveHistoryIfAllowed(
            PersistentConversation persistentConversation,
            OnboardingAnalyzeRequest request,
            OnboardingAnalyzeRequest analysisRequest,
            ConversationContext conversationContext,
            String conversationId,
            String summary,
            List<RecommendedConceptResponse> concepts,
            String report
    ) {
        if (!shouldPersistAnalysis(persistentConversation)) {
            return null;
        }
        return historyRepository.save(toHistoryEntity(
                persistentConversation,
                request,
                analysisRequest,
                conversationContext,
                conversationId,
                summary,
                concepts,
                report
        ));
    }

    private boolean shouldPersistAnalysis(PersistentConversation persistentConversation) {
        if (currentUserService == null) {
            return true;
        }
        return persistentConversation != null && persistentConversation.hasConversation();
    }

    private boolean isAdmin(CurrentUser currentUser) {
        String role = currentUser == null ? null : currentUser.role();
        return "ADMIN".equalsIgnoreCase(role) || "OWNER".equalsIgnoreCase(role);
    }

    private String conversationalIntent(AgentPlan agentPlan) {
        if (agentPlan == null) {
            return "SMALL_TALK";
        }
        String intent = normalize(agentPlan.intent()).toUpperCase(Locale.ROOT);
        if (isConversationalIntent(intent)) {
            return intent;
        }
        String analysisPurpose = normalize(agentPlan.analysisPurpose()).toUpperCase(Locale.ROOT);
        return isConversationalIntent(analysisPurpose) ? analysisPurpose : "SMALL_TALK";
    }

    private String conversationalSummary(String intent) {
        return switch (intent) {
            case "GREETING" -> "인사 메시지입니다. 분석 도구를 호출하지 않고 짧게 안내합니다.";
            case "HELP" -> "도움말 요청입니다. Agent가 할 수 있는 일을 간단히 안내합니다.";
            case "OUT_OF_SCOPE" -> "게임 분석 범위를 벗어난 질문입니다. 가능한 게임 분석 질문으로 부드럽게 안내합니다.";
            default -> "일상 대화입니다. 분석 도구를 호출하지 않고 자연스럽게 응답합니다.";
        };
    }

    private String conversationalAnswer(String intent) {
        return switch (intent) {
            case "GREETING" -> "안녕하세요! 게임 추천, 실시간 인기 게임, 방송에서 뜨는 게임, 개발 아이디어까지 편하게 물어보세요.";
            case "HELP" -> "저는 게임 트렌드 분석 Agent예요. 요즘 할만한 게임 추천, Twitch/CHZZK 라이브 트렌드, 개발자가 참고할 장르, 과거 게임 재해석 아이디어를 도와드릴 수 있어요.";
            case "OUT_OF_SCOPE" -> "저는 게임 트렌드와 게임 개발 아이디어를 중심으로 돕는 Agent예요. 요즘 인기 게임, 방송에서 뜨는 게임, 개발할 만한 장르처럼 물어보면 바로 분석해볼게요.";
            default -> "좋아요. 저는 게임 트렌드와 추천을 도와드릴 수 있어요. 궁금한 게임, 플랫폼, 개발 아이디어가 있으면 편하게 물어보세요.";
        };
    }

    private String conversationalReport(String summary, String answer) {
        return """
                ## 요약
                %s

                ## 답변
                %s
                """.formatted(summary, answer);
    }

    private List<String> conversationalDetectedKeywords(String intent) {
        return switch (intent) {
            case "GREETING" -> List.of("인사");
            case "HELP" -> List.of("도움말", "사용법");
            case "OUT_OF_SCOPE" -> List.of("범위 외 질문");
            default -> List.of("일상 대화");
        };
    }

    private List<String> conversationalFollowUpQuestions(String intent) {
        if ("HELP".equals(intent)) {
            return List.of(
                    "요즘 할만한 게임 추천해줘",
                    "방송에서 뜨는 게임 알려줘",
                    "개발자 관점으로 인기 장르 알려줘"
            );
        }
        return List.of(
                "요즘 할만한 게임 추천해줘",
                "방송에서 뜨는 게임 알려줘",
                "친구랑 할 게임으로 추천해줘"
        );
    }

    private AgentPlan fallbackAgentPlan(
            String message,
            AgentQueryConditionResponse condition,
            AgentPlanningContext planningContext
    ) {
        String purpose = condition.analysisPurpose();
        String resolvedTopic = "";
        if (containsAny(normalize(message), "그거", "그 게임", "네가 알려준", "아까", "그 후보", "그걸", "아까 말한 게임", "방금 추천한 것", "그 기준", "위에 말한 것")) {
            resolvedTopic = resolveTopicFromMemory(purpose, planningContext);
        }
        return new AgentPlan(
                purpose,
                switch (purpose) {
                    case "USER_GAME_RECOMMENDATION" -> "PLAYER";
                    case "DEVELOPER_MARKET_ANALYSIS", "GAME_REINTERPRETATION", "INTERACTION_GAME_IDEA" -> "DEVELOPER";
                    case "STREAMING_FIT_ANALYSIS" -> "STREAMER";
                    default -> "UNKNOWN";
                },
                condition.platformFilter(),
                extractAgentGenreFilter(message),
                condition.sortMetric(),
                purpose,
                condition.interactionFeatures(),
                List.of("TREND_ANALYSIS", "USER_GAME_RECOMMENDATION", "DEVELOPER_MARKET_ANALYSIS", "STREAMING_FIT_ANALYSIS", "INTERACTION_GAME_IDEA").contains(purpose),
                "GAME_REINTERPRETATION".equals(purpose),
                "USER_GAME_RECOMMENDATION".equals(purpose),
                false,
                resolvedTopic.isBlank() ? "" : "이전 답변 참조",
                resolvedTopic,
                "대화체, 직접 답변",
                0.45,
                "AgentPlanner가 비활성화되어 QueryConditionResolver fallback을 사용했습니다.",
                responseDepthFor(message)
        );
    }

    private String resolveTopicFromMemory(String purpose, AgentPlanningContext planningContext) {
        if (planningContext.memorySummary() != null) {
            String recommendedTopic = firstFrom(planningContext.memorySummary().recommendedGames());
            if (recommendedTopic != null && "USER_GAME_RECOMMENDATION".equals(planningContext.memorySummary().lastIntent())) {
                return recommendedTopic;
            }
            if ("DEVELOPER_MARKET_ANALYSIS".equals(purpose) || "GAME_REINTERPRETATION".equals(purpose)) {
                String developerTopic = firstFrom(planningContext.memorySummary().developerCandidates());
                if (developerTopic != null) {
                    return developerTopic;
                }
                String reinterpretationTopic = firstFrom(planningContext.memorySummary().reinterpretationCandidates());
                if (reinterpretationTopic != null) {
                    return reinterpretationTopic;
                }
            }
            if (recommendedTopic != null) {
                return recommendedTopic;
            }
        }
        if (planningContext.previousConceptTitles() != null && !planningContext.previousConceptTitles().isEmpty()) {
            return planningContext.previousConceptTitles().get(0);
        }
        return "";
    }

    private String firstFrom(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String buildContextAwareMessage(
            OnboardingAnalyzeRequest request,
            ConversationContext conversationContext
    ) {
        if (!conversationContext.hasHistory()) {
            return request.message();
        }

        return """
                이전 질문: %s
                이전 요약: %s
                현재 후속 질문: %s
                """.formatted(
                displayValue(conversationContext.message(), "이전 질문 없음"),
                displayValue(conversationContext.summary(), "이전 요약 없음"),
                request.message()
        );
    }

    private String resolveEffectivePlatform(
            OnboardingAnalyzeRequest request,
            ConversationContext conversationContext
    ) {
        String message = normalize(request.message());
        if (containsAny(message, "모바일", "mobile")) {
            return "Mobile";
        }
        if (containsAny(message, "웹", "web")) {
            return "Web";
        }
        if (containsAny(message, "pc", "피씨", "스팀", "steam")) {
            return "PC";
        }
        if (containsAny(message, "콘솔", "console", "플스", "playstation", "xbox")) {
            return "Console";
        }
        return firstNonBlank(request.targetPlatform(), conversationContext.targetPlatform());
    }

    private String resolveEffectiveTeamSize(
            OnboardingAnalyzeRequest request,
            ConversationContext conversationContext
    ) {
        String message = normalize(request.message());
        if (containsAny(message, "혼자", "1인", "개인", "solo")) {
            return "solo";
        }
        if (containsAny(message, "소규모", "작은 팀", "small", "인디팀")) {
            return "small";
        }
        if (containsAny(message, "중간 규모", "medium")) {
            return "medium";
        }
        return firstNonBlank(request.teamSize(), conversationContext.teamSize());
    }

    private String resolveEffectiveDevelopmentPeriod(
            OnboardingAnalyzeRequest request,
            ConversationContext conversationContext
    ) {
        String message = normalize(request.message());
        if (containsAny(message, "1개월", "한 달", "1 month")) {
            return "1 month";
        }
        if (containsAny(message, "3개월", "세 달", "3 months")) {
            return "3 months";
        }
        if (containsAny(message, "6개월", "반년", "6 months")) {
            return "6 months";
        }
        if (containsAny(message, "12개월", "1년", "12 months", "1 year")) {
            return "12 months";
        }
        return firstNonBlank(request.developmentPeriod(), conversationContext.developmentPeriod());
    }

    private List<String> resolveEffectiveFeatures(
            OnboardingAnalyzeRequest request,
            ConversationContext conversationContext
    ) {
        LinkedHashSet<String> features = new LinkedHashSet<>(resolvePreferredFeatures(conversationContext.preferredFeatures()));
        features.addAll(resolvePreferredFeatures(request.preferredFeatures()));

        String message = normalize(request.message());
        if (containsAny(message, "웹캠", "webcam", "표정")) {
            features.add("webcam");
        }
        if (containsAny(message, "tts", "채팅", "chat")) {
            features.add("tts");
        }
        if (containsAny(message, "stt", "음성", "마이크", "voice", "말로")) {
            features.add("stt");
        }
        return List.copyOf(features);
    }

    private String resolveConversationId(
            OnboardingAnalyzeRequest request,
            ConversationContext conversationContext
    ) {
        String requestedConversationId = stripToNull(request.conversationId());
        if (requestedConversationId != null) {
            return requestedConversationId;
        }
        if (conversationContext.hasHistory() && stripToNull(conversationContext.conversationId()) != null) {
            return conversationContext.conversationId().strip();
        }
        if (conversationContext.hasHistory()) {
            return "history-" + conversationContext.historyId();
        }
        return "conv-" + UUID.randomUUID();
    }

    private String firstNonBlank(String currentValue, String fallbackValue) {
        String current = stripToNull(currentValue);
        if (current != null) {
            return current;
        }
        return stripToNull(fallbackValue);
    }

    private FollowUpFocus detectFollowUpFocus(
            OnboardingAnalyzeRequest request,
            ConversationContext conversationContext
    ) {
        String message = normalize(request.message());
        boolean prototypeRequested = containsAny(message, "프로토타입", "prototype", "시제품", "검증용");
        boolean releaseMvpRequested = containsAny(message, "출시 가능한", "출시용", "출시", "mvp", "상용", "릴리즈");
        if (prototypeRequested && releaseMvpRequested) {
            return FollowUpFocus.SCOPE_DECISION;
        }
        if (prototypeRequested) {
            return FollowUpFocus.PROTOTYPE_SCOPE;
        }
        if (releaseMvpRequested) {
            return FollowUpFocus.RELEASE_MVP_SCOPE;
        }
        if (platformFilterResolver.resolve(request.message()).isPresent()
                || containsAny(message, "전체 기준", "전체로", "전체 데이터")) {
            return FollowUpFocus.MARKET_TREND;
        }
        if (containsAny(message, "혼자", "1인", "개인", "solo")) {
            return FollowUpFocus.SOLO_DEVELOPMENT;
        }
        if (isFriendPlayRequest(message)) {
            return FollowUpFocus.PLAYER_RECOMMENDATION;
        }
        if (containsAny(message, "스트리머", "방송", "유튜브", "시청자", "클립")) {
            return FollowUpFocus.STREAMER_TARGET;
        }
        if (containsAny(message, "모바일", "mobile")) {
            return FollowUpFocus.MOBILE_PLATFORM;
        }
        if (containsAny(message, "웹캠", "webcam", "tts", "stt", "음성", "채팅")) {
            return FollowUpFocus.FEATURE_SCOPE;
        }
        if (containsAny(message, "트렌드", "인기", "시장", "steam", "twitch", "치지직", "chzzk", "soop", "아프리카")) {
            return FollowUpFocus.MARKET_TREND;
        }
        if (containsAny(message, "추천", "할만한", "플레이", "즐길")) {
            return FollowUpFocus.PLAYER_RECOMMENDATION;
        }
        if (conversationContext.hasHistory()) {
            return FollowUpFocus.CONTEXT_REFINEMENT;
        }
        return FollowUpFocus.GENERAL;
    }

    private String buildReportWithLlmOrFallback(
            OnboardingAnalyzeRequest request,
            OnboardingAnalyzeRequest analysisRequest,
            ConversationContext conversationContext,
            FollowUpFocus followUpFocus,
            OnboardingIntent intent,
            String summary,
            String answer,
            List<RecommendedConceptResponse> concepts,
            List<TrendGameResponse> trendSignals,
            List<LiveTrendGameResponse> liveTrendSignals,
            List<ReinterpretationCandidateResponse> reinterpretationCandidates,
            Optional<String> selectedPlatform,
            AgentQueryConditionResponse queryCondition,
            List<EvidenceCardResponse> evidenceCards
    ) {
        try {
            String content = llmClient.complete(SYSTEM_PROMPT, buildUserPrompt(
                    request,
                    analysisRequest,
                    conversationContext,
                    followUpFocus,
                    intent,
                    summary,
                    answer,
                    concepts,
                    trendSignals,
                    liveTrendSignals,
                    reinterpretationCandidates,
                    selectedPlatform,
                    queryCondition,
                    evidenceCards
            ));
            if (content == null || content.isBlank()) {
                log.warn("온보딩 LLM 응답이 비어 있어 fallback 리포트로 대체합니다.");
                return buildFallbackReport(request, analysisRequest, conversationContext, followUpFocus, intent, summary, answer, concepts, trendSignals, liveTrendSignals, reinterpretationCandidates, selectedPlatform, queryCondition, evidenceCards);
            }
            return content.trim();
        } catch (RuntimeException ex) {
            log.warn("온보딩 LLM 분석 실패, fallback 리포트로 대체합니다. cause={}", ex.toString());
            return buildFallbackReport(request, analysisRequest, conversationContext, followUpFocus, intent, summary, answer, concepts, trendSignals, liveTrendSignals, reinterpretationCandidates, selectedPlatform, queryCondition, evidenceCards);
        }
    }

    private String buildUserPrompt(
            OnboardingAnalyzeRequest request,
            OnboardingAnalyzeRequest analysisRequest,
            ConversationContext conversationContext,
            FollowUpFocus followUpFocus,
            OnboardingIntent intent,
            String summary,
            String answer,
            List<RecommendedConceptResponse> concepts,
            List<TrendGameResponse> trendSignals,
            List<LiveTrendGameResponse> liveTrendSignals,
            List<ReinterpretationCandidateResponse> reinterpretationCandidates,
            Optional<String> selectedPlatform,
            AgentQueryConditionResponse queryCondition,
            List<EvidenceCardResponse> evidenceCards
    ) {
        String conceptLines = concepts.stream()
                .map(concept -> "- %s | 장르=%s | streamability=%d | marketSignal=%d | devFeasibility=%d | 근거=%s"
                        .formatted(
                                concept.title(),
                                concept.genre(),
                                concept.streamabilityScore(),
                                concept.marketSignalScore(),
                                concept.devFeasibilityScore(),
                                concept.reason()
                        ))
                .collect(Collectors.joining(System.lineSeparator()));

        return """
                사용자 요청:
                %s

                이전 분석 맥락:
                %s

                입력 조건:
                - 목표 플랫폼: %s
                - 팀 규모: %s
                - 선호 기능: %s
                - 개발 기간: %s

                추출된 장르/의도:
                - 질문 의도: %s
                - 주 장르: %s
                - 감지된 키워드: %s
                - 플랫폼 해석: %s
                - 개발 범위: %s
                - 현재 분석 관점: %s
                - 사용자 관점: %s
                - selectedPlatform=%s
                - sortMetric=%s
                - analysisPurpose=%s
                - interactionFeatures=%s
                - excludeNonGameCategories=%s

                시스템이 규칙 기반으로 먼저 해석한 요약:
                %s

                사용자 질문에 대한 직접 답변 초안:
                %s

                추천 컨셉 후보:
                %s

                저장된 트렌드 시그널:
                %s

                liveTrendCandidates:
                %s

                reinterpretationCandidates:
                %s

                근거 카드 데이터:
                %s

                위 정보를 바탕으로 프론트 온보딩 화면에 바로 표시할 수 있는 분석 리포트를 작성해 주세요.
                반드시 사용자의 원문 질문에 직접 답하고, 입력 조건과 다르게 일반론으로 흐르지 마세요.
                parentHistoryId가 있는 후속 요청이라면 이전 질문의 맥락을 유지하되, 현재 질문의 관점 변화에 직접 답하세요.
                현재 질문이 선택지형 문장이라면 선택지를 되묻지 말고, 각 선택지의 차이와 추천 선택을 바로 비교해 주세요.
                이전 답변의 핵심 문장을 그대로 반복하지 말고, 현재 질문에 맞춰 판단 기준과 추천 컨셉을 재구성하세요.
                트렌드 분석과 게임 추천은 현재 수집된 라이브 트렌드의 시청자 수, 방송 수, trendScore를 우선 근거로 반영해 주세요.
                라이브 트렌드 데이터가 있으면 반드시 "현재 수집된 라이브 트렌드 기준"이라는 표현을 사용하고,
                source=TWITCH는 "Twitch 기준", source=CHZZK는 "CHZZK 기준"처럼 플랫폼 기준을 명확히 밝혀 주세요.
                dataOrigin=REAL은 "실제 수집 데이터 기준"으로 표현하고, signalStatus=PARTIAL 또는 dataOrigin=FALLBACK이면 "부분 수집 데이터는 보조 신호로만 해석"한다고 조심스럽게 설명해 주세요.
                라이브 트렌드 데이터가 부족하면 "아직 수집된 라이브 트렌드 데이터가 부족합니다. /live-trends에서 수동 갱신을 먼저 실행해주세요."라고 안내한 뒤 일반 분석으로 보완해 주세요.
                analysisPurpose=GAME_REINTERPRETATION이면 반드시 "과거 게임 재해석 관점"이라는 표현을 사용하고,
                원작을 그대로 베끼는 방식이 아니라 핵심 메커니즘을 현대 스트리밍/시청자 참여 환경에 맞게 재해석하는 방향으로 답해 주세요.
                재해석 후보가 있으면 원작 게임명, 원작 장르/태그, 재해석 컨셉, reinterpretationScore, 추천 이유, Webcam/TTS/STT 적용 아이디어를 근거로 사용해 주세요.
                개인/소규모 팀이 만들 수 있는 구현 범위, 방송 반응성, 시청자 참여성, 개발 가능성을 함께 설명해 주세요.
                재해석 후보가 없으면 "아직 과거 게임 재해석 후보 데이터가 부족합니다. /api/legacy-games/refresh를 먼저 실행해주세요."라고 안내해 주세요.
                근거 카드 데이터가 있으면 답변의 핵심 근거로 사용하고, 숫자를 과장하지 마세요.
                각 추천 컨셉의 장점과 리스크를 함께 설명하고, 개발 질문이라면 첫 MVP에서 구현할 기능 범위를 구체적으로 제안해 주세요.
                마지막에는 follow-up을 질문형 문장이 아니라 클릭 즉시 분석 가능한 요청 문장으로 제안해 주세요.
                """.formatted(
                request.message(),
                buildConversationContextPromptSection(conversationContext, request),
                displayValue(analysisRequest.targetPlatform(), "미정"),
                displayValue(analysisRequest.teamSize(), "미정"),
                displayFeatures(analysisRequest.preferredFeatures()),
                displayValue(analysisRequest.developmentPeriod(), "미정"),
                intent.questionIntent().name(),
                intent.primaryGenre().displayName(),
                String.join(", ", intent.detectedKeywords()),
                intent.platform(),
                intent.scopeLabel(),
                followUpFocus.displayName(),
                resolveUserPerspective(intent, followUpFocus),
                selectedPlatformValue(selectedPlatform),
                queryCondition.sortMetric(),
                queryCondition.analysisPurpose(),
                displayQueryConditionFeatures(queryCondition),
                queryCondition.excludeNonGameCategories(),
                summary,
                answer,
                conceptLines,
                buildTrendSignalLines(trendSignals),
                buildLiveTrendSignalLines(liveTrendSignals, selectedPlatform),
                buildReinterpretationCandidateLines(reinterpretationCandidates),
                buildEvidenceCardLines(evidenceCards)
        );
    }

    private String buildConversationContextPromptSection(
            ConversationContext conversationContext,
            OnboardingAnalyzeRequest request
    ) {
        if (!conversationContext.hasHistory()) {
            return "- 신규 분석입니다. 이전 분석 맥락은 없습니다.";
        }

        return """
                - 이전 historyId: %d
                - conversationId: %s
                - 이전 질문: %s
                - 현재 후속 질문: %s
                - 이전 요약: %s
                - 이전 추천 컨셉: %s
                - 이전 리포트 발췌: %s
                """.formatted(
                conversationContext.historyId(),
                displayValue(conversationContext.conversationId(), "미정"),
                displayValue(conversationContext.message(), "이전 질문 없음"),
                request.message(),
                displayValue(conversationContext.summary(), "이전 요약 없음"),
                buildConceptTitleSummary(conversationContext.recommendedConcepts()),
                truncateForPrompt(conversationContext.report(), 900)
        );
    }

    private String buildConceptTitleSummary(List<RecommendedConceptResponse> concepts) {
        if (concepts == null || concepts.isEmpty()) {
            return "이전 추천 컨셉 없음";
        }

        return concepts.stream()
                .limit(3)
                .map(concept -> "%s(%s)".formatted(concept.title(), concept.genre()))
                .collect(Collectors.joining(", "));
    }

    private String resolveUserPerspective(OnboardingIntent intent, FollowUpFocus followUpFocus) {
        if (followUpFocus == FollowUpFocus.PLAYER_RECOMMENDATION
                || intent.questionIntent() == QuestionIntent.GAME_RECOMMENDATION) {
            return "플레이할 게임 추천";
        }
        if (followUpFocus == FollowUpFocus.MARKET_TREND
                || intent.questionIntent() == QuestionIntent.TREND_ANALYSIS) {
            return "게임 시장/트렌드 분석";
        }
        if (intent.questionIntent() == QuestionIntent.GAME_REINTERPRETATION) {
            return "과거 게임 재해석 인사이트";
        }
        if (followUpFocus == FollowUpFocus.STREAMER_TARGET
                || intent.questionIntent() == QuestionIntent.STREAMING_FIT_ANALYSIS) {
            return "스트리머/방송 적합성 분석";
        }
        if (intent.questionIntent() == QuestionIntent.SPECIFIC_GAME_ANALYSIS) {
            return "특정 게임 인기와 유사작 가능성 분석";
        }
        return "게임 개발 가능성 분석";
    }

    private String truncateForPrompt(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "내용 없음";
        }
        String stripped = value.strip();
        if (stripped.length() <= maxLength) {
            return stripped;
        }
        return stripped.substring(0, maxLength) + "...";
    }

    private String buildSummary(
            OnboardingAnalyzeRequest originalRequest,
            OnboardingAnalyzeRequest request,
            ConversationContext conversationContext,
            OnboardingIntent intent,
            List<TrendGameResponse> trendSignals,
            List<LiveTrendGameResponse> liveTrendSignals,
            FollowUpFocus followUpFocus,
            Optional<String> selectedPlatform,
            AgentQueryConditionResponse queryCondition
    ) {
        if (conversationContext.hasHistory()) {
            return buildContextAwareSummary(originalRequest, conversationContext, intent, trendSignals, liveTrendSignals, followUpFocus, selectedPlatform, queryCondition);
        }

        return switch (intent.questionIntent()) {
            case GAME_RECOMMENDATION -> liveTrendSignals.isEmpty()
                    ? "플레이어 관점의 추천 요청입니다. 현재 정보만으로는 %s 기반 후보를 제안하되, 함께 플레이할 사람 수와 선호 난이도에 따라 추천 폭이 달라집니다."
                    .formatted(intent.primaryGenre().displayName())
                    : "현재 수집된 %s 중 %s 기준 플레이 추천 요청입니다. %s 같은 상위 라이브 게임을 우선 근거로 봅니다."
                    .formatted(liveTrendScopeLabel(selectedPlatform), sortMetricLabel(queryCondition.sortMetric()), liveTrendSignals.get(0).title());
            case TREND_ANALYSIS -> buildTrendSummary(intent, trendSignals, liveTrendSignals, selectedPlatform, queryCondition);
            case GAME_REINTERPRETATION -> "과거 게임 재해석 관점의 요청입니다. 원작 메커니즘, 현재 라이브 트렌드 적합도, Webcam/TTS/STT 적용 가능성을 함께 봅니다.";
            case DEVELOPMENT_FEASIBILITY -> liveTrendSignals.isEmpty()
                    ? buildDevelopmentSummary(request, intent)
                    : "개발자 관점에서는 현재 수집된 %s 중 %s 기준 후보를 보고 시장 신호와 방송 확산도를 함께 참고하는 것이 좋습니다. 상위 후보는 %s입니다."
                    .formatted(liveTrendScopeLabel(selectedPlatform), sortMetricLabel(queryCondition.sortMetric()), liveTrendSignals.get(0).title());
            case SPECIFIC_GAME_ANALYSIS -> "특정 게임 분석 요청입니다. 현재성, 인기 지속 가능성, 그리고 유사 게임을 만들 때의 차별화 가능성을 나눠 보는 것이 핵심입니다.";
            case STREAMING_FIT_ANALYSIS -> liveTrendSignals.isEmpty()
                    ? "방송 적합성 분석 요청입니다. %s 스트리머 반응, 시청자 참여, 클립화 가능한 실패/반전 장면을 우선 평가해야 합니다."
                    .formatted(liveTrendMissingDataMessage(selectedPlatform))
                    : "현재 수집된 %s 중 %s 기준 방송 적합성 분석 요청입니다. %s 같은 상위 라이브 게임의 시청자 수와 방송 수를 함께 봐야 합니다."
                    .formatted(liveTrendScopeLabel(selectedPlatform), sortMetricLabel(queryCondition.sortMetric()), liveTrendSignals.get(0).title());
            case FEATURE_BASED_IDEA -> "기능 기반 아이디어 요청입니다. %s 기능을 장식 기능이 아니라 플레이 규칙으로 연결하는지가 성패를 가릅니다."
                    .formatted(displayQueryConditionFeatures(queryCondition));
            case GENERAL_GAME_QUESTION -> "일반 게임 질문입니다. 플레이 추천, 트렌드 분석, 개발 가능성 중 어느 관점인지 좁히면 다음 답변의 정확도가 올라갑니다.";
        };
    }

    private String buildContextAwareSummary(
            OnboardingAnalyzeRequest request,
            ConversationContext conversationContext,
            OnboardingIntent intent,
            List<TrendGameResponse> trendSignals,
            List<LiveTrendGameResponse> liveTrendSignals,
            FollowUpFocus followUpFocus,
            Optional<String> selectedPlatform,
            AgentQueryConditionResponse queryCondition
    ) {
        String previousQuestion = displayValue(conversationContext.message(), "이전 질문");
        String genre = intent.primaryGenre().displayName();
        return switch (followUpFocus) {
            case SCOPE_DECISION -> "이전 '%s' 분석을 범위 선택 기준으로 보면, %s는 프로토타입과 출시 MVP의 목표가 다릅니다. 지금은 먼저 프로토타입으로 핵심 재미를 검증하고, 반응이 확인될 때 출시 MVP로 확장하는 순서가 안전합니다."
                    .formatted(previousQuestion, genre);
            case PROTOTYPE_SCOPE -> "이전 '%s' 분석을 프로토타입 기준으로 좁히면, %s는 출시 완성도보다 핵심 조작감·세션 긴장감·차별화 한 가지를 검증하는 범위가 적합합니다."
                    .formatted(previousQuestion, genre);
            case RELEASE_MVP_SCOPE -> "이전 '%s' 분석을 출시 가능한 MVP 기준으로 보면, %s는 콘텐츠 양보다 안정성, 반복 플레이, 최소 매칭/세션 구조를 먼저 갖춰야 합니다."
                    .formatted(previousQuestion, genre);
            case SOLO_DEVELOPMENT -> "이전 '%s' 분석을 1인 개발 기준으로 재평가하면, %s는 대규모 시스템을 피하고 한 맵·한 모드·짧은 루프로 범위를 강하게 줄여야 합니다."
                    .formatted(previousQuestion, genre);
            case STREAMER_TARGET -> "이전 '%s' 분석을 스트리머 타깃 기준으로 보면, %s의 시장성보다 방송 반응, 시청자 이해도, 클립화 가능한 순간이 더 중요한 판단 축입니다."
                    .formatted(previousQuestion, genre);
            case MOBILE_PLATFORM -> "이전 '%s' 분석을 모바일 기준으로 바꾸면, %s는 조작 단순화와 짧은 세션 유지가 핵심이며 PC식 복잡도를 그대로 가져가면 위험합니다."
                    .formatted(previousQuestion, genre);
            case FEATURE_SCOPE -> "이전 '%s' 분석을 입력 기능 중심으로 좁히면, %s에 Webcam/TTS/STT를 모두 얹기보다 하나를 핵심 규칙으로 쓰는 편이 안전합니다."
                    .formatted(previousQuestion, genre);
            case MARKET_TREND -> buildTrendSummary(intent, trendSignals, liveTrendSignals, selectedPlatform, queryCondition);
            case PLAYER_RECOMMENDATION -> "이전 '%s' 맥락을 플레이 추천 관점으로 돌리면, 개발 난도보다 사용자가 실제로 즐길 장르 취향과 플레이 상황을 먼저 나눠야 합니다."
                    .formatted(previousQuestion);
            case CONTEXT_REFINEMENT -> "이전 '%s' 분석을 이어 받은 후속 요청입니다. 현재 질문 '%s'에 맞춰 같은 결론을 반복하기보다 판단 범위와 우선순위를 다시 좁힙니다."
                    .formatted(previousQuestion, request.message());
            case GENERAL -> "이전 분석 맥락 없이 현재 질문을 기준으로 %s 관점을 분석합니다.".formatted(genre);
        };
    }

    private String buildDevelopmentSummary(OnboardingAnalyzeRequest request, OnboardingIntent intent) {
        return switch (intent.primaryGenre()) {
            case FPS -> "%s 기준으로 FPS/배틀로얄 방향은 시장 신호는 강하지만 경쟁과 구현 난도가 높아, %s에 맞춘 축소형 슈팅 컨셉이 적합합니다."
                    .formatted(intent.platform(), intent.scopeLabel());
            case HORROR -> "%s 환경에서는 리액션이 잘 드러나는 호러 컨셉이 유리하며, %s 팀에는 짧은 방 단위 공포 루프가 적합합니다."
                    .formatted(intent.platform(), displayValue(request.teamSize(), "현재"));
            case PARTY -> "시청자 반응과 참여를 끌어내는 파티/협동형 컨셉이 요청과 가장 잘 맞으며, %s 범위로 빠르게 검증하기 좋습니다."
                    .formatted(intent.scopeLabel());
            case PUZZLE_ADVENTURE -> "퍼즐/어드벤처 방향은 차별화 포인트를 만들기 좋고, 음성·채팅 인터랙션을 규칙으로 연결하면 포트폴리오성이 높습니다.";
            case SURVIVAL_ROGUELIKE -> "생존/로그라이크 방향은 반복 플레이와 스트리밍 리액션을 만들기 좋지만, %s에서는 콘텐츠 범위를 작게 잡는 것이 중요합니다."
                    .formatted(intent.scopeLabel());
            case RPG -> "RPG 방향은 긴 성장 동선보다 전투, 빌드 선택, 퀘스트 한 줄기를 작게 잡아 취향 적합성을 먼저 검증하는 편이 좋습니다.";
            case MOBA -> "MOBA 방향은 팀 경쟁 수요는 강하지만 밸런싱 부담이 크므로, 초반에는 영웅 수와 맵 목표를 강하게 줄여야 합니다.";
            case STREAMER_INTERACTION -> "장르가 명확히 좁혀지지는 않았지만, 스트리머·시청자 참여 의도가 강해 반응형 방송 친화 컨셉이 적합합니다.";
            case GENERAL -> "입력 조건상 소규모로 검증 가능한 인터랙션 중심 게임 컨셉을 먼저 만들고, 시장 반응을 보며 장르를 좁히는 접근이 적합합니다.";
        };
    }

    private String buildTrendSummary(
            OnboardingIntent intent,
            List<TrendGameResponse> trendSignals,
            List<LiveTrendGameResponse> liveTrendSignals,
            Optional<String> selectedPlatform,
            AgentQueryConditionResponse queryCondition
    ) {
        if (liveTrendSignals != null && !liveTrendSignals.isEmpty()) {
            String topTitles = liveTrendSignals.stream()
                    .limit(3)
                    .map(game -> "%s(%s) %.1f점".formatted(game.title(), game.source(), game.trendScore()))
                    .collect(Collectors.joining(", "));

            return "현재 수집된 %s 중 %s 기준 상위 게임은 %s입니다. dataOrigin=REAL이면 실제 수집 데이터 기준이며, PARTIAL 또는 FALLBACK 신호는 보조 신호로만 해석해야 합니다."
                    .formatted(liveTrendScopeLabel(selectedPlatform), sortMetricLabel(queryCondition.sortMetric()), topTitles);
        }

        if (selectedPlatform.isPresent()) {
            return "%s 일반 분석 fallback으로 %s 계열의 시장 신호, 방송 노출, 개발 난도를 함께 봐야 합니다."
                    .formatted(liveTrendMissingDataMessage(selectedPlatform), intent.primaryGenre().displayName());
        }

        if (!trendSignals.isEmpty()) {
            String topTitles = trendSignals.stream()
                    .limit(3)
                    .map(game -> "%s %.1f점".formatted(game.title(), game.trendScore()))
                    .collect(Collectors.joining(", "));

            return "저장된 보조 트렌드 시그널 기준 상위 게임은 %s입니다. 다만 현재 공개 답변에서는 라이브 트렌드 snapshot을 우선 근거로 봅니다. %s 계열은 방송 노출과 반복 플레이 신호를 함께 확인해야 합니다."
                    .formatted(topTitles, intent.primaryGenre().displayName());
        }

        return "요즘 인기/트렌드 질문으로 분류했습니다. %s 일반 분석 fallback으로 %s 계열의 시장 신호, 방송 노출, 개발 난도를 함께 봐야 합니다."
                .formatted(liveTrendMissingDataMessage(selectedPlatform), intent.primaryGenre().displayName());
    }

    private String buildReinterpretationSummary(
            AgentQueryConditionResponse queryCondition,
            List<ReinterpretationCandidateResponse> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return "과거 게임 재해석 관점의 요청입니다. 아직 과거 게임 재해석 후보 데이터가 부족합니다. /api/legacy-games/refresh를 먼저 실행해주세요.";
        }

        String topCandidates = candidates.stream()
                .limit(3)
                .map(candidate -> "%s(%.1f점, %s)"
                        .formatted(candidate.title(), candidate.reinterpretationScore(), candidate.reinterpretationConcept()))
                .collect(Collectors.joining(", "));
        String featureLabel = displayQueryConditionFeatures(queryCondition);
        return "과거 게임 재해석 관점에서 상위 후보는 %s입니다. %s 적용 가능성과 방송 반응성, 시청자 참여성, 소규모 구현 가능성을 함께 기준으로 봤습니다."
                .formatted(topCandidates, "없음".equals(featureLabel) ? "Webcam/TTS/STT" : featureLabel);
    }

    private String buildAnswer(
            OnboardingAnalyzeRequest originalRequest,
            OnboardingAnalyzeRequest request,
            ConversationContext conversationContext,
            OnboardingIntent intent,
            List<RecommendedConceptResponse> concepts,
            List<TrendGameResponse> trendSignals,
            List<LiveTrendGameResponse> liveTrendSignals,
            FollowUpFocus followUpFocus,
            Optional<String> selectedPlatform,
            AgentQueryConditionResponse queryCondition
    ) {
        if (conversationContext.hasHistory()) {
            return buildContextAwareAnswer(originalRequest, conversationContext, intent, concepts, trendSignals, liveTrendSignals, followUpFocus, selectedPlatform, queryCondition);
        }

        String topConcept = concepts.isEmpty() ? "작은 실험형 컨셉" : concepts.get(0).title();
        return switch (intent.questionIntent()) {
            case GAME_RECOMMENDATION -> buildGameRecommendationAnswer(topConcept, originalRequest, intent, liveTrendSignals, selectedPlatform, queryCondition);
            case TREND_ANALYSIS -> buildTrendAnswer(intent, trendSignals, liveTrendSignals, selectedPlatform, queryCondition);
            case GAME_REINTERPRETATION -> "과거 게임 재해석 후보를 기준으로 답해야 합니다. 후보 데이터가 비어 있다면 /api/legacy-games/refresh 실행 후 다시 분석하는 편이 정확합니다.";
            case DEVELOPMENT_FEASIBILITY -> buildDeveloperMarketAnswer(intent, liveTrendSignals, selectedPlatform, queryCondition);
            case SPECIFIC_GAME_ANALYSIS -> "질문한 게임은 인기 요인을 그대로 따라 하기보다, 왜 사람들이 반복해서 플레이하고 방송에서 소비하는지 분해해서 보는 것이 좋습니다.";
            case STREAMING_FIT_ANALYSIS -> buildStreamingFitAnswer(topConcept, liveTrendSignals, selectedPlatform, queryCondition);
            case FEATURE_BASED_IDEA -> buildInteractionIdeaAnswer(topConcept, liveTrendSignals, selectedPlatform, queryCondition);
            case GENERAL_GAME_QUESTION -> "질문 범위가 넓어서 단정 답변은 어렵지만, 현재 입력만 보면 %s 기준으로 장르와 목적을 조금 더 좁히면 정확한 분석이 가능합니다."
                    .formatted(intent.platform());
        };
    }

    private String buildReinterpretationAnswer(
            AgentQueryConditionResponse queryCondition,
            List<ReinterpretationCandidateResponse> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return "아직 과거 게임 재해석 후보 데이터가 부족합니다. /api/legacy-games/refresh를 먼저 실행해주세요. 후보가 쌓이면 원작 메커니즘, 현재 라이브 트렌드 적합도, Webcam/TTS/STT 적용 가능성을 함께 분석할 수 있습니다.";
        }

        String featureLabel = displayQueryConditionFeatures(queryCondition);
        ReinterpretationCandidateResponse top = candidates.get(0);
        String recommended = candidates.stream()
                .limit(3)
                .map(candidate -> "%s는 '%s' 방향".formatted(candidate.title(), candidate.reinterpretationConcept()))
                .collect(Collectors.joining(", "));
        return "현재 후보 기준으로는 %s을 우선 추천합니다. 특히 %s는 reinterpretationScore %.1f점이며, 핵심 메커니즘(%s)을 그대로 베끼기보다 %s로 바꾸면 방송 반응성과 시청자 참여성을 만들기 좋습니다. %s 관점에서는 interactionFitScore %d점, modernTrendFitScore %d점, devFeasibilityScore %d점이라 개인/소규모 팀 MVP 후보로도 검토할 만합니다."
                .formatted(
                        recommended,
                        top.title(),
                        top.reinterpretationScore(),
                        String.join(", ", top.mechanics()),
                        top.reinterpretationConcept(),
                        "없음".equals(featureLabel) ? "Webcam/TTS/STT" : featureLabel,
                        top.interactionFitScore(),
                        top.modernTrendFitScore(),
                        top.devFeasibilityScore()
                );
    }

    private String buildContextAwareAnswer(
            OnboardingAnalyzeRequest request,
            ConversationContext conversationContext,
            OnboardingIntent intent,
            List<RecommendedConceptResponse> concepts,
            List<TrendGameResponse> trendSignals,
            List<LiveTrendGameResponse> liveTrendSignals,
            FollowUpFocus followUpFocus,
            Optional<String> selectedPlatform,
            AgentQueryConditionResponse queryCondition
    ) {
        String topConcept = concepts.isEmpty() ? "작은 검증용 컨셉" : concepts.get(0).title();
        String genre = intent.primaryGenre().displayName();
        return switch (followUpFocus) {
            case SCOPE_DECISION -> "프로토타입과 출시 가능한 MVP 중 하나를 고르는 질문이라면, 지금 단계에서는 프로토타입을 먼저 추천합니다. %s 계열은 출시 MVP로 가면 안정성·밸런싱·콘텐츠 부담이 급격히 커지므로, %s로 교전 감각과 차별화 루프를 먼저 증명한 뒤 확장 여부를 판단하는 편이 좋습니다."
                    .formatted(genre, topConcept);
            case PROTOTYPE_SCOPE -> "프로토타입 수준이라면 '%s'의 전체 시장 가능성을 증명하려 하기보다 %s 한 가지 루프를 2~3분 안에 반복 가능하게 만드는 것이 우선입니다. FPS라면 대규모 배틀로얄이 아니라 작은 교전, 피드백, 긴장감 검증이 핵심입니다."
                    .formatted(conversationContext.message(), topConcept);
            case RELEASE_MVP_SCOPE -> "출시 가능한 MVP 기준이면 판단이 더 엄격해집니다. %s 계열은 최소 콘텐츠, 튜토리얼, 밸런싱, 세션 안정성이 필요하므로 %s를 중심으로 기능을 줄이고 완성도를 올리는 전략이 좋습니다."
                    .formatted(genre, topConcept);
            case SOLO_DEVELOPMENT -> "1인 개발 기준이면 가능성은 '작게 만들 수 있느냐'로 바뀝니다. %s는 네트워크·맵·무기 수를 줄이고, 봇/싱글 검증 또는 제한된 멀티 한 모드로 시작해야 현실적입니다."
                    .formatted(genre);
            case STREAMER_TARGET -> "스트리머 타깃이라면 %s의 재미를 설명하는 데 오래 걸리면 불리합니다. 시청자가 10초 안에 상황을 이해하고 실패·역전·리액션이 클립으로 남는 구조를 우선 설계하세요."
                    .formatted(topConcept);
            case MOBILE_PLATFORM -> "모바일 기준이면 %s의 복잡한 조작을 그대로 옮기기보다 자동화, 짧은 매치, 세로/가로 조작 부담을 먼저 줄여야 합니다. 시장성보다 조작 피로도가 리스크입니다."
                    .formatted(genre);
            case FEATURE_SCOPE -> "기능 중심 후속 분석이라면 Webcam/TTS/STT를 모두 넣는 것보다 %s 안에서 하나의 입력이 승패나 이벤트를 바꾸는 규칙이 되어야 합니다."
                    .formatted(topConcept);
            case MARKET_TREND -> buildTrendAnswer(intent, trendSignals, liveTrendSignals, selectedPlatform, queryCondition);
            case PLAYER_RECOMMENDATION -> liveTrendSignals.isEmpty()
                    ? "%s 플레이 추천 관점이라면 개발 가능성과 별개로 %s 같은 후보를 취향별로 나누는 편이 좋습니다. 경쟁형, 협동형, 짧은 세션형 중 무엇을 원하는지가 다음 분기입니다."
                    .formatted(selectedPlatform.isPresent() ? liveTrendMissingDataMessage(selectedPlatform) : "", topConcept)
                    : buildGameRecommendationAnswer(topConcept, request, intent, liveTrendSignals, selectedPlatform, queryCondition);
            case CONTEXT_REFINEMENT -> "현재 후속 질문은 이전 답변의 결론을 반복하기보다 '%s' 조건을 추가해 재평가해야 합니다. 그래서 %s를 중심으로 범위, 리스크, 다음 검증 단계를 다시 잡겠습니다."
                    .formatted(request.message(), topConcept);
            case GENERAL -> "현재 질문만 보면 %s 기준의 일반 분석입니다. 더 구체적인 플랫폼, 팀 규모, 목표 범위를 주면 답변을 훨씬 좁힐 수 있습니다."
                    .formatted(genre);
        };
    }

    private String buildTrendAnswer(
            OnboardingIntent intent,
            List<TrendGameResponse> trendSignals,
            List<LiveTrendGameResponse> liveTrendSignals,
            Optional<String> selectedPlatform,
            AgentQueryConditionResponse queryCondition
    ) {
        if (liveTrendSignals != null && !liveTrendSignals.isEmpty()) {
            LiveTrendGameResponse topGame = liveTrendSignals.get(0);
            return "현재 수집된 %s 중 %s 기준으로는 %s가 %.1f점으로 가장 강합니다. %s에서 방송 %,d개와 시청자 %,d명이 잡혔고, %s/%s 상태입니다. %s"
                    .formatted(
                            liveTrendScopeLabel(selectedPlatform),
                            sortMetricLabel(queryCondition.sortMetric()),
                            topGame.title(),
                            topGame.trendScore(),
                            topGame.source(),
                            topGame.liveStreamCount(),
                            topGame.totalViewerCount(),
                            dataOriginDescription(topGame.dataOrigin()),
                            signalStatusDescription(topGame.signalStatus()),
                            partialCaution(topGame)
                    );
        }

        if (selectedPlatform.isPresent()) {
            return "%s 다른 플랫폼 데이터로 자동 대체하지 않고, 해당 플랫폼의 live trend snapshot이 쌓인 뒤 다시 분석하는 편이 정확합니다."
                    .formatted(liveTrendMissingDataMessage(selectedPlatform));
        }

        if (!trendSignals.isEmpty()) {
            TrendGameResponse topGame = trendSignals.get(0);
            return "요즘 인기 흐름은 %s처럼 trendScore가 높은 게임을 기준으로 보면 더 선명합니다. 현재 저장된 보조 데이터에서는 %s가 %.1f점으로 가장 강하지만, 공개 답변에서는 라이브 트렌드 수집 데이터가 쌓인 뒤 Twitch/CHZZK/SOOP 순위를 우선 확인하는 편이 좋습니다."
                    .formatted(
                            intent.primaryGenre().displayName(),
                            topGame.title(),
                            topGame.trendScore()
                    );
        }

        return "%s 요즘 인기 흐름은 한 가지 장르로만 보기 어렵지만, 일반 분석 fallback으로 보면 %s 관점에서는 방송 노출, 커뮤니티 확산, 반복 플레이가 강한 게임이 유리합니다."
                .formatted(liveTrendMissingDataMessage(selectedPlatform), intent.primaryGenre().displayName());
    }

    private String buildDeveloperMarketAnswer(
            OnboardingIntent intent,
            List<LiveTrendGameResponse> liveTrendSignals,
            Optional<String> selectedPlatform,
            AgentQueryConditionResponse queryCondition
    ) {
        if (liveTrendSignals != null && !liveTrendSignals.isEmpty()) {
            LiveTrendGameResponse topGame = liveTrendSignals.get(0);
            return "개발자 관점에서는 현재 수집된 %s 중 %s 기준 상위 후보인 %s를 참고할 만합니다. 시장 신호 %d점, 방송 %,d개, 시청자 %,d명을 함께 보면 장르 수요와 방송 확산도를 동시에 가늠할 수 있습니다. 첫 MVP는 인기 요소를 전부 따라 하기보다 핵심 루프 하나를 작게 검증하는 방향이 안전합니다."
                    .formatted(
                            liveTrendScopeLabel(selectedPlatform),
                            sortMetricLabel(queryCondition.sortMetric()),
                            topGame.title(),
                            topGame.marketSignalScore(),
                            topGame.liveStreamCount(),
                            topGame.totalViewerCount()
                    );
        }

        return "%s 개발자 관점에서는 %s 계열의 시장 신호, 방송 확산도, 구현 범위를 함께 보고 작은 MVP로 검증하는 편이 좋습니다."
                .formatted(liveTrendMissingDataMessage(selectedPlatform), intent.primaryGenre().displayName());
    }

    private String buildInteractionIdeaAnswer(
            String topConcept,
            List<LiveTrendGameResponse> liveTrendSignals,
            Optional<String> selectedPlatform,
            AgentQueryConditionResponse queryCondition
    ) {
        String features = displayQueryConditionFeatures(queryCondition);
        if (liveTrendSignals != null && !liveTrendSignals.isEmpty()) {
            LiveTrendGameResponse topGame = liveTrendSignals.get(0);
            return "인터랙션 게임 아이디어 관점에서는 %s 기능을 %s 같은 라이브 트렌드 후보의 리액션 포인트와 연결해 볼 수 있습니다. 현재 수집된 %s 중 %s 기준으로 방송 %,d개와 시청자 %,d명이 잡혀 있어, Webcam/TTS/STT를 단순 부가 기능이 아니라 실패, 선택, 이벤트 발생 규칙으로 묶는 방향이 좋습니다."
                    .formatted(
                            features,
                            topGame.title(),
                            liveTrendScopeLabel(selectedPlatform),
                            sortMetricLabel(queryCondition.sortMetric()),
                            topGame.liveStreamCount(),
                            topGame.totalViewerCount()
                    );
        }

        return "%s %s 기능은 모두 넣기보다 하나를 핵심 재미로 잡아야 합니다. 지금 조건에서는 %s 방향으로 MVP를 잡는 것이 좋습니다."
                .formatted(liveTrendMissingDataMessage(selectedPlatform), features, topConcept);
    }

    private String buildGameRecommendationAnswer(
            String topConcept,
            OnboardingAnalyzeRequest request,
            OnboardingIntent intent,
            List<LiveTrendGameResponse> liveTrendSignals,
            Optional<String> selectedPlatform,
            AgentQueryConditionResponse queryCondition
    ) {
        boolean friendPlayRequest = (request != null && isFriendPlayRequest(normalize(request.message())))
                || (intent != null && intent.primaryGenre() == PrimaryGenre.PARTY);
        if (liveTrendSignals != null && !liveTrendSignals.isEmpty()) {
            String topGames = buildLiveTrendTopSummary(liveTrendSignals, 3);
            LiveTrendGameResponse topGame = liveTrendSignals.get(0);
            if (intent != null && intent.soloTeam()) {
                return "혼자 할 게임 기준이면 %s를 먼저 볼 만합니다. 현재 수집된 %s 중 %s 조건에 맞는 후보이고, 상위 근거는 %s입니다."
                        .formatted(
                                topGame.title(),
                                liveTrendScopeLabel(selectedPlatform),
                                intent.primaryGenre().displayName(),
                                topGames
                        );
            }
            if (friendPlayRequest) {
                return "맞아요. 친구랑 할 게임 기준이면 순수 인기 순위보다 협동, 파티성, 같이 웃을 상황이 나오는지를 먼저 봐야 합니다. 현재 수집된 %s 중 %s 기준으로는 %s를 먼저 추천 후보로 볼 수 있고, 같이 보기 좋은 상위 근거는 %s입니다."
                        .formatted(
                                liveTrendScopeLabel(selectedPlatform),
                                sortMetricLabel(queryCondition.sortMetric()),
                                topGame.title(),
                                topGames
                        );
            }
            return "현재 수집된 %s 중 %s 기준으로는 %s를 먼저 추천 후보로 볼 수 있습니다. %s 관측 후보이며 데이터 출처는 %s입니다. dataOrigin=REAL인 실제 수집 데이터 기준이면 우선순위를 더 높게 보고, 부분 수집 데이터는 보조 신호로만 해석해야 합니다. 상위 근거는 %s입니다. 취향이 정해지지 않았다면 이 후보들 중 질문 기준에 가장 강한 게임부터 확인하는 편이 좋습니다."
                    .formatted(
                            liveTrendScopeLabel(selectedPlatform),
                            sortMetricLabel(queryCondition.sortMetric()),
                            topGame.title(),
                            platformBasisLabel(topGame.source()),
                            dataOriginDescription(topGame.dataOrigin()),
                            topGames
                    );
        }

        if (friendPlayRequest) {
            return "%s 친구랑 할 기준이면 Dota 2나 단순 인기 카테고리보다, 협동 호러, 파티 게임, 샌드박스 생존처럼 같이 역할을 나누고 리액션이 생기는 게임을 우선 추천하는 편이 맞습니다. 라이브 데이터가 부족하면 Lethal Company, Phasmophobia, It Takes Two, Overcooked, Minecraft 같은 방향부터 보는 것이 좋습니다."
                    .formatted(liveTrendMissingDataMessage(selectedPlatform));
        }

        if ((request != null && asksForCurrentPopularGame(request))
                || (intent != null && intent.trendIntent())) {
            if (intent != null && intent.soloTeam()) {
                return "%s 혼자 할 기준이면 %s를 먼저 추천할 수 있습니다. 이미 혼자 플레이 조건이 있으니, 멀티 경쟁 순위보다 싱글 진행감과 반복 플레이 피로도를 우선으로 보는 게 맞습니다."
                        .formatted(liveTrendMissingDataMessage(selectedPlatform), topConcept);
            }
            return "%s 지금 정보만으로는 취향을 단정하기보다, %s 같은 방향을 먼저 추천할 수 있습니다. 혼자 할지, 친구와 할지, 경쟁을 좋아하는지에 따라 추천은 더 달라집니다."
                    .formatted(liveTrendMissingDataMessage(selectedPlatform), topConcept);
        }

        if (intent != null && intent.soloTeam()) {
            return "혼자 할 기준이면 %s를 먼저 추천할 수 있습니다. 멀티 대기나 팀 합류 부담보다 혼자 진행해도 재미가 유지되는 구조를 우선으로 봤습니다."
                    .formatted(topConcept);
        }

        return "지금 정보만으로는 취향을 단정하기보다, %s 같은 방향을 먼저 추천할 수 있습니다. 혼자 할지, 친구와 할지, 경쟁을 좋아하는지에 따라 추천은 더 달라집니다."
                .formatted(topConcept);
    }

    private String buildStreamingFitAnswer(
            String topConcept,
            List<LiveTrendGameResponse> liveTrendSignals,
            Optional<String> selectedPlatform,
            AgentQueryConditionResponse queryCondition
    ) {
        if (!liveTrendSignals.isEmpty()) {
            LiveTrendGameResponse topGame = liveTrendSignals.stream()
                    .sorted(liveTrendCandidateComparator(queryCondition.sortMetric()))
                    .findFirst()
                    .orElse(liveTrendSignals.get(0));
            return "현재 수집된 %s 중 %s 기준으로 스트리머들이 많이 다루는 후보는 %s입니다. %s에서 방송 %,d개와 시청자 %,d명이 잡혀 있어, 시청자가 바로 이해하고 반응할 수 있는 장면을 만드는 방향이 중요합니다. %s"
                    .formatted(
                            liveTrendScopeLabel(selectedPlatform),
                            sortMetricLabel(queryCondition.sortMetric()),
                            topGame.title(),
                            topGame.source(),
                            topGame.liveStreamCount(),
                            topGame.totalViewerCount(),
                            partialCaution(topGame)
                    );
        }

        return "%s 일반 분석 fallback으로는 스트리머용 게임에서 시청자가 바로 이해하고 반응할 수 있는 장면이 중요하며, %s처럼 짧은 리액션 루프가 있는 방향이 적합합니다."
                .formatted(liveTrendMissingDataMessage(selectedPlatform), topConcept);
    }

    private List<RecommendedConceptResponse> buildFallbackConcepts(
            OnboardingAnalyzeRequest request,
            OnboardingIntent intent,
            FollowUpFocus followUpFocus,
            ConversationContext conversationContext
    ) {
        List<RecommendedConceptResponse> baseConcepts = conceptSeedsFor(intent).stream()
                .map(seed -> toRecommendedConcept(seed, request, intent))
                .sorted((first, second) -> Integer.compare(totalScore(second), totalScore(first)))
                .limit(3)
                .toList();

        if (!conversationContext.hasHistory() || followUpFocus == FollowUpFocus.GENERAL) {
            return baseConcepts;
        }

        return baseConcepts.stream()
                .map(concept -> adaptConceptForFollowUp(concept, conversationContext, followUpFocus))
                .toList();
    }

    private RecommendedConceptResponse adaptConceptForFollowUp(
            RecommendedConceptResponse concept,
            ConversationContext conversationContext,
            FollowUpFocus followUpFocus
    ) {
        int streamabilityAdjustment = switch (followUpFocus) {
            case STREAMER_TARGET -> 8;
            case FEATURE_SCOPE -> 4;
            case SCOPE_DECISION, PROTOTYPE_SCOPE -> 1;
            default -> 0;
        };
        int marketAdjustment = switch (followUpFocus) {
            case RELEASE_MVP_SCOPE, MARKET_TREND -> 5;
            case SCOPE_DECISION -> 1;
            case PROTOTYPE_SCOPE -> -4;
            case SOLO_DEVELOPMENT -> -2;
            default -> 0;
        };
        int feasibilityAdjustment = switch (followUpFocus) {
            case SCOPE_DECISION -> 4;
            case PROTOTYPE_SCOPE -> 10;
            case SOLO_DEVELOPMENT -> -8;
            case RELEASE_MVP_SCOPE -> -6;
            case MOBILE_PLATFORM -> -3;
            case STREAMER_TARGET -> 2;
            default -> 0;
        };

        return new RecommendedConceptResponse(
                "%s %s".formatted(concept.title(), followUpFocus.titleSuffix()).strip(),
                "%s / %s".formatted(concept.genre(), followUpFocus.displayName()),
                "%s 이전 분석 '%s'의 후속 요청을 %s 관점으로 재평가했습니다."
                        .formatted(concept.reason(), displayValue(conversationContext.message(), "이전 질문"), followUpFocus.displayName()),
                adjustScore(concept.streamabilityScore(), streamabilityAdjustment),
                adjustScore(concept.marketSignalScore(), marketAdjustment),
                adjustScore(concept.devFeasibilityScore(), feasibilityAdjustment)
        );
    }

    private String buildFallbackReport(
            OnboardingAnalyzeRequest request,
            OnboardingAnalyzeRequest analysisRequest,
            ConversationContext conversationContext,
            FollowUpFocus followUpFocus,
            OnboardingIntent intent,
            String summary,
            String answer,
            List<RecommendedConceptResponse> concepts,
            List<TrendGameResponse> trendSignals,
            List<LiveTrendGameResponse> liveTrendSignals,
            List<ReinterpretationCandidateResponse> reinterpretationCandidates,
            Optional<String> selectedPlatform,
            AgentQueryConditionResponse queryCondition,
            List<EvidenceCardResponse> evidenceCards
    ) {
        String conceptLines = concepts.stream()
                .map(concept -> """
                        - **%s** (%s)
                          - 추천 근거: %s
                          - 스트리밍 적합도: %d
                          - 마켓 시그널: %d
                          - 개발 가능성: %d
                        """.formatted(
                        concept.title(),
                        concept.genre(),
                        concept.reason(),
                        concept.streamabilityScore(),
                        concept.marketSignalScore(),
                        concept.devFeasibilityScore()
                ))
                .collect(Collectors.joining(System.lineSeparator()));

        return """
                ## 분석 요약
                %s

                ## 사용자 질문에 대한 답변
                %s

                ## 이전 분석 맥락
                %s

                ## 입력 조건
                - 목표 플랫폼: %s
                - 팀 규모: %s
                - 선호 기능: %s
                - 개발 기간: %s

                ## 추출된 의도
                - 질문 의도: %s
                - 주 장르: %s
                - 감지된 키워드: %s
                - 현재 분석 관점: %s
                - 사용자 관점: %s
                - selectedPlatform=%s
                - sortMetric=%s
                - analysisPurpose=%s
                - interactionFeatures=%s
                - excludeNonGameCategories=%s

                ## 보조 트렌드 시그널
                %s

                ## liveTrendCandidates
                %s

                ## reinterpretationCandidates
                %s

                ## 근거 카드
                %s

                ## 추천 컨셉
                %s

                ## MVP 방향
                %s

                ## 참고
                이 리포트는 LLM 호출 실패 또는 빈 응답에 대비한 fallback 분석 결과입니다.
                """.formatted(
                summary,
                answer,
                buildConversationContextFallbackSection(conversationContext),
                displayValue(analysisRequest.targetPlatform(), "미정"),
                displayValue(analysisRequest.teamSize(), "미정"),
                displayFeatures(analysisRequest.preferredFeatures()),
                displayValue(analysisRequest.developmentPeriod(), "미정"),
                intent.questionIntent().name(),
                intent.primaryGenre().displayName(),
                String.join(", ", intent.detectedKeywords()),
                followUpFocus.displayName(),
                resolveUserPerspective(intent, followUpFocus),
                selectedPlatformValue(selectedPlatform),
                queryCondition.sortMetric(),
                queryCondition.analysisPurpose(),
                displayQueryConditionFeatures(queryCondition),
                queryCondition.excludeNonGameCategories(),
                buildTrendSignalLines(trendSignals),
                buildLiveTrendSignalLines(liveTrendSignals, selectedPlatform),
                buildReinterpretationCandidateLines(reinterpretationCandidates),
                buildEvidenceCardLines(evidenceCards),
                conceptLines,
                buildMvpDirection(intent)
        );
    }

    private String buildConversationContextFallbackSection(ConversationContext conversationContext) {
        if (!conversationContext.hasHistory()) {
            return "- 신규 분석이라 이전 분석 맥락은 없습니다.";
        }

        return """
                - 이전 질문: %s
                - 이전 요약: %s
                - 이전 추천 컨셉: %s
                - 이번 응답은 위 맥락을 이어 받아 현재 추가 질문을 재평가한 결과입니다.
                """.formatted(
                displayValue(conversationContext.message(), "이전 질문 없음"),
                displayValue(conversationContext.summary(), "이전 요약 없음"),
                buildConceptTitleSummary(conversationContext.recommendedConcepts())
        );
    }

    private List<EvidenceCardResponse> buildEvidenceCards(
            OnboardingIntent intent,
            List<RecommendedConceptResponse> concepts,
            List<TrendGameResponse> trendSignals,
            List<LiveTrendGameResponse> liveTrendSignals,
            Optional<String> selectedPlatform,
            AgentQueryConditionResponse queryCondition
    ) {
        return switch (intent.questionIntent()) {
            case TREND_ANALYSIS -> {
                List<EvidenceCardResponse> liveTrendCards = liveTrendSignals.stream()
                        .limit(3)
                        .map(game -> toLiveTrendEvidenceCard(game, "LIVE_TREND_GAME", "현재 수집된 라이브 트렌드 기준으로 인기 흐름을 판단하는 근거입니다."))
                        .toList();
                if (!liveTrendCards.isEmpty()) {
                    yield liveTrendCards;
                }
                List<EvidenceCardResponse> trendCards = trendSignals.stream()
                        .limit(3)
                        .map(game -> toTrendEvidenceCard(game, "TREND_GAME", "저장된 트렌드 시그널 기준으로 인기 흐름을 판단하는 근거입니다."))
                        .toList();
                if (!trendCards.isEmpty()) {
                    yield trendCards;
                }
                if (selectedPlatform.isPresent()) {
                    yield fallbackEvidenceCards(intent, concepts, "LIVE_TREND_PLATFORM_EMPTY", liveTrendMissingDataMessage(selectedPlatform));
                }
                yield fallbackEvidenceCards(intent, concepts, "LIVE_TREND_EMPTY", "아직 수집된 라이브 트렌드 데이터가 부족해 규칙 기반 해석을 근거로 사용합니다.");
            }
            case GAME_RECOMMENDATION -> {
                List<EvidenceCardResponse> liveRecommendationCards = liveTrendSignals.stream()
                        .limit(3)
                        .map(game -> toLiveTrendEvidenceCard(game, "LIVE_TREND_RECOMMENDATION", "현재 수집된 라이브 트렌드 기준 추천 후보를 판단하는 근거입니다."))
                        .toList();
                if (!liveRecommendationCards.isEmpty()) {
                    yield liveRecommendationCards;
                }
                List<EvidenceCardResponse> trendRecommendationCards = trendSignals.stream()
                        .limit(3)
                        .map(game -> toTrendEvidenceCard(game, "TREND_GAME_RECOMMENDATION", "저장된 트렌드 시그널 기준 추천 후보를 판단하는 근거입니다."))
                        .toList();
                if (!trendRecommendationCards.isEmpty()) {
                    yield trendRecommendationCards;
                }
                yield fallbackEvidenceCards(
                        intent,
                        concepts,
                        selectedPlatform.isPresent() ? "LIVE_TREND_PLATFORM_EMPTY" : "INTERNAL_RECOMMENDATION",
                        selectedPlatform.isPresent()
                                ? liveTrendMissingDataMessage(selectedPlatform)
                                : "추천 컨셉의 장르, 시장성, 플레이 맥락을 근거로 사용합니다."
                );
            }
            case GAME_REINTERPRETATION -> fallbackEvidenceCards(intent, concepts, "REINTERPRETATION_EMPTY", "과거 게임 재해석 후보가 비어 있어 fallback 근거를 사용합니다.");
            case DEVELOPMENT_FEASIBILITY -> {
                List<EvidenceCardResponse> liveMarketCards = liveTrendSignals.stream()
                        .limit(3)
                        .map(game -> toLiveTrendEvidenceCard(game, "LIVE_MARKET_SIGNAL", "현재 수집된 라이브 트렌드 기준 개발자 관점의 시장 신호를 판단하는 근거입니다."))
                        .toList();
                if (!liveMarketCards.isEmpty()) {
                    yield liveMarketCards;
                }
                List<EvidenceCardResponse> trendMarketCards = trendSignals.stream()
                        .limit(3)
                        .map(game -> toTrendEvidenceCard(game, "DEVELOPMENT_FEASIBILITY", "저장된 트렌드 시그널 기준 개발자 관점의 시장 신호를 판단하는 근거입니다."))
                        .toList();
                if (!trendMarketCards.isEmpty()) {
                    yield trendMarketCards;
                }
                yield fallbackEvidenceCards(intent, concepts, "DEVELOPMENT_FEASIBILITY", "개발 범위, 라이브 트렌드 신호, 구현 난도를 함께 본 fallback 근거입니다.");
            }
            case STREAMING_FIT_ANALYSIS -> {
                List<EvidenceCardResponse> liveStreamingCards = liveTrendSignals.stream()
                        .limit(3)
                        .map(game -> toLiveTrendEvidenceCard(game, "LIVE_STREAMING_SIGNAL", "현재 수집된 라이브 트렌드 기준 방송 적합성을 판단하는 근거입니다. sortMetric=%s".formatted(queryCondition.sortMetric())))
                        .toList();
                if (!liveStreamingCards.isEmpty()) {
                    yield liveStreamingCards;
                }
                if (selectedPlatform.isPresent()) {
                    yield fallbackEvidenceCards(intent, concepts, "LIVE_TREND_PLATFORM_EMPTY", liveTrendMissingDataMessage(selectedPlatform));
                }

                yield fallbackEvidenceCards(intent, concepts, "STREAMING_SIGNAL", "짧은 리액션 루프와 시청자 이해도를 기준으로 만든 fallback 방송 근거입니다.");
            }
            case FEATURE_BASED_IDEA -> {
                List<EvidenceCardResponse> liveInteractionCards = liveTrendSignals.stream()
                        .limit(3)
                        .map(game -> toLiveTrendEvidenceCard(game, "LIVE_INTERACTION_IDEA_SIGNAL", "현재 수집된 라이브 트렌드 기준 인터랙션 게임 아이디어를 판단하는 근거입니다."))
                        .toList();
                yield liveInteractionCards.isEmpty()
                        ? fallbackEvidenceCards(
                        intent,
                        concepts,
                        "FEATURE_SIGNAL",
                        "Webcam/TTS/STT/채팅/음성 기능과 추천 컨셉의 적합도를 기준으로 만든 근거입니다."
                )
                        : liveInteractionCards;
            }
            case SPECIFIC_GAME_ANALYSIS, GENERAL_GAME_QUESTION -> {
                List<EvidenceCardResponse> liveGeneralCards = liveTrendSignals.stream()
                        .limit(3)
                        .map(game -> toLiveTrendEvidenceCard(game, "LIVE_TREND_GAME", "질문 해석에 참고할 수 있는 라이브 트렌드 근거입니다."))
                        .toList();
                if (!liveGeneralCards.isEmpty()) {
                    yield liveGeneralCards;
                }
                List<EvidenceCardResponse> trendGeneralCards = trendSignals.stream()
                        .limit(3)
                        .map(game -> toTrendEvidenceCard(game, "TREND_GAME", "질문 해석에 참고할 수 있는 저장된 트렌드 근거입니다."))
                        .toList();
                yield trendGeneralCards.isEmpty()
                        ? fallbackEvidenceCards(intent, concepts, "LIVE_TREND_EMPTY", "질문 해석에 참고할 라이브 트렌드 데이터가 아직 부족합니다.")
                        : trendGeneralCards;
            }
        };
    }

    private EvidenceCardResponse toLiveTrendEvidenceCard(
            LiveTrendGameResponse game,
            String type,
            String description
    ) {
        return new EvidenceCardResponse(
                game.title(),
                type,
                "%s %s %s 장르이며 %s 플랫폼에서 관측된 라이브 신호입니다. %s %s"
                        .formatted(
                                description,
                                dataOriginDescription(game.dataOrigin()),
                                displayValue(game.genre(), "미분류"),
                                displayValue(game.source(), "UNKNOWN"),
                                signalStatusDescription(game.signalStatus()),
                                partialCaution(game)
                        ),
                game.trendScore(),
                null,
                "TWITCH".equalsIgnoreCase(nullToEmpty(game.source())) ? game.totalViewerCount() : null,
                "TWITCH".equalsIgnoreCase(nullToEmpty(game.source())) ? game.liveStreamCount() : null,
                game.streamabilityScore(),
                game.marketSignalScore(),
                game.reason(),
                game.source(),
                game.genre(),
                game.totalViewerCount(),
                game.liveStreamCount(),
                game.signalStatus(),
                game.dataOrigin(),
                game.imageUrl()
        );
    }

    private EvidenceCardResponse toTrendEvidenceCard(
            TrendGameResponse game,
            String type,
            String description
    ) {
        return new EvidenceCardResponse(
                game.title(),
                type,
                "%s %s 장르이며 %s 플랫폼에서 관측된 신호입니다.".formatted(description, game.genre(), game.platform()),
                game.trendScore(),
                game.steamReviewScore(),
                game.twitchTotalViewerCount(),
                game.twitchLiveStreamCount(),
                game.streamabilityScore(),
                game.marketSignalScore(),
                game.reason(),
                GameImageResolver.resolveImageUrl(game.title(), null, null)
        );
    }

    private List<EvidenceCardResponse> buildReinterpretationEvidenceCards(
            List<ReinterpretationCandidateResponse> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of(new EvidenceCardResponse(
                    "과거 게임 재해석 후보",
                    "REINTERPRETATION_EMPTY",
                    "아직 과거 게임 재해석 후보 데이터가 부족합니다. /api/legacy-games/refresh를 먼저 실행해주세요.",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "/api/legacy-games/refresh 실행 후 seed 기반 후보와 라이브 트렌드 적합도를 보조 근거로 사용할 수 있습니다.",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }

        return candidates.stream()
                .limit(3)
                .map(this::toReinterpretationEvidenceCard)
                .toList();
    }

    private EvidenceCardResponse toReinterpretationEvidenceCard(ReinterpretationCandidateResponse candidate) {
        String originalGenre = String.join(", ", candidate.genres());
        return new EvidenceCardResponse(
                candidate.title(),
                "REINTERPRETATION_CANDIDATE",
                "%s 원작의 핵심 메커니즘을 현대 스트리밍/시청자 참여 환경에 맞게 재해석하는 후보입니다."
                        .formatted(candidate.reinterpretationConcept()),
                candidate.reinterpretationScore(),
                null,
                null,
                null,
                candidate.streamabilityScore(),
                candidate.modernTrendFitScore(),
                candidate.reason(),
                candidate.source(),
                originalGenre,
                null,
                null,
                null,
                candidate.dataOrigin(),
                "REINTERPRETATION",
                "REINTERPRETATION_CANDIDATE",
                originalGenre,
                candidate.reinterpretationConcept(),
                candidate.reinterpretationScore(),
                candidate.legacyPopularityScore(),
                candidate.reviewSentimentScore(),
                candidate.mechanicUniquenessScore(),
                candidate.interactionFitScore(),
                candidate.modernTrendFitScore(),
                candidate.devFeasibilityScore(),
                GameImageResolver.resolveImageUrl(candidate.title(), null, candidate.steamAppId())
        );
    }

    private List<EvidenceCardResponse> fallbackEvidenceCards(
            OnboardingIntent intent,
            List<RecommendedConceptResponse> concepts,
            String type,
            String description
    ) {
        if (concepts.isEmpty()) {
            return List.of(new EvidenceCardResponse(
                    intent.primaryGenre().displayName(),
                    type,
                    "저장된 트렌드 데이터가 없어 감지된 의도와 키워드를 기준으로 만든 fallback 근거입니다.",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    String.join(", ", intent.detectedKeywords())
            ));
        }

        return concepts.stream()
                .limit(2)
                .map(concept -> new EvidenceCardResponse(
                        concept.title(),
                        type,
                        description,
                        null,
                        null,
                        null,
                        null,
                        concept.streamabilityScore(),
                        concept.marketSignalScore(),
                        concept.reason()
                ))
                .toList();
    }

    private String buildEvidenceCardLines(List<EvidenceCardResponse> evidenceCards) {
        if (evidenceCards == null || evidenceCards.isEmpty()) {
            return "- 현재 응답에 포함할 근거 카드가 없습니다.";
        }

        return evidenceCards.stream()
                .map(card -> "- %s | evidenceType=%s | type=%s | category=%s | source=%s | genre=%s | originalGenre=%s | trendScore=%s | Steam 리뷰=%s | reinterpretationScore=%s | legacyPopularityScore=%s | reviewSentimentScore=%s | mechanicUniquenessScore=%s | streamabilityScore=%s | interactionFitScore=%s | modernTrendFitScore=%s | devFeasibilityScore=%s | totalViewerCount=%s | liveStreamCount=%s | viewerCount=%s | marketSignalScore=%s | signalStatus=%s | dataOrigin=%s | reinterpretationConcept=%s | 근거=%s"
                        .formatted(
                                card.title(),
                                displayValue(card.evidenceType(), "없음"),
                                card.type(),
                                displayValue(card.category(), "없음"),
                                displayValue(card.source(), "없음"),
                                displayValue(card.genre(), "없음"),
                                displayValue(card.originalGenre(), "없음"),
                                displayNullableNumber(card.trendScore()),
                                displayNullableNumber(card.steamReviewScore()),
                                displayNullableNumber(card.reinterpretationScore()),
                                displayNullableNumber(card.legacyPopularityScore()),
                                displayNullableNumber(card.reviewSentimentScore()),
                                displayNullableNumber(card.mechanicUniquenessScore()),
                                displayNullableNumber(card.streamabilityScore()),
                                displayNullableNumber(card.interactionFitScore()),
                                displayNullableNumber(card.modernTrendFitScore()),
                                displayNullableNumber(card.devFeasibilityScore()),
                                displayNullableNumber(card.totalViewerCount()),
                                displayNullableNumber(card.liveStreamCount()),
                                displayNullableNumber(card.twitchViewerCount()),
                                displayNullableNumber(card.marketSignalScore()),
                                displayValue(card.signalStatus(), "없음"),
                                displayValue(card.dataOrigin(), "없음"),
                                displayValue(card.reinterpretationConcept(), "없음"),
                                card.reason()
                        ))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String displayNullableNumber(Number value) {
        if (value == null) {
            return "없음";
        }
        return value.toString();
    }

    private List<TrendGameResponse> resolveTrendSignals(OnboardingIntent intent) {
        if (trendGameService == null) {
            return List.of();
        }

        try {
            List<TrendGameResponse> trendGames = trendGameService.findTrendGames();
            List<TrendGameResponse> filteredTrendGames = trendGames.stream()
                    .filter(game -> matchesTrendIntent(game, intent))
                    .limit(5)
                    .toList();

            if (!filteredTrendGames.isEmpty()) {
                return filteredTrendGames;
            }
            if (intent.questionIntent() == QuestionIntent.GAME_RECOMMENDATION
                    && intent.primaryGenre() != PrimaryGenre.GENERAL) {
                log.info("플레이 추천 장르({})와 맞는 내부 트렌드가 없어 전체 트렌드 fallback을 건너뜁니다.", intent.primaryGenre());
                return List.of();
            }
            return trendGames.stream().limit(5).toList();
        } catch (RuntimeException ex) {
            log.warn("온보딩 트렌드 시그널 조회 실패. cause={}", ex.toString());
            return List.of();
        }
    }

    private List<LiveTrendGameResponse> resolveLiveTrendSignals(
            OnboardingAnalyzeRequest originalRequest,
            OnboardingAnalyzeRequest analysisRequest,
            OnboardingIntent intent,
            AgentQueryConditionResponse queryCondition
    ) {
        if (liveTrendService == null || !usesLiveTrendSignals(analysisRequest, intent, queryCondition)) {
            return List.of();
        }

        try {
            Optional<String> selectedPlatform = selectedPlatform(queryCondition);
            List<LiveTrendGameResponse> liveTrendGames = liveTrendService.findTopLiveTrendGames(50, selectedPlatformValue(selectedPlatform));
            List<LiveTrendGameResponse> gameOnlyLiveTrendGames = liveTrendGames.stream()
                    .filter(game -> !queryCondition.excludeNonGameCategories()
                            || nonGameCategoryFilter.shouldInclude(game, originalRequest.message()))
                    .sorted(liveTrendCandidateComparator(queryCondition.sortMetric()))
                    .toList();
            List<LiveTrendGameResponse> filteredLiveTrendGames = gameOnlyLiveTrendGames.stream()
                    .filter(game -> matchesLiveTrendIntent(game, intent))
                    .filter(game -> matchesRequestedPlayMode(game, originalRequest, intent))
                    .sorted(liveTrendCandidateComparator(queryCondition.sortMetric()))
                    .limit(5)
                    .toList();

            if (!filteredLiveTrendGames.isEmpty()) {
                return filteredLiveTrendGames;
            }
            if (intent.questionIntent() == QuestionIntent.GAME_RECOMMENDATION
                    && intent.primaryGenre() != PrimaryGenre.GENERAL) {
                log.info("플레이 추천 장르({})와 맞는 라이브 트렌드가 없어 전체 인기 게임 fallback을 건너뜁니다.", intent.primaryGenre());
                return List.of();
            }
            return gameOnlyLiveTrendGames.stream().limit(5).toList();
        } catch (RuntimeException ex) {
            log.warn("온보딩 라이브 트렌드 시그널 조회 실패. cause={}", ex.toString());
            return List.of();
        }
    }

    private List<ReinterpretationCandidateResponse> resolveReinterpretationCandidates(
            OnboardingIntent intent,
            AgentQueryConditionResponse queryCondition
    ) {
        if (reinterpretationCandidateService == null || !usesReinterpretationCandidates(intent, queryCondition)) {
            return List.of();
        }

        try {
            return reinterpretationCandidateService.findCandidates(queryCondition, 5);
        } catch (RuntimeException ex) {
            log.warn("온보딩 과거 게임 재해석 후보 조회 실패. cause={}", ex.toString());
            return List.of();
        }
    }

    private boolean usesReinterpretationCandidates(
            OnboardingIntent intent,
            AgentQueryConditionResponse queryCondition
    ) {
        return (queryCondition != null && "GAME_REINTERPRETATION".equals(queryCondition.analysisPurpose()))
                || intent.questionIntent() == QuestionIntent.GAME_REINTERPRETATION;
    }

    private boolean usesLiveTrendSignals(
            OnboardingAnalyzeRequest request,
            OnboardingIntent intent,
            AgentQueryConditionResponse queryCondition
    ) {
        return switch (queryCondition.analysisPurpose()) {
            case "TREND_ANALYSIS",
                 "USER_GAME_RECOMMENDATION",
                 "DEVELOPER_MARKET_ANALYSIS",
                 "STREAMING_FIT_ANALYSIS",
                 "INTERACTION_GAME_IDEA" -> true;
            default -> intent.questionIntent() == QuestionIntent.TREND_ANALYSIS
                || intent.questionIntent() == QuestionIntent.STREAMING_FIT_ANALYSIS
                || (intent.questionIntent() == QuestionIntent.GAME_RECOMMENDATION
                && asksForCurrentPopularGame(request));
        };
    }

    private boolean asksForCurrentPopularGame(OnboardingAnalyzeRequest request) {
        String normalizedMessage = normalize(request.message());
        return containsAny(
                normalizedMessage,
                "요즘",
                "최근",
                "현재",
                "지금",
                "인기",
                "트렌드",
                "뜨는",
                "유행",
                "핫한",
                "많이 하는",
                "많이 보는",
                "할만한",
                "할 만한"
        );
    }

    private boolean matchesRequestedPlayMode(
            LiveTrendGameResponse game,
            OnboardingAnalyzeRequest request,
            OnboardingIntent intent
    ) {
        if (intent.questionIntent() != QuestionIntent.GAME_RECOMMENDATION) {
            return true;
        }
        if (intent.soloTeam() || isSoloPlayRequest(request.message())) {
            return matchesSoloFriendlyLiveGame(game);
        }
        if (isFriendPlayRequest(request.message())) {
            return matchesFriendFriendlyLiveGame(game);
        }
        return true;
    }

    private boolean matchesSoloFriendlyLiveGame(LiveTrendGameResponse game) {
        String title = normalize(game.title());
        String genre = normalize(game.genre());
        String keyword = normalize(game.sourceKeyword());
        if (containsAny(title, "valorant", "counter-strike", "dota", "league of legends", "lol")
                || containsAny(keyword, "valorant", "counter-strike", "dota", "league of legends", "lol")
                || containsAny(genre, "moba", "battle royale", "tactical fps", "competitive")) {
            return false;
        }
        return containsAny(title, "doom", "titanfall", "metro", "borderlands", "cyberpunk",
                "elden ring", "baldur", "witcher", "skyrim", "persona", "diablo",
                "path of exile", "monster hunter", "hades", "subnautica", "resident evil")
                || containsAny(genre, "single", "single-player", "single player", "rpg", "role-playing",
                "action rpg", "jrpg", "mmorpg", "adventure", "survival horror", "roguelike",
                "soulslike", "open world", "sandbox survival", "puzzle");
    }

    private boolean matchesFriendFriendlyLiveGame(LiveTrendGameResponse game) {
        String title = normalize(game.title());
        String genre = normalize(game.genre());
        String keyword = normalize(game.sourceKeyword());
        return containsAny(title, "lethal", "phasmophobia", "content warning", "among us",
                "fall guys", "pummel party", "jackbox", "goose goose duck", "it takes two",
                "overcooked", "plateup", "unrailed", "minecraft", "valheim", "palworld",
                "monster hunter")
                || containsAny(keyword, "lethal", "phasmophobia", "content warning", "among us",
                "fall guys", "pummel party", "jackbox", "goose goose duck", "it takes two",
                "overcooked", "plateup", "unrailed", "minecraft", "valheim", "palworld",
                "monster hunter")
                || containsAny(genre, "party", "social", "co-op", "coop", "cooperative",
                "multiplayer party", "social deduction", "survival", "craft", "sandbox", "action rpg");
    }

    private boolean matchesTrendIntent(TrendGameResponse game, OnboardingIntent intent) {
        String title = normalize(game.title());
        String genre = normalize(game.genre());
        return switch (intent.primaryGenre()) {
            case FPS -> containsAny(title, "counter-strike", "pubg", "helldivers")
                    || containsAny(genre, "fps", "shooter", "battle royale");
            case HORROR -> containsAny(title, "lethal") || containsAny(genre, "horror");
            case PARTY, STREAMER_INTERACTION -> containsAny(title, "among us", "lethal", "phasmophobia",
                    "content warning", "fall guys", "pummel party", "it takes two", "overcooked",
                    "minecraft", "valheim", "palworld")
                    || containsAny(genre, "party", "social", "co-op", "coop", "cooperative",
                    "social deduction", "survival", "craft", "sandbox");
            case SURVIVAL_ROGUELIKE -> containsAny(title, "minecraft", "palworld")
                    || containsAny(genre, "survival", "craft", "roguelike");
            case PUZZLE_ADVENTURE -> containsAny(genre, "puzzle", "adventure");
            case RPG -> containsAny(title, "monster hunter", "elden ring", "baldur", "path of exile",
                    "diablo", "final fantasy", "persona", "witcher", "skyrim")
                    || containsAny(genre, "rpg", "role-playing", "action rpg", "jrpg", "mmorpg");
            case MOBA -> containsAny(title, "league of legends", "dota", "heroes of the storm", "eternal return")
                    || containsAny(genre, "moba", "multiplayer online battle");
            case GENERAL -> true;
        };
    }

    private boolean matchesLiveTrendIntent(LiveTrendGameResponse game, OnboardingIntent intent) {
        String title = normalize(game.title());
        String genre = normalize(game.genre());
        String sourceKeyword = normalize(game.sourceKeyword());
        return switch (intent.primaryGenre()) {
            case FPS -> containsAny(title, "counter-strike", "pubg", "배틀그라운드", "서든어택")
                    || containsAny(sourceKeyword, "counter-strike", "pubg", "배틀그라운드", "서든어택")
                    || containsAny(genre, "fps", "shooter", "battle royale");
            case HORROR -> containsAny(title, "lethal") || containsAny(genre, "horror");
            case PARTY, STREAMER_INTERACTION -> containsAny(title, "lethal", "phasmophobia", "content warning",
                    "among us", "fall guys", "pummel party", "jackbox", "goose goose duck",
                    "it takes two", "overcooked", "plateup", "unrailed", "minecraft", "valheim", "palworld")
                    || containsAny(sourceKeyword, "lethal", "phasmophobia", "content warning",
                    "among us", "fall guys", "pummel party", "jackbox", "goose goose duck",
                    "it takes two", "overcooked", "plateup", "unrailed", "minecraft", "valheim", "palworld")
                    || containsAny(genre, "party", "social", "co-op", "coop", "cooperative",
                    "multiplayer party", "social deduction", "survival", "craft", "sandbox");
            case SURVIVAL_ROGUELIKE -> containsAny(title, "minecraft")
                    || containsAny(genre, "survival", "craft", "roguelike");
            case PUZZLE_ADVENTURE -> containsAny(genre, "puzzle", "adventure");
            case RPG -> containsAny(title, "monster hunter", "elden ring", "baldur", "path of exile",
                    "diablo", "final fantasy", "persona", "witcher", "skyrim")
                    || containsAny(sourceKeyword, "monster hunter", "elden ring", "baldur", "path of exile",
                    "diablo", "final fantasy", "persona", "witcher", "skyrim")
                    || containsAny(genre, "rpg", "role-playing", "action rpg", "jrpg", "mmorpg");
            case MOBA -> containsAny(title, "league of legends", "dota", "heroes of the storm", "eternal return")
                    || containsAny(sourceKeyword, "league of legends", "dota", "heroes of the storm", "eternal return")
                    || containsAny(genre, "moba", "multiplayer online battle");
            case GENERAL -> true;
        };
    }

    private String buildTrendSignalLines(List<TrendGameResponse> trendSignals) {
        if (trendSignals == null || trendSignals.isEmpty()) {
            return """
                    - 현재 공개 답변에 사용할 보조 트렌드 시그널이 없습니다.
                    - POST /api/live-trends/refresh 실행 후 Twitch/CHZZK/SOOP 라이브 순위를 우선 근거로 사용합니다.
                    """;
        }

        return trendSignals.stream()
                .map(game -> "- %s | 장르=%s | trendScore=%.1f | 방송 시청자=%,d | 방송 수=%d | internal=%.1f"
                        .formatted(
                                game.title(),
                                game.genre(),
                                game.trendScore(),
                                game.twitchTotalViewerCount(),
                                game.twitchLiveStreamCount(),
                                game.internalRecommendationScore()
                        ))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String buildLiveTrendSignalLines(
            List<LiveTrendGameResponse> liveTrendSignals,
            Optional<String> selectedPlatform
    ) {
        if (liveTrendSignals == null || liveTrendSignals.isEmpty()) {
            return """
                    - %s
                    - POST /api/live-trends/refresh 실행 후에는 Twitch/CHZZK/SOOP/Steam live trend snapshot을 답변 근거로 사용할 수 있습니다.
                    """.formatted(liveTrendMissingDataMessage(selectedPlatform));
        }

        return liveTrendSignals.stream()
                .map(game -> "- 현재 수집된 라이브 트렌드 기준 %s | %s | source=%s | 장르=%s | trendScore=%.1f | 시청자=%,d | 방송=%d | dataOrigin=%s(%s) | signalStatus=%s(%s) | 주의=%s | 근거=%s"
                        .formatted(
                                game.title(),
                                platformBasisLabel(game.source()),
                                game.source(),
                                displayValue(game.genre(), "미분류"),
                                game.trendScore(),
                                game.totalViewerCount(),
                                game.liveStreamCount(),
                                displayValue(game.dataOrigin(), "없음"),
                                dataOriginDescription(game.dataOrigin()),
                                displayValue(game.signalStatus(), "없음"),
                                signalStatusDescription(game.signalStatus()),
                                partialCaution(game).isBlank() ? "주의사항 없음" : partialCaution(game),
                                displayValue(game.reason(), "근거 없음")
                        ))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String buildReinterpretationCandidateLines(
            List<ReinterpretationCandidateResponse> reinterpretationCandidates
    ) {
        if (reinterpretationCandidates == null || reinterpretationCandidates.isEmpty()) {
            return """
                    - 아직 과거 게임 재해석 후보 데이터가 부족합니다.
                    - POST /api/legacy-games/refresh 실행 후에는 seed 기반 후보와 현재 라이브 트렌드 적합도를 보조 근거로 사용할 수 있습니다.
                    """;
        }

        return reinterpretationCandidates.stream()
                .map(candidate -> "- 원작=%s | 장르=%s | 태그=%s | 메커니즘=%s | 컨셉=%s | reinterpretationScore=%.1f | legacyPopularity=%d | reviewSentiment=%d | mechanicUniqueness=%d | streamability=%d | interactionFit=%d | modernTrendFit=%d | devFeasibility=%d | 적용 힌트=%s | 근거=%s"
                        .formatted(
                                candidate.title(),
                                String.join(", ", candidate.genres()),
                                String.join(", ", candidate.tags()),
                                String.join(", ", candidate.mechanics()),
                                candidate.reinterpretationConcept(),
                                candidate.reinterpretationScore(),
                                candidate.legacyPopularityScore(),
                                candidate.reviewSentimentScore(),
                                candidate.mechanicUniquenessScore(),
                                candidate.streamabilityScore(),
                                candidate.interactionFitScore(),
                                candidate.modernTrendFitScore(),
                                candidate.devFeasibilityScore(),
                                String.join(", ", candidate.interactionHints()),
                                displayValue(candidate.reason(), "근거 없음")
                        ))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String buildLiveTrendTopSummary(List<LiveTrendGameResponse> liveTrendSignals, int limit) {
        return liveTrendSignals.stream()
                .limit(limit)
                .map(game -> "%s(%s, 시청자 %,d명, 방송 %,d개, trendScore %.1f, %s/%s)"
                        .formatted(
                                game.title(),
                                platformBasisLabel(game.source()),
                                game.totalViewerCount(),
                                game.liveStreamCount(),
                                game.trendScore(),
                                dataOriginDescription(game.dataOrigin()),
                                signalStatusDescription(game.signalStatus())
                        ))
                .collect(Collectors.joining(", "));
    }

    private Comparator<LiveTrendGameResponse> liveTrendCandidateComparator(String sortMetric) {
        return switch (nullToEmpty(sortMetric)) {
            case "VIEWER_COUNT" -> Comparator.comparingInt(LiveTrendGameResponse::totalViewerCount).reversed()
                    .thenComparing(Comparator.comparingDouble(LiveTrendGameResponse::trendScore).reversed())
                    .thenComparing(Comparator.comparingInt(LiveTrendGameResponse::liveStreamCount).reversed());
            case "STREAM_COUNT" -> Comparator.comparingInt(LiveTrendGameResponse::liveStreamCount).reversed()
                    .thenComparing(Comparator.comparingDouble(LiveTrendGameResponse::trendScore).reversed())
                    .thenComparing(Comparator.comparingInt(LiveTrendGameResponse::totalViewerCount).reversed());
            case "STREAMER_SPREAD" -> Comparator.comparingInt(LiveTrendGameResponse::liveStreamCount).reversed()
                    .thenComparing(Comparator.comparingInt(LiveTrendGameResponse::streamCountScore).reversed())
                    .thenComparing(Comparator.comparingInt(LiveTrendGameResponse::totalViewerCount).reversed())
                    .thenComparing(Comparator.comparingDouble(LiveTrendGameResponse::trendScore).reversed());
            case "MARKET_SIGNAL" -> Comparator.comparingInt(LiveTrendGameResponse::marketSignalScore).reversed()
                    .thenComparing(Comparator.comparingDouble(LiveTrendGameResponse::trendScore).reversed())
                    .thenComparing(Comparator.comparingInt(LiveTrendGameResponse::totalViewerCount).reversed());
            default -> Comparator.comparingDouble(LiveTrendGameResponse::trendScore).reversed()
                    .thenComparing(Comparator.comparingInt(LiveTrendGameResponse::totalViewerCount).reversed())
                    .thenComparing(Comparator.comparingInt(LiveTrendGameResponse::liveStreamCount).reversed());
        };
    }

    private OnboardingIntent alignIntentWithQueryCondition(
            OnboardingIntent intent,
            AgentQueryConditionResponse queryCondition
    ) {
        if (queryCondition == null || queryCondition.analysisPurpose() == null) {
            return intent;
        }
        QuestionIntent resolvedIntent = questionIntentFromAnalysisPurpose(queryCondition.analysisPurpose());
        if (resolvedIntent == QuestionIntent.TREND_ANALYSIS && isMoreSpecificThanTrend(intent.questionIntent())) {
            return intent;
        }
        if (resolvedIntent == intent.questionIntent()) {
            return intent;
        }

        List<String> detectedKeywords = new ArrayList<>(intent.detectedKeywords());
        detectedKeywords.add(analysisPurposeLabel(queryCondition.analysisPurpose()));

        return new OnboardingIntent(
                resolvedIntent,
                intent.primaryGenre(),
                detectedKeywords.stream().distinct().toList(),
                intent.preferredFeatures(),
                intent.platform(),
                intent.teamSize(),
                intent.developmentPeriod(),
                intent.scopeLabel(),
                intent.fpsIntent(),
                intent.horrorIntent(),
                intent.partyIntent(),
                intent.puzzleAdventureIntent(),
                intent.survivalRoguelikeIntent(),
                intent.streamerIntent(),
                intent.webcamRequested(),
                intent.ttsRequested(),
                intent.sttRequested(),
                resolvedIntent == QuestionIntent.GAME_RECOMMENDATION || intent.recommendationIntent(),
                resolvedIntent == QuestionIntent.TREND_ANALYSIS || intent.trendIntent(),
                resolvedIntent == QuestionIntent.DEVELOPMENT_FEASIBILITY || intent.developmentIntent(),
                intent.specificGameIntent(),
                intent.soloTeam(),
                intent.smallTeam(),
                intent.shortPeriod()
        );
    }

    private OnboardingIntent preserveContextGenreForFollowUp(
            OnboardingIntent intent,
            ConversationContext conversationContext,
            String currentMessage
    ) {
        if (!conversationContext.hasHistory()
                || intent.primaryGenre() != PrimaryGenre.GENERAL
                || requestsDifferentGenre(currentMessage)) {
            return intent;
        }
        PrimaryGenre contextGenre = inferPrimaryGenreFromContext(conversationContext);
        if (contextGenre == PrimaryGenre.GENERAL) {
            return intent;
        }

        List<String> detectedKeywords = new ArrayList<>(intent.detectedKeywords());
        detectedKeywords.add(contextGenre.displayName());

        return new OnboardingIntent(
                intent.questionIntent(),
                contextGenre,
                detectedKeywords.stream().distinct().toList(),
                intent.preferredFeatures(),
                intent.platform(),
                intent.teamSize(),
                intent.developmentPeriod(),
                intent.scopeLabel(),
                contextGenre == PrimaryGenre.FPS || intent.fpsIntent(),
                contextGenre == PrimaryGenre.HORROR || intent.horrorIntent(),
                contextGenre == PrimaryGenre.PARTY || intent.partyIntent(),
                contextGenre == PrimaryGenre.PUZZLE_ADVENTURE || intent.puzzleAdventureIntent(),
                contextGenre == PrimaryGenre.SURVIVAL_ROGUELIKE || intent.survivalRoguelikeIntent(),
                contextGenre == PrimaryGenre.STREAMER_INTERACTION || intent.streamerIntent(),
                intent.webcamRequested(),
                intent.ttsRequested(),
                intent.sttRequested(),
                intent.recommendationIntent(),
                intent.trendIntent(),
                intent.developmentIntent(),
                intent.specificGameIntent(),
                intent.soloTeam(),
                intent.smallTeam(),
                intent.shortPeriod()
        );
    }

    private OnboardingIntent applyAgentPlanGenre(OnboardingIntent intent, AgentPlan agentPlan) {
        PrimaryGenre planGenre = primaryGenreFromAgentPlan(agentPlan);
        if (planGenre == PrimaryGenre.GENERAL || planGenre == intent.primaryGenre()) {
            return intent;
        }

        List<String> detectedKeywords = new ArrayList<>(intent.detectedKeywords());
        detectedKeywords.add(planGenre.displayName());

        return new OnboardingIntent(
                intent.questionIntent(),
                planGenre,
                detectedKeywords.stream().distinct().toList(),
                intent.preferredFeatures(),
                intent.platform(),
                intent.teamSize(),
                intent.developmentPeriod(),
                intent.scopeLabel(),
                planGenre == PrimaryGenre.FPS || intent.fpsIntent(),
                planGenre == PrimaryGenre.HORROR || intent.horrorIntent(),
                planGenre == PrimaryGenre.PARTY || intent.partyIntent(),
                planGenre == PrimaryGenre.PUZZLE_ADVENTURE || intent.puzzleAdventureIntent(),
                planGenre == PrimaryGenre.SURVIVAL_ROGUELIKE || intent.survivalRoguelikeIntent(),
                planGenre == PrimaryGenre.STREAMER_INTERACTION || intent.streamerIntent(),
                intent.webcamRequested(),
                intent.ttsRequested(),
                intent.sttRequested(),
                intent.recommendationIntent(),
                intent.trendIntent(),
                intent.developmentIntent(),
                intent.specificGameIntent(),
                intent.soloTeam(),
                intent.smallTeam(),
                intent.shortPeriod()
        );
    }

    private PrimaryGenre primaryGenreFromAgentPlan(AgentPlan agentPlan) {
        if (agentPlan == null || agentPlan.genreFilter() == null || agentPlan.genreFilter().isBlank()) {
            return PrimaryGenre.GENERAL;
        }
        return switch (agentPlan.genreFilter().strip().toUpperCase(Locale.ROOT)) {
            case "FPS" -> PrimaryGenre.FPS;
            case "HORROR" -> PrimaryGenre.HORROR;
            case "PARTY" -> PrimaryGenre.PARTY;
            case "SURVIVAL", "SURVIVAL_ROGUELIKE" -> PrimaryGenre.SURVIVAL_ROGUELIKE;
            case "PUZZLE", "PUZZLE_ADVENTURE" -> PrimaryGenre.PUZZLE_ADVENTURE;
            case "RPG" -> PrimaryGenre.RPG;
            case "MOBA" -> PrimaryGenre.MOBA;
            default -> PrimaryGenre.GENERAL;
        };
    }

    private boolean requestsDifferentGenre(String message) {
        String currentMessage = normalize(message);
        return containsAny(
                currentMessage,
                "다른 장르",
                "다른장르",
                "다른 것도",
                "다른것도",
                "다른 게임",
                "다른게임",
                "말고",
                "말고도"
        );
    }

    private PrimaryGenre inferPrimaryGenreFromContext(ConversationContext conversationContext) {
        String contextText = normalize("%s %s %s".formatted(
                conversationContext.message(),
                conversationContext.summary(),
                conversationContext.report()
        ));
        if (containsAny(contextText, "fps", "슈팅", "배그", "배틀로얄", "발로란트", "서든", "shooter")) {
            return PrimaryGenre.FPS;
        }
        if (containsAny(contextText, "공포", "호러", "horror")) {
            return PrimaryGenre.HORROR;
        }
        if (containsAny(contextText, "파티", "협동", "멀티", "party", "co-op", "coop", "친구랑", "친구와", "같이", "함께")) {
            return PrimaryGenre.PARTY;
        }
        if (containsAny(contextText, "퍼즐", "어드벤처", "puzzle", "adventure")) {
            return PrimaryGenre.PUZZLE_ADVENTURE;
        }
        if (containsAny(contextText, "생존", "로그라이크", "survival", "roguelike", "rogue-like")) {
            return PrimaryGenre.SURVIVAL_ROGUELIKE;
        }
        if (containsAny(contextText, "rpg", "롤플레잉", "role-playing", "role playing", "액션 rpg", "jrpg", "mmorpg")) {
            return PrimaryGenre.RPG;
        }
        if (containsAny(contextText, "moba", "리그오브레전드", "리그 오브 레전드", "도타", "dota", "league of legends")) {
            return PrimaryGenre.MOBA;
        }
        if (containsAny(contextText, "웹캠", "tts", "stt", "스트리머", "방송", "시청자 참여")) {
            return PrimaryGenre.STREAMER_INTERACTION;
        }
        return PrimaryGenre.GENERAL;
    }

    private boolean isMoreSpecificThanTrend(QuestionIntent questionIntent) {
        return questionIntent == QuestionIntent.GAME_RECOMMENDATION
                || questionIntent == QuestionIntent.DEVELOPMENT_FEASIBILITY
                || questionIntent == QuestionIntent.SPECIFIC_GAME_ANALYSIS
                || questionIntent == QuestionIntent.FEATURE_BASED_IDEA
                || questionIntent == QuestionIntent.STREAMING_FIT_ANALYSIS
                || questionIntent == QuestionIntent.GAME_REINTERPRETATION;
    }

    private QuestionIntent questionIntentFromAnalysisPurpose(String analysisPurpose) {
        return switch (nullToEmpty(analysisPurpose)) {
            case "USER_GAME_RECOMMENDATION" -> QuestionIntent.GAME_RECOMMENDATION;
            case "GAME_REINTERPRETATION" -> QuestionIntent.GAME_REINTERPRETATION;
            case "DEVELOPER_MARKET_ANALYSIS" -> QuestionIntent.DEVELOPMENT_FEASIBILITY;
            case "STREAMING_FIT_ANALYSIS" -> QuestionIntent.STREAMING_FIT_ANALYSIS;
            case "INTERACTION_GAME_IDEA" -> QuestionIntent.FEATURE_BASED_IDEA;
            default -> QuestionIntent.TREND_ANALYSIS;
        };
    }

    private String analysisPurposeLabel(String analysisPurpose) {
        return switch (nullToEmpty(analysisPurpose)) {
            case "USER_GAME_RECOMMENDATION" -> "플레이어 게임 추천";
            case "GAME_REINTERPRETATION" -> "과거 게임 재해석";
            case "DEVELOPER_MARKET_ANALYSIS" -> "개발자 시장 분석";
            case "STREAMING_FIT_ANALYSIS" -> "방송 적합성 분석";
            case "INTERACTION_GAME_IDEA" -> "인터랙션 게임 아이디어";
            default -> "트렌드 분석";
        };
    }

    private OnboardingIntent analyzeIntent(OnboardingAnalyzeRequest request) {
        String normalizedMessage = normalize(request.message());
        String normalizedPlatform = normalize(request.targetPlatform());
        List<String> features = normalizedFeatures(request.preferredFeatures());

        boolean fps = containsAny(normalizedMessage, "fps", "배그", "배틀로얄", "배틀 로얄", "슈팅", "shooting", "battleground", "battle royale");
        boolean horror = containsAny(normalizedMessage, "공포", "호러", "horror");
        boolean party = containsAny(normalizedMessage, "파티", "협동", "멀티", "multi", "co-op", "coop")
                || isFriendPlayRequest(normalizedMessage);
        boolean puzzleAdventure = containsAny(normalizedMessage, "퍼즐", "어드벤처", "puzzle", "adventure");
        boolean survivalRoguelike = containsAny(normalizedMessage, "생존", "로그라이크", "survival", "roguelike", "rogue-like");
        boolean rpg = containsAny(normalizedMessage, "rpg", "롤플레잉", "role-playing", "role playing", "액션 rpg", "jrpg", "mmorpg");
        boolean moba = containsAny(normalizedMessage, "moba", "롤", "리그오브레전드", "리그 오브 레전드", "도타", "dota", "league of legends");
        boolean streamer = containsAny(
                normalizedMessage,
                "스트리머",
                "방송",
                "유튜브",
                "시청자 참여",
                "시청자",
                "streamer",
                "streaming",
                "broadcast",
                "youtube"
        );
        boolean webcam = features.contains("webcam") || containsAny(normalizedMessage, "웹캠", "webcam", "표정", "리액션");
        boolean tts = features.contains("tts") || containsAny(normalizedMessage, "tts", "채팅", "chat", "시청자 참여");
        boolean stt = features.contains("stt") || containsAny(normalizedMessage, "stt", "음성", "마이크", "voice", "말로");
        boolean mobile = containsAny(normalizedMessage, "모바일", "mobile") || containsAny(normalizedPlatform, "mobile", "모바일");
        boolean web = containsAny(normalizedMessage, "웹", "web") || containsAny(normalizedPlatform, "web", "웹");
        boolean recommendation = containsAny(
                normalizedMessage,
                "나한테",
                "내가",
                "할만한 게임",
                "할 만한 게임",
                "할만 게임",
                "게임 추천",
                "추천해줘",
                "뭐가 좋을까",
                "뭐 하면 좋을까",
                "플레이할",
                "친구랑 할",
                "친구랑 한다고",
                "친구와 할",
                "같이 할",
                "함께 할",
                "혼자 할",
                "요즘 할만",
                "뭐 할",
                "뭘 할",
                "재밌는 게임"
        ) || isFriendPlayRequest(normalizedMessage);
        boolean platformTrendRequest = platformFilterResolver.resolve(request.message()).isPresent()
                && containsAny(normalizedMessage, "기준", "다시 분석", "알려줘", "추천");
        boolean trend = containsAny(normalizedMessage, "인기", "트렌드", "요즘", "뜨는", "유행", "전체 기준")
                || platformTrendRequest;
        boolean development = containsAny(
                normalizedMessage,
                "개발",
                "만들고 싶은데",
                "개발하면",
                "기획",
                "장르",
                "mvp",
                "프로토타입",
                "시장성",
                "개발자 관점",
                "만들",
                "만들고",
                "가능성",
                "출시"
        );
        boolean reinterpretation = containsAny(
                normalizedMessage,
                "과거 게임",
                "예전 게임",
                "옛날 게임",
                "재해석",
                "다시 만들",
                "리메이크",
                "레트로",
                "지금 다시",
                "이전에 있었던 게임"
        );
        boolean specificGame = containsAny(normalizedMessage, "배그", "pubg", "battleground", "롤", "리그 오브 레전드", "마인크래프트", "발로란트", "오버워치");

        PrimaryGenre primaryGenre = resolvePrimaryGenre(fps, horror, party, puzzleAdventure, survivalRoguelike, rpg, moba, streamer, webcam, tts, stt);
        QuestionIntent questionIntent = classifyQuestionIntent(
                recommendation,
                trend,
                development,
                reinterpretation,
                specificGame,
                streamer,
                webcam,
                tts,
                stt
        );
        String platform = resolvePlatform(request, normalizedMessage, mobile, web);
        String scopeLabel = resolveScopeLabel(request);

        List<String> detectedKeywords = buildDetectedKeywords(
                primaryGenre,
                fps,
                horror,
                party,
                puzzleAdventure,
                survivalRoguelike,
                rpg,
                moba,
                streamer,
                webcam,
                tts,
                stt,
                recommendation,
                trend,
                development,
                reinterpretation,
                specificGame,
                platform,
                scopeLabel
        );

        return new OnboardingIntent(
                questionIntent,
                primaryGenre,
                detectedKeywords,
                features,
                platform,
                displayValue(request.teamSize(), "미정"),
                displayValue(request.developmentPeriod(), "미정"),
                scopeLabel,
                fps,
                horror,
                party,
                puzzleAdventure,
                survivalRoguelike,
                streamer,
                webcam,
                tts,
                stt,
                recommendation,
                trend,
                development,
                specificGame,
                isSoloTeam(request),
                isSmallTeam(request),
                isShortPeriod(request)
        );
    }

    private QuestionIntent classifyQuestionIntent(
            boolean recommendation,
            boolean trend,
            boolean development,
            boolean reinterpretation,
            boolean specificGame,
            boolean streamer,
            boolean webcam,
            boolean tts,
            boolean stt
    ) {
        if (recommendation) {
            return QuestionIntent.GAME_RECOMMENDATION;
        }
        if (reinterpretation) {
            return QuestionIntent.GAME_REINTERPRETATION;
        }
        if (specificGame) {
            return QuestionIntent.SPECIFIC_GAME_ANALYSIS;
        }
        if (webcam || tts || stt) {
            return QuestionIntent.FEATURE_BASED_IDEA;
        }
        if (development) {
            return QuestionIntent.DEVELOPMENT_FEASIBILITY;
        }
        if (streamer) {
            return QuestionIntent.STREAMING_FIT_ANALYSIS;
        }
        if (trend) {
            return QuestionIntent.TREND_ANALYSIS;
        }
        return QuestionIntent.GENERAL_GAME_QUESTION;
    }

    private PrimaryGenre resolvePrimaryGenre(
            boolean fps,
            boolean horror,
            boolean party,
            boolean puzzleAdventure,
            boolean survivalRoguelike,
            boolean rpg,
            boolean moba,
            boolean streamer,
            boolean webcam,
            boolean tts,
            boolean stt
    ) {
        if (fps) {
            return PrimaryGenre.FPS;
        }
        if (horror) {
            return PrimaryGenre.HORROR;
        }
        if (party) {
            return PrimaryGenre.PARTY;
        }
        if (puzzleAdventure) {
            return PrimaryGenre.PUZZLE_ADVENTURE;
        }
        if (survivalRoguelike) {
            return PrimaryGenre.SURVIVAL_ROGUELIKE;
        }
        if (rpg) {
            return PrimaryGenre.RPG;
        }
        if (moba) {
            return PrimaryGenre.MOBA;
        }
        if (streamer || webcam || tts || stt) {
            return PrimaryGenre.STREAMER_INTERACTION;
        }
        return PrimaryGenre.GENERAL;
    }

    private List<String> buildDetectedKeywords(
            PrimaryGenre primaryGenre,
            boolean fps,
            boolean horror,
            boolean party,
            boolean puzzleAdventure,
            boolean survivalRoguelike,
            boolean rpg,
            boolean moba,
            boolean streamer,
            boolean webcam,
            boolean tts,
            boolean stt,
            boolean recommendation,
            boolean trend,
            boolean development,
            boolean reinterpretation,
            boolean specificGame,
            String platform,
            String scopeLabel
    ) {
        List<String> labels = new ArrayList<>();
        if (fps) {
            labels.add("FPS/배틀로얄/슈팅");
        }
        if (horror) {
            labels.add("공포/호러");
        }
        if (party) {
            labels.add("파티/협동/멀티");
        }
        if (puzzleAdventure) {
            labels.add("퍼즐/어드벤처");
        }
        if (survivalRoguelike) {
            labels.add("생존/로그라이크");
        }
        if (rpg) {
            labels.add("RPG/롤플레잉");
        }
        if (moba) {
            labels.add("MOBA");
        }
        if (streamer) {
            labels.add("스트리머/방송/Twitch/YouTube/시청자 참여");
        }
        if (webcam) {
            labels.add("Webcam");
        }
        if (tts) {
            labels.add("TTS/채팅");
        }
        if (stt) {
            labels.add("STT/음성");
        }
        if (recommendation) {
            labels.add("추천/할만한 게임");
        }
        if (trend) {
            labels.add("인기/트렌드/요즘");
        }
        if (development) {
            labels.add("개발/가능성");
        }
        if (reinterpretation) {
            labels.add("과거 게임 재해석");
        }
        if (specificGame) {
            labels.add("특정 게임 분석");
        }
        if (labels.isEmpty()) {
            labels.add(primaryGenre.displayName());
        }
        labels.add(platform);
        labels.add(scopeLabel);
        return labels;
    }

    private List<ConceptSeed> conceptSeedsFor(OnboardingIntent intent) {
        if (intent.questionIntent() == QuestionIntent.GAME_RECOMMENDATION) {
            return recommendationSeedsFor(intent);
        }
        if (intent.questionIntent() == QuestionIntent.TREND_ANALYSIS) {
            return List.of(
                    new ConceptSeed(
                            "Extraction Shooter Trend",
                            "Trend Analysis",
                            "배틀로얄 이후에는 탈출, 세션 긴장감, 아이템 손실 리스크를 섞은 슈팅 트렌드가 강합니다.",
                            88,
                            90,
                            55,
                            "general"
                    ),
                    new ConceptSeed(
                            "Co-op Survival Trend",
                            "Trend Analysis",
                            "협동 생존, 제작, 짧은 세션 클립화가 가능한 게임은 방송과 커뮤니티 확산에 유리합니다.",
                            86,
                            84,
                            70,
                            "general"
                    ),
                    new ConceptSeed(
                            "Streamer Party Trend",
                            "Trend Analysis",
                            "짧은 라운드, 실패 리액션, 시청자 밈을 만들 수 있는 파티 게임 흐름도 계속 강합니다.",
                            90,
                            80,
                            84,
                            "general"
                    )
            );
        }
        if (intent.questionIntent() == QuestionIntent.SPECIFIC_GAME_ANALYSIS) {
            return List.of(
                    new ConceptSeed(
                            "Core Popularity Breakdown",
                            "Specific Game Analysis",
                            "해당 게임의 인기 요인을 전투 긴장감, 반복 플레이, 소셜 확산 포인트로 나눠 분석하는 방향입니다.",
                            84,
                            88,
                            62,
                            "general"
                    ),
                    new ConceptSeed(
                            "Differentiation Angle",
                            "Specific Game Analysis",
                            "유사 게임을 만들기보다 규칙, 세션 길이, 조작 경험 중 하나를 바꾸는 차별화 전략이 필요합니다.",
                            80,
                            84,
                            60,
                            "general"
                    ),
                    new ConceptSeed(
                            "Streamer Clip Potential",
                            "Specific Game Analysis",
                            "시청자가 이해하기 쉬운 클러치, 반전, 리액션 장면이 있는지 방송 적합성을 따져볼 수 있습니다.",
                            88,
                            82,
                            68,
                            "general"
                    )
            );
        }
        if (intent.questionIntent() == QuestionIntent.STREAMING_FIT_ANALYSIS) {
            return List.of(
                    new ConceptSeed(
                            "Clip Moment Design",
                            "Streaming Fit",
                            "시청자가 바로 이해하는 반전, 실패, 극적 성공 순간을 만드는 구조가 핵심입니다.",
                            92,
                            80,
                            78,
                            "webcam"
                    ),
                    new ConceptSeed(
                            "Audience Trigger Loop",
                            "Streaming Fit",
                            "채팅이나 후원 이벤트를 게임 규칙과 연결하면 시청자 참여 이유가 명확해집니다.",
                            90,
                            78,
                            76,
                            "tts"
                    ),
                    new ConceptSeed(
                            "Streamer Challenge Format",
                            "Streaming Fit",
                            "스트리머가 반복 도전하고 시청자가 결과를 예측하기 쉬운 챌린지 포맷이 적합합니다.",
                            89,
                            76,
                            82,
                            "general"
                    )
            );
        }
        return switch (intent.primaryGenre()) {
            case FPS -> List.of(
                    new ConceptSeed(
                            "Tactical Extraction Lite",
                            "Tactical FPS",
                            "배그/FPS 관심을 반영하되, 대규모 배틀로얄 대신 작은 맵과 짧은 탈출 루프로 범위를 줄인 컨셉입니다.",
                            86,
                            92,
                            48,
                            "stt"
                    ),
                    new ConceptSeed(
                            "Streamer Squad Arena",
                            "Squad Shooter",
                            "스쿼드 전술, 관전 포인트, 클러치 상황을 만들어 스트리머 방송 반응에 맞춘 슈팅 컨셉입니다.",
                            91,
                            88,
                            54,
                            "webcam"
                    ),
                    new ConceptSeed(
                            "Voice Command Survival FPS",
                            "Survival FPS",
                            "음성 명령으로 핑, 재장전, 팀 콜을 실험할 수 있어 STT 포트폴리오 포인트가 분명합니다.",
                            84,
                            82,
                            58,
                            "stt"
                    )
            );
            case HORROR -> List.of(
                    new ConceptSeed(
                            "Reaction Horror Party",
                            "Horror Party",
                            "짧은 공포 이벤트와 리액션 장면을 결합해 방송에서 즉각적인 반응을 만들기 좋습니다.",
                            92,
                            84,
                            72,
                            "webcam"
                    ),
                    new ConceptSeed(
                            "Webcam Fear Room",
                            "Reaction Horror",
                            "웹캠 표정이나 움직임을 공포 트리거로 연결해 입력 기능과 장르 재미가 맞물립니다.",
                            90,
                            80,
                            70,
                            "webcam"
                    ),
                    new ConceptSeed(
                            "Voice Triggered Horror",
                            "Voice Horror",
                            "플레이어의 음성 크기나 특정 단어를 위험 요소로 쓰는 STT 기반 호러 컨셉입니다.",
                            86,
                            78,
                            74,
                            "stt"
                    )
            );
            case PARTY, STREAMER_INTERACTION -> List.of(
                    new ConceptSeed(
                            "Reaction Party Challenge",
                            "Party Challenge",
                            "짧은 라운드, 실패 리액션, 시청자 미션을 조합해 자연어 요청의 방송 친화성을 살린 컨셉입니다.",
                            91,
                            82,
                            84,
                            "webcam"
                    ),
                    new ConceptSeed(
                            "Chat TTS Party Room",
                            "Chat Party",
                            "채팅과 TTS를 게임 규칙으로 연결해 시청자 참여가 바로 드러나는 파티룸 컨셉입니다.",
                            88,
                            80,
                            82,
                            "tts"
                    ),
                    new ConceptSeed(
                            "Webcam Rhythm Battle",
                            "Rhythm Party",
                            "웹캠 움직임과 리듬 판정을 결합해 소규모 팀도 시각적으로 보여주기 쉬운 컨셉입니다.",
                            85,
                            76,
                            86,
                            "webcam"
                    )
            );
            case PUZZLE_ADVENTURE -> List.of(
                    new ConceptSeed(
                            "Voice Puzzle Adventure",
                            "Puzzle Adventure",
                            "음성 명령과 단서 조합을 활용해 퍼즐 해결 과정을 인터랙션 포인트로 보여줄 수 있습니다.",
                            76,
                            72,
                            78,
                            "stt"
                    ),
                    new ConceptSeed(
                            "Chat Clue Adventure",
                            "Chat Adventure",
                            "채팅/TTS를 단서 제공과 선택지 이벤트로 연결해 시청자 참여형 어드벤처를 만들 수 있습니다.",
                            80,
                            74,
                            82,
                            "tts"
                    ),
                    new ConceptSeed(
                            "Co-op Mystery Room",
                            "Co-op Puzzle",
                            "협동 퍼즐과 방 단위 구성을 사용해 개발 범위를 통제하기 쉬운 컨셉입니다.",
                            78,
                            70,
                            84,
                            "stt"
                    )
            );
            case SURVIVAL_ROGUELIKE -> List.of(
                    new ConceptSeed(
                            "Micro Survival Roguelike",
                            "Survival Roguelike",
                            "생존/로그라이크의 반복 플레이를 유지하되, 작은 아레나와 제한된 아이템 풀로 범위를 줄인 컨셉입니다.",
                            84,
                            80,
                            70,
                            "tts"
                    ),
                    new ConceptSeed(
                            "Chat Event Survival",
                            "Chat Survival",
                            "시청자 채팅이나 TTS를 랜덤 이벤트로 바꿔 방송 참여성과 생존 긴장감을 함께 만들 수 있습니다.",
                            88,
                            82,
                            74,
                            "tts"
                    ),
                    new ConceptSeed(
                            "Voice Command Survival Run",
                            "Voice Survival",
                            "음성 명령을 회피, 제작, 경고 시스템으로 연결하는 STT 기반 생존 컨셉입니다.",
                            82,
                            76,
                            72,
                            "stt"
                    )
            );
            case RPG -> List.of(
                    new ConceptSeed(
                            "Compact Quest RPG",
                            "RPG Prototype",
                            "RPG의 긴 성장 구조를 줄이고 전투, 선택지, 보상 루프 하나로 검증하는 컨셉입니다.",
                            78,
                            80,
                            68,
                            "general"
                    ),
                    new ConceptSeed(
                            "Build Craft Arena",
                            "Action RPG",
                            "빌드 선택과 짧은 전투를 결합해 RPG 취향을 빠르게 검증할 수 있습니다.",
                            82,
                            78,
                            70,
                            "tts"
                    ),
                    new ConceptSeed(
                            "Story Choice Dungeon",
                            "Narrative RPG",
                            "짧은 던전과 선택지 결과를 중심으로 RPG 몰입감을 작게 보여주는 방향입니다.",
                            74,
                            76,
                            72,
                            "stt"
                    )
            );
            case MOBA -> List.of(
                    new ConceptSeed(
                            "Mini Lane Brawler",
                            "MOBA Prototype",
                            "MOBA의 복잡도를 줄이고 한 라인, 소수 캐릭터, 짧은 한타만 검증하는 컨셉입니다.",
                            84,
                            82,
                            48,
                            "general"
                    ),
                    new ConceptSeed(
                            "Hero Draft Micro Match",
                            "Team Battle",
                            "캐릭터 선택과 상성 재미를 작게 보여주되 밸런싱 부담을 제한하는 방향입니다.",
                            82,
                            80,
                            50,
                            "tts"
                    ),
                    new ConceptSeed(
                            "Objective Fight Arena",
                            "Arena MOBA",
                            "타워와 라인을 줄이고 단일 오브젝트 전투로 팀플레이 재미를 검증합니다.",
                            80,
                            78,
                            52,
                            "general"
                    )
            );
            case GENERAL -> List.of(
                    new ConceptSeed(
                            "Interaction Mini Challenge",
                            "Streamer Mini Game",
                            "장르가 아직 넓을 때는 한 가지 입력 기술을 중심으로 빠르게 검증하는 미니 챌린지가 적합합니다.",
                            82,
                            68,
                            88,
                            "webcam"
                    ),
                    new ConceptSeed(
                            "Trend Prototype Lab",
                            "Prototype Collection",
                            "여러 미니 루프를 작게 비교해 어떤 장르와 입력 방식이 맞는지 확인하는 포트폴리오형 컨셉입니다.",
                            76,
                            66,
                            90,
                            "tts"
                    ),
                    new ConceptSeed(
                            "Voice Reaction Quest",
                            "Interaction Adventure",
                            "음성, 채팅, 리액션 중 하나를 핵심 루프로 잡아 자연어 요청을 구체화하기 좋은 컨셉입니다.",
                            78,
                            70,
                            82,
                            "stt"
                    )
            );
        };
    }

    private List<ConceptSeed> recommendationSeedsFor(OnboardingIntent intent) {
        boolean solo = intent.soloTeam();
        return switch (intent.primaryGenre()) {
            case FPS -> solo
                    ? List.of(
                    new ConceptSeed(
                            "Titanfall 2",
                            "Solo FPS",
                            "혼자 할 FPS라면 캠페인 완성도, 이동감, 짧은 전투 템포가 좋아 부담 없이 추천하기 좋습니다.",
                            82,
                            78,
                            90,
                            "general"
                    ),
                    new ConceptSeed(
                            "DOOM Eternal",
                            "Solo FPS",
                            "혼자 집중해서 하기 좋은 고속 전투형 FPS라서 팀 매칭 스트레스 없이 손맛을 느끼기 좋습니다.",
                            86,
                            80,
                            88,
                            "general"
                    ),
                    new ConceptSeed(
                            "Metro Exodus",
                            "Story FPS",
                            "전술 슈팅보다 스토리와 탐험을 같이 즐기는 싱글 FPS를 원할 때 잘 맞습니다.",
                            74,
                            72,
                            86,
                            "general"
                    )
            )
                    : List.of(
                    new ConceptSeed(
                            "VALORANT",
                            "Tactical FPS",
                            "친구와 경쟁전을 돌리거나 팀 합을 맞추는 FPS를 원할 때 추천할 수 있습니다.",
                            86,
                            88,
                            62,
                            "general"
                    ),
                    new ConceptSeed(
                            "Counter-Strike 2",
                            "Tactical FPS",
                            "전술 슈팅의 기본기를 원하고 방송/경쟁 신호까지 참고할 때 강한 후보입니다.",
                            84,
                            90,
                            58,
                            "general"
                    ),
                    new ConceptSeed(
                            "Apex Legends",
                            "Battle Royale FPS",
                            "스쿼드 기반 이동과 교전을 좋아한다면 팀 플레이용 FPS 후보로 볼 수 있습니다.",
                            82,
                            82,
                            60,
                            "general"
                    )
            );
            case RPG -> solo
                    ? List.of(
                    new ConceptSeed(
                            "Baldur's Gate 3",
                            "Solo RPG",
                            "혼자 천천히 선택지를 고르고 캐릭터 빌드를 즐기기 좋아 RPG 입문/몰입형 추천으로 강합니다.",
                            72,
                            88,
                            84,
                            "general"
                    ),
                    new ConceptSeed(
                            "Elden Ring",
                            "Action RPG",
                            "혼자 도전하는 전투와 탐험을 원한다면 가장 먼저 볼 만한 액션 RPG 후보입니다.",
                            82,
                            90,
                            78,
                            "general"
                    ),
                    new ConceptSeed(
                            "Cyberpunk 2077",
                            "Story RPG",
                            "스토리, 빌드 성장, 오픈월드 탐험을 혼자 즐기고 싶을 때 추천하기 좋습니다.",
                            76,
                            82,
                            82,
                            "general"
                    )
            )
                    : List.of(
                    new ConceptSeed(
                            "Monster Hunter Wilds",
                            "Co-op Action RPG",
                            "RPG 성장과 협동 사냥을 같이 원할 때 친구와 맞춰 하기 좋은 액션 RPG 후보입니다.",
                            84,
                            88,
                            74,
                            "general"
                    ),
                    new ConceptSeed(
                            "Baldur's Gate 3",
                            "Co-op RPG",
                            "친구와 선택지를 같이 고르고 다른 결과를 보는 식의 협동 RPG 경험이 강합니다.",
                            76,
                            88,
                            82,
                            "general"
                    ),
                    new ConceptSeed(
                            "Path of Exile 2",
                            "Action RPG",
                            "빌드 파밍과 반복 플레이를 좋아한다면 친구와 오래 할 수 있는 액션 RPG 후보입니다.",
                            78,
                            84,
                            70,
                            "general"
                    )
            );
            case HORROR -> List.of(
                    new ConceptSeed(
                            solo ? "Resident Evil 4" : "Phasmophobia",
                            solo ? "Solo Horror" : "Co-op Horror",
                            solo
                                    ? "혼자 하는 공포 게임이라면 액션과 긴장감의 균형이 좋아 추천하기 쉽습니다."
                                    : "친구와 역할을 나누고 놀라는 상황이 계속 생겨 같이 하기 좋은 공포 후보입니다.",
                            84,
                            82,
                            solo ? 82 : 74,
                            "general"
                    ),
                    new ConceptSeed(
                            solo ? "Amnesia: The Bunker" : "Lethal Company",
                            solo ? "Solo Horror" : "Co-op Horror",
                            solo
                                    ? "짧고 밀도 있는 생존 공포를 혼자 즐기고 싶을 때 잘 맞습니다."
                                    : "협동 실수와 리액션이 재미가 되는 구조라 친구와 할 때 강합니다.",
                            82,
                            78,
                            solo ? 80 : 78,
                            "general"
                    ),
                    new ConceptSeed(
                            solo ? "Little Nightmares II" : "Content Warning",
                            solo ? "Horror Adventure" : "Co-op Horror",
                            solo
                                    ? "무거운 조작보다 분위기와 퍼즐 중심의 공포를 원할 때 추천하기 좋습니다."
                                    : "같이 촬영하고 도망치는 식의 소셜 리액션이 좋아 친구용 후보로 맞습니다.",
                            78,
                            74,
                            84,
                            "general"
                    )
            );
            case PARTY, STREAMER_INTERACTION -> List.of(
                    new ConceptSeed(
                            "It Takes Two",
                            "Co-op Adventure",
                            "친구 한 명과 확실히 같이 할 게임을 찾는다면 협동 퍼즐과 액션 분담이 가장 명확합니다.",
                            78,
                            80,
                            90,
                            "general"
                    ),
                    new ConceptSeed(
                            "Overcooked! 2",
                            "Party Co-op",
                            "짧은 라운드와 실수에서 웃음이 나와 가볍게 같이 하기 좋습니다.",
                            86,
                            76,
                            88,
                            "general"
                    ),
                    new ConceptSeed(
                            "Lethal Company",
                            "Co-op Horror",
                            "협동, 공포, 웃긴 사고가 같이 나와 친구들과 오래 이야기할 장면이 많습니다.",
                            90,
                            84,
                            78,
                            "general"
                    )
            );
            case SURVIVAL_ROGUELIKE -> List.of(
                    new ConceptSeed(
                            solo ? "Subnautica" : "Valheim",
                            solo ? "Solo Survival" : "Co-op Survival",
                            solo
                                    ? "혼자 탐험과 생존 몰입을 즐기기 좋아 싱글 생존 게임으로 추천하기 좋습니다."
                                    : "친구와 기지를 만들고 탐험 목표를 나누기 좋은 협동 생존 후보입니다.",
                            78,
                            80,
                            84,
                            "general"
                    ),
                    new ConceptSeed(
                            solo ? "Hades" : "Minecraft",
                            solo ? "Action Roguelike" : "Sandbox Survival",
                            solo
                                    ? "짧은 반복 플레이와 성장감이 강해 혼자 꾸준히 하기 좋습니다."
                                    : "목표를 정하지 않아도 같이 건설하고 탐험하기 좋아 친구용으로 안정적입니다.",
                            84,
                            86,
                            88,
                            "general"
                    ),
                    new ConceptSeed(
                            solo ? "The Long Dark" : "Palworld",
                            solo ? "Solo Survival" : "Co-op Survival",
                            solo
                                    ? "전투보다 생존 판단과 분위기를 원할 때 잘 맞는 혼자용 후보입니다."
                                    : "수집, 전투, 기지 운영을 친구와 나눠 하기 쉬운 생존형 후보입니다.",
                            76,
                            78,
                            82,
                            "general"
                    )
            );
            case PUZZLE_ADVENTURE -> List.of(
                    new ConceptSeed(
                            solo ? "Outer Wilds" : "Portal 2",
                            solo ? "Puzzle Adventure" : "Co-op Puzzle",
                            solo
                                    ? "혼자 단서를 연결하며 탐험하는 맛이 강해 퍼즐/어드벤처 추천으로 좋습니다."
                                    : "친구와 서로 역할을 나눠 퍼즐을 푸는 협동 구조가 명확합니다.",
                            74,
                            78,
                            88,
                            "general"
                    ),
                    new ConceptSeed(
                            solo ? "Return of the Obra Dinn" : "Escape Simulator",
                            solo ? "Deduction Puzzle" : "Co-op Puzzle",
                            solo
                                    ? "혼자 추리하고 기록을 맞추는 깊이가 있어 조용히 몰입하기 좋습니다."
                                    : "방 단위 퍼즐을 같이 풀기 좋아 부담 없는 협동 후보입니다.",
                            70,
                            74,
                            90,
                            "general"
                    ),
                    new ConceptSeed(
                            solo ? "Tunic" : "We Were Here Forever",
                            solo ? "Adventure Puzzle" : "Co-op Adventure",
                            solo
                                    ? "탐험과 퍼즐, 발견의 재미를 혼자 즐기기 좋은 후보입니다."
                                    : "서로 다른 정보를 말로 공유해야 해서 친구와 할 때 재미가 살아납니다.",
                            76,
                            76,
                            84,
                            "general"
                    )
            );
            case MOBA -> List.of(
                    new ConceptSeed(
                            "League of Legends",
                            "MOBA",
                            "친구와 팀 경쟁을 하고 싶다면 접근성과 국내 플레이 풀이 큰 MOBA 후보입니다.",
                            86,
                            90,
                            54,
                            "general"
                    ),
                    new ConceptSeed(
                            "Dota 2",
                            "MOBA",
                            "복잡한 전략과 깊은 팀 전투를 원할 때 맞지만, 진입 장벽은 높게 봐야 합니다.",
                            84,
                            86,
                            50,
                            "general"
                    ),
                    new ConceptSeed(
                            "Eternal Return",
                            "MOBA Battle Royale",
                            "MOBA식 성장과 배틀로얄 흐름을 섞은 후보라 다른 맛을 원할 때 볼 만합니다.",
                            78,
                            78,
                            58,
                            "general"
                    )
            );
            case GENERAL -> List.of(
                    new ConceptSeed(
                            "Monster Hunter Wilds",
                            "Action RPG",
                            "장르를 아직 정하지 않았다면 혼자와 협동을 모두 커버할 수 있는 액션 RPG를 먼저 볼 만합니다.",
                            84,
                            88,
                            76,
                            "general"
                    ),
                    new ConceptSeed(
                            "Minecraft",
                            "Sandbox Survival",
                            "혼자/친구 모두 가능한 범용성이 커서 취향을 더 좁히기 전 기본 후보로 안정적입니다.",
                            82,
                            84,
                            90,
                            "general"
                    ),
                    new ConceptSeed(
                            "Lethal Company",
                            "Co-op Horror",
                            "친구와 짧게 웃으며 할 수 있는 협동형 후보라 일반 추천에서 같이 제안하기 좋습니다.",
                            90,
                            82,
                            78,
                            "general"
                    )
            );
        };
    }

    private RecommendedConceptResponse toRecommendedConcept(
            ConceptSeed seed,
            OnboardingAnalyzeRequest request,
            OnboardingIntent intent
    ) {
        int streamabilityScore = adjustScore(seed.streamabilityScore(), streamabilityBonus(seed, intent));
        int marketSignalScore = adjustScore(seed.marketSignalScore(), marketSignalBonus(intent));
        int devFeasibilityScore = adjustScore(seed.devFeasibilityScore(), devFeasibilityAdjustment(intent));
        String reason = buildReason(seed.reason(), request, intent, seed.featureFocus(), devFeasibilityScore);

        return new RecommendedConceptResponse(
                seed.title(),
                seed.genre(),
                reason,
                streamabilityScore,
                marketSignalScore,
                devFeasibilityScore
        );
    }

    private int streamabilityBonus(ConceptSeed seed, OnboardingIntent intent) {
        int bonus = 0;
        if (intent.streamerIntent()) {
            bonus += 5;
        }
        if ("webcam".equals(seed.featureFocus()) && intent.webcamRequested()) {
            bonus += 6;
        }
        if ("tts".equals(seed.featureFocus()) && intent.ttsRequested()) {
            bonus += 5;
        }
        if ("stt".equals(seed.featureFocus()) && intent.sttRequested()) {
            bonus += 5;
        }
        if (intent.primaryGenre() == PrimaryGenre.FPS && intent.webcamRequested()) {
            bonus += 2;
        }
        return bonus;
    }

    private int marketSignalBonus(OnboardingIntent intent) {
        return switch (intent.primaryGenre()) {
            case FPS -> 5;
            case HORROR -> intent.streamerIntent() ? 4 : 2;
            case PARTY, STREAMER_INTERACTION -> 3;
            case SURVIVAL_ROGUELIKE -> 2;
            case PUZZLE_ADVENTURE -> -1;
            case RPG -> 3;
            case MOBA -> 4;
            case GENERAL -> 0;
        };
    }

    private int devFeasibilityAdjustment(OnboardingIntent intent) {
        int adjustment = 0;
        if (intent.soloTeam()) {
            adjustment -= 10;
        } else if (intent.smallTeam()) {
            adjustment -= 3;
        }
        if (intent.shortPeriod()) {
            adjustment -= 8;
        }
        if (intent.primaryGenre() == PrimaryGenre.FPS) {
            adjustment -= 9;
        }
        if (intent.primaryGenre() == PrimaryGenre.PARTY || intent.primaryGenre() == PrimaryGenre.STREAMER_INTERACTION) {
            adjustment += 7;
        }
        if (intent.primaryGenre() == PrimaryGenre.HORROR) {
            adjustment += 3;
        }
        if (intent.primaryGenre() == PrimaryGenre.RPG || intent.primaryGenre() == PrimaryGenre.MOBA) {
            adjustment -= 4;
        }
        return adjustment;
    }

    private String buildReason(
            String baseReason,
            OnboardingAnalyzeRequest request,
            OnboardingIntent intent,
            String featureFocus,
            int devFeasibilityScore
    ) {
        String featureReason = switch (featureFocus) {
            case "webcam" -> intent.webcamRequested() ? "Webcam 선호가 있어 리액션 연출 점수를 높게 봤습니다." : "Webcam을 선택하지 않아도 시각적 리액션을 확장 포인트로 둘 수 있습니다.";
            case "tts" -> intent.ttsRequested() ? "TTS/채팅 의도가 있어 시청자 참여 루프와 잘 맞습니다." : "TTS는 선택 기능으로 후순위 확장에 적합합니다.";
            case "stt" -> intent.sttRequested() ? "STT/음성 의도가 있어 조작·명령 실험에 적합합니다." : "STT는 기술 리스크가 있어 MVP 이후 검증이 좋습니다.";
            default -> "선호 기능을 MVP 범위에 맞게 조합하는 방식이 적합합니다.";
        };
        String feasibilityReason = devFeasibilityScore < 60
                ? "다만 현재 팀/기간 조건에서는 구현 범위를 강하게 줄여야 합니다."
                : "현재 팀/기간 조건에서도 MVP 단위 검증이 가능합니다.";

        return "%s %s %s 입력 조건(%s, %s, %s)을 반영했습니다.".formatted(
                baseReason,
                featureReason,
                feasibilityReason,
                displayValue(request.targetPlatform(), "목표 플랫폼 미정"),
                displayValue(request.teamSize(), "팀 규모 미정"),
                displayValue(request.developmentPeriod(), "개발 기간 미정")
        );
    }

    private String buildMvpDirection(OnboardingIntent intent) {
        if (intent.questionIntent() == QuestionIntent.GAME_RECOMMENDATION) {
            return "플레이어용 추천이라면 선호 장르, 같이 할 사람 수, 플레이 시간, 난이도 취향을 더 받으면 추천 정확도가 올라갑니다.";
        }
        if (intent.questionIntent() == QuestionIntent.TREND_ANALYSIS) {
            return "트렌드 분석은 Twitch/CHZZK/SOOP 라이브 순위, 방송 수, 시청자 수, 커뮤니티 확산 여부를 함께 보면 더 정확해집니다.";
        }
        if (intent.questionIntent() == QuestionIntent.GAME_REINTERPRETATION) {
            return "과거 게임 재해석은 원작의 핵심 메커니즘 하나만 골라 Webcam/TTS/STT 또는 시청자 참여 규칙으로 바꾸고, 소규모 MVP에서는 한 모드와 짧은 세션으로 검증하는 편이 좋습니다.";
        }
        if (intent.questionIntent() == QuestionIntent.SPECIFIC_GAME_ANALYSIS) {
            return "특정 게임 분석은 인기 요인, 유지율을 만드는 반복 루프, 유사작과의 차별점, 방송 확산성을 나눠 보는 것이 좋습니다.";
        }
        if (intent.questionIntent() == QuestionIntent.STREAMING_FIT_ANALYSIS) {
            return "방송 적합성은 시청자가 이해하기 쉬운 규칙, 짧은 리액션 주기, 클립으로 남을 장면을 먼저 설계하는 것이 좋습니다.";
        }
        return switch (intent.primaryGenre()) {
            case FPS -> "FPS/배틀로얄은 네트워크, 맵, 밸런싱 부담이 크므로 첫 버전은 1개 소형 맵, 1개 모드, 2~3개 무기, 봇 또는 제한된 멀티플레이로 검증하는 편이 좋습니다.";
            case HORROR -> "호러는 방 1~2개, 공포 이벤트 5개 이하, Webcam 또는 STT 트리거 1개를 핵심 루프로 잡으면 짧은 기간에도 설득력 있는 MVP가 됩니다.";
            case PARTY, STREAMER_INTERACTION -> "파티/방송형은 60~90초 라운드, 실패 리액션, 채팅/TTS/Webcam 중 하나의 핵심 입력을 먼저 완성하고 나머지는 확장 기능으로 두는 것이 좋습니다.";
            case PUZZLE_ADVENTURE -> "퍼즐/어드벤처는 스테이지 2~3개, 단서 시스템 1개, 음성 또는 채팅 힌트 1개를 중심으로 구현 범위를 제한하는 것이 좋습니다.";
            case SURVIVAL_ROGUELIKE -> "생존/로그라이크는 작은 아레나, 짧은 웨이브, 제한된 아이템 풀로 시작하고 채팅 이벤트나 음성 명령을 하나만 연결하는 편이 안전합니다.";
            case RPG -> "RPG는 전체 성장 시스템보다 전투 한 종류, 빌드 2~3개, 짧은 퀘스트 한 줄기로 먼저 검증하는 편이 좋습니다.";
            case MOBA -> "MOBA는 밸런싱 부담이 크므로 영웅 3~4개와 단일 맵 목표 하나로 팀 전투 재미만 먼저 검증하는 편이 좋습니다.";
            case GENERAL -> "아직 장르가 넓으므로 한 가지 입력 기술과 1분짜리 플레이 루프를 먼저 만들고, 이후 사용자 반응에 따라 장르를 좁히는 방식이 좋습니다.";
        };
    }

    private List<String> buildFollowUpQuestions(
            OnboardingIntent intent,
            FollowUpFocus followUpFocus,
            ConversationContext conversationContext,
            List<LiveTrendGameResponse> liveTrendSignals
    ) {
        List<String> questions = new ArrayList<>();
        if (intent.questionIntent() != QuestionIntent.GAME_RECOMMENDATION
                && usesLiveTrendFollowUps(intent, liveTrendSignals)) {
            questions.add("Twitch 기준으로 다시 분석해줘");
            questions.add("CHZZK 기준으로 다시 분석해줘");
            questions.add("스트리머 확산도 기준으로 다시 분석해줘");
            questions.add("시청자 수보다 방송 수 중심으로 다시 분석해줘");
        }

        switch (intent.questionIntent()) {
            case GAME_RECOMMENDATION -> {
                questions.add("혼자 할 게임 기준으로 추천해줘");
                questions.add("친구랑 할 게임 기준으로 추천해줘");
                if (liveTrendSignals != null && !liveTrendSignals.isEmpty()) {
                    questions.add("Twitch 기준으로 다시 분석해줘");
                    questions.add("시청자 수보다 방송 수 중심으로 다시 분석해줘");
                } else {
                    questions.add("가볍게 할 게임 기준으로 추천해줘");
                    questions.add("치지직 기준으로 인기 있는 게임 추천해줘");
                }
            }
            case TREND_ANALYSIS -> {
                questions.add("PC 트렌드 기준으로 다시 분석해줘");
                questions.add("개발 기회 중심으로 다시 분석해줘");
                questions.add("라이브 순위 근거 중심으로 다시 분석해줘");
            }
            case GAME_REINTERPRETATION -> {
                questions.add("웹캠 중심으로 다시 추천해줘");
                questions.add("TTS/STT 중심으로 다시 분석해줘");
                questions.add("소규모 팀이 만들기 쉬운 후보만 알려줘");
                questions.add("스트리밍 반응성이 높은 후보만 알려줘");
            }
            case DEVELOPMENT_FEASIBILITY, FEATURE_BASED_IDEA -> {
                questions.add("프로토타입 수준으로 다시 분석해줘");
                questions.add("출시 가능한 MVP 기준으로 다시 분석해줘");
                questions.add(intent.soloTeam() ? "소규모 팀 기준으로 다시 분석해줘" : "1인 개발 기준으로 다시 분석해줘");
            }
            case SPECIFIC_GAME_ANALYSIS -> {
                questions.add("인기 지속 가능성 중심으로 다시 분석해줘");
                questions.add("유사 게임 개발 가능성 기준으로 다시 분석해줘");
                questions.add("방송 확산성 기준으로 다시 분석해줘");
            }
            case STREAMING_FIT_ANALYSIS -> {
                questions.add("스트리머 타깃 기준으로 다시 분석해줘");
                questions.add("시청자 참여 기능 중심으로 다시 분석해줘");
                questions.add("클립화 가능성 기준으로 다시 분석해줘");
            }
            case GENERAL_GAME_QUESTION -> {
                questions.add("플레이할 게임 추천 관점으로 답해줘");
                questions.add("개발 아이디어 관점으로 답해줘");
                questions.add("요즘 트렌드 분석 관점으로 답해줘");
            }
        }
        if ("플랫폼 미정".equals(intent.platform()) && followUpFocus != FollowUpFocus.MOBILE_PLATFORM) {
            questions.add("PC 기준으로 다시 분석해줘");
        }
        if (conversationContext.hasHistory() && followUpFocus != FollowUpFocus.STREAMER_TARGET) {
            questions.add("스트리머 타깃 기준으로 다시 분석해줘");
        }
        if (conversationContext.hasHistory() && followUpFocus == FollowUpFocus.SCOPE_DECISION) {
            questions.add("프로토타입 수준으로 다시 분석해줘");
            questions.add("출시 가능한 MVP 기준으로 다시 분석해줘");
        }
        return questions.stream().distinct().limit(4).toList();
    }

    private boolean usesLiveTrendFollowUps(OnboardingIntent intent, List<LiveTrendGameResponse> liveTrendSignals) {
        return intent.questionIntent() == QuestionIntent.TREND_ANALYSIS
                || intent.questionIntent() == QuestionIntent.STREAMING_FIT_ANALYSIS
                || (intent.questionIntent() == QuestionIntent.GAME_RECOMMENDATION
                && liveTrendSignals != null
                && !liveTrendSignals.isEmpty());
    }

    private OnboardingAnalysisHistory toHistoryEntity(
            PersistentConversation persistentConversation,
            OnboardingAnalyzeRequest request,
            OnboardingAnalyzeRequest analysisRequest,
            ConversationContext conversationContext,
            String conversationId,
            String summary,
            List<RecommendedConceptResponse> concepts,
            String report
    ) {
        return OnboardingAnalysisHistory.builder()
                .userId(persistentConversation.userId())
                .projectId(null)
                .parentHistoryId(conversationContext.hasHistory() ? conversationContext.historyId() : null)
                .conversationId(conversationId)
                .message(request.message().strip())
                .targetPlatform(stripToNull(analysisRequest.targetPlatform()))
                .teamSize(stripToNull(analysisRequest.teamSize()))
                .preferredFeaturesJson(writeJson(resolvePreferredFeatures(analysisRequest.preferredFeatures())))
                .developmentPeriod(stripToNull(analysisRequest.developmentPeriod()))
                .summary(summary)
                .recommendedConceptsJson(writeJson(concepts))
                .report(report)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private OnboardingHistoryItemResponse toHistoryItemResponse(OnboardingAnalysisHistory history) {
        List<RecommendedConceptResponse> concepts = readRecommendedConcepts(history.getRecommendedConceptsJson());

        return new OnboardingHistoryItemResponse(
                history.getId(),
                history.getProjectId(),
                history.getParentHistoryId(),
                history.getConversationId(),
                history.getMessage(),
                history.getTargetPlatform(),
                history.getTeamSize(),
                readPreferredFeatures(history.getPreferredFeaturesJson()),
                history.getDevelopmentPeriod(),
                history.getSummary(),
                concepts.size(),
                history.getCreatedAt()
        );
    }

    private OnboardingHistoryDetailResponse toHistoryDetailResponse(OnboardingAnalysisHistory history) {
        return new OnboardingHistoryDetailResponse(
                history.getId(),
                history.getProjectId(),
                history.getParentHistoryId(),
                history.getConversationId(),
                history.getMessage(),
                history.getTargetPlatform(),
                history.getTeamSize(),
                readPreferredFeatures(history.getPreferredFeaturesJson()),
                history.getDevelopmentPeriod(),
                history.getSummary(),
                readRecommendedConcepts(history.getRecommendedConceptsJson()),
                history.getReport(),
                history.getCreatedAt()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("온보딩 분석 이력 JSON 직렬화에 실패했습니다.", ex);
        }
    }

    private List<String> readPreferredFeatures(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, STRING_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            log.warn("선호 기능 JSON 파싱 실패. raw={}", rawJson, ex);
            return List.of();
        }
    }

    private List<RecommendedConceptResponse> readRecommendedConcepts(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, RECOMMENDED_CONCEPT_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            log.warn("추천 컨셉 JSON 파싱 실패. raw={}", rawJson, ex);
            return List.of();
        }
    }

    private List<String> resolvePreferredFeatures(List<String> preferredFeatures) {
        if (preferredFeatures == null || preferredFeatures.isEmpty()) {
            return List.of();
        }
        return preferredFeatures.stream()
                .filter(feature -> feature != null && !feature.isBlank())
                .map(String::strip)
                .toList();
    }

    private String stripToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private String resolvePlatform(OnboardingAnalyzeRequest request, String normalizedMessage, boolean mobile, boolean web) {
        if (request.targetPlatform() != null && !request.targetPlatform().isBlank()) {
            return request.targetPlatform().strip();
        }
        if (mobile) {
            return "Mobile";
        }
        if (web) {
            return "Web";
        }
        if (containsAny(normalizedMessage, "pc", "피씨", "스팀", "steam")) {
            return "PC";
        }
        return "플랫폼 미정";
    }

    private String resolveScopeLabel(OnboardingAnalyzeRequest request) {
        String developmentPeriod = normalize(request.developmentPeriod());
        if (containsAny(developmentPeriod, "1 month", "2 months", "3 months", "1개월", "2개월", "3개월")) {
            return "단기 MVP";
        }
        if (containsAny(developmentPeriod, "4 months", "5 months", "6 months", "4개월", "5개월", "6개월")) {
            return "중기 MVP";
        }
        if (containsAny(developmentPeriod, "12 months", "1 year", "12개월", "1년")) {
            return "확장 프로젝트";
        }
        return "범위 미정 MVP";
    }

    private boolean isSoloTeam(OnboardingAnalyzeRequest request) {
        String teamSize = normalize(request.teamSize());
        return containsAny(teamSize, "solo", "1인", "혼자", "개인");
    }

    private boolean isSmallTeam(OnboardingAnalyzeRequest request) {
        String teamSize = normalize(request.teamSize());
        return isSoloTeam(request) || containsAny(teamSize, "small", "소규모", "작은", "인디");
    }

    private boolean isShortPeriod(OnboardingAnalyzeRequest request) {
        String developmentPeriod = normalize(request.developmentPeriod());
        return containsAny(developmentPeriod, "1 month", "2 months", "3 months", "1개월", "2개월", "3개월");
    }

    private int totalScore(RecommendedConceptResponse concept) {
        return concept.streamabilityScore() + concept.marketSignalScore() + concept.devFeasibilityScore();
    }

    private int adjustScore(int baseScore, int bonus) {
        return Math.min(100, Math.max(0, baseScore + bonus));
    }

    private List<String> normalizedFeatures(List<String> preferredFeatures) {
        if (preferredFeatures == null || preferredFeatures.isEmpty()) {
            return List.of();
        }

        return preferredFeatures.stream()
                .filter(feature -> feature != null && !feature.isBlank())
                .map(this::normalize)
                .toList();
    }

    private String displayFeatures(List<String> preferredFeatures) {
        if (preferredFeatures == null || preferredFeatures.isEmpty()) {
            return "미정";
        }

        String features = preferredFeatures.stream()
                .filter(feature -> feature != null && !feature.isBlank())
                .map(String::strip)
                .collect(Collectors.joining(", "));
        if (features.isBlank()) {
            return "미정";
        }
        return features;
    }

    private String displayValue(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.strip();
    }

    private String dataOriginDescription(String dataOrigin) {
        if ("REAL".equalsIgnoreCase(nullToEmpty(dataOrigin))) {
            return "실제 수집 데이터 기준";
        }
        if ("FALLBACK".equalsIgnoreCase(nullToEmpty(dataOrigin))) {
            return "시연용 fallback 데이터";
        }
        if ("PARTIAL".equalsIgnoreCase(nullToEmpty(dataOrigin))) {
            return "부분 수집 데이터";
        }
        return "출처 상태 미정";
    }

    private String signalStatusDescription(String signalStatus) {
        if ("COMPLETE".equalsIgnoreCase(nullToEmpty(signalStatus))) {
            return "수집 완료";
        }
        if ("PARTIAL".equalsIgnoreCase(nullToEmpty(signalStatus))) {
            return "부분 수집 / fallback 포함";
        }
        return "수집 상태 미정";
    }

    private String partialCaution(LiveTrendGameResponse game) {
        if ("PARTIAL".equalsIgnoreCase(nullToEmpty(game.signalStatus()))
                || "FALLBACK".equalsIgnoreCase(nullToEmpty(game.dataOrigin()))
                || "PARTIAL".equalsIgnoreCase(nullToEmpty(game.dataOrigin()))) {
            return "부분 수집 데이터는 보조 신호로만 해석하고, 실제 데이터와 fallback/partial 데이터가 섞였을 수 있어 보수적으로 봐야 합니다.";
        }
        return "";
    }

    private String platformBasisLabel(String source) {
        if ("TWITCH".equalsIgnoreCase(nullToEmpty(source))) {
            return "Twitch 기준";
        }
        if ("CHZZK".equalsIgnoreCase(nullToEmpty(source))) {
            return "CHZZK 기준";
        }
        if ("SOOP".equalsIgnoreCase(nullToEmpty(source))) {
            return "SOOP 기준";
        }
        if ("STEAM".equalsIgnoreCase(nullToEmpty(source))) {
            return "Steam 기준";
        }
        return "플랫폼 미정 기준";
    }

    private String selectedPlatformValue(Optional<String> selectedPlatform) {
        return selectedPlatform.orElse("ALL");
    }

    private Optional<String> selectedPlatform(AgentQueryConditionResponse queryCondition) {
        if (queryCondition == null
                || queryCondition.platformFilter() == null
                || queryCondition.platformFilter().isBlank()
                || "ALL".equalsIgnoreCase(queryCondition.platformFilter())) {
            return Optional.empty();
        }
        return Optional.of(queryCondition.platformFilter().strip().toUpperCase(Locale.ROOT));
    }

    private String displayQueryConditionFeatures(AgentQueryConditionResponse queryCondition) {
        if (queryCondition == null || queryCondition.interactionFeatures() == null || queryCondition.interactionFeatures().isEmpty()) {
            return "없음";
        }
        return String.join(", ", queryCondition.interactionFeatures());
    }

    private String sortMetricLabel(String sortMetric) {
        return switch (nullToEmpty(sortMetric)) {
            case "VIEWER_COUNT" -> "시청자 수";
            case "STREAM_COUNT" -> "방송 수";
            case "STREAMER_SPREAD" -> "스트리머 확산도";
            case "MARKET_SIGNAL" -> "시장 신호";
            default -> "트렌드 점수";
        };
    }

    private String liveTrendScopeLabel(Optional<String> selectedPlatform) {
        return selectedPlatform
                .map(platform -> switch (platform) {
                    case "TWITCH" -> "Twitch 라이브 트렌드";
                    case "CHZZK" -> "CHZZK 라이브 트렌드";
                    case "SOOP" -> "SOOP 라이브 트렌드";
                    case "STEAM" -> "Steam 라이브 트렌드";
                    default -> platform + " 라이브 트렌드";
                })
                .orElse("라이브 트렌드");
    }

    private String liveTrendMissingDataMessage(Optional<String> selectedPlatform) {
        return selectedPlatform
                .map(platform -> "%s 기준으로 수집된 데이터가 아직 부족합니다. /live-trends에서 수동 갱신을 먼저 실행해주세요."
                        .formatted(platform))
                .orElse("아직 수집된 라이브 트렌드 데이터가 부족합니다. /live-trends에서 수동 갱신을 먼저 실행해주세요.");
    }

    private String extractAgentGenreFilter(String message) {
        String normalized = normalize(message);
        if (containsAny(normalized, "fps", "슈팅", "배그", "배틀로얄", "발로란트", "서든")) {
            return "FPS";
        }
        if (containsAny(normalized, "공포", "horror", "호러", "귀신")) {
            return "HORROR";
        }
        if (isFriendPlayRequest(normalized)
                || containsAny(normalized, "파티", "party", "같이", "함께", "친구", "협동", "멀티", "co-op", "coop")) {
            return "PARTY";
        }
        if (containsAny(normalized, "생존", "서바이벌", "survival", "마크", "minecraft")) {
            return "SURVIVAL";
        }
        if (containsAny(normalized, "rpg", "롤플레잉")) {
            return "RPG";
        }
        if (containsAny(normalized, "moba", "롤", "리그오브레전드", "도타")) {
            return "MOBA";
        }
        if (containsAny(normalized, "퍼즐", "puzzle", "어드벤처")) {
            return "PUZZLE";
        }
        return null;
    }

    private boolean isSoloPlayRequest(String message) {
        return containsAny(normalize(message), "혼자", "솔로", "1인", "싱글", "single", "singleplayer", "single-player", "solo");
    }

    private boolean isFriendPlayRequest(String message) {
        String normalized = normalize(message);
        return containsAny(
                normalized,
                "친구랑",
                "친구와",
                "친구들이랑",
                "친구",
                "같이",
                "함께",
                "둘이",
                "여럿",
                "협동",
                "멀티",
                "co-op",
                "coop"
        ) && containsAny(
                normalized,
                "게임",
                "추천",
                "할",
                "한다",
                "한다고",
                "했는데",
                "플레이",
                "즐길"
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String responseDepthFor(String message) {
        String normalized = normalize(message);
        if (containsAny(normalized, "자세히", "구체적으로", "이유까지", "분석해줘", "리포트로", "단계별로")) {
            return "DETAILED";
        }
        if (normalized.length() <= 30 && !containsAny(normalized, "추천", "트렌드", "개발", "재해석")) {
            return "SHORT";
        }
        return "NORMAL";
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).strip();
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private enum PrimaryGenre {
        FPS("FPS/슈팅"),
        HORROR("호러"),
        PARTY("파티/협동"),
        PUZZLE_ADVENTURE("퍼즐/어드벤처"),
        SURVIVAL_ROGUELIKE("생존/로그라이크"),
        RPG("RPG/롤플레잉"),
        MOBA("MOBA"),
        STREAMER_INTERACTION("스트리머 참여형"),
        GENERAL("일반 인터랙션");

        private final String displayName;

        PrimaryGenre(String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            return displayName;
        }
    }

    private enum QuestionIntent {
        GAME_RECOMMENDATION,
        TREND_ANALYSIS,
        GAME_REINTERPRETATION,
        DEVELOPMENT_FEASIBILITY,
        SPECIFIC_GAME_ANALYSIS,
        STREAMING_FIT_ANALYSIS,
        FEATURE_BASED_IDEA,
        GENERAL_GAME_QUESTION
    }

    private enum FollowUpFocus {
        SCOPE_DECISION("프로토타입/MVP 범위 선택", "Scope Decision"),
        PROTOTYPE_SCOPE("프로토타입 범위", "Prototype Slice"),
        RELEASE_MVP_SCOPE("출시 가능한 MVP", "Release MVP"),
        SOLO_DEVELOPMENT("1인 개발", "Solo Scope"),
        STREAMER_TARGET("스트리머 타깃", "Streamer Cut"),
        MOBILE_PLATFORM("모바일 플랫폼", "Mobile Cut"),
        FEATURE_SCOPE("입력 기능 연결", "Feature Loop"),
        MARKET_TREND("시장/트렌드 근거", "Trend Read"),
        PLAYER_RECOMMENDATION("플레이 추천", "Player Pick"),
        CONTEXT_REFINEMENT("이전 맥락 재분석", "Refined"),
        GENERAL("일반 분석", "");

        private final String displayName;
        private final String titleSuffix;

        FollowUpFocus(String displayName, String titleSuffix) {
            this.displayName = displayName;
            this.titleSuffix = titleSuffix;
        }

        String displayName() {
            return displayName;
        }

        String titleSuffix() {
            return titleSuffix;
        }
    }

    private record OnboardingIntent(
            QuestionIntent questionIntent,
            PrimaryGenre primaryGenre,
            List<String> detectedKeywords,
            List<String> preferredFeatures,
            String platform,
            String teamSize,
            String developmentPeriod,
            String scopeLabel,
            boolean fpsIntent,
            boolean horrorIntent,
            boolean partyIntent,
            boolean puzzleAdventureIntent,
            boolean survivalRoguelikeIntent,
            boolean streamerIntent,
            boolean webcamRequested,
            boolean ttsRequested,
            boolean sttRequested,
            boolean recommendationIntent,
            boolean trendIntent,
            boolean developmentIntent,
            boolean specificGameIntent,
            boolean soloTeam,
            boolean smallTeam,
            boolean shortPeriod
    ) {
    }

    private record ConceptSeed(
            String title,
            String genre,
            String reason,
            int streamabilityScore,
            int marketSignalScore,
            int devFeasibilityScore,
            String featureFocus
    ) {
    }

    private record ConversationContext(
            Long historyId,
            String conversationId,
            String message,
            String targetPlatform,
            String teamSize,
            List<String> preferredFeatures,
            String developmentPeriod,
            String summary,
            List<RecommendedConceptResponse> recommendedConcepts,
            String report
    ) {
        static ConversationContext empty() {
            return new ConversationContext(
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    List.of(),
                    null
            );
        }

        boolean hasHistory() {
            return historyId != null;
        }
    }

    private record PersistentConversation(
            Conversation conversation
    ) {
        static PersistentConversation none() {
            return new PersistentConversation(null);
        }

        static PersistentConversation of(Conversation conversation) {
            return new PersistentConversation(conversation);
        }

        boolean hasConversation() {
            return conversation != null && conversation.getId() != null;
        }

        Long conversationId() {
            return hasConversation() ? conversation.getId() : null;
        }

        Long userId() {
            return hasConversation() ? conversation.getUserId() : null;
        }

        String conversationKey() {
            return hasConversation() ? String.valueOf(conversation.getId()) : null;
        }

        String sessionId() {
            return hasConversation() ? conversation.getSessionId() : null;
        }
    }
}
