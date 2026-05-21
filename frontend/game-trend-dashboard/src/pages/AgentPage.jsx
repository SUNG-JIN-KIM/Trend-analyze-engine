import { useEffect, useMemo, useState } from 'react';
import Button from '../components/common/Button.jsx';
import LoadingIndicator from '../components/common/LoadingIndicator.jsx';
import {
  LoginRequiredCard,
  NaturalOnboardingResult,
} from '../components/onboarding/NaturalOnboardingScreen.jsx';

const MAIN_SIDEBAR_COLLAPSED_KEY = 'gameTrendAgentMainSidebarCollapsed';

const starterQuestions = [
  '요즘 할만한 게임 추천해줘',
  '요즘 방송에서 뜨는 게임 알려줘',
  '치지직 기준 인기 게임 알려줘',
  '개발자가 참고할 만한 장르 알려줘',
  '과거 게임 재해석 아이디어 알려줘',
];

function AgentPage({
  dashboard,
  authUser,
  onGoHome,
  onGoTrends,
  onGoHistory,
  onGoPricing,
  onGoLogin,
  onGoRegister,
  onLogout,
}) {
  const [draftMessage, setDraftMessage] = useState('');
  const [isChatListOpen, setIsChatListOpen] = useState(false);
  const [chatSearch, setChatSearch] = useState('');
  const [stoppedNotice, setStoppedNotice] = useState('');
  const [isMainSidebarCollapsed, setIsMainSidebarCollapsed] = useState(() => (
    localStorage.getItem(MAIN_SIDEBAR_COLLAPSED_KEY) === 'true'
  ));
  const isAnalyzing = dashboard.isAnalyzingOnboarding || dashboard.isAnalyzingFollowUp;

  useEffect(() => {
    dashboard.loadLiveTrendGames(3, 'all');
  }, [dashboard.loadLiveTrendGames]);

  useEffect(() => {
    if (authUser) {
      dashboard.loadConversations();
    }
  }, [authUser, dashboard.loadConversations]);

  useEffect(() => {
    localStorage.setItem(MAIN_SIDEBAR_COLLAPSED_KEY, String(isMainSidebarCollapsed));
  }, [isMainSidebarCollapsed]);

  useEffect(() => {
    setDraftMessage('');
  }, [dashboard.selectedConversationId]);

  const pendingMessage = dashboard.activeAnalysisMessage || '';
  const chatTurns = useMemo(() => dashboard.conversationTurns || [], [dashboard.conversationTurns]);
  const filteredConversations = useMemo(() => {
    const keyword = chatSearch.trim().toLowerCase();
    if (!keyword) {
      return dashboard.conversations || [];
    }
    return (dashboard.conversations || []).filter((conversation) => (
      `${conversation.title || ''} ${conversation.lastMessage || ''}`.toLowerCase().includes(keyword)
    ));
  }, [chatSearch, dashboard.conversations]);
  const hasMessages = chatTurns.length > 0
    || Boolean(pendingMessage)
    || Boolean(stoppedNotice)
    || Boolean(dashboard.loginRequiredNotice);

  const submitQuestion = async (question = draftMessage) => {
    const message = question.trim();
    if (!message || isAnalyzing) {
      return;
    }

    setDraftMessage('');
    setStoppedNotice('');
    const shouldContinueConversation = Boolean(
      dashboard.selectedConversationId
      || dashboard.onboardingResult?.historyId
      || dashboard.currentAgentSessionId
    );
    if (shouldContinueConversation) {
      await dashboard.analyzeFollowUpQuestion(message, dashboard.onboardingResult?.historyId);
    } else {
      await dashboard.analyzeNewQuestion(message);
    }
  };

  const handleKeyDown = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      submitQuestion();
    }
  };

  const handleNewChat = () => {
    if (isAnalyzing) {
      dashboard.abortActiveAnalysis();
    }
    dashboard.startNewAgentQuestion();
    setDraftMessage('');
    setStoppedNotice('');
    setIsChatListOpen(false);
  };

  const handleSelectConversation = async (conversationId) => {
    await dashboard.selectConversation(conversationId);
    setStoppedNotice('');
    setIsChatListOpen(false);
  };

  const handleStopGeneration = () => {
    dashboard.abortActiveAnalysis();
    setStoppedNotice('응답 생성을 중지했습니다.');
  };

  return (
    <div className={`agent-workspace-shell ${isMainSidebarCollapsed ? 'main-sidebar-collapsed' : ''} ${isChatListOpen ? 'chat-list-open' : ''}`}>
      <AgentMainSidebar
        collapsed={isMainSidebarCollapsed}
        authUser={authUser}
        onToggle={() => setIsMainSidebarCollapsed((collapsed) => !collapsed)}
        onGoHome={onGoHome}
        onGoAgent={() => setIsChatListOpen(false)}
        onGoTrends={onGoTrends}
        onGoHistory={onGoHistory}
        onGoPricing={onGoPricing}
        onGoLogin={onGoLogin}
        onLogout={onLogout}
      />

      <button
        className="agent-chat-list-mobile-button"
        type="button"
        onClick={() => setIsChatListOpen((open) => !open)}
        aria-label="채팅 목록 열기"
      >
        <span />
        <span />
        <span />
      </button>

      <AgentChatListPanel
        authUser={authUser}
        conversations={filteredConversations}
        selectedConversationId={dashboard.selectedConversationId}
        searchValue={chatSearch}
        isLoading={dashboard.isLoadingConversations}
        isLoadingDetail={dashboard.isLoadingConversationDetail}
        isDeleting={dashboard.isDeletingConversation}
        onSearchChange={setChatSearch}
        onNewConversation={handleNewChat}
        onSelectConversation={handleSelectConversation}
        onDeleteConversation={dashboard.removeSavedConversation}
        onGoLogin={onGoLogin}
        onGoRegister={onGoRegister}
        onCloseMobile={() => setIsChatListOpen(false)}
      />

      <main className="agent-chat-main">
        <section className="agent-chat-scroll" aria-label="Agent 대화">
          {!hasMessages && (
            <EmptyChatState
              isAnalyzing={isAnalyzing}
              onUseQuestion={submitQuestion}
            />
          )}

          {chatTurns.map((turn, index) => {
            const isLastTurn = index === chatTurns.length - 1;
            const canShowDetailedResult = isLastTurn && dashboard.onboardingResult && !pendingMessage;
            return (
              <ChatTurn
                turn={turn}
                result={canShowDetailedResult ? dashboard.onboardingResult : null}
                onUseQuestion={submitQuestion}
                onPrepareQuestion={(question) => setDraftMessage(question)}
                onGoTrends={onGoTrends}
                isAnalyzingFollowUp={dashboard.isAnalyzingFollowUp}
                analyzingFollowUpQuestion={dashboard.analyzingFollowUpQuestion}
                key={turn.id || `${turn.question}-${index}`}
              />
            );
          })}

          {pendingMessage && (
            <PendingChatTurn message={pendingMessage} />
          )}

          {stoppedNotice && (
            <SystemChatNotice message={stoppedNotice} />
          )}

          {dashboard.loginRequiredNotice && (
            <div className="chat-row agent">
              <div className="chat-avatar agent">AI</div>
              <div className="chat-bubble agent login-required-bubble">
                <LoginRequiredCard
                  notice={dashboard.loginRequiredNotice}
                  onGoLogin={onGoLogin}
                  onGoRegister={onGoRegister}
                  onAnalyzePublicQuestion={submitQuestion}
                  isAnalyzing={isAnalyzing}
                />
              </div>
            </div>
          )}
        </section>

        <form className="agent-chat-composer" onSubmit={(event) => {
          event.preventDefault();
          submitQuestion();
        }}>
          <label>
            <span>메시지</span>
            <textarea
              value={draftMessage}
              onChange={(event) => setDraftMessage(event.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="게임 트렌드, 인기 게임, 개발 아이디어를 물어보세요"
              rows="2"
            />
          </label>
          {isAnalyzing ? (
            <button
              className="agent-stop-button"
              type="button"
              onClick={handleStopGeneration}
            >
              정지
            </button>
          ) : (
            <Button type="submit" disabled={!draftMessage.trim()}>
              전송
            </Button>
          )}
        </form>
      </main>
    </div>
  );
}

function AgentMainSidebar({
  collapsed,
  authUser,
  onToggle,
  onGoHome,
  onGoAgent,
  onGoTrends,
  onGoHistory,
  onGoPricing,
  onGoLogin,
  onLogout,
}) {
  const menuItems = [
    { icon: 'H', label: '홈', onClick: onGoHome },
    { icon: 'A', label: 'Agent', onClick: onGoAgent, active: true },
    { icon: 'R', label: '실시간 순위', onClick: onGoTrends },
    ...(authUser ? [{ icon: 'C', label: '대화 기록', onClick: onGoHistory }] : []),
    { icon: '$', label: '요금 안내', onClick: onGoPricing },
    { icon: 'U', label: authUser ? '프로필' : '로그인/프로필', onClick: authUser ? undefined : onGoLogin },
  ];

  return (
    <aside className="agent-main-sidebar" aria-label="메인 메뉴">
      <div className="agent-main-brand">
        <span className="agent-main-logo">GT</span>
        {!collapsed && (
          <div>
            <strong>Game Trend</strong>
            <small>Agent-V</small>
          </div>
        )}
      </div>

      <button
        className="agent-main-toggle"
        type="button"
        onClick={onToggle}
        aria-label={collapsed ? '메인 사이드바 펼치기' : '메인 사이드바 접기'}
      >
        {collapsed ? '>' : '<'}
      </button>

      <nav className="agent-main-nav" aria-label="Agent 메인 메뉴">
        {menuItems.map((item) => (
          <button
            className={item.active ? 'active' : ''}
            type="button"
            key={item.label}
            onClick={item.onClick}
            disabled={!item.onClick}
            title={collapsed ? item.label : undefined}
          >
            <span className="agent-main-nav-icon" aria-hidden="true">{item.icon}</span>
            {!collapsed && <span className="agent-main-nav-label">{item.label}</span>}
          </button>
        ))}
      </nav>

      <div className="agent-main-profile">
        <span className="agent-main-nav-icon" aria-hidden="true">U</span>
        {!collapsed && (
          <div>
            <strong>{authUser ? authUser.nickname || authUser.email : '비로그인'}</strong>
            {authUser ? (
              <button type="button" onClick={onLogout}>로그아웃</button>
            ) : (
              <button type="button" onClick={onGoLogin}>로그인하기</button>
            )}
          </div>
        )}
      </div>
    </aside>
  );
}

function AgentChatListPanel({
  authUser,
  conversations,
  selectedConversationId,
  searchValue,
  isLoading,
  isLoadingDetail,
  isDeleting,
  onSearchChange,
  onNewConversation,
  onSelectConversation,
  onDeleteConversation,
  onGoLogin,
  onGoRegister,
  onCloseMobile,
}) {
  return (
    <aside className="agent-chat-list-panel" aria-label="채팅 목록">
      <div className="agent-chat-list-header">
        <div>
          <p className="section-kicker">Agent-V</p>
          <h2>내 채팅</h2>
        </div>
        <button className="agent-chat-list-close" type="button" onClick={onCloseMobile} aria-label="채팅 목록 닫기">
          x
        </button>
      </div>

      <button className="agent-new-chat-button" type="button" onClick={onNewConversation}>
        새 채팅
      </button>

      <label className="agent-chat-search">
        <span>채팅 검색</span>
        <input
          type="search"
          value={searchValue}
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder="대화 제목이나 마지막 메시지 검색"
          disabled={!authUser}
        />
      </label>

      <div className="agent-chat-list-section">
        <div className="agent-sidebar-section-title">
          <span>내 채팅 목록</span>
          {isLoading && <LoadingIndicator label="조회 중" />}
        </div>

        {authUser && conversations.length === 0 && !isLoading && (
          <p className="agent-sidebar-empty">아직 저장된 대화가 없습니다.</p>
        )}

        {authUser && conversations.length > 0 && (
          <div className="agent-sidebar-conversations">
            {conversations.map((conversation) => {
              const active = Number(conversation.id) === Number(selectedConversationId);
              return (
                <article className={`agent-sidebar-conversation ${active ? 'active' : ''}`} key={conversation.id}>
                  <button
                    type="button"
                    disabled={isLoadingDetail}
                    onClick={() => onSelectConversation(conversation.id)}
                  >
                    <strong>{conversation.title || '새 대화'}</strong>
                    <span>{conversation.lastMessage || '아직 메시지가 없습니다.'}</span>
                    <time>{formatDateTime(conversation.updatedAt)}</time>
                  </button>
                  <button
                    className="agent-sidebar-delete"
                    type="button"
                    disabled={isDeleting}
                    onClick={() => onDeleteConversation(conversation.id)}
                    aria-label={`${conversation.title || '대화'} 삭제`}
                  >
                    삭제
                  </button>
                </article>
              );
            })}
          </div>
        )}

        {!authUser && (
          <div className="agent-sidebar-login-card">
            <p>로그인하면 대화 기록을 저장할 수 있어요.</p>
            <div>
              <button type="button" onClick={onGoLogin}>로그인하기</button>
              <button type="button" onClick={onGoRegister}>회원가입</button>
            </div>
          </div>
        )}
      </div>
    </aside>
  );
}

function EmptyChatState({ isAnalyzing, onUseQuestion }) {
  return (
    <div className="agent-empty-chat">
      <p className="section-kicker">Game Trend Agent</p>
      <h2>오늘은 어떤 게임 트렌드를 분석해볼까요?</h2>
      <p>실시간 순위, 방송 반응, 플레이어 추천, 개발 아이디어를 대화처럼 이어서 물어보세요.</p>
      <div className="agent-empty-question-grid">
        {starterQuestions.map((question) => (
          <button
            type="button"
            key={question}
            disabled={isAnalyzing}
            onClick={() => onUseQuestion(question)}
          >
            {question}
          </button>
        ))}
      </div>
    </div>
  );
}

function ChatTurn({
  turn,
  result,
  onUseQuestion,
  onPrepareQuestion,
  onGoTrends,
  isAnalyzingFollowUp,
  analyzingFollowUpQuestion,
}) {
  return (
    <article className="chat-turn-pair">
      <div className="chat-row user">
        <div className="chat-bubble user">
          <p>{turn.question}</p>
        </div>
      </div>

      <div className="chat-row agent">
        <div className="chat-avatar agent">AI</div>
        <div className="chat-bubble agent">
          {result ? (
            <NaturalOnboardingResult
              result={result}
              onUseQuestion={onUseQuestion}
              onPrepareQuestion={onPrepareQuestion}
              onGoTrends={onGoTrends}
              isAnalyzingFollowUp={isAnalyzingFollowUp}
              analyzingFollowUpQuestion={analyzingFollowUpQuestion}
            />
          ) : (
            <>
              <div className="chat-bubble-topline">
                <span>Agent 답변</span>
                {turn.intent && <b>{formatIntent(turn.intent)}</b>}
              </div>
              <p>{turn.answer || turn.summary || '이전 Agent 답변입니다.'}</p>
            </>
          )}
        </div>
      </div>
    </article>
  );
}

function PendingChatTurn({ message }) {
  return (
    <article className="chat-turn-pair">
      <div className="chat-row user">
        <div className="chat-bubble user">
          <p>{message}</p>
        </div>
      </div>
      <div className="chat-row agent">
        <div className="chat-avatar agent">AI</div>
        <div className="chat-bubble agent typing">
          <span>분석 중...</span>
          <div className="typing-dots" aria-hidden="true">
            <i />
            <i />
            <i />
          </div>
        </div>
      </div>
    </article>
  );
}

function SystemChatNotice({ message }) {
  return (
    <div className="chat-row agent">
      <div className="chat-avatar agent">AI</div>
      <div className="chat-bubble agent system">
        <p>{message}</p>
      </div>
    </div>
  );
}

function formatIntent(intent) {
  const labels = {
    TREND_ANALYSIS: '트렌드 분석',
    USER_GAME_RECOMMENDATION: '게임 추천',
    GAME_RECOMMENDATION: '게임 추천',
    DEVELOPER_MARKET_ANALYSIS: '개발 가능성',
    STREAMING_FIT_ANALYSIS: '방송 적합성',
    INTERACTION_GAME_IDEA: '인터랙션 아이디어',
    GAME_REINTERPRETATION: '과거 게임 재해석',
  };
  return labels[intent] || intent;
}

function formatDateTime(value) {
  if (!value) {
    return '시간 정보 없음';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('ko-KR', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export default AgentPage;
