import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ApiError,
  analyzeOnboarding,
  createGame,
  createConversation,
  createReportDraft,
  deleteConversation,
  deleteOnboardingHistory,
  getConversation,
  getConversations,
  getStoredAccessToken,
  getGames,
  getLiveTrendStatus,
  getOnboardingHistories,
  getOnboardingHistory,
  getRecommendations,
  getSteamReview,
  getTopLiveTrendGames,
  getTopTrendGames,
  importSteamGame,
  refreshLiveTrendData,
  refreshTrendData,
  updateConversation,
} from '../api/gameTrendApi.js';

const initialFormData = {
  steamAppId: '',
  title: '',
  genre: '',
  platform: '',
  playStyle: '',
  streamabilityScore: 50,
  webcamFitScore: 50,
  ttsFitScore: 50,
  sttFitScore: 50,
  noveltyScore: 50,
  devFeasibilityScore: 50,
  marketSignalScore: 50,
  reason: '',
};

const initialOnboardingData = {
  message: '',
  targetPlatform: 'PC',
  teamSize: 'small',
  preferredFeatures: [],
  developmentPeriod: '3 months',
};

const sampleGames = [
  {
    title: 'CamPanic',
    steamAppId: '413150',
    genre: 'Party Horror',
    platform: 'PC',
    playStyle: 'Streamer Co-op',
    streamabilityScore: 94,
    webcamFitScore: 96,
    ttsFitScore: 72,
    sttFitScore: 84,
    noveltyScore: 88,
    devFeasibilityScore: 72,
    marketSignalScore: 91,
    reason: '웹캠 표정 변화와 시청자 미션을 공포 이벤트로 연결하기 좋아 방송 반응을 만들기 쉽습니다.',
  },
  {
    title: 'VoiceQuest',
    steamAppId: '620',
    genre: 'Adventure RPG',
    platform: 'PC',
    playStyle: 'Solo Voice Control',
    streamabilityScore: 82,
    webcamFitScore: 64,
    ttsFitScore: 78,
    sttFitScore: 95,
    noveltyScore: 86,
    devFeasibilityScore: 76,
    marketSignalScore: 80,
    reason: '음성 명령으로 주문, 대화, 퍼즐 풀이를 진행하는 구조라 STT 기반 상호작용을 포트폴리오로 보여주기 좋습니다.',
  },
  {
    title: 'TTS Tavern',
    steamAppId: '1599600',
    genre: 'Simulation',
    platform: 'Web',
    playStyle: 'Chat-driven Management',
    streamabilityScore: 88,
    webcamFitScore: 58,
    ttsFitScore: 97,
    sttFitScore: 70,
    noveltyScore: 82,
    devFeasibilityScore: 84,
    marketSignalScore: 86,
    reason: '채팅 메시지를 손님 대사와 이벤트로 변환해 TTS 반응을 만들 수 있어 시청자 참여형 운영 게임에 적합합니다.',
  },
];

