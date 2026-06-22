import api from './api'

export function getMyInfo() {
  return api.get('/users/me')
}

export function updateMyInfo(payload) {
  return api.put('/users/me', payload)
}

export function getInvestorProfile() {
  return api.get('/users/me/user-profile')
}

export function saveInvestorProfile(payload) {
  return api.put('/users/me/user-profile', payload)
}
