<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Chart, LinearScale, TimeScale, TimeSeriesScale, Tooltip } from 'chart.js'
import 'chartjs-adapter-date-fns'
import { CandlestickController, CandlestickElement } from 'chartjs-chart-financial'
import AppFooter from '../components/layout/AppFooter.vue'
import AppHeader from '../components/layout/AppHeader.vue'
import { getStockAnalysis, getStockChart, getStockFinancial, getStockInfo, getStockPrice } from '../api/stockApi'

Chart.register(LinearScale, TimeScale, TimeSeriesScale, Tooltip, CandlestickController, CandlestickElement)

const route = useRoute()
const code = computed(() => route.params.code)

const price = ref(null)
const chartData = ref([])
const financial = ref(null)
const analysis = ref(null)
const period = ref('D')

const loading = ref(false)
const chartLoading = ref(false)
const analysisLoading = ref(false)

const priceError = ref('')
const chartError = ref('')
const financialError = ref('')
const analysisError = ref('')

const canvasRef = ref(null)
let chartInstance = null

const periods = [
  { label: '일', value: 'D' },
  { label: '주', value: 'W' },
  { label: '월', value: 'M' },
]

function unwrap(response) {
  return response?.data?.data ?? response?.data ?? []
}

function formatAmount(value) {
  if (value == null) return '-'
  const num = Number(value)
  if (isNaN(num)) return String(value)
  if (Math.abs(num) >= 1e12) return `${(num / 1e12).toFixed(1)}조원`
  if (Math.abs(num) >= 1e8) return `${Math.round(num / 1e8).toLocaleString()}억원`
  return `${num.toLocaleString()}원`
}

function formatRate(value) {
  if (value == null) return '-'
  const num = Number(value)
  if (isNaN(num)) return String(value)
  return `${num.toFixed(1)}%`
}

function formatMultiple(value) {
  if (value == null || value === '' || value === '0' || value === '0.00') return '-'
  const num = Number(value)
  if (isNaN(num) || num <= 0) return '-'
  return `${num.toFixed(2)}배`
}

const currentPrice = computed(() => {
  if (!price.value) return null
  return price.value.currentPrice ?? price.value.price ?? price.value.clpr ?? price.value.stckPrpr ?? null
})

const priceChange = computed(() => {
  if (!price.value) return null
  return price.value.changeAmount ?? price.value.change ?? price.value.priceChange ?? price.value.prdyVrss ?? null
})

const priceChangeRate = computed(() => {
  if (!price.value) return null
  return price.value.changeRate ?? price.value.changePercent ?? price.value.fluctuationRate ?? price.value.prdyCtrt ?? null
})

const isPositive = computed(() => Number(priceChange.value ?? 0) >= 0)

const stockName = ref('')
const stockInfo = ref(null)

const POLL_INTERVAL = 30_000
let priceTimer = null

function stopPricePolling() {
  if (priceTimer !== null) { clearInterval(priceTimer); priceTimer = null }
}

function startPricePolling() {
  stopPricePolling()
  priceTimer = setInterval(() => {
    if (!document.hidden) refreshPrice()
  }, POLL_INTERVAL)
}

async function refreshPrice() {
  try {
    const raw = unwrap(await getStockPrice(code.value))
    price.value = Array.isArray(raw) ? (raw[0] ?? null) : raw
  } catch { /* ignore */ }
}

function onPriceVisibilityChange() {
  if (!document.hidden) refreshPrice()
}

function parseKisDate(dateStr) {
  if (!dateStr) return null
  // KIS API: "20241231" → "2024-12-31" (ISO 포맷)
  if (/^\d{8}$/.test(dateStr)) {
    return `${dateStr.slice(0, 4)}-${dateStr.slice(4, 6)}-${dateStr.slice(6, 8)}`
  }
  return dateStr
}

