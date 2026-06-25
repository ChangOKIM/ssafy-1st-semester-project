# 주린이 안경 (DartPoint AI) — 프로젝트 분석 문서

> 발표 자료 제작용 코드 기반 분석. 코드에서 확인되지 않은 항목은 "확인 안 됨"으로 표기.

---

## 1. 프로젝트 구조

### 1-1. 백엔드 패키지 구조

| 패키지 | 주요 클래스 | 책임 |
|--------|-------------|------|
| `auth` | `AuthController`, `AuthService`, `LoginRequest`, `SignupRequest`, `AuthResponse` | JWT 기반 이메일/비밀번호 인증 및 회원가입 |
| `global/config` | `SecurityConfig`, `AiConfig`, `RestTemplateConfig`, `ChatModelCompletionContentObservationFilter` | Spring Security 설정, AI 클라이언트 빈 등록, HTTP 클라이언트, 관측성(OTEL) |
| `global/security` | `JwtTokenProvider`, `JwtAuthenticationFilter`, `CustomUserDetails`, `JwtAuthenticationEntryPoint`, `MessageDigestUtil` | JWT 발급·검증(HmacSHA256), 요청 필터, 타이밍 공격 방어 |
| `global/exception` | `BusinessException`, `ErrorCode`, `GlobalExceptionHandler` | 비즈니스 예외 표준화 및 API 응답 포맷 통일 |
| `global/response` | `ApiResponse`, `ErrorResponse` | 통일된 JSON 응답 래퍼 |
| `user` | `UserController`, `UserService`, `UserRepository`, `UserResponse` | 사용자 정보 조회/수정 |
| `investor` | `InvestorProfileController`, `InvestorProfileService`, `InvestorProfileRepository` | 투자자 성향(위험도·목표·선호 업종) 저장/조회 |
| `stock` | `StockController`, `StockService`, `StockMapper`, `ChartService`, `FinancialService`, `MarketCapRankingService` | 주가 조회(KIS), 차트, 재무(DART), 시가총액 순위 |
| `stock/kis` | `KisTokenService`, `KisPriceResponse`, `KisChartResponse`, `KisMarketCapRankingResponse` | KIS OAuth 토큰 캐싱(1시간), 시세·차트·시총 응답 모델 |
| `stock/dart` | `DartClient`, `DartFinancialResponse`, `StockFinancial` | DART 재무정보 API 호출, DB 캐싱 |
| `recommendation` | `RecommendationController`, `RecommendationService`, `RecommendationMapper` | 투자성향 기반 종목 추천(전체 TOP 10 + 섹터별 TOP 3) |
| `recommendation/score` | `RecommendScorer`, `ScoreWeights` | 0~100점 점수 산출 알고리즘, 가중치 상수 관리 |
| `recommendation/dto` | `RecommendItem`, `RecommendResponse`, `ScoringInput`, `SectorRecommend` | 추천 관련 DTO |
| `report` | `ReportController`, `ReportService`, `ReportLlmService`, `ReportMapper`, `SectorGuide` | AI 리포트 생성(Claude), 재무 분석, 업종별 해석 가이드 |
| `report/dto` | `ReportInput`, `StockSearchResponseDto` | 리포트 입력 데이터 가공, 종목 검색 응답 |
| `portfolio` | `HoldingController`, `HoldingService`, `HoldingExtractService`, `HoldingDiagnosisAiService`, `HoldingRepository` | 보유 종목 CRUD, 이미지→종목 추출(GPT-4o-mini Vision), AI 포트폴리오 진단 |
| `portfolio/dto` | `HoldingRequest`, `HoldingResponse`, `HoldingSummaryResponse`, `ExtractedHoldingDto` | 보유 종목 관련 DTO |

경로 기준: `backend/src/main/java/com/ssafy/dartservice/`

---

### 1-2. 프론트엔드 디렉터리 구조

