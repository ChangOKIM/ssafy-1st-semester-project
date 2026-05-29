import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/auth/LoginView.vue'
import SignupView from '../views/auth/SignupView.vue'
import InvestorProfileView from '../views/investor/InvestorProfileView.vue'
import CompaniesView from '../views/CompaniesView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/signup', name: 'signup', component: SignupView },
    { path: '/investor-profile', name: 'investor-profile', component: InvestorProfileView },
    { path: '/companies', name: 'companies', component: CompaniesView },
  ],
})

export default router
