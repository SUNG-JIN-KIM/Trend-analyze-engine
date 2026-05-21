# Trend Analyze Engine

게임 시장 트렌드와 라이브 방송 신호를 분석하고, 사용자 질문에 맞는 게임 추천과 개발 아이디어를 제공하는 포트폴리오 프로젝트입니다.

백엔드는 Spring Boot 기반의 REST API, 프론트엔드는 React/Vite 기반 대시보드로 구성되어 있습니다. 로컬 LLM은 Ollama 또는 LM Studio의 OpenAI 호환 API를 사용하고, 최종 런타임 모델은 GEMMA4 E2B를 기준으로 합니다.

## 핵심 기능

- 게임 트렌드 점수 조회 및 추천
- 라이브 트렌드 수집 상태 조회
- 자연어 기반 게임 추천/분석 Agent
- 로그인 사용자 대화 기록 저장 및 본인 기록 조회
- 이메일 승인 기반 관리자 권한 신청
- 관리자 전용 로그인과 관리자 콘솔
- 관리자 사용자 관리, 승인 요청 관리, 대화 숨김/복구/soft delete
- 관리자 활동 audit log

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 4, Spring Web MVC, Spring Security, Spring Data JDBC |
| Database | H2 local, PostgreSQL runtime |
| Build | Gradle |
| Frontend | React 19, Vite |
| Auth | JWT, BCrypt |
| LLM | Ollama 또는 LM Studio OpenAI-compatible API, GEMMA4 E2B |
| Infra | Docker, Docker Compose, GitHub Actions |

## 프로젝트 구조

```text
.
├── backend/
│   └── trend-agent/
│       └── trend-agent/          # Spring Boot API
├── frontend/
│   └── game-trend-dashboard/     # React/Vite dashboard
├── docs/                         # 기획/API 문서
├── ai-lab/                       # 프롬프트 실험 자료
├── .github/workflows/            # CI/CD workflow
├── docker-compose.yml
└── README.md
```

## 로컬 실행

### Backend

```bash
cd backend/trend-agent/trend-agent
./gradlew bootRun
```

기본 DB는 H2 file DB입니다. 운영/컨테이너 환경에서는 PostgreSQL 환경변수를 사용합니다.

### Frontend

```bash
cd frontend/game-trend-dashboard
npm ci
npm run dev
```

## 환경변수 설정

민감정보는 코드에 직접 작성하지 않습니다. 로컬에서는 OS 환경변수 또는 커밋하지 않는 `.env`류 파일을 사용하세요.

### Backend 필수/권장 환경변수

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/trend_agent
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
SPRING_DATASOURCE_USERNAME=trend_agent
SPRING_DATASOURCE_PASSWORD=[MASKED]
AUTH_JWT_SECRET=[MASKED]
APP_FRONTEND_URL=http://localhost:5173
ADMIN_OWNER_EMAIL=owner@example.com
ADMIN_MAIL_ENABLED=false
LLM_BASE_URL=http://localhost:11434/v1
LLM_MODEL=gemma4:e2b
```

관리자 승인 이메일의 수신자는 서버에서 `ksjcloud98@gmail.com`으로 고정됩니다.

### SMTP 발송 사용 시

```properties
ADMIN_MAIL_ENABLED=true
ADMIN_MAIL_FROM=sender@example.com
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=[MASKED]
MAIL_PASSWORD=[MASKED]
```

### Frontend 환경변수

```properties
VITE_API_BASE_URL=http://localhost:8080
```

## Docker 실행

```bash
docker compose up --build
```

실행 전 다음 값은 환경변수로 설정해야 합니다.

```properties
DB_PASSWORD=[MASKED]
AUTH_JWT_SECRET=[MASKED]
```

## API 문서

초기 API 문서는 `docs/03_api_spec.md`를 기준으로 관리합니다. 추후 OpenAPI/Swagger 문서 생성을 추가할 예정입니다.

## GitHub Actions

- `.github/workflows/ci.yml`
  - `main` 브랜치 push와 pull request에서 실행
  - Backend: Java 21 설정, Gradle test, bootJar
  - Frontend: Node.js 22 설정, npm ci, Vite build

- `.github/workflows/cd.yml`
  - `main` 브랜치 push에서 Docker build 검증
  - 실제 배포 서버/레지스트리 정보는 TODO로 남김

## 향후 개발 계획

- OpenAPI 문서 자동화
- 관리자 대시보드 통계 고도화
- 대화 신고 기능과 moderation workflow 강화
- 운영 DB migration 전략 정리
- Docker image registry push와 서버 배포 자동화
- 프론트 관리자 화면 UX 개선

## 배포 계획

1. GitHub Actions CI로 테스트와 빌드 검증
2. Docker image build
3. Docker registry push
4. 서버에서 환경변수/GitHub Secrets 기반 배포
5. 배포 후 health check 및 rollback 절차 추가

## 필요한 GitHub Secrets 후보

```text
AUTH_JWT_SECRET
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
ADMIN_OWNER_EMAIL
ADMIN_MAIL_FROM
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
DEPLOY_HOST
DEPLOY_USER
DEPLOY_SSH_KEY
```

## 보안 원칙

- 실제 API 키, DB 비밀번호, 토큰, 시크릿 값은 커밋하지 않습니다.
- `.env`, `application-secret.yml`, `application-local.yml`은 커밋하지 않습니다.
- 관리자 권한은 이메일 승인 토큰을 통해서만 부여합니다.
- 일반 사용자는 본인 대화기록만 조회하고, 관리자는 관리자 API에서 전체 대화를 관리합니다.
