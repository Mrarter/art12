import request from './request'

/**
 * 获取艺术家评分
 * GET /api/artist/score/{artistId}
 *
 * 注意：后端可能返回 404（数据库无该艺术家评分记录）或 null（评分未计算）。
 * 调用方（detail.vue loadArtistScore）已有 try/catch，会优雅降级。
 * 此处额外处理：404 响应也 resolve 为 null 而非 reject，
 * 避免 404 触发 401 处理流程。
 */
export function getArtistScore(artistId) {
  return new Promise((resolve, reject) => {
    request.get(`/artist/score/${artistId}`)
      .then(data => resolve(data || null))
      .catch(err => {
        // 404：数据库无记录 → 当作空数据处理，不抛出错误
        if (err.message === 'NOT_FOUND') {
          resolve(null)
          return
        }
        // 其他错误（401 等）继续抛出
        reject(err)
      })
  })
}

/**
 * 重新计算艺术家评分
 * POST /api/artist/score/recalculate/{artistId}
 */
export function recalculateArtistScore(artistId) {
  return request.post(`/artist/score/recalculate/${artistId}`)
}
