<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import AppFooter from '../components/layout/AppFooter.vue'
import AppHeader from '../components/layout/AppHeader.vue'
import { createHolding, deleteHolding, extractHoldingsFromImage, getHoldingDiagnosis, getHoldings, updateHolding } from '../api/holdingApi'
import { searchStocks } from '../api/stockApi'

const holdings = ref([])
const diagnosis = ref(null)
const holdingsLoading = ref(false)
const diagnosisLoading = ref(false)
const holdingSaving = ref(false)
const holdingEditingId = ref(null)
const holdingMessage = ref('')
const holdingSearchKeyword = ref('')
const holdingSearchResults = ref([])

// 사진 추출
const showExtract = ref(false)
const extractFile = ref(null)
const extractFileInput = ref(null)
const extracting = ref(false)
const extractError = ref('')
const extractedItems = ref([])

function onExtractFileChange(e) {
  extractFile.value = e.target.files[0] ?? null
}

async function runExtract() {
  if (!extractFile.value) {
    extractError.value = '이미지를 선택해 주세요.'
    return
  }
  extracting.value = true
  extractError.value = ''
  extractedItems.value = []
  try {
    const res = await extractHoldingsFromImage(extractFile.value)
    const list = res?.data?.data ?? res?.data ?? []
    extractedItems.value = list.map((item) => ({
      ...item,
      selectedCode: item.stockCode || (item.candidates?.[0]?.code ?? ''),
    }))
    if (!extractedItems.value.length) extractError.value = '추출된 종목이 없습니다.'
  } catch {
    extractError.value = '이미지 분석에 실패했습니다.'
  } finally {
    extracting.value = false
  }
}

async function addExtractedItem(item) {
  const code = item.selectedCode
  if (!code) {
    holdingMessage.value = '종목 코드를 선택해 주세요.'
    return
  }
  const candidateName = item.candidates?.find((c) => c.code === code)?.name ?? item.name
  holdingSaving.value = true
  holdingMessage.value = ''
  try {
    await createHolding({
      stockCode: code,
      quantity: Number(item.quantity ?? 1),
      purchasePrice: Number(item.avgPrice ?? 0),
      purchaseDate: new Date().toISOString().slice(0, 10),
    })
    extractedItems.value = extractedItems.value.filter((i) => i !== item)
    holdingMessage.value = `${candidateName} 추가 완료`
    loadHoldings()
    loadDiagnosis()
  } catch (e) {
    holdingMessage.value = e.response?.data?.error?.message || '추가에 실패했습니다.'
  } finally {
    holdingSaving.value = false
  }
}

const holdingForm = reactive({
  stockCode: '',
  stockName: '',
  quantity: 1,
  purchasePrice: 0,
  purchaseDate: new Date().toISOString().slice(0, 10),
})

const totalPurchase = computed(() => {
  return holdings.value.reduce((sum, item) => sum + Number(item.purchaseAmount ?? item.quantity * item.purchasePrice ?? 0), 0)
})

const totalEvaluation = computed(() => {
  return holdings.value.reduce((sum, item) => {
    const fallback = Number(item.purchaseAmount ?? item.quantity * item.purchasePrice ?? 0)
    return sum + Number(item.evaluationAmount ?? fallback)
  }, 0)
})

const totalProfitAmount = computed(() => totalEvaluation.value - totalPurchase.value)

const totalProfitRate = computed(() => {
  if (!totalPurchase.value) return 0
  return (totalProfitAmount.value / totalPurchase.value) * 100
})

function unwrap(response) {
  return response?.data?.data ?? response?.data ?? []
}

function formatMoney(value) {
  return Math.round(Number(value || 0)).toLocaleString('ko-KR')
}

function stockLabel(item) {
  return item.stockName || item.stock_name || item.name || item.stockCode || item.stock_code || '보유 종목'
}

function stockCode(item) {
  return item.stockCode || item.stock_code || item.code || '-'
}

function holdingProfitAmount(item) {
  if (item.profitAmount != null) return Number(item.profitAmount)
  const quantity = Number(item.quantity ?? 0)
  const currentPrice = Number(item.currentPrice ?? item.purchasePrice ?? 0)
  const purchasePrice = Number(item.purchasePrice ?? 0)
  return quantity * (currentPrice - purchasePrice)
}

