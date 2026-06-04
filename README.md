# DartPoint AI

SSAFY 1학기 관통 프로젝트입니다. DART, KRX, 한국투자증권 API와 사용자의 투자 성향 데이터를 바탕으로 초보 투자자가 이해하기 쉬운 종목 검색, 종목 분석, 맞춤 추천, 보유 종목 현황을 제공하는 서비스입니다.

## 프로젝트 개요

`DartPoint AI`는 다음 흐름을 목표로 합니다.

```text
회원가입/로그인
-> 투자자 프로필 등록
-> 종목 검색 및 종목 리포트 조회
-> AI 추천 확인
-> 보유 종목 등록 및 투자 진단 확인
-> 마이페이지에서 내 정보와 투자 성향 관리
```

## 기술 스택

### Backend

- Java 21
- Spring Boot 3.5
- Spring Security
- JWT
- MyBatis
- MySQL
- Springdoc OpenAPI
- KRX Open API
- 한국투자증권 Open API

### Frontend

- Vue 3
- Vite
- Vue Router
- Pinia
- Axios

### Database

- MySQL
- Database: `dart_service`

## 프로젝트 구조

```text
ssafy-1st-semester-project/
├─ backend/
│  ├─ src/main/java/com/ssafy/dartservice/
│  │  ├─ auth/
│  │  ├─ global/
│  │  ├─ investor/
│  │  ├─ report/
│  │  ├─ stock/
│  │  └─ user/
│  └─ src/main/resources/
│     ├─ application.properties
│     ├─ schema.sql
│     └─ mapper/
├─ frontend/
│  └─ src/
│     ├─ api/
│     ├─ components/
│     ├─ router/
│     ├─ stores/
│     └─ views/
└─ README.md
```

## 실행 준비

### 1. Java 21 확인

```powershell
java -version
```

백엔드는 Java 21 기준입니다.

### 2. MySQL 데이터베이스 생성

```sql
CREATE DATABASE IF NOT EXISTS dart_service
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'ssafy'@'localhost' IDENTIFIED BY 'ssafy';
CREATE USER IF NOT EXISTS 'ssafy'@'127.0.0.1' IDENTIFIED BY 'ssafy';

GRANT ALL PRIVILEGES ON dart_service.* TO 'ssafy'@'localhost';
GRANT ALL PRIVILEGES ON dart_service.* TO 'ssafy'@'127.0.0.1';

FLUSH PRIVILEGES;
```

기본 접속 정보:

```text
host: 127.0.0.1
port: 3306
database: dart_service
username: ssafy
password: ssafy
```

### 3. 외부 API 키 환경변수

실제 키는 Git에 올리지 않습니다. 로컬 환경변수로 등록합니다.

```powershell
[Environment]::SetEnvironmentVariable('KRX_API_KEY', '발급받은_KRX_KEY', 'User')
[Environment]::SetEnvironmentVariable('KIS_APP_KEY', '발급받은_KIS_APP_KEY', 'User')
[Environment]::SetEnvironmentVariable('KIS_APP_SECRET', '발급받은_KIS_SECRET_KEY', 'User')
```

`application.properties`는 환경변수만 참조합니다.

```properties
krx.api-key=${KRX_API_KEY:}
krx.base-url=https://openapi.krx.co.kr

kis.app-key=${KIS_APP_KEY:}
kis.app-secret=${KIS_APP_SECRET:}
kis.base-url=https://openapi.koreainvestment.com:9443
```

환경변수 등록 후에는 IntelliJ 또는 터미널을 다시 시작해야 Spring Boot가 값을 읽을 수 있습니다.

## 실행 방법

### Backend

```powershell
cd backend
.\gradlew.bat bootRun
```

헬스 체크:

```text
GET http://localhost:8080/api/v1/health
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

### Frontend

```powershell
cd frontend
npm install
npm run dev
```

접속 주소:

```text
http://localhost:5173
```

## DB 구조

### users

회원 기본 정보와 인증 정보를 저장합니다.

```sql
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
);
```

### users_profile

투자 성향, 관심 분야, 투자 목표를 저장합니다.

```sql
CREATE TABLE IF NOT EXISTS users_profile (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    investment_experience VARCHAR(30) NOT NULL,
    risk_tolerance VARCHAR(30) NOT NULL,
    investment_goal VARCHAR(30) NOT NULL,
    investable_amount DECIMAL(15, 0) NOT NULL,
    preferred_sectors VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_profile_user_id (user_id),
    CONSTRAINT fk_users_profile_user FOREIGN KEY (user_id) REFERENCES users (id)
);
```

### stocks

KRX에서 가져온 종목 기본 정보를 저장합니다.

```sql
CREATE TABLE IF NOT EXISTS stocks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(10) NOT NULL,
    stock_name VARCHAR(100) NOT NULL,
    market VARCHAR(10) NOT NULL,
    sector VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stocks_code (stock_code),
    INDEX idx_stocks_name (stock_name),
    INDEX idx_stocks_sector (sector)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### stock_financials

