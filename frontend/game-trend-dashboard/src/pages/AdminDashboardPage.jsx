import { useEffect, useMemo, useState } from 'react';
import {
  approveAdminApprovalRequest,
  deleteAdminConversation,
  getAdminApprovalRequests,
  getAdminAuditLogs,
  getAdminConversations,
  getAdminDashboard,
  getAdminUsers,
  hideAdminConversation,
  rejectAdminApprovalRequest,
  restoreAdminConversation,
  updateAdminUserRole,
  updateAdminUserStatus,
} from '../api/gameTrendApi.js';

const menuItems = [
  { path: '/admin/dashboard', label: 'Dashboard' },
  { path: '/admin/users', label: 'Users' },
  { path: '/admin/approval-requests', label: 'Admin Approvals' },
  { path: '/admin/conversations', label: 'Conversations' },
  { path: '/admin/reports', label: 'Reports' },
  { path: '/admin/audit-logs', label: 'Audit Logs' },
  { path: '/admin/settings', label: 'Settings' },
];

const emptyPage = {
  items: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
};

function AdminDashboardPage({ authUser, currentPath, onNavigate, onLogout }) {
  const normalizedPath = currentPath === '/admin'
    ? '/admin/dashboard'
    : currentPath === '/admin/chats'
      ? '/admin/conversations'
      : currentPath;
  const isOwner = String(authUser?.role || '').toUpperCase() === 'OWNER';
  const [dashboard, setDashboard] = useState(null);
  const [users, setUsers] = useState(emptyPage);
  const [approvals, setApprovals] = useState(emptyPage);
  const [conversations, setConversations] = useState(emptyPage);
  const [auditLogs, setAuditLogs] = useState(emptyPage);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const [userFilters, setUserFilters] = useState({
    email: '',
    nickname: '',
    role: '',
    status: '',
    sort: 'createdAt',
    page: 0,
    size: 10,
  });
  const [approvalFilters, setApprovalFilters] = useState({
    search: '',
    status: '',
    sort: 'requestedAt',
    page: 0,
    size: 10,
  });
  const [chatFilters, setChatFilters] = useState({
    user: '',
    keyword: '',
    status: '',
    sort: 'createdAt',
    page: 0,
    size: 10,
  });
  const [auditFilters, setAuditFilters] = useState({
    action: '',
    targetType: '',
    search: '',
    page: 0,
    size: 10,
  });

  const loadDashboard = async () => {
    const data = await getAdminDashboard();
    setDashboard(data);
  };

  const loadUsers = async () => {
    const data = await getAdminUsers(userFilters);
    setUsers(data);
  };

  const loadApprovals = async () => {
    const data = await getAdminApprovalRequests(approvalFilters);
    setApprovals(data);
  };

  const loadConversations = async () => {
    const data = await getAdminConversations(chatFilters);
    setConversations(data);
  };

  const loadAuditLogs = async () => {
    const data = await getAdminAuditLogs(auditFilters);
    setAuditLogs(data);
  };

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      setIsLoading(true);
      setErrorMessage('');
      try {
        if (normalizedPath === '/admin/dashboard') {
          await loadDashboard();
        } else if (normalizedPath === '/admin/users') {
          await loadUsers();
        } else if (normalizedPath === '/admin/approval-requests') {
          await loadApprovals();
        } else if (normalizedPath === '/admin/conversations') {
          await loadConversations();
        } else if (normalizedPath === '/admin/reports') {
          await loadConversations();
        } else if (normalizedPath === '/admin/audit-logs') {
          await loadAuditLogs();
        }
      } catch (error) {
        if (!cancelled) {
          setErrorMessage(error.message || '관리자 데이터를 불러오지 못했습니다.');
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    };
    run();
    return () => {
      cancelled = true;
    };
  }, [normalizedPath, userFilters, approvalFilters, chatFilters, auditFilters]);

  const title = useMemo(() => {
    const current = menuItems.find((item) => item.path === normalizedPath);
    return current?.label || 'Dashboard';
  }, [normalizedPath]);

  const reloadCurrent = async () => {
    setIsLoading(true);
    setErrorMessage('');
    try {
      if (normalizedPath === '/admin/dashboard') await loadDashboard();
      if (normalizedPath === '/admin/users') await loadUsers();
      if (normalizedPath === '/admin/approval-requests') await loadApprovals();
      if (normalizedPath === '/admin/conversations') await loadConversations();
      if (normalizedPath === '/admin/reports') await loadConversations();
      if (normalizedPath === '/admin/audit-logs') await loadAuditLogs();
    } catch (error) {
      setErrorMessage(error.message || '관리자 데이터를 불러오지 못했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="admin-console">
      <aside className="admin-sidebar">
        <div className="admin-brand">
          <span>GT</span>
          <strong>Admin</strong>
        </div>
        <nav className="admin-menu" aria-label="관리자 메뉴">
          {menuItems.map((item) => (
            <button
              className={`admin-menu-item ${normalizedPath === item.path ? 'active' : ''}`}
              type="button"
              key={item.path}
              onClick={() => onNavigate(item.path)}
            >
              {item.label}
            </button>
          ))}
        </nav>
      </aside>

      <main className="admin-main">
        <header className="admin-topbar">
          <div>
            <p>Admin Console</p>
            <h1>{title}</h1>
          </div>
          <div className="admin-profile">
            <span>{authUser?.role}</span>
            <strong>{authUser?.nickname || authUser?.email}</strong>
            <button type="button" onClick={onLogout}>로그아웃</button>
          </div>
        </header>

        {errorMessage && <p className="admin-auth-error">{errorMessage}</p>}
        {isLoading && <p className="admin-loading">관리자 데이터를 불러오는 중입니다.</p>}

        {normalizedPath === '/admin/dashboard' && (
          <DashboardSection dashboard={dashboard} />
        )}

        {normalizedPath === '/admin/users' && (
          <UsersSection
            page={users}
            filters={userFilters}
            setFilters={setUserFilters}
            isOwner={isOwner}
            onStatusChange={async (user, status) => {
              const reason = window.prompt('상태 변경 사유를 입력해주세요.', '');
              await updateAdminUserStatus(user.id, { status, reason });
              await reloadCurrent();
            }}
            onRoleChange={async (user, role) => {
              const reason = window.prompt('권한 변경 사유를 입력해주세요.', '');
              await updateAdminUserRole(user.id, { role, reason });
              await reloadCurrent();
            }}
          />
        )}

        {normalizedPath === '/admin/approval-requests' && (
          <ApprovalsSection
            page={approvals}
            filters={approvalFilters}
            setFilters={setApprovalFilters}
            isOwner={isOwner}
            onApprove={async (request) => {
              await approveAdminApprovalRequest(request.id);
              await reloadCurrent();
            }}
            onReject={async (request) => {
              const reason = window.prompt('거절 사유를 입력해주세요.', '');
              await rejectAdminApprovalRequest(request.id, { reason });
              await reloadCurrent();
            }}
          />
        )}

        {(normalizedPath === '/admin/conversations' || normalizedPath === '/admin/reports') && (
          <ChatsSection
            page={conversations}
            filters={chatFilters}
            setFilters={setChatFilters}
            reportsOnly={normalizedPath === '/admin/reports'}
            onHide={async (chat) => {
              const reason = window.prompt('숨김 처리 사유를 입력해주세요.', '');
              await hideAdminConversation(chat.id, { reason });
              await reloadCurrent();
            }}
            onRestore={async (chat) => {
              const reason = window.prompt('숨김 해제 사유를 입력해주세요.', '');
              await restoreAdminConversation(chat.id, { reason });
              await reloadCurrent();
            }}
            onDelete={async (chat) => {
              const reason = window.prompt('삭제 사유를 입력해주세요.', '');
              await deleteAdminConversation(chat.id, { reason });
              await reloadCurrent();
            }}
          />
        )}

        {normalizedPath === '/admin/audit-logs' && (
          <AuditLogsSection
            page={auditLogs}
            filters={auditFilters}
            setFilters={setAuditFilters}
          />
        )}

        {normalizedPath === '/admin/settings' && (
          <SettingsSection authUser={authUser} />
        )}
      </main>
    </div>
  );
}

function DashboardSection({ dashboard }) {
  const stats = [
    { label: '전체 사용자', value: dashboard?.totalUserCount ?? 0 },
    { label: '일반 사용자', value: dashboard?.userCount ?? 0 },
    { label: '관리자', value: dashboard?.adminCount ?? 0 },
    { label: 'OWNER', value: dashboard?.ownerCount ?? 0 },
    { label: '승인 대기', value: dashboard?.pendingApprovalCount ?? 0 },
    { label: '오늘 가입', value: dashboard?.todaySignupCount ?? 0 },
    { label: '최근 로그인', value: dashboard?.recentLoginCount ?? 0 },
    { label: '전체 대화', value: dashboard?.totalConversationCount ?? 0 },
    { label: '신고된 대화', value: dashboard?.reportedConversationCount ?? 0 },
    { label: '숨김 대화', value: dashboard?.hiddenConversationCount ?? 0 },
  ];

  return (
    <section className="admin-stat-grid">
      {stats.map((stat) => (
        <article className="admin-stat-card" key={stat.label}>
          <span>{stat.label}</span>
          <strong>{formatCount(stat.value)}</strong>
        </article>
      ))}
    </section>
  );
}

function UsersSection({ page, filters, setFilters, isOwner, onStatusChange, onRoleChange }) {
  return (
    <AdminPanel title="유저 목록 관리">
      <div className="admin-filter-grid">
        <FilterInput label="이메일" value={filters.email} onChange={(email) => setFilters((prev) => ({ ...prev, email, page: 0 }))} />
        <FilterInput label="닉네임" value={filters.nickname} onChange={(nickname) => setFilters((prev) => ({ ...prev, nickname, page: 0 }))} />
        <FilterSelect label="권한" value={filters.role} onChange={(role) => setFilters((prev) => ({ ...prev, role, page: 0 }))} options={['', 'USER', 'ADMIN', 'OWNER']} />
        <FilterSelect label="상태" value={filters.status} onChange={(status) => setFilters((prev) => ({ ...prev, status, page: 0 }))} options={['', 'ACTIVE', 'SUSPENDED', 'DELETED']} />
        <FilterSelect label="정렬" value={filters.sort} onChange={(sort) => setFilters((prev) => ({ ...prev, sort, page: 0 }))} options={['createdAt', 'email', 'lastLoginAt']} />
      </div>

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>이메일</th>
              <th>닉네임</th>
              <th>권한</th>
              <th>상태</th>
              <th>가입일</th>
              <th>작업</th>
            </tr>
          </thead>
          <tbody>
            {page.items.map((user) => (
              <tr key={user.id}>
                <td>{user.id}</td>
                <td>{user.email}</td>
                <td>{user.nickname}</td>
                <td><StatusBadge value={user.role} /></td>
                <td><StatusBadge value={user.status} /></td>
                <td>{formatDate(user.createdAt)}</td>
                <td>
                  <div className="admin-row-actions">
                    <button type="button" onClick={() => onStatusChange(user, user.status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED')}>
                      {user.status === 'SUSPENDED' ? '해제' : '정지'}
                    </button>
                    <button type="button" disabled={!isOwner} onClick={() => onRoleChange(user, user.role === 'ADMIN' ? 'USER' : 'ADMIN')}>
                      {user.role === 'ADMIN' ? '권한 회수' : 'ADMIN 부여'}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination page={page} onPageChange={(nextPage) => setFilters((prev) => ({ ...prev, page: nextPage }))} />
    </AdminPanel>
  );
}

function ApprovalsSection({ page, filters, setFilters, isOwner, onApprove, onReject }) {
  return (
    <AdminPanel title="관리자 승인 요청">
      <div className="admin-filter-grid">
        <FilterInput label="검색" value={filters.search} onChange={(search) => setFilters((prev) => ({ ...prev, search, page: 0 }))} />
        <FilterSelect label="상태" value={filters.status} onChange={(status) => setFilters((prev) => ({ ...prev, status, page: 0 }))} options={['', 'PENDING', 'APPROVED', 'REJECTED', 'EXPIRED']} />
        <FilterSelect label="정렬" value={filters.sort} onChange={(sort) => setFilters((prev) => ({ ...prev, sort, page: 0 }))} options={['requestedAt', 'expiresAt', 'status']} />
      </div>

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>이메일</th>
              <th>닉네임</th>
              <th>전화번호</th>
              <th>상태</th>
              <th>요청일</th>
              <th>만료</th>
              <th>작업</th>
            </tr>
          </thead>
          <tbody>
            {page.items.map((request) => (
              <tr key={request.id}>
                <td>{request.id}</td>
                <td>{request.requesterEmail}</td>
                <td>{request.requesterNickname}</td>
                <td>{request.requesterPhoneNumber || '-'}</td>
                <td><StatusBadge value={request.status} /></td>
                <td>{formatDate(request.requestedAt)}</td>
                <td>{formatDate(request.tokenExpiresAt)}</td>
                <td>
                  <div className="admin-row-actions">
                    <button type="button" disabled={!isOwner || request.status !== 'PENDING'} onClick={() => onApprove(request)}>
                      승인
                    </button>
                    <button type="button" disabled={!isOwner || request.status !== 'PENDING'} onClick={() => onReject(request)}>
                      거절
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination page={page} onPageChange={(nextPage) => setFilters((prev) => ({ ...prev, page: nextPage }))} />
    </AdminPanel>
  );
}

function ChatsSection({ page, filters, setFilters, reportsOnly, onHide, onRestore, onDelete }) {
  return (
    <AdminPanel title={reportsOnly ? '대화 신고 관리' : '대화 관리'}>
      <div className="admin-filter-grid">
        <FilterInput label="사용자" value={filters.user} onChange={(user) => setFilters((prev) => ({ ...prev, user, page: 0 }))} />
        <FilterInput label="키워드" value={filters.keyword} onChange={(keyword) => setFilters((prev) => ({ ...prev, keyword, page: 0 }))} />
        <FilterSelect label="상태" value={filters.status} onChange={(status) => setFilters((prev) => ({ ...prev, status, page: 0 }))} options={['', 'ACTIVE', 'HIDDEN', 'DELETED']} />
      </div>

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>사용자</th>
              <th>마지막 메시지</th>
              <th>상태</th>
              <th>신고</th>
              <th>생성일</th>
              <th>작업</th>
            </tr>
          </thead>
          <tbody>
            {page.items.map((chat) => (
              <tr key={chat.id}>
                <td>{chat.id}</td>
                <td>{chat.userId || '-'}</td>
                <td className="admin-content-cell">{chat.lastMessage || chat.title || '-'}</td>
                <td><StatusBadge value={chat.status} /></td>
                <td>{chat.reported ? '신고됨' : '-'}</td>
                <td>{formatDate(chat.createdAt)}</td>
                <td>
                  <div className="admin-row-actions">
                    <button type="button" disabled={chat.status === 'HIDDEN'} onClick={() => onHide(chat)}>숨김</button>
                    <button type="button" disabled={chat.status === 'ACTIVE'} onClick={() => onRestore(chat)}>복구</button>
                    <button type="button" disabled={chat.status === 'DELETED'} onClick={() => onDelete(chat)}>삭제</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination page={page} onPageChange={(nextPage) => setFilters((prev) => ({ ...prev, page: nextPage }))} />
    </AdminPanel>
  );
}

function AuditLogsSection({ page, filters, setFilters }) {
  return (
    <AdminPanel title="관리자 활동 로그">
      <div className="admin-filter-grid">
        <FilterInput label="검색" value={filters.search} onChange={(search) => setFilters((prev) => ({ ...prev, search, page: 0 }))} />
        <FilterInput label="액션" value={filters.action} onChange={(action) => setFilters((prev) => ({ ...prev, action, page: 0 }))} />
        <FilterInput label="대상" value={filters.targetType} onChange={(targetType) => setFilters((prev) => ({ ...prev, targetType, page: 0 }))} />
      </div>

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>관리자</th>
              <th>액션</th>
              <th>대상</th>
              <th>상세</th>
              <th>시간</th>
            </tr>
          </thead>
          <tbody>
            {page.items.map((log) => (
              <tr key={log.id}>
                <td>{log.id}</td>
                <td>{log.adminUserId || '-'}</td>
                <td><StatusBadge value={log.action} /></td>
                <td>{log.targetType} #{log.targetId || '-'}</td>
                <td className="admin-content-cell">{log.detail || '-'}</td>
                <td>{formatDate(log.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination page={page} onPageChange={(nextPage) => setFilters((prev) => ({ ...prev, page: nextPage }))} />
    </AdminPanel>
  );
}

function SettingsSection({ authUser }) {
  return (
    <AdminPanel title="Settings">
      <section className="admin-settings-grid">
        <article>
          <span>현재 권한</span>
          <strong>{authUser?.role}</strong>
          <p>관리자 승인과 권한 변경은 OWNER 계정만 수행할 수 있습니다.</p>
        </article>
        <article>
          <span>승인 담당 이메일</span>
          <strong>ksjcloud98@gmail.com</strong>
          <p>관리자 승인 요청은 서버에서 이 주소로만 발송되도록 고정됩니다.</p>
        </article>
      </section>
    </AdminPanel>
  );
}

function AdminPanel({ title, children }) {
  return (
    <section className="admin-panel">
      <div className="admin-panel-heading">
        <h2>{title}</h2>
      </div>
      {children}
    </section>
  );
}

function FilterInput({ label, value, onChange }) {
  return (
    <label className="admin-filter-field">
      <span>{label}</span>
      <input value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function FilterSelect({ label, value, onChange, options }) {
  return (
    <label className="admin-filter-field">
      <span>{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => (
          <option value={option} key={option || 'ALL'}>
            {option || 'ALL'}
          </option>
        ))}
      </select>
    </label>
  );
}

function Pagination({ page, onPageChange }) {
  const current = page.page ?? 0;
  const totalPages = page.totalPages ?? 0;
  return (
    <div className="admin-pagination">
      <span>
        {formatCount(page.totalElements || 0)}개 중 {totalPages === 0 ? 0 : current + 1}/{Math.max(totalPages, 1)}
      </span>
      <div>
        <button type="button" disabled={current <= 0} onClick={() => onPageChange(current - 1)}>이전</button>
        <button type="button" disabled={totalPages === 0 || current >= totalPages - 1} onClick={() => onPageChange(current + 1)}>다음</button>
      </div>
    </div>
  );
}

function StatusBadge({ value }) {
  return <span className={`admin-status-badge ${String(value || '').toLowerCase()}`}>{value || '-'}</span>;
}

function formatDate(value) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}

function formatCount(value) {
  return new Intl.NumberFormat('ko-KR').format(Number(value) || 0);
}

export function AdminAccessDeniedPage({ authUser, onGoHome, onLogout }) {
  return (
    <main className="admin-auth-page">
      <section className="admin-auth-panel denied">
        <div className="admin-auth-heading">
          <p>Access Denied</p>
          <h1>관리자 권한이 필요합니다</h1>
          <span>
            현재 계정 {authUser?.email ? `(${authUser.email})` : ''}은 관리자 페이지에 접근할 수 없습니다.
          </span>
        </div>
        <div className="admin-auth-actions">
          <ButtonLike onClick={onGoHome}>일반 화면으로</ButtonLike>
          <ButtonLike variant="secondary" onClick={onLogout}>로그아웃</ButtonLike>
        </div>
      </section>
    </main>
  );
}

function ButtonLike({ children, variant = 'primary', onClick }) {
  return (
    <button className={`button button-${variant}`} type="button" onClick={onClick}>
      {children}
    </button>
  );
}

export default AdminDashboardPage;
