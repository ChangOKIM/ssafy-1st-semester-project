import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/auth/LoginView.vue'
import SignupView from '../views/auth/SignupView.vue'
import InvestorProfileView from '../views/investor/InvestorProfileView.vue'
import HoldingsView from '../views/HoldingsView.vue'
import MainView from '../views/MainView.vue'
import MyInfoView from '../views/MyInfoView.vue'
import MyPageView from '../views/MyPageView.vue'
import { useAuthStore } from '../stores/authStore'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: MainView },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/signup', name: 'signup', component: SignupView },
    { path: '/investor-profile', name: 'investor-profile', component: InvestorProfileView, meta: { requiresAuth: true } },
    { path: '/mypage', name: 'mypage', component: MyPageView, meta: { requiresAuth: true } },
    { path: '/myinfo', name: 'myinfo', component: MyInfoView, meta: { requiresAuth: true } },
    { path: '/recommendations', redirect: '/' },
    { path: '/holdings', name: 'holdings', component: HoldingsView, meta: { requiresAuth: true } },
  ],
})

router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})

export default router
