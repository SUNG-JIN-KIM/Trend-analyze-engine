import { formatScore, getScoreTone } from '../../utils/score.js';

function GameListTable({ games }) {
  if (!games.length) {
    return <p className="empty-state">아직 등록된 게임이 없습니다.</p>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>제목</th>
            <th>장르</th>
            <th>플랫폼</th>
            <th>플레이</th>
            <th>추천 점수</th>
            <th>추천 근거</th>
          </tr>
        </thead>
        <tbody>
          {games.map((game) => (
            <tr key={game.id}>
              <td>{game.title}</td>
              <td>{game.genre}</td>
              <td>{game.platform}</td>
              <td>{game.playStyle}</td>
              <td>
                <span className={`score-badge ${getScoreTone(game.recommendationScore)}`}>
                  {formatScore(game.recommendationScore)}
                </span>
              </td>
              <td className="reason-cell">{game.reason || '추천 근거가 아직 없습니다.'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default GameListTable;
