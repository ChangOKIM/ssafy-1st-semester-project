<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppFooter from '../../components/layout/AppFooter.vue'
import AppHeader from '../../components/layout/AppHeader.vue'
import BaseButton from '../../components/common/BaseButton.vue'
import { saveInvestorProfile } from '../../api/userApi'

const router = useRouter()
const loading = ref(false)
const serverError = ref('')
const form = reactive({
  investmentExperience: 'BEGINNER',
  riskTolerance: 'MEDIUM',
  investmentGoal: 'STABLE_GROWTH',
  preferredSectors: ['IT·소프트웨어'],
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

const sectorOptions = [
  'IT·소프트웨어',
  '반도체',
  '금융',
  '자동차·모빌리티',
  '바이오·헬스케어',
  '2차전지·에너지',
  '플랫폼·인터넷',
  '소비재',
  '엔터테인먼트',
  '산업재·조선·방산',
  '소재·화학·철강',
  '유틸리티',
]
const canSubmit = computed(() => form.preferredSectors.length > 0)

function toggleSector(sector) {
  if (form.preferredSectors.includes(sector)) {
    form.preferredSectors = form.preferredSectors.filter((item) => item !== sector)
    return
  }

  form.preferredSectors.push(sector)
}

async function submit() {
  if (!canSubmit.value) {
    serverError.value = '관심분야를 하나 이상 선택하세요.'
    return
  }

  loading.value = true
  serverError.value = ''

  try {
    await saveInvestorProfile({
      investmentExperience: form.investmentExperience,
      riskTolerance: form.riskTolerance,
      investmentGoal: form.investmentGoal,
      preferredSectors: form.preferredSectors,
    })
    router.push('/mypage')
  } catch (error) {
    serverError.value = error.response?.data?.error?.message || '투자성향 저장에 실패했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page-shell">
    <AppHeader />

    <main class="profile-page">
      <section class="profile-header">
        <p class="eyebrow">Investor profile</p>
        <h1>맞춤 추천을 위한 투자 성향을 입력하세요.</h1>
        <p>입력값은 DB `users_profile`에 저장되고 마이페이지와 추천 API의 기준 데이터로 사용됩니다.</p>
      </section>

      <form class="auth-card" @submit.prevent="submit">
        <label class="field">
          <span>투자경험</span>
          <select v-model="form.investmentExperience">
            <option v-for="option in experienceOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>

        <label class="field">
          <span>위험감수 성향</span>
          <select v-model="form.riskTolerance">
            <option v-for="option in riskOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>

        <label class="field">
          <span>투자목표</span>
          <select v-model="form.investmentGoal">
            <option v-for="option in goalOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>

        <div class="sector-picker">
          <span>관심분야</span>
          <div>
            <button
              v-for="sector in sectorOptions"
              :key="sector"
              type="button"
              :class="{ selected: form.preferredSectors.includes(sector) }"
              @click="toggleSector(sector)"
            >
              {{ sector }}
            </button>
          </div>
        </div>

        <p v-if="serverError" class="form-error">{{ serverError }}</p>

        <BaseButton type="submit" :disabled="loading">{{ loading ? '저장 중' : '투자성향 저장' }}</BaseButton>
      </form>
    </main>

    <AppFooter />
  </div>
</template>
