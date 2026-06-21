<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import AppFooter from '../components/layout/AppFooter.vue'
import AppHeader from '../components/layout/AppHeader.vue'
import { getRecommendations } from '../api/recommendationApi'
import { searchStocks } from '../api/stockApi'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()
const keyword = ref('')
const searchResults = ref([])
const searchLoading = ref(false)
const searchMessage = ref('')
const overallRecommendations = ref([])
const recommendationMessage = ref('')

const isLoggedIn = computed(() => authStore.isLoggedIn)

const serviceCards = [
  {
    title: '종목 검색',
    description: '종목명 또는 종목코드 2글자 이상으로 빠르게 검색합니다.',
  },
  {
    title: 'AI 3분 분석',
    description: '가격, 재무, 리포트 요약을 한 번에 볼 수 있는 분석 흐름을 준비했습니다.',
  },
  {
    title: '투자 성향 추천',
    description: '투자 경험, 위험 감수 성향, 관심 분야를 기준으로 추천 후보를 정리합니다.',
  },
]

function unwrap(response) {
  return response?.data?.data ?? response?.data ?? []
}

function stockName(stock) {
  return stock.stockName || stock.name || stock.stock_name || '종목명 없음'
}

function stockCode(stock) {
  return stock.stockCode || stock.code || stock.stock_code || '-'
}

function recommendationName(item) {
  return item.stockName || item.name || item.stockCode || item.stock_code || '추천 종목'
}

// TODO: API 연동 — 인기 종목 실시간 데이터 연동 필요
const POPULAR_STOCKS = [
  { name: '삼성전자', code: '005930' },
  { name: 'SK하이닉스', code: '000660' },
  { name: '카카오', code: '035720' },
  { name: 'NAVER', code: '035420' },
  { name: '현대자동차', code: '005380' },
]

// TODO: API 연동 — 추천 종목 실시간 등락 데이터 연동 필요
const DUMMY_CHANGES = [+1.23, -0.87, +2.10, -1.45, +0.67, +3.21, -2.34, +0.92, -0.56, +1.78]
function getItemChange(index) {
  return DUMMY_CHANGES[index] ?? 0
}

async function submitSearch() {
  const trimmed = keyword.value.trim()
  searchMessage.value = ''

  if (trimmed.length < 2) {
    searchResults.value = []
    searchMessage.value = '종목명 또는 코드를 2글자 이상 입력하세요.'
    return
  }

  searchLoading.value = true

  try {
    searchResults.value = unwrap(await searchStocks(trimmed)).slice(0, 8)
    if (searchResults.value.length === 0) {
      searchMessage.value = '검색 결과가 없습니다.'
    }
  } catch (error) {
    searchResults.value = []
    searchMessage.value = '종목 검색 API 응답을 확인할 수 없습니다.'
  } finally {
    searchLoading.value = false
  }
}

function goToRecommendations() {
  if (!isLoggedIn.value) {
    router.push('/login')
    return
  }

  router.push('/mypage')
}

async function loadRecommendations() {
  if (!isLoggedIn.value) {
    recommendationMessage.value = '로그인하면 맞춤 추천 TOP 10을 볼 수 있습니다.'
    return
  }

  try {
    const res = await getRecommendations()
    const data = res?.data ?? {}

    overallRecommendations.value = (data.overall ?? []).slice(0, 10)

    if (overallRecommendations.value.length === 0) {
      recommendationMessage.value = '추천 데이터가 아직 없습니다. 투자 성향을 먼저 등록해 주세요.'
    }
  } catch (error) {
    recommendationMessage.value = '추천 API 응답을 확인할 수 없습니다.'
  }
}

onMounted(loadRecommendations)
</script>

<template>
  <div class="page-shell">
    <AppHeader />

    <main>
      <section class="hero-section">
        <div class="hero-copy">
          <p class="eyebrow">AI stock navigator</p>
          <h1>어려운 기업 공시, 3분이면 끝</h1>
          <p>
            궁금한 종목을 검색하면 가격·재무·공시를 AI가 쉬운 말로 정리해드려요. 
          </p>

          <form class="stock-search" @submit.prevent="submitSearch">
            <input v-model="keyword" type="search" placeholder="예: 삼성전자, 005930" />
            <button type="submit">{{ searchLoading ? '검색 중' : '검색' }}</button>
          </form>

          <!-- TODO: API 연동 — 인기 종목 실시간 데이터 연동 필요 -->
          <div class="popular-chips">
            <span class="popular-label">인기 종목</span>
            <button
              v-for="stock in POPULAR_STOCKS"
              :key="stock.code"
              type="button"
              class="popular-chip"
              @click="router.push('/report/' + stock.code)"
            >
              {{ stock.name }}
            </button>
          </div>

          <div v-if="searchResults.length || searchMessage" class="search-panel">
            <p v-if="searchMessage" class="panel-message">{{ searchMessage }}</p>
            <button v-for="stock in searchResults" :key="stockCode(stock)" type="button" class="search-result"
              @click="router.push('/report/' + stockCode(stock))">
              <strong>{{ stockName(stock) }}</strong>
              <span>{{ stockCode(stock) }}</span>
            </button>
          </div>
        </div>

        <aside class="recommendation-summary">
          <div class="summary-header">
            <span>나의 AI 추천 종목 TOP 10</span>
            <!-- <button type="button" @click="goToRecommendations">리포트 보기</button> -->
          </div>

          <div class="rec-list">
            <p v-if="!overallRecommendations.length && !recommendationMessage" class="panel-message">추천 데이터가 없습니다.</p>
            <button
              v-for="(item, index) in overallRecommendations"
              :key="recommendationName(item)"
              type="button"
              class="rec-item"
              @click="router.push('/report/' + item.stockCode)"
            >
              <em class="rec-rank">{{ index + 1 }}</em>
              <strong class="rec-name">{{ recommendationName(item) }} <span class="rec-code">({{ item.stockCode }})</span></strong>
              <!-- TODO: API 연동 — 실시간 등락 데이터 연동 필요 -->
              <span
                class="rec-change"
                :class="getItemChange(index) >= 0 ? 'positive' : 'negative'"
              >{{ getItemChange(index) >= 0 ? '▲' : '▼' }}{{ Math.abs(getItemChange(index)).toFixed(2) }}%</span>
              <b class="rec-score">{{ item.score != null ? Math.round(item.score) : '-' }}<small v-if="item.score != null" class="rec-score-unit">점</small></b>
            </button>
          </div>

          <p v-if="recommendationMessage" class="panel-message">{{ recommendationMessage }}</p>
        </aside>
      </section>

      <!-- <section class="service-section">
        <div class="section-title">
          <p class="eyebrow">Service</p>
          <h2>초보 투자자가 바로 쓰는 핵심 흐름</h2>
        </div>

        <div class="service-grid">
          <article v-for="card in serviceCards" :key="card.title">
            <h3>{{ card.title }}</h3>
            <p>{{ card.description }}</p>
          </article>
        </div>
      </section> -->
    </main>

    <AppFooter />
  </div>
</template>