function renderChart() {
  if (!canvasRef.value || !chartData.value.length) return

  if (chartInstance) {
    chartInstance.destroy()
    chartInstance = null
  }

  const items = Array.isArray(chartData.value) ? chartData.value : [chartData.value]
  const candles = items
    .map((item) => ({
      x: new Date(parseKisDate(item.date)).getTime(),
      o: Number(item.openPrice ?? 0),
      h: Number(item.highPrice ?? 0),
      l: Number(item.lowPrice ?? 0),
      c: Number(item.closePrice ?? 0),
    }))
    .filter((c) => !isNaN(c.x) && c.o && c.h && c.l && c.c)
    .sort((a, b) => a.x - b.x) // KIS는 최신→과거 순으로 주므로 오름차순 정렬

  chartInstance = new Chart(canvasRef.value, {
    type: 'candlestick',
    data: {
      datasets: [
        {
          label: '주가',
          data: candles,
          backgroundColors: {
            up: 'rgb(229, 62, 62)',
            down: 'rgb(49, 130, 206)',
            unchanged: 'rgb(104, 118, 110)',
          },
          borderColors: {
            up: 'rgb(229, 62, 62)',
            down: 'rgb(49, 130, 206)',
            unchanged: 'rgb(104, 118, 110)',
          },
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      elements: {
        candlestick: {
          backgroundColors: {
            up: 'rgb(229, 62, 62)',
            down: 'rgb(49, 130, 206)',
            unchanged: 'rgb(104, 118, 110)',
          },
          borderColors: {
            up: 'rgb(229, 62, 62)',
            down: 'rgb(49, 130, 206)',
            unchanged: 'rgb(104, 118, 110)',
          },
        },
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx) => {
              const d = ctx.raw
              return [
                `시가: ${Number(d.o).toLocaleString()}원`,
                `고가: ${Number(d.h).toLocaleString()}원`,
                `저가: ${Number(d.l).toLocaleString()}원`,
                `종가: ${Number(d.c).toLocaleString()}원`,
              ]
            },
          },
        },
      },
      scales: {
        x: {
          type: 'timeseries', // 실제 데이터 있는 날짜만 표시 → 주말/공휴일 갭 없음
          time: {
            unit: period.value === 'M' ? 'month' : period.value === 'W' ? 'week' : 'day',
            displayFormats: { day: 'MM.dd', week: 'yy.MM.dd', month: 'yy.MM' },
          },
          grid: { display: false },
          ticks: { maxTicksLimit: 8, color: '#68766e', font: { size: 11 } },
        },
        y: {
          position: 'right',
          grid: { color: 'rgba(203, 216, 207, 0.45)' },
          ticks: {
            color: '#68766e',
            font: { size: 11 },
            callback: (v) => Number(v).toLocaleString(),
          },
        },
      },
    },
  })
}

async function loadChart() {
  chartLoading.value = true
  chartError.value = ''
  try {
    chartData.value = unwrap(await getStockChart(code.value, period.value))
  } catch {
    chartError.value = '차트 데이터를 불러올 수 없습니다.'
    chartLoading.value = false
    return
  }
  chartLoading.value = false
  await nextTick()
  renderChart()
}

async function loadMainData() {
  loading.value = true
  priceError.value = ''
  chartError.value = ''
  financialError.value = ''

  const [priceRes, chartRes, financialRes, infoRes] = await Promise.allSettled([
    getStockPrice(code.value),
    getStockChart(code.value, period.value),
    getStockFinancial(code.value),
    getStockInfo(code.value),
  ])

  if (priceRes.status === 'fulfilled') {
    const raw = unwrap(priceRes.value)
    price.value = Array.isArray(raw) ? (raw[0] ?? null) : raw
  } else {
    priceError.value = '시세 정보를 불러올 수 없습니다.'
  }

  if (chartRes.status === 'fulfilled') {
    chartData.value = unwrap(chartRes.value)
  } else {
    chartError.value = '차트 데이터를 불러올 수 없습니다.'
  }

  if (financialRes.status === 'fulfilled') {
    const raw = unwrap(financialRes.value)
    financial.value = Array.isArray(raw) ? (raw[0] ?? null) : raw
  } else {
    financialError.value = '재무 정보를 불러올 수 없습니다.'
  }

  if (infoRes.status === 'fulfilled') {
    const raw = unwrap(infoRes.value)
    stockInfo.value = Array.isArray(raw) ? (raw[0] ?? null) : raw
    stockName.value = stockInfo.value?.stockName ?? code.value
  } else {
    stockName.value = code.value
  }

  loading.value = false
  await nextTick()
  renderChart()
}

async function loadAnalysis() {
  analysisLoading.value = true
  analysisError.value = ''
  try {
    const raw = unwrap(await getStockAnalysis(code.value))
    analysis.value = Array.isArray(raw) ? (raw[0] ?? null) : raw
  } catch {
    analysisError.value = 'AI 분석을 불러올 수 없습니다.'
  }
  analysisLoading.value = false
}

onMounted(async () => {
  await loadMainData()
  startPricePolling()
  document.addEventListener('visibilitychange', onPriceVisibilityChange)
  loadAnalysis()
})
watch(period, loadChart)
onUnmounted(() => {
  if (chartInstance) chartInstance.destroy()
  stopPricePolling()
  document.removeEventListener('visibilitychange', onPriceVisibilityChange)
})
</script>

