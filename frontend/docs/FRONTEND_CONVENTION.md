# Frontend Convention

분석 대상: `src/views/MainView.vue`, `CompaniesView.vue`, `src/api/`, `src/stores/authStore.js`, `src/router/index.js`, `src/components/layout/`, `src/style.css`, `src/main.js`

---

## 1. 컴포넌트 작성 규칙

### `<script setup>` 필수

모든 컴포넌트는 `<script setup>` (Composition API) 을 사용한다. `<script setup>` 없이 template만 있는 순수 마크업 컴포넌트(예: `AppFooter.vue`)는 예외.

```vue
<!-- 올바름 -->
<script setup>
import { ref, computed, onMounted } from 'vue'
</script>

<!-- 금지 -->
<script>
export default { data() { ... } }
</script>
```

### ref / reactive 패턴

- 상태 선언은 `ref()` 만 사용한다. `reactive()`는 사용하지 않는다.
- 파생 상태는 `computed()`로 선언한다.
- 스토어에서 가져온 값도 로컬 `computed()`로 한 번 감싸서 사용한다.

```js
const keyword = ref('')
const searchLoading = ref(false)
const isLoggedIn = computed(() => authStore.isLoggedIn)  // 스토어 값 → 로컬 computed
```

### 정적 데이터

반응형이 필요 없는 목록·설정 데이터는 `ref`/`reactive` 없이 `const`로 선언한다.

```js
const serviceCards = [
  { title: '종목 검색', description: '...' },
]
```

### 함수 네이밍

- 이벤트 핸들러: `submit`, `go`, `load` 동사 + 명사 (camelCase). `handleXxx` 접두어 사용 안 함.
- 헬퍼 함수: 짧은 명사형 camelCase (`unwrap`, `stockName`, `stockCode`).

```js
function submitSearch() { ... }
function goToRecommendations() { ... }
async function loadRecommendations() { ... }
```

### 라이프사이클

`onMounted`에 async 함수 이름을 직접 전달한다 (inline async 함수 금지).

```js
onMounted(loadRecommendations)  // O
onMounted(async () => { ... })  // X
```

### Props / Emit

현재 코드베이스에서는 컴포넌트 간 데이터 전달을 props/emit 대신 Pinia store + composable로 처리한다. props가 필요한 경우 `defineProps<{...}>()` TypeScript 형식을 사용한다 (프로젝트가 JS이므로 `defineProps({})` 사용 가능).

---

## 2. 스타일링 방식

### 전역 CSS (스코프 없음)

- 모든 스타일은 `src/style.css` 한 파일에 작성한다.
- 컴포넌트 내부에 `<style scoped>` 또는 `<style>` 블록을 추가하지 않는다.
- CSS Modules, Tailwind, CSS-in-JS 사용 안 함.

### 색상 (CSS 변수 없음, 값 직접 사용)

현재 CSS custom property(`--변수`)를 사용하지 않는다. 아래 색상을 일관되게 사용한다.

| 용도 | 색상 값 |
|------|---------|
| 브랜드 / 액션 초록 | `#1f6f5b` |
| 텍스트 최강 (제목) | `#102018` |
| 텍스트 기본 | `#162033` |
| 텍스트 보조 | `#5d6b61` |
| 텍스트 뮤트 | `#68766e` |
| 배경 전체 | `#f3f6f1` |
| 카드 배경 | `#fff` / `rgba(255,255,255,0.94)` / `#fbfcfa` / `#f7faf6` |
| 테두리 기본 | `#cbd8cf` |
| 테두리 카드 | `#d6ded8` |
| 테두리 내부 | `#e0e8e2` |
| 상승 (양수) | `#16824f` |
| 하락 (음수) | `#c2412d` |
| 에러 | `#b42318` |

### 레이아웃 토큰

- **최대 너비**: `width: min(1140px, calc(100% - 32px))` (데스크톱)
- **브레이크포인트**: `@media (max-width: 900px)`, `@media (max-width: 560px)`
- **주요 레이아웃 도구**: CSS Grid (`display: grid`) 우선, Flexbox 보조.

### 클래스 네이밍

BEM에 가까운 플랫 클래스명. 블록 + 요소를 `-`로 이어 붙인다 (`__` 대신 `-` 사용).

```
.app-header          → 블록
.app-header-inner    → 블록의 내부 래퍼
.hero-section        → 새 블록
.hero-copy           → hero 내부 텍스트 영역
.search-result       → 검색 결과 아이템
```

유틸리티 클래스: `.eyebrow`, `.panel-message`, `.positive`, `.negative`, `.muted`

### 공유 카드 스타일

아래 선택자들이 하나의 CSS 선언 블록을 공유한다. 새 카드 컴포넌트 추가 시 이 패턴에 셀렉터를 추가한다.

```css
.search-panel,
.recommendation-summary,
.profile-card,
.metric-card,
.service-grid article,
.auth-card,
.profile-card-form {
  border: 1px solid #d6ded8;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 42px rgba(32, 48, 40, 0.08);
}
```

### 버튼 클래스

| 클래스 | 용도 |
|--------|------|
| `.primary-action` / `.base-button.primary` | 주요 액션 (초록 배경) |
| `.secondary-action` / `.base-button.secondary` | 보조 액션 (흰 배경, 테두리) |

---

## 3. API 호출 패턴

