import api from './api'

export function signup(payload) {
  return api.post('/auth/signup', payload)
}

export function login(payload) {
  return api.post('/auth/login', payload)
}

export function checkEmail(email) {
  return api.get('/auth/check-email', { params: { email } })
}