#### Views (페이지)
| 파일 | 역할 |
|------|------|
| `views/MainView.vue` | 홈 — 추천 TOP 10 요약, 히어로 섹션 |
| `views/LoginView.vue`, `SignupView.vue` | 로그인 / 회원가입 |
| `views/StocksView.vue` | 시가총액 TOP 30 종목 목록 (두 열 레이아웃) |
| `views/ReportView.vue` | 개별 종목 리포트 — 차트, 재무 지표, AI 3분 리포트 |
| `views/RecommendationView.vue` | 맞춤 추천 — 전체 TOP 10 + 섹터별 TOP 3 |
| `views/HoldingsView.vue` | 보유 종목 관리, 이미지 업로드, AI 포트폴리오 진단 |
| `views/MyPageView.vue`, `MyInfoView.vue` | 마이페이지, 사용자 정보 수정 |
| `views/InvestorProfileView.vue` | 투자성향 설정 (경험·위험도·목표·관심 업종) |

#### API 모듈 (`frontend/src/api/`)
| 파일 | 역할 |
|------|------|
| `api.js` | Axios 인스턴스 — `baseURL`, JWT 자동 주입 인터셉터, 401 리다이렉트 |
| `authApi.js` | 회원가입, 로그인, 이메일 중복 확인 |
| `stockApi.js` | 종목 검색·시세·차트·재무·분석 리포트·시가총액 순위 |
| `recommendationApi.js` | 추천 데이터 조회 |
| `holdingApi.js` | 보유 종목 CRUD, AI 진단, 이미지 추출 |
| `userApi.js` | 사용자 정보 조회/수정 |

#### 상태 관리 (`frontend/src/stores/`)
| 파일 | 역할 |
|------|------|
| `authStore.js` | 인증 상태 (토큰, 사용자 정보, 로그인·로그아웃·회원가입) |

#### 라우터
- `frontend/src/router/index.js` — 17개 라우트, 인증 필요 페이지 메타 설정, 미인증 리다이렉트

---

### 1-3. 백-프론트 연결 지점

| 항목 | 내용 |
|------|------|
| **API 호출 모듈** | `frontend/src/api/api.js` — Axios 인스턴스 중앙 관리 |
| **baseURL** | 개발: `http://localhost:8080` (환경 변수 `VITE_API_BASE_URL`) |
| **인증 방식** | JWT Bearer 토큰 — `localStorage`에 저장, 모든 요청 헤더 자동 주입 |
| **401 처리** | 인터셉터에서 토큰 삭제 후 `/login?expired=true&redirect=...` 리다이렉트 |
| **CORS** | `SecurityConfig`에서 `http://localhost:5173` 허용 |

---

## 2. 핵심 기능별 동작 흐름

### 2-1. 종목 검색 및 리포트 (AI 리포트 생성 포함)

```
[StocksView] 검색어 입력
  → GET /stocks?keyword=삼성 (listAllStocks)
  → StockController → StockService → StockMapper.listStocks
  → MySQL stocks 테이블
  → StockSearchResponseDto[] 응답

[ReportView] 종목 코드로 진입 (/report/:code)
  → Promise.allSettled([
       GET /stocks/{code}/price   (KIS 실시간 시세)
       GET /stocks/{code}/chart   (KIS 차트 데이터)
       GET /stocks/{code}/financial (DART 재무정보)
       GET /reports/{code}/info   (종목명·업종·소개)
     ])
  → 각 서비스에서 캐시 확인 → 캐시 미스 시 외부 API 호출 → 저장

[AI 리포트 요청] "AI 3분 리포트 생성" 버튼
  → POST /reports/{code}/report (인증 필요)
  → ReportController → ReportService
  → reportMapper.findCachedReport(code, level) — 당일 캐시 확인
  → 캐시 없으면 ReportLlmService.generateReport(ReportInput)
  → Spring AI ChatClient.prompt().system(SYSTEM_PROMPT).user(재무데이터).call()
  → claude-sonnet-4-6 응답
  → reportMapper.saveReport(code, level, content) — DB 저장
  → 프론트 표시
```

### 2-2. 맞춤형 종목 추천 (점수 산출 포함)

