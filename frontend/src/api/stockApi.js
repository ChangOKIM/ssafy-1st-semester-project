import api from './api'

export function searchStocks(keyword) {
  return api.get('/reports/search', { params: { keyword } })
}

export function getStockPrice(code) {
  return api.get(`/stocks/${code}/price`)
}

export function getStockChart(code, period = 'daily') {
  return api.get(`/stocks/${code}/chart`, { params: { period } })
}

export function getStockFinancial(code) {
  return api.get(`/stocks/${code}/financial`)
}

export function getStockAnalysis(code) {
  return api.get(`/stocks/${code}/analysis`)
}
