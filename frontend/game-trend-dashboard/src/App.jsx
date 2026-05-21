import { useEffect, useMemo, useState } from 'react';
import Layout from './components/layout/Layout.jsx';
import ErrorMessage from './components/common/ErrorMessage.jsx';
import SuccessMessage from './components/common/SuccessMessage.jsx';
import {
  clearStoredAccessToken,
  adminLogin,
  getCurrentUser,
  getStoredAccessToken,
  login,
  register,
  storeAccessToken,
} from './api/gameTrendApi.js';
import { useGameTrendOnboarding } from './hooks/useGameTrendOnboarding.js';
import AdminDashboardPage, { AdminAccessDeniedPage } from './pages/AdminDashboardPage.jsx';
import AdminLoginPage from './pages/AdminLoginPage.jsx';
import AgentPage from './pages/AgentPage.jsx';
import HistoryPage from './pages/HistoryPage.jsx';
import HomePage from './pages/HomePage.jsx';
import LiveTrendsPage from './pages/LiveTrendsPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import OAuthCallbackPage from './pages/OAuthCallbackPage.jsx';
import RankingsPage from './pages/RankingsPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';

const baseRoutes = [
  { path: '/agent', label: 'Agent', description: '자연어 질문' },
  { path: '/rankings', label: '실시간 순위', description: '뜨는 게임' },
];

const authenticatedRoutes = [
  { path: '/history', label: '대화 기록', description: '저장된 분석' },
];

const hiddenRoutePaths = ['/live-trends'];

