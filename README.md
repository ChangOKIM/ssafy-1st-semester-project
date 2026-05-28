# ssafy-1st-semester-project

SSAFY 1학기 관통 프로젝트입니다.

DART 기반 초보자용 기업 정보 및 추천 서비스를 목표로 합니다. 투자 초보자가 어려운 기업 공시, 재무 정보, 기업 리포트를 쉽게 이해할 수 있도록 돕고, 사용자 투자 성향에 맞는 기업 추천을 제공합니다.

## 주요 기능

- 회원가입 / 로그인
- 투자자 프로필 입력
- 기업 검색
- 기업 리포트 조회
- AI 초보자 요약 영역
- 개인화 기업 추천

초기 MVP는 더미 데이터 기반으로 전체 흐름을 먼저 완성한 뒤, DART API, AI API, 시세 API를 순차적으로 연동합니다.

## 기술 스택

### Backend

- Java 21
- Spring Boot
- Gradle
- Spring Web
- Spring Data JPA
- Spring Security
- MySQL
- Lombok
- Validation
- JWT 인증

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
 ├── backend/
 ├── docs/
 ├── frontend/
 ├── .gitignore
 └── README.md
```

## 백엔드 실행 준비

### 1. JDK 확인

백엔드는 Java 21 기준입니다.

```powershell
java -version
```

현재 PC에서 JDK 21 경로를 직접 지정해야 하면:

```powershell
$env:JAVA_HOME='C:\Users\SSAFY\.jdks\ms-21.0.11'
```

### 2. MySQL Workbench에서 DB 준비

MySQL Workbench에서 `Local instance MySQL80`에 접속한 뒤 아래 SQL을 실행합니다.

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

백엔드 기본 DB 접속 정보:

```text
host: 127.0.0.1
port: 3306
database: dart_service
username: ssafy
password: ssafy
```

### 3. 백엔드 실행

```powershell
cd backend
.\gradlew.bat bootRun
```

정상 실행 후 확인:

```powershell
Invoke-WebRequest http://localhost:8080/api/v1/health
```

응답:

```text
backend ok
```

## 인증 API

### 회원가입

```http
POST /api/v1/auth/signup
Content-Type: application/json
```

```json
{
  "email": "test@example.com",
  "password": "password1234",
  "name": "테스트"
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

## 프론트엔드 실행

```powershell
cd frontend
npm install
npm run dev
```

기본 주소:

```text
http://localhost:5173
```

## 공통 응답 형식

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

## Git 주의사항

다음 파일과 디렉터리는 Git에 올리지 않습니다.

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
