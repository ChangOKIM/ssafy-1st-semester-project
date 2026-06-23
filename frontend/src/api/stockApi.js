import api from './api'

export function searchStocks(keyword) {
  return api.get('/reports/search', { params: { keyword } })
}

export function getSectors() {
  return api.get('/stocks/sectors')
}

export function listAllStocks(keyword = '', sector = '', page = 0, size = 20) {
  return api.get('/stocks', { params: { keyword, sector, page, size } })
}

export function getStockInfo(code) {
  return api.get(`/reports/${code}/info`)
}

export function initMarketCap() {
  return api.post('/stocks/init-marketcap')
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
  return api.get(`/reports/${code}/report`)
}
