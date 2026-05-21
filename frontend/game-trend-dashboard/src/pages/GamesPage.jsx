import { useEffect } from 'react';
import ResultScreen from '../components/onboarding/ResultScreen.jsx';

function GamesPage({ dashboard, onAddGame }) {
  useEffect(() => {
    Promise.all([dashboard.loadGames(), dashboard.loadRecommendations()]);
  }, [dashboard.loadGames, dashboard.loadRecommendations]);

  return (
    <ResultScreen
      games={dashboard.games}
      recommendations={dashboard.recommendations}
      report={dashboard.report}
      onRefreshGames={dashboard.loadGames}
      onRefreshRecommendations={dashboard.loadRecommendations}
      onRefreshResults={dashboard.refreshResults}
      onCreateReport={dashboard.createReport}
      onAddAnother={onAddGame}
      isLoadingGames={dashboard.isLoadingGames}
      isLoadingRecommendations={dashboard.isLoadingRecommendations}
      isGeneratingReport={dashboard.isGeneratingReport}
    />
  );
}

export default GamesPage;
