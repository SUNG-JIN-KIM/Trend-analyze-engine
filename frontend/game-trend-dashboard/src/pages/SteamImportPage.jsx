import Button from '../components/common/Button.jsx';
import Card from '../components/common/Card.jsx';
import LoadingIndicator from '../components/common/LoadingIndicator.jsx';
import ScoreControl from '../components/game/ScoreControl.jsx';
import { formatScore } from '../utils/score.js';

function SteamImportPage({ dashboard, onGoGames }) {
  const { formData, steamReview } = dashboard;
  const canImport = Boolean(
    formData.steamAppId && formData.title && formData.genre && formData.platform && formData.playStyle,
  );

  const handleImport = async () => {
    const succeeded = await dashboard.importFromSteam();
    if (succeeded) {
      onGoGames?.();
    }
  };

  return (
    <div className="page-stack">
      <Card className="page-intro-card">
        <p className="section-kicker">Steam</p>
        <h2>Steam 리뷰 기반 Import</h2>
        <p>
          Steam App ID로 리뷰 요약과 시장 신호를 확인하고, 해당 데이터를 반영해 게임 후보를 등록합니다.
        </p>
      </Card>

      <Card>
        <div className="card-heading">
          <div>
            <p className="section-kicker">Review Signal</p>
            <h3>Steam 리뷰 요약 조회</h3>
          </div>
          {dashboard.isLoadingSteamReview && <LoadingIndicator label="Steam 리뷰 조회 중" />}
        </div>

        <div className="steam-query-row">
          <label>
            <span>Steam App ID</span>
            <input
              value={formData.steamAppId}
              onChange={(event) => dashboard.updateField('steamAppId', event.target.value)}
              placeholder="예: 620"
              inputMode="numeric"
            />
          </label>
          <Button
            variant="secondary"
            onClick={dashboard.loadSteamReview}
            disabled={dashboard.isLoadingSteamReview || !formData.steamAppId}
          >
            {dashboard.isLoadingSteamReview ? '조회 중' : '리뷰 요약 조회'}
          </Button>
        </div>

        {steamReview && (
          <div className="steam-review-summary page-review-summary">
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
      </Card>

      <Card>
        <div className="card-heading">
          <div>
            <p className="section-kicker">Import Form</p>
            <h3>Steam 데이터 기반 게임 등록</h3>
          </div>
          <Button variant="secondary" onClick={dashboard.resetForm}>입력 초기화</Button>
        </div>

        <div className="form-grid">
          <label>
            <span>제목</span>
            <input
              value={formData.title}
              onChange={(event) => dashboard.updateField('title', event.target.value)}
              placeholder="예: Portal 2"
            />
          </label>
          <label>
            <span>장르</span>
            <input
              value={formData.genre}
              onChange={(event) => dashboard.updateField('genre', event.target.value)}
              placeholder="예: Puzzle Adventure"
            />
          </label>
          <label>
            <span>플랫폼</span>
            <input
              value={formData.platform}
              onChange={(event) => dashboard.updateField('platform', event.target.value)}
              placeholder="예: PC"
            />
          </label>
          <label>
            <span>플레이 스타일</span>
            <input
              value={formData.playStyle}
              onChange={(event) => dashboard.updateField('playStyle', event.target.value)}
              placeholder="예: Solo Puzzle"
            />
          </label>
        </div>

        <div className="score-grid compact-score-grid">
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
        </div>

        <label className="reason-field">
          <span>추천 근거</span>
          <textarea
            value={formData.reason}
            onChange={(event) => dashboard.updateField('reason', event.target.value)}
            placeholder="Steam 리뷰 신호와 게임 컨셉을 바탕으로 등록 근거를 적어주세요."
            rows="5"
          />
        </label>

        <div className="step-actions">
          <Button variant="secondary" onClick={dashboard.resetForm}>입력 초기화</Button>
          <Button onClick={handleImport} disabled={!canImport || dashboard.isImportingSteamGame}>
            {dashboard.isImportingSteamGame ? 'Import 중' : 'Steam 데이터로 등록'}
          </Button>
        </div>
      </Card>
    </div>
  );
}

export default SteamImportPage;
