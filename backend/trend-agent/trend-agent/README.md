# Game Trend Insight Agent Backend

## 로컬 H2 파일 DB

개발 환경의 기본 datasource는 H2 file DB입니다.

```yaml
spring.datasource.url=jdbc:h2:file:./data/trend_agent;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DEFAULT_NULL_ORDERING=HIGH
```

서버 시작 시 `data` 폴더가 없으면 자동으로 생성됩니다. 수동으로 만들고 싶다면 `backend/trend-agent/trend-agent`에서 `mkdir data`를 실행하면 됩니다.

H2 console은 기존처럼 `/h2-console` 경로를 사용합니다.

운영 환경에서 PostgreSQL을 사용하려면 아래 환경변수를 실행 환경에 설정합니다.

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/trend_agent
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your-password
```

## 전화번호 인증 SMS 설정

현재 회원가입은 이메일, 비밀번호, 닉네임만 사용하며 전화번호 인증은 회원가입 흐름에서 비활성화되어 있습니다.
`/api/auth/phone/send-code`, `/api/auth/phone/verify`, `SmsSender` 구조는 나중에 다시 붙일 수 있도록 남겨둔 상태입니다.

전화번호 인증을 다시 활성화할 때 실제 문자 발송은 Naver Cloud SENS 설정을 사용합니다.
로컬에서 로그 출력만 원할 때는 `SPRING_PROFILES_ACTIVE=local` 또는 `SMS_PROVIDER=mock`을 사용합니다.

```properties
SMS_PROVIDER=naver-sens
SMS_API_KEY=your-naver-cloud-access-key
SMS_API_SECRET=your-naver-cloud-secret-key
SMS_SERVICE_ID=ncp:sms:kr:...
SMS_FROM_NUMBER=01012345678
SMS_BASE_URL=https://sens.apigw.ntruss.com
AUTH_PHONE_CODE_EXPIRATION_MINUTES=5
AUTH_PHONE_RESEND_COOLDOWN_SECONDS=60
AUTH_PHONE_DAILY_REQUEST_LIMIT=5
```

SMS 기능 재활성화 시 수동 테스트:

1. `POST /api/auth/phone/send-code`에 `{ "phoneNumber": "01012345678" }`를 보냅니다.
2. 실제 휴대폰으로 인증번호가 도착하는지 확인합니다.
3. `POST /api/auth/phone/verify`에 `{ "phoneNumber": "01012345678", "code": "123456" }`를 보냅니다.
4. 60초 안에 재요청하면 `PHONE_CODE_RESEND_TOO_SOON`, 일일 제한 초과 시 `PHONE_CODE_DAILY_LIMIT_EXCEEDED`가 나와야 합니다.
5. 현재 `POST /api/auth/register`는 전화번호와 인증번호 없이 `{ "email": "...", "password": "...", "nickname": "..." }`만으로 가입됩니다.

## 관리자 승인/대화기록 보안 테스트

관리자 승인 요청 이메일 수신자는 서버에서 `ksjcloud98@gmail.com`으로 고정됩니다.
`ADMIN_APPROVAL_EMAIL` 환경변수를 다른 값으로 넣어도 승인 요청 저장값과 발송 대상은 이 주소만 사용합니다.
실제 SMTP 발송을 사용하려면 아래 환경변수를 설정합니다. 설정하지 않으면 로컬 개발용 로그 발송 구현체가 동작합니다.

```properties
ADMIN_MAIL_ENABLED=true
ADMIN_MAIL_FROM=your-sender@example.com
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=your-smtp-username
MAIL_PASSWORD=your-smtp-password
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
```

수동 테스트:

1. 일반 USER로 로그인 후 `POST /api/admin-approval/request`를 호출합니다.
2. 승인 메일 본문에 `/admin/approval/approve?token=...`, `/admin/approval/reject?token=...` 링크가 포함되는지 확인합니다.
3. 승인 링크를 한 번 클릭하면 USER의 role이 ADMIN으로 바뀌는지 확인합니다.
4. 같은 승인 링크를 다시 호출하면 토큰 재사용 오류가 나와야 합니다.
5. 일반 USER 토큰으로 `/api/admin/dashboard`, `/api/admin/users`, `/api/admin/conversations` 접근 시 403이 나와야 합니다.
6. ADMIN 또는 OWNER 토큰으로 `/api/admin/conversations`에서 전체 대화를 조회하고, hide/restore/delete가 audit log에 남는지 확인합니다.
7. 일반 USER 토큰으로 `/api/conversations` 또는 `/api/onboarding/history`를 호출하면 본인 데이터만 조회되어야 합니다.
8. 다른 사용자의 대화 ID를 `/api/conversations/{id}`로 직접 호출하면 `CONVERSATION_NOT_FOUND`가 나와야 합니다.
