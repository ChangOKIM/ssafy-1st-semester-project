# ssafy-1st-semester-project

SSAFY 1학기 관통 프로젝트입니다.

## 프로젝트 소개

`DartPoint AI`는 DART 공시와 기업 정보를 초보 투자자도 이해하기 쉽게 정리하고, 사용자의 투자성향을 바탕으로 기업 추천을 제공하는 서비스입니다.

현재 MVP에서는 아래 흐름을 우선 구현합니다.

```text
회원가입
-> 투자성향 입력
-> 메인 화면
-> 로그인
```

## 주요 기능

- 회원가입 / 로그인
- JWT 기반 인증
- 투자성향 입력
- 투자 가능 총 금액 입력
- 관심 섹터 복수 선택
- DART 기반 기업 검색/추천 메인 화면

## 기술 스택

### Backend

- Java 21
- Spring Boot
- Gradle
- Spring Web
- Spring Security
- MyBatis
- MySQL
- Validation
- JWT

### Frontend

- Vue 3
- Vite
- Vue Router
- Pinia
- Axios

### Database

- MySQL
- Database name: `dart_service`

## 프로젝트 구조

```text
ssafy-1st-semester-project/
├─ backend/
│  ├─ src/main/java/com/ssafy/dartservice/
│  │  ├─ auth/
│  │  ├─ global/
│  │  ├─ investor/
│  │  └─ user/
│  └─ src/main/resources/
│     ├─ application.properties
│     └─ schema.sql
├─ frontend/
│  └─ src/
│     ├─ api/
│     ├─ components/
│     ├─ router/
│     ├─ stores/
│     └─ views/
├─ docs/
└─ README.md
```

## 1. 개발 환경 준비

### Java 확인

백엔드는 Java 21 기준입니다.

```powershell
java -version
```

만약 터미널에서 Java 8 등 낮은 버전이 잡히면 JDK 21 경로를 지정합니다.

예시:

```powershell
$env:JAVA_HOME='C:\Users\SSAFY\.jdks\ms-21.0.11'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version
```

IntelliJ에서도 Project SDK를 Java 21로 맞춰주세요.

## 2. MySQL DB 준비

MySQL Workbench에서 로컬 MySQL에 접속한 뒤 아래 SQL을 실행합니다.

```sql
CREATE DATABASE IF NOT EXISTS dart_service
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'ssafy'@'localhost'
IDENTIFIED BY 'ssafy';

CREATE USER IF NOT EXISTS 'ssafy'@'127.0.0.1'
IDENTIFIED BY 'ssafy';

GRANT ALL PRIVILEGES ON dart_service.* TO 'ssafy'@'localhost';
GRANT ALL PRIVILEGES ON dart_service.* TO 'ssafy'@'127.0.0.1';

FLUSH PRIVILEGES;
```

기본 DB 접속 정보는 아래와 같습니다.

```text
host: 127.0.0.1
port: 3306
database: dart_service
username: ssafy
password: ssafy
```

## 3. 테이블 생성 방식

현재 백엔드는 MyBatis를 사용합니다.

JPA/Hibernate의 `ddl-auto`로 테이블을 만드는 구조가 아닙니다.

대신 앱 실행 시 아래 파일이 실행되어 테이블을 생성합니다.

```text
backend/src/main/resources/schema.sql
```

현재 설정:

```properties
spring.sql.init.mode=always
mybatis.configuration.map-underscore-to-camel-case=true
```

즉, `dart_service` 스키마만 먼저 만들어두면 앱 실행 시 아래 테이블이 자동 생성됩니다.

```text
users
users_profile
```

주의: `CREATE TABLE IF NOT EXISTS` 방식이라 이미 있는 테이블 구조를 자동 수정하지는 않습니다. 테이블 구조를 바꾼 경우에는 Workbench에서 직접 확인하거나 기존 테스트 테이블을 삭제해야 할 수 있습니다.

## 4. DB 구조

### users

회원 로그인과 기본 정보를 저장합니다.

```text
id BIGINT PK AUTO_INCREMENT
email VARCHAR(100) NOT NULL UNIQUE
password VARCHAR(255) NOT NULL
name VARCHAR(50) NOT NULL
role VARCHAR(20) NOT NULL
created_at DATETIME NOT NULL
```

### users_profile

투자성향과 추천에 필요한 정보를 저장합니다.

```text
id BIGINT PK AUTO_INCREMENT
user_id BIGINT NOT NULL UNIQUE FK -> users.id
investment_experience VARCHAR(30) NOT NULL
risk_tolerance VARCHAR(30) NOT NULL
investment_goal VARCHAR(30) NOT NULL
investable_amount DECIMAL(15,0) NOT NULL
preferred_sectors VARCHAR(255) NOT NULL
created_at DATETIME NOT NULL
updated_at DATETIME NOT NULL
```

