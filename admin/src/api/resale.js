/**
 * 转售管理 API
 * 接口: 艺术品二级流通系统后台管理
 */
import request from './request'

/**
 * 转售记录列表
 * GET /admin/resale/list
 */
export const getResaleList = (params) => {
  return request.get('/resale/list', { params })
}

/**
 * 转售详情
 * GET /admin/resale/{id}
 */
export const getResaleDetail = (id) => {
  return request.get(`/resale/${id}`)
}

/**
 * 手动完成转售
 * POST /admin/resale/{id}/complete
 */
export const completeResale = (id) => {
  return request.post(`/resale/${id}/complete`)
}

/**
 * 强制取消转售
 * POST /admin/resale/{id}/cancel
 */
export const cancelResale = (id) => {
  return request.post(`/resale/${id}/cancel`)
}

/**
 * 平台抽佣统计
 * GET /admin/resale/platform-fee-stats
 */
export const getPlatformFeeStats = () => {
  return request.get('/resale/platform-fee-stats')
}

/**
 * 流通数据统计
 * GET /admin/resale/circulation-stats
 */
export const getCirculationStats = () => {
  return request.get('/resale/circulation-stats')
}

/**
 * 作品交易链路
 * GET /admin/resale/artwork/{artworkId}/trades
 */
export const getArtworkTrades = (artworkId) => {
  return request.get(`/resale/artwork/${artworkId}/trades`)
}

/**
 * 作品价格历史
 * GET /admin/resale/artwork/{artworkId}/price-history
 */
export const getArtworkPriceHistory = (artworkId) => {
  return request.get(`/resale/artwork/${artworkId}/price-history`)
}

/**
 * 作品转售统计
 * GET /admin/resale/artwork/{artworkId}/stats
 */
export const getArtworkResaleStats = (artworkId) => {
  return request.get(`/resale/artwork/${artworkId}/stats`)
}
