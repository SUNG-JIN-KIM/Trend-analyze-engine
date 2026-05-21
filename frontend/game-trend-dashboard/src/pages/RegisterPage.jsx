import { useState } from 'react';
import Button from '../components/common/Button.jsx';
import Card from '../components/common/Card.jsx';

const initialForm = {
  email: '',
  password: '',
  nickname: '',
};

function RegisterPage({ onRegister, onGoLogin, isAuthLoading }) {
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
      await onRegister(form);
      setForm(initialForm);
    } catch (error) {
      setErrorMessage(error.message || '회원가입에 실패했습니다.');
    }
  };

  return (
    <section className="auth-page">
      <Card className="auth-card">
        <div className="auth-heading">
          <p className="section-kicker">Register</p>
          <h2>회원가입</h2>
          <p>비로그인 질문은 바로 사용할 수 있고, 계정을 만들면 고급 분석 기능까지 열립니다.</p>
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
              autoComplete="new-password"
              minLength={8}
              required
            />
          </label>

          <label>
            <span>닉네임</span>
            <input
              type="text"
              value={form.nickname}
              onChange={(event) => updateField('nickname', event.target.value)}
              autoComplete="nickname"
              required
            />
          </label>

          {errorMessage && <p className="auth-error">{errorMessage}</p>}

          <div className="auth-actions">
            <Button type="submit" disabled={isAuthLoading}>
              {isAuthLoading ? '가입 중' : '회원가입'}
            </Button>
            <Button variant="secondary" onClick={onGoLogin} disabled={isAuthLoading}>
              로그인하기
            </Button>
          </div>
        </form>
      </Card>
    </section>
  );
}

export default RegisterPage;
