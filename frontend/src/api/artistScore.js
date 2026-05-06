import request from './request'

/**
 * 获取艺术家评分
 * GET /api/artist/score/{artistId}
 */
export function getArtistScore(artistId) {
  return request.get(`/api/artist/score/${artistId}`)
}

/**
 * 重新计算艺术家评分
 * POST /api/artist/score/recalculate/{artistId}
 */
export function recalculateArtistScore(artistId) {
  return request.post(`/api/artist/score/recalculate/${artistId}`)
}
