/**
 * 作品/画廊相关 API
 * 接口: 5.3节 画廊模块
 */
import request from './request'

/**
 * 获取作品列表
 * GET /product/list
 * @param {Object} params - 筛选参数
 * @param {number} params.categoryId - 分类ID
 * @param {string} params.sort - 排序: price_asc/price_desc/time/new
 * @param {number} params.minPrice - 最低价格
 * @param {number} params.maxPrice - 最高价格
 * @param {number} params.yearFrom - 创作年代起始
 * @param {number} params.yearTo - 创作年代结束
 * @param {string} params.region - 艺术家地区
 * @param {number} params.holdDuration - 持有时长(天)
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 */
export const getProductList = (params) => {
  return request.get('/product/list', params)
}

// 获取画廊列表（别名）
export const getGalleryList = (params) => {
  return getProductList(params)
}

/**
 * 获取作品详情
 * GET /product/{id}
 */
export const getProductDetail = (id) => {
  return request.get(`/product/${id}`)
}

/**
 * 获取作品详情（通过 artcircle）
 * GET /artcircle/detail/{id}
 */
export const getArtworkDetail = (id) => {
  return request.get(`/artcircle/detail/${id}`)
}

/**
 * 搜索作品
 * GET /product/search
 * @param {string} params.keyword - 搜索关键词
 */
export const searchProduct = (params) => {
  return request.get('/product/search', params)
}

/**
 * 获取Banner列表
 * GET /product/banners
 */
export const getBanners = () => {
  return request.get('/product/banners')
}

/**
 * 获取分类列表
 * GET /product/categories
 */
export const getCategories = () => {
  return request.get('/product/categories')
}

/**
 * 收藏作品
 * POST /product/favorite
 * @param {number} artworkId - 作品ID
 */
export const addFavorite = (artworkId) => {
  return request.post('/product/favorite', { artworkId })
}

/**
 * 取消收藏
 * DELETE /product/favorite/{id}
 */
export const removeFavorite = (id) => {
  return request.delete(`/product/favorite/${id}`)
}

/**
 * 获取我的收藏列表
 * GET /product/favorites
 */
export const getFavorites = (params) => {
  return request.get('/product/favorites', params)
}

/**
 * 获取推荐作品
 * GET /product/recommend
 */
export const getRecommend = (params) => {
  return request.get('/product/recommend', params)
}

/**
 * 获取关注艺术家作品
 * GET /product/following
 */
export const getFollowingWorks = (params) => {
  return request.get('/product/following', params)
}

/**
 * 获取我的作品列表（艺术家）
 * GET /product/my-works
 */
export const getMyWorks = (params) => {
  return request.get('/product/my-works', params)
}

/**
 * 发布作品
 * POST /product/publish
 * @param {Object} data - 作品信息
 */
export const publishArtwork = (data) => {
  return request.post('/product/publish', data)
}

/**
 * 更新作品
 * PUT /product/{id}
 * @param {number} id - 作品ID
 * @param {Object} data - 作品信息
 */
export const updateArtwork = (id, data) => {
  return request.put(`/product/${id}`, data)
}

/**
 * 更新作品状态
 * PUT /product/{id}/status
 * @param {number} id - 作品ID
 * @param {string} status - 状态: online/offline
 */
export const updateWorkStatus = (id, status) => {
  return request.put(`/product/${id}/status`, { status })
}

/**
 * 删除作品
 * DELETE /product/{id}
 * @param {number} id - 作品ID
 */
export const deleteWork = (id) => {
  return request.delete(`/product/${id}`)
}

/**
 * 获取作品价格变动记录
 * GET /product/price-records
 */
export const getPriceRecords = (params) => {
  return request.get('/product/price-records', params)
}

/**
 * ========== 评价相关 API ==========
 */

/**
 * 获取作品评价列表
 * GET /product/reviews/artwork/{artworkId}
 * @param {number} artworkId - 作品ID
 * @param {number} page - 页码
 * @param {number} pageSize - 每页数量
 */
export const getArtworkReviews = (artworkId, params) => {
  return request.get(`/product/reviews/artwork/${artworkId}`, params)
}

/**
 * 获取用户评价列表
 * GET /product/reviews/user/{userId}
 * @param {number} userId - 用户ID
 * @param {number} page - 页码
 * @param {number} pageSize - 每页数量
 */
export const getUserReviews = (userId, params) => {
  return request.get(`/product/reviews/user/${userId}`, params)
}

/**
 * 创建评价
 * POST /product/reviews
 * @param {Object} data
 * @param {number} data.artworkId - 作品ID
 * @param {number} data.orderId - 订单ID
 * @param {number} data.orderItemId - 订单项ID
 * @param {number} data.rating - 总体评分 1-5
 * @param {string} data.content - 评价内容
 * @param {string} data.images - 评价图片（逗号分隔）
 * @param {number} data.qualityRating - 质量评分
 * @param {number} data.logisticsRating - 物流评分
 * @param {number} data.serviceRating - 服务评分
 * @param {number} data.isAnonymous - 是否匿名 0-否 1-是
 */
export const createReview = (data) => {
  return request.post('/product/reviews', data)
}

/**
 * 删除评价
 * DELETE /product/reviews/{reviewId}
 * @param {number} reviewId - 评价ID
 */
export const deleteReview = (reviewId) => {
  return request.delete(`/product/reviews/${reviewId}`)
}

/**
 * 获取评价详情
 * GET /product/reviews/{reviewId}
 * @param {number} reviewId - 评价ID
 */
export const getReviewDetail = (reviewId) => {
  return request.get(`/product/reviews/${reviewId}`)
}

/**
 * 获取作品评分统计
 * GET /product/reviews/stats/{artworkId}
 * @param {number} artworkId - 作品ID
 */
export const getArtworkReviewStats = (artworkId) => {
  return request.get(`/product/reviews/stats/${artworkId}`)
}

/**
 * 商家回复评价
 * POST /product/reviews/{reviewId}/reply
 * @param {number} reviewId - 评价ID
 * @param {string} content - 回复内容
 */
export const replyToReview = (reviewId, content) => {
  return request.post(`/product/reviews/${reviewId}/reply`, { content })
}