<template>
  <div class="page-shell">
    <AppHeader />

    <main class="report-page">
      <div v-if="loading" class="report-loading">
        <p>리포트를 불러오는 중입니다…</p>
      </div>

      <template v-else>
        <!-- 1. 종목명 / 현재 시세 -->
        <section class="report-price-section">
          <div class="report-name-price-row">
            <h1 class="report-stock-name">{{ stockName }} ({{ code }})</h1>
            <div v-if="price && !priceError" class="report-price-row">
              <strong
                class="report-current-price"
                :class="isPositive ? 'positive' : 'negative'"
              >
                {{ Number(currentPrice ?? 0).toLocaleString() }}원
              </strong>
              <span
                class="report-change"
                :class="isPositive ? 'positive' : 'negative'"
              >
                {{ isPositive ? '+' : '' }}{{ Number(priceChange ?? 0).toLocaleString() }}
                ({{ isPositive ? '+' : '' }}{{ formatRate(priceChangeRate) }})
              </span>
            </div>
            <p v-else-if="priceError" class="panel-message">{{ priceError }}</p>
          </div>
          <p v-if="stockInfo?.intro" class="report-stock-intro muted">{{ stockInfo.intro }}</p>
        </section>

        <!-- 2. 주가 차트 -->
        <section>
          <div class="report-chart-card">
            <div class="report-chart-header">
              <h2>주가 차트</h2>
              <div class="report-chart-tabs">
                <button
                  v-for="tab in periods"
                  :key="tab.value"
                  type="button"
                  :class="{ active: period === tab.value }"
                  :disabled="chartLoading"
                  @click="period = tab.value"
                >
                  {{ tab.label }}
                </button>
              </div>
            </div>

            <div class="report-chart-body">
              <p v-if="chartError" class="panel-message">{{ chartError }}</p>
              <div v-else-if="chartLoading" class="chart-placeholder">
                <p class="panel-message">차트 로딩 중…</p>
              </div>
              <canvas v-else ref="canvasRef"></canvas>
            </div>
          </div>
        </section>

        <!-- 4. AI 3분 리포트 -->
        <section>
          <h2 class="report-section-title">AI 3분 리포트</h2>

          <p v-if="analysisError" class="panel-message">{{ analysisError }}</p>

          <div v-else-if="analysisLoading" class="report-analysis-card">
            <p class="panel-message">AI 리포트 생성 중입니다… 잠시만 기다려 주세요.</p>
          </div>

          <div v-else-if="analysis" class="report-analysis-card">
            <p>{{ typeof analysis === 'string' ? analysis : (analysis.content ?? analysis.summary ?? analysis.report ?? analysis.analysis) }}</p>
          </div>

          <p v-else class="panel-message">AI 분석 데이터가 없습니다.</p>
        </section>

        <!-- 3. 재무 핵심 지표 -->
        <section>
          <h2 class="report-section-title">재무 핵심 지표</h2>

          <p v-if="financialError" class="panel-message">{{ financialError }}</p>

          <div v-else-if="financial" class="report-financial-grid">
            <div class="metric-card">
              <span>매출액</span>
              <small class="metric-desc">회사가 한 해 동안 물건·서비스를 팔아 번 돈이에요</small>
              <strong>{{ formatAmount(financial.revenue ?? financial.totalRevenue ?? financial.매출액) }}</strong>
            </div>
            <div class="metric-card">
              <span>영업이익</span>
              <small class="metric-desc">비용을 다 빼고 실제로 남긴 이익이에요</small>
              <strong>{{ formatAmount(financial.operatingIncome ?? financial.operatingProfit ?? financial.영업이익) }}</strong>
            </div>
            <div class="metric-card">
              <span>당기순이익</span>
              <small class="metric-desc">세금까지 다 내고 최종적으로 회사에 남은 돈이에요</small>
              <strong>{{ formatAmount(financial.netIncome ?? financial.netProfit ?? financial.당기순이익) }}</strong>
            </div>
            <div class="metric-card">
              <span>부채비율</span>
              <small class="metric-desc">빚이 자기 돈의 몇 배인지를 의미해요 - 낮을수록 안전해요</small>
              <strong>{{ formatRate(financial.debtRatio ?? financial.debtToEquity ?? financial.부채비율) }}</strong>
            </div>
            <div class="metric-card">
              <span>PER</span>
              <small class="metric-desc">이익 대비 주가 수준을 의미해요 - 낮으면 저평가됐을 가능성이 있어요</small>
              <strong>{{ formatMultiple(price?.per) }}</strong>
            </div>
            <div class="metric-card">
              <span>PBR</span>
              <small class="metric-desc">자산 대비 주가 수준을 의미해요. 1배 미만이면 자산보다 싸게 거래 중이에요</small>
              <strong>{{ formatMultiple(price?.pbr) }}</strong>
            </div>
          </div>

          <p v-else class="panel-message">재무 데이터가 없습니다.</p>
        </section>

        <!-- 5. 고지 문구 -->
        <p class="report-disclosure muted">
          본 화면에서 제공하는 정보는 학습 및 투자 참고용이며, 실제 투자 판단과 그에 따른 책임은 사용자에게 있습니다.
        </p>
      </template>
    </main>

    <AppFooter />
  </div>
</template>