function App() {
  const dashboard = useGameTrendOnboarding();
  const [currentPath, setCurrentPath] = useState(normalizePath(window.location.pathname));
  const [authUser, setAuthUser] = useState(null);
  const [isAuthLoading, setIsAuthLoading] = useState(false);

  useEffect(() => {
    const handlePopState = () => setCurrentPath(normalizePath(window.location.pathname));
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  useEffect(() => {
    const accessToken = getStoredAccessToken();
    if (!accessToken) {
      return;
    }

    let cancelled = false;
    setIsAuthLoading(true);
    getCurrentUser()
      .then((user) => {
        if (!cancelled) {
          setAuthUser(user);
        }
      })
      .catch(() => {
        clearStoredAccessToken();
        if (!cancelled) {
          setAuthUser(null);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsAuthLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const routes = useMemo(
    () => (authUser ? [...baseRoutes, ...authenticatedRoutes] : baseRoutes),
    [authUser]
  );

  const routePaths = useMemo(
    () => new Set([
      ...baseRoutes.map((route) => route.path),
      ...authenticatedRoutes.map((route) => route.path),
      ...hiddenRoutePaths,
      '/login',
      '/register',
      '/oauth/callback',
      '/admin',
      '/admin/login',
      '/admin/dashboard',
      '/admin/users',
      '/admin/approval-requests',
      '/admin/chats',
      '/admin/conversations',
      '/admin/reports',
      '/admin/audit-logs',
      '/admin/settings',
      '/',
    ]),
    []
  );
  const isAdminPath = currentPath === '/admin' || currentPath.startsWith('/admin/');
  const activePath = isAdminPath || routePaths.has(currentPath) ? currentPath : '/';

  const navigate = (path) => {
    const nextPath = normalizePath(path);
    if (nextPath === activePath) {
      return;
    }
    window.history.pushState({}, '', nextPath);
    setCurrentPath(nextPath);
    dashboard.clearError();
    dashboard.clearSuccess();
  };

  const handleLogin = async (payload) => {
    setIsAuthLoading(true);
    dashboard.clearError();
    dashboard.clearSuccess();
    try {
      const tokenResponse = await login(payload);
      storeAccessToken(tokenResponse.accessToken);
      const user = await getCurrentUser();
      setAuthUser(user);
      dashboard.clearLoginRequiredNotice();
      dashboard.clearSuccess();
      navigate('/');
    } finally {
      setIsAuthLoading(false);
    }
  };

  const handleAdminLogin = async (payload) => {
    setIsAuthLoading(true);
    dashboard.clearError();
    dashboard.clearSuccess();
    try {
      const tokenResponse = await adminLogin(payload);
      storeAccessToken(tokenResponse.accessToken);
      const user = await getCurrentUser();
      setAuthUser(user);
      navigate('/admin/dashboard');
    } finally {
      setIsAuthLoading(false);
    }
  };

  const handleRegister = async (payload) => {
    setIsAuthLoading(true);
    dashboard.clearError();
    dashboard.clearSuccess();
    try {
      const tokenResponse = await register(payload);
      storeAccessToken(tokenResponse.accessToken);
      const user = await getCurrentUser();
      setAuthUser(user);
      dashboard.clearLoginRequiredNotice();
      dashboard.clearSuccess();
      navigate('/');
    } finally {
      setIsAuthLoading(false);
    }
  };

  const handleOAuthToken = async (accessToken) => {
    setIsAuthLoading(true);
    dashboard.clearError();
    dashboard.clearSuccess();
    try {
      storeAccessToken(accessToken);
      const user = await getCurrentUser();
      setAuthUser(user);
      dashboard.clearLoginRequiredNotice();
      return user;
    } catch (error) {
      clearStoredAccessToken();
      setAuthUser(null);
      throw error;
    } finally {
      setIsAuthLoading(false);
    }
  };

  const handleLogout = () => {
    clearStoredAccessToken();
    setAuthUser(null);
    dashboard.clearConversationState();
    dashboard.clearLoginRequiredNotice();
    dashboard.clearError();
    dashboard.clearSuccess();
  };

  const renderAdminPage = () => {
    const adminPath = activePath === '/admin' ? '/admin/dashboard' : activePath;

    if (adminPath === '/admin/login') {
      return (
        <AdminLoginPage
          onAdminLogin={handleAdminLogin}
          onGoHome={() => navigate('/')}
          isAuthLoading={isAuthLoading}
        />
      );
    }

    if (isAuthLoading && !authUser) {
      return (
        <AdminLoginPage
          onAdminLogin={handleAdminLogin}
          onGoHome={() => navigate('/')}
          isAuthLoading={isAuthLoading}
          notice="관리자 로그인 상태를 확인하고 있습니다."
        />
      );
    }

    if (!authUser) {
      return (
        <AdminLoginPage
          onAdminLogin={handleAdminLogin}
          onGoHome={() => navigate('/')}
          isAuthLoading={isAuthLoading}
          notice="관리자 페이지에 접근하려면 관리자 로그인이 필요합니다."
        />
      );
    }

    if (!isAdminUser(authUser)) {
      return (
        <AdminAccessDeniedPage
          authUser={authUser}
          onGoHome={() => navigate('/')}
          onLogout={handleLogout}
        />
      );
    }

    return (
      <AdminDashboardPage
        authUser={authUser}
        currentPath={adminPath}
        onNavigate={navigate}
        onLogout={handleLogout}
      />
    );
  };

  const renderPage = () => {
    switch (activePath) {
      case '/login':
        return (
          <LoginPage
            onLogin={handleLogin}
            onGoRegister={() => navigate('/register')}
            isAuthLoading={isAuthLoading}
          />
        );
      case '/register':
        return (
          <RegisterPage
            onRegister={handleRegister}
            onGoLogin={() => navigate('/login')}
            isAuthLoading={isAuthLoading}
          />
        );
      case '/oauth/callback':
        return (
          <OAuthCallbackPage
            onOAuthToken={handleOAuthToken}
            onGoHome={() => navigate('/')}
            onGoLogin={() => navigate('/login')}
          />
        );
      case '/history':
        if (!authUser) {
          return (
            <LoginPage
              onLogin={handleLogin}
              onGoRegister={() => navigate('/register')}
              isAuthLoading={isAuthLoading}
            />
          );
        }
        return <HistoryPage dashboard={dashboard} />;
      case '/rankings':
        return <RankingsPage />;
      case '/live-trends':
        return <LiveTrendsPage dashboard={dashboard} onGoLogin={() => navigate('/login')} />;
      case '/agent':
        return (
          <AgentPage
            dashboard={dashboard}
            authUser={authUser}
            onGoHome={() => navigate('/')}
            onGoTrends={() => navigate('/rankings')}
            onGoHistory={() => navigate('/history')}
            onGoPricing={() => window.alert('요금 안내는 준비 중입니다.')}
            onGoLogin={() => navigate('/login')}
            onGoRegister={() => navigate('/register')}
            onLogout={handleLogout}
          />
        );
      case '/':
        return (
          <HomePage
            isAnalyzing={dashboard.isAnalyzingOnboarding || dashboard.isAnalyzingFollowUp}
            onAsk={(question) => {
              navigate('/agent');
              dashboard.analyzeNewQuestion(question);
            }}
          />
        );
      default:
        return (
          <HomePage
            isAnalyzing={dashboard.isAnalyzingOnboarding || dashboard.isAnalyzingFollowUp}
            onAsk={(question) => {
              navigate('/agent');
              dashboard.analyzeNewQuestion(question);
            }}
          />
        );
    }
  };

  if (isAdminPath) {
    return renderAdminPage();
  }

  return (
    <Layout
      routes={routes}
      currentPath={activePath}
      onNavigate={navigate}
      authUser={authUser}
      isAuthLoading={isAuthLoading}
      onGoLogin={() => navigate('/login')}
      onGoRegister={() => navigate('/register')}
      onLogout={handleLogout}
    >
      <ErrorMessage message={dashboard.errorMessage} onClose={dashboard.clearError} />
      <SuccessMessage message={dashboard.successMessage} onClose={dashboard.clearSuccess} />
      {renderPage()}
    </Layout>
  );
}

function isAdminUser(user) {
  const role = String(user?.role || '').toUpperCase();
  return role === 'ADMIN' || role === 'OWNER';
}

function normalizePath(path) {
  if (!path || path === '') {
    return '/';
  }
  const normalized = path.replace(/\/+$/, '');
  return normalized || '/';
}

export default App;
