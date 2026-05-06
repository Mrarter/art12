import request from './request'

/**
 * 触发每日涨价
 * POST /api/artwork/price/daily/{artworkId}
 */
export function triggerDailyIncrease(artworkId) {
  return request.post(`/api/artwork/price/daily/${artworkId}`)
}

/**
 * 触发成交涨价
 * POST /api/artwork/price/sale/{artworkId}
 */
export function triggerSaleIncrease(artworkId) {
  return request.post(`/api/artwork/price/sale/${artworkId}`)
}

/**
 * 触发收藏涨价
 * POST /api/artwork/price/collect/{artworkId}
 */
export function triggerCollectIncrease(artworkId) {
  return request.post(`/api/artwork/price/collect/${artworkId}`)
}
