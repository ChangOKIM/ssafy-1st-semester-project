<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppFooter from '../components/layout/AppFooter.vue'
import AppHeader from '../components/layout/AppHeader.vue'
import { getHoldingDiagnosis, getHoldings } from '../api/holdingApi'
import { getRecommendations } from '../api/recommendationApi'
import { getInvestorProfile, getMyInfo, saveInvestorProfile } from '../api/userApi'
import { useAuthStore } from '../stores/authStore'

const authStore = useAuthStore()
const loading = ref(true)
const saving = ref(false)
const message = ref('')
const me = ref(null)
const profileLoaded = ref(false)
const holdings = ref([])
const diagnosis = ref(null)
const recommendations = ref([])

const profileForm = reactive({
  investmentExperience: 'BEGINNER',
  riskTolerance: 'MEDIUM',
  investmentGoal: 'STABLE_GROWTH',
  investableAmount: 1000000,
  preferredSectors: ['IT'],
})

const experienceOptions = [
  { value: 'NONE', label: '경험 없음' },
  { value: 'BEGINNER', label: '초보' },
  { value: 'INTERMEDIATE', label: '중급' },
  { value: 'ADVANCED', label: '고급' },
]

const riskOptions = [
  { value: 'LOW', label: '안정형' },
  { value: 'MEDIUM', label: '중립형' },
  { value: 'HIGH', label: '공격형' },
]

const goalOptions = [
  { value: 'STABLE_GROWTH', label: '안정 성장' },
  { value: 'HIGH_RETURN', label: '고수익 추구' },
  { value: 'DIVIDEND', label: '배당 중심' },
  { value: 'LONG_TERM', label: '장기 투자' },
]

const sectorOptions = ['IT', '반도체', '금융', '자동차', '바이오', '2차전지', '플랫폼']

const totalEvaluation = computed(() => {
  return holdings.value.reduce((sum, item) => {
    const quantity = Number(item.quantity ?? 0)
    const currentPrice = Number(item.currentPrice ?? item.price ?? item.purchasePrice ?? 0)
    return sum + quantity * currentPrice
  }, 0)
})

const totalPurchase = computed(() => {
  return holdings.value.reduce((sum, item) => {
    const quantity = Number(item.quantity ?? 0)
    const purchasePrice = Number(item.purchasePrice ?? item.purchase_price ?? 0)
    return sum + quantity * purchasePrice
  }, 0)
})

const totalProfitRate = computed(() => {
  if (!totalPurchase.value) {
    return 0
  }

  return ((totalEvaluation.value - totalPurchase.value) / totalPurchase.value) * 100
})

function unwrap(response) {
  return response?.data?.data ?? response?.data ?? []
}

function formatMoney(value) {
  return Number(value || 0).toLocaleString('ko-KR')
}

function displayUserName() {
  return me.value?.name || authStore.user?.name || '사용자'
}

function displayEmail() {
  return me.value?.email || authStore.user?.email || '-'
}

function applyProfile(profile) {
  if (!profile) {
    return
  }

  profileForm.investmentExperience = profile.investmentExperience || profileForm.investmentExperience
  profileForm.riskTolerance = profile.riskTolerance || profileForm.riskTolerance
  profileForm.investmentGoal = profile.investmentGoal || profileForm.investmentGoal
  profileForm.investableAmount = Number(profile.investableAmount ?? profileForm.investableAmount)
  profileForm.preferredSectors = Array.isArray(profile.preferredSectors)
    ? profile.preferredSectors
    : String(profile.preferredSectors || 'IT').split(',').map((item) => item.trim()).filter(Boolean)
}

function toggleSector(sector) {
  if (profileForm.preferredSectors.includes(sector)) {
    profileForm.preferredSectors = profileForm.preferredSectors.filter((item) => item !== sector)
    return
  }

  profileForm.preferredSectors.push(sector)
}