function holdingProfitRate(item) {
  if (item.profitRate != null) return Number(item.profitRate)
  const purchaseAmount = Number(item.purchaseAmount ?? item.quantity * item.purchasePrice ?? 0)
  if (!purchaseAmount) return 0
  return (holdingProfitAmount(item) / purchaseAmount) * 100
}

function resetHoldingForm() {
  holdingEditingId.value = null
  holdingForm.stockCode = ''
  holdingForm.stockName = ''
  holdingForm.quantity = 1
  holdingForm.purchasePrice = 0
  holdingForm.purchaseDate = new Date().toISOString().slice(0, 10)
  holdingSearchKeyword.value = ''
  holdingSearchResults.value = []
}

async function searchHoldingStock() {
  const keyword = holdingSearchKeyword.value.trim()
  holdingMessage.value = ''

  if (keyword.length < 2) {
    holdingSearchResults.value = []
    holdingMessage.value = '종목명 또는 종목코드를 2글자 이상 입력해 주세요.'
    return
  }

  try {
    holdingSearchResults.value = unwrap(await searchStocks(keyword)).slice(0, 8)
    if (holdingSearchResults.value.length === 0) holdingMessage.value = '검색 결과가 없습니다.'
  } catch (error) {
    holdingSearchResults.value = []
    holdingMessage.value = '종목 검색에 실패했습니다.'
  }
}

function selectHoldingStock(stock) {
  holdingForm.stockCode = stockCode(stock)
  holdingForm.stockName = stockLabel(stock)
  holdingSearchKeyword.value = `${stockLabel(stock)} (${stockCode(stock)})`
  holdingSearchResults.value = []
}

function editHolding(item) {
  holdingEditingId.value = item.id
  holdingForm.stockCode = stockCode(item)
  holdingForm.stockName = stockLabel(item)
  holdingForm.quantity = Number(item.quantity ?? 1)
  holdingForm.purchasePrice = Number(item.purchasePrice ?? 0)
  holdingForm.purchaseDate = item.purchaseDate || new Date().toISOString().slice(0, 10)
  holdingSearchKeyword.value = `${stockLabel(item)} (${stockCode(item)})`
  holdingSearchResults.value = []
  holdingMessage.value = ''
}

async function loadHoldings() {
  holdingsLoading.value = true
  try {
    holdings.value = unwrap(await getHoldings())
  } catch {
    holdings.value = []
  } finally {
    holdingsLoading.value = false
  }
}

async function loadDiagnosis() {
  diagnosisLoading.value = true
  try {
    diagnosis.value = unwrap(await getHoldingDiagnosis())
  } catch {
    diagnosis.value = null
  } finally {
    diagnosisLoading.value = false
  }
}

async function saveHolding() {
  if (!holdingForm.stockCode) {
    holdingMessage.value = '종목을 먼저 선택해 주세요.'
    return
  }

  holdingSaving.value = true
  holdingMessage.value = ''

  const payload = {
    stockCode: holdingForm.stockCode,
    quantity: Number(holdingForm.quantity),
    purchasePrice: Number(holdingForm.purchasePrice),
    purchaseDate: holdingForm.purchaseDate,
  }

  try {
    if (holdingEditingId.value) {
      await updateHolding(holdingEditingId.value, payload)
      holdingMessage.value = '보유 종목을 수정했습니다.'
    } else {
      await createHolding(payload)
      holdingMessage.value = '보유 종목을 추가했습니다.'
    }
    loadHoldings()
    loadDiagnosis()
    resetHoldingForm()
  } catch (error) {
    holdingMessage.value = error.response?.data?.error?.message || '보유 종목 저장에 실패했습니다.'
  } finally {
    holdingSaving.value = false
  }
}

async function removeHolding(item) {
  holdingSaving.value = true
  holdingMessage.value = ''

  try {
    await deleteHolding(item.id)
    loadHoldings()
    loadDiagnosis()
    if (holdingEditingId.value === item.id) resetHoldingForm()
    holdingMessage.value = '보유 종목을 삭제했습니다.'
  } catch (error) {
    holdingMessage.value = error.response?.data?.error?.message || '보유 종목 삭제에 실패했습니다.'
  } finally {
    holdingSaving.value = false
  }
}

