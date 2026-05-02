import request from './request'

export function getArtworkPriceList(params) {
  return request({
    url: '/api/admin/artwork/price/list',
    method: 'get',
    params
  })
}

export function manualAdjustArtworkPrice(data) {
  return request({
    url: '/api/admin/artwork/price/manual-adjust',
    method: 'post',
    data
  })
}

export function getArtworkPriceLogs(params) {
  return request({
    url: '/api/admin/artwork/price/logs',
    method: 'get',
    params
  })
}
