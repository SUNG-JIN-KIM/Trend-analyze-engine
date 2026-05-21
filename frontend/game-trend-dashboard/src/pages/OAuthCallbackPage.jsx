import { useEffect, useMemo, useState } from 'react';
import Button from '../components/common/Button.jsx';
import Card from '../components/common/Card.jsx';

function OAuthCallbackPage({ onOAuthToken, onGoHome, onGoLogin }) {
  const query = useMemo(() => new URLSearchParams(window.location.search), []);
  const token = query.get('token');
  const error = query.get('error');
  const errorMessage = query.get('message');
  const [status, setStatus] = useState(error ? 'error' : 'loading');
  const [message, setMessage] = useState(
    errorMessage || (error ? '소셜 로그인에 실패했습니다.' : '소셜 로그인 정보를 확인하고 있습니다.')
  );

  useEffect(() => {
    if (error) {
      setStatus('error');
      setMessage(errorMessage || '소셜 로그인에 실패했습니다.');
      return;
    }

    if (!token) {
      setStatus('error');
      setMessage('OAuth 콜백에 accessToken이 없습니다. 다시 로그인해주세요.');
      return;
    }

    let cancelled = false;
    onOAuthToken(token)
      .then(() => {
        if (cancelled) {
          return;
        }
        setStatus('success');
        setMessage('소셜 로그인이 완료되었습니다. Agent 화면으로 이동합니다.');
        window.setTimeout(() => {
          if (!cancelled) {
            onGoHome();
          }
        }, 450);
      })
      .catch((caughtError) => {
        if (cancelled) {
          return;
        }
        setStatus('error');
        setMessage(caughtError.message || '사용자 정보를 가져오지 못했습니다. 다시 로그인해주세요.');
      });

    return () => {
      cancelled = true;
    };
  }, [error, errorMessage, onGoHome, onOAuthToken, token]);

  return (
    <section className="auth-page">
      <Card className={`auth-card oauth-callback-card ${status}`}>
        <div className="auth-heading">
          <p className="section-kicker">OAuth Callback</p>
          <h2>{status === 'error' ? '소셜 로그인 실패' : '소셜 로그인 처리 중'}</h2>
          <p>{message}</p>
        </div>

        {status === 'loading' && (
          <div className="oauth-status-row">
            <span className="loading-dot" />
            <strong>토큰 저장 및 사용자 정보 조회 중</strong>
          </div>
        )}

        {status === 'success' && (
          <div className="oauth-status-row success">
            <strong>로그인 완료</strong>
          </div>
        )}

        {status === 'error' && (
          <div className="auth-actions">
            <Button onClick={onGoLogin}>로그인으로 돌아가기</Button>
            <Button variant="secondary" onClick={onGoHome}>Agent로 이동</Button>
          </div>
        )}
      </Card>
    </section>
  );
}

export default OAuthCallbackPage;