관심 섹터는 프론트에서는 배열로 관리합니다.

```javascript
['반도체', '금융', 'IT']
```

DB에는 MVP 단계라서 콤마 문자열로 저장합니다.

```text
반도체,금융,IT
```

나중에 섹터별 검색/통계/추천 가중치가 중요해지면 `users_profile_sectors` 같은 별도 테이블로 분리하는 것이 좋습니다.

## 5. 백엔드 실행

터미널에서 실행:

```powershell
cd backend
.\gradlew.bat bootRun
```

IntelliJ에서는 아래 파일을 실행해도 됩니다.

```text
backend/src/main/java/com/ssafy/dartservice/DartServiceApplication.java
```

정상 실행 확인:

```powershell
Invoke-WebRequest http://localhost:8080/api/v1/health
```

응답:

```text
backend ok
```

Workbench에서 테이블 확인:

```sql
USE dart_service;

SHOW TABLES;

DESC users;
DESC users_profile;
```

## 6. 프론트엔드 실행

처음 클론한 뒤에는 의존성을 설치합니다.

```powershell
cd frontend
npm install
```

개발 서버 실행:

```powershell
npm run dev
```

기본 접속 주소:

```text
http://localhost:5173
```

주요 화면:

```text
/signup
/login
/investor-profile
/companies
```

프론트에서 `/api` 요청은 Vite proxy를 통해 백엔드 `http://localhost:8080`으로 전달됩니다.

## 7. 실행 순서

새로 프로젝트를 받은 팀원은 아래 순서대로 진행하면 됩니다.

```text
1. MySQL 실행
2. Workbench에서 dart_service DB 생성
3. IntelliJ에서 backend 실행
4. VS Code 또는 IntelliJ Terminal에서 frontend 실행
5. 브라우저에서 http://localhost:5173 접속
```

권장 테스트 흐름:

```text
/signup 접속
-> 이메일, 비밀번호, 이름 입력
-> 회원가입
-> 투자성향 입력
-> /companies 이동
-> Workbench에서 users, users_profile 확인
```

## 8. API

### 회원가입

```http
POST /api/v1/auth/signup
Content-Type: application/json
```

```json
{
  "email": "test@example.com",
  "password": "password1234",
  "name": "김싸피"
}
```

### 로그인

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "test@example.com",
  "password": "password1234"
}
```

### 내 정보 조회

```http
GET /api/v1/users/me
Authorization: Bearer {accessToken}
```

### 투자성향 저장

```http
PUT /api/v1/users/me/investor-profile
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "investmentExperience": "BEGINNER",
  "riskTolerance": "LOW",
  "investmentGoal": "STABLE_GROWTH",
  "investableAmount": 1000000,
  "preferredSectors": ["반도체", "금융", "IT"]
}
```

## 9. 응답 형식

성공:

```json
{
  "success": true,
  "data": {}
}
```

실패:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  }
}
```

## 10. DB 확인 쿼리

회원 확인:

```sql
USE dart_service;

SELECT id, email, name, role, created_at
FROM users
ORDER BY id DESC;
```

투자성향 확인:

```sql
SELECT *
FROM users_profile
ORDER BY id DESC;
```

회원과 투자성향 같이 보기:

```sql
SELECT
  u.id AS user_id,
  u.email,
  u.name,
  p.investment_experience,
  p.risk_tolerance,
  p.investment_goal,
  p.investable_amount,
  p.preferred_sectors,
  p.created_at AS profile_created_at,
  p.updated_at AS profile_updated_at
FROM users u
LEFT JOIN users_profile p
  ON u.id = p.user_id
ORDER BY u.id DESC;
```

## 11. Git 주의사항

아래 파일/폴더는 Git에 올리지 않습니다.

```text
.env
.idea
backend/.gradle
backend/build
frontend/node_modules
frontend/dist
```

커밋 전 확인:

```powershell
git status --short
```

## 12. 현재 구현 참고

- 백엔드는 MyBatis 기반입니다.
- `users`, `users_profile` 테이블은 `schema.sql`로 생성합니다.
- 비밀번호 암호화는 백엔드에서 BCrypt로 처리합니다.
- 로그인/회원가입 성공 시 accessToken을 localStorage에 저장합니다.
- 프론트 인증 상태는 Pinia `authStore`에서 관리합니다.
- `/companies`는 현재 메인 화면 역할을 합니다.
