/**
 * 用户状态管理
 */
import { defineStore } from 'pinia'
import { getUserInfo, getUserCenter } from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: uni.getStorageSync('token') || '',
    userInfo: uni.getStorageSync('userInfo') || null,
    centerData: null, // 个人中心聚合数据
    identities: [], // 身份数组 ['artist', 'collector', 'promoter']
    isArtist: false,
    isPromoter: false,
    isAgent: false
  }),
  
  getters: {
    isLogin: (state) => !!state.token,
    currentIdentity: (state) => state.userInfo?.identity || 'collector',
    avatar: (state) => state.userInfo?.avatar || '/static/images/avatar.png',
    nickname: (state) => state.userInfo?.nickname || '未登录'
  },
  
  actions: {
    // 设置Token
    setToken(token) {
      this.token = token
      uni.setStorageSync('token', token)
    },
    
    // 设置用户信息
    setUserInfo(info) {
      this.userInfo = info
      uni.setStorageSync('userInfo', info)
      this.updateIdentities(info)
    },
    
    // 更新身份信息
    updateIdentities(info) {
      const identityJson = info?.identity_json || info?.identity || '["collector"]'
      try {
        this.identities = typeof identityJson === 'string' ? JSON.parse(identityJson) : identityJson
      } catch (e) {
        this.identities = ['collector']
      }
      this.isArtist = this.identities.includes('artist')
      this.isPromoter = this.identities.includes('promoter')
      this.isAgent = this.identities.includes('agent')
    },
    
    // 获取用户信息
    async fetchUserInfo() {
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
      this.userInfo = null
      this.centerData = null
      this.identities = []
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
    }
  }
})