```
[RecommendationView] 페이지 진입 (인증 필요)
  → GET /recommendations (인증 헤더 필요)
  → RecommendationController → RecommendationService
  → investorProfileRepository.findByUser(user) — 투자성향 로드
  → 전체 종목 × 재무정보 JOIN 조회 (RecommendationMapper)
  → RecommendScorer.score(ScoringInput, userProfile) — 각 종목 점수 계산
  → 전체 TOP 10 추출
  → 사용자 선호 섹터별 TOP 3 추출
  → RecommendResponse 응답 (두 섹션)
```

점수 산출 로직은 섹션 3 참조.

### 2-3. 실시간 시세 표시

```
[ReportView] onMounted 시
  → getStockPrice(code) — KIS FHKST01010100
  → 30초마다 refreshPrice() 호출 (setInterval)
  → Page Visibility API: 탭 복귀 시 즉시 새로고침
  → onUnmounted: clearInterval + removeEventListener

[StocksView] TOP 30 목록
  → getTopMarketCapStocks() — GET /stocks/market-cap/top
  → MarketCapRankingService: KIS FHPST01710000 (KOSPI + KOSDAQ 순위)
  → 현재가·등락률 포함하여 정렬된 리스트 반환
```

### 2-4. 나의 투자 현황 / 마이페이지 / 인증

```
[회원가입] POST /auth/signup → AuthService.signup → bcrypt 비밀번호 해시 → users 테이블 저장
[로그인]   POST /auth/login → 비밀번호 검증 → JwtTokenProvider.createToken → JWT 반환
           → 프론트 localStorage.setItem('accessToken', jwt)

[보유 종목] GET /holdings (인증 필요)
  → HoldingController → HoldingService
  → holdingRepository.findAllByUserId(userId)
  → CompletableFuture 병렬로 각 종목 현재가 조회 (KIS)
  → 총 평가금액·손익·수익률 집계

[이미지 추출] POST /holdings/extract (multipart/form-data)
  → HoldingExtractService.extract(file)
  → 파일 검증 (image/*, 10MB 이하)
  → GPT-4o-mini Vision 호출
  → [{ name, quantity, avgPrice }] 파싱 후 반환

[AI 진단] POST /holdings/diagnosis (인증 필요)
  → SHA-256 해시(보유종목+투자성향) 캐시 키 생성
  → holdingRepository.findCachedDiagnosis(userId, hash)
  → 캐시 없으면 HoldingDiagnosisAiService.diagnose() → GPT-4o-mini
  → JSON 응답 파싱 → DB 저장

[마이페이지] GET /users/me → 사용자 정보 조회
             GET /investor-profiles/me → 투자성향 조회
             PUT /investor-profiles → 투자성향 수정
```

---

## 3. AI 연동 (상세)

### 3-1. LLM 설정

**파일**: `backend/src/main/java/com/ssafy/dartservice/global/config/AiConfig.java`

```java
@Bean("anthropicChatClient")
public ChatClient anthropicChatClient(AnthropicChatModel model) {
    return ChatClient.create(model);
}

@Bean("openaiChatClient")
public ChatClient openaiChatClient(OpenAiChatModel model) {
    return ChatClient.create(model);
}
```

**설정** (`backend/src/main/resources/application.properties`):
```properties
# Claude — AI 리포트 생성용
spring.ai.anthropic.chat.options.model=claude-sonnet-4-6
spring.ai.anthropic.chat.options.temperature=0.3
spring.ai.anthropic.chat.options.max-tokens=4096
spring.ai.anthropic.base-url=https://gms.ssafy.io/gmsapi/api.anthropic.com

# OpenAI — 포트폴리오 진단 + 이미지 추출용
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.base-url=https://gms.ssafy.io/gmsapi/api.openai.com
```

> 두 LLM을 역할별로 분리: 텍스트 분석(Claude) vs 구조화 출력+Vision(GPT-4o-mini)

---

### 3-2. AI 리포트 생성 (Claude)

**파일**: `backend/src/main/java/com/ssafy/dartservice/report/ReportLlmService.java`

**메서드**: `generateReport(ReportInput input)`

```java
public String generateReport(ReportInput input) {
    String block = input.toPromptText();
    return chatClient.prompt()
        .system(SYSTEM_PROMPT)
        .user("다음 데이터로 분석해주세요:\n" + block)
        .call()
        .content();
}
```

