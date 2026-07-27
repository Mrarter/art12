/**
 * 转售市场 API
 * 接口: 用户端转售/流通/价格
 */
import request from './request'

/**
 * 发布转售
 * POST /user/resale/publish
 * @param {Object} data - { artworkId, resalePrice }
 */
export const publishResale = (data) => {
  return request.post('/user/resale/publish', data)
}

/**
 * 转售市场列表
 * GET /user/resale/list
 * @param {Object} params - { page, pageSize, artworkId }
 */
export const getResaleList = (params) => {
  return request.get('/user/resale/list', params)
}

/**
 * 我的转售列表
 * GET /user/resale/my
 * @param {Object} params - { page, pageSize, status }
 */
export const getMyResales = (params) => {
  return request.get('/user/resale/my', params)
}

/**
 * 转售详情
 * GET /user/resale/{id}
 */
export const getResaleDetail = (id) => {
  return request.get(`/user/resale/${id}`)
}

/**
 * 取消转售
 * POST /user/resale/{id}/cancel
 */
export const cancelResale = (id) => {
  return request.post(`/user/resale/${id}/cancel`)
}

/**
 * 调整转售价
 * POST /user/resale/{id}/price
 */
export const updateResalePrice = (id, data) => {
  return request.post(`/user/resale/${id}/price`, data)
}

/**
 * 切换平台评估与热度涨价机制
 * POST /user/resale/{id}/platform-pricing
 */
export const updatePlatformPricing = (id, data) => {
  return request.post(`/user/resale/${id}/platform-pricing`, data)
}

/**
 * 作品交易链路
 * GET /user/resale/artwork/{artworkId}/trades
 */
export const getArtworkTrades = (artworkId) => {
  return request.get(`/user/resale/artwork/${artworkId}/trades`)
}

/**
 * 作品价格历史
 * GET /user/resale/artwork/{artworkId}/price-history
 */
export const getArtworkPriceHistory = (artworkId) => {
  return request.get(`/user/resale/artwork/${artworkId}/price-history`)
}

/**
 * 作品转售统计
 * GET /user/resale/artwork/{artworkId}/stats
 */
export const getArtworkResaleStats = (artworkId) => {
  return request.get(`/user/resale/artwork/${artworkId}/stats`)
}
