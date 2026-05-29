import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi, signup as signupApi } from '../api/authApi'
import { getMyInfo } from '../api/userApi'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const user = ref(null)
  const isLoggedIn = computed(() => Boolean(accessToken.value))

  function setAuth(authData) {
    accessToken.value = authData?.accessToken || ''
    refreshToken.value = authData?.refreshToken || ''
    user.value = authData?.user || null

    if (accessToken.value) {
      localStorage.setItem('accessToken', accessToken.value)
    }

    if (refreshToken.value) {
      localStorage.setItem('refreshToken', refreshToken.value)
    }
  }

  async function signup(payload) {
    const response = await signupApi(payload)
    setAuth(response.data.data)
    return response.data.data
  }

  async function login(payload) {
    const response = await loginApi(payload)
    setAuth(response.data.data)
    return response.data.data
  }

  async function fetchMe() {
    const response = await getMyInfo()
    user.value = response.data.data
    return user.value
  }

  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  return {
    accessToken,
    refreshToken,
    user,
    isLoggedIn,
    signup,
    login,
    fetchMe,
    logout,
  }
})