**시스템 프롬프트** (전문, `ReportLlmService.SYSTEM_PROMPT`):
```
당신은 기업 재무를 풀어 설명해 주는 10년차 투자 전문가입니다. 한숨 깊게 쉬고 아래의 요청에 정확하고 친절하게 답변해주세요.
user 메시지 맨 위의 '[독자 수준]'을 확인하고, 그 수준에 맞는 깊이·길이로 설명하세요.
user 메시지에 주어진 숫자 데이터만 사용하세요.

[독자 수준별 깊이 — 가장 중요한 규칙]
형식(섹션·판정)은 모든 수준에서 동일. 깊이만 조절하세요.
- 입문/초보(NONE·BEGINNER): 용어는 괄호로 짧게 풀기. 비유는 리포트 전체에서 최대 1개.
- 중급(INTERMEDIATE): 용어는 쓰되 해석 한 줄로.
- 고급(ADVANCED): 용어 설명·비유 없이 수치+해석만.
전체 리포트 총량 상한: BEGINNER 1200자, INTERMEDIATE 800자, ADVANCED 500자.

★[판정 기준 — 정량 조건으로 기계적으로 판정]

💰 판정 기준 (연간 3년 데이터만 사용):
- 매출과 영업이익이 모두 3년간 증가 → 꾸준히 잘 벌어요
- 매출 또는 영업이익 중 하나라도 중간에 5%이상 감소한 해가 있음 → 들쑥날쑥해요
- 매출과 영업이익이 모두 3년간 감소 → 줄고 있어요

🏦 판정 기준 (가장 최근 연도 기준):
- 부채비율 100% 이하이고 이자보상배율 3배 이상 → 안정적이에요
- 부채비율 100~200%이거나 이자보상배율 1~3배 → 보통이에요
- 부채비율 200% 초과이거나 이자보상배율 1배 미만 → 주의가 필요해요

[출력 형식]:
💰 돈은 잘 벌고 있나요? — (판정 기준에 따른 결과)
🏦 재무는 튼튼한가요? — (판정 기준에 따른 결과)
📊 지금 주가, 이렇게 이해해요 — 참고만 하세요
✅ 좋은 점 / ⚠️ 살필 점 — 각 1개, 한 줄씩
📝 종합 진단 — 위 내용을 3문장 이내로 엮기

마지막에 반드시 추가: "본 정보는 참고용이며, 투자 판단과 책임은 본인에게 있습니다."
```

---

### 3-3. 프롬프트에 주입되는 데이터 가공 로직

**파일**: `backend/src/main/java/com/ssafy/dartservice/report/dto/ReportInput.java`

**메서드**: `toPromptText()`

`ReportInput`은 다음 필드로 구성된 레코드(record):
- `회사명`, `업종`, `investmentLevel` (독자 수준)
- `최근분기` (매출·영업이익·영업이익률·부채비율)
- `연간3년` (List — 연도별 매출·영업이익·영업이익률·부채비율·이자보상배율)
- `현재가`, `per`, `pbr`

`toPromptText()` 출력 예시:
```
[독자 수준] BEGINNER

[기업]
회사명: 삼성전자

[업종 해석 가이드]
반도체는 경기 사이클을 크게 타는 산업...

[최근 분기 — 현재 상태 참고용]
2025년 1분기 누적
매출: 약 70.5조 원
영업이익: 약 12.3조 원
영업이익률: 17.50%
부채비율: 42.30%

[연간 재무 3년]
2024년 | 매출 약 258.9조 원 | 영업이익 약 40.2조 원 | 영업이익률 15.55% | 부채비율 42.10% | 이자보상배율 15.67배
2023년 | ...
2022년 | ...

[밸류에이션]
현재가: 약 75,500원
PER: 12.34배
PBR: 1.45배
```

**업종 해석 가이드 주입**: `SectorGuide.getGuide(sector)` — 섹터별 사전 정의된 해석 문장을 주입하여 AI가 업종 특수성을 고려한 분석을 할 수 있도록 컨텍스트 제공

