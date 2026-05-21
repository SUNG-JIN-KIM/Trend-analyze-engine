import Button from '../common/Button.jsx';
import Card from '../common/Card.jsx';
import ScoreControl from '../game/ScoreControl.jsx';

function InteractionScoreStep({ formData, onChange, onNext, onBack }) {
  return (
    <Card>
      <p className="section-kicker">Step 2</p>
      <h2>인터랙션 점수</h2>
      <p className="section-description">방송, 웹캠, 음성 합성, 음성 인식과의 결합 가능성을 평가합니다.</p>

      <div className="score-grid">
        <ScoreControl
          label="스트리밍 적합도"
          value={formData.streamabilityScore}
          onChange={(value) => onChange('streamabilityScore', value)}
        />
        <ScoreControl
          label="Webcam 적합도"
          value={formData.webcamFitScore}
          onChange={(value) => onChange('webcamFitScore', value)}
        />
        <ScoreControl
          label="TTS 적합도"
          value={formData.ttsFitScore}
          onChange={(value) => onChange('ttsFitScore', value)}
        />
        <ScoreControl
          label="STT 적합도"
          value={formData.sttFitScore}
          onChange={(value) => onChange('sttFitScore', value)}
        />
      </div>

      <div className="step-actions">
        <Button variant="secondary" onClick={onBack}>이전</Button>
        <Button onClick={onNext}>다음</Button>
      </div>
    </Card>
  );
}

export default InteractionScoreStep;
