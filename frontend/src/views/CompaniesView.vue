<script setup>
import { ref } from 'vue'
import AppFooter from '../components/layout/AppFooter.vue'
import AppHeader from '../components/layout/AppHeader.vue'

const keyword = ref('')

const marketItems = [
  { label: '코스피', value: '2,732.18', change: '+0.84%' },
  { label: '코스닥', value: '846.42', change: '+1.12%' },
  { label: '관심 공시', value: '128건', change: '오늘' },
  { label: '추천 후보', value: '24개', change: '맞춤' },
]

const featureCards = [
  {
    title: 'DART 공시 요약',
    description: '복잡한 사업보고서와 공시를 초보자 눈높이로 정리합니다.',
  },
  {
    title: '기업 재무 체크',
    description: '매출, 영업이익, 부채 흐름을 핵심 지표 중심으로 비교합니다.',
  },
  {
    title: '투자성향 추천',
    description: '위험 성향과 투자 가능 금액에 맞는 기업 후보를 보여줍니다.',
  },
]

const sectors = [
  'IT·소프트웨어',
  '반도체',
  '금융',
  '자동차·모빌리티',
  '바이오·헬스케어',
  '2차전지·에너지',
  '플랫폼·인터넷',
  '소비재',
  '엔터테인먼트',
  '산업재·조선·방산',
  '소재·화학·철강',
  '유틸리티',
]

function searchCompany() {
  if (!keyword.value.trim()) {
    return
  }
}
</script>

<template>
  <main class="home-page">
    <AppHeader />

    <section class="main-hero">
      <div class="hero-copy">
        <p class="eyebrow">DART 기반 기업 분석</p>
        <h1>기업 공시를 쉽게 읽고, 나에게 맞는 회사를 찾아보세요</h1>
        <p>
          사업보고서, 재무 지표, 투자성향 정보를 한 번에 연결해서 초보 투자자도 이해하기 쉬운 기업 추천을 제공합니다.
        </p>

        <form class="company-search" @submit.prevent="searchCompany">
          <input v-model="keyword" type="search" placeholder="기업명 또는 종목코드를 입력하세요" />
          <button type="submit">검색</button>
        </form>
      </div>

      <div class="hero-visual" aria-label="시장 흐름 요약">
        <div class="chart-card">
          <div class="chart-top">
            <div>
              <strong>추천 관심 기업</strong>
              <span>투자성향 기반</span>
            </div>
            <b>+18.6%</b>
          </div>
          <div class="line-chart">
            <span style="height: 30%"></span>
            <span style="height: 42%"></span>
            <span style="height: 36%"></span>
            <span style="height: 58%"></span>
            <span style="height: 52%"></span>
            <span style="height: 76%"></span>
            <span style="height: 68%"></span>
            <span style="height: 88%"></span>
          </div>
          <div class="chart-note">
            <span>공시 업데이트</span>
            <strong>오늘 128건 분석</strong>
          </div>
        </div>
      </div>
    </section>

    <section class="market-strip">
      <article v-for="item in marketItems" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <em>{{ item.change }}</em>
      </article>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <p class="eyebrow">Analysis</p>
        <h2>기업 정보 분석에 필요한 핵심 기능</h2>
      </div>
      <div class="feature-grid">
        <article v-for="card in featureCards" :key="card.title" class="feature-card">
          <div class="feature-icon"></div>
          <h3>{{ card.title }}</h3>
          <p>{{ card.description }}</p>
        </article>
      </div>
    </section>

    <section class="section-block split-block">
      <div>
        <p class="eyebrow">Recommendation</p>
        <h2>관심 섹터 중심으로 추천을 시작합니다</h2>
        <p>
          회원가입 때 입력한 투자 경험, 위험 성향, 투자 가능 금액, 관심 섹터를 바탕으로 기업 후보를 좁혀갑니다.
        </p>
      </div>
      <div class="sector-panel">
        <button v-for="sector in sectors" :key="sector" type="button">{{ sector }}</button>
      </div>
    </section>

    <AppFooter />
  </main>
</template>