**파일**: `backend/src/main/java/com/ssafy/dartservice/report/SectorGuide.java`

---

### 3-4. 재무 데이터 → 텍스트 변환

**파일**: `backend/src/main/java/com/ssafy/dartservice/report/ReportService.java`

재무 원본 데이터(DART에서 가져온 단위: 원)를 `ReportInput` 생성 시 포맷 변환:
- 매출·영업이익·당기순이익: 조/억 단위 한국어 변환
- 영업이익률: `(영업이익 / 매출) × 100` 계산
- 이자보상배율: `(영업이익 / 이자비용)` 계산
- 부채비율: `(총부채 / 자본) × 100` 계산

---

### 3-5. 포트폴리오 진단 (GPT-4o-mini)

**파일**: `backend/src/main/java/com/ssafy/dartservice/portfolio/HoldingDiagnosisAiService.java`

**메서드**: `diagnose(InvestorProfile, List<HoldingResponse>, ...)`

**시스템 프롬프트** (핵심):
```
당신은 초보 투자자의 투자성향과 실제 보유 종목을 비교해서 설명하는 포트폴리오 진단가입니다.
반드시 사용자가 제공한 JSON 데이터만 근거로 한국어 진단을 작성하세요.

반드시 다룰 내용:
1. 투자성향 적합성 (riskTolerance, investmentGoals 기준)
2. 관심 업종(preferredSectors) vs 실제 보유 업종(holdings[].sector) 비교
3. 손익 구조 (개별 종목 profitRate 비교, 수익/손실 1위 종목 언급)
4. 가이드라인 (매수/매도 지시 금지, 점검 질문만)

응답은 반드시 JSON만 반환:
{
  "sections": [
    { "title": "...", "content": "...", "guideItems": ["...", "...", "..."] }
  ]
}
```

**입력**: JSON (투자자 프로필 스냅샷 + 포트폴리오 스냅샷 + 보유 종목 목록)

**캐싱**: SHA-256 해시(보유 종목 + 투자성향) → `holdings_diagnosis` 테이블

---

### 3-6. 이미지 추출 (GPT-4o-mini Vision)

**파일**: `backend/src/main/java/com/ssafy/dartservice/portfolio/HoldingExtractService.java`

**메서드**: `extract(MultipartFile file)`

**시스템 프롬프트**:
```
당신은 증권사 앱 보유종목 스크린샷에서 정보를 추출하는 도구입니다.
이미지에 있는 각 종목의 종목명, 보유수량, 매입단가(평균단가)를 추출하세요.
평가금액, 손익, 수익률 등 매입 기준이 아닌 값은 무시하세요.
읽을 수 없는 값은 null로 두세요.
설명, 마크다운, 코드블록 없이 JSON 배열만 반환:
[{ "name": "삼성전자", "quantity": 10, "avgPrice": 75000 }]
```

---

### 3-7. AI 관측성 (Observability)

**파일**: `backend/src/main/java/com/ssafy/dartservice/global/config/ChatModelCompletionContentObservationFilter.java`

- Spring AI + OpenTelemetry로 LLM 호출 프롬프트·응답 트레이싱
- 수집: Langfuse (`https://jp.cloud.langfuse.com/api/public/otel/v1/traces`)

---

## 4. 외부 API 연동

### 4-1. KIS API (한국투자증권)

**경로**: `backend/src/main/java/com/ssafy/dartservice/stock/kis/`

#### 토큰 발급 및 캐싱
**파일**: `KisTokenService.java`
```java
// POST https://openapi.koreainvestment.com:9443/oauth2/tokenP
// grant_type=client_credentials, appkey, appsecret
// 응답: access_token, expires_in (초)
// 캐싱: 만료 1시간 전에 갱신 (synchronized 블록)
```

#### 호출 엔드포인트

| 기능 | TR_ID | URL |
|------|-------|-----|
| 현재 시세 (PER·PBR 포함) | `FHKST01010100` | `/uapi/domestic-stock/v1/quotations/inquire-price` |
| 일·주·월 차트 | `FHKST01010400` | `/uapi/domestic-stock/v1/quotations/inquire-daily-price` |
| 시가총액 순위 | `FHPST01710000` | `/uapi/domestic-stock/v1/ranking/market-cap` |

