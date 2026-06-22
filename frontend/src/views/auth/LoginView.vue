<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AppFooter from '../../components/layout/AppFooter.vue'
import AppHeader from '../../components/layout/AppHeader.vue'
import BaseButton from '../../components/common/BaseButton.vue'
import BaseInput from '../../components/common/BaseInput.vue'
import { useAuthStore } from '../../stores/authStore'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const serverError = ref('')
const isExpired = route.query.expired === 'true'
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
    router.push(route.query.redirect || '/')
  } catch (error) {
    serverError.value = error.response?.data?.error?.message || '로그인에 실패했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page-shell">
    <AppHeader />

    <main class="auth-page">
      <!-- <section class="auth-copy">
        <p class="eyebrow">Login</p>
        <h1>내 종목 리포트와 맞춤 종목을 확인하세요.</h1>
      </section> -->

      <form class="auth-card" @submit.prevent="submit">
        <div>
          <h2>로그인</h2>
          <p class="muted">이메일과 비밀번호를 입력하세요.</p>
        </div>

        <BaseInput id="email" v-model="form.email" label="이메일" type="email" autocomplete="email" />
        <BaseInput id="password" v-model="form.password" label="비밀번호" type="password" autocomplete="current-password" />

        <p v-if="isExpired" class="form-error">세션이 만료됐습니다. 다시 로그인해 주세요.</p>
        <p v-if="serverError" class="form-error">{{ serverError }}</p>

        <BaseButton type="submit" :disabled="loading">{{ loading ? '로그인 중' : '로그인' }}</BaseButton>

        <p class="auth-link">
          아직 계정이 없나요?
          <RouterLink to="/signup">회원가입</RouterLink>
        </p>
      </form>
    </main>

    <AppFooter />
  </div>
</template>