const POLL_INTERVAL = 10_000
let pollTimer = null

async function refreshHoldings() {
  try {
    holdings.value = unwrap(await getHoldings())
  } catch { /* ignore */ }
}

function onVisibilityChange() {
  if (!document.hidden) refreshHoldings()
}

onMounted(() => {
  loadHoldings()
  loadDiagnosis()
  pollTimer = setInterval(() => { if (!document.hidden) refreshHoldings() }, POLL_INTERVAL)
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

    <main class="mypage">
      <section class="recs-header">
        <p class="eyebrow">Portfolio</p>
        <h1>투자현황</h1>
      </section>

      <section class="dashboard-grid">
        <article class="metric-card">
          <span>총 매입금액</span>
          <strong>{{ formatMoney(totalPurchase) }}원</strong>
        </article>
        <article class="metric-card">
          <span>총 평가금액</span>
          <strong>{{ formatMoney(totalEvaluation) }}원</strong>
        </article>
        <article class="metric-card">
          <span>전체 손익 / 수익률</span>
          <strong :class="{ positive: totalProfitRate >= 0, negative: totalProfitRate < 0 }">
            {{ formatMoney(totalProfitAmount) }}원 · {{ totalProfitRate.toFixed(2) }}%
          </strong>
        </article>
      </section>

      <section class="profile-card wide-card">
        <h2>보유 종목 관리</h2>
        <form class="profile-form holding-form" @submit.prevent="saveHolding">
          <label>
            <span>종목 검색</span>
            <div class="holding-search-row">
              <input v-model="holdingSearchKeyword" type="search" placeholder="삼성전자 또는 005930" />
              <button class="primary-action" type="button" @click="searchHoldingStock">검색</button>
            </div>
          </label>

          <div v-if="holdingSearchResults.length" class="search-panel holding-search-panel">
            <button
              v-for="stock in holdingSearchResults"
              :key="stockCode(stock)"
              type="button"
              class="search-result"
              @click="selectHoldingStock(stock)"
            >
              <strong>{{ stockLabel(stock) }}</strong>
              <span>{{ stockCode(stock) }}</span>
            </button>
          </div>

          <div class="holding-form-grid">
            <label>
              <span>선택 종목</span>
              <input :value="holdingForm.stockName ? `${holdingForm.stockName} (${holdingForm.stockCode})` : ''" type="text" readonly />
            </label>
            <label>
              <span>수량</span>
              <input v-model.number="holdingForm.quantity" type="number" min="1" step="1" />
            </label>
            <label>
              <span>매입가</span>
              <input v-model.number="holdingForm.purchasePrice" type="number" min="1" step="1" />
            </label>
            <label>
              <span>매입일</span>
              <input v-model="holdingForm.purchaseDate" type="date" />
            </label>
          </div>

          <div class="holding-actions">
            <button class="primary-action" type="submit" :disabled="holdingSaving">
              {{ holdingEditingId ? '보유 종목 수정' : '보유 종목 추가' }}
            </button>
            <button class="secondary-action" type="button" @click="resetHoldingForm">취소</button>
            <button class="secondary-action" type="button" @click="showExtract = !showExtract">📷 사진으로 추가</button>
          </div>
          <p v-if="holdingMessage" class="panel-message">{{ holdingMessage }}</p>
        </form>

        <div v-if="holdingsLoading" class="panel-message">보유 종목 불러오는 중…</div>

        <div v-else-if="holdings.length" class="holding-table">
          <div class="holding-row holding-head">
            <span>종목</span>
            <span>수량</span>
            <span>매입가</span>
            <span>현재가</span>
            <span>평가금액</span>
            <span>손익률</span>
            <span>관리</span>
          </div>
          <div v-for="item in holdings" :key="item.id || stockCode(item)" class="holding-row">
            <strong>{{ stockLabel(item) }} <small>{{ stockCode(item) }}</small></strong>
            <span>{{ item.quantity }}주</span>
            <span>{{ formatMoney(item.purchasePrice) }}원</span>
            <span>{{ item.currentPrice == null ? '조회 실패' : `${formatMoney(item.currentPrice)}원` }}</span>
            <span>{{ formatMoney(item.evaluationAmount ?? item.purchaseAmount) }}원</span>
            <span :class="{ positive: holdingProfitRate(item) >= 0, negative: holdingProfitRate(item) < 0 }">
              {{ formatMoney(holdingProfitAmount(item)) }}원 · {{ holdingProfitRate(item).toFixed(2) }}%
            </span>
            <span class="row-actions">
              <button type="button" @click="editHolding(item)">수정</button>
              <button type="button" @click="removeHolding(item)">삭제</button>
            </span>
          </div>
        </div>
        <p v-else class="panel-message">등록된 보유 종목이 없습니다. 종목을 검색해서 매입가와 수량을 추가해 주세요.</p>

      </section>

      <section class="profile-card analysis-card">
        <h2>AI 투자 진단</h2>

        <div v-if="diagnosisLoading" class="panel-message">AI 진단 생성 중입니다… 잠시만 기다려 주세요.</div>

        <template v-else-if="diagnosis?.summary || diagnosis?.sections?.length">
          <p v-if="diagnosis?.summary">{{ diagnosis.summary }}</p>
          <div v-if="diagnosis?.sections?.length" class="diagnosis-grid">
            <article v-for="section in diagnosis.sections" :key="section.title" class="diagnosis-card">
              <h3>{{ section.title }}</h3>
              <p>{{ section.content }}</p>
              <ul v-if="section.guideItems?.length" class="diagnosis-guide">
                <li v-for="item in section.guideItems" :key="item">{{ item }}</li>
              </ul>
            </article>
          </div>
        </template>

        <p v-else class="panel-message">보유 종목을 등록하면 전체 매입금액, 평가금액, 수익률을 요약합니다.</p>
      </section>
    </main>

    <!-- 사진으로 추가 모달 -->
    <Teleport to="body">
      <div v-if="showExtract" class="extract-modal-backdrop" @click.self="showExtract = false">
        <div class="extract-modal">
          <div class="extract-modal-head">
            <strong>📷 사진으로 보유종목 추가</strong>
            <button type="button" class="extract-modal-close" @click="showExtract = false">✕</button>
          </div>
          <p class="extract-desc muted">보유 종목 화면 캡처 이미지를 업로드하면 AI가 종목·수량·매입가를 자동으로 읽어냅니다.</p>
          <div class="extract-upload-row">
            <input ref="extractFileInput" type="file" accept="image/*" class="extract-file-input" @change="onExtractFileChange" />
            <button type="button" class="primary-action" :disabled="extracting" @click="runExtract">
              {{ extracting ? '분석 중…' : '분석하기' }}
            </button>
          </div>
          <p v-if="extractError" class="panel-message">{{ extractError }}</p>

          <div v-if="extractedItems.length" class="extract-results">
            <p class="extract-results-title">추출 결과 — 확인 후 추가하세요</p>
            <div v-for="(item, idx) in extractedItems" :key="idx" class="extract-item">
              <div class="extract-item-info">
                <strong>{{ item.name }}</strong>
                <span>{{ Number(item.quantity ?? 0).toLocaleString() }}주</span>
                <span>{{ Number(item.avgPrice ?? 0).toLocaleString() }}원</span>
              </div>
              <div class="extract-item-code">
                <template v-if="item.stockCode">
                  <span class="code-tag">{{ item.stockCode }}</span>
                </template>
                <template v-else-if="item.candidates?.length">
                  <select v-model="item.selectedCode" class="extract-candidate-select">
                    <option v-for="c in item.candidates" :key="c.code" :value="c.code">
                      {{ c.name }} ({{ c.code }})
                    </option>
                  </select>
                </template>
                <span v-else class="muted">매칭 실패</span>
              </div>
              <button
                type="button"
                class="primary-action extract-add-btn"
                :disabled="!item.selectedCode || holdingSaving"
                @click="addExtractedItem(item)"
              >추가</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <AppFooter />
  </div>
</template>
