import api from './api'

export function saveInvestorProfile(payload) {
  return api.put('/api/v1/users/me/investor-profile', payload)
}

export function getMyInfo() {
  return api.get('/api/v1/users/me')
}