**설정**:
```properties
kis.app-key=${KIS_APP_KEY}
kis.app-secret=${KIS_APP_SECRET}
kis.base-url=https://openapi.koreainvestment.com:9443
```

---

### 4-2. DART API (금감원 전자공시)

**경로**: `backend/src/main/java/com/ssafy/dartservice/stock/dart/DartClient.java`

```
GET https://opendart.fss.or.kr/api/fnlttSinglAcntAll.json
  ?crtfc_key={API_KEY}
  &corp_code={기업코드}       // e.g., "00126380" (삼성전자)
  &bsns_year={연도}           // e.g., 2024
  &reprt_code={기간코드}      // 11011(연간), 11013(1Q), 11012(반기), 11014(3Q)
  &fs_div=CFS                 // 연결재무제표
```

**기간코드별 캐싱 전략**:
- **연간 3년**: 3건 모두 DB에 있으면 DART 호출 스킵
- **분기 (최신)**: 3분기 → 반기 → 1분기 순서로 시도, 첫 성공 데이터 반환
- **저장**: `INSERT ... ON DUPLICATE KEY UPDATE` (PK: stock_code + base_year + period_code)

**설정**:
```properties
dart.api-key=${DART_API_KEY}
dart.base-url=https://opendart.fss.or.kr
```

---

### 4-3. KRX API

**확인 안 됨** — `application.properties`에 `krx.api-key`와 `krx.base-url` 설정만 존재. 구현 클래스 없음.

---

### 4-4. 캐싱 전략 요약

| 대상 | 캐싱 방식 | 위치 | 무효화 조건 |
|------|-----------|------|------------|
| KIS 액세스 토큰 | 인메모리 변수 (`synchronized`) | `KisTokenService.java` | 만료 1시간 전 자동 갱신 |
| DART 재무정보 (연간) | MySQL `stock_financials` | `FinancialService.java` | 영구 저장 (ON DUPLICATE KEY) |
| DART 재무정보 (분기) | MySQL `stock_financials` | `FinancialService.java` | 영구 저장 |
| AI 리포트 | MySQL `stock_reports` | `ReportService.java` | 당일 (DATE = CURDATE()) |
| 포트폴리오 진단 | MySQL `holdings_diagnosis` | `HoldingService.java` | 종목·성향 변경 시 해시 불일치 |

---

## 5. 발표에서 어필할 만한 기술적 의사결정

### 5-1. JWT 수동 구현 + 타이밍 공격 방어

**근거**: `backend/src/main/java/com/ssafy/dartservice/global/security/JwtTokenProvider.java`, `MessageDigestUtil.java`

Spring Security의 기본 JWT 라이브러리(jjwt 등) 없이 직접 구현:
- Base64 URL 인코딩 (패딩 없음)
- HmacSHA256 서명
- `MessageDigest.isEqual()`을 사용한 Constant-time 비교 → 타이밍 공격(Timing Attack) 방어

```java
private boolean constantTimeEquals(String expected, String actual) {
    return MessageDigestUtil.equals(
        expected.getBytes(StandardCharsets.UTF_8),
        actual.getBytes(StandardCharsets.UTF_8)
    );
}
```

---

### 5-2. 보유 종목 현재가 병렬 조회 (CompletableFuture)

**근거**: `backend/src/main/java/com/ssafy/dartservice/portfolio/HoldingService.java`

보유 종목 N개에 대해 KIS API를 순차 호출하면 N × 응답시간 만큼 지연 발생. CompletableFuture로 병렬 처리:

```java
List<CompletableFuture<HoldingResponse>> futures = holdings.stream()
    .map(h -> CompletableFuture.supplyAsync(() -> toResponse(h)))
    .toList();

return futures.stream().map(CompletableFuture::join).toList();
```

---

### 5-3. 멱등한 재무 데이터 저장

**근거**: `backend/src/main/resources/mapper/ReportMapper.xml`

