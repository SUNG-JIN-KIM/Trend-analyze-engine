import Button from '../common/Button.jsx';
import Card from '../common/Card.jsx';

function StartScreen({ onStart }) {
  return (
    <Card className="start-card">
      <div>
        <p className="section-kicker">온보딩 시작</p>
        <h2>새 게임 후보를 빠르게 분석해보세요</h2>
        <p>
          기본 정보와 점수를 단계별로 입력하면 백엔드가 추천 점수를 계산하고, 등록된 후보 기반 리포트를 생성합니다.
        </p>
      </div>
      <div className="feature-grid">
        <div>
          <strong>점수 기반 추천</strong>
          <span>스트리밍, 인터랙션, 시장성을 종합합니다.</span>
        </div>
        <div>
          <strong>로컬 LLM 리포트</strong>
          <span>GEMMA4 E2B 호출은 백엔드에서만 처리합니다.</span>
        </div>
        <div>
          <strong>안정적인 fallback</strong>
          <span>LLM 실패 시에도 정적 초안을 표시합니다.</span>
        </div>
      </div>
      <Button onClick={onStart}>분석 시작</Button>
    </Card>
  );
}

export default StartScreen;
