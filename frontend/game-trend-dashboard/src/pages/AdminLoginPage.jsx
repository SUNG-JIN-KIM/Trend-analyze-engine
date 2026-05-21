import { useState } from 'react';
import Button from '../components/common/Button.jsx';

const initialForm = {
  email: '',
  password: '',
};

function AdminLoginPage({ onAdminLogin, onGoHome, isAuthLoading, notice }) {
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
      await onAdminLogin(form);
      setForm(initialForm);
    } catch (error) {
      setErrorMessage(error.message || '관리자 로그인에 실패했습니다.');
    }
  };

  return (
    <main className="admin-auth-page">
      <section className="admin-auth-panel">
        <div className="admin-auth-heading">
          <p>Admin Console</p>
          <h1>관리자 로그인</h1>
          <span>승인된 ADMIN 또는 OWNER 계정만 접근할 수 있습니다.</span>
        </div>

        {notice && <p className="admin-auth-notice">{notice}</p>}

        <form className="admin-auth-form" onSubmit={handleSubmit}>
          <label>
            <span>관리자 이메일</span>
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

          {errorMessage && <p className="admin-auth-error">{errorMessage}</p>}

          <div className="admin-auth-actions">
            <Button type="submit" disabled={isAuthLoading}>
              {isAuthLoading ? '확인 중' : '관리자 로그인'}
            </Button>
            <Button variant="secondary" onClick={onGoHome} disabled={isAuthLoading}>
              일반 화면으로
            </Button>
          </div>
        </form>
      </section>
    </main>
  );
}

export default AdminLoginPage;
