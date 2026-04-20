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
