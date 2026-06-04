import api from './api'

export function getRecommendations(sector) {
  return api.get('/recommendations', { params: sector ? { sector } : {} })
}
