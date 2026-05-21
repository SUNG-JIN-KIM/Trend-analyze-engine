import { useEffect, useState } from 'react';
import { getLiveTrendRankings } from '../api/gameTrendApi.js';
import Button from '../components/common/Button.jsx';
import Card from '../components/common/Card.jsx';
import LoadingIndicator from '../components/common/LoadingIndicator.jsx';
import { formatScore, getScoreTone } from '../utils/score.js';

const suggestedQuestions = [
  '요즘 할만한 게임 추천해줘',
  '요즘 방송에서 뜨는 게임 알려줘',
  '치지직 기준 인기 게임 알려줘',
  '개발자가 참고할 만한 장르 알려줘',
  '과거 게임 재해석 아이디어 알려줘',
];

function HomePage({ onAsk, isAnalyzing = false }) {
  const [message, setMessage] = useState('');
  const [rankings, setRankings] = useState([]);
  const [isLoadingRankings, setIsLoadingRankings] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setIsLoadingRankings(true);
    getLiveTrendRankings({ limit: 5, sort: 'TREND_SCORE' })
      .then((data) => {
        if (!cancelled) {
          setRankings(Array.isArray(data) ? data.slice(0, 5) : []);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setRankings([]);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoadingRankings(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const submitQuestion = (question = message) => {
    const trimmed = question.trim();
    if (!trimmed || isAnalyzing) {
      return;
    }
    setMessage(trimmed);
    onAsk?.(trimmed);
  };

  const handleKeyDown = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      submitQuestion();
    }
  };

  return (
    <div className="home-page">
      <section className="home-hero">
        <div className="home-hero-copy">
          <p className="section-kicker">Game Trend Agent</p>
          <h2>오늘은 어떤 게임 트렌드를 분석해볼까요?</h2>
          <p>
            실시간 라이브 순위와 대화형 Agent를 통해 지금 뜨는 게임, 방송 반응, 개발 기회를 빠르게 확인하세요.
          </p>
        </div>

        <Card className="home-ask-card">
          <label className="home-ask-input">
            <span>질문 입력</span>
            <textarea
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="요즘 방송에서 반응 좋은 게임 알려줘"
              rows="4"
            />
          </label>
          <div className="home-ask-actions">
            <span>Enter로 바로 분석</span>
            <Button onClick={() => submitQuestion()} disabled={!message.trim() || isAnalyzing}>
              {isAnalyzing ? '분석 중' : 'Agent에게 묻기'}
            </Button>
          </div>
        </Card>

        <div className="home-question-grid" aria-label="추천 질문">
          {suggestedQuestions.map((question) => (
            <button
              className="home-question-chip"
              type="button"
              key={question}
              disabled={isAnalyzing}
              onClick={() => submitQuestion(question)}
            >
              {question}
            </button>
          ))}
        </div>
      </section>

      <Card className="home-preview-card">
        <div className="card-heading">
          <div>
            <p className="section-kicker">Live Preview</p>
            <h3>실시간 인기 게임 미리보기</h3>
          </div>
          {isLoadingRankings && <LoadingIndicator label="순위 조회 중" />}
        </div>

        {!isLoadingRankings && rankings.length === 0 && (
          <p className="empty-state">아직 표시할 실시간 순위가 없습니다. 실시간 순위 페이지에서 새로고침해보세요.</p>
        )}

        {rankings.length > 0 && (
          <div className="home-ranking-preview-list">
            {rankings.map((game, index) => (
              <article className="home-ranking-preview-item" key={`${game.rank || index}-${game.title}`}>
                <span>#{game.rank || index + 1}</span>
                <div>
                  <strong>{game.title || '이름 없는 게임'}</strong>
                  <small>{[game.source, game.genre].filter(Boolean).join(' · ') || '라이브 트렌드'}</small>
                </div>
                <b className={`score-badge ${getScoreTone(game.trendScore)}`}>
                  {formatScore(game.trendScore)}
                </b>
              </article>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}

export default HomePage;
