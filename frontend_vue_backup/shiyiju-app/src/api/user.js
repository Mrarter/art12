/**
 * 用户相关 API
 */
import { get, post, put } from './request'

/**
 * 微信登录
 */
export function wxLogin(data) {
  return post('/user/wx/login', data)
}

/**
 * 获取用户信息
 */
export function getUserInfo() {
  return get('/user/info')
}

/**
 * 更新用户信息
 */
export function updateUserInfo(data) {
  return put('/user/info', data)
}

/**
 * 开通艺荐官
 */
export function openPromoter() {
  return post('/user/promoter/open')
}

/**
 * 获取艺荐官邀请码
 */
export function getPromoterInviteCode() {
  return get('/user/promoter/invite-code')
}

/**
 * 艺术家认证申请
 */
export function applyArtistCert(data) {
  return post('/user/artist/cert/apply', data)
}

/**
 * 获取艺术家认证状态
 */
export function getArtistCertStatus() {
  return get('/user/artist/cert/status')
}

/**
 * 获取收货地址列表
 */
export function getAddressList() {
  return get('/user/address/list')
}

/**
 * 添加收货地址
 */
export function addAddress(data) {
  return post('/user/address/add', data)
}

/**
 * 更新收货地址
 */
export function updateAddress(data) {
  return put('/user/address/update', data)
}

/**
 * 删除收货地址
 */
export function deleteAddress(id) {
  return del(`/user/address/delete/${id}`)
}

/**
 * 设置默认地址
 */
export function setDefaultAddress(id) {
  return put('/user/address/default/' + id)
}

/**
 * 获取收藏列表
 */
export function getFavorites(params) {
  return get('/user/favorites', params)
}

/**
 * 获取浏览足迹
 */
export function getBrowseHistory(params) {
  return get('/user/history', params)
}
