<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppFooter from '../components/layout/AppFooter.vue'
import AppHeader from '../components/layout/AppHeader.vue'
import { getRecommendations } from '../api/recommendationApi'

const router = useRouter()

const overallList = ref([])
const sectorCards = ref([])
const loading = ref(false)
const overallError = ref('')

function itemName(item) {
  return item.stockName || item.name || item.stockCode || item.stock_code || '추천 종목'
}

async function loadRecommendations() {
  loading.value = true
  overallError.value = ''

  try {
    const res = await getRecommendations()
    const data = res?.data ?? {}

    overallList.value = (data.overall ?? []).slice(0, 10)

    sectorCards.value = (data.bySector ?? []).map((s) => ({
      key: s.sector,
      label: s.sector,
      items: (s.items ?? []).slice(0, 3),
      error: '',
    }))
  } catch {
    overallError.value = '추천 데이터를 불러올 수 없습니다.'
    sectorCards.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadRecommendations)
</script>

<template>
  <div class="page-shell">
    <AppHeader />

    <main class="recs-page">
      <div v-if="loading" class="recs-loading">
        <p>추천 데이터를 불러오는 중입니다…</p>
      </div>

      <template v-else>
        <!-- 1. 페이지 헤더 -->
        <section class="recs-header">
          <p class="eyebrow">AI 투자 추천</p>
          <h1>나에게 맞는 종목 추천</h1>
          <p>투자 성향과 관심 분야를 반영한 맞춤 추천 목록입니다.</p>
        </section>

        <!-- 2. 종합 추천 TOP 10 -->
        <section>
          <div class="recs-section-head">
            <h2>종합 추천 TOP 10</h2>
            <span class="recs-badge">투자성향 기반</span>
          </div>

          <p v-if="overallError" class="panel-message">{{ overallError }}</p>
          <p v-else-if="!overallList.length" class="panel-message">
            투자 성향을 먼저 등록해 주세요. 마이페이지에서 성향을 설정하면 맞춤 추천을 받을 수 있습니다.
          </p>

          <div v-else class="recs-overall-card">
            <ol class="recs-rank-list">
              <li
                v-for="(item, index) in overallList"
                :key="itemName(item) + index"
                class="recs-rank-item recs-clickable"
                @click="router.push('/report/' + item.stockCode)"
              >
                <em class="recs-rank-num">{{ index + 1 }}</em>
                <strong>{{ itemName(item) }}</strong>
                <b class="recs-score">{{ item.score != null ? Math.round(item.score) : '-' }}</b>
              </li>
            </ol>
          </div>
        </section>

        <!-- 3. 섹터별 TOP 3 -->
        <section>
          <div class="recs-section-head">
            <h2>섹터별 추천 TOP 3</h2>
            <span class="recs-badge">관심 분야 기반</span>
          </div>

          <div class="recs-sector-grid">
            <div
              v-for="card in sectorCards"
              :key="card.key"
              class="recs-sector-card"
            >
              <div class="recs-sector-head">
                <strong>{{ card.label }}</strong>
                <span>TOP 3</span>
              </div>

              <p v-if="card.error" class="panel-message">{{ card.error }}</p>
              <p v-else-if="!card.items.length" class="panel-message">데이터 없음</p>

              <ol v-else class="recs-sector-list">
                <li
                  v-for="(item, index) in card.items"
                  :key="itemName(item) + index"
                  class="recs-sector-item recs-clickable"
                  @click="router.push('/report/' + item.stockCode)"
                >
                  <em class="recs-rank-num recs-rank-num-sm">{{ index + 1 }}</em>
                  <span>{{ itemName(item) }}</span>
                  <b class="recs-score">{{ item.score != null ? Math.round(item.score) : '-' }}</b>
                </li>
              </ol>
            </div>
          </div>
        </section>

        <!-- 4. 고지 문구 -->
        <p class="recs-disclosure muted">
          본 화면에서 제공하는 추천 정보는 투자 참고용이며, 실제 투자 판단과 그에 따른 책임은 사용자에게 있습니다.
        </p>
      </template>
    </main>

    <AppFooter />
  </div>
</template>
