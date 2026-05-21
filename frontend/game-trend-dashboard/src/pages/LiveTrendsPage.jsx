import { useEffect, useMemo, useState } from 'react';
import Button from '../components/common/Button.jsx';
import Card from '../components/common/Card.jsx';
import LoadingIndicator from '../components/common/LoadingIndicator.jsx';
import { formatScore, getScoreTone } from '../utils/score.js';

const LIVE_PLATFORM_TABS = [
  { value: 'all', label: '전체' },
  { value: 'TWITCH', label: 'Twitch' },
  { value: 'CHZZK', label: 'CHZZK' },
  { value: 'SOOP', label: 'SOOP' },
];

function LiveTrendsPage({ dashboard, onGoLogin }) {
  const [selectedPlatform, setSelectedPlatform] = useState('all');

  useEffect(() => {
    dashboard.loadLiveTrendStatus();
  }, [dashboard.loadLiveTrendStatus]);

  useEffect(() => {
    dashboard.loadLiveTrendGames(8, selectedPlatform);
  }, [dashboard.loadLiveTrendGames, selectedPlatform]);

  const displayedGames = useMemo(() => {
    if (selectedPlatform === 'all') {
      return dashboard.liveTrendGames;
    }

    return dashboard.liveTrendGames.filter((game) => {
      const source = normalizePlatform(game.source || game.platform || game.platformName);
      return source === selectedPlatform;
    });
  }, [dashboard.liveTrendGames, selectedPlatform]);

  const status = dashboard.liveTrendStatus;
  const isRunning = Boolean(status?.running || dashboard.isRefreshingLiveTrendData);
  const isBusy = dashboard.isLoadingLiveTrendGames
    || dashboard.isLoadingLiveTrendStatus
    || dashboard.isRefreshingLiveTrendData;

  const refreshCurrentView = () => Promise.all([
    dashboard.loadLiveTrendStatus(),
    dashboard.loadLiveTrendGames(8, selectedPlatform),
  ]);

  const refreshManually = () => dashboard.refreshLiveTrendSignals(selectedPlatform);

  return (
    <div className="page-stack">
      <Card className="page-intro-card live-trend-intro-card">
        <div className="trend-page-heading">
          <div>
            <p className="section-kicker">Live Trend Signals</p>
            <h2>라이브 트렌드 데이터 확인</h2>
            <p>
              Twitch, CHZZK, SOOP, Steam의 수집 상태와 저장된 게임별 라이브 트렌드 snapshot을 확인합니다.
            </p>
          </div>
          <div className="trend-actions">
            {isBusy && (
              <LoadingIndicator
                label={dashboard.isRefreshingLiveTrendData ? '라이브 트렌드 갱신 중' : '라이브 트렌드 조회 중'}
              />
            )}
            <Button
              variant="secondary"
              onClick={refreshManually}
              disabled={dashboard.isRefreshingLiveTrendData || status?.running}
            >
              {dashboard.isRefreshingLiveTrendData || status?.running ? '갱신 중...' : '지금 수동 갱신'}
            </Button>
          </div>
        </div>
      </Card>

      {dashboard.loginRequiredNotice && (
        <Card className="rankings-notice-card warning">
          <strong>{dashboard.loginRequiredNotice.title || '로그인이 필요한 기능입니다'}</strong>
          <p>{dashboard.loginRequiredNotice.message}</p>
          {onGoLogin && (
            <Button type="button" onClick={onGoLogin}>
              로그인하기
            </Button>
          )}
        </Card>
      )}

      <LiveTrendStatusCard
        status={status}
        isLoading={dashboard.isLoadingLiveTrendStatus}
        isRunning={isRunning}
        onRefreshStatus={dashboard.loadLiveTrendStatus}
      />

      <PlatformStatusSection
        platformStatuses={status?.platformStatuses || []}
        isLoading={dashboard.isLoadingLiveTrendStatus}
        onRefreshStatus={dashboard.loadLiveTrendStatus}
      />

      {dashboard.liveTrendRefreshResult && (
        <Card className="trend-refresh-card">
          <div className="trend-refresh-grid">
            <MetricBlock label="요청 항목" value={dashboard.liveTrendRefreshResult.requestedCount} />
            <MetricBlock label="갱신 항목" value={dashboard.liveTrendRefreshResult.refreshedCount} />
            <MetricBlock label="부분 fallback" value={dashboard.liveTrendRefreshResult.partialCount} />
            <MetricBlock label="갱신 상태" value={dashboard.liveTrendRefreshResult.status} />
          </div>
          <p>{dashboard.liveTrendRefreshResult.message}</p>
        </Card>
      )}

      <Card>
        <div className="card-heading">
          <div>
            <p className="section-kicker">Top Live Games</p>
            <h3>라이브 trendScore 상위 게임</h3>
          </div>
          <div className="live-trend-toolbar">
            <Button
              variant="secondary"
              onClick={refreshCurrentView}
              disabled={dashboard.isLoadingLiveTrendGames || dashboard.isLoadingLiveTrendStatus}
            >
              {dashboard.isLoadingLiveTrendGames || dashboard.isLoadingLiveTrendStatus ? '조회 중' : '목록 새로고침'}
            </Button>
          </div>
        </div>

        <PlatformTabs selectedPlatform={selectedPlatform} onSelectPlatform={setSelectedPlatform} />

        {dashboard.isLoadingLiveTrendGames && <LoadingIndicator label="라이브 트렌드 게임 목록을 불러오는 중" />}

        {!dashboard.isLoadingLiveTrendGames && dashboard.liveTrendGames.length === 0 && (
          <p className="empty-state">
            {selectedPlatform === 'SOOP'
              ? 'SOOP 인증 정보가 없거나 아직 실제 수집 데이터가 없습니다.'
              : '아직 수집된 라이브 트렌드 데이터가 없습니다.'}
          </p>
        )}

        {!dashboard.isLoadingLiveTrendGames && dashboard.liveTrendGames.length > 0 && displayedGames.length === 0 && (
          <p className="empty-state">
            {selectedPlatform === 'SOOP'
              ? 'SOOP 인증 정보가 없거나 아직 실제 수집 데이터가 없습니다.'
              : '선택한 플랫폼에 해당하는 라이브 트렌드 데이터가 없습니다.'}
          </p>
        )}

        {!dashboard.isLoadingLiveTrendGames && displayedGames.length > 0 && (
          <div className="trend-card-grid">
            {displayedGames.map((game, index) => (
              <LiveTrendGameCard game={game} rank={index + 1} key={game.id || `${game.source}-${game.title}-${index}`} />
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}

function LiveTrendStatusCard({ status, isLoading, isRunning, onRefreshStatus }) {
  const intervalMinutes = status?.refreshIntervalMs
    ? Math.round(status.refreshIntervalMs / 60000)
    : null;
  const statusMessage = status?.lastRefreshStatus === 'NEVER_RUN'
    ? '아직 자동 갱신이 실행되지 않았습니다. 다음 예정 시간에 실행됩니다.'
    : status?.lastRefreshMessage || '자동 갱신 상태를 불러오면 마지막 갱신 결과를 확인할 수 있습니다.';

  return (
    <Card className={`live-trend-status-card ${isRunning ? 'running' : ''}`}>
      <div className="card-heading">
        <div>
          <p className="section-kicker">Scheduler Status</p>
          <h3>자동 갱신 상태</h3>
        </div>
        <div className="trend-actions">
          {isLoading && <LoadingIndicator label="상태 조회 중" />}
          <StatusBadge status={isRunning ? 'RUNNING' : status?.lastRefreshStatus} />
          <Button variant="secondary" onClick={onRefreshStatus} disabled={isLoading}>
            상태 새로고침
          </Button>
        </div>
      </div>

      {isRunning && (
        <div className="live-trend-running-banner">
          트렌드 데이터 갱신 중...
        </div>
      )}

      <div className="live-trend-status-grid">
        <MetricBlock label="schedulerEnabled" value={formatBoolean(status?.schedulerEnabled)} />
        <MetricBlock label="refreshIntervalMs" value={formatInterval(status?.refreshIntervalMs, intervalMinutes)} />
        <MetricBlock label="refreshOnStartup" value={formatBoolean(status?.refreshOnStartup)} />
        <MetricBlock label="running" value={formatBoolean(isRunning)} />
        <MetricBlock label="lastRefreshStartedAt" value={formatDateTime(status?.lastRefreshStartedAt)} />
        <MetricBlock label="lastRefreshCompletedAt" value={formatDateTime(status?.lastRefreshCompletedAt)} />
        <MetricBlock label="lastRefreshStatus" value={status?.lastRefreshStatus || '-'} />
        <MetricBlock label="nextRefreshEstimate" value={formatDateTime(status?.nextRefreshEstimate)} />
      </div>

      <div className="live-trend-status-message">
        <span>lastRefreshMessage</span>
        <strong>{statusMessage}</strong>
      </div>
    </Card>
  );
}

function PlatformStatusSection({ platformStatuses, isLoading, onRefreshStatus }) {
  const statusesByPlatform = new Map(platformStatuses.map((status) => [normalizePlatform(status.platform), status]));
  const displayStatuses = ['TWITCH', 'CHZZK', 'SOOP', 'STEAM'].map((platform) => (
    statusesByPlatform.get(platform) || {
      platform,
      configured: false,
      status: 'NEVER_RUN',
      message: '상태 정보를 아직 불러오지 못했습니다.',
    }
  ));

  return (
    <Card className="platform-status-section">
      <div className="card-heading">
        <div>
          <p className="section-kicker">Platform Collection</p>
          <h3>플랫폼별 수집 상태</h3>
        </div>
        <div className="trend-actions">
          {isLoading && <LoadingIndicator label="플랫폼 상태 조회 중" />}
          <Button variant="secondary" onClick={onRefreshStatus} disabled={isLoading}>
            상태 새로고침
          </Button>
        </div>
      </div>

      <div className="platform-status-grid">
        {displayStatuses.map((status) => (
          <PlatformStatusCard status={status} key={status.platform} />
        ))}
      </div>
    </Card>
  );
}

function PlatformStatusCard({ status }) {
  const normalizedStatus = status.status || 'NEVER_RUN';
  const configuredLabel = status.configured ? 'true' : 'false';

  return (
    <article className={`platform-status-card ${statusTone(normalizedStatus)}`}>
      <div className="platform-status-card-top">
        <div>
          <span className="platform-name">{platformLabel(status.platform)}</span>
          <strong>configured={configuredLabel}</strong>
        </div>
        <StatusBadge status={normalizedStatus} />
      </div>
      <p>{status.message || statusLabel(normalizedStatus)}</p>
      <div className="platform-status-meta">
        <MetricBlock label="platform" value={normalizePlatform(status.platform) || '-'} />
        <MetricBlock label="status" value={normalizedStatus} />
        <MetricBlock label="마지막 성공" value={formatDateTime(status.lastSuccessAt)} />
        <MetricBlock label="마지막 실패" value={formatDateTime(status.lastFailureAt)} />
      </div>
    </article>
  );
}

function PlatformTabs({ selectedPlatform, onSelectPlatform }) {
  return (
    <div className="platform-tab-list" role="tablist" aria-label="라이브 트렌드 플랫폼 필터">
      {LIVE_PLATFORM_TABS.map((tab) => (
        <button
          className={`platform-tab ${selectedPlatform === tab.value ? 'active' : ''}`}
          type="button"
          role="tab"
          aria-selected={selectedPlatform === tab.value}
          onClick={() => onSelectPlatform(tab.value)}
          key={tab.value}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}

function LiveTrendGameCard({ game, rank }) {
  const title = game.displayGameName || game.title || game.normalizedGameName || game.sourceKeyword || '이름 없는 게임';
  const score = firstNumber(game.trendScore, game.liveTrendScore);
  const source = game.source || game.platformSource || game.livePlatform || '-';

  return (
    <article className="trend-game-card live-trend-game-card">
      <div className="trend-game-top">
        <div>
          <div className="live-trend-card-badges">
            <span className="rank-pill">#{rank}</span>
            <span className="source-pill">{source}</span>
            <span className={`source-pill ${game.dataOrigin === 'REAL' ? 'real' : 'muted'}`}>
              {dataOriginLabel(game.dataOrigin)}
            </span>
            <span className={`source-pill ${game.signalStatus === 'COMPLETE' ? 'complete' : 'muted'}`}>
              {signalStatusLabel(game.signalStatus)}
            </span>
          </div>
          <h4>{title}</h4>
          <p>
            {[game.genre, game.platform].filter(Boolean).join(' · ') || '라이브 트렌드 게임'}
          </p>
        </div>
        <div className={`trend-score-emphasis ${getScoreTone(score)}`}>
          <span>trendScore</span>
          <strong>{formatScore(score)}</strong>
        </div>
      </div>

      <div className="trend-metric-grid">
        <OptionalMetricBlock label="source" value={source} />
        <OptionalMetricBlock label="genre" value={game.genre || '-'} />
        <OptionalMetricBlock label="liveStreamCount" value={formatStreamCount(game.liveStreamCount)} />
        <OptionalMetricBlock label="totalViewerCount" value={formatViewerCount(game.totalViewerCount)} />
        <ScoreMetric label="viewerScore" score={game.viewerScore} />
        <ScoreMetric label="streamCountScore" score={game.streamCountScore} />
        <ScoreMetric label="streamabilityScore" score={game.streamabilityScore} />
        <ScoreMetric label="marketSignalScore" score={game.marketSignalScore} />
        <ScoreMetric label="trendScore" score={score} />
        <OptionalMetricBlock label="signalStatus" value={signalStatusLabel(game.signalStatus)} />
        <OptionalMetricBlock label="dataOrigin" value={dataOriginLabel(game.dataOrigin)} />
        <OptionalMetricBlock label="updatedAt" value={formatDateTime(game.updatedAt)} />
      </div>

      <details className="live-trend-raw-details">
        <summary>원본 필드</summary>
        <div className="trend-metric-grid">
          <OptionalMetricBlock label="source" value={source} />
          <OptionalMetricBlock label="genre" value={game.genre || '-'} />
          <OptionalMetricBlock label="liveStreamCount" value={formatStreamCount(game.liveStreamCount)} />
          <OptionalMetricBlock label="totalViewerCount" value={formatViewerCount(game.totalViewerCount)} />
          <OptionalMetricBlock label="viewerScore" value={formatScoreOrDash(game.viewerScore)} />
          <OptionalMetricBlock label="streamCountScore" value={formatScoreOrDash(game.streamCountScore)} />
          <OptionalMetricBlock label="streamabilityScore" value={formatScoreOrDash(game.streamabilityScore)} />
          <OptionalMetricBlock label="marketSignalScore" value={formatScoreOrDash(game.marketSignalScore)} />
          <OptionalMetricBlock label="trendScore" value={formatScoreOrDash(score)} />
          <OptionalMetricBlock label="signalStatus" value={game.signalStatus || '-'} />
          <OptionalMetricBlock label="dataOrigin" value={game.dataOrigin || '-'} />
          <OptionalMetricBlock label="updatedAt" value={formatDateTime(game.updatedAt)} />
        </div>
      </details>

      <div className="trend-metric-grid live-trend-extra-grid">
        <ScoreMetric label="스트리밍" score={game.streamabilityScore} />
        <ScoreMetric label="마켓 신호" score={game.marketSignalScore} />
        <ScoreMetric label="시청 점수" score={game.viewerScore} />
        <ScoreMetric label="방송 수 점수" score={game.streamCountScore} />
        <OptionalMetricBlock label="Twitch 시청자" value={formatViewerCount(game.twitchViewerCount)} />
        <OptionalMetricBlock label="CHZZK 시청자" value={formatViewerCount(game.chzzkViewerCount)} />
        <OptionalMetricBlock label="SOOP 시청자" value={formatViewerCount(game.soopViewerCount)} />
        <OptionalMetricBlock label="Twitch 방송 수" value={formatStreamCount(game.twitchStreamCount)} />
        <OptionalMetricBlock label="CHZZK 방송 수" value={formatStreamCount(game.chzzkStreamCount)} />
        <OptionalMetricBlock label="SOOP 방송 수" value={formatStreamCount(game.soopStreamCount)} />
        <OptionalMetricBlock label="스트리머 분산" value={formatScoreOrDash(game.streamerSpreadScore)} />
        <OptionalMetricBlock label="크리에이터 의존도" value={formatScoreOrDash(game.creatorDependencyScore)} />
        <OptionalMetricBlock label="상위 방송 점유" value={formatPercentLike(game.topStreamShare)} />
      </div>

      <p className="trend-reason">{game.reason || '라이브 트렌드 계산 근거가 아직 없습니다.'}</p>
    </article>
  );
}

function StatusBadge({ status }) {
  const normalizedStatus = status || 'NEVER_RUN';
  const tone = statusTone(normalizedStatus);

  return (
    <span className={`live-status-badge ${tone}`}>
      {statusLabel(normalizedStatus)}
    </span>
  );
}

function ScoreMetric({ label, score }) {
  if (score === null || score === undefined || Number.isNaN(Number(score))) {
    return <MetricBlock label={label} value="-" />;
  }

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

function OptionalMetricBlock({ label, value }) {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  return <MetricBlock label={label} value={value} />;
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

function firstValue(...values) {
  return values.find((value) => value !== null && value !== undefined && value !== '');
}

function firstNumber(...values) {
  const value = firstValue(...values);
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return null;
  }
  return Number(value);
}

function formatNumber(value) {
  if (value === null || value === undefined || value === '-' || Number.isNaN(Number(value))) {
    return '-';
  }
  return Number(value).toLocaleString('ko-KR');
}

function formatViewerCount(value) {
  const formattedValue = formatNumber(value);
  return formattedValue === '-' ? '-' : `${formattedValue}명`;
}

function formatStreamCount(value) {
  const formattedValue = formatNumber(value);
  return formattedValue === '-' ? '-' : `${formattedValue}개`;
}

function formatScoreOrDash(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-';
  }
  return formatScore(value);
}

function formatPercentLike(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-';
  }

  const numberValue = Number(value);
  const percent = numberValue <= 1 ? numberValue * 100 : numberValue;
  return `${percent.toFixed(1)}%`;
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

function statusTone(status) {
  if (status === 'SUCCESS') {
    return 'success';
  }
  if (status === 'RUNNING') {
    return 'running';
  }
  if (status === 'FAILED') {
    return 'danger';
  }
  if (status === 'SKIPPED' || status === 'NOT_USED_IN_THIS_REFRESH') {
    return 'muted';
  }
  if (
    status === 'PARTIAL'
    || status === 'PARTIAL_SUCCESS'
    || status === 'MISSING_CREDENTIALS'
    || status === 'PUBLIC_OR_FALLBACK'
    || status === 'NEVER_RUN'
    || status === 'WAITING'
  ) {
    return 'warning';
  }
  return 'warning';
}

function statusLabel(status) {
  const labels = {
    SUCCESS: '성공',
    PARTIAL: '부분 수집',
    PARTIAL_SUCCESS: '부분 갱신',
    FAILED: '실패',
    RUNNING: '갱신 중',
    SKIPPED: '건너뜀',
    MISSING_CREDENTIALS: '인증 정보 없음',
    PUBLIC_OR_FALLBACK: '공개 API 또는 fallback',
    NOT_USED_IN_THIS_REFRESH: '이번 갱신에서 사용 안 함',
    NEVER_RUN: '대기 중',
    WAITING: '대기 중',
  };

  return labels[status] || status;
}

function platformLabel(platform) {
  const labels = {
    TWITCH: 'Twitch',
    CHZZK: 'CHZZK',
    SOOP: 'SOOP',
    STEAM: 'Steam',
  };
  return labels[normalizePlatform(platform)] || platform || '-';
}

function normalizePlatform(platform) {
  if (!platform) {
    return '';
  }
  return String(platform).trim().toUpperCase();
}

function signalStatusLabel(status) {
  const labels = {
    COMPLETE: '수집 완료',
    PARTIAL: '부분 수집 / fallback 포함',
  };
  return labels[status] || status || '-';
}

function dataOriginLabel(dataOrigin) {
  const labels = {
    REAL: '실제 수집 데이터',
    FALLBACK: '시연용 fallback 데이터',
    PARTIAL: 'partial 데이터',
  };
  return labels[dataOrigin] || dataOrigin || '-';
}

function formatBoolean(value) {
  if (value === null || value === undefined) {
    return '-';
  }
  return value ? 'true' : 'false';
}

function formatInterval(value, intervalMinutes) {
  if (!value || Number.isNaN(Number(value))) {
    return '-';
  }
  return `${Number(value).toLocaleString('ko-KR')} ms${intervalMinutes ? ` · ${intervalMinutes}분` : ''}`;
}

export default LiveTrendsPage;
