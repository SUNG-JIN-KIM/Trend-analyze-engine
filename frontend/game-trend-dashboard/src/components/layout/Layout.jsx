function Layout({
  children,
  routes = [],
  currentPath = '/',
  onNavigate,
  authUser,
  isAuthLoading = false,
  onGoLogin,
  onGoRegister,
  onLogout,
}) {
  const isAgentPage = currentPath === '/agent';
  const isHomePage = currentPath === '/';

  return (
    <div className={`app-shell ${isAgentPage ? 'agent-app-shell' : ''}`}>
      <div className="background-grid" />
      <header className={`hero-header ${isHomePage ? 'home-shell-header' : ''} ${isAgentPage ? 'agent-shell-header' : ''}`}>
        <div className="hero-topline">
          {isHomePage ? (
            <div className="home-brand-mark">
              <span>GT</span>
              <strong>Game Trend Agent</strong>
            </div>
          ) : (
            <div className="hero-copy">
              <p className="eyebrow">Live Game Trend Intelligence</p>
              <h1>게임 트렌드를 무엇이든 물어보세요</h1>
              <p>
                실시간 라이브 순위와 대화 기록을 바탕으로 인기 게임, 방송 반응, 개발 기회까지 자연스럽게 분석합니다.
              </p>
            </div>
          )}
          <AuthNavigation
            authUser={authUser}
            isAuthLoading={isAuthLoading}
            onGoLogin={onGoLogin}
            onGoRegister={onGoRegister}
            onLogout={onLogout}
          />
        </div>

        <nav className="app-navigation" aria-label="주요 화면">
          {routes.map((route) => {
            const active = currentPath === route.path
              || (route.path !== '/' && currentPath.startsWith(`${route.path}/`));
            return (
              <button
                className={`nav-item ${active ? 'active' : ''}`}
                type="button"
                key={route.path}
                aria-current={active ? 'page' : undefined}
                onClick={() => onNavigate?.(route.path)}
              >
                <strong>{route.label}</strong>
                <span>{route.description}</span>
              </button>
            );
          })}
        </nav>
      </header>
      <main className="dashboard-container">{children}</main>
    </div>
  );
}

function AuthNavigation({ authUser, isAuthLoading, onGoLogin, onGoRegister, onLogout }) {
  if (isAuthLoading && !authUser) {
    return (
      <div className="auth-nav-panel">
        <span>로그인 상태 확인 중</span>
      </div>
    );
  }

  if (authUser) {
    return (
      <div className="auth-nav-panel logged-in">
        <span>로그인</span>
        <strong>{authUser.nickname || authUser.email}</strong>
        <button className="auth-nav-button" type="button" onClick={onLogout}>
          로그아웃
        </button>
      </div>
    );
  }

  return (
    <div className="auth-nav-panel">
      <button className="auth-nav-button" type="button" onClick={onGoLogin}>
        로그인
      </button>
      <button className="auth-nav-button primary" type="button" onClick={onGoRegister}>
        회원가입
      </button>
    </div>
  );
}

export default Layout;
