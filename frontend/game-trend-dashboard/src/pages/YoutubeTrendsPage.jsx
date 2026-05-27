import { useEffect, useMemo, useState } from 'react';
import { getYoutubeTopGames, getYoutubeTrend } from '../api/gameTrendApi.js';

function YoutubeTrendsPage({ keyword }) {
  const [topGames, setTopGames] = useState([]);
  const [trend, setTrend] = useState(null);
  const [searchKeyword, setSearchKeyword] = useState(keyword || '');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const activeKeyword = useMemo(() => keyword || searchKeyword, [keyword, searchKeyword]);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      setIsLoading(true);
      setErrorMessage('');
      try {
        const topData = await getYoutubeTopGames(10);
        if (!cancelled) {
          setTopGames(topData);
        }
        if (activeKeyword && activeKeyword.trim()) {
          const trendData = await getYoutubeTrend(activeKeyword.trim());
          if (!cancelled) {
            setTrend(trendData);
          }
        } else if (!cancelled) {
          setTrend(null);
        }
      } catch (error) {
        if (!cancelled) {
          setErrorMessage(error.message || 'YouTube 트렌드를 불러오지 못했습니다.');
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    };
    run();
    return () => {
      cancelled = true;
    };
  }, [activeKeyword]);

  const handleSubmit = (event) => {
    event.preventDefault();
    const nextKeyword = searchKeyword.trim();
    if (nextKeyword) {
      window.history.pushState({}, '', `/youtube-trends/${encodeURIComponent(nextKeyword)}`);
      window.dispatchEvent(new Event('popstate'));
    }
  };

  return (
    <main className="youtube-trends-page">
      <section className="youtube-trends-header">
        <div>
          <p>YouTube Trend</p>
          <h1>게임별 YouTube 관심도</h1>
        </div>
        <form className="youtube-search-form" onSubmit={handleSubmit}>
          <input
            value={searchKeyword}
            onChange={(event) => setSearchKeyword(event.target.value)}
            placeholder="게임 키워드 검색"
          />
          <button type="submit">조회</button>
        </form>
      </section>

      {errorMessage && <p className="admin-auth-error">{errorMessage}</p>}
      {isLoading && <p className="admin-loading">YouTube 트렌드를 불러오는 중입니다.</p>}

      {trend?.score && (
        <>
          <section className="youtube-summary-grid">
            <Metric label="게임 키워드" value={trend.score.keyword} />
            <Metric label="영상 수" value={formatCount(trend.score.videoCount)} />
            <Metric label="총 조회수" value={formatCount(trend.score.totalViewCount)} />
            <Metric label="총 좋아요" value={formatCount(trend.score.totalLikeCount)} />
            <Metric label="총 댓글" value={formatCount(trend.score.totalCommentCount)} />
            <Metric label="평균 반응률" value={`${((Number(trend.score.averageEngagementRate) || 0) * 100).toFixed(2)}%`} />
            <Metric label="관심도 점수" value={`${Number(trend.score.youtubeInterestScore || 0).toFixed(1)}점`} />
            <Metric label="최근 수집일" value={formatDate(trend.score.collectedAt)} />
          </section>

          <section className="youtube-reaction-panel">
            <div>
              <p>Comment Reaction</p>
              <h2>{trend.commentReactionSummary?.summary || '아직 댓글 반응 요약이 없습니다.'}</h2>
            </div>
            <div className="youtube-summary-grid">
              <Metric label="긍정 언급" value={formatCount(trend.commentReactionSummary?.positiveMentionCount)} />
              <Metric label="부정 언급" value={formatCount(trend.commentReactionSummary?.negativeMentionCount)} />
            </div>
            <KeywordList title="긍정 키워드" items={trend.commentReactionSummary?.topPositiveKeywords || []} />
            <KeywordList title="부정 키워드" items={trend.commentReactionSummary?.topNegativeKeywords || []} />
          </section>
        </>
      )}

      <section className="youtube-ranking-section">
        <div className="admin-panel-heading">
          <h2>YouTube 관심도 TOP 게임</h2>
        </div>
        <div className="youtube-ranking-grid">
          {topGames.map((game, index) => (
            <article className="youtube-ranking-card" key={game.keyword}>
              <span>#{index + 1}</span>
              <strong>{game.gameTitle || game.keyword}</strong>
              <p>{Number(game.youtubeInterestScore || 0).toFixed(1)}점</p>
              <small>
                영상 {formatCount(game.videoCount)}개 · 조회수 {formatCount(game.totalViewCount)}
              </small>
            </article>
          ))}
        </div>
        {topGames.length === 0 && (
          <p className="admin-empty">저장된 YouTube 트렌드 분석 결과가 없습니다. 관리자 페이지에서 먼저 YouTube 데이터를 수집해주세요.</p>
        )}
      </section>
    </main>
  );
}

function Metric({ label, value }) {
  return (
    <article className="admin-stat-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function KeywordList({ title, items }) {
  return (
    <div className="youtube-keyword-list">
      <strong>{title}</strong>
      <div>
        {items.length === 0 && <span>분석 결과 없음</span>}
        {items.map((item) => (
          <span key={`${item.sentiment}-${item.statKeyword}`}>
            {item.statKeyword} {formatCount(item.mentionCount)}
          </span>
        ))}
      </div>
    </div>
  );
}

function formatCount(value) {
  return new Intl.NumberFormat('ko-KR').format(Number(value) || 0);
}

function formatDate(value) {
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

export default YoutubeTrendsPage;
