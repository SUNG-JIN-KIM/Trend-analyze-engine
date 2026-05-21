import { formatScore, getScoreTone } from '../../utils/score.js';

function RecommendationCards({ recommendations }) {
  if (!recommendations.length) {
    return <p className="empty-state">추천 순위를 조회하면 이곳에 표시됩니다.</p>;
  }

  return (
    <div className="recommendation-list">
      {recommendations.map((game) => (
        <article className="recommendation-card" key={`${game.rank}-${game.id}`}>
          <div className="rank-pill">#{game.rank}</div>
          <div>
            <h4>{game.title}</h4>
            <p>{game.genre} · {game.platform} · {game.playStyle}</p>
            <span>{game.summaryReason || '추천 근거가 아직 없습니다.'}</span>
          </div>
          <strong className={`score-badge ${getScoreTone(game.recommendationScore)}`}>
            {formatScore(game.recommendationScore)}
          </strong>
        </article>
      ))}
    </div>
  );
}

export default RecommendationCards;
