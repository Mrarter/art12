/**
 * 用户状态管理
 */
import { defineStore } from 'pinia'
import { wxLogin, getUserInfo } from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: uni.getStorageSync('token') || '',
    userInfo: uni.getStorageSync('userInfo') || null,
    isLoggedIn: false
  }),
  
  getters: {
    hasIdentity: (state) => (identity) => {
      if (!state.userInfo?.identities) return false
      return state.userInfo.identities.includes(identity)
    },
    isArtist: (state) => state.userInfo?.isArtist || false,
    isAgent: (state) => state.userInfo?.isAgent || false,
    isCollector: (state) => state.userInfo?.isCollector || false,
    isPromoter: (state) => state.userInfo?.isPromoter || false
  },
  
  actions: {
    /**
     * 检查登录状态
     */
    checkLogin() {
      const token = uni.getStorageSync('token')
      if (token) {
        this.token = token
        this.isLoggedIn = true
        this.fetchUserInfo()
      }
    },
    
    /**
     * 微信登录
     */
    async login(loginData) {
      try {
        const res = await wxLogin(loginData)
        this.token = res.token
        this.isLoggedIn = true
        uni.setStorageSync('token', res.token)
        uni.setStorageSync('userInfo', res)
        this.userInfo = res
        
        if (res.isNewUser) {
          // 新用户引导设置
          uni.navigateTo({ url: '/pages/profile/setup' })
        }
        
        return res
      } catch (e) {
        console.error('登录失败', e)
        throw e
      }
    },
    
    /**
     * 获取用户信息
     */
    async fetchUserInfo() {
      try {
        const res = await getUserInfo()
        this.userInfo = res
        uni.setStorageSync('userInfo', res)
      } catch (e) {
        console.error('获取用户信息失败', e)
      }
    },
    
    /**
     * 退出登录
     */
    logout() {
      this.token = ''
      this.userInfo = null
      this.isLoggedIn = false
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
      uni.reLaunch({ url: '/pages/index/index' })
    },
    
    /**
     * 更新本地用户信息
     */
    updateUserInfo(info) {
      if (this.userInfo) {
        this.userInfo = { ...this.userInfo, ...info }
        uni.setStorageSync('userInfo', this.userInfo)
      }
    }
  }
})
