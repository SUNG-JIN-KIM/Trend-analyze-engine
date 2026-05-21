import { useState } from 'react';
import Button from '../common/Button.jsx';
import Card from '../common/Card.jsx';
import LoadingIndicator from '../common/LoadingIndicator.jsx';
import { formatScore, getScoreTone } from '../../utils/score.js';

const featureOptions = [
  { value: 'webcam', label: 'Webcam' },
  { value: 'tts', label: 'TTS' },
  { value: 'stt', label: 'STT' },
];

const exampleQuestions = [
  '요즘 어떤 게임이 인기 있어?',
  '치지직 기준으로 뜨는 게임 알려줘',
  '시청자 수 기준으로 인기 게임 보여줘',
  '스트리머들이 많이 하는 게임 알려줘',
  '나한테 맞는 게임 추천해줘',
  '요즘 어떤 게임을 개발하면 좋을까?',
  '웹캠/TTS/STT로 만들만한 게임 아이디어 알려줘',
];

const publicExampleQuestions = [
  '요즘 할만한 게임 추천해줘',
  '요즘 인기 있는 게임 알려줘',
  '방송에서 뜨는 게임 알려줘',
  '친구랑 할만한 게임 추천해줘',
];

const placeholderGameImageUrl = 'https://placehold.co/640x360/111827/f9738a?text=Game+Trend';

const progressMessages = [
  '요청 내용을 확인하고 있어요',
  '게임 트렌드 신호를 분석하고 있어요',
  '추천 방향을 정리하고 있어요',
];

function NaturalOnboardingScreen({
  onboardingData,
  onboardingResult,
  loginRequiredNotice,
  authUser,
  activeParentHistoryId,
  selectedConversation,
  conversationTurns = [],
  trendGames = [],
  onboardingHistories,
  selectedOnboardingHistory,
  onChange,
  onToggleFeature,
  onAnalyze,
  onAnalyzePublicQuestion,
  onReset,
  onAnalyzeFollowUp,
  onPrepareFollowUp,
  onStartNewConversation,
  onGoTrends,
  onGoLogin,
  onGoRegister,
  onRefreshHistories,
  onSelectHistory,
  onDeleteHistory,
  isAnalyzing,
  isAnalyzingFollowUp = false,
  analyzingFollowUpQuestion = '',
  isLoadingTrendGames = false,
  isLoadingHistories,
  isLoadingHistoryDetail,
  isDeletingHistory,
  showHistory = false,
}) {
  const canAnalyze = Boolean(onboardingData.message.trim()) && !isAnalyzing;
  const applyQuestion = (question) => onChange('message', question);
  const handleFollowUpAnalyze = (question) => {
    if (onAnalyzeFollowUp) {
      onAnalyzeFollowUp(question, onboardingResult?.historyId);
      return;
    }
    if (onPrepareFollowUp) {
      onPrepareFollowUp(question, onboardingResult?.historyId);
      return;
    }
    onChange('message', question);
  };
  const handleFollowUpPrepare = (question) => {
    if (onPrepareFollowUp) {
      onPrepareFollowUp(question, onboardingResult?.historyId);
      return;
    }
    onChange('message', question);
  };

  return (
    <div className="natural-onboarding">
      <Card className="agent-input-card">
        <div className="agent-heading">
          <div>
            <p className="section-kicker">Ask The Market</p>
            <h2>게임 트렌드를 무엇이든 물어보세요</h2>
            <p className="section-description">
              실시간 순위, 방송 반응, 플레이어 추천, 개발 가능성을 한 대화 안에서 이어서 분석합니다.
            </p>
          </div>
          {isAnalyzing && <LoadingIndicator label="GEMMA4 E2B 분석 중" />}
        </div>

        <label className="agent-message-field">
          <span>질문</span>
          <textarea
            value={onboardingData.message}
            onChange={(event) => onChange('message', event.target.value)}
            placeholder={'예: 요즘 어떤 게임이 인기 있어?\n예: 치지직 기준으로 방송 수 많은 게임 알려줘\n예: 웹캠/TTS/STT로 만들만한 게임 아이디어 알려줘'}
            rows="7"
          />
        </label>

        <div className="example-question-list" aria-label="예시 질문">
          {exampleQuestions.map((question) => (
            <button
              className="question-chip"
              type="button"
              key={question}
              onClick={() => applyQuestion(question)}
            >
              {question}
            </button>
          ))}
        </div>

        <div className="trend-connection-note">
          <strong>실시간 데이터 연결됨</strong>
          <span>
            질문이 트렌드나 추천과 관련되면 Twitch, CHZZK, SOOP 라이브 순위를 우선 근거로 사용합니다.
          </span>
        </div>

        {authUser && (
          <section className="conversation-context-note" aria-label="현재 대화 저장 상태">
            <div>
              <span>대화 저장</span>
              <strong>{selectedConversation?.title || '새 대화로 시작'}</strong>
            </div>
            <p>
              로그인 상태에서는 질문을 보내면 자동으로 대화 기록에 저장됩니다.
            </p>
          </section>
        )}

        <AgentTrendSignalPreview
          trendGames={trendGames.slice(0, 3)}
          isLoading={isLoadingTrendGames}
          onGoTrends={onGoTrends}
        />

        <details className="advanced-condition-panel">
          <summary>고급 조건</summary>
          <div className="onboarding-form-grid">
            <label>
              <span>플랫폼</span>
              <select
                value={onboardingData.targetPlatform}
                onChange={(event) => onChange('targetPlatform', event.target.value)}
              >
                <option value="PC">PC</option>
                <option value="Web">Web</option>
                <option value="Mobile">Mobile</option>
                <option value="Console">Console</option>
              </select>
            </label>
            <label>
              <span>팀 규모</span>
              <select
                value={onboardingData.teamSize}
                onChange={(event) => onChange('teamSize', event.target.value)}
              >
                <option value="solo">1인</option>
                <option value="small">소규모</option>
                <option value="medium">중간 규모</option>
              </select>
            </label>
            <label>
              <span>개발 기간</span>
              <select
                value={onboardingData.developmentPeriod}
                onChange={(event) => onChange('developmentPeriod', event.target.value)}
              >
                <option value="1 month">1개월</option>
                <option value="3 months">3개월</option>
                <option value="6 months">6개월</option>
                <option value="12 months">12개월</option>
              </select>
            </label>
            <fieldset className="feature-fieldset">
              <legend>사용하고 싶은 기능</legend>
              <div className="feature-toggle-group">
                {featureOptions.map((feature) => {
                  const selected = onboardingData.preferredFeatures.includes(feature.value);
                  return (
                    <button
                      className={`feature-toggle ${selected ? 'selected' : ''}`}
                      type="button"
                      key={feature.value}
                      aria-pressed={selected}
                      onClick={() => onToggleFeature(feature.value)}
                    >
                      {feature.label}
                    </button>
                  );
                })}
              </div>
            </fieldset>
          </div>
        </details>

        <div className="agent-actions">
          {activeParentHistoryId && (
            <span className="active-context-note">
              #{activeParentHistoryId} 분석 맥락으로 이어서 분석합니다.
            </span>
          )}
          <Button variant="secondary" onClick={onReset} disabled={isAnalyzing}>초기화</Button>
          <Button variant="secondary" onClick={onStartNewConversation || onReset} disabled={isAnalyzing}>
            새 질문 시작
          </Button>
          <Button onClick={onAnalyze} disabled={!canAnalyze}>
            {isAnalyzing ? '분석 중' : '분석 시작'}
          </Button>
        </div>
      </Card>

      {conversationTurns.length > 0 && (
        <ConversationThread
          turns={conversationTurns}
          onStartNewConversation={onStartNewConversation || onReset}
        />
      )}

      {isAnalyzing && (
        <Card className="analysis-loading-card">
          <div className="analysis-progress-list">
            {progressMessages.map((message) => (
              <div key={message}>
                <span className="loading-dot" />
                <strong>{message}</strong>
              </div>
            ))}
          </div>
        </Card>
      )}

      {loginRequiredNotice && (
        <LoginRequiredCard
          notice={loginRequiredNotice}
          onGoLogin={onGoLogin}
          onGoRegister={onGoRegister}
          onAnalyzePublicQuestion={onAnalyzePublicQuestion}
          isAnalyzing={isAnalyzing}
        />
      )}

      {onboardingResult && (
        <NaturalOnboardingResult
          result={onboardingResult}
          onUseQuestion={handleFollowUpAnalyze}
          onPrepareQuestion={handleFollowUpPrepare}
          onGoTrends={onGoTrends}
          isAnalyzingFollowUp={isAnalyzingFollowUp}
          analyzingFollowUpQuestion={analyzingFollowUpQuestion}
        />
      )}

      {showHistory && (
        <OnboardingHistorySection
          histories={onboardingHistories}
          selectedHistory={selectedOnboardingHistory}
          onRefresh={onRefreshHistories}
          onSelect={onSelectHistory}
          onDelete={onDeleteHistory}
          isLoadingHistories={isLoadingHistories}
          isLoadingHistoryDetail={isLoadingHistoryDetail}
          isDeletingHistory={isDeletingHistory}
        />
      )}
    </div>
  );
}

