import api from './api'

export function getHoldings() {
  return api.get('/holdings')
}

export function createHolding(payload) {
  return api.post('/holdings', payload)
}

export function updateHolding(id, payload) {
  return api.put(`/holdings/${id}`, payload)
}

export function deleteHolding(id) {
  return api.delete(`/holdings/${id}`)
}

export function getHoldingDiagnosis() {
  return api.get('/holdings/diagnosis')
}