async function saveProfile() {
  saving.value = true
  message.value = ''

  try {
    const response = await saveInvestorProfile({
      investmentExperience: profileForm.investmentExperience,
      riskTolerance: profileForm.riskTolerance,
      investmentGoal: profileForm.investmentGoal,
      investableAmount: Number(profileForm.investableAmount),
      preferredSectors: profileForm.preferredSectors,
    })
    applyProfile(unwrap(response))
    profileLoaded.value = true
    message.value = '투자 성향이 저장되었습니다.'
  } catch (error) {
    message.value = error.response?.data?.error?.message || '투자 성향 저장에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

function stockLabel(item) {
  return item.stockName || item.stock_name || item.stockCode || item.stock_code || '보유 종목'
}

function stockCode(item) {
  return item.stockCode || item.stock_code || '-'
}

function recommendationLabel(item) {
  return item.stockName || item.stockCode || item.stock_code || '추천 종목'
}

async function loadMyPage() {
  loading.value = true
  message.value = ''

  const [meResult, profileResult, holdingsResult, diagnosisResult, recommendationsResult] = await Promise.allSettled([
    getMyInfo(),
    getInvestorProfile(),
    getHoldings(),
    getHoldingDiagnosis(),
    getRecommendations(),
  ])

  if (meResult.status === 'fulfilled') {
    me.value = unwrap(meResult.value)
    authStore.user = me.value
  }

  if (profileResult.status === 'fulfilled') {
    applyProfile(unwrap(profileResult.value))
    profileLoaded.value = true
  }

  if (holdingsResult.status === 'fulfilled') {
    holdings.value = unwrap(holdingsResult.value)
  }

  if (diagnosisResult.status === 'fulfilled') {
    diagnosis.value = unwrap(diagnosisResult.value)
  }

  if (recommendationsResult.status === 'fulfilled') {
    recommendations.value = unwrap(recommendationsResult.value).slice(0, 5)
  }

  loading.value = false
}

onMounted(loadMyPage)
</script>

<template>
  <div class="page-shell">
    <AppHeader />

    <main class="mypage">
      <section class="mypage-header">
        <p class="eyebrow">My page</p>
        <h1>{{ displayUserName() }}님의 투자 대시보드</h1>
        <p>기본 정보, 투자 성향, 보유 종목과 추천 이유를 명세 API 기준으로 확인합니다.</p>
      </section>

      <section class="mypage-grid">
        <article class="profile-card compact-card">
          <h2>기본정보</h2>
          <dl>
            <div>
              <dt>이메일</dt>
              <dd>{{ displayEmail() }}</dd>
            </div>
            <div>
              <dt>닉네임</dt>
              <dd>{{ displayUserName() }}</dd>
            </div>
            <div>
              <dt>권한</dt>
              <dd>{{ me?.role || authStore.user?.role || 'USER' }}</dd>
            </div>
          </dl>
        </article>

        <article class="profile-card compact-card">
          <h2>투자성향</h2>
          <p v-if="!profileLoaded" class="panel-message">등록된 투자 성향이 없으면 아래 값으로 저장할 수 있습니다.</p>

          <form class="profile-form" @submit.prevent="saveProfile">
            <label>
              <span>투자경험</span>
              <select v-model="profileForm.investmentExperience">
                <option v-for="option in experienceOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
            </label>

            <label>
              <span>위험감수 성향</span>
              <select v-model="profileForm.riskTolerance">
                <option v-for="option in riskOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
            </label>

            <label>
              <span>투자목표</span>
              <select v-model="profileForm.investmentGoal">
                <option v-for="option in goalOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
            </label>

            <label>
              <span>투자가능 금액</span>
              <input v-model.number="profileForm.investableAmount" type="number" min="0" step="10000" />
            </label>

            <div class="sector-picker">
              <span>관심분야</span>
              <div>
                <button
                  v-for="sector in sectorOptions"
                  :key="sector"
                  type="button"
                  :class="{ selected: profileForm.preferredSectors.includes(sector) }"
                  @click="toggleSector(sector)"
                >
                  {{ sector }}
                </button>
              </div>
            </div>

            <p v-if="message" class="panel-message">{{ message }}</p>
            <button class="primary-action" type="submit" :disabled="saving">{{ saving ? '저장 중' : '투자성향 저장' }}</button>
          </form>
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="metric-card">
          <span>투자가능 금액</span>
          <strong>{{ formatMoney(profileForm.investableAmount) }}원</strong>
          <p>DB `users_profile.investable_amount` 기준</p>
        </article>

        <article class="metric-card">
          <span>보유 평가금액</span>
          <strong>{{ formatMoney(totalEvaluation) }}원</strong>
          <p>DB `holdings` 수량과 가격 기준</p>
        </article>

        <article class="metric-card">
          <span>전체 수익률</span>
          <strong :class="{ positive: totalProfitRate >= 0, negative: totalProfitRate < 0 }">
            {{ totalProfitRate.toFixed(2) }}%
          </strong>
          <p>보유 종목 API 응답 기준</p>
        </article>
      </section>

      <section class="mypage-grid">
        <article class="profile-card wide-card">
          <h2>추천 이유</h2>
          <div v-if="recommendations.length" class="recommendation-list">
            <div v-for="item in recommendations" :key="recommendationLabel(item)">
              <strong>{{ recommendationLabel(item) }}</strong>
              <span>{{ item.reason || '투자 성향과 관심 분야를 기준으로 추천되었습니다.' }}</span>
            </div>
          </div>
          <p v-else class="panel-message">추천 데이터가 아직 없습니다. `/recommendations` 구현 후 표시됩니다.</p>
        </article>

        <article class="profile-card wide-card">
          <h2>보유 종목</h2>
          <div v-if="holdings.length" class="holding-list">
            <div v-for="item in holdings" :key="item.id || stockCode(item)">
              <strong>{{ stockLabel(item) }}</strong>
              <span>{{ stockCode(item) }}</span>
              <span>{{ item.quantity }}주</span>
              <span>{{ formatMoney(item.purchasePrice || item.purchase_price) }}원</span>
            </div>
          </div>
          <p v-else class="panel-message">보유 종목이 없습니다. `/holdings` 데이터가 생기면 종목별 수익률을 표시합니다.</p>
        </article>
      </section>

      <section class="profile-card analysis-card">
        <h2>AI 투자 진단</h2>
        <p v-if="diagnosis?.summary">{{ diagnosis.summary }}</p>
        <p v-else class="panel-message">`/holdings/diagnosis` 응답이 준비되면 성향, 비중, 종목별 진단을 표시합니다.</p>
      </section>
    </main>

    <AppFooter />
  </div>
</template>