DART 기반 종목 재무 정보를 저장합니다.

```sql
CREATE TABLE IF NOT EXISTS stock_financials (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(10) NOT NULL,
    base_year YEAR NOT NULL,
    revenue BIGINT,
    operating_profit BIGINT,
    net_income BIGINT,
    total_assets BIGINT,
    total_debt BIGINT,
    debt_ratio DECIMAL(6, 2),
    fetched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_financials_code_year (stock_code, base_year),
    CONSTRAINT fk_financials_stock FOREIGN KEY (stock_code) REFERENCES stocks (stock_code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### stock_reports

종목별 AI 분석 리포트를 저장합니다.

```sql
CREATE TABLE IF NOT EXISTS stock_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_code VARCHAR(10) NOT NULL,
    content TEXT NOT NULL,
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_reports_code_date (stock_code, generated_at),
    INDEX idx_reports_code (stock_code),
    CONSTRAINT fk_reports_stock FOREIGN KEY (stock_code) REFERENCES stocks (stock_code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### recommendations

사용자별 맞춤 추천 종목을 저장합니다.

```sql
CREATE TABLE IF NOT EXISTS recommendations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    stock_code VARCHAR(10) NOT NULL,
    rec_type VARCHAR(20) NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    reason TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rec_user_stock_type (user_id, stock_code, rec_type),
    INDEX idx_rec_user (user_id),
    CONSTRAINT fk_rec_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_rec_stock FOREIGN KEY (stock_code) REFERENCES stocks (stock_code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### holdings

사용자 보유 종목과 매입 정보를 저장합니다.

```sql
CREATE TABLE IF NOT EXISTS holdings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    stock_code VARCHAR(10) NOT NULL,
    quantity INT NOT NULL,
    purchase_price DECIMAL(12, 2) NOT NULL,
    purchase_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_holdings_user (user_id),
    CONSTRAINT fk_holdings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_holdings_stock FOREIGN KEY (stock_code) REFERENCES stocks (stock_code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 테이블 관계

```text
users.id -> users_profile.user_id
users.id -> recommendations.user_id
users.id -> holdings.user_id
stocks.stock_code -> stock_financials.stock_code
stocks.stock_code -> stock_reports.stock_code
stocks.stock_code -> recommendations.stock_code
stocks.stock_code -> holdings.stock_code
```

## API 명세

### 1. 인증 API

| 기능 | Method | Endpoint | 권한 | 상태 |
| --- | --- | --- | --- | --- |
| 회원가입 | POST | `/auth/signup` | PUBLIC | 구현 |
| 로그인 | POST | `/auth/login` | PUBLIC | 구현 |
| 로그아웃 | POST | `/auth/logout` | USER | 예정 |
| 토큰 재발급 | POST | `/auth/refresh` | PUBLIC | 예정 |
| 이메일 중복 확인 | GET | `/auth/check-email` | PUBLIC | 구현 |

### 2. 회원 / 마이페이지 API

| 기능 | Method | Endpoint | 권한 | 상태 |
| --- | --- | --- | --- | --- |
| 내 정보 조회 | GET | `/users/me` | USER | 구현 |
| 내 정보 수정 | PATCH | `/users/me` | USER | 예정 |
| 회원 탈퇴 | DELETE | `/users/me` | USER | 예정 |

### 3. 투자자 프로필 API

| 기능 | Method | Endpoint | 권한 | 상태 |
| --- | --- | --- | --- | --- |
| 투자자 프로필 조회 | GET | `/users/me/user-profile` | INVESTOR | 구현 |
| 투자자 프로필 등록/수정 | PUT | `/users/me/user-profile` | INVESTOR | 구현 |
| 투자자 프로필 일부 수정 | PATCH | `/users/me/user-profile` | INVESTOR | 예정 |

### 4. 종목 검색 / 리포트 API

| 기능 | Method | Endpoint | 권한 | 상태 |
| --- | --- | --- | --- | --- |
| 종목 자동완성 | GET | `/stocks/search` | PUBLIC | 구현 |
| 종목 시세 조회 | GET | `/stocks/{code}/price` | USER | 구현 |
| 종목 차트 조회 | GET | `/stocks/{code}/chart` | USER | 예정 |
| 종목 재무 조회 | GET | `/stocks/{code}/financial` | USER | 예정 |
| AI 분석 조회 | GET | `/stocks/{code}/analysis` | USER | 예정 |

### 5. 맞춤 추천 API

| 기능 | Method | Endpoint | 권한 | 상태 |
| --- | --- | --- | --- | --- |
| 전체 추천 조회 | GET | `/recommendations` | USER | 예정 |
| 분야별 추천 조회 | GET | `/recommendations?sector=IT` | USER | 예정 |

### 6. 나의 투자 현황 API

| 기능 | Method | Endpoint | 권한 | 상태 |
| --- | --- | --- | --- | --- |
| 보유 종목 목록 | GET | `/holdings` | USER | 예정 |
| 보유 종목 추가 | POST | `/holdings` | USER | 예정 |
| 보유 종목 수정 | PUT | `/holdings/{id}` | USER | 예정 |
| 보유 종목 삭제 | DELETE | `/holdings/{id}` | USER | 예정 |
| 투자 진단 조회 | GET | `/holdings/diagnosis` | USER | 예정 |

## 주요 API 예시

### 회원가입

```http
POST /auth/signup
Content-Type: application/json
```

```json
{
  "email": "test@example.com",
  "password": "password1234",
  "name": "투자초보"
}
```

### 로그인

```http
POST /auth/login
Content-Type: application/json
```

```json
{
  "email": "test@example.com",
  "password": "password1234"
}
```

### 이메일 중복 확인

```http
GET /auth/check-email?email=test@example.com
```

```json
{
  "success": true,
  "data": {
    "available": true
  },
  "error": null
}
```

### 내 정보 조회

```http
GET /users/me
Authorization: Bearer {accessToken}
```

### 투자자 프로필 저장

```http
PUT /users/me/user-profile
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "investmentExperience": "BEGINNER",
  "riskTolerance": "MEDIUM",
  "investmentGoal": "STABLE_GROWTH",
  "investableAmount": 1000000,
  "preferredSectors": ["IT", "반도체", "금융"]
}
```

### 종목 검색

```http
GET /stocks/search?keyword=삼성
```

응답 예시:

```json
[
  {
    "stockCode": "005930",
    "stockName": "삼성전자",
    "market": "KOSPI"
  }
]
```

### 종목 시세 조회

```http
GET /stocks/005930/price
Authorization: Bearer {accessToken}
```

## 종목 데이터 초기화

`/stocks/search`는 KRX API를 직접 호출하지 않고 DB의 `stocks` 테이블을 검색합니다. 처음 실행한 DB라면 KRX 종목 목록을 먼저 적재해야 합니다.

```http
POST /stocks/init
```

주의: 현재 개발 편의를 위해 열어둔 내부 초기화 API입니다. 운영 배포 전에는 관리자 권한으로 제한하거나 비활성화해야 합니다.

## 응답 형식

성공 응답:

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

일부 목록 API는 현재 구현상 배열을 직접 반환할 수 있습니다.

실패 응답:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "오류 메시지"
  }
}
```

## 프론트 화면

| 경로 | 설명 |
| --- | --- |
| `/` | 메인페이지, 종목 검색, AI 추천 요약 |
| `/login` | 로그인 |
| `/signup` | 회원가입, 이메일 중복 확인 |
| `/investor-profile` | 투자자 프로필 최초 등록 |
| `/mypage` | 내 정보, 투자 성향, 보유 종목, 추천 요약 |

## DB 확인 쿼리

```sql
USE dart_service;

SELECT id, email, name, role, created_at
FROM users
ORDER BY id DESC;

SELECT *
FROM users_profile
ORDER BY id DESC;

SELECT stock_code, stock_name, market, sector
FROM stocks
WHERE stock_name LIKE '삼성%'
LIMIT 8;

SELECT *
FROM holdings
ORDER BY id DESC;
```

회원과 투자자 프로필 조인:

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

## Git 주의사항

다음 파일과 디렉터리는 Git에 올리지 않습니다.

```text
.env
*.env
.env.*
backend/.env
application-local.properties
backend/build/
backend/.gradle/
frontend/node_modules/
frontend/dist/
```

커밋 전 확인:

```powershell
git status --short
```

민감정보가 들어갔는지 확인:

```powershell
git diff --cached
```
