import Button from '../common/Button.jsx';
import Card from '../common/Card.jsx';
import LoadingIndicator from '../common/LoadingIndicator.jsx';
import GameListTable from '../game/GameListTable.jsx';
import RecommendationCards from '../game/RecommendationCards.jsx';
import ReportPanel from '../report/ReportPanel.jsx';

function ResultScreen({
  games,
  recommendations,
  report,
  onRefreshGames,
  onRefreshRecommendations,
  onRefreshResults,
  onCreateReport,
  onAddAnother,
  isLoadingGames,
  isLoadingRecommendations,
  isGeneratingReport,
}) {
  const topRecommendation = recommendations[0];

  return (
    <div className="result-layout">
      <Card className="result-hero">
        <div>
          <p className="section-kicker">Games</p>
          <h2>게임 목록과 추천 순위</h2>
          <p>등록된 게임 후보와 추천 순위를 확인하고 GEMMA4 E2B 리포트를 생성합니다.</p>
        </div>
        <div className="result-actions">
          <Button variant="secondary" onClick={onAddAnother}>다른 게임 추가</Button>
          <Button variant="secondary" onClick={onRefreshResults} disabled={isLoadingGames || isLoadingRecommendations}>
            결과 새로고침
          </Button>
          <Button onClick={onRefreshRecommendations} disabled={isLoadingRecommendations}>추천 순위 조회</Button>
        </div>
      </Card>

      <div className="summary-grid">
        <div className="summary-card">
          <span>등록 게임</span>
          <strong>{games.length}</strong>
        </div>
        <div className="summary-card">
          <span>추천 후보</span>
          <strong>{recommendations.length}</strong>
        </div>
        <div className="summary-card highlight">
          <span>최고 추천</span>
          <strong>{topRecommendation ? topRecommendation.title : '-'}</strong>
        </div>
      </div>

      <div className="result-grid">
        <Card>
          <div className="card-heading">
            <div>
              <p className="section-kicker">Games</p>
              <h3>등록된 게임 목록</h3>
            </div>
            {isLoadingGames && <LoadingIndicator label="목록 불러오는 중" />}
          </div>
          <div className="inline-actions">
            <Button variant="secondary" onClick={onRefreshGames} disabled={isLoadingGames}>게임 목록 조회</Button>
          </div>
          <GameListTable games={games} />
        </Card>

        <Card>
          <div className="card-heading">
            <div>
              <p className="section-kicker">Ranking</p>
              <h3>추천 순위</h3>
            </div>
            {isLoadingRecommendations && <LoadingIndicator label="추천 순위 계산 중" />}
          </div>
          <RecommendationCards recommendations={recommendations} />
        </Card>
      </div>

      <ReportPanel
        report={report}
        onCreateReport={onCreateReport}
        isGeneratingReport={isGeneratingReport}
      />
    </div>
  );
}

export default ResultScreen;
