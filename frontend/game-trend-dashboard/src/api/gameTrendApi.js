const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
const OAUTH_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const ACCESS_TOKEN_KEY = 'gameTrendAccessToken';

export class ApiError extends Error {
  constructor(message, { status, code, details, body } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.details = details || [];
    this.body = body;
  }
}

export function getStoredAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function storeAccessToken(accessToken) {
  if (!accessToken) {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    return;
  }
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
}

export function clearStoredAccessToken() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
}

async function request(path, options = {}) {
  const accessToken = getStoredAccessToken();
  const { headers: optionHeaders, ...requestOptions } = options;
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...requestOptions,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...(optionHeaders || {}),
    },
  });

  const contentType = response.headers.get('content-type') || '';
  const body = contentType.includes('application/json')
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    if (response.status === 401) {
      clearStoredAccessToken();
    }
    const message = resolveApiErrorMessage(body);
    throw new ApiError(message, {
      status: response.status,
      code: typeof body === 'string' ? undefined : body.code,
      details: typeof body === 'string' ? [] : body.details,
      body,
    });
  }

  return body;
}

function resolveApiErrorMessage(body) {
  if (typeof body === 'string') {
    return body;
  }
  if (Array.isArray(body?.details) && body.details.length > 0) {
    return body.details[0];
  }
  return body?.message || 'API 요청 중 오류가 발생했습니다.';
}

