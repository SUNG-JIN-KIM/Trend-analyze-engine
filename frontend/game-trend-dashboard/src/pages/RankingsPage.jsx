import { useEffect, useMemo, useState } from 'react';
import {
  ApiError,
  getLiveTrendRankings,
  getStoredAccessToken,
  refreshLiveTrendData,
} from '../api/gameTrendApi.js';
import Button from '../components/common/Button.jsx';
import Card from '../components/common/Card.jsx';
import LoadingIndicator from '../components/common/LoadingIndicator.jsx';
import { formatScore, getScoreTone } from '../utils/score.js';

const PLATFORM_TABS = [
  { value: 'all', label: '전체' },
  { value: 'TWITCH', label: 'Twitch' },
  { value: 'CHZZK', label: 'CHZZK' },
  { value: 'SOOP', label: 'SOOP' },
];

const SORT_OPTIONS = [
  { value: 'TREND_SCORE', label: '트렌드 점수' },
  { value: 'VIEWER_COUNT', label: '시청자 수' },
  { value: 'STREAM_COUNT', label: '방송 수' },
];

function RankingsPage() {
  const [selectedPlatform, setSelectedPlatform] = useState('all');
  const [selectedSort, setSelectedSort] = useState('TREND_SCORE');
  const [rankings, setRankings] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [permissionMessage, setPermissionMessage] = useState('');
  const [refreshMessage, setRefreshMessage] = useState('');

  const selectedPlatformLabel = useMemo(
    () => PLATFORM_TABS.find((tab) => tab.value === selectedPlatform)?.label || '전체',
    [selectedPlatform]
  );
  const selectedSortLabel = useMemo(
    () => SORT_OPTIONS.find((option) => option.value === selectedSort)?.label || '트렌드 점수',
    [selectedSort]
  );

  const loadRankings = async () => {
    setIsLoading(true);
    setErrorMessage('');
    setPermissionMessage('');
    try {
      const data = await getLiveTrendRankings({
        platform: selectedPlatform,
        sort: selectedSort,
        limit: 50,
      });
      setRankings(Array.isArray(data) ? data : []);
    } catch (error) {
      setRankings([]);
      setErrorMessage(error.message || '실시간 게임 순위를 불러오지 못했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadRankings();
  }, [selectedPlatform, selectedSort]);

  const handleRefresh = async () => {
    if (!getStoredAccessToken()) {
      setErrorMessage('');
      setRefreshMessage('');
      setPermissionMessage('라이브 트렌드 수동 갱신은 로그인 후 사용할 수 있습니다. 현재 저장된 순위 조회는 계속 볼 수 있어요.');
      return;
    }
    setIsRefreshing(true);
    setErrorMessage('');
    setPermissionMessage('');
    setRefreshMessage('');
    try {
      const result = await refreshLiveTrendData();
      setRefreshMessage(result?.message || '라이브 트렌드 데이터를 새로고침했습니다.');
      await loadRankings();
    } catch (error) {
      if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
        setPermissionMessage('지금 새로고침은 로그인 또는 관리자 권한이 필요합니다. 현재 저장된 순위는 계속 볼 수 있어요.');
      } else {
        setErrorMessage(error.message || '라이브 트렌드 새로고침에 실패했습니다.');
      }
    } finally {
      setIsRefreshing(false);
    }
  };

  const isSoopEmpty = selectedPlatform === 'SOOP' && rankings.length === 0 && !isLoading;

  return (
    <div className="rankings-page">
      <Card className="rankings-hero-card">
        <div className="trend-page-heading">
          <div>
            <p className="section-kicker">Live Rankings</p>
            <h2>실시간 게임 순위</h2>
            <p>
              Twitch, CHZZK, SOOP 라이브 트렌드 데이터를 기준으로 지금 반응이 좋은 게임을 확인합니다.
            </p>
          </div>
          <div className="rankings-hero-actions">
            {(isLoading || isRefreshing) && (
              <LoadingIndicator label={isRefreshing ? '라이브 데이터 갱신 중' : '순위 조회 중'} />
            )}
            <Button variant="secondary" onClick={loadRankings} disabled={isLoading || isRefreshing}>
              목록 새로고침
            </Button>
            <Button onClick={handleRefresh} disabled={isRefreshing}>
              {isRefreshing ? '새로고침 중...' : '지금 새로고침'}
            </Button>
          </div>
        </div>
      </Card>

      <Card className="rankings-filter-card">
        <div className="rankings-filter-top">
          <div>
            <p className="section-kicker">Filter</p>
            <h3>{selectedPlatformLabel} · {selectedSortLabel} 기준</h3>
          </div>
          <span>{rankings.length.toLocaleString('ko-KR')}개 항목</span>
        </div>

        <div className="platform-tab-list" role="tablist" aria-label="순위 플랫폼 필터">
          {PLATFORM_TABS.map((tab) => (
            <button
              className={`platform-tab ${selectedPlatform === tab.value ? 'active' : ''}`}
              type="button"
              role="tab"
              aria-selected={selectedPlatform === tab.value}
              onClick={() => setSelectedPlatform(tab.value)}
              key={tab.value}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="ranking-sort-control" aria-label="정렬 기준">
          {SORT_OPTIONS.map((option) => (
            <button
              className={`ranking-sort-button ${selectedSort === option.value ? 'active' : ''}`}
              type="button"
              onClick={() => setSelectedSort(option.value)}
              key={option.value}
            >
              {option.label}
            </button>
          ))}
        </div>
      </Card>

      {permissionMessage && (
        <Card className="rankings-notice-card warning">
          <strong>권한 안내</strong>
          <p>{permissionMessage}</p>
        </Card>
      )}

      {refreshMessage && (
        <Card className="rankings-notice-card success">
          <strong>새로고침 완료</strong>
          <p>{refreshMessage}</p>
        </Card>
      )}

      {errorMessage && (
        <Card className="rankings-notice-card danger">
          <strong>조회 실패</strong>
          <p>{errorMessage}</p>
        </Card>
      )}

      <Card className="rankings-list-card">
        <div className="card-heading">
          <div>
            <p className="section-kicker">Ranking</p>
            <h3>게임 순위</h3>
          </div>
        </div>

        {isLoading && <LoadingIndicator label="실시간 게임 순위를 불러오는 중" />}

        {!isLoading && isSoopEmpty && (
          <p className="empty-state">SOOP 데이터는 아직 준비 중입니다.</p>
        )}

        {!isLoading && !isSoopEmpty && rankings.length === 0 && (
          <p className="empty-state">아직 표시할 실시간 게임 순위가 없습니다. 라이브 트렌드 새로고침 후 다시 확인해주세요.</p>
        )}

        {!isLoading && rankings.length > 0 && (
          <div className="ranking-card-list">
            {rankings.map((item, index) => (
              <RankingCard
                ranking={item}
                fallbackRank={index + 1}
                key={`${item.rank || index}-${item.source || item.sources || 'all'}-${item.title}`}
              />
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}

function RankingCard({ ranking, fallbackRank }) {
  const rank = ranking.rank || fallbackRank;
  const title = ranking.title || ranking.displayGameName || ranking.normalizedGameName || '이름 없는 게임';
  const sourceLabel = formatSources(ranking.sources, ranking.source);
  const trendScore = toNumberOrNull(ranking.trendScore);

  return (
    <article className="ranking-card">
      <div className="ranking-card-rank">
        <span>Rank</span>
        <strong>#{rank}</strong>
      </div>

      <div className="ranking-card-main">
        <div className="ranking-card-top">
          <div>
            <div className="live-trend-card-badges">
              <span className="source-pill">{sourceLabel}</span>
              {ranking.dataOrigin === 'REAL' && (
                <span className="source-pill real">실제 수집 데이터</span>
              )}
              {ranking.dataOrigin && ranking.dataOrigin !== 'REAL' && (
                <span className="source-pill muted">{dataOriginLabel(ranking.dataOrigin)}</span>
              )}
              {ranking.signalStatus === 'COMPLETE' && (
                <span className="source-pill complete">수집 완료</span>
              )}
              {ranking.signalStatus === 'PARTIAL' && (
                <span className="source-pill muted">부분 수집</span>
              )}
            </div>
            <h4>{title}</h4>
            <p>{ranking.genre || '장르 정보 없음'}</p>
          </div>
          <div className={`trend-score-emphasis ${getScoreTone(trendScore)}`}>
            <span>trendScore</span>
            <strong>{formatScoreOrDash(trendScore)}</strong>
          </div>
        </div>

        <div className="ranking-metric-grid">
          <MetricBlock label="시청자 수" value={formatViewerCount(ranking.totalViewerCount)} />
          <MetricBlock label="방송 수" value={formatStreamCount(ranking.liveStreamCount)} />
          <MetricBlock label="상태" value={signalStatusLabel(ranking.signalStatus)} />
          <MetricBlock label="데이터" value={dataOriginLabel(ranking.dataOrigin)} />
          <MetricBlock label="업데이트" value={formatDateTime(ranking.updatedAt)} />
        </div>

        {ranking.reason && <p className="trend-reason">{ranking.reason}</p>}
      </div>
    </article>
  );
}

function MetricBlock({ label, value }) {
  return (
    <div className="trend-metric-block">
      <span>{label}</span>
      <strong>{value ?? '-'}</strong>
    </div>
  );
}

function formatSources(sources, source) {
  if (Array.isArray(sources) && sources.length > 0) {
    return sources.join(' · ');
  }
  return source || '-';
}

function toNumberOrNull(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return null;
  }
  return Number(value);
}

function formatNumber(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-';
  }
  return Number(value).toLocaleString('ko-KR');
}

function formatViewerCount(value) {
  const formatted = formatNumber(value);
  return formatted === '-' ? '-' : `${formatted}명`;
}

function formatStreamCount(value) {
  const formatted = formatNumber(value);
  return formatted === '-' ? '-' : `${formatted}개`;
}

function formatScoreOrDash(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-';
  }
  return formatScore(value);
}

function signalStatusLabel(status) {
  const labels = {
    COMPLETE: '수집 완료',
    PARTIAL: '부분 수집',
  };
  return labels[status] || status || '-';
}

function dataOriginLabel(dataOrigin) {
  const labels = {
    REAL: '실제 수집 데이터',
    FALLBACK: 'fallback 데이터',
    PARTIAL: 'partial 데이터',
  };
  return labels[dataOrigin] || dataOrigin || '-';
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

export default RankingsPage;
