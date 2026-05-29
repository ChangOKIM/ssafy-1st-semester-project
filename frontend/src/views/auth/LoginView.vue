<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import BaseButton from '../../components/common/BaseButton.vue'
import BaseInput from '../../components/common/BaseInput.vue'
import { useAuthStore } from '../../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const serverError = ref('')
const form = reactive({
  email: '',
  password: '',
})

async function submit() {
  loading.value = true
  serverError.value = ''

  try {
    await authStore.login({
      email: form.email,
      password: form.password,
    })
    router.push('/companies')
  } catch (error) {
    serverError.value = error.response?.data?.error?.message || '로그인에 실패했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-copy">
      <p class="eyebrow">DART Service</p>
      <h1>기업 정보를 쉽게 읽고, 나에게 맞는 회사를 찾아보세요.</h1>
      <p>
        금융 공시와 기업 리포트를 초보자 눈높이에 맞춰 정리하는 추천 서비스입니다.
      </p>
    </section>

    <form class="auth-card" @submit.prevent="submit">
      <div>
        <h2>로그인</h2>
        <p class="muted">가입한 이메일과 비밀번호로 계속 진행합니다.</p>
      </div>

      <BaseInput id="email" v-model="form.email" label="이메일" type="email" autocomplete="email" />
      <BaseInput id="password" v-model="form.password" label="비밀번호" type="password" autocomplete="current-password" />

      <p v-if="serverError" class="form-error">{{ serverError }}</p>

      <BaseButton type="submit" :disabled="loading">
        {{ loading ? '로그인 중...' : '로그인' }}
      </BaseButton>

      <p class="auth-link">
        아직 계정이 없나요?
        <RouterLink to="/signup">회원가입</RouterLink>
      </p>
    </form>
  </main>
</template>
