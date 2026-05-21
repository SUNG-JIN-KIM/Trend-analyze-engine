# AGENTS.md

## 프로젝트

Trend Analyze Engine은 게임 트렌드 분석, 자연어 기반 게임 추천, 관리자 승인/관리 기능을 제공하는 Spring Boot + React 포트폴리오 프로젝트입니다.

## 기본 작업 대상

- Backend: `backend/trend-agent/trend-agent`
- Frontend: `frontend/game-trend-dashboard`
- `backend/src` 아래의 미완성 스텁 트리는 명시 요청 없이는 수정하지 않습니다.

## 기술 규칙

- Java 21을 사용합니다.
- Spring Boot 구조를 유지합니다.
- Backend 계층은 Controller, Service, Repository, DTO, Entity를 분리합니다.
- Spring Data JDBC 패턴을 우선 사용합니다.
- Lombok을 사용할 수 있습니다.
- Gradle 기반 빌드 구조를 유지합니다.
- Frontend는 React/Vite 구조를 유지합니다.

## 보안 규칙

- 실제 API 키, DB 비밀번호, 토큰, 시크릿 값을 코드나 문서에 직접 작성하지 않습니다.
- 민감값을 설명해야 할 때는 `[MASKED]`로만 표시합니다.
- `.env`, `.env.*`, `application-secret.yml`, `application-local.yml`은 커밋하지 않습니다.
- 환경변수 또는 GitHub Secrets 기반 설정을 우선 사용합니다.
- 관리자 권한은 이메일 승인 토큰으로만 부여합니다.
- 일반 사용자가 직접 ADMIN/OWNER 권한을 얻는 API를 만들지 않습니다.
- 프론트에서 버튼을 숨기는 것만으로 보안을 처리하지 않습니다. 서버 Controller/Service 단계에서 권한을 검증합니다.

## LLM 연동 규칙

- 런타임 LLM은 GEMMA4 E2B를 기준으로 합니다.
- Ollama 또는 LM Studio의 OpenAI 호환 API를 사용합니다.
- OpenAI SDK, Anthropic SDK, Claude SDK를 추가하지 않습니다.
- Claude/Anthropic API를 서비스 런타임에 연결하지 않습니다.

## 테스트 규칙

- Backend 변경 후 가능하면 `./gradlew test`를 실행합니다.
- 컴파일 확인만 필요한 경우 `./gradlew compileJava`를 실행합니다.
- Frontend 변경 후 `npm run build`를 실행합니다.
- 인증/권한/관리자 기능 변경 시 테스트 코드 또는 수동 테스트 방법을 문서화합니다.

## Git 규칙

- 기본 브랜치는 `main`입니다.
- 커밋 메시지는 명확한 명령형 또는 요약형으로 작성합니다.
- 예시:
  - `feat: add admin approval workflow`
  - `fix: restrict conversation access by owner`
  - `docs: update deployment guide`
  - `chore: configure CI pipeline`
- `git push`는 사용자의 명시 요청이 있을 때만 실행합니다.
- 기존 사용자의 변경사항을 임의로 되돌리지 않습니다.

## GitHub Issue/PR 관리 규칙

- 기능 추가는 가능하면 Issue로 요구사항을 정리한 뒤 PR로 연결합니다.
- PR에는 변경 요약, 테스트 결과, 보안 영향 여부를 포함합니다.
- 인증, 권한, 배포, 환경변수 변경은 PR 설명에 검증 방법을 적습니다.
- 민감정보가 포함된 로그나 스크린샷은 PR에 올리지 않습니다.

## 응답 언어

- 설명, 계획, 변경 요약, 테스트 방법은 한국어로 작성합니다.
- 코드의 클래스명, 메서드명, 변수명은 Java/Spring/React 관례에 맞게 영어를 사용합니다.
