<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import BaseButton from '../../components/common/BaseButton.vue'
import { saveInvestorProfile } from '../../api/userApi'
import { useAuthStore } from '../../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const serverError = ref('')
const form = reactive({
  investmentExperience: 'BEGINNER',
  riskTolerance: 'LOW',
  investmentGoal: 'STABLE_GROWTH',
  investableAmount: 1000000,
  preferredSectors: ['반도체'],
})

const experienceOptions = [
  { value: 'NONE', label: '투자 경험 없음' },
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
  { value: 'STABLE_GROWTH', label: '안정적 성장' },
  { value: 'HIGH_RETURN', label: '고수익 추구' },
  { value: 'DIVIDEND', label: '배당 중심' },
  { value: 'LONG_TERM', label: '장기 투자' },
]

const sectorOptions = ['반도체', '금융', '자동차', 'IT', '바이오', '2차전지', '플랫폼']
const canSubmit = computed(() => form.preferredSectors.length > 0 && Number(form.investableAmount) >= 0)

function toggleSector(sector) {
  if (form.preferredSectors.includes(sector)) {
    form.preferredSectors = form.preferredSectors.filter((item) => item !== sector)
    return
  }

  form.preferredSectors.push(sector)
}

async function submit() {
  if (!canSubmit.value) {
    serverError.value = '투자 가능 금액과 관심 분야를 확인해주세요.'
    return
  }

  loading.value = true
  serverError.value = ''

  try {
    await saveInvestorProfile({
      // JWT 인증이 없는 임시 테스트 환경에서는 userId를 요청 바디로 넘길 수 있게 유지합니다.
      // 최종 구조에서는 백엔드가 accessToken에서 로그인 사용자 ID를 꺼내 저장합니다.
      userId: authStore.user?.id,
      investmentExperience: form.investmentExperience,
      riskTolerance: form.riskTolerance,
      investmentGoal: form.investmentGoal,
      investableAmount: Number(form.investableAmount),
      // 현재 백엔드는 배열을 받아 "반도체,금융,IT" 같은 콤마 문자열로 저장합니다.
      preferredSectors: form.preferredSectors,
    })
    router.push('/companies')
  } catch (error) {
    serverError.value = error.response?.data?.error?.message || '투자성향 저장에 실패했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="profile-page">
    <section class="profile-header">
      <p class="eyebrow">Investor Profile</p>
      <h1>나에게 맞는 기업 추천을 위한 투자성향 입력</h1>
      <p>입력한 투자 성향과 투자 가능 금액은 개인화 기업 추천에 활용됩니다.</p>
    </section>

    <form class="profile-card" @submit.prevent="submit">
      <fieldset>
        <legend>투자 경험</legend>
        <p class="muted">기업 공시와 리포트를 어느 정도 읽어봤는지 알려주세요.</p>
        <div class="option-grid">
          <label v-for="option in experienceOptions" :key="option.value" class="choice">
            <input v-model="form.investmentExperience" type="radio" name="experience" :value="option.value" />
            <span>{{ option.label }}</span>
          </label>
        </div>
      </fieldset>

      <fieldset>
        <legend>위험 감수 성향</legend>
        <p class="muted">변동성이 큰 기업을 추천해도 괜찮은지 판단하는 기준입니다.</p>
        <div class="option-grid">
          <label v-for="option in riskOptions" :key="option.value" class="choice">
            <input v-model="form.riskTolerance" type="radio" name="risk" :value="option.value" />
            <span>{{ option.label }}</span>
          </label>
        </div>
      </fieldset>

      <fieldset>
        <legend>투자 목표</legend>
        <p class="muted">추천 기업을 고를 때 우선순위를 정하는 데 사용합니다.</p>
        <div class="option-grid">
          <label v-for="option in goalOptions" :key="option.value" class="choice">
            <input v-model="form.investmentGoal" type="radio" name="goal" :value="option.value" />
            <span>{{ option.label }}</span>
          </label>
        </div>
      </fieldset>

      <fieldset>
        <legend>투자 가능 총 금액</legend>
        <p class="muted">추천 기업의 가격대와 포트폴리오 구성을 맞추는 데 사용합니다.</p>
        <label class="amount-field">
          <span>금액</span>
          <input v-model.number="form.investableAmount" type="number" min="0" step="10000" />
          <small>원 단위로 입력해주세요.</small>
        </label>
      </fieldset>

      <fieldset>
        <legend>관심 분야</legend>
        <p class="muted">복수 선택이 가능하며, 선택한 산업 중심으로 기업을 탐색합니다.</p>
        <div class="sector-grid">
          <button
            v-for="sector in sectorOptions"
            :key="sector"
            type="button"
            class="sector-chip"
            :class="{ selected: form.preferredSectors.includes(sector) }"
            @click="toggleSector(sector)"
          >
            {{ sector }}
          </button>
        </div>
      </fieldset>

      <p v-if="serverError" class="form-error">{{ serverError }}</p>

      <BaseButton type="submit" :disabled="loading">
        {{ loading ? '저장 중...' : '투자성향 저장' }}
      </BaseButton>
    </form>
  </main>
</template>
