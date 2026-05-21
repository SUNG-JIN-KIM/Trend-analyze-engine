import { useState } from 'react';
import Button from '../components/common/Button.jsx';
import Card from '../components/common/Card.jsx';
import { getOAuthAuthorizationUrl } from '../api/gameTrendApi.js';

const initialForm = {
  email: '',
  password: '',
};

function LoginPage({ onLogin, onGoRegister, isAuthLoading }) {
  const [form, setForm] = useState(initialForm);
  const [errorMessage, setErrorMessage] = useState('');

  const updateField = (field, value) => {
    setErrorMessage('');
    setForm((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setErrorMessage('');
    try {
      await onLogin(form);
      setForm(initialForm);
    } catch (error) {
      setErrorMessage(error.message || '로그인에 실패했습니다.');
    }
  };

  const startSocialLogin = (provider) => {
    window.location.href = getOAuthAuthorizationUrl(provider);
  };

  return (
    <section className="auth-page">
      <Card className="auth-card">
        <div className="auth-heading">
          <p className="section-kicker">Login</p>
          <h2>로그인</h2>
          <p>개발자 분석, 과거 게임 재해석, 웹캠/TTS/STT 아이디어 분석을 이어서 사용할 수 있습니다.</p>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          <label>
            <span>이메일</span>
            <input
              type="email"
              value={form.email}
              onChange={(event) => updateField('email', event.target.value)}
              autoComplete="email"
              required
            />
          </label>

          <label>
            <span>비밀번호</span>
            <input
              type="password"
              value={form.password}
              onChange={(event) => updateField('password', event.target.value)}
              autoComplete="current-password"
              required
            />
          </label>

          {errorMessage && <p className="auth-error">{errorMessage}</p>}

          <div className="auth-actions">
            <Button type="submit" disabled={isAuthLoading}>
              {isAuthLoading ? '로그인 중' : '로그인'}
            </Button>
            <Button variant="secondary" onClick={onGoRegister} disabled={isAuthLoading}>
              회원가입하기
            </Button>
          </div>
        </form>

        <div className="social-login-section">
          <div className="auth-divider">
            <span>또는</span>
          </div>
          <div className="social-login-buttons">
            <button
              className="social-login-button google"
              type="button"
              disabled={isAuthLoading}
              onClick={() => startSocialLogin('google')}
            >
              Google로 계속하기
            </button>
            <button
              className="social-login-button kakao"
              type="button"
              disabled={isAuthLoading}
              onClick={() => startSocialLogin('kakao')}
            >
              Kakao로 계속하기
            </button>
            <button
              className="social-login-button naver"
              type="button"
              disabled={isAuthLoading}
              onClick={() => startSocialLogin('naver')}
            >
              Naver로 계속하기
            </button>
          </div>
        </div>
      </Card>
    </section>
  );
}

export default LoginPage;