function ConversationThread({ turns, onStartNewConversation }) {
  return (
    <Card className="conversation-thread-card">
      <div className="card-heading">
        <div>
          <p className="section-kicker">Conversation</p>
          <h3>이번 세션 대화 흐름</h3>
        </div>
        <Button variant="secondary" onClick={onStartNewConversation}>새 질문 시작</Button>
      </div>
      <div className="conversation-thread">
        {turns.map((turn, index) => (
          <article className="conversation-turn" key={turn.id}>
            <div className="conversation-bubble user">
              <span>{index === 0 ? '사용자 질문' : '사용자 후속 질문'}</span>
              <p>{turn.question}</p>
            </div>
            <div className="conversation-bubble agent">
              <div className="conversation-bubble-top">
                <span>Agent 요약 답변</span>
                {turn.intent && <b>{formatIntent(turn.intent)}</b>}
              </div>
              <p>{turn.answer || turn.summary}</p>
              <div className="conversation-meta">
                {turn.historyId && <strong>history #{turn.historyId}</strong>}
                {turn.parentHistoryId && <strong>parent #{turn.parentHistoryId}</strong>}
              </div>
            </div>
          </article>
        ))}
      </div>
    </Card>
  );
}

function AgentTrendSignalPreview({ trendGames, isLoading, onGoTrends }) {
  return (
    <section className="agent-trend-preview" aria-labelledby="agent-trend-title">
      <div className="agent-trend-preview-top">
        <div>
          <p className="section-kicker">Current Signals</p>
          <h3 id="agent-trend-title">실시간 인기 게임 미리보기</h3>
        </div>
        <Button variant="secondary" onClick={onGoTrends}>전체 순위 보기</Button>
      </div>

      {isLoading && <LoadingIndicator label="상위 트렌드 데이터를 불러오는 중" />}

      {!isLoading && trendGames.length === 0 && (
        <p className="agent-trend-empty">실시간 순위를 불러오면 Agent 답변의 근거가 더 선명해집니다.</p>
      )}

      {!isLoading && trendGames.length > 0 && (
        <div className="agent-trend-list">
          {trendGames.map((game) => (
            <article className="agent-trend-item" key={game.id || game.title}>
              <div>
                <strong>{game.title}</strong>
                <span>{game.genre || '장르 미정'}</span>
              </div>
              <b className={`score-badge ${getScoreTone(game.trendScore)}`}>
                {formatScore(game.trendScore)}
              </b>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function LoginRequiredCard({
  notice,
  onGoLogin,
  onGoRegister,
  onAnalyzePublicQuestion,
  isAnalyzing = false,
}) {
  const [showPublicQuestions, setShowPublicQuestions] = useState(false);

  return (
    <Card className="login-required-card">
      <div className="login-required-content">
        <div>
          <p className="section-kicker">Login Required</p>
          <h3>{notice?.title || '이 기능은 로그인 후 이용할 수 있어요.'}</h3>
          <p>
            {notice?.message
              || '개발자 분석, 과거 게임 재해석, 웹캠/TTS/STT 아이디어 분석은 로그인 후 사용할 수 있습니다.'}
          </p>
          {notice?.requestedMessage && (
            <div className="login-required-request">
              <span>요청한 질문</span>
              <strong>{notice.requestedMessage}</strong>
            </div>
          )}
        </div>

        <div className="login-required-actions">
          <Button onClick={onGoLogin}>로그인하기</Button>
          <Button variant="secondary" onClick={onGoRegister}>회원가입하기</Button>
          <Button
            variant="secondary"
            onClick={() => setShowPublicQuestions((visible) => !visible)}
          >
            로그인 없이 가능한 질문 보기
          </Button>
        </div>
      </div>

      {showPublicQuestions && (
        <div className="public-question-panel">
          <span>바로 실행할 수 있는 질문</span>
          <div className="example-question-list">
            {publicExampleQuestions.map((question) => (
              <button
                className="question-chip"
                type="button"
                key={question}
                disabled={isAnalyzing || !onAnalyzePublicQuestion}
                onClick={() => onAnalyzePublicQuestion?.(question)}
              >
                {question}
              </button>
            ))}
          </div>
        </div>
      )}
    </Card>
  );
}

function NaturalOnboardingResult({
  result,
  onUseQuestion,
  onPrepareQuestion,
  onGoTrends,
  isAnalyzingFollowUp = false,
  analyzingFollowUpQuestion = '',
}) {
  const [showAllCards, setShowAllCards] = useState(false);
  const directAnswer = extractDirectAnswer(result);
  const followUpQuestions = result.followUpQuestions || [];
  const evidenceCards = result.evidenceCards || [];
  const recommendedConcepts = result.recommendedConcepts || [];
  const memorySummary = result.memorySummary;
  const analysisCriteria = resolveAnalysisCriteria(result);
  const hasAnalysisCriteria = analysisCriteria.length > 0;
  const isSimpleConversation = isSimpleConversationResult(result);
  const candidateCards = buildCandidateCards(evidenceCards, recommendedConcepts);
  const visibleCandidateCards = showAllCards ? candidateCards : candidateCards.slice(0, 3);
  const hasReport = hasValue(result.report);
  const summaryText = result.summary || directAnswer;
  const shouldShowDirectAnswer = hasValue(directAnswer) && directAnswer !== summaryText;

  if (isSimpleConversation) {
    return (
      <div className="natural-result-layout simple-agent-result">
        <Card className="agent-answer-card compact-agent-answer">
          <p className="section-kicker">Agent</p>
          <p className="agent-short-answer">{directAnswer || summaryText}</p>
        </Card>
      </div>
    );
  }

  return (
    <div className="natural-result-layout">
      <Card className="agent-answer-card compact-agent-answer">
        <p className="section-kicker">Answer</p>
        <h3>{summaryText || '분석 결과를 정리했어요.'}</h3>
        {result.savedToConversation && (
          <span className="conversation-saved-badge">이 대화에 저장됨</span>
        )}
        {shouldShowDirectAnswer && (
          <p className="agent-short-answer">{directAnswer}</p>
        )}
      </Card>

      {candidateCards.length > 0 && (
        <Card className="evidence-section agent-candidate-section">
          <div className="card-heading">
            <div>
              <p className="section-kicker">Recommendations</p>
              <h3>추천 게임과 근거</h3>
            </div>
            {onGoTrends && (
              <Button variant="secondary" onClick={onGoTrends}>트렌드 데이터 보기</Button>
            )}
          </div>

          <div className="evidence-card-grid image-evidence-grid">
            {visibleCandidateCards.map((evidence, index) => (
              <EvidenceCard
                evidence={evidence}
                key={`${evidence.type || 'evidence'}-${evidence.title || index}`}
              />
            ))}
          </div>

          {candidateCards.length > 3 && (
            <button
              className="evidence-more-button"
              type="button"
              onClick={() => setShowAllCards((current) => !current)}
            >
              {showAllCards ? '접기' : `더 보기 ${candidateCards.length - 3}개`}
            </button>
          )}
        </Card>
      )}

      {followUpQuestions.length > 0 && (
        <Card className="follow-up-card">
          <p className="section-kicker">Follow-up</p>
          <h3>이어서 물어보기</h3>
          {isAnalyzingFollowUp && <LoadingIndicator label="후속 질문을 이어서 분석하는 중" />}
          <div className="follow-up-list">
            {followUpQuestions.map((question) => {
              const isLoadingQuestion = isAnalyzingFollowUp && analyzingFollowUpQuestion === question;
              return (
                <div className="follow-up-action" key={question}>
                  <button
                    className={`question-chip ${isLoadingQuestion ? 'loading' : ''}`}
                    type="button"
                    disabled={isAnalyzingFollowUp}
                    onClick={() => onUseQuestion(question)}
                  >
                    {isLoadingQuestion ? '분석 중...' : question}
                  </button>
                  <button
                    className="question-chip secondary"
                    type="button"
                    disabled={isAnalyzingFollowUp || !onPrepareQuestion}
                    onClick={() => onPrepareQuestion?.(question)}
                  >
                    입력창에 넣기
                  </button>
                </div>
              );
            })}
          </div>
        </Card>
      )}

      {hasAnalysisCriteria && (
        <details className="agent-collapsible-panel">
          <summary>분석 기준 보기</summary>
          <AgentPlanCriteria criteria={analysisCriteria} />
        </details>
      )}

      {memorySummary && (
        <details className="agent-collapsible-panel">
          <summary>대화 흐름 보기</summary>
          <ConversationMemoryCard memorySummary={memorySummary} sessionId={result.sessionId} />
        </details>
      )}

      {hasReport && (
        <details className="agent-collapsible-panel report-collapsible-panel">
          <summary>자세히 보기</summary>
          <pre className="report-draft">{result.report}</pre>
        </details>
      )}
    </div>
  );
}

function ConversationMemoryCard({ memorySummary, sessionId }) {
  const memoryItems = [
    { label: '세션', value: sessionId || memorySummary.sessionId },
    { label: '현재 목표', value: memorySummary.currentUserGoal },
    { label: '마지막 의도', value: hasValue(memorySummary.lastIntent) ? formatIntent(memorySummary.lastIntent) : null },
    { label: '사용자 관점', value: hasValue(memorySummary.lastUserRole) ? formatUserRole(memorySummary.lastUserRole) : null },
    { label: '선호 플랫폼', value: hasValue(memorySummary.preferredPlatform) ? formatPlatformFilter(memorySummary.preferredPlatform) : null },
    { label: '정렬 기준', value: hasValue(memorySummary.preferredSortMetric) ? formatSortMetric(memorySummary.preferredSortMetric) : null },
  ].filter((item) => hasValue(item.value));
  const recommendedGames = formatMemoryList(memorySummary.recommendedGames);
  const developerCandidates = formatMemoryList(memorySummary.developerCandidates);
  const reinterpretationCandidates = formatMemoryList(memorySummary.reinterpretationCandidates);
  const interactionFeatures = formatMemoryList(memorySummary.interactionFeatures);

  return (
    <Card className="conversation-memory-card">
      <div className="card-heading">
        <div>
          <p className="section-kicker">Conversation</p>
          <h3>현재 대화 흐름</h3>
        </div>
      </div>

      {hasValue(memorySummary.summaryText) && (
        <p className="conversation-memory-summary">{memorySummary.summaryText}</p>
      )}

      {memoryItems.length > 0 && (
        <div className="conversation-memory-grid">
          {memoryItems.map((item) => (
            <div className="conversation-memory-item" key={item.label}>
              <span>{item.label}</span>
              <strong>{item.value}</strong>
            </div>
          ))}
        </div>
      )}

      <div className="conversation-memory-lists">
        {recommendedGames && <MemoryList label="추천 게임" value={recommendedGames} />}
        {developerCandidates && <MemoryList label="개발 후보" value={developerCandidates} />}
        {reinterpretationCandidates && <MemoryList label="재해석 후보" value={reinterpretationCandidates} />}
        {interactionFeatures && <MemoryList label="상호작용 기능" value={interactionFeatures} />}
      </div>
    </Card>
  );
}

function MemoryList({ label, value }) {
  return (
    <div className="conversation-memory-list">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function AgentPlanCriteria({ criteria }) {
  if (!criteria.length) {
    return null;
  }

  return (
    <section className="agent-plan-criteria" aria-label="분석 기준">
      <div className="agent-plan-criteria-top">
        <span>분석 기준</span>
        <strong>질문 해석</strong>
      </div>
      <div className="agent-plan-chip-grid">
        {criteria.map((item) => (
          <div className={`agent-plan-chip ${item.kind || ''}`} key={item.label}>
            <span>{item.label}</span>
            <strong>{item.value}</strong>
          </div>
        ))}
      </div>
    </section>
  );
}

function buildCandidateCards(evidenceCards, recommendedConcepts) {
  if (Array.isArray(evidenceCards) && evidenceCards.length > 0) {
    const displayableEvidenceCards = evidenceCards.filter(isDisplayableCandidateCard);
    if (displayableEvidenceCards.length > 0) {
      return displayableEvidenceCards;
    }
  }
  if (!Array.isArray(recommendedConcepts) || recommendedConcepts.length === 0) {
    return [];
  }
  return recommendedConcepts
    .filter((concept) => hasValue(concept?.title))
    .map((concept) => ({
      title: concept.title,
      type: 'GAME_RECOMMENDATION',
      evidenceType: 'GAME_RECOMMENDATION',
      category: 'GAME_RECOMMENDATION',
      genre: concept.genre,
      reason: concept.reason,
      trendScore: concept.marketSignalScore ?? concept.streamabilityScore,
      streamabilityScore: concept.streamabilityScore,
      marketSignalScore: concept.marketSignalScore,
      devFeasibilityScore: concept.devFeasibilityScore,
      imageUrl: concept.imageUrl,
    }));
}

function isDisplayableCandidateCard(evidence) {
  if (!evidence || !hasValue(evidence.title)) {
    return false;
  }

  const title = String(evidence.title).trim();
  const evidenceType = getEvidenceType(evidence);
  const type = String(evidence.type || '').toUpperCase();
  const category = String(evidence.category || '').toUpperCase();

  if (title === '추가 정보 필요') {
    return false;
  }

  const hiddenTypes = [
    'GENERAL_CONTEXT',
    'CLARIFICATION_REQUIRED',
    'LIVE_TREND_EMPTY',
    'LIVE_TREND_PLATFORM_EMPTY',
    'REINTERPRETATION_EMPTY',
  ];

  return !hiddenTypes.some((hiddenType) => (
    evidenceType === hiddenType || type === hiddenType || category === hiddenType
  ));
}

function isSimpleConversationResult(result) {
  const intent = String(result?.agentPlan?.intent || result?.intent || '').toUpperCase();
  const analysisPurpose = String(result?.agentPlan?.analysisPurpose || result?.queryCondition?.analysisPurpose || '').toUpperCase();
  return ['SMALL_TALK', 'GREETING', 'HELP'].includes(intent)
    || ['SMALL_TALK', 'GREETING', 'HELP'].includes(analysisPurpose);
}

function EvidenceCard({ evidence }) {
  if (isLiveTrendEvidence(evidence)) {
    return <LiveTrendEvidenceCard evidence={evidence} />;
  }

  if (isReinterpretationEvidence(evidence)) {
    return <ReinterpretationEvidenceCard evidence={evidence} />;
  }

  if (isGameRecommendationEvidence(evidence)) {
    return <GameRecommendationEvidenceCard evidence={evidence} />;
  }

  const twitchViewerCount = evidence.twitchViewerCount ?? evidence.twitchTotalViewerCount;
  const metricItems = [
    { label: 'Twitch 시청자', value: twitchViewerCount, kind: 'count' },
    { label: '방송 적합성', value: evidence.streamabilityScore, kind: 'score' },
    { label: '시장 신호', value: evidence.marketSignalScore, kind: 'score' },
  ].filter((metric) => hasValue(metric.value));

  return (
    <article className="evidence-card">
      <EvidenceImage evidence={evidence} />
      <div className="evidence-card-top">
        <div>
          {hasValue(evidence.type) && (
            <span className="evidence-type-badge">{formatEvidenceType(evidence.type)}</span>
          )}
          <h4>{evidence.title || '분석 근거'}</h4>
        </div>
        {hasValue(evidence.trendScore) && (
          <div className={`evidence-trend-score ${getScoreTone(evidence.trendScore)}`}>
            <span>Trend</span>
            <strong>{formatScore(evidence.trendScore)}</strong>
          </div>
        )}
      </div>

      {hasValue(evidence.description) && <p>{evidence.description}</p>}
      {hasValue(evidence.reason) && <p className="evidence-reason">{evidence.reason}</p>}

      {metricItems.length > 0 && (
        <div className="evidence-metric-grid">
          {metricItems.map((metric) => (
            <EvidenceMetric
              kind={metric.kind}
              label={metric.label}
              value={metric.value}
              key={metric.label}
            />
          ))}
        </div>
      )}
    </article>
  );
}

function LiveTrendEvidenceCard({ evidence }) {
  const metricItems = [
    { label: '시청자 수', value: evidence.totalViewerCount, kind: 'count' },
    { label: '방송 수', value: evidence.liveStreamCount, kind: 'count' },
    { label: '방송 적합성', value: evidence.streamabilityScore, kind: 'score' },
    { label: '시장 신호', value: evidence.marketSignalScore, kind: 'score' },
  ].filter((metric) => hasValue(metric.value));

  return (
    <article className="evidence-card live-trend-evidence-card">
      <EvidenceImage evidence={evidence} />
      <div className="evidence-card-top">
        <div>
          <span className="evidence-type-badge live-trend-badge">라이브 트렌드</span>
          <h4>{evidence.title || '라이브 트렌드 근거'}</h4>
          <div className="evidence-inline-meta">
            {hasValue(evidence.source) && <span>{evidence.source}</span>}
            {hasValue(evidence.genre) && <span>{evidence.genre}</span>}
            {hasValue(evidence.signalStatus) && <span>{formatSignalStatus(evidence.signalStatus)}</span>}
            {hasValue(evidence.dataOrigin) && <span>{formatDataOrigin(evidence.dataOrigin)}</span>}
          </div>
        </div>
        {hasValue(evidence.trendScore) && (
          <div className={`evidence-trend-score ${getScoreTone(evidence.trendScore)}`}>
            <span>Trend</span>
            <strong>{formatScore(evidence.trendScore)}</strong>
          </div>
        )}
      </div>

      {hasValue(evidence.reason) && <p className="evidence-reason">{evidence.reason}</p>}

      {metricItems.length > 0 && (
        <div className="evidence-metric-grid">
          {metricItems.map((metric) => (
            <EvidenceMetric
              kind={metric.kind}
              label={metric.label}
              value={metric.value}
              key={metric.label}
            />
          ))}
        </div>
      )}
    </article>
  );
}

function GameRecommendationEvidenceCard({ evidence }) {
  const metricItems = [
    { label: '시청자 수', value: evidence.totalViewerCount, kind: 'count' },
    { label: '방송 수', value: evidence.liveStreamCount, kind: 'count' },
    { label: '방송 적합성', value: evidence.streamabilityScore, kind: 'score' },
    { label: '시장 신호', value: evidence.marketSignalScore, kind: 'score' },
  ].filter((metric) => hasValue(metric.value));

  return (
    <article className="evidence-card game-recommendation-evidence-card">
      <EvidenceImage evidence={evidence} />
      <div className="evidence-card-top">
        <div>
          <span className="evidence-type-badge recommendation-badge">플레이어 추천 근거</span>
          <h4>{evidence.title || '추천 후보'}</h4>
          <div className="evidence-inline-meta">
            {hasValue(evidence.source) && <span>{evidence.source}</span>}
            {hasValue(evidence.genre) && <span>{evidence.genre}</span>}
          </div>
        </div>
        {hasValue(evidence.trendScore) && (
          <div className={`evidence-trend-score ${getScoreTone(evidence.trendScore)}`}>
            <span>추천 점수</span>
            <strong>{formatScore(evidence.trendScore)}</strong>
          </div>
        )}
      </div>

      {hasValue(evidence.reason) && (
        <div className="recommendation-reason">
          <span>추천 이유</span>
          <p>{evidence.reason}</p>
        </div>
      )}

      {metricItems.length > 0 && (
        <div className="evidence-metric-grid">
          {metricItems.map((metric) => (
            <EvidenceMetric
              kind={metric.kind}
              label={metric.label}
              value={metric.value}
              key={metric.label}
            />
          ))}
        </div>
      )}
    </article>
  );
}

function ReinterpretationEvidenceCard({ evidence }) {
  const scoreItems = [
    { label: '과거 인기', value: evidence.legacyPopularityScore },
    { label: '리뷰 반응', value: evidence.reviewSentimentScore },
    { label: '메커니즘 독창성', value: evidence.mechanicUniquenessScore },
    { label: '방송 적합성', value: evidence.streamabilityScore },
    { label: '인터랙션 적합성', value: evidence.interactionFitScore },
    { label: '현 트렌드 적합성', value: evidence.modernTrendFitScore },
    { label: '개발 가능성', value: evidence.devFeasibilityScore },
  ].filter((metric) => hasValue(metric.value));
  const originalGenre = evidence.originalGenre || evidence.genre;

  return (
    <article className="evidence-card reinterpretation-evidence-card">
      <EvidenceImage evidence={evidence} />
      <div className="evidence-card-top">
        <div>
          <span className="evidence-type-badge reinterpretation-badge">과거 게임 재해석 후보</span>
          <h4>{evidence.title || '재해석 후보'}</h4>
          {hasValue(originalGenre) && (
            <span className="reinterpretation-genre">{originalGenre}</span>
          )}
        </div>
        {hasValue(evidence.reinterpretationScore) && (
          <div className={`evidence-trend-score reinterpretation-score ${getScoreTone(evidence.reinterpretationScore)}`}>
            <span>재해석 점수</span>
            <strong>{formatScore(evidence.reinterpretationScore)}</strong>
          </div>
        )}
      </div>

      {hasValue(evidence.reinterpretationConcept) && (
        <p className="reinterpretation-concept">{evidence.reinterpretationConcept}</p>
      )}

      {scoreItems.length > 0 && (
        <div className="reinterpretation-metric-grid">
          {scoreItems.map((metric) => (
            <EvidenceMetric
              kind="score"
              label={metric.label}
              value={metric.value}
              key={metric.label}
            />
          ))}
        </div>
      )}

      {hasValue(evidence.reason) && (
        <div className="reinterpretation-reason">
          <span>추천 이유</span>
          <p>{evidence.reason}</p>
        </div>
      )}
    </article>
  );
}

function EvidenceImage({ evidence }) {
  const imageUrl = evidence?.imageUrl || placeholderGameImageUrl;
  const title = evidence?.title || '게임 이미지';
  return (
    <div className="evidence-image-frame">
      <img
        src={imageUrl}
        alt={`${title} 이미지`}
        loading="lazy"
        onError={(event) => {
          event.currentTarget.onerror = null;
          event.currentTarget.src = placeholderGameImageUrl;
        }}
      />
    </div>
  );
}

function EvidenceMetric({ label, value, kind }) {
  if (!hasValue(value)) {
    return null;
  }

  const displayValue = kind === 'count' ? formatCount(value) : formatScore(value);

  return (
    <div className="evidence-metric">
      <span>{label}</span>
      {kind === 'score' ? (
        <strong className={`score-badge ${getScoreTone(value)}`}>{displayValue}</strong>
      ) : (
        <strong>{displayValue}</strong>
      )}
    </div>
  );
}

function extractDirectAnswer(result) {
  if (result.answer) {
    return result.answer;
  }

  if (result.directAnswer) {
    return result.directAnswer;
  }

  const report = result.report || '';
  const answerSectionIndex = report.indexOf('## 사용자 질문에 대한 답변');
  if (answerSectionIndex >= 0) {
    const answerLines = report
      .slice(answerSectionIndex)
      .split('\n')
      .slice(1)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith('##') && !line.startsWith('-'));
    if (answerLines.length > 0) {
      return answerLines[0];
    }
  }

  return result.summary;
}

function formatIntent(intent) {
  const labels = {
    TREND_ANALYSIS: '트렌드 분석',
    USER_GAME_RECOMMENDATION: '게임 추천',
    GAME_RECOMMENDATION: '게임 추천',
    DEVELOPER_MARKET_ANALYSIS: '개발 가능성',
    DEVELOPMENT_FEASIBILITY: '개발 가능성',
    SPECIFIC_GAME_ANALYSIS: '특정 게임 분석',
    STREAMING_FIT_ANALYSIS: '방송 적합성',
    INTERACTION_GAME_IDEA: '인터랙션 게임 아이디어',
    FEATURE_BASED_IDEA: '기능 기반 아이디어',
    GAME_REINTERPRETATION: '과거 게임 재해석',
    GENERAL_GAME_ADVICE: '일반 게임 조언',
    GENERAL_GAME_QUESTION: '일반 게임 질문',
    CLARIFICATION_REQUIRED: '추가 정보 필요',
    SMALL_TALK: '일상 대화',
    GREETING: '인사',
    HELP: '도움말',
    OUT_OF_SCOPE: '범위 외 질문',
  };

  return labels[intent] || intent || '분석 의도';
}

function formatUserRole(role) {
  const labels = {
    PLAYER: '플레이어',
    DEVELOPER: '개발자',
    STREAMER: '스트리머',
    UNKNOWN: '미확인',
  };

  return labels[role] || role;
}

function formatPlatformFilter(platform) {
  const value = String(platform || '').toUpperCase();
  const labels = {
    TWITCH: 'TWITCH',
    CHZZK: 'CHZZK',
    SOOP: 'SOOP',
    STEAM: 'STEAM',
    ALL: 'ALL',
  };

  return labels[value] || platform;
}

function formatSortMetric(sortMetric) {
  const labels = {
    TREND_SCORE: '트렌드 점수',
    VIEWER_COUNT: '시청자 수',
    STREAM_COUNT: '방송 수',
    STREAMER_SPREAD: '스트리머 확산도',
    MARKET_SIGNAL: '시장 신호',
  };

  return labels[sortMetric] || sortMetric;
}

function formatConfidence(confidence) {
  if (!hasValue(confidence)) {
    return null;
  }
  const number = Number(confidence);
  if (Number.isNaN(number)) {
    return confidence;
  }
  const normalized = number <= 1 ? number * 100 : number;
  return `${Math.round(normalized)}%`;
}

function formatInteractionFeatures(features) {
  if (!Array.isArray(features) || features.length === 0) {
    return null;
  }
  return features
    .filter(hasValue)
    .map((feature) => String(feature).toUpperCase())
    .join(', ');
}

function formatMemoryList(values) {
  if (!Array.isArray(values) || values.length === 0) {
    return null;
  }
  const formatted = values
    .filter(hasValue)
    .slice(0, 4)
    .join(', ');
  return formatted || null;
}

function resolveAnalysisCriteria(result) {
  const agentPlan = result.agentPlan || {};
  const queryCondition = result.queryCondition || {};
  const intent = agentPlan.intent || result.intent || queryCondition.analysisPurpose;
  const analysisPurpose = agentPlan.analysisPurpose || queryCondition.analysisPurpose;
  const fields = [
    { label: '의도', value: hasValue(intent) ? formatIntent(intent) : null },
    { label: '사용자 관점', value: formatUserRole(agentPlan.userRole) },
    { label: '플랫폼', value: formatPlatformFilter(agentPlan.platformFilter || queryCondition.platformFilter), kind: 'platform' },
    { label: '정렬 기준', value: formatSortMetric(agentPlan.sortMetric || queryCondition.sortMetric) },
    { label: '분석 목적', value: hasValue(analysisPurpose) ? formatIntent(analysisPurpose) : null },
    { label: '상호작용 기능', value: formatInteractionFeatures(agentPlan.interactionFeatures || queryCondition.interactionFeatures) },
    { label: '참조 대상', value: agentPlan.resolvedTopic },
    { label: '신뢰도', value: formatConfidence(agentPlan.confidence), kind: 'confidence' },
  ];

  return fields.filter((field) => hasValue(field.value));
}

function formatEvidenceType(type) {
  const labels = {
    TREND_GAME: '트렌드 게임',
    LIVE_TREND: '라이브 트렌드',
    LIVE_TREND_GAME: '라이브 트렌드',
    LIVE_TREND_RECOMMENDATION: '라이브 추천 근거',
    LIVE_MARKET_SIGNAL: '라이브 시장 신호',
    LIVE_STREAMING_SIGNAL: '라이브 방송 신호',
    LIVE_INTERACTION_IDEA_SIGNAL: '라이브 인터랙션 신호',
    LIVE_TREND_PLATFORM_EMPTY: '라이브 데이터 없음',
    REINTERPRETATION: '과거 게임 재해석',
    REINTERPRETATION_CANDIDATE: '과거 게임 재해석 후보',
    REINTERPRETATION_EMPTY: '재해석 후보 없음',
    INTERNAL_RECOMMENDATION: '추천 근거',
    DEVELOPMENT_SIGNAL: '개발 신호',
    TWITCH_SIGNAL: '방송 신호',
    FEATURE_SIGNAL: '기능 신호',
    MARKET_SIGNAL: '시장 신호',
  };

  return labels[type] || type;
}

function getEvidenceType(evidence) {
  return String(evidence?.evidenceType || evidence?.type || '').toUpperCase();
}

function isReinterpretationEvidence(evidence) {
  return getEvidenceType(evidence).includes('REINTERPRETATION');
}

function isLiveTrendEvidence(evidence) {
  const evidenceType = getEvidenceType(evidence);
  return evidenceType === 'LIVE_TREND' || evidenceType.startsWith('LIVE_');
}

function isGameRecommendationEvidence(evidence) {
  return getEvidenceType(evidence) === 'GAME_RECOMMENDATION';
}

function formatSignalStatus(status) {
  const labels = {
    COMPLETE: '수집 완료',
    PARTIAL: '부분 수집',
    FAILED: '수집 실패',
  };

  return labels[status] || status;
}

function formatDataOrigin(dataOrigin) {
  const labels = {
    REAL: '실제 수집 데이터',
    FALLBACK: 'fallback 데이터',
    PARTIAL: '부분 데이터',
  };

  return labels[dataOrigin] || dataOrigin;
}

function hasValue(value) {
  return value !== null && value !== undefined && String(value).trim() !== '';
}

function formatCount(value) {
  const number = Number(value);
  if (Number.isNaN(number)) {
    return value;
  }
  return new Intl.NumberFormat('ko-KR').format(number);
}

function ScoreMetric({ label, score }) {
  return (
    <div>
      <span>{label}</span>
      <strong className={`score-badge ${getScoreTone(score)}`}>{score}</strong>
    </div>
  );
}

function OnboardingHistorySection({
  histories,
  selectedHistory,
  onRefresh,
  onSelect,
  onDelete,
  isLoadingHistories,
  isLoadingHistoryDetail,
  isDeletingHistory,
}) {
  return (
    <section className="history-section" aria-labelledby="history-title">
      <div className="history-header">
        <div>
          <p className="section-kicker">History</p>
          <h3 id="history-title">분석 이력</h3>
        </div>
        <Button variant="secondary" onClick={onRefresh} disabled={isLoadingHistories}>
          {isLoadingHistories ? '이력 조회 중' : '이력 새로고침'}
        </Button>
      </div>

      <div className="history-layout">
        <div className="history-list-panel">
          {isLoadingHistories && <LoadingIndicator label="분석 이력을 불러오는 중" />}

          {!isLoadingHistories && histories.length === 0 && (
            <p className="empty-state">아직 저장된 분석 이력이 없습니다.</p>
          )}

          {!isLoadingHistories && histories.length > 0 && (
            <div className="history-card-list">
              {histories.map((history) => {
                const selected = selectedHistory?.id === history.id;
                return (
                  <article
                    className={`history-card ${selected ? 'selected' : ''}`}
                    key={history.id}
                    role="button"
                    tabIndex="0"
                    onClick={() => onSelect(history.id)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        onSelect(history.id);
                      }
                    }}
                  >
                    <div>
                      <time dateTime={history.createdAt}>{formatDateTime(history.createdAt)}</time>
                      <h4>{history.message}</h4>
                      <p>{history.summary}</p>
                      <div className="history-meta-grid" aria-label="분석 조건">
                        <MetaItem label="플랫폼" value={history.targetPlatform} />
                        <MetaItem label="팀 규모" value={history.teamSize} />
                        <MetaItem label="개발 기간" value={history.developmentPeriod} />
                      </div>
                    </div>
                    <div className="history-card-footer">
                      <span>{history.recommendedConceptCount}개 컨셉</span>
                      <button
                        className="text-danger-button"
                        type="button"
                        disabled={isDeletingHistory}
                        onClick={(event) => {
                          event.stopPropagation();
                          onDelete(history.id);
                        }}
                      >
                        삭제
                      </button>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </div>

        <div className="history-detail-panel">
          {isLoadingHistoryDetail && <LoadingIndicator label="이력 상세를 불러오는 중" />}

          {!isLoadingHistoryDetail && !selectedHistory && (
            <p className="empty-state">이력 카드를 선택하면 상세 분석 결과가 표시됩니다.</p>
          )}

          {!isLoadingHistoryDetail && selectedHistory && (
            <HistoryDetail
              history={selectedHistory}
              onDelete={onDelete}
              isDeletingHistory={isDeletingHistory}
            />
          )}
        </div>
      </div>
    </section>
  );
}

function HistoryDetail({ history, onDelete, isDeletingHistory }) {
  return (
    <div className="history-detail-content">
      <div className="history-detail-top">
        <div>
          <time dateTime={history.createdAt}>{formatDateTime(history.createdAt)}</time>
          <p className="section-kicker">원본 요청 메시지</p>
          <h3>{history.message}</h3>
          <p>{history.summary}</p>
        </div>
        <Button
          variant="secondary"
          onClick={() => onDelete(history.id)}
          disabled={isDeletingHistory}
        >
          {isDeletingHistory ? '삭제 중' : '이력 삭제'}
        </Button>
      </div>

      <div className="history-detail-meta">
        <MetaItem label="플랫폼" value={history.targetPlatform} />
        <MetaItem label="팀 규모" value={history.teamSize} />
        <MetaItem label="개발 기간" value={history.developmentPeriod} />
        <div className="history-feature-box">
          <span>선호 기능</span>
          <div className="history-feature-list">
            {resolveFeatures(history.preferredFeatures).map((feature) => (
              <strong key={feature}>{feature}</strong>
            ))}
          </div>
        </div>
      </div>

      <div className="concept-card-grid">
        {history.recommendedConcepts.map((concept) => (
          <article className="concept-card" key={`${history.id}-${concept.title}-${concept.genre}`}>
            <div className="concept-card-top">
              <div>
                <span>{concept.genre}</span>
                <h4>{concept.title}</h4>
              </div>
            </div>
            <p>{concept.reason}</p>
            <div className="concept-score-grid">
              <ScoreMetric label="스트리밍" score={concept.streamabilityScore} />
              <ScoreMetric label="마켓 신호" score={concept.marketSignalScore} />
              <ScoreMetric label="개발 가능성" score={concept.devFeasibilityScore} />
            </div>
          </article>
        ))}
      </div>

      <pre className="report-draft">{history.report}</pre>
    </div>
  );
}

function MetaItem({ label, value }) {
  return (
    <div className="history-meta-item">
      <span>{label}</span>
      <strong>{displayValue(value)}</strong>
    </div>
  );
}

function resolveFeatures(features) {
  if (!features || features.length === 0) {
    return ['미정'];
  }
  return features;
}

function displayValue(value) {
  if (value === null || value === undefined || String(value).trim() === '') {
    return '미정';
  }
  return value;
}

function formatDateTime(value) {
  if (!value) {
    return '-';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}

export { LoginRequiredCard, NaturalOnboardingResult, OnboardingHistorySection };

export default NaturalOnboardingScreen;
