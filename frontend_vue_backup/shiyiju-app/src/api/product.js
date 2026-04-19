/**
 * 商品/作品相关 API
 */
import { get, post } from './request'

/**
 * 获取艺术门类列表
 */
export function getCategoryList() {
  return get('/product/category/list')
}

/**
 * 获取作品列表（画廊）
 */
export function getArtworkList(params) {
  return get('/product/artwork/list', params)
}

/**
 * 获取作品详情
 */
export function getArtworkDetail(id) {
  return get('/product/artwork/detail/' + id)
}

/**
 * 获取首页推荐
 */
export function getHomepageRecommend() {
  return get('/product/homepage/recommend')
}

/**
 * 获取首页 Banner
 */
export function getHomepageBanners() {
  return get('/product/homepage/banners')
}

/**
 * 搜索作品
 */
export function searchArtworks(params) {
  return get('/product/artwork/search', params)
}

/**
 * 收藏作品
 */
export function favoriteArtwork(id) {
  return post('/product/artwork/favorite/' + id)
}

/**
 * 取消收藏作品
 */
export function unfavoriteArtwork(id) {
  return post('/product/artwork/unfavorite/' + id)
}

/**
 * 获取艺术家主页信息
 */
export function getArtistHomepage(userId) {
  return get('/product/homepage/' + userId)
}

/**
 * 获取艺术家作品列表
 */
export function getArtistWorks(userId, params) {
  return get('/product/homepage/' + userId + '/works', params)
}

/**
 * 获取艺术家动态
 */
export function getArtistMoments(userId, params) {
  return get('/product/homepage/' + userId + '/moments', params)
}

/**
 * 关注艺术家
 */
export function followArtist(userId) {
  return post('/product/homepage/' + userId + '/follow')
}

/**
 * 取消关注艺术家
 */
export function unfollowArtist(userId) {
  return post('/product/homepage/' + userId + '/unfollow')
}