### axios 인스턴스 (`src/api/api.js`)

- `axios.create()`로 단일 인스턴스 생성.
- `baseURL`: 환경 변수 `VITE_BACKEND_API_BASE_URL` → 없으면 `http://localhost:8080`
- 기본 헤더: `Content-Type: application/json`
- **Request 인터셉터**: `localStorage.getItem('accessToken')` 읽어서 `Authorization: Bearer {token}` 헤더 자동 첨부.
- **Response 인터셉터 없음**: 에러는 각 호출 지점(view)에서 처리.

```js
// api.js 핵심 구조
const api = axios.create({ baseURL: '...' })
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
```

### API 파일 구조

각 도메인마다 파일을 분리하고, `api` 인스턴스를 import해서 named export 함수로 작성한다.

```
src/api/
  api.js             ← axios 인스턴스 (default export)
  authApi.js         ← login, signup
  userApi.js         ← getMyInfo
  stockApi.js        ← 종목 관련
  recommendationApi.js ← 추천 관련
```

### 현재 정의된 API 함수 목록

**stockApi.js**
```js
searchStocks(keyword)           // GET /reports/search?keyword=
getStockPrice(code)             // GET /stocks/{code}/price
getStockChart(code, period)     // GET /stocks/{code}/chart?period=  (default: 'daily')
getStockFinancial(code)         // GET /stocks/{code}/financial
getStockAnalysis(code)          // GET /stocks/{code}/analysis
```

**recommendationApi.js**
```js
getRecommendations(sector?)     // GET /recommendations?sector=  (sector 없으면 전체)
```

### 응답 데이터 unwrap 패턴

백엔드 응답이 `{ data: { data: [...] } }` 이중 중첩 구조. 다음 헬퍼로 unwrap한다.

```js
function unwrap(response) {
  return response?.data?.data ?? response?.data ?? []
}
```

### 에러 처리

try / catch / finally 블록을 각 view에서 직접 사용. 에러 시 빈 배열 또는 사용자 메시지 문자열 할당.

```js
try {
  searchResults.value = unwrap(await searchStocks(trimmed)).slice(0, 8)
} catch {
  searchResults.value = []
  searchMessage.value = '종목 검색 API 응답을 확인할 수 없습니다.'
} finally {
  searchLoading.value = false
}
```

병렬 요청은 `Promise.allSettled()`를 사용해 일부 실패 시에도 나머지 결과를 표시한다.

```js
const [overall, sector] = await Promise.allSettled([req1(), req2()])
if (overall.status === 'fulfilled') { ... }
```

---

## 4. Pinia Store 사용 방식

### 스토어 정의

Setup Store 스타일 (Options Store 금지).

```js
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const user = ref(null)
  const isLoggedIn = computed(() => Boolean(accessToken.value))
  // ...
  return { accessToken, user, isLoggedIn, login, logout, fetchMe }
})
```

### 토큰 / 유저 정보 흐름

| 시점 | 동작 |
|------|------|
| 스토어 초기화 | `localStorage`에서 token 읽어 `ref` 초기화 |
| 로그인 성공 | `setAuth()` 호출 → `ref` 갱신 + `localStorage.setItem` |
| 로그아웃 | `ref` 초기화 + `localStorage.removeItem` |
| 마이페이지 진입 | `fetchMe()` → `GET /users/me` → `user.value` 갱신 |

### view에서 스토어 사용

```js
const authStore = useAuthStore()
const isLoggedIn = computed(() => authStore.isLoggedIn)  // 로컬 computed로 한 번 감쌈

authStore.login(payload)
authStore.logout()
authStore.fetchMe()
```

`authStore.isLoggedIn`을 template에서 직접 쓰지 않고 로컬 `computed`로 감싸는 것이 기존 패턴.

---

## 5. 라우터 등록 방식 / 인증 가드

### 히스토리 모드

```js
createRouter({ history: createWebHistory(), routes: [...] })
```

### 라우트 등록 형식

```js
{ path: '/경로', name: 'route-name', component: XxxView, meta: { requiresAuth: true } }
```

- `name`: kebab-case
- 보호 라우트는 반드시 `meta: { requiresAuth: true }` 추가

### 현재 등록된 라우트

| path | name | requiresAuth |
|------|------|:---:|
| `/` | `home` | |
| `/login` | `login` | |
| `/signup` | `signup` | |
| `/investor-profile` | `investor-profile` | ✓ |
| `/mypage` | `mypage` | ✓ |
| `/myinfo` | `myinfo` | ✓ |
| `/holdings` | `holdings` | ✓ |
| `/recommendations` | — | redirect → `/` |

### 인증 가드

`router.beforeEach` 전역 가드. `meta.requiresAuth && !authStore.isLoggedIn` 조건 시 로그인 페이지로 redirect하며 원래 경로를 query string으로 전달.

```js
router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})
```

### 프로그래매틱 내비게이션

```js
const router = useRouter()
router.push('/mypage')              // 경로 직접
router.push({ name: 'mypage' })    // 이름 사용 (권장)
```

---

## 6. main.js 플러그인 등록 순서

```js
createApp(App)
  .use(createPinia())   // 1. Pinia (라우터보다 먼저)
  .use(router)          // 2. Vue Router
  .mount('#app')
```

`style.css`는 `main.js`에서 `import './style.css'`로 전역 로드.