export function useGameTrendOnboarding() {
  const activeAnalysisControllerRef = useRef(null);
  const [currentStep, setCurrentStep] = useState(0);
  const [formData, setFormData] = useState(initialFormData);
  const [onboardingData, setOnboardingData] = useState(initialOnboardingData);
  const [onboardingResult, setOnboardingResult] = useState(null);
  const [activeParentHistoryId, setActiveParentHistoryId] = useState(null);
  const [currentAgentSessionId, setCurrentAgentSessionId] = useState(null);
  const [selectedConversationId, setSelectedConversationId] = useState(null);
  const [selectedConversation, setSelectedConversation] = useState(null);
  const [conversationTurns, setConversationTurns] = useState([]);
  const [conversations, setConversations] = useState([]);
  const [onboardingHistories, setOnboardingHistories] = useState([]);
  const [selectedOnboardingHistory, setSelectedOnboardingHistory] = useState(null);
  const [games, setGames] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [report, setReport] = useState(null);
  const [steamReview, setSteamReview] = useState(null);
  const [trendGames, setTrendGames] = useState([]);
  const [trendRefreshResult, setTrendRefreshResult] = useState(null);
  const [liveTrendGames, setLiveTrendGames] = useState([]);
  const [liveTrendStatus, setLiveTrendStatus] = useState(null);
  const [liveTrendRefreshResult, setLiveTrendRefreshResult] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loginRequiredNotice, setLoginRequiredNotice] = useState(null);
  const [isCreatingGame, setIsCreatingGame] = useState(false);
  const [isImportingSteamGame, setIsImportingSteamGame] = useState(false);
  const [isLoadingSteamReview, setIsLoadingSteamReview] = useState(false);
  const [isLoadingGames, setIsLoadingGames] = useState(false);
  const [isLoadingRecommendations, setIsLoadingRecommendations] = useState(false);
  const [isGeneratingReport, setIsGeneratingReport] = useState(false);
  const [isAnalyzingOnboarding, setIsAnalyzingOnboarding] = useState(false);
  const [isAnalyzingFollowUp, setIsAnalyzingFollowUp] = useState(false);
  const [activeAnalysisMessage, setActiveAnalysisMessage] = useState('');
  const [analyzingFollowUpQuestion, setAnalyzingFollowUpQuestion] = useState('');
  const [isLoadingConversations, setIsLoadingConversations] = useState(false);
  const [isLoadingConversationDetail, setIsLoadingConversationDetail] = useState(false);
  const [isCreatingConversation, setIsCreatingConversation] = useState(false);
  const [isUpdatingConversation, setIsUpdatingConversation] = useState(false);
  const [isDeletingConversation, setIsDeletingConversation] = useState(false);
  const [isLoadingOnboardingHistories, setIsLoadingOnboardingHistories] = useState(false);
  const [isLoadingOnboardingHistoryDetail, setIsLoadingOnboardingHistoryDetail] = useState(false);
  const [isDeletingOnboardingHistory, setIsDeletingOnboardingHistory] = useState(false);
  const [isLoadingTrendGames, setIsLoadingTrendGames] = useState(false);
  const [isRefreshingTrendData, setIsRefreshingTrendData] = useState(false);
  const [isLoadingLiveTrendGames, setIsLoadingLiveTrendGames] = useState(false);
  const [isLoadingLiveTrendStatus, setIsLoadingLiveTrendStatus] = useState(false);
  const [isRefreshingLiveTrendData, setIsRefreshingLiveTrendData] = useState(false);

  const clearError = () => setErrorMessage('');
  const clearLoginRequiredNotice = () => setLoginRequiredNotice(null);
  const clearSuccess = () => setSuccessMessage('');

  const updateField = (field, value) => {
    clearSuccess();
    if (field === 'steamAppId') {
      setSteamReview(null);
    }
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const updateOnboardingField = (field, value) => {
    clearError();
    clearLoginRequiredNotice();
    clearSuccess();
    setOnboardingData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const togglePreferredFeature = (feature) => {
    clearError();
    clearLoginRequiredNotice();
    clearSuccess();
    setOnboardingData((prev) => {
      const selected = prev.preferredFeatures.includes(feature);
      return {
        ...prev,
        preferredFeatures: selected
          ? prev.preferredFeatures.filter((item) => item !== feature)
          : [...prev.preferredFeatures, feature],
      };
    });
  };

  const goNext = () => {
    clearError();
    clearSuccess();
    setCurrentStep((step) => Math.min(step + 1, 4));
  };

  const goBack = () => {
    clearError();
    clearSuccess();
    setCurrentStep((step) => Math.max(step - 1, 0));
  };

  const startManualEntry = () => {
    clearError();
    clearSuccess();
    setCurrentStep(1);
  };

  const loadOnboardingHistories = useCallback(async () => {
    if (!hasAuthToken()) {
      setOnboardingHistories([]);
      setSelectedOnboardingHistory(null);
      return [];
    }

    setIsLoadingOnboardingHistories(true);
    clearError();
    try {
      const data = await getOnboardingHistories();
      setOnboardingHistories(data);
      return data;
    } catch (error) {
      if (isAuthRequiredError(error)) {
        setOnboardingHistories([]);
        setSelectedOnboardingHistory(null);
        return [];
      }
      setErrorMessage(error.message);
      return [];
    } finally {
      setIsLoadingOnboardingHistories(false);
    }
  }, []);

  const selectOnboardingHistory = async (id) => {
    if (!hasAuthToken()) {
      setLoginRequiredNotice({
        title: '로그인하면 대화 기록을 저장할 수 있어요.',
        message: '저장된 분석 이력을 보려면 로그인해주세요.',
      });
      return null;
    }

    setIsLoadingOnboardingHistoryDetail(true);
    clearError();
    clearSuccess();
    try {
      const data = await getOnboardingHistory(id);
      setSelectedOnboardingHistory(data);
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setIsLoadingOnboardingHistoryDetail(false);
    }
  };

  const removeOnboardingHistory = async (id) => {
    if (!hasAuthToken()) {
      return;
    }

    const confirmed = window.confirm('이 분석 이력을 삭제할까요? 삭제 후에는 복구할 수 없습니다.');
    if (!confirmed) {
      return;
    }

    setIsDeletingOnboardingHistory(true);
    clearError();
    clearSuccess();
    try {
      await deleteOnboardingHistory(id);
      if (selectedOnboardingHistory?.id === id) {
        setSelectedOnboardingHistory(null);
      }
      await loadOnboardingHistories();
      setSuccessMessage('분석 이력을 삭제했습니다.');
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setIsDeletingOnboardingHistory(false);
    }
  };

  const loadConversations = useCallback(async () => {
    if (!hasAuthToken()) {
      setConversations([]);
      setSelectedConversationId(null);
      setSelectedConversation(null);
      return [];
    }

    setIsLoadingConversations(true);
    clearError();
    try {
      const data = await getConversations();
      setConversations(data);
      return data;
    } catch (error) {
      if (isAuthRequiredError(error)) {
        setConversations([]);
        return [];
      }
      setErrorMessage(error.message);
      return [];
    } finally {
      setIsLoadingConversations(false);
    }
  }, []);

  const createSavedConversation = async (payload = {}) => {
    if (!hasAuthToken()) {
      setLoginRequiredNotice({
        title: '로그인하면 대화 기록을 저장할 수 있어요.',
        message: '새 대화를 저장하려면 먼저 로그인해주세요.',
      });
      return null;
    }

    setIsCreatingConversation(true);
    clearError();
    clearSuccess();
    try {
      const conversation = await createConversation(payload);
      setConversations((prev) => [
        conversation,
        ...prev.filter((item) => Number(item.id) !== Number(conversation.id)),
      ]);
      setSelectedConversationId(conversation.id);
      setSelectedConversation(conversation);
      setCurrentAgentSessionId(conversation.sessionId ?? null);
      setConversationTurns([]);
      setOnboardingResult(null);
      setActiveParentHistoryId(null);
      setSuccessMessage('새 대화를 만들었습니다.');
      return conversation;
    } catch (error) {
      setErrorMessage(error.message);
      throw error;
    } finally {
      setIsCreatingConversation(false);
    }
  };

  const updateSavedConversation = async (conversationId, payload) => {
    if (!hasAuthToken()) {
      return null;
    }

    setIsUpdatingConversation(true);
    clearError();
    clearSuccess();
    try {
      const conversation = await updateConversation(conversationId, payload);
      setConversations((prev) => prev.map((item) => (
        Number(item.id) === Number(conversation.id) ? conversation : item
      )));
      if (Number(selectedConversationId) === Number(conversation.id)) {
        setSelectedConversation((prev) => ({ ...(prev || {}), ...conversation }));
      }
      setSuccessMessage('대화 제목을 수정했습니다.');
      return conversation;
    } catch (error) {
      setErrorMessage(error.message);
      throw error;
    } finally {
      setIsUpdatingConversation(false);
    }
  };

  const removeSavedConversation = async (conversationId) => {
    if (!hasAuthToken()) {
      return;
    }

    const normalizedConversationId = normalizeConversationId(conversationId);
    if (!normalizedConversationId) {
      return;
    }
    const confirmed = window.confirm('이 대화 기록을 삭제할까요? 삭제 후에는 복구할 수 없습니다.');
    if (!confirmed) {
      return;
    }
    setIsDeletingConversation(true);
    clearError();
    clearSuccess();
    try {
      await deleteConversation(normalizedConversationId);
      setConversations((prev) => prev.filter((item) => Number(item.id) !== Number(normalizedConversationId)));
      if (Number(selectedConversationId) === Number(normalizedConversationId)) {
        startNewAgentQuestion();
      }
      setSuccessMessage('대화 기록을 삭제했습니다.');
    } catch (error) {
      setErrorMessage(error.message);
      throw error;
    } finally {
      setIsDeletingConversation(false);
    }
  };

  const selectConversation = async (conversationId) => {
    if (!hasAuthToken()) {
      setLoginRequiredNotice({
        title: '로그인하면 대화 기록을 저장할 수 있어요.',
        message: '이전 대화를 보려면 로그인해주세요.',
      });
      return null;
    }

    const normalizedConversationId = normalizeConversationId(conversationId);
    if (!normalizedConversationId) {
      startNewAgentQuestion();
      return null;
    }

    setIsLoadingConversationDetail(true);
    clearError();
    clearLoginRequiredNotice();
    clearSuccess();
    try {
      const detail = await getConversation(normalizedConversationId);
      const turns = conversationDetailToTurns(detail);
      const result = conversationDetailToLatestResult(detail, turns);
      setSelectedConversationId(detail.id);
      setSelectedConversation(detail);
      setCurrentAgentSessionId(detail.sessionId ?? null);
      setConversationTurns(turns);
      setOnboardingResult(result);
      setActiveParentHistoryId(null);
      setOnboardingData((prev) => ({ ...prev, message: '' }));
      return detail;
    } catch (error) {
      if (isAuthRequiredError(error)) {
        setLoginRequiredNotice({
          title: '로그인하면 대화 기록을 저장할 수 있어요.',
          message: '이전 대화를 보려면 다시 로그인해주세요.',
        });
        return null;
      }
      setErrorMessage(error.message);
      return null;
    } finally {
      setIsLoadingConversationDetail(false);
    }
  };

  const clearConversationState = () => {
    setConversations([]);
    setSelectedConversationId(null);
    setSelectedConversation(null);
    setConversationTurns([]);
    setOnboardingResult(null);
    setActiveParentHistoryId(null);
    setCurrentAgentSessionId(null);
    setActiveAnalysisMessage('');
  };

  const startSavedConversationForQuestion = async (message) => {
    const conversation = await createConversation({
      title: titleFromQuestion(message),
    });
    const visibleConversation = withVisibleLastMessage(conversation, message);
    setConversations((prev) => [
      visibleConversation,
      ...prev.filter((item) => Number(item.id) !== Number(conversation.id)),
    ]);
    setSelectedConversationId(conversation.id);
    setSelectedConversation(visibleConversation);
    setCurrentAgentSessionId(conversation.sessionId ?? null);
    return visibleConversation;
  };

  const touchConversationWithQuestion = (conversationId, message) => {
    const normalizedConversationId = normalizeConversationId(conversationId);
    if (!normalizedConversationId) {
      return;
    }
    const now = new Date().toISOString();
    setConversations((prev) => {
      const existing = prev.find((item) => Number(item.id) === Number(normalizedConversationId));
      if (!existing) {
        return prev;
      }
      const updated = {
        ...existing,
        lastMessage: message,
        updatedAt: now,
      };
      return [
        updated,
        ...prev.filter((item) => Number(item.id) !== Number(normalizedConversationId)),
      ];
    });
    setSelectedConversation((prev) => (
      prev && Number(prev.id) === Number(normalizedConversationId)
        ? { ...prev, lastMessage: message, updatedAt: now }
        : prev
    ));
  };

  const blockGuestRestrictedQuestion = (message, preferredFeatures = []) => {
    if (hasAuthToken() || !isRestrictedGuestQuestion(message, preferredFeatures)) {
      return false;
    }

    setActiveAnalysisMessage('');
    clearError();
    clearSuccess();
    setLoginRequiredNotice({
      requestedMessage: message,
      title: '로그인이 필요한 분석이에요.',
      message: '개발자 분석, 과거 게임 재해석, 웹캠/TTS/STT 게임 아이디어 분석은 로그인 후 바로 사용할 수 있습니다.',
    });
    return true;
  };

  const analyzeNaturalOnboarding = async () => {
    if (isAnalyzingOnboarding || isAnalyzingFollowUp) {
      return;
    }
    if (!onboardingData.message.trim()) {
      setErrorMessage('분석할 자연어 요청을 입력해주세요.');
      return;
    }

    const requestedMessage = onboardingData.message.trim();
    const parentHistoryId = activeParentHistoryId;
    const sessionId = currentAgentSessionId;
    const conversationId = normalizeConversationId(selectedConversationId);
    const authenticated = hasAuthToken();
    if (blockGuestRestrictedQuestion(requestedMessage, onboardingData.preferredFeatures)) {
      return null;
    }
    const controller = createAnalysisController(activeAnalysisControllerRef);

    setIsAnalyzingOnboarding(true);
    setActiveAnalysisMessage(requestedMessage);
    clearError();
    clearLoginRequiredNotice();
    clearSuccess();
    try {
      let nextConversationId = conversationId;
      let nextSessionId = sessionId;
      if (authenticated && !nextConversationId) {
        const conversation = await startSavedConversationForQuestion(requestedMessage);
        nextConversationId = normalizeConversationId(conversation.id);
        nextSessionId = conversation.sessionId ?? sessionId ?? null;
      } else if (authenticated && nextConversationId) {
        touchConversationWithQuestion(nextConversationId, requestedMessage);
      }

      const data = await analyzeOnboarding(
        toOnboardingPayload(onboardingData, parentHistoryId, nextSessionId, nextConversationId, authenticated),
        { signal: controller.signal }
      );
      if (handleLoginRequiredResponse(data, requestedMessage)) {
        return null;
      }
      nextConversationId = authenticated
        ? normalizeConversationId(data.conversationId) ?? nextConversationId
        : null;
      const result = attachConversationToAnalyzeResult(data, nextConversationId);
      setOnboardingResult(result);
      setActiveParentHistoryId(data.historyId ?? null);
      setCurrentAgentSessionId(data.sessionId ?? sessionId ?? null);
      setSelectedConversationId(nextConversationId);
      setConversationTurns((prev) => [
        ...prev,
        toConversationTurn(requestedMessage, result, parentHistoryId),
      ]);
      await Promise.all([loadOnboardingHistories(), nextConversationId ? loadConversations() : Promise.resolve()]);
      setSuccessMessage(nextConversationId
        ? '분석이 완료되었고 이 대화 기록에 저장되었습니다.'
        : parentHistoryId
        ? '이전 분석 맥락을 이어서 후속 분석이 완료되었습니다.'
        : '자연어 기반 게임 트렌드 분석이 완료되었습니다.');
      return result;
    } catch (error) {
      if (handleAbortError(error)) {
        return null;
      }
      handleAnalyzeError(error, requestedMessage);
    } finally {
      clearAnalysisController(activeAnalysisControllerRef, controller);
      setIsAnalyzingOnboarding(false);
      setActiveAnalysisMessage('');
    }
  };

  const analyzeFollowUpQuestion = async (
    question,
    parentHistoryId = onboardingResult?.historyId
  ) => {
    if (isAnalyzingOnboarding || isAnalyzingFollowUp) {
      return;
    }
    if (!question?.trim()) {
      setErrorMessage('분석할 후속 질문이 비어 있습니다.');
      return;
    }
    const conversationId = normalizeConversationId(selectedConversationId ?? onboardingResult?.conversationId);
    const sessionId = currentAgentSessionId ?? onboardingResult?.sessionId ?? null;
    const authenticated = hasAuthToken();
    if (!parentHistoryId && !conversationId && !sessionId) {
      setErrorMessage('이전 분석 결과가 없어 후속 분석을 시작할 수 없습니다.');
      return;
    }

    const followUpMessage = question.trim();
    const followUpData = {
      ...onboardingData,
      message: followUpMessage,
    };
    if (blockGuestRestrictedQuestion(followUpMessage, followUpData.preferredFeatures)) {
      setOnboardingData(followUpData);
      return null;
    }
    const controller = createAnalysisController(activeAnalysisControllerRef);

    setOnboardingData(followUpData);
    setActiveParentHistoryId(parentHistoryId);
    setIsAnalyzingFollowUp(true);
    setActiveAnalysisMessage(followUpMessage);
    setAnalyzingFollowUpQuestion(followUpMessage);
    clearError();
    clearLoginRequiredNotice();
    clearSuccess();
    try {
      if (authenticated && conversationId) {
        touchConversationWithQuestion(conversationId, followUpMessage);
      }
      const data = await analyzeOnboarding(
        toOnboardingPayload(followUpData, parentHistoryId, sessionId, conversationId, authenticated),
        { signal: controller.signal }
      );
      if (handleLoginRequiredResponse(data, followUpMessage)) {
        return null;
      }
      const nextConversationId = authenticated
        ? normalizeConversationId(data.conversationId) ?? conversationId
        : null;
      const result = attachConversationToAnalyzeResult(data, nextConversationId);
      setOnboardingResult(result);
      setActiveParentHistoryId(data.historyId ?? null);
      setCurrentAgentSessionId(data.sessionId ?? sessionId ?? null);
      setSelectedConversationId(nextConversationId);
      setConversationTurns((prev) => [
        ...prev,
        toConversationTurn(followUpMessage, result, parentHistoryId),
      ]);
      await Promise.all([loadOnboardingHistories(), nextConversationId ? loadConversations() : Promise.resolve()]);
      setSuccessMessage(nextConversationId
        ? '후속 분석이 완료되었고 이 대화 기록에 저장되었습니다.'
        : '후속 질문을 이전 분석 맥락으로 이어서 분석했습니다.');
      return result;
    } catch (error) {
      if (handleAbortError(error)) {
        return null;
      }
      handleAnalyzeError(error, followUpMessage);
    } finally {
      clearAnalysisController(activeAnalysisControllerRef, controller);
      setIsAnalyzingFollowUp(false);
      setActiveAnalysisMessage('');
      setAnalyzingFollowUpQuestion('');
    }
  };

  const analyzePublicExampleQuestion = async (question) => {
    if (isAnalyzingOnboarding || isAnalyzingFollowUp) {
      return;
    }
    if (!question?.trim()) {
      setErrorMessage('분석할 질문이 비어 있습니다.');
      return;
    }

    const requestedMessage = question.trim();
    const sessionId = currentAgentSessionId ?? onboardingResult?.sessionId ?? null;
    const conversationId = normalizeConversationId(selectedConversationId ?? onboardingResult?.conversationId);
    const authenticated = hasAuthToken();
    const publicQuestionData = {
      ...onboardingData,
      message: requestedMessage,
      preferredFeatures: [],
    };
    if (blockGuestRestrictedQuestion(requestedMessage, publicQuestionData.preferredFeatures)) {
      setOnboardingData(publicQuestionData);
      return null;
    }
    const controller = createAnalysisController(activeAnalysisControllerRef);

    setOnboardingData(publicQuestionData);
    setActiveParentHistoryId(null);
    setIsAnalyzingOnboarding(true);
    setActiveAnalysisMessage(requestedMessage);
    clearError();
    clearLoginRequiredNotice();
    clearSuccess();
    try {
      let nextConversationId = conversationId;
      let nextSessionId = sessionId;
      if (authenticated && !nextConversationId) {
        const conversation = await startSavedConversationForQuestion(requestedMessage);
        nextConversationId = normalizeConversationId(conversation.id);
        nextSessionId = conversation.sessionId ?? sessionId ?? null;
      } else if (authenticated && nextConversationId) {
        touchConversationWithQuestion(nextConversationId, requestedMessage);
      }

      const data = await analyzeOnboarding(
        toOnboardingPayload(publicQuestionData, null, nextSessionId, nextConversationId, authenticated),
        { signal: controller.signal }
      );
      if (handleLoginRequiredResponse(data, requestedMessage)) {
        return null;
      }
      nextConversationId = authenticated
        ? normalizeConversationId(data.conversationId) ?? nextConversationId
        : null;
      const result = attachConversationToAnalyzeResult(data, nextConversationId);
      setOnboardingResult(result);
      setActiveParentHistoryId(data.historyId ?? null);
      setCurrentAgentSessionId(data.sessionId ?? sessionId ?? null);
      setSelectedConversationId(nextConversationId);
      setConversationTurns((prev) => [
        ...prev,
        toConversationTurn(requestedMessage, result, null),
      ]);
      await Promise.all([loadOnboardingHistories(), nextConversationId ? loadConversations() : Promise.resolve()]);
      setSuccessMessage(nextConversationId
        ? '질문 분석이 완료되었고 이 대화 기록에 저장되었습니다.'
        : '로그인 없이 가능한 질문으로 분석을 완료했습니다.');
      return result;
    } catch (error) {
      if (handleAbortError(error)) {
        return null;
      }
      handleAnalyzeError(error, requestedMessage);
    } finally {
      clearAnalysisController(activeAnalysisControllerRef, controller);
      setIsAnalyzingOnboarding(false);
      setActiveAnalysisMessage('');
    }
  };

  const analyzeNewQuestion = async (question) => {
    if (isAnalyzingOnboarding || isAnalyzingFollowUp) {
      return;
    }
    if (!question?.trim()) {
      setErrorMessage('분석할 질문을 입력해주세요.');
      return;
    }

    const requestedMessage = question.trim();
    const questionData = {
      ...initialOnboardingData,
      message: requestedMessage,
    };
    const authenticated = hasAuthToken();
    if (blockGuestRestrictedQuestion(requestedMessage, questionData.preferredFeatures)) {
      setOnboardingData(questionData);
      setOnboardingResult(null);
      setActiveParentHistoryId(null);
      setCurrentAgentSessionId(null);
      return null;
    }
    const controller = createAnalysisController(activeAnalysisControllerRef);

    setOnboardingData(questionData);
    setOnboardingResult(null);
    setActiveParentHistoryId(null);
    setCurrentAgentSessionId(null);
    setSelectedConversationId(null);
    setSelectedConversation(null);
    setConversationTurns([]);
    setIsAnalyzingOnboarding(true);
    setActiveAnalysisMessage(requestedMessage);
    clearError();
    clearLoginRequiredNotice();
    clearSuccess();
    try {
      let nextConversationId = null;
      let nextSessionId = null;
      if (authenticated) {
        const conversation = await startSavedConversationForQuestion(requestedMessage);
        nextConversationId = normalizeConversationId(conversation.id);
        nextSessionId = conversation.sessionId ?? null;
      }

      const data = await analyzeOnboarding(
        toOnboardingPayload(questionData, null, nextSessionId, nextConversationId, authenticated),
        { signal: controller.signal }
      );
      if (handleLoginRequiredResponse(data, requestedMessage)) {
        return null;
      }
      nextConversationId = authenticated ? normalizeConversationId(data.conversationId) ?? nextConversationId : null;
      const result = attachConversationToAnalyzeResult(data, nextConversationId);
      setOnboardingResult(result);
      setActiveParentHistoryId(data.historyId ?? null);
      setCurrentAgentSessionId(data.sessionId ?? null);
      setSelectedConversationId(nextConversationId);
      setConversationTurns([
        toConversationTurn(requestedMessage, result, null),
      ]);
      await Promise.all([loadOnboardingHistories(), nextConversationId ? loadConversations() : Promise.resolve()]);
      setSuccessMessage(nextConversationId
        ? '새 대화를 만들고 분석 결과를 저장했습니다.'
        : '질문 분석이 완료되었습니다.');
      return result;
    } catch (error) {
      if (handleAbortError(error)) {
        return null;
      }
      handleAnalyzeError(error, requestedMessage);
    } finally {
      clearAnalysisController(activeAnalysisControllerRef, controller);
      setIsAnalyzingOnboarding(false);
      setActiveAnalysisMessage('');
    }
  };

  const abortActiveAnalysis = () => {
    if (activeAnalysisControllerRef.current) {
      activeAnalysisControllerRef.current.abort();
    }
  };

  const handleAnalyzeError = (error, requestedMessage) => {
    if (isAuthRequiredError(error)) {
      setLoginRequiredNotice({
        requestedMessage,
        title: '이 기능은 로그인 후 이용할 수 있어요.',
        message: '개발자 분석, 과거 게임 재해석, 웹캠/TTS/STT 아이디어 분석은 로그인 후 사용할 수 있습니다.',
      });
      return;
    }
    setErrorMessage(error.message);
  };

  const handleLoginRequiredResponse = (data, requestedMessage) => {
    if (!data?.requiresLogin && data?.code !== 'AUTH_REQUIRED') {
      return false;
    }

    setLoginRequiredNotice({
      requestedMessage,
      title: data.title || '이 기능은 로그인 후 이용할 수 있어요.',
      message: data.message || '개발자 분석, 과거 게임 재해석, 웹캠/TTS/STT 아이디어 분석은 로그인 후 사용할 수 있습니다.',
    });
    return true;
  };

  const prepareFollowUpQuestion = (question, parentHistoryId = onboardingResult?.historyId) => {
    clearError();
    clearLoginRequiredNotice();
    clearSuccess();
    setActiveParentHistoryId(parentHistoryId ?? null);
    setOnboardingData((prev) => ({
      ...prev,
      message: question,
    }));
    setSuccessMessage('후속 질문을 입력창에 넣었습니다. 필요하면 문장을 수정한 뒤 분석 시작을 눌러주세요.');
  };

  const startNewAgentQuestion = () => {
    setOnboardingData(initialOnboardingData);
    setOnboardingResult(null);
    setActiveParentHistoryId(null);
    setCurrentAgentSessionId(null);
    setSelectedConversationId(null);
    setSelectedConversation(null);
    setConversationTurns([]);
    clearError();
    clearLoginRequiredNotice();
    setSuccessMessage('새 질문을 시작합니다.');
  };

  const loadGames = useCallback(async () => {
    setIsLoadingGames(true);
    clearError();
    try {
      const data = await getGames();
      setGames(data);
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setIsLoadingGames(false);
    }
  }, []);

  const loadRecommendations = useCallback(async () => {
    setIsLoadingRecommendations(true);
    clearError();
    try {
      const data = await getRecommendations();
      setRecommendations(data);
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setIsLoadingRecommendations(false);
    }
  }, []);

  const loadTrendGames = useCallback(async (limit = 8) => {
    setIsLoadingTrendGames(true);
    clearError();
    try {
      const data = await getTopTrendGames(limit);
      setTrendGames(data);
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setIsLoadingTrendGames(false);
    }
  }, []);

  const refreshTrendSignals = useCallback(async () => {
    setIsRefreshingTrendData(true);
    clearError();
    clearSuccess();
    try {
      const result = await refreshTrendData();
      setTrendRefreshResult(result);
      await loadTrendGames();
      setSuccessMessage(`트렌드 데이터 ${result.refreshedCount}개를 갱신했습니다.`);
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setIsRefreshingTrendData(false);
    }
  }, [loadTrendGames]);

  const loadLiveTrendStatus = useCallback(async () => {
    setIsLoadingLiveTrendStatus(true);
    clearError();
    try {
      const data = await getLiveTrendStatus();
      setLiveTrendStatus(data);
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setIsLoadingLiveTrendStatus(false);
    }
  }, []);

  const loadLiveTrendGames = useCallback(async (limit = 8, platform = 'all') => {
    setIsLoadingLiveTrendGames(true);
    clearError();
    try {
      const data = await getTopLiveTrendGames(limit, platform);
      setLiveTrendGames(data);
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setIsLoadingLiveTrendGames(false);
    }
  }, []);

  const refreshLiveTrendSignals = useCallback(async (platform = 'all') => {
    if (!hasAuthToken()) {
      clearError();
      clearSuccess();
      setLiveTrendRefreshResult(null);
      setLoginRequiredNotice({
        title: '로그인이 필요한 기능입니다',
        message: '라이브 트렌드 수동 갱신은 외부 데이터를 새로 수집하는 관리 기능이라 로그인 후 사용할 수 있어요. 현재 저장된 라이브 트렌드 목록과 순위 조회는 계속 볼 수 있습니다.',
        requestedMessage: '라이브 트렌드 수동 갱신',
      });
      setErrorMessage('라이브 트렌드 수동 갱신은 로그인 후 사용할 수 있습니다.');
      return null;
    }
    setIsRefreshingLiveTrendData(true);
    clearError();
    clearSuccess();
    clearLoginRequiredNotice();
    try {
      const result = await refreshLiveTrendData();
      setLiveTrendRefreshResult(result);
      await Promise.all([loadLiveTrendStatus(), loadLiveTrendGames(8, platform)]);
      setSuccessMessage(`라이브 트렌드 데이터 ${result.refreshedCount}개를 갱신했습니다.`);
      return result;
    } catch (error) {
      if (isAuthRequiredError(error)) {
        setLoginRequiredNotice({
          title: '로그인이 필요한 기능입니다',
          message: '라이브 트렌드 수동 갱신은 로그인 후 사용할 수 있어요. 현재 저장된 목록과 상태 조회는 계속 가능합니다.',
          requestedMessage: '라이브 트렌드 수동 갱신',
        });
        setErrorMessage('라이브 트렌드 수동 갱신은 로그인 후 사용할 수 있습니다.');
      } else {
        setErrorMessage(error.message);
      }
      await loadLiveTrendStatus();
      return null;
    } finally {
      setIsRefreshingLiveTrendData(false);
    }
  }, [loadLiveTrendGames, loadLiveTrendStatus]);

  const submitGame = async () => {
    setIsCreatingGame(true);
    clearError();
    clearSuccess();
    try {
      await createGame(toGamePayload(formData));
      await Promise.all([loadGames(), loadRecommendations()]);
      setSuccessMessage(`${formData.title} 등록이 완료되었습니다. 추천 순위와 리포트를 확인해보세요.`);
      setCurrentStep(4);
      return true;
    } catch (error) {
      setErrorMessage(error.message);
      return false;
    } finally {
      setIsCreatingGame(false);
    }
  };

  const loadSteamReview = async () => {
    const appId = Number(formData.steamAppId);
    if (!Number.isInteger(appId) || appId <= 0) {
      setErrorMessage('Steam App ID를 올바른 숫자로 입력해주세요.');
      return;
    }

    setIsLoadingSteamReview(true);
    clearError();
    clearSuccess();
    try {
      const data = await getSteamReview(appId);
      setSteamReview(data);
      setSuccessMessage(`Steam 리뷰 요약을 불러왔습니다. marketSignalScore 예상값은 ${data.marketSignalScore}점입니다.`);
    } catch (error) {
      setErrorMessage(error.message);
      setSteamReview(null);
    } finally {
      setIsLoadingSteamReview(false);
    }
  };

  const importFromSteam = async () => {
    const appId = Number(formData.steamAppId);
    if (!Number.isInteger(appId) || appId <= 0) {
      setErrorMessage('Steam App ID를 올바른 숫자로 입력해주세요.');
      return;
    }

    setIsImportingSteamGame(true);
    clearError();
    clearSuccess();
    try {
      const response = await importSteamGame({
        appId,
        ...toSteamImportPayload(formData),
      });
      setSteamReview(response.steamReview);
      await Promise.all([loadGames(), loadRecommendations()]);
      setSuccessMessage(`${response.game.title}을 Steam 리뷰 데이터 기반으로 등록했습니다.`);
      setCurrentStep(4);
      return true;
    } catch (error) {
      setErrorMessage(error.message);
      return false;
    } finally {
      setIsImportingSteamGame(false);
    }
  };

  const createReport = async () => {
    setIsGeneratingReport(true);
    clearError();
    clearSuccess();
    try {
      const data = await createReportDraft(5);
      setReport(data);
      setSuccessMessage('GEMMA4 E2B 리포트 초안이 생성되었습니다.');
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setIsGeneratingReport(false);
    }
  };

  const resetOnboarding = () => {
    setFormData(initialFormData);
    setReport(null);
    setSteamReview(null);
    clearError();
    clearSuccess();
    setCurrentStep(0);
  };

  const resetNaturalOnboarding = () => {
    setOnboardingData(initialOnboardingData);
    setOnboardingResult(null);
    setActiveParentHistoryId(null);
    setCurrentAgentSessionId(null);
    setSelectedConversationId(null);
    setSelectedConversation(null);
    setConversationTurns([]);
    clearError();
    clearLoginRequiredNotice();
    setSuccessMessage('자연어 온보딩 입력을 초기화했습니다.');
  };

  const resetForm = () => {
    setFormData(initialFormData);
    setSteamReview(null);
    clearError();
    setSuccessMessage('입력 폼을 초기화했습니다.');
  };

  const applySampleGame = (sampleGame) => {
    setFormData(sampleGame);
    clearError();
    setSuccessMessage(`${sampleGame.title} 샘플 데이터를 입력했습니다.`);
  };

  const refreshResults = async () => {
    clearError();
    clearSuccess();
    await Promise.all([loadGames(), loadRecommendations()]);
    setSuccessMessage('게임 목록과 추천 순위를 새로고침했습니다.');
  };

  useEffect(() => {
    loadOnboardingHistories();
  }, [loadOnboardingHistories]);

  return {
    currentStep,
    formData,
    onboardingData,
    onboardingResult,
    activeParentHistoryId,
    currentAgentSessionId,
    selectedConversationId,
    selectedConversation,
    conversationTurns,
    conversations,
    onboardingHistories,
    selectedOnboardingHistory,
    sampleGames,
    games,
    recommendations,
    report,
    steamReview,
    trendGames,
    trendRefreshResult,
    liveTrendGames,
    liveTrendStatus,
    liveTrendRefreshResult,
    errorMessage,
    successMessage,
    loginRequiredNotice,
    isCreatingGame,
    isImportingSteamGame,
    isLoadingSteamReview,
    isLoadingGames,
    isLoadingRecommendations,
    isGeneratingReport,
    isAnalyzingOnboarding,
    isAnalyzingFollowUp,
    activeAnalysisMessage,
    analyzingFollowUpQuestion,
    isLoadingConversations,
    isLoadingConversationDetail,
    isCreatingConversation,
    isUpdatingConversation,
    isDeletingConversation,
    isLoadingOnboardingHistories,
    isLoadingOnboardingHistoryDetail,
    isDeletingOnboardingHistory,
    isLoadingTrendGames,
    isRefreshingTrendData,
    isLoadingLiveTrendGames,
    isLoadingLiveTrendStatus,
    isRefreshingLiveTrendData,
    updateField,
    updateOnboardingField,
    togglePreferredFeature,
    goNext,
    goBack,
    startManualEntry,
    analyzeNaturalOnboarding,
    analyzeFollowUpQuestion,
    analyzePublicExampleQuestion,
    analyzeNewQuestion,
    abortActiveAnalysis,
    loadConversations,
    createSavedConversation,
    updateSavedConversation,
    removeSavedConversation,
    selectConversation,
    clearConversationState,
    clearProjectState: clearConversationState,
    prepareFollowUpQuestion,
    startNewAgentQuestion,
    loadOnboardingHistories,
    selectOnboardingHistory,
    removeOnboardingHistory,
    submitGame,
    loadGames,
    loadRecommendations,
    loadTrendGames,
    refreshTrendSignals,
    loadLiveTrendStatus,
    loadLiveTrendGames,
    refreshLiveTrendSignals,
    loadSteamReview,
    importFromSteam,
    createReport,
    resetOnboarding,
    resetNaturalOnboarding,
    resetForm,
    applySampleGame,
    refreshResults,
    clearError,
    clearLoginRequiredNotice,
    clearSuccess,
  };
}

function isAuthRequiredError(error) {
  return error instanceof ApiError
    && (error.status === 401 || error.code === 'AUTH_REQUIRED');
}

function isRestrictedGuestQuestion(message, preferredFeatures = []) {
  const normalizedMessage = normalizeText(message);
  const normalizedFeatures = Array.isArray(preferredFeatures)
    ? preferredFeatures.map(normalizeText)
    : [];
  const hasRestrictedFeature = normalizedFeatures.some((feature) => (
    ['webcam', 'tts', 'stt', '웹캠', '카메라', '음성'].some((keyword) => feature.includes(keyword))
  ));
  if (hasRestrictedFeature) {
    return true;
  }

  return containsAny(normalizedMessage, [
    '개발',
    '개발자',
    '만들고 싶은',
    '만들고 싶',
    '만들면',
    '만들 수',
    '만들만',
    '만들 만',
    '기획',
    '시장성',
    '상업성',
    '수익성',
    '프로토타입',
    'mvp',
    '출시',
    '구현',
    '개발 가능',
    'feasibility',
    'prototype',
    'release',
    '과거 게임',
    '예전 게임',
    '옛날 게임',
    '재해석',
    '다시 만들',
    '리메이크',
    '레트로',
    '현대화',
    '웹캠',
    'webcam',
    'tts',
    'stt',
    '음성 인식',
    '마이크',
    '시청자 참여형',
    '채팅으로 조작',
    '채팅 참여',
    '카메라',
  ]);
}

function normalizeText(value) {
  return String(value || '').trim().toLowerCase();
}

function containsAny(value, keywords) {
  return keywords.some((keyword) => value.includes(keyword));
}

function toOnboardingPayload(onboardingData, parentHistoryId, sessionId, conversationId, authenticated = false) {
  const payload = {
    message: onboardingData.message,
    targetPlatform: onboardingData.targetPlatform,
    teamSize: onboardingData.teamSize,
    preferredFeatures: onboardingData.preferredFeatures,
    developmentPeriod: onboardingData.developmentPeriod,
  };

  if (authenticated && parentHistoryId !== null && parentHistoryId !== undefined) {
    payload.parentHistoryId = parentHistoryId;
  }

  if (!authenticated && sessionId !== null && sessionId !== undefined && String(sessionId).trim()) {
    payload.sessionId = String(sessionId).trim();
  }

  if (authenticated && conversationId !== null && conversationId !== undefined && String(conversationId).trim()) {
    payload.conversationId = String(conversationId).trim();
  }

  return payload;
}

function hasAuthToken() {
  return Boolean(getStoredAccessToken());
}

function createAnalysisController(controllerRef) {
  controllerRef.current?.abort();
  const controller = new AbortController();
  controllerRef.current = controller;
  return controller;
}

function clearAnalysisController(controllerRef, controller) {
  if (controllerRef.current === controller) {
    controllerRef.current = null;
  }
}

function handleAbortError(error) {
  if (error?.name !== 'AbortError') {
    return false;
  }
  return true;
}

function normalizeConversationId(conversationId) {
  if (conversationId === null || conversationId === undefined || String(conversationId).trim() === '') {
    return null;
  }
  const normalized = Number(conversationId);
  return Number.isNaN(normalized) ? null : normalized;
}

function attachConversationToAnalyzeResult(data, conversationId) {
  if (!conversationId) {
    return data;
  }
  return {
    ...data,
    conversationId: String(conversationId),
    savedToConversation: true,
  };
}

function titleFromQuestion(message) {
  const title = String(message || '').trim().replace(/\s+/g, ' ');
  if (!title) {
    return '새 Agent 대화';
  }
  return title.length > 36 ? `${title.slice(0, 36)}...` : title;
}

function withVisibleLastMessage(conversation, message) {
  const now = new Date().toISOString();
  return {
    ...conversation,
    lastMessage: message,
    updatedAt: conversation.updatedAt || now,
  };
}

function toConversationTurn(message, response, parentHistoryId) {
  return {
    id: response.historyId || `${Date.now()}-${message}`,
    parentHistoryId: response.parentHistoryId || parentHistoryId || null,
    historyId: response.historyId || null,
    question: message,
    summary: response.summary || '',
    answer: response.answer || response.summary || '',
    intent: response.intent || '',
    conversationId: response.conversationId || null,
  };
}

function conversationDetailToTurns(detail) {
  const messages = Array.isArray(detail?.messages) ? detail.messages : [];
  const turns = [];
  let pendingUser = null;

  messages.forEach((message) => {
    if (message.role === 'USER') {
      if (pendingUser) {
        turns.push(messagePairToTurn(pendingUser, null));
      }
      pendingUser = message;
      return;
    }

    if (message.role === 'ASSISTANT') {
      turns.push(messagePairToTurn(pendingUser, message));
      pendingUser = null;
    }
  });

  if (pendingUser) {
    turns.push(messagePairToTurn(pendingUser, null));
  }

  return turns;
}

function messagePairToTurn(userMessage, assistantMessage) {
  const id = assistantMessage?.id || userMessage?.id || `${Date.now()}-${Math.random()}`;
  const evidenceCards = parseEvidenceCards(assistantMessage?.evidenceJson);
  return {
    id,
    parentHistoryId: null,
    historyId: null,
    question: userMessage?.content || '',
    summary: assistantMessage?.content || '',
    answer: assistantMessage?.content || '',
    intent: assistantMessage?.intent || userMessage?.intent || '',
    conversationId: assistantMessage?.conversationId || userMessage?.conversationId || null,
    evidenceCards,
  };
}

function conversationDetailToLatestResult(detail, turns) {
  const lastTurn = [...turns].reverse().find((turn) => turn.answer || turn.summary);
  if (!lastTurn) {
    return null;
  }

  return {
    historyId: null,
    parentHistoryId: null,
    conversationId: String(detail.id),
    sessionId: detail.sessionId,
    summary: lastTurn.summary,
    answer: lastTurn.answer,
    report: lastTurn.answer,
    intent: lastTurn.intent || detail.lastIntent || '',
    recommendedConcepts: [],
    followUpQuestions: [],
    evidenceCards: lastTurn.evidenceCards || [],
    savedToConversation: true,
  };
}

function parseEvidenceCards(rawJson) {
  if (!rawJson || typeof rawJson !== 'string') {
    return [];
  }
  try {
    const parsed = JSON.parse(rawJson);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function toGamePayload(formData) {
  return {
    title: formData.title,
    genre: formData.genre,
    platform: formData.platform,
    playStyle: formData.playStyle,
    streamabilityScore: formData.streamabilityScore,
    webcamFitScore: formData.webcamFitScore,
    ttsFitScore: formData.ttsFitScore,
    sttFitScore: formData.sttFitScore,
    noveltyScore: formData.noveltyScore,
    devFeasibilityScore: formData.devFeasibilityScore,
    marketSignalScore: formData.marketSignalScore,
    reason: formData.reason,
  };
}

function toSteamImportPayload(formData) {
  return {
    title: formData.title,
    genre: formData.genre,
    platform: formData.platform,
    playStyle: formData.playStyle,
    streamabilityScore: formData.streamabilityScore,
    webcamFitScore: formData.webcamFitScore,
    ttsFitScore: formData.ttsFitScore,
    sttFitScore: formData.sttFitScore,
    noveltyScore: formData.noveltyScore,
    devFeasibilityScore: formData.devFeasibilityScore,
    reason: formData.reason,
  };
}
