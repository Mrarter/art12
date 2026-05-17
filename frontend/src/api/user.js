import request from './request'

// 用户登录 (使用微信登录接口)
export const login = (data) => {
  return request({
    url: '/user/auth/wx-login',
    method: 'POST',
    data
  })
}

// 微信登录
export const wxLogin = (data) => {
  return request({
    url: '/user/auth/wx-login',
    method: 'POST',
    data
  })
}

// 获取用户信息
export const getUserInfo = () => {
  return request({
    url: '/user/info'
  })
}

// 更新用户信息
export const updateUserInfo = (data) => {
  return request({
    url: '/user/user/update',
    method: 'PUT',
    data
  })
}

// 退出登录
export const logout = () => {
  return request({
    url: '/user/logout',
    method: 'POST'
  })
}

// 获取收藏列表
export const getFavorites = (params) => {
  return request({
    url: '/user/favorites',
    data: params
  })
}

// 获取已购作品
export const getPurchased = (params) => {
  return request({
    url: '/user/purchased',
    data: params
  })
}

// 获取艺术家信息
export const getArtistInfo = (userId) => {
  return request({
    url: `/user/artist/info/${userId}`
  })
}

// 成为艺术家
export const becomeArtist = (data) => {
  return request({
    url: '/user/become-artist',
    method: 'POST',
    data
  })
}

// 获取个人中心数据
export const getUserCenter = () => {
  return request({
    url: '/user/center'
  })
}

// 获取收货地址列表
export const getAddressList = () => {
  return request({
    url: '/order/user/addresses'
  })
}

// 添加收货地址
export const addAddress = (data) => {
  return request({
    url: '/order/user/addresses',
    method: 'POST',
    data
  })
}

// 更新收货地址
export const updateAddress = (id, data) => {
  return request({
    url: `/order/user/addresses/${id}`,
    method: 'PUT',
    data
  })
}

// 删除收货地址
export const deleteAddress = (id) => {
  return request({
    url: `/order/user/addresses/${id}`,
    method: 'DELETE'
  })
}

// 关注艺术家
export const followArtist = (artistId) => {
  return request({
    url: `/user/artist/${artistId}/follow`,
    method: 'POST',
    data: {}
  })
}

// 取消关注艺术家
export const unfollowArtist = (artistId) => {
  return request({
    url: `/user/artist/${artistId}/follow`,
    method: 'DELETE',
    data: {}
  })
}

// 绑定手机号
export const bindPhone = (data) => {
  return request({
    url: '/user/bind-phone',
    method: 'POST',
    data
  })
}

// 获取关注列表
export const getFollowingList = (params) => {
  return request({
    url: '/user/following',
    data: params
  })
}

// ===================== 实名认证 API =====================

/** 提交实名认证申请 */
export const submitRealnameCert = (data) => {
  return request({
    url: '/user/realname/submit',
    method: 'POST',
    data
  })
}

/** 查询实名认证状态 */
export const getRealnameCertStatus = () => {
  return request({
    url: '/user/realname/status'
  })
}

// ===================== 以下为已有 API =====================

// 手机号登录
export const phoneLogin = (data) => {
  return request({
    url: '/user/phone-login',
    method: 'POST',
    data
  })
}

// 发送短信验证码
export const sendSmsCode = (phone, type = 'login') => {
  return request({
    url: '/user/sms-code',
    method: 'POST',
    data: { phone, type }
  })
}

// 搜索艺术家
export const searchArtists = (keyword, limit = 10) => {
  return request({
    url: '/user/artist/search',
    data: { keyword, limit }
  })
}

// 查找或创建艺术家
export const findOrCreateArtist = (name) => {
  return request({
    url: '/user/artist/find-or-create',
    data: { name }
  })
}

// 搜索全局用户列表（用于发布作品时选择作者）
export const searchUsers = (keyword, limit = 20) => {
  return request({
    url: '/user/search',
    data: { keyword, limit }
  })
}

// ===================== 数据分析 API =====================

/** 获取艺术家核心指标概览 */
export const getAnalyticsOverview = (artistId) => {
  return request({ url: `/user/artist/analytics/${artistId}/overview` })
}

/** 获取艺术家趋势数据 (days: 7/30/90) */
export const getAnalyticsTrend = (artistId, days = 30) => {
  return request({ url: `/user/artist/analytics/${artistId}/trend`, data: { days } })
}

/** 获取艺术家受众画像 */
export const getAudienceProfile = (artistId) => {
  return request({ url: `/user/artist/analytics/${artistId}/audience` })
}
