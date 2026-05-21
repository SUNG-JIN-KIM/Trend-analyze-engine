import { useEffect } from 'react';
import Card from '../components/common/Card.jsx';
import { OnboardingHistorySection } from '../components/onboarding/NaturalOnboardingScreen.jsx';

function HistoryPage({ dashboard }) {
  useEffect(() => {
    dashboard.loadOnboardingHistories();
  }, [dashboard.loadOnboardingHistories]);

  return (
    <div className="page-stack">
      <Card className="page-intro-card">
        <p className="section-kicker">History</p>
        <h2>분석 이력</h2>
        <p>
          자연어 질문으로 실행한 게임 트렌드 분석 결과를 다시 열어보고, 필요 없는 이력은 삭제할 수 있습니다.
        </p>
      </Card>

      <OnboardingHistorySection
        histories={dashboard.onboardingHistories}
        selectedHistory={dashboard.selectedOnboardingHistory}
        onRefresh={dashboard.loadOnboardingHistories}
        onSelect={dashboard.selectOnboardingHistory}
        onDelete={dashboard.removeOnboardingHistory}
        isLoadingHistories={dashboard.isLoadingOnboardingHistories}
        isLoadingHistoryDetail={dashboard.isLoadingOnboardingHistoryDetail}
        isDeletingHistory={dashboard.isDeletingOnboardingHistory}
      />
    </div>
  );
}

export default HistoryPage;
