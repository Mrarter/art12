/**
 * 用户状态管理
 */
import { defineStore } from 'pinia'
import { getUserInfo, getUserCenter } from '@/api/user'
import {
  getTokenData,
  setTokenData,
  getUserInfo as getStoredUserInfo,
  isGuestUser,
  handleLoginSuccess,
  handleLogout as authLogout
} from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => {
    // 从新的 auth 模块初始化
    const tokenData = getTokenData()
    return {
      token: tokenData?.accessToken || '',
      tokenData: tokenData,  // 完整的 token 数据（含过期时间）
      userInfo: getStoredUserInfo() || null,
      openId: '',
      centerData: null, // 个人中心聚合数据
      identities: [], // 身份数组 ['artist', 'collector', 'promoter']
      isArtist: false,
      isPromoter: false,
      isAgent: false
    }
  },

  getters: {
    // 是否已登录（非游客）
    isLogin: (state) => !!state.token && !isGuestUser(),
    // 是否为游客
    isGuest: () => isGuestUser(),
    // 是否已认证（登录态有效）
    isAuthenticated: (state) => !!state.token,
    currentIdentity: (state) => state.userInfo?.identity || 'collector',
    avatar: (state) => state.userInfo?.avatar || '/static/images/avatar.png',
    nickname: (state) => state.userInfo?.nickname || '未登录'
  },

  actions: {
    // 设置Token（兼容旧接口，同时更新 auth 模块）
    setToken(token) {
      this.token = token
      // 同时更新 auth 模块
      const currentData = getTokenData() || {}
      setTokenData({
        ...currentData,
        accessToken: token
      })
      uni.setStorageSync('token', token)
    },

    // 设置完整的 Token 数据（包含过期时间）
    setTokenWithExpiry(token, expiresAt, userId) {
      this.token = token
      this.tokenData = {
        accessToken: token,
        expiresAt,
        userId,
        tokenType: 'bearer',
        isGuest: false
      }
      setTokenData(this.tokenData)
      uni.setStorageSync('token', token)
    },

    // 设置用户信息
    setUserInfo(info) {
      this.userInfo = info
      uni.setStorageSync('userInfo', info)
      this.updateIdentities(info)
      // 提取 openId
      if (info?.openId || info?.openid) {
        this.openId = info.openId || info.openid
        uni.setStorageSync('openId', this.openId)
      }
    },

    // 登录成功处理（集成 auth 模块）
    onLoginSuccess(token, userInfo) {
      const isGuest = userInfo?.isGuest || token === 'guest_token'

      // 存储 token 数据（包含过期时间，假设 7 天有效期）
      const expiresAt = Date.now() + 7 * 24 * 60 * 60 * 1000
      this.setTokenWithExpiry(token, expiresAt, userInfo?.id || userInfo?.userId)

      // 存储用户信息
      if (userInfo) {
        this.setUserInfo(userInfo)
      }

      // 调用 auth 模块的登录成功处理
      handleLoginSuccess(token, userInfo, isGuest)
    },

    // 设置 openId
    setOpenId(openId) {
      this.openId = openId
      uni.setStorageSync('openId', openId)
    },

    // 更新身份信息
    updateIdentities(info) {
      const rawIdentities = info?.identities || info?.identity_json || info?.identity || ['collector']
      if (Array.isArray(rawIdentities)) {
        this.identities = rawIdentities
      } else if (typeof rawIdentities === 'string') {
        try {
          const parsed = JSON.parse(rawIdentities)
          this.identities = Array.isArray(parsed) ? parsed : [String(parsed)]
        } catch (e) {
          this.identities = rawIdentities.split(',').map(item => item.trim()).filter(Boolean)
        }
      } else {
        this.identities = ['collector']
      }
      if (!this.identities.length) this.identities = ['collector']
      this.isArtist = this.identities.includes('artist')
      this.isPromoter = this.identities.includes('promoter')
      this.isAgent = this.identities.includes('agent')
    },

    // 获取用户信息
    async fetchUserInfo() {
      // 无 Token → 跳过 API 调用，避免触发 401 流程
      if (!this.token || isGuestUser()) {
        return null
      }

      try {
        const info = await getUserInfo()
        this.setUserInfo(info)
        return info
      } catch (e) {
        return null
      }
    },

    // 获取个人中心数据
    async fetchCenterData() {
      try {
        const data = await getUserCenter()
        this.centerData = data
        return data
      } catch (e) {
        return null
      }
    },

    // 退出登录
    logout() {
      this.token = ''
      this.tokenData = null
      this.userInfo = null
      this.centerData = null
      this.identities = []
      this.isArtist = false
      this.isPromoter = false
      this.isAgent = false

      // 清除所有存储
      authLogout()
    },

    // 初始化用户信息（兼容小程序端调用）
    initUserInfo() {
      return this.fetchUserInfo()
    }
  }
})
