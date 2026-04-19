import request from './request'

// 用户登录
export const login = (data) => {
  return request({
    url: '/user/login',
    method: 'POST',
    data
  })
}

// 微信登录
export const wxLogin = (data) => {
  return request({
    url: '/user/wxlogin',
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
    url: '/user/update',
    method: 'POST',
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
    url: `/user/artist/${userId}`
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
