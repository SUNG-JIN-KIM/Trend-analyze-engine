function StatsSection({ rankings = [], topGames = [], conversationCount = null, isLoading = false }) {
  const topYoutubeGame = topGames[0];
  const topLiveGame = rankings[0];
  const youtubeVideoCount = topGames.reduce((sum, game) => sum + (Number(game.videoCount) || 0), 0);

  const stats = [
    {
      label: '수집 게임 수',
      value: formatCount(rankings.length),
      detail: topLiveGame?.title ? `현재 1위 ${topLiveGame.title}` : '실시간 데이터 대기 중',
    },
    {
      label: 'YouTube 분석 수',
      value: formatCount(youtubeVideoCount),
      detail: topYoutubeGame ? `${topYoutubeGame.gameTitle || topYoutubeGame.keyword} 관심도 상위` : '수집 후 표시됩니다',
    },
    {
      label: '최근 대화 수',
      value: conversationCount === null ? '-' : formatCount(conversationCount),
      detail: conversationCount === null ? '로그인 후 확인 가능' : '저장된 분석 대화',
    },
    {
      label: '관심도 TOP 게임',
      value: topYoutubeGame ? Number(topYoutubeGame.youtubeInterestScore || 0).toFixed(1) : '-',
      detail: topYoutubeGame ? topYoutubeGame.gameTitle || topYoutubeGame.keyword : 'YouTube 데이터 없음',
    },
  ];

  return (
    <section className="home-section home-stats-section">
      <div className="home-section-heading">
        <p className="section-kicker">Dashboard Snapshot</p>
        <h2>현재 데이터 흐름</h2>
      </div>
      <div className="home-stats-grid" aria-busy={isLoading}>
        {stats.map((stat) => (
          <article className="home-stat-card" key={stat.label}>
            <span>{stat.label}</span>
            <strong>{isLoading ? '...' : stat.value}</strong>
            <small>{stat.detail}</small>
          </article>
        ))}
      </div>
    </section>
  );
}

function formatCount(value) {
  return new Intl.NumberFormat('ko-KR').format(Number(value) || 0);
}

export default StatsSection;
