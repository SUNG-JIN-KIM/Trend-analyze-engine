import Button from '../common/Button.jsx';
import Card from '../common/Card.jsx';
import LoadingIndicator from '../common/LoadingIndicator.jsx';
import { formatScore } from '../../utils/score.js';

function BasicInfoStep({
  formData,
  sampleGames,
  steamReview,
  onChange,
  onApplySample,
  onResetForm,
  onLoadSteamReview,
  onNext,
  onBack,
  isLoadingSteamReview,
}) {
  const isValid = formData.title && formData.genre && formData.platform && formData.playStyle;

  return (
    <Card>
      <p className="section-kicker">Step 1</p>
      <h2>게임 기본 정보</h2>
      <p className="section-description">추천 점수와 리포트에 사용할 게임 후보의 기본 정보를 입력합니다.</p>

      <div className="sample-panel">
        <div>
          <strong>발표용 샘플 데이터</strong>
          <span>버튼 하나로 전체 입력값을 채워 빠르게 시연할 수 있습니다.</span>
        </div>
        <div className="sample-actions">
          {sampleGames.map((sampleGame) => (
            <button
              className="sample-chip"
              type="button"
              key={sampleGame.title}
              onClick={() => onApplySample(sampleGame)}
            >
              {sampleGame.title}
            </button>
          ))}
        </div>
      </div>

      <div className="form-grid">
        <label>
          <span>Steam App ID</span>
          <input
            value={formData.steamAppId}
            onChange={(event) => onChange('steamAppId', event.target.value)}
            placeholder="예: 620"
            inputMode="numeric"
          />
        </label>
        <label>
          <span>제목</span>
          <input
            value={formData.title}
            onChange={(event) => onChange('title', event.target.value)}
            placeholder="예: Rhythm Talk Party"
          />
        </label>
        <label>
          <span>장르</span>
          <input
            value={formData.genre}
            onChange={(event) => onChange('genre', event.target.value)}
            placeholder="예: Party, Rhythm, Simulation"
          />
        </label>
        <label>
          <span>플랫폼</span>
          <input
            value={formData.platform}
            onChange={(event) => onChange('platform', event.target.value)}
            placeholder="예: PC, Mobile, Web"
          />
        </label>
        <label>
          <span>플레이 스타일</span>
          <input
            value={formData.playStyle}
            onChange={(event) => onChange('playStyle', event.target.value)}
            placeholder="예: Co-op, Solo, Party"
          />
        </label>
      </div>

      <div className="steam-import-panel">
        <div>
          <p className="section-kicker">Steam Import</p>
          <h3>Steam 리뷰 데이터로 시장 신호 확인</h3>
          <p>
            App ID를 입력하고 리뷰 요약을 조회하면 import 시 계산될 marketSignalScore를 미리 볼 수 있습니다.
          </p>
        </div>
        <div className="steam-panel-actions">
          <Button
            variant="secondary"
            onClick={onLoadSteamReview}
            disabled={isLoadingSteamReview || !formData.steamAppId}
          >
            {isLoadingSteamReview ? 'Steam 조회 중' : 'Steam 리뷰 요약 조회'}
          </Button>
        </div>
        {isLoadingSteamReview && <LoadingIndicator label="Steam 리뷰 데이터를 불러오는 중" />}
        {steamReview && (
          <div className="steam-review-summary">
            <div>
              <span>리뷰 요약</span>
              <strong>{steamReview.reviewScoreDesc}</strong>
            </div>
            <div>
              <span>총 리뷰</span>
              <strong>{steamReview.totalReviews.toLocaleString()}</strong>
            </div>
            <div>
              <span>긍정 비율</span>
              <strong>{formatScore(steamReview.positiveRate * 100)}%</strong>
            </div>
            <div>
              <span>시장 신호</span>
              <strong>{steamReview.marketSignalScore}</strong>
            </div>
          </div>
        )}
      </div>

      <div className="step-actions">
        <Button variant="secondary" onClick={onBack}>이전</Button>
        <Button variant="secondary" onClick={onResetForm}>입력 초기화</Button>
        <Button onClick={onNext} disabled={!isValid}>다음</Button>
      </div>
    </Card>
  );
}

export default BasicInfoStep;
