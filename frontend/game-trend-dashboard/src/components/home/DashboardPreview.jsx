import { formatScore, getScoreTone } from '../../utils/score.js';

function DashboardPreview({ rankings = [], topGames = [], isLoadingRankings = false, onNavigate }) {
  const youtubePreview = topGames.slice(0, 3);
  const rankingPreview = rankings.slice(0, 5);

  return (
    <section className="home-preview-layout">
      <article className="home-preview-panel live">
        <div className="home-preview-heading">
          <div>
            <p className="section-kicker">Live Preview</p>
            <h2>실시간 순위</h2>
          </div>
          <button type="button" onClick={() => onNavigate?.('/rankings')}>
            전체 보기
          </button>
        </div>

        {isLoadingRankings && <p className="home-muted">순위를 불러오는 중입니다.</p>}
        {!isLoadingRankings && rankingPreview.length === 0 && (
          <p className="home-muted">아직 표시할 실시간 순위가 없습니다.</p>
        )}

        <div className="home-ranking-preview-list v2">
          {rankingPreview.map((game, index) => (
            <article className="home-ranking-preview-item v2" key={`${game.rank || index}-${game.title}`}>
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
      </article>

      <article className="home-preview-panel youtube">
        <div className="home-preview-heading">
          <div>
            <p className="section-kicker">YouTube Signal</p>
            <h2>관심도 TOP 게임</h2>
          </div>
          <button type="button" onClick={() => onNavigate?.('/youtube-trends')}>
            분석 보기
          </button>
        </div>

        {youtubePreview.length === 0 && <p className="home-muted">관리자 페이지에서 데이터를 수집하면 표시됩니다.</p>}
        <div className="home-youtube-preview-list">
          {youtubePreview.map((game, index) => (
            <article className="home-youtube-preview-item" key={game.keyword || index}>
              <span>{index + 1}</span>
              <div>
                <strong>{game.gameTitle || game.keyword}</strong>
                <small>영상 {formatCount(game.videoCount)}개 · 조회수 {formatCount(game.totalViewCount)}</small>
              </div>
              <b>{Number(game.youtubeInterestScore || 0).toFixed(1)}</b>
            </article>
          ))}
        </div>
      </article>
    </section>
  );
}

function formatCount(value) {
  return new Intl.NumberFormat('ko-KR').format(Number(value) || 0);
}

export default DashboardPreview;
