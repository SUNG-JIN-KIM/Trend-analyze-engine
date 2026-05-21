import { useState } from 'react';
import Button from '../common/Button.jsx';
import Card from '../common/Card.jsx';
import LoadingIndicator from '../common/LoadingIndicator.jsx';

function ReportPanel({ report, onCreateReport, isGeneratingReport }) {
  const [copyStatus, setCopyStatus] = useState('');

  const handleCopy = async () => {
    if (!report?.draft) {
      return;
    }
    try {
      await navigator.clipboard.writeText(report.draft);
      setCopyStatus('리포트를 클립보드에 복사했습니다.');
    } catch {
      setCopyStatus('브라우저에서 복사를 허용하지 않았습니다.');
    }
  };

  return (
    <Card className="report-panel">
      <div className="card-heading">
        <div>
          <p className="section-kicker">GEMMA4 E2B</p>
          <h3>리포트 초안</h3>
        </div>
        <div className="report-actions">
          {report && (
            <Button variant="secondary" onClick={handleCopy}>
              복사
            </Button>
          )}
          <Button onClick={onCreateReport} disabled={isGeneratingReport}>
            {isGeneratingReport ? '리포트 생성 중' : '리포트 생성하기'}
          </Button>
        </div>
      </div>

      {isGeneratingReport && <LoadingIndicator label="GEMMA4 E2B 리포트 생성 중" />}
      {copyStatus && <p className="copy-status">{copyStatus}</p>}

      {!report && !isGeneratingReport && (
        <p className="empty-state">리포트 생성 버튼을 누르면 분석 초안이 표시됩니다.</p>
      )}

      {report && (
        <pre className="report-draft">{report.draft}</pre>
      )}
    </Card>
  );
}

export default ReportPanel;
