<script setup>
import { computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()
const isLoggedIn = computed(() => authStore.isLoggedIn)

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <header class="app-header">
    <div class="app-header-inner">
      <RouterLink class="brand" to="/companies">
        <span class="brand-mark">D</span>
        <span>DartPoint AI</span>
      </RouterLink>

      <nav class="main-nav" aria-label="주요 메뉴">
        <RouterLink to="/companies">기업 검색</RouterLink>
        <RouterLink to="/companies">추천 기업</RouterLink>
        <RouterLink to="/companies">투자현황</RouterLink>
        <RouterLink to="/investor-profile">마이페이지</RouterLink>
      </nav>

      <div class="header-actions">
        <template v-if="isLoggedIn">
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
