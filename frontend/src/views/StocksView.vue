<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppFooter from '../components/layout/AppFooter.vue'
import AppHeader from '../components/layout/AppHeader.vue'
import { getTopMarketCapStocks } from '../api/stockApi'

const router = useRouter()
const stocks = ref([])
const loading = ref(false)
const errorMsg = ref('')
const fetchedAt = ref('')

const leftColumn = computed(() => stocks.value.slice(0, 15))
const rightColumn = computed(() => stocks.value.slice(15, 30))

function formatFetchedAt() {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${now.getFullYear()}.${pad(now.getMonth() + 1)}.${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())} 기준`
}

function changeClass(sign) {
  if (sign === 'UP') return 'positive'
  if (sign === 'DOWN') return 'negative'
  return 'muted'
}

function changeSymbol(sign) {
  if (sign === 'UP') return '▲'
  if (sign === 'DOWN') return '▼'
  return '–'
}

const POLL_INTERVAL = 10_000
let pollTimer = null

async function loadTopStocks() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await getTopMarketCapStocks()
    stocks.value = res?.data ?? []
    fetchedAt.value = formatFetchedAt()
  } catch {
    errorMsg.value = '시총 상위 종목을 불러올 수 없습니다.'
  } finally {
    loading.value = false
  }
}

async function refreshStocks() {
  try {
    const res = await getTopMarketCapStocks()
    stocks.value = res?.data ?? []
    fetchedAt.value = formatFetchedAt()
  } catch { /* ignore */ }
}

function onVisibilityChange() {
  if (!document.hidden) refreshStocks()
}

onMounted(async () => {
  await loadTopStocks()
  pollTimer = setInterval(() => { if (!document.hidden) refreshStocks() }, POLL_INTERVAL)
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onUnmounted(() => {
  clearInterval(pollTimer)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<template>
  <div class="page-shell">
    <AppHeader />

    <main class="recs-page top30-page">
      <div v-if="loading" class="recs-loading">
        <p>불러오는 중입니다…</p>
      </div>

      <template v-else>
        <!-- 페이지 헤더 -->
        <section class="recs-header">
          <p class="eyebrow">코스피200</p>
          <h1>시가총액 TOP 30 </h1>
          <p>{{ fetchedAt }} </p>
          
        </section>

        <!-- 종목 리스트 -->
        <section>
          <!-- <div class="recs-section-head">
            <h2>시가총액 상위 30개 종목</h2>
            <span class="recs-badge">실시간 시세</span>
          </div> -->

          <p v-if="errorMsg" class="panel-message">{{ errorMsg }}</p>

          <div v-else class="recs-overall-card top30-dual-card">
            <ol class="recs-rank-list">
              <li
                v-for="stock in leftColumn"
                :key="stock.stockCode"
                class="recs-rank-item top30-rank-item recs-clickable"
                @click="router.push('/report/' + stock.stockCode)"
              >
                <em class="recs-rank-num">{{ stock.rank }}</em>
                <span class="top30-item-name">
                  <strong>{{ stock.stockName }}</strong>
                  <small>({{ stock.stockCode }})</small>
                </span>
                <span class="rec-price-group">
                  <span class="rec-price">{{ stock.currentPrice.toLocaleString() }}원</span>
                  <span class="rec-change" :class="changeClass(stock.priceChangeSign)">
                    {{ changeSymbol(stock.priceChangeSign) }}{{ Math.abs(stock.changeRate).toFixed(2) }}%
                  </span>
                </span>
              </li>
            </ol>

            <ol class="recs-rank-list">
              <li
                v-for="stock in rightColumn"
                :key="stock.stockCode"
                class="recs-rank-item top30-rank-item recs-clickable"
                @click="router.push('/report/' + stock.stockCode)"
              >
                <em class="recs-rank-num">{{ stock.rank }}</em>
                <span class="top30-item-name">
                  <strong>{{ stock.stockName }}</strong>
                  <small>({{ stock.stockCode }})</small>
                </span>
                <span class="rec-price-group">
                  <span class="rec-price">{{ stock.currentPrice.toLocaleString() }}원</span>
                  <span class="rec-change" :class="changeClass(stock.priceChangeSign)">
                    {{ changeSymbol(stock.priceChangeSign) }}{{ Math.abs(stock.changeRate).toFixed(2) }}%
                  </span>
                </span>
              </li>
            </ol>
          </div>
        </section>

        <p class="recs-disclosure muted">
          본 화면에서 제공하는 정보는 학습 및 투자 참고용이며, 실제 투자 판단과 그에 따른 책임은 사용자에게 있습니다.
        </p>
      </template>
    </main>

    <AppFooter />
  </div>
</template>
