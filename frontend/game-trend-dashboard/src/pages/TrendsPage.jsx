import { useEffect } from 'react';
import Button from '../components/common/Button.jsx';
import Card from '../components/common/Card.jsx';
import LoadingIndicator from '../components/common/LoadingIndicator.jsx';
import { formatScore, getScoreTone } from '../utils/score.js';

function TrendsPage({ dashboard }) {
  useEffect(() => {
    dashboard.loadTrendGames();
  }, [dashboard.loadTrendGames]);

  const isBusy = dashboard.isLoadingTrendGames || dashboard.isRefreshingTrendData;
  const topGame = dashboard.trendGames[0];

  return (
    <div className="page-stack">
      <Card className="page-intro-card trend-intro-card">
        <div className="trend-page-heading">
          <div>
            <p className="section-kicker">Trend Signals</p>
            <h2>게임 트렌드 시그널</h2>
            <p>
              Steam 리뷰, Twitch 시청/방송 지표, 내부 추천 점수를 합쳐 trendScore를 계산합니다.
              Agent 메인에서 “요즘 어떤 게임이 인기 있어?”라고 질문하면 이 데이터가 분석 근거로 사용됩니다.
            </p>
          </div>
          <div className="trend-actions">
            {isBusy && <LoadingIndicator label={dashboard.isRefreshingTrendData ? '트렌드 갱신 중' : '트렌드 조회 중'} />}
            <Button onClick={dashboard.refreshTrendSignals} disabled={dashboard.isRefreshingTrendData}>
              {dashboard.isRefreshingTrendData ? '새로고침 중' : '트렌드 데이터 새로고침'}
            </Button>
          </div>
        </div>
      </Card>

      <div className="summary-grid trend-summary-grid">
        <div className="summary-card highlight">
          <span>최고 trendScore</span>
          <strong>{topGame ? `${formatScore(topGame.trendScore)}점` : '-'}</strong>
        </div>
        <div className="summary-card">
          <span>상위 게임</span>
          <strong>{dashboard.trendGames.length}</strong>
        </div>
        <div className="summary-card">
          <span>최근 갱신</span>
          <strong>{topGame ? formatDateTime(topGame.updatedAt) : '-'}</strong>
        </div>
      </div>

      {dashboard.trendRefreshResult && (
        <Card className="trend-refresh-card">
          <div className="trend-refresh-grid">
            <MetricBlock label="요청 게임" value={dashboard.trendRefreshResult.requestedCount} />
            <MetricBlock label="갱신 성공" value={dashboard.trendRefreshResult.refreshedCount} />
            <MetricBlock label="부분 fallback" value={dashboard.trendRefreshResult.partialCount} />
            <MetricBlock label="갱신 시각" value={formatDateTime(dashboard.trendRefreshResult.refreshedAt)} />
          </div>
          <p>{dashboard.trendRefreshResult.message}</p>
        </Card>
      )}

      <Card>
        <div className="card-heading">
          <div>
            <p className="section-kicker">Top Games</p>
            <h3>trendScore 상위 게임</h3>
          </div>
          <Button variant="secondary" onClick={dashboard.loadTrendGames} disabled={dashboard.isLoadingTrendGames}>
            {dashboard.isLoadingTrendGames ? '조회 중' : '목록 새로고침'}
          </Button>
        </div>

        {dashboard.isLoadingTrendGames && <LoadingIndicator label="트렌드 게임 목록을 불러오는 중" />}

        {!dashboard.isLoadingTrendGames && dashboard.trendGames.length === 0 && (
          <p className="empty-state">
            아직 수집된 트렌드 데이터가 없습니다. 새로고침을 눌러주세요.
          </p>
        )}

        {!dashboard.isLoadingTrendGames && dashboard.trendGames.length > 0 && (
          <div className="trend-card-grid">
            {dashboard.trendGames.map((game, index) => (
              <TrendGameCard game={game} rank={index + 1} key={game.id || game.title} />
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}

function TrendGameCard({ game, rank }) {
  return (
    <article className="trend-game-card">
      <div className="trend-game-top">
        <div>
          <span className="rank-pill">#{rank}</span>
          <h4>{game.title}</h4>
          <p>{game.genre} · {game.platform}</p>
        </div>
        <div className={`trend-score-emphasis ${getScoreTone(game.trendScore)}`}>
          <span>trendScore</span>
          <strong>{formatScore(game.trendScore)}</strong>
        </div>
      </div>

      <div className="trend-metric-grid">
        <ScoreMetric label="스트리밍" score={game.streamabilityScore} />
        <ScoreMetric label="마켓 신호" score={game.marketSignalScore} />
        <ScoreMetric label="Steam 리뷰" score={game.steamReviewScore} />
        <MetricBlock label="Steam 리뷰 수" value={formatNumber(game.steamTotalReviews)} />
        <MetricBlock label="Twitch 방송 수" value={formatNumber(game.twitchLiveStreamCount)} />
        <MetricBlock label="Twitch 시청자" value={formatNumber(game.twitchTotalViewerCount)} />
        <MetricBlock label="플랫폼" value={game.platform} />
        <MetricBlock label="업데이트" value={formatDateTime(game.updatedAt)} />
      </div>

      <p className="trend-reason">{game.reason || '트렌드 계산 근거가 아직 없습니다.'}</p>
    </article>
  );
}

function ScoreMetric({ label, score }) {
  const normalizedScore = normalizeScore(score);
  const tone = getScoreTone(normalizedScore);

  return (
    <div className="trend-metric-block score-metric-block">
      <span>{label}</span>
      <strong className={`score-text ${tone}`}>{formatScore(normalizedScore)}</strong>
      <div className="score-gauge" aria-hidden="true">
        <span className={tone} style={{ width: `${normalizedScore}%` }} />
      </div>
    </div>
  );
}

function MetricBlock({ label, value, tone }) {
  return (
    <div className="trend-metric-block">
      <span>{label}</span>
      <strong className={tone ? `score-text ${tone}` : undefined}>{value ?? '-'}</strong>
    </div>
  );
}

function normalizeScore(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return 0;
  }
  return Math.min(100, Math.max(0, Number(value)));
}

function formatNumber(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-';
  }
  return Number(value).toLocaleString('ko-KR');
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

export default TrendsPage;
