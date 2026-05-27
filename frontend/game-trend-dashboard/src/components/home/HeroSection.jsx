import { useState } from 'react';
import Button from '../common/Button.jsx';

const suggestedQuestions = [
  '요즘 뜨는 게임 알려줘',
  '마인크래프트 유튜브 반응 어때?',
  '개발자가 참고할 장르 추천해줘',
  '실시간 순위에서 주목할 게임은?',
];

function HeroSection({ onAsk, onNavigate, isAnalyzing = false }) {
  const [message, setMessage] = useState('');

  const handlePrimaryClick = () => {
    onAsk?.('요즘 유튜브와 라이브 순위에서 반응 좋은 게임 알려줘');
  };

  const submitQuestion = (question = message) => {
    const trimmed = question.trim();
    if (!trimmed || isAnalyzing) {
      return;
    }
    setMessage(trimmed);
    onAsk?.(trimmed);
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    submitQuestion();
  };

  const handleKeyDown = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      submitQuestion();
    }
  };

  return (
    <section className="home-hero-v2">
      <div className="home-hero-v2-copy">
        <div className="home-hero-badge">
          <span className="home-symbol-mark" aria-hidden="true">GT</span>
          <span>Live Game Trend Intelligence</span>
        </div>
        <h1>게임 트렌드를 한눈에 읽고, 다음 아이디어까지 연결하세요</h1>
        <p>
          Agent 질문, 실시간 순위, YouTube 관심도, 대화 기록을 하나의 흐름으로 묶어 지금 반응이 생기는 게임과
          개발 기회를 빠르게 파악합니다.
        </p>
        <div className="home-hero-signal-row" aria-label="분석 신호">
          <span>Live Ranking</span>
          <span>YouTube Signal</span>
          <span>AI Agent</span>
          <span>Trend Memory</span>
        </div>
        <div className="home-hero-actions">
          <Button onClick={handlePrimaryClick} disabled={isAnalyzing}>
            {isAnalyzing ? '분석 중' : 'Agent에게 물어보기'}
          </Button>
          <Button variant="secondary" onClick={() => onNavigate?.('/youtube-trends')}>
            YouTube 트렌드 보기
          </Button>
        </div>

        <form className="home-hero-ask-card" onSubmit={handleSubmit}>
          <label htmlFor="home-agent-question">
            <span>Agent에게 바로 질문하기</span>
            <textarea
              id="home-agent-question"
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="예: 요즘 유튜브에서 반응 좋은 게임 알려줘"
              rows="3"
            />
          </label>
          <div className="home-hero-ask-actions">
            <small>Enter로 바로 분석</small>
            <Button type="submit" disabled={!message.trim() || isAnalyzing}>
              {isAnalyzing ? '분석 중' : '질문하기'}
            </Button>
          </div>
          <div className="home-hero-question-chips" aria-label="추천 질문">
            {suggestedQuestions.map((question) => (
              <button
                type="button"
                key={question}
                disabled={isAnalyzing}
                onClick={() => submitQuestion(question)}
              >
                {question}
              </button>
            ))}
          </div>
        </form>
      </div>

      <div className="home-hero-visual" aria-label="게임 트렌드 대시보드 미리보기">
        <div className="home-dashboard-illustration">
          <div className="home-dashboard-sidebar">
            <span />
            <span />
            <span />
            <strong />
          </div>
          <div className="home-dashboard-main">
            <div className="home-dashboard-chart">
              <span />
              <span />
              <span />
              <span />
            </div>
            <div className="home-dashboard-panels">
              <span />
              <span />
            </div>
          </div>
          <div className="home-dashboard-avatar" aria-hidden="true">
            <span />
          </div>
        </div>
        <div className="home-hero-floating-card score">
          <span>TOP 관심도</span>
          <strong>87.5</strong>
        </div>
        <div className="home-hero-floating-card rank">
          <span>Live Rank</span>
          <strong>#1</strong>
        </div>
      </div>
    </section>
  );
}

export default HeroSection;
