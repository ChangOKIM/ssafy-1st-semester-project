import api from './api'

export function signup(payload) {
  return api.post('/api/v1/auth/signup', payload)
}

export function login(payload) {
  return api.post('/api/v1/auth/login', payload)
}
