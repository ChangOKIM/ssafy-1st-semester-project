<script setup>
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import BaseButton from '../../components/common/BaseButton.vue'
import BaseInput from '../../components/common/BaseInput.vue'
import { useAuthStore } from '../../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const submitted = ref(false)
const serverError = ref('')
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
    next.email = '올바른 이메일 형식으로 입력해주세요.'
  }

  if (form.password.length < 8) {
    next.password = '비밀번호는 8자 이상이어야 합니다.'
  }

  if (form.password !== form.passwordConfirm) {
    next.passwordConfirm = '비밀번호가 일치하지 않습니다.'
  }

  if (!form.name.trim()) {
    next.name = '이름을 입력해주세요.'
  }

  return next
})

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

    // 추후 회원가입 직후 투자성향 입력으로 연결 예정.
    router.push('/investor-profile')
  } catch (error) {
    serverError.value = error.response?.data?.error?.message || '회원가입에 실패했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-copy">
      <p class="eyebrow">DART Service</p>
      <h1>초보 투자자를 위한 기업 정보 추천을 시작하세요.</h1>
      <p>
        DART 기업 정보를 초보자 눈높이에 맞게 쉽게 해석해드립니다.
        투자 성향을 입력하면 나에게 맞는 기업 추천을 받을 수 있습니다.
      </p>
    </section>

    <form class="auth-card" @submit.prevent="submit">
      <div>
        <h2>회원가입</h2>
        <p class="muted">기본 정보를 입력한 뒤 투자성향 설정으로 이어집니다.</p>
      </div>

      <BaseInput id="email" v-model="form.email" label="이메일" type="email" autocomplete="email" :error="submitted ? errors.email : ''" />
      <BaseInput id="password" v-model="form.password" label="비밀번호" type="password" autocomplete="new-password" :error="submitted ? errors.password : ''" />
      <BaseInput id="passwordConfirm" v-model="form.passwordConfirm" label="비밀번호 확인" type="password" autocomplete="new-password" :error="submitted ? errors.passwordConfirm : ''" />
      <BaseInput id="name" v-model="form.name" label="이름" autocomplete="name" :error="submitted ? errors.name : ''" />

      <p v-if="serverError" class="form-error">{{ serverError }}</p>

      <BaseButton type="submit" :disabled="loading">
        {{ loading ? '가입 중...' : '회원가입 후 투자성향 입력' }}
      </BaseButton>

      <p class="auth-link">
        이미 계정이 있나요?
        <RouterLink to="/login">로그인</RouterLink>
      </p>
    </form>
  </main>
</template>