중복 DART 호출 시 데이터 손상 없이 안전하게 Upsert:
```xml
INSERT INTO stock_financials (...)
VALUES (...)
ON DUPLICATE KEY UPDATE
    revenue=VALUES(revenue),
    operating_profit=VALUES(operating_profit), ...
```

---

### 5-4. 보유 종목 중복 추가 시 가중 평균 단가 자동 계산

**근거**: `backend/src/main/java/com/ssafy/dartservice/portfolio/HoldingService.java`

동일 종목을 재매수할 때:
```java
BigDecimal weightedPrice = oldPrice.multiply(BigDecimal.valueOf(oldQty))
    .add(newPrice.multiply(BigDecimal.valueOf(newQty)))
    .divide(BigDecimal.valueOf(oldQty + newQty), 0, RoundingMode.HALF_UP);
```
→ 가중 평균 매입 단가를 자동으로 갱신

---

### 5-5. AI 응답 캐싱 (비용 절감 + 속도)

**근거**: `backend/src/main/java/com/ssafy/dartservice/report/ReportService.java`, `HoldingService.java`

- **리포트**: 동일 종목·동일 투자자 수준이면 당일 AI 응답 재사용
- **포트폴리오 진단**: SHA-256 해시로 보유 종목+성향이 변경되지 않으면 캐시 반환
- → LLM API 비용 최소화, 응답 속도 향상

---

### 5-6. Dual LLM 전략 (역할 분리)

**근거**: `backend/src/main/java/com/ssafy/dartservice/global/config/AiConfig.java`

| 역할 | 모델 | 이유 |
|------|------|------|
| AI 리포트 (긴 텍스트, 한국어 품질) | claude-sonnet-4-6 | 한국어 금융 텍스트 생성 품질 |
| 포트폴리오 진단 (JSON 구조화 출력) | gpt-4o-mini | 빠른 JSON 응답, 비용 효율 |
| 이미지 추출 (Vision) | gpt-4o-mini | 멀티모달 Vision 지원 |

---

### 5-7. 투자자 수준별 프롬프트 분기

**근거**: `backend/src/main/java/com/ssafy/dartservice/report/dto/ReportInput.java`

4단계 독자 수준(NONE·BEGINNER·INTERMEDIATE·ADVANCED)에 따라 AI 응답의 깊이와 길이를 프롬프트에서 명시적으로 제어:
- 초보: 비유 허용, 최대 1200자
- 고급: 숫자+해석만, 최대 500자

→ 단순 프롬프트 전달이 아닌, 사용자 데이터를 활용한 동적 프롬프트 구성

---

### 5-8. 30초 시세 폴링 + Page Visibility 최적화

**근거**: `frontend/src/views/ReportView.vue`

```javascript
// 탭 숨겨지면 API 낭비 없이 중단, 복귀 시 즉시 갱신
function startPricePolling() {
    priceTimer = setInterval(() => {
        if (!document.hidden) refreshPrice()
    }, 30_000)
}
document.addEventListener('visibilitychange', onPriceVisibilityChange)
// onUnmounted: clearInterval + removeEventListener (메모리 누수 방지)
```

---

### 5-9. DART 분기 데이터 폴백 체인

**근거**: `backend/src/main/java/com/ssafy/dartservice/stock/FinancialService.java`

DART는 분기별로 데이터 공시 시점이 다름. 가장 최신 분기부터 시도하여 실패 시 자동으로 이전 분기로 폴백:
```java
for (String periodCode : new String[]{"11014", "11012", "11013"}) {
    // 3분기 → 반기 → 1분기 순서로 시도
    DartFinancialResponse res = dartClient.fetch(corpCode, year, periodCode, FS_DIV);
    if (res != null && "000".equals(res.getStatus())) return save(res);
}
```

---

## 6. PPT 캡처 추천 위치

### 6-1. AI 적용 코드 캡처 추천

