const featureItems = [
  {
    key: 'agent',
    label: 'Agent',
    title: '질문형 게임 트렌드 분석',
    description: '자연어로 물으면 라이브 반응, YouTube 지표, 개발 관점까지 맥락 있게 정리합니다.',
    accent: 'blue',
  },
  {
    key: 'ranking',
    label: 'Rank',
    title: '실시간 순위 미리보기',
    description: '플랫폼별 인기 게임을 빠르게 비교하고 상승 흐름이 있는 타이틀을 확인합니다.',
    accent: 'mint',
  },
  {
    key: 'youtube',
    label: 'Tube',
    title: 'YouTube 관심도 추적',
    description: '조회수, 좋아요, 댓글 수를 기반으로 게임별 영상 반응을 수치화합니다.',
    accent: 'purple',
  },
  {
    key: 'history',
    label: 'Log',
    title: '대화 기록 관리',
    description: '이전 분석을 다시 열어보고, 이어지는 질문으로 아이디어를 구체화합니다.',
    accent: 'lime',
  },
];

function FeatureCards({ onNavigate }) {
  const routes = {
    agent: '/agent',
    ranking: '/rankings',
    youtube: '/youtube-trends',
    history: '/history',
  };

  return (
    <section className="home-section">
      <div className="home-section-heading">
        <p className="section-kicker">Core Workflow</p>
        <h2>트렌드 발견부터 아이디어 정리까지</h2>
      </div>
      <div className="home-feature-grid">
        {featureItems.map((item) => (
          <button
            className={`home-feature-card ${item.accent}`}
            type="button"
            key={item.key}
            onClick={() => onNavigate?.(routes[item.key])}
          >
            <span className="home-feature-icon">{item.label}</span>
            <strong>{item.title}</strong>
            <small>{item.description}</small>
          </button>
        ))}
      </div>
    </section>
  );
}

export default FeatureCards;
