import Button from '../components/common/Button.jsx';
import Card from '../components/common/Card.jsx';
import ScoreControl from '../components/game/ScoreControl.jsx';

function ManualGamePage({ dashboard, onGoGames }) {
  const { formData } = dashboard;
  const isValid = formData.title && formData.genre && formData.platform && formData.playStyle;

  const handleSubmit = async () => {
    const succeeded = await dashboard.submitGame();
    if (succeeded) {
      onGoGames?.();
    }
  };

  return (
    <div className="page-stack">
      <Card className="page-intro-card">
        <p className="section-kicker">Manual</p>
        <h2>직접 점수 입력형 게임 등록</h2>
        <p>
          게임 후보의 장르, 플레이 스타일, 인터랙션 적합도, 시장성 점수를 직접 입력해 추천 엔진에 등록합니다.
        </p>
      </Card>

      <Card>
        <div className="card-heading">
          <div>
            <p className="section-kicker">Basic Info</p>
            <h3>게임 기본 정보</h3>
          </div>
          <Button variant="secondary" onClick={dashboard.resetForm}>입력 초기화</Button>
        </div>

        <div className="sample-panel">
          <div>
            <strong>발표용 샘플 데이터</strong>
            <span>샘플을 선택하면 직접 입력 폼이 빠르게 채워집니다.</span>
          </div>
          <div className="sample-actions">
            {dashboard.sampleGames.map((sampleGame) => (
              <button
                className="sample-chip"
                type="button"
                key={sampleGame.title}
                onClick={() => dashboard.applySampleGame(sampleGame)}
              >
                {sampleGame.title}
              </button>
            ))}
          </div>
        </div>

        <div className="form-grid">
          <label>
            <span>제목</span>
            <input
              value={formData.title}
              onChange={(event) => dashboard.updateField('title', event.target.value)}
              placeholder="예: Rhythm Talk Party"
            />
          </label>
          <label>
            <span>장르</span>
            <input
              value={formData.genre}
              onChange={(event) => dashboard.updateField('genre', event.target.value)}
              placeholder="예: Party, Rhythm, Simulation"
            />
          </label>
          <label>
            <span>플랫폼</span>
            <input
              value={formData.platform}
              onChange={(event) => dashboard.updateField('platform', event.target.value)}
              placeholder="예: PC, Mobile, Web"
            />
          </label>
          <label>
            <span>플레이 스타일</span>
            <input
              value={formData.playStyle}
              onChange={(event) => dashboard.updateField('playStyle', event.target.value)}
              placeholder="예: Co-op, Solo, Party"
            />
          </label>
        </div>
      </Card>

      <Card>
        <div className="card-heading">
          <div>
            <p className="section-kicker">Scores</p>
            <h3>추천 점수 입력</h3>
          </div>
        </div>

        <div className="score-grid">
          <ScoreControl
            label="스트리밍 적합도"
            value={formData.streamabilityScore}
            onChange={(value) => dashboard.updateField('streamabilityScore', value)}
          />
          <ScoreControl
            label="Webcam 적합도"
            value={formData.webcamFitScore}
            onChange={(value) => dashboard.updateField('webcamFitScore', value)}
          />
          <ScoreControl
            label="TTS 적합도"
            value={formData.ttsFitScore}
            onChange={(value) => dashboard.updateField('ttsFitScore', value)}
          />
          <ScoreControl
            label="STT 적합도"
            value={formData.sttFitScore}
            onChange={(value) => dashboard.updateField('sttFitScore', value)}
          />
          <ScoreControl
            label="참신성"
            value={formData.noveltyScore}
            onChange={(value) => dashboard.updateField('noveltyScore', value)}
          />
          <ScoreControl
            label="개발 가능성"
            value={formData.devFeasibilityScore}
            onChange={(value) => dashboard.updateField('devFeasibilityScore', value)}
          />
          <ScoreControl
            label="마켓 시그널"
            value={formData.marketSignalScore}
            onChange={(value) => dashboard.updateField('marketSignalScore', value)}
          />
        </div>

        <label className="reason-field">
          <span>추천 근거</span>
          <textarea
            value={formData.reason}
            onChange={(event) => dashboard.updateField('reason', event.target.value)}
            placeholder="예: 시청자 참여형 미션과 음성 명령이 자연스럽게 결합될 수 있습니다."
            rows="5"
          />
        </label>

        <div className="step-actions">
          <Button variant="secondary" onClick={dashboard.resetForm}>입력 초기화</Button>
          <Button onClick={handleSubmit} disabled={!isValid || dashboard.isCreatingGame}>
            {dashboard.isCreatingGame ? '등록 중' : '게임 등록'}
          </Button>
        </div>
      </Card>
    </div>
  );
}

export default ManualGamePage;
