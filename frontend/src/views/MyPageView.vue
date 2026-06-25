<script setup>
import { onMounted, reactive, ref } from 'vue'
import AppFooter from '../components/layout/AppFooter.vue'
import AppHeader from '../components/layout/AppHeader.vue'
import { getInvestorProfile, saveInvestorProfile } from '../api/userApi'

const saving = ref(false)
const message = ref('')
const profileLoaded = ref(false)

const profileForm = reactive({
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
  { value: 'CAPITAL_GAIN', label: '시세 차익' },
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

function unwrap(response) {
  return response?.data?.data ?? response?.data ?? null
}

function applyProfile(profile) {
  if (!profile) return
  profileForm.investmentExperience = profile.investmentExperience || profileForm.investmentExperience
  profileForm.riskTolerance = profile.riskTolerance || profileForm.riskTolerance
  profileForm.investmentGoal = profile.investmentGoal || profile.investmentGoals || profileForm.investmentGoal
  profileForm.preferredSectors = Array.isArray(profile.preferredSectors)
    ? profile.preferredSectors
    : String(profile.preferredSectors || 'IT·소프트웨어').split(',').map((item) => item.trim()).filter(Boolean)
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

async function loadProfile() {
  try {
    applyProfile(unwrap(await getInvestorProfile()))
    profileLoaded.value = true
  } catch (error) {
    profileLoaded.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="page-shell">
    <AppHeader />

    <main class="mypage">
      <section class="recs-header">
        <p class="eyebrow">Investment Profile</p>
        <h1>투자성향</h1>
      </section>

      <section class="profile-card wide-card">
        <h2>투자성향 {{ profileLoaded ? '수정' : '입력' }}</h2>
        <p v-if="!profileLoaded" class="panel-message">등록된 투자 성향이 없으면 아래 값으로 새로 저장합니다.</p>

        <form class="profile-form" @submit.prevent="saveProfile">
          <label>
            <span>투자 경험</span>
            <select v-model="profileForm.investmentExperience">
              <option v-for="option in experienceOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </label>

          <label>
            <span>위험 감수 성향</span>
            <select v-model="profileForm.riskTolerance">
              <option v-for="option in riskOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </label>

          <label>
            <span>투자 목표</span>
            <select v-model="profileForm.investmentGoal">
              <option v-for="option in goalOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </label>

          <div class="sector-picker">
            <span>관심 분야</span>
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
          <button class="primary-action" type="submit" :disabled="saving">
            {{ saving ? '저장 중' : '투자 성향 저장' }}
          </button>
        </form>
      </section>
    </main>

    <AppFooter />
  </div>
</template>