| 슬라이드 주제 | 파일 경로 | 캡처할 내용 |
|--------------|-----------|------------|
| Spring AI 두 LLM 연동 | `backend/src/main/java/com/ssafy/dartservice/global/config/AiConfig.java` | `anthropicChatClient`, `openaiChatClient` 두 빈 등록 |
| AI 리포트 생성 핵심 로직 | `backend/src/main/java/com/ssafy/dartservice/report/ReportLlmService.java` | `generateReport()` 메서드 전체 |
| 포트폴리오 진단 + 폴백 | `backend/src/main/java/com/ssafy/dartservice/portfolio/HoldingDiagnosisAiService.java` | `diagnose()` try-catch + fallback |
| 이미지 추출 (Vision) | `backend/src/main/java/com/ssafy/dartservice/portfolio/HoldingExtractService.java` | `extract()` + 파일 검증 |
| 추천 점수 산출 | `backend/src/main/java/com/ssafy/dartservice/recommendation/score/RecommendScorer.java` | `score()` 가중합 공식 |
| 가중치 상수 | `backend/src/main/java/com/ssafy/dartservice/recommendation/score/ScoreWeights.java` | 4가지 가중치 상수 |
| 실시간 시세 폴링 | `frontend/src/views/ReportView.vue` | `startPricePolling()` + `onPriceVisibilityChange()` |

---

### 6-2. 프롬프트 캡처 추천

| 슬라이드 주제 | 파일 경로 | 캡처할 내용 |
|--------------|-----------|------------|
| AI 리포트 시스템 프롬프트 | `backend/src/main/java/com/ssafy/dartservice/report/ReportLlmService.java` | `SYSTEM_PROMPT` 상수 전체 |
| 독자 수준별 분기 | `backend/src/main/java/com/ssafy/dartservice/report/dto/ReportInput.java` | `toPromptText()` — `[독자 수준]` 주입 부분 |
| 업종 해석 가이드 | `backend/src/main/java/com/ssafy/dartservice/report/SectorGuide.java` | 섹터별 한국어 가이드 문자열 |
| 재무 데이터 텍스트 변환 | `backend/src/main/java/com/ssafy/dartservice/report/dto/ReportInput.java` | `toPromptText()` 출력 포맷 (조/억 단위 변환) |
| 포트폴리오 진단 프롬프트 | `backend/src/main/java/com/ssafy/dartservice/portfolio/HoldingDiagnosisAiService.java` | `SYSTEM_PROMPT` + JSON 응답 형식 |
| 이미지 추출 프롬프트 | `backend/src/main/java/com/ssafy/dartservice/portfolio/HoldingExtractService.java` | `SYSTEM_PROMPT` 전체 (짧고 명확) |

---

## 부록 — 주요 DB 테이블

| 테이블 | 용도 |
|--------|------|
| `users` | 사용자 계정 (email, password_hash, name, role) |
| `investor_profiles` | 투자성향 (risk_tolerance, preferred_sectors CSV, investment_goals CSV) |
| `stocks` | 종목 마스터 (stock_code, corp_code, stock_name, market) |
| `stock_info` | 종목 상세 (sector, intro, market_cap) |
| `stock_financials` | 재무정보 캐시 (PK: stock_code + base_year + period_code) |
| `stock_reports` | AI 리포트 캐시 (stock_code, level, content, generated_at) |
| `holdings` | 보유 종목 (user_id, stock_code, quantity, purchase_price, purchase_date) |
| `holdings_diagnosis` | AI 진단 캐시 (user_id, hash, diagnosis_json) |
| `recommendations` | 추천 기록 (user_id, stock_code, rec_type OVERALL/SECTOR, score) |

---

## 부록 — 기술 스택

| 계층 | 기술 |
|------|------|
| 백엔드 | Spring Boot 3.x, MyBatis, MySQL, Spring AI |
| 프론트엔드 | Vue 3 (Composition API), Pinia, Axios, Chart.js (캔들스틱) |
| LLM | Anthropic Claude (claude-sonnet-4-6), OpenAI GPT-4o-mini |
| 외부 API | KIS (한국투자증권), DART (금감원) |
| 관측성 | Langfuse (OpenTelemetry), Spring Boot Micrometer |
| 인증 | JWT (HmacSHA256, 1시간 유효) |
