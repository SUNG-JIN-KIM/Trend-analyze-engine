# 서버 재시작 후 로그인 무효화 테스트

## 목적

백엔드 서버가 종료되었다가 다시 실행되면 기존 브라우저의 `JSESSIONID`, `remember-me`, localStorage JWT가 남아 있어도 인증 상태로 인정하지 않는지 확인한다.

## 확인 전제

- `server.servlet.session.persistent=false`
- `server.servlet.session.timeout=30m`
- Spring Security remember-me 비활성화
- logout 시 `JSESSIONID`, `remember-me` 쿠키 삭제
- JWT는 현재 서버 부팅 이후 발급된 토큰만 유효

## 테스트 절차

1. 백엔드 실행
   ```bash
   cd backend/trend-agent/trend-agent
   ./gradlew bootRun
   ```

2. 프론트 실행
   ```bash
   cd frontend/game-trend-dashboard
   npm run dev
   ```

3. 일반 사용자로 로그인한 뒤 `/history` 또는 `/my/conversations` 접근을 확인한다.

4. 관리자 계정으로 로그인한 뒤 `/admin/dashboard` 접근을 확인한다.

5. 백엔드 서버를 종료한다.

6. 백엔드 서버를 다시 실행한다.

7. 브라우저 새로고침 후 다음을 확인한다.
   - `/admin/dashboard` 접근 시 관리자 로그인 화면으로 이동한다.
   - `/my/conversations` 접근 시 일반 로그인 화면으로 이동한다.
   - `/api/auth/me` 호출은 `401 AUTH_REQUIRED`를 반환한다.
   - 프론트 localStorage의 기존 access token은 401 응답 이후 제거된다.

8. 로그아웃 버튼을 누른 뒤 브라우저 개발자 도구에서 다음 쿠키가 삭제되는지 확인한다.
   - `JSESSIONID`
   - `remember-me`

## 주의

이 프로젝트는 기본 인증에 JWT를 사용한다. 서버 재시작 후 자동 로그인처럼 보이는 주된 원인은 브라우저 localStorage에 남은 access token이다. 현재 구현은 서버 부팅마다 바뀌는 내부 식별자를 JWT 검증에 포함해, 이전 서버 인스턴스에서 발급된 토큰을 거부한다.
