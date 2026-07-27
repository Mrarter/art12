import request, { rawRefreshRequest } from './request'

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

// 刷新 Token（无感刷新）
// 关键：必须使用 rawRefreshRequest 绕过拦截器，否则刷新 401 时会递归进入 401 处理 → 死锁
export const refreshToken = () => {
  return rawRefreshRequest({
    url: '/user/auth/refresh',
    method: 'POST'
  })
}

// 获取用户信息
export const getUserInfo = () => {
  return request({
    url: '/user/info',
    requireAuth: true
  })
}

// 生成微信小程序码
export const getMiniProgramCode = (params) => {
  return request({
    url: '/user/share/minicode',
    data: params
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

// 更新艺术家主页版式
export const updateArtistHomepageStyle = (style) => {
  return request({
    url: '/user/artist/homepage-style',
    method: 'PUT',
    requireAuth: true,
    data: { style }
  })
}

// 更新艺术家履历
export const updateArtistResume = (resume) => {
  return request({
    url: '/user/artist/resume',
    method: 'PUT',
    requireAuth: true,
    data: { resume }
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

// 提交艺术家入驻/认证申请
export const submitArtistCert = (data) => {
  return request({
    url: '/user/user/artist/cert',
    method: 'POST',
    requireAuth: true,
    data
  })
}

// 身份证 OCR 识别校验
export const verifyArtistIdCard = (data) => {
  return request({
    url: '/user/artist/cert/id-card/verify',
    method: 'POST',
    requireAuth: true,
    data
  })
}

// 查询艺术家入驻/认证状态
export const getArtistCertStatus = () => {
  return request({
    url: '/user/artist/cert/status',
    requireAuth: true
  })
}

// 获取个人中心数据
export const getUserCenter = () => {
  return request({
    url: '/user/center',
    requireAuth: true
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

// 账号安全概览
export const getAccountSecurity = () => {
  return request({
    url: '/user/security',
    requireAuth: true
  })
}

// 绑定/换绑手机号
export const updateSecurityPhone = (data) => {
  return request({
    url: '/user/security/phone',
    method: 'POST',
    requireAuth: true,
    data
  })
}

// 设置/修改登录密码
export const updateSecurityPassword = (data) => {
  return request({
    url: '/user/security/password',
    method: 'POST',
    requireAuth: true,
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

/** 发起支付宝实名认证 */
export const startAlipayRealname = (data) => {
  return request({
    url: '/user/realname/alipay/start',
    method: 'POST',
    data
  })
}

/** 同步支付宝实名认证结果 */
export const syncAlipayRealname = (data) => {
  return request({
    url: '/user/realname/alipay/sync',
    method: 'POST',
    data
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

// 密码登录
export const passwordLogin = (data) => {
  return request({
    url: '/user/password-login',
    method: 'POST',
    data
  })
}

// 发送短信验证码
export const sendSmsCode = async (phone, type = 'login') => {
  try {
    await request({
      url: '/user/sms-code',
      method: 'POST',
      data: { phone, type }
    })
    return { mock: false }
  } catch (error) {
    const message = error?.message || ''
    const canUseMockCode = typeof window !== 'undefined' && message.includes('NOT_FOUND')

    if (canUseMockCode) {
      return {
        mock: true,
        code: '888888'
      }
    }

    throw error
  }
}

// 用户注册
export const register = (data) => {
  return request({
    url: '/user/register',
    method: 'POST',
    data
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
