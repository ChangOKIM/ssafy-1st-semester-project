<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { searchStocks } from '../api/stockApi'

const router = useRouter()
const keyword = ref('')
const searchResults = ref([])
const searchLoading = ref(false)
const searchMessage = ref('')

function stockName(stock) {
  return stock.stockName || stock.name || stock.stock_name || '종목명 없음'
}

function stockCode(stock) {
  return stock.stockCode || stock.code || stock.stock_code || '-'
}

function unwrap(response) {
  return response?.data?.data ?? response?.data ?? []
}

async function submitSearch() {
  const trimmed = keyword.value.trim()
  searchMessage.value = ''

  if (trimmed.length < 2) {
    searchResults.value = []
    searchMessage.value = '종목명 또는 코드를 2글자 이상 입력하세요.'
    return
  }

  searchLoading.value = true
  try {
    searchResults.value = unwrap(await searchStocks(trimmed)).slice(0, 8)
    if (searchResults.value.length === 0) {
      searchMessage.value = '검색 결과가 없습니다.'
    }
  } catch {
    searchResults.value = []
    searchMessage.value = '종목 검색 API 응답을 확인할 수 없습니다.'
  } finally {
    searchLoading.value = false
  }
}
</script>

<template>
  <div class="stock-search-bar">
    <form class="stock-search" @submit.prevent="submitSearch">
      <input v-model="keyword" type="search" placeholder="예: 삼성전자, 005930" />
      <button type="submit">{{ searchLoading ? '검색 중' : '검색' }}</button>
    </form>

    <div v-if="searchResults.length || searchMessage" class="search-panel">
      <p v-if="searchMessage" class="panel-message">{{ searchMessage }}</p>
      <button
        v-for="stock in searchResults"
        :key="stockCode(stock)"
        type="button"
        class="search-result"
        @click="router.push('/report/' + stockCode(stock))"
      >
        <strong>{{ stockName(stock) }}</strong>
        <span>{{ stockCode(stock) }}</span>
      </button>
    </div>
  </div>
</template>
