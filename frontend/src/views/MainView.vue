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
const sectorRecommendations = ref([])
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
  return item.stockName || item.name || item.stock_code || item.stockCode || '추천 종목'
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
    recommendationMessage.value = '로그인하면 맞춤 추천 TOP 10과 관심 분야 TOP 3를 볼 수 있습니다.'
    return
  }

  try {
    const [overall, sector] = await Promise.allSettled([
      getRecommendations(),
      getRecommendations('IT'),
    ])

    overallRecommendations.value = overall.status === 'fulfilled' ? unwrap(overall.value).slice(0, 10) : []
    sectorRecommendations.value = sector.status === 'fulfilled' ? unwrap(sector.value).slice(0, 3) : []

    if (overallRecommendations.value.length === 0 && sectorRecommendations.value.length === 0) {
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
          <h1>공시와 투자 성향을 한 화면에서 연결합니다.</h1>
          <p>
            종목 검색, AI 추천, 보유 종목 현황, 마이페이지의 투자 성향을 명세서 API 흐름에 맞춰 제공합니다.
          </p>

          <form class="stock-search" @submit.prevent="submitSearch">
            <input v-model="keyword" type="search" placeholder="예: 삼성전자, 005930" />
            <button type="submit">{{ searchLoading ? '검색 중' : '검색' }}</button>
          </form>

          <div v-if="searchResults.length || searchMessage" class="search-panel">
            <p v-if="searchMessage" class="panel-message">{{ searchMessage }}</p>
            <button v-for="stock in searchResults" :key="stockCode(stock)" type="button" class="search-result">
              <strong>{{ stockName(stock) }}</strong>
              <span>{{ stockCode(stock) }}</span>
            </button>
          </div>
        </div>

        <aside class="recommendation-summary">
          <div class="summary-header">
            <span>AI 투자성향 요약</span>
            <button type="button" @click="goToRecommendations">리포트 보기</button>
          </div>

          <div class="ranking-grid">
            <section>
              <h2>섹터 추천 TOP 3</h2>
              <ol>
                <li v-for="item in sectorRecommendations" :key="recommendationName(item)">
                  <span>{{ recommendationName(item) }}</span>
                  <b>{{ item.score ?? '-' }}</b>
                </li>
              </ol>
            </section>

            <section>
              <h2>종합 추천 TOP 10</h2>
              <ol>
                <li v-for="item in overallRecommendations" :key="recommendationName(item)">
                  <span>{{ recommendationName(item) }}</span>
                  <b>{{ item.score ?? '-' }}</b>
                </li>
              </ol>
            </section>
          </div>

          <p v-if="recommendationMessage" class="panel-message">{{ recommendationMessage }}</p>
        </aside>
      </section>

      <section class="service-section">
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
      </section>
    </main>

    <AppFooter />
  </div>
</template>
