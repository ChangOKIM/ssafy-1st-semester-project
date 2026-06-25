<script setup>
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import AppFooter from '../../components/layout/AppFooter.vue'
import AppHeader from '../../components/layout/AppHeader.vue'
import BaseButton from '../../components/common/BaseButton.vue'
import BaseInput from '../../components/common/BaseInput.vue'
import { checkEmail } from '../../api/authApi'
import { useAuthStore } from '../../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const submitted = ref(false)
const serverError = ref('')
const emailCheckMessage = ref('')
const emailAvailable = ref(null)
const form = reactive({
  email: '',
  password: '',
  passwordConfirm: '',
  name: '',
})

const errors = computed(() => {
  const next = {}
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  if (!emailPattern.test(form.email)) {
    next.email = '올바른 이메일 형식으로 입력하세요.'
  }

  if (form.password.length < 8) {
    next.password = '비밀번호는 8자 이상이어야 합니다.'
  }

  if (form.password !== form.passwordConfirm) {
    next.passwordConfirm = '비밀번호가 일치하지 않습니다.'
  }

  if (!form.name.trim()) {
    next.name = '닉네임을 입력하세요.'
  }

  if (emailAvailable.value === false) {
    next.email = '이미 사용 중인 이메일입니다.'
  }

  return next
})

async function checkEmailAvailability() {
  emailCheckMessage.value = ''
  emailAvailable.value = null

  if (!form.email) {
    emailCheckMessage.value = '이메일을 먼저 입력하세요.'
    return
  }

  try {
    const response = await checkEmail(form.email)
    const available = response.data?.data?.available

    if (typeof available !== 'boolean') {
      emailCheckMessage.value = '이메일 중복 확인 응답 형식이 올바르지 않습니다.'
      return
    }

    emailAvailable.value = available
    emailCheckMessage.value = emailAvailable.value ? '사용 가능한 이메일입니다.' : '이미 사용 중인 이메일입니다.'
  } catch (error) {
    emailCheckMessage.value = '이메일 중복 확인에 실패했습니다.'
  }
}

async function submit() {
  submitted.value = true
  serverError.value = ''

  if (Object.keys(errors.value).length > 0) {
    return
  }

  loading.value = true

  try {
    await authStore.signup({
      email: form.email,
      password: form.password,
      name: form.name,
    })
    router.push('/investor-profile')
  } catch (error) {
    serverError.value = error.response?.data?.error?.message || '회원가입에 실패했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page-shell">
    <AppHeader />

    <main class="auth-page">
      <section class="auth-copy">
        <p class="eyebrow">Signup</p>
        <h1>투자 성향 기반 추천을 시작하세요.</h1>
      </section>

      <form class="auth-card" @submit.prevent="submit">
        <div>
          <h2>회원가입</h2>
          <p class="muted">기본 정보를 입력한 뒤 투자 성향 등록으로 이동합니다.</p>
        </div>

        <BaseInput id="email" v-model="form.email" label="이메일" type="email" autocomplete="email" :error="submitted ? errors.email : ''" />
        <button class="primary-action" type="button" @click="checkEmailAvailability">이메일 중복 확인</button>
        <p v-if="emailCheckMessage" class="panel-message">{{ emailCheckMessage }}</p>

        <BaseInput id="password" v-model="form.password" label="비밀번호" type="password" autocomplete="new-password" :error="submitted ? errors.password : ''" />
        <BaseInput id="passwordConfirm" v-model="form.passwordConfirm" label="비밀번호 확인" type="password" autocomplete="new-password" :error="submitted ? errors.passwordConfirm : ''" />
        <BaseInput id="name" v-model="form.name" label="닉네임" autocomplete="name" :error="submitted ? errors.name : ''" />

        <p v-if="serverError" class="form-error">{{ serverError }}</p>

        <BaseButton type="submit" :disabled="loading">{{ loading ? '가입 중' : '회원가입 후 투자성향 입력' }}</BaseButton>

        <p class="auth-link">
          이미 계정이 있나요?
          <RouterLink to="/login">로그인</RouterLink>
        </p>
      </form>
    </main>

    <AppFooter />
  </div>
</template>