export function register(payload) {
  return request('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function sendPhoneVerificationCode(payload) {
  return request('/api/auth/phone/send-code', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function verifyPhoneVerificationCode(payload) {
  return request('/api/auth/phone/verify', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function login(payload) {
  return request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function logout() {
  return request('/api/auth/logout', {
    method: 'POST',
  });
}

export function adminLogin(payload) {
  return request('/api/auth/admin/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getCurrentUser() {
  return request('/api/auth/me');
}

function toQueryString(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && String(value).trim() !== '') {
      query.set(key, String(value));
    }
  });
  const text = query.toString();
  return text ? `?${text}` : '';
}

function toKeywordQuery(keyword) {
  const normalizedKeyword = String(keyword ?? '').trim();
  if (!normalizedKeyword) {
    throw new ApiError('수집할 게임 키워드를 입력해주세요.', {
      status: 400,
      code: 'INVALID_KEYWORD',
    });
  }
  return `?keyword=${encodeURIComponent(normalizedKeyword)}`;
}

export function getAdminDashboard() {
  return request('/api/admin/dashboard');
}

export function getAdminUsers(params = {}) {
  return request(`/api/admin/users${toQueryString(params)}`);
}

export function getAdminUser(userId) {
  return request(`/api/admin/users/${userId}`);
}

export function updateAdminUserStatus(userId, payload) {
  return request(`/api/admin/users/${userId}/status`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function updateAdminUserRole(userId, payload) {
  return request(`/api/admin/users/${userId}/role`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function getAdminApprovalRequests(params = {}) {
  return request(`/api/admin/approval-requests${toQueryString(params)}`);
}

export function approveAdminApprovalRequest(requestId) {
  return request(`/api/admin/approval-requests/${requestId}/approve`, {
    method: 'POST',
    body: JSON.stringify({}),
  });
}

export function rejectAdminApprovalRequest(requestId, payload = {}) {
  return request(`/api/admin/approval-requests/${requestId}/reject`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getAdminChats(params = {}) {
  return request(`/api/admin/chats${toQueryString(params)}`);
}

export function getAdminConversations(params = {}) {
  return request(`/api/admin/conversations${toQueryString(params)}`);
}

export function hideAdminChat(chatId, payload = {}) {
  return request(`/api/admin/chats/${chatId}/hide`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function hideAdminConversation(conversationId, payload = {}) {
  return request(`/api/admin/conversations/${conversationId}/hide`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function restoreAdminChat(chatId, payload = {}) {
  return request(`/api/admin/chats/${chatId}/restore`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function restoreAdminConversation(conversationId, payload = {}) {
  return request(`/api/admin/conversations/${conversationId}/restore`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function deleteAdminChat(chatId, payload = {}) {
  return request(`/api/admin/chats/${chatId}`, {
    method: 'DELETE',
    body: JSON.stringify(payload),
  });
}

export function deleteAdminConversation(conversationId, payload = {}) {
  return request(`/api/admin/conversations/${conversationId}`, {
    method: 'DELETE',
    body: JSON.stringify(payload),
  });
}

export function getAdminAuditLogs(params = {}) {
  return request(`/api/admin/audit-logs${toQueryString(params)}`);
}

export function getAdminYoutubeLogs(params = {}) {
  return request(`/admin/youtube/logs${toQueryString(params)}`);
}

export function getAdminYoutubeVideos(params = {}) {
  return request(`/admin/youtube/videos${toQueryString(params)}`);
}

export function getAdminYoutubeKeywords(params = {}) {
  return request(`/admin/youtube/keywords${toQueryString(params)}`);
}

export function getAdminYoutubeDashboard() {
  return request('/admin/youtube/dashboard');
}

export function collectAdminYoutube(keyword) {
  const query = toKeywordQuery(keyword);
  return request(`/admin/youtube/collect${query}`, {
    method: 'POST',
  });
}

export function collectAdminYoutubeComments(keyword) {
  const query = toKeywordQuery(keyword);
  return request(`/admin/youtube/comments/collect${query}`, {
    method: 'POST',
  });
}

export function getYoutubeTopGames(limit = 10) {
  return request(`/api/youtube/top-games${toQueryString({ limit })}`);
}

export function getYoutubeTrend(keyword) {
  return request(`/api/youtube/trends${toQueryString({ keyword })}`);
}

export function getOAuthAuthorizationUrl(provider) {
  return `${OAUTH_BASE_URL}/oauth2/authorization/${provider}`;
}

export function createConversation(payload = {}) {
  return request('/api/conversations', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getConversations() {
  return request('/api/conversations');
}

export function getConversation(conversationId) {
  return request(`/api/conversations/${conversationId}`);
}

export function updateConversation(conversationId, payload) {
  return request(`/api/conversations/${conversationId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function deleteConversation(conversationId) {
  return request(`/api/conversations/${conversationId}`, {
    method: 'DELETE',
  });
}

export function createGame(payload) {
  return request('/api/games', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function analyzeOnboarding(payload, options = {}) {
  return request('/api/onboarding/analyze', {
    method: 'POST',
    body: JSON.stringify(payload),
    signal: options.signal,
  });
}

export function getOnboardingHistories() {
  return request('/api/onboarding/history');
}

export function getOnboardingHistory(id) {
  return request(`/api/onboarding/history/${id}`);
}

export function deleteOnboardingHistory(id) {
  return request(`/api/onboarding/history/${id}`, {
    method: 'DELETE',
  });
}

export function importSteamGame(payload) {
  return request('/api/games/import/steam', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getGames() {
  return request('/api/games');
}

export function getRecommendations() {
  return request('/api/games/recommendations');
}

export function getSteamReview(appId) {
  return request(`/api/steam/reviews/${appId}`);
}

export function createReportDraft(recommendationLimit = 5) {
  return request('/api/reports/draft', {
    method: 'POST',
    body: JSON.stringify({ recommendationLimit }),
  });
}

export function refreshTrendData(payload = {}) {
  return request('/api/trends/refresh', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getTrendGames() {
  return request('/api/trends/games');
}

export function getTopTrendGames(limit = 5) {
  return request(`/api/trends/games/top?limit=${limit}`);
}

export function getTrendGame(id) {
  return request(`/api/trends/games/${id}`);
}

export function refreshLiveTrendData() {
  return request('/api/live-trends/refresh', {
    method: 'POST',
  });
}

export function getLiveTrendStatus() {
  return request('/api/live-trends/status');
}

export function getTopLiveTrendGames(limit = 5, platform = 'all') {
  const params = new URLSearchParams({ limit: String(limit) });
  if (platform && platform !== 'all') {
    params.set('platform', platform);
  }
  return request(`/api/live-trends/games/top?${params.toString()}`);
}

export function getLiveTrendRankings({ platform = 'all', sort = 'TREND_SCORE', limit = 50 } = {}) {
  const params = new URLSearchParams({
    sort,
    limit: String(limit),
  });
  if (platform && platform !== 'all') {
    params.set('platform', platform);
  }
  return request(`/api/live-trends/rankings?${params.toString()}`);
}
