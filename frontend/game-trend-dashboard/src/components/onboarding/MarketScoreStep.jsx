import Button from '../common/Button.jsx';
import Card from '../common/Card.jsx';
import ScoreControl from '../game/ScoreControl.jsx';

function MarketScoreStep({
  formData,
  onChange,
  onSubmit,
  onImportFromSteam,
  onResetForm,
  onBack,
  isSubmitting,
  isImportingSteamGame,
}) {
  const canImportFromSteam = Boolean(
    formData.steamAppId && formData.title && formData.genre && formData.platform && formData.playStyle,
  );

  return (
    <Card>
      <p className="section-kicker">Step 3</p>
      <h2>시장성 / 개발성 점수</h2>
      <p className="section-description">게임 아이디어의 새로움, 구현 가능성, 시장 신호를 함께 입력합니다.</p>

      <div className="score-grid">
        <ScoreControl
          label="참신성"
          value={formData.noveltyScore}
          onChange={(value) => onChange('noveltyScore', value)}
        />
        <ScoreControl
          label="개발 가능성"
          value={formData.devFeasibilityScore}
          onChange={(value) => onChange('devFeasibilityScore', value)}
        />
        <ScoreControl
          label="마켓 시그널"
          value={formData.marketSignalScore}
          onChange={(value) => onChange('marketSignalScore', value)}
        />
      </div>

      <label className="reason-field">
        <span>추천 근거</span>
        <textarea
          value={formData.reason}
          onChange={(event) => onChange('reason', event.target.value)}
          placeholder="예: 시청자 참여형 미션과 음성 명령이 자연스럽게 결합될 수 있습니다."
          rows="5"
        />
      </label>

      <div className="step-actions">
        <Button variant="secondary" onClick={onBack}>이전</Button>
        <Button variant="secondary" onClick={onResetForm}>입력 초기화</Button>
        <Button
          variant="secondary"
          onClick={onImportFromSteam}
          disabled={isImportingSteamGame || !canImportFromSteam}
        >
          {isImportingSteamGame ? 'Steam Import 중' : 'Steam 데이터로 등록'}
        </Button>
        <Button onClick={onSubmit} disabled={isSubmitting}>
          {isSubmitting ? '게임 등록 중' : '게임 등록하고 분석 보기'}
        </Button>
      </div>
    </Card>
  );
}

export default MarketScoreStep;
