# 로컬 회원가입 및 로그인

기본 프로필은 `mysql,spring-data-jpa`입니다. MySQL 연결 정보는
`src/main/resources/application-mysql.properties`에 있으며 `MYSQL_URL`,
`MYSQL_USER`, `MYSQL_PASS` 환경 변수로 덮어쓸 수 있습니다.

백엔드를 재시작하고 프론트엔드에서 새 아이디로 회원가입한 뒤 같은 비밀번호로
로그인하세요. 프론트엔드 개발 서버의 `VITE_API_PROXY_TARGET`은
`http://localhost:9966`으로 설정합니다. 로그인 성공 시 대시보드로 이동합니다.

| 요청 | 동작 |
| --- | --- |
| `GET /petclinic/api/auth/csrf` | 변경 요청에 사용할 CSRF 토큰 발급 |
| `POST /petclinic/api/auth/register` | JSON `username`, `password`로 가입, 성공 201, 중복 409 |
| `POST /petclinic/api/auth/login` | 폼 인코딩 `username`, `password`로 로그인, 성공 204, 실패 401 |
| `GET /petclinic/api/auth/me` | 로그인 계정 및 권한 조회, 미인증 401 |
| `POST /petclinic/api/auth/logout` | 서버 세션 종료, 성공 204 |

프론트엔드는 변경 요청 전에 CSRF 토큰을 받고 해당 헤더를 전송합니다.
로그인은 HttpOnly 세션 쿠키로 유지하며 비밀번호를 브라우저 저장소에 저장하지 않습니다.
서버 재시작 후 계정은 MySQL에 유지되지만 세션은 다시 로그인해야 합니다.
외부 서비스로 배포할 때는 HTTPS와 Secure 세션 쿠키를 설정하세요.

신규 계정은 `ROLE_USER`만 부여됩니다. 화면의 보호자/수의사 선택은 아직 계정 유형을
저장하거나 관리 권한을 부여하지 않습니다. 기존 관리 API는 관리자 권한을 요구합니다.
`POST /petclinic/api/users`는 관리자용 API로 유지합니다.

신규 비밀번호는 BCrypt로 해시하여 저장합니다. 이전에 평문으로 저장한 계정은
이 로그인 방식으로 인증되지 않으므로 새 아이디로 가입하거나 별도 비밀번호 재설정이 필요합니다.
기존 계정의 비밀번호를 자동 변경하지 않습니다.

MySQL Workbench에서 저장 여부를 확인할 수 있습니다.

```sql
SELECT username, enabled FROM petclinic.users;
SELECT username, role FROM petclinic.roles;
```

인증 회귀 테스트:

```powershell
.\mvnw.cmd '-Dtest=AuthenticationIntegrationTests,UserRestControllerV1Tests,UserServiceSpringDataJpaTests' test
```
