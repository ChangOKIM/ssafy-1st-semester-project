<script setup>
import { onMounted, reactive, ref } from 'vue'
import AppFooter from '../components/layout/AppFooter.vue'
import AppHeader from '../components/layout/AppHeader.vue'
import { getMyInfo, updateMyInfo } from '../api/userApi'
import { useAuthStore } from '../stores/authStore'

const authStore = useAuthStore()
const saving = ref(false)
const message = ref('')
const previewImage = ref(localStorage.getItem('profileImage') || '')

const form = reactive({
  email: '',
  name: '',
})

function unwrap(response) {
  return response?.data?.data ?? response?.data ?? null
}

function initials() {
  return (form.name || form.email || 'U').slice(0, 1).toUpperCase()
}

function applyUser(user) {
  if (!user) return
  form.email = user.email || ''
  form.name = user.name || ''
  authStore.user = user
}

async function loadMe() {
  try {
    applyUser(unwrap(await getMyInfo()))
  } catch (error) {
    message.value = '내 정보를 불러오지 못했습니다.'
  }
}

async function saveInfo() {
  saving.value = true
  message.value = ''

  try {
    const user = unwrap(await updateMyInfo({
      email: form.email,
      name: form.name,
    }))
    applyUser(user)
    message.value = '내 정보가 저장되었습니다. 이메일을 바꿨다면 다음 로그인부터 새 이메일을 사용해 주세요.'
  } catch (error) {
    message.value = error.response?.data?.error?.message || '내 정보 저장에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

function updateImage(event) {
  const file = event.target.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = () => {
    previewImage.value = String(reader.result)
    localStorage.setItem('profileImage', previewImage.value)
  }
  reader.readAsDataURL(file)
}

function removeImage() {
  previewImage.value = ''
  localStorage.removeItem('profileImage')
}

onMounted(loadMe)
</script>

<template>
  <div class="page-shell">
    <AppHeader />

    <main class="mypage">
      <section class="mypage-header">
        <p class="eyebrow">Account</p>
        <h1>회원 정보</h1>
      </section>

      <section class="profile-card wide-card account-card">
        <div class="profile-image-box">
          <img v-if="previewImage" :src="previewImage" alt="프로필 이미지" />
          <div v-else class="profile-initial">{{ initials() }}</div>
          <div class="holding-actions">
            <label class="secondary-action image-upload">
              이미지 선택
              <input type="file" accept="image/*" @change="updateImage" />
            </label>
            <button class="secondary-action" type="button" @click="removeImage">삭제</button>
          </div>
        </div>

        <form class="profile-form" @submit.prevent="saveInfo">
          <label>
            <span>이름</span>
            <input v-model="form.name" type="text" maxlength="50" />
          </label>

          <label>
            <span>이메일</span>
            <input v-model="form.email" type="email" />
          </label>

          <p v-if="message" class="panel-message">{{ message }}</p>
          <button class="primary-action" type="submit" :disabled="saving">
            {{ saving ? '저장 중' : '내정보 저장' }}
          </button>
        </form>
      </section>
    </main>

    <AppFooter />
  </div>
</template>
