<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppFooter from '../components/layout/AppFooter.vue'
import AppHeader from '../components/layout/AppHeader.vue'
import { getStockPrice, getSectors, listAllStocks } from '../api/stockApi'

const POLL_INTERVAL = 30_000

const router = useRouter()

const inputKeyword = ref('')
const keyword = ref('')
const selectedSector = ref('')
const sectors = ref([])
const stocks = ref([])
const priceMap = ref({})
const total = ref(0)
const page = ref(0)
const PAGE_SIZE = 20

const loading = ref(false)
const pricesLoading = ref(false)
const errorMsg = ref('')

const totalPages = computed(() => Math.ceil(total.value / PAGE_SIZE))

function selectSector(sector) {
  selectedSector.value = sector
  page.value = 0
  loadStocks()
}

function unwrap(response) {
  return response?.data?.data ?? response?.data ?? {}
}

async function loadSectors() {
  try {
    const res = await getSectors()
    sectors.value = res?.data?.data ?? res?.data ?? []
  } catch {
    sectors.value = []
  }
}

async function loadStocks() {
  loading.value = true
  errorMsg.value = ''
  priceMap.value = {}
  try {
    const res = unwrap(await listAllStocks(keyword.value, selectedSector.value, page.value, PAGE_SIZE))
    stocks.value = res.stocks ?? []
    total.value = res.total ?? 0
  } catch {
    errorMsg.value = '종목 목록을 불러올 수 없습니다.'
    stocks.value = []
    total.value = 0
    loading.value = false
    return
  }
  loading.value = false
  await loadPrices()
  startPolling()
}

let pollTimer = null

function stopPolling() {
  if (pollTimer !== null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(() => {
    if (!document.hidden) loadPrices()
  }, POLL_INTERVAL)
}

async function loadPrices() {
  if (!stocks.value.length || pricesLoading.value) return
  pricesLoading.value = true
  const snapshot = [...stocks.value]
  const results = await Promise.allSettled(
    snapshot.map((s) => getStockPrice(s.stockCode))
  )
  const newMap = {}
  results.forEach((res, i) => {
    if (res.status === 'fulfilled') {
      const raw = res.value?.data?.data ?? res.value?.data ?? null
      const price = Array.isArray(raw) ? (raw[0] ?? null) : raw
      if (price) newMap[snapshot[i].stockCode] = price
    }
  })
  priceMap.value = { ...priceMap.value, ...newMap }
  pricesLoading.value = false
}

function onVisibilityChange() {
  if (!document.hidden) loadPrices()
}

function search() {
  keyword.value = inputKeyword.value.trim()
  page.value = 0
  loadStocks()
}

function goPage(p) {
  if (p < 0 || p >= totalPages.value) return
  page.value = p
  loadStocks()
}

function priceOf(code) {
  const p = priceMap.value[code]
  return p ? Number(p.currentPrice ?? 0) : null
}

function changeOf(code) {
  const p = priceMap.value[code]
  return p ? Number(p.changeRate ?? 0) : null
}

onMounted(() => {
  loadSectors()
  loadStocks()
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onUnmounted(() => {
  stopPolling()
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<template>
  <div class="page-shell">
    <AppHeader />

    <main class="stocks-page">
      <div class="stocks-page-header">
        <div>
          <h1 class="stocks-page-title">전체 종목</h1>
          <p class="muted stocks-total">총 {{ total.toLocaleString() }}개 종목</p>
        </div>

        <form class="stock-search" @submit.prevent="search">
          <input v-model="inputKeyword" type="search" placeholder="종목명 또는 코드로 검색" />
          <button type="submit">검색</button>
        </form>
      </div>

      <!-- 섹터 필터 -->
      <div v-if="sectors.length" class="sector-chips">
        <button
          type="button"
          :class="['sector-chip', { active: selectedSector === '' }]"
          @click="selectSector('')"
        >전체</button>
        <button
          v-for="s in sectors"
          :key="s"
          type="button"
          :class="['sector-chip', { active: selectedSector === s }]"
          @click="selectSector(s)"
        >{{ s }}</button>
      </div>

      <p v-if="errorMsg" class="panel-message">{{ errorMsg }}</p>
      <div v-else-if="loading" class="panel-message">종목 목록을 불러오는 중입니다…</div>
      <div v-else-if="!stocks.length" class="panel-message">검색 결과가 없습니다.</div>

      <div v-else class="stocks-list">
        <button
          v-for="stock in stocks"
          :key="stock.stockCode"
          type="button"
          class="stock-row"
          @click="router.push('/report/' + stock.stockCode)"
        >
          <div class="stock-row-left">
            <strong class="stock-row-name">{{ stock.stockName }}</strong>
            <div class="stock-row-meta">
              <span class="code-tag">{{ stock.stockCode }}</span>
              <span class="market-tag" :class="stock.market === 'KOSPI' ? 'kospi' : 'kosdaq'">{{ stock.market }}</span>
              <span v-if="stock.sector" class="sector-tag">{{ stock.sector }}</span>
            </div>
          </div>

          <div class="stock-row-right">
            <span class="stock-row-price">
              {{ priceOf(stock.stockCode) != null ? priceOf(stock.stockCode).toLocaleString() + '원' : (pricesLoading ? '…' : '-') }}
            </span>
            <span
              v-if="changeOf(stock.stockCode) != null"
              class="stock-row-change"
              :class="changeOf(stock.stockCode) >= 0 ? 'positive' : 'negative'"
            >{{ changeOf(stock.stockCode) >= 0 ? '▲' : '▼' }}{{ Math.abs(changeOf(stock.stockCode)).toFixed(2) }}%</span>
            <span v-else class="stock-row-change muted">—</span>
          </div>
        </button>
      </div>

      <div v-if="!loading && totalPages > 1" class="stocks-pagination">
        <button type="button" :disabled="page === 0" @click="goPage(page - 1)">이전</button>
        <span>{{ page + 1 }} / {{ totalPages }}</span>
        <button type="button" :disabled="page >= totalPages - 1" @click="goPage(page + 1)">다음</button>
      </div>
    </main>

    <AppFooter />
  </div>
</template>
