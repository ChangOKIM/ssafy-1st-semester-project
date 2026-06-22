<script setup>
import { computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()
const isLoggedIn = computed(() => authStore.isLoggedIn)

function logout() {
  authStore.logout()
  router.push('/')
}
</script>

<template>
  <header class="app-header">
    <div class="app-header-inner">
      <RouterLink class="brand" to="/">
        <img src="/logo.svg" alt="DartPoint AI" class="brand-logo" />
        <span>주린이 안경</span>
      </RouterLink>

      <nav class="main-nav" aria-label="주요 메뉴">
        <RouterLink to="/">홈</RouterLink>
        <RouterLink v-if="isLoggedIn" to="/recommendations">AI 추천</RouterLink>
        <RouterLink v-if="isLoggedIn" to="/holdings">투자현황</RouterLink>
        <RouterLink v-if="isLoggedIn" to="/mypage">투자성향</RouterLink>
      </nav>

      <div class="header-actions">
        <template v-if="isLoggedIn">
          <RouterLink to="/myinfo">내정보</RouterLink>
          <button type="button" @click="logout">로그아웃</button>
        </template>
        <template v-else>
          <RouterLink to="/login">로그인</RouterLink>
          <RouterLink class="signup-link" to="/signup">회원가입</RouterLink>
        </template>
      </div>
    </div>
  </header>
</template>
