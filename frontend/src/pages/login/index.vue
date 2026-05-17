<template>
  <view class="login-page">
    <image class="hero-bg" src="/static/images/museum-v12-hero-bg.png" mode="aspectFill"></image>
    <view class="page-shade"></view>

    <view class="brand-section">
      <view class="brand-mark">
        <image class="brand-logo" :src="loginBrandLogo" mode="aspectFit"></image>
      </view>
      <view class="brand-copy">
        <text class="app-name">拾艺局</text>
        <text class="app-slogan">艺术收藏、发布与分享的一站式入口</text>
      </view>
    </view>

    <view class="identity-panel">
      <view class="panel-head">
        <text class="panel-title">先登录，再申请身份认证</text>
        <text class="panel-subtitle">默认以收藏家身份进入，登录后可在我的页面申请艺术家或艺荐官认证。</text>
      </view>
      <view class="identity-note">
        <view class="note-badge">
          <text>默认身份</text>
        </view>
        <view class="note-copy">
          <text class="note-title">收藏家模式</text>
          <text class="note-desc">先浏览作品、参与拍卖与收藏，认证通过后再开启发布或推广能力。</text>
        </view>
      </view>
    </view>

    <view class="feature-strip">
      <view class="feature-item" v-for="item in featureItems" :key="item.title">
        <view class="feature-icon">
          <image :src="item.icon" mode="aspectFit"></image>
        </view>
        <view class="feature-text">
          <text class="feature-title">{{ item.title }}</text>
          <text class="feature-desc">{{ item.desc }}</text>
        </view>
      </view>
    </view>

    <view class="login-section">
      <view class="agreement-text">
        登录即表示同意
        <text class="link" @click="viewAgreement('user')">《用户协议》</text>
        和
        <text class="link" @click="viewAgreement('privacy')">《隐私政策》</text>
      </view>

      <button class="btn-wechat" @click="onWechatLogin" :loading="loading">
        <text>微信登录</text>
      </button>

      <button class="btn-phone" @click="openPhoneLogin">
        <text>手机号登录</text>
      </button>

      <button class="btn-guest" @click="onGuestLogin">
        <text>游客体验</text>
      </button>
    </view>
    
    <!-- 手机号登录弹窗 -->
    <view v-if="showPhoneLogin" class="popup-mask" @click="closePhoneLogin">
      <view class="phone-login-popup">
        <view class="popup-header">
          <text class="popup-title">手机号登录</text>
          <text class="popup-close iconfont icon-close" @click.stop="closePhoneLogin"></text>
        </view>
        
        <view class="phone-form" @click.stop>
          <view class="form-item">
            <input class="phone-input" type="number" v-model="phoneForm.phone" placeholder="请输入手机号" maxlength="11" />
          </view>
          
          <view class="form-item captcha-item">
            <input class="captcha-input" type="number" v-model="phoneForm.captcha" placeholder="请输入验证码" maxlength="6" />
            <button class="captcha-btn" :disabled="captchaCountdown > 0" @click="sendCaptcha">
              {{ captchaCountdown > 0 ? `${captchaCountdown}s` : '获取验证码' }}
            </button>
          </view>
          
          <button class="btn-submit" :disabled="!canSubmitPhone" @click="onPhoneLogin">登录</button>
        </view>
        
        <!-- 微信授权说明 -->
        <view class="wechat-tip">
          <text>登录后将同步获取您的微信昵称、头像等信息</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { wxLogin, phoneLogin, sendSmsCode } from '@/api/user'
import { useUserStore } from '@/store/modules/user'
import loginBrandLogo from '@/static/logo.png'

export default {
  data() {
    return {
      loading: false,
      loginBrandLogo,
      showPhoneLogin: false,
      redirect: '',
      phoneForm: {
        phone: '',
        captcha: ''
      },
      captchaCountdown: 0
    }
  },
  
  computed: {
    featureItems() {
      return [
        { title: '精品拍卖', desc: '竞拍与订单统一管理', icon: '/static/art-icons/icon-payment.svg' },
        { title: '艺术社交', desc: '关注艺术家与创作动态', icon: '/static/art-icons/icon-follow.svg' },
        { title: '分享收益', desc: '推广作品获得佣金', icon: '/static/art-icons/icon-budget.svg' }
      ]
    },
    canSubmitPhone() {
      return /^1[3-9]\d{9}$/.test(this.phoneForm.phone) && 
             this.phoneForm.captcha.length === 6
    }
  },
  
  onLoad(options = {}) {
    this.initLogin(options)
  },
  
  methods: {
    // 初始化登录状态检查
    initLogin(options = {}) {
      this.redirect = this.decodeRedirect(options.redirect || '')
      const userStore = useUserStore()
      // 如果已登录，直接跳转
      if (userStore.isAuthenticated) {
        this.afterLogin()
      }
    },
    
    // 微信登录
    async onWechatLogin() {
      if (this.loading) return
      this.loading = true
      
      try {
        const profile = await this.resolveWechatProfile()
        const { code } = await this.resolveWechatLoginCode()
        
        // 调用后端登录接口
        const data = await wxLogin({ 
          code, 
          ...profile
        })
        
        // 保存Token和用户信息
        const userStore = useUserStore()
        userStore.setToken(data.token)
        userStore.setOpenId(data.openId || '')
        userStore.setUserInfo(this.buildLoginUserInfo(data, profile))
        
        const toastTitle = data.phone ? '微信登录成功' : '已同步微信头像昵称'
        uni.showToast({ title: toastTitle, icon: 'success' })
        
        setTimeout(() => {
          this.afterLogin()
        }, 1500)
      } catch (e) {
        console.error('微信登录失败', e)
        const errMsg = e.message || '微信登录失败，请重试'
        
        // 配置缺失类错误（500）→ 使用模态框，更醒目
        if (errMsg.includes('暂不可用') || errMsg.includes('联系管理员') || errMsg.includes('小程序密钥')) {
          uni.showModal({
            title: '微信登录不可用',
            content: errMsg,
            confirmText: '我知道了',
            showCancel: false
          })
        } else {
          uni.showToast({ title: errMsg, icon: 'none', duration: 3000 })
        }
      } finally {
        this.loading = false
      }
    },

    openPhoneLogin() {
      this.showPhoneLogin = true
    },

    closePhoneLogin() {
      this.showPhoneLogin = false
    },
    
    // 手机号登录
    async onPhoneLogin() {
      if (!this.canSubmitPhone) return
      
      try {
        const data = await phoneLogin({
          phone: this.phoneForm.phone,
          code: this.phoneForm.captcha
        })
        
        const userStore = useUserStore()
        userStore.setToken(data.token)
        userStore.setUserInfo(this.buildPhoneLoginUserInfo(data))
        
        uni.showToast({ title: '登录成功', icon: 'success' })
        this.closePhoneLogin()
        
        setTimeout(() => {
          this.afterLogin()
        }, 1500)
      } catch (e) {
        console.error('手机号登录失败', e)
        uni.showToast({ title: '登录失败，请重试', icon: 'none' })
      }
    },
    
    // 发送验证码
    async sendCaptcha() {
      if (!/^1[3-9]\d{9}$/.test(this.phoneForm.phone)) {
        uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
        return
      }
      
      try {
        await sendSmsCode(this.phoneForm.phone, 'login')
        uni.showToast({ title: '验证码已发送', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: '验证码发送失败', icon: 'none' })
        return
      }
      
      // 开始倒计时
      this.captchaCountdown = 60
      const timer = setInterval(() => {
        this.captchaCountdown--
        if (this.captchaCountdown <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    },
    
    // 游客体验
    onGuestLogin() {
      const userStore = useUserStore()
      userStore.setToken('guest_token')
      userStore.setUserInfo({
        id: 0,
        nickname: '游客',
        avatar: '',
        isGuest: true,
        currentIdentity: 'collector'
      })
      
      uni.showToast({ title: '已进入游客模式', icon: 'success' })
      
      setTimeout(() => {
        this.afterLogin()
      }, 1500)
    },
    
    afterLogin() {
      if (this.redirect) {
        uni.navigateTo({ url: this.redirect })
        return
      }
      uni.switchTab({ url: '/pages/index/index' })
    },

    async resolveWechatLoginCode() {
      try {
        const loginRes = await new Promise((resolve, reject) => {
          uni.login({
            provider: 'weixin',
            success: (res) => {
              if (res && res.code) {
                resolve(res)
              } else {
                reject(new Error('获取微信授权码失败'))
              }
            },
            fail: (err) => {
              reject(err || new Error('微信登录不可用'))
            }
          })
        })
        return loginRes
      } catch (error) {
        if (this.canUseDevWechatFallback()) {
          return { code: `h5_dev_${Date.now()}` }
        }
        throw error
      }
    },

    async resolveWechatProfile() {
      const fallback = this.buildDevWechatProfile()
      if (typeof wx === 'undefined' || typeof wx.getUserProfile !== 'function') {
        return fallback
      }
      try {
        const profileRes = await new Promise((resolve, reject) => {
          wx.getUserProfile({
            desc: '用于完善头像、昵称与身份资料',
            success: resolve,
            fail: reject
          })
        })
        const userInfo = profileRes?.userInfo || {}
        return {
          nickname: userInfo.nickName || fallback.nickname,
          avatar: userInfo.avatarUrl || fallback.avatar,
          gender: typeof userInfo.gender === 'number' ? userInfo.gender : 0,
          region: [userInfo.country, userInfo.province, userInfo.city].filter(Boolean).join(' ')
        }
      } catch (error) {
        if (this.canUseDevWechatFallback()) {
          return fallback
        }
        throw new Error('未完成微信头像授权')
      }
    },

    canUseDevWechatFallback() {
      return typeof window !== 'undefined'
    },

    decodeRedirect(value) {
      let text = value || ''
      for (let i = 0; i < 2; i += 1) {
        try {
          const decoded = decodeURIComponent(text)
          if (decoded === text) break
          text = decoded
        } catch (error) {
          break
        }
      }
      return text
    },

    buildDevWechatProfile() {
      return {
        nickname: '微信用户',
        avatar: this.loginBrandLogo,
        gender: 0,
        region: '本地调试'
      }
    },

    normalizeIdentityList(rawValue) {
      if (Array.isArray(rawValue)) return rawValue
      if (typeof rawValue === 'string' && rawValue.trim()) {
        return rawValue.split(',').map(item => item.trim()).filter(Boolean)
      }
      return ['collector']
    },

    buildLoginUserInfo(data, profile = {}) {
      const identities = this.normalizeIdentityList(data.identities)
      return {
        id: data.userId,
        userId: data.userId,
        uid: data.uid || '',
        nickname: data.nickname || profile.nickname || '微信用户',
        avatar: data.avatar || profile.avatar || this.loginBrandLogo,
        phone: data.phone || '',
        identities,
        openId: data.openId || '',
        currentIdentity: identities[0] || 'collector'
      }
    },

    buildPhoneLoginUserInfo(data) {
      const source = data.userInfo || data || {}
      const identities = this.normalizeIdentityList(source.identities || source.identity)
      return {
        ...source,
        id: source.id || source.userId || data.userId || 0,
        userId: source.userId || source.id || data.userId || 0,
        uid: source.uid || data.uid || '',
        nickname: source.nickname || source.nickName || this.phoneForm.phone || '用户',
        avatar: source.avatar || this.loginBrandLogo,
        phone: source.phone || this.phoneForm.phone,
        identities,
        currentIdentity: identities[0] || 'collector'
      }
    },

    // 查看协议
    viewAgreement(type) {
      const urls = {
        user: '/pages/user/agreement?type=terms',
        privacy: '/pages/user/agreement?type=privacy'
      }
      uni.navigateTo({ url: urls[type] })
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  position: relative;
  padding: 64rpx 28rpx calc(34rpx + env(safe-area-inset-bottom));
  background: #0b0b0c;
  color: #f6f2e8;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  width: 100%;
  height: 470rpx;
  opacity: 0.42;
}

.page-shade {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(11, 11, 12, 0.25) 0%, #0b0b0c 44%, #0b0b0c 100%),
    radial-gradient(circle at 18% 8%, rgba(201, 162, 39, 0.18), transparent 38%);
}

.brand-section,
.identity-panel,
.feature-strip,
.login-section {
  position: relative;
  z-index: 1;
}

.brand-section {
  min-height: 310rpx;
  display: flex;
  align-items: flex-end;
  gap: 24rpx;
  padding-bottom: 26rpx;
}

.brand-mark {
  width: 118rpx;
  height: 118rpx;
  border-radius: 32rpx;
  background: rgba(17, 13, 10, 0.28);
  border: 2rpx solid rgba(232, 186, 70, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 20rpx 48rpx rgba(232, 186, 70, 0.18);
  overflow: hidden;
}

.brand-logo {
  width: 100%;
  height: 100%;
}

.brand-copy {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.app-name {
  font-size: 52rpx;
  line-height: 60rpx;
  font-weight: 800;
  color: #f6f2e8;
}

.app-slogan {
  font-size: 24rpx;
  line-height: 34rpx;
  color: #b9b1a5;
}

.identity-panel {
  padding: 24rpx;
  border-radius: 18rpx;
  background: rgba(23, 23, 25, 0.94);
  border: 1rpx solid rgba(255, 255, 255, 0.08);
}

.panel-head {
  margin-bottom: 20rpx;
}

.panel-title {
  display: block;
  font-size: 30rpx;
  line-height: 38rpx;
  color: #f6f2e8;
  font-weight: 700;
}

.panel-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 32rpx;
  color: #8f887e;
}

.identity-note {
  min-height: 112rpx;
  padding: 22rpx 20rpx;
  border-radius: 14rpx;
  background: rgba(201, 162, 39, 0.12);
  border: 1rpx solid rgba(201, 162, 39, 0.32);
  display: flex;
  align-items: flex-start;
  gap: 18rpx;
  box-sizing: border-box;
}

.note-badge {
  min-width: 108rpx;
  height: 52rpx;
  padding: 0 16rpx;
  border-radius: 26rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: rgba(201, 162, 39, 0.2);
}

.note-badge text {
  font-size: 22rpx;
  line-height: 22rpx;
  color: #ddb53a;
  font-weight: 700;
}

.note-copy {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.note-title {
  font-size: 28rpx;
  line-height: 36rpx;
  color: #f6f2e8;
  font-weight: 700;
}

.note-desc {
  font-size: 22rpx;
  line-height: 32rpx;
  color: #8f887e;
}

.feature-strip {
  margin-top: 18rpx;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
}

.feature-item {
  min-height: 142rpx;
  padding: 16rpx 12rpx;
  border-radius: 14rpx;
  background: rgba(255, 255, 255, 0.045);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  box-sizing: border-box;
}

.feature-icon {
  width: 48rpx;
  height: 48rpx;
  border-radius: 12rpx;
  background: rgba(201, 162, 39, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10rpx;
}

.feature-icon image {
  width: 28rpx;
  height: 28rpx;
}

.feature-text {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.feature-title {
  font-size: 22rpx;
  line-height: 30rpx;
  color: #f6f2e8;
  font-weight: 700;
}

.feature-desc {
  margin-top: 4rpx;
  font-size: 19rpx;
  line-height: 28rpx;
  color: #777166;
}

.login-section {
  margin-top: auto;
  padding-top: 34rpx;
}

.agreement-text {
  margin-bottom: 18rpx;
  font-size: 22rpx;
  color: #8f887e;
  text-align: center;
  line-height: 34rpx;
}

.link {
  color: #c9a227;
}

.btn-wechat,
.btn-phone,
.btn-guest,
.btn-submit {
  width: 100%;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-wechat::after,
.btn-phone::after,
.btn-guest::after,
.btn-submit::after,
.captcha-btn::after {
  border: none;
}

.btn-wechat {
  height: 92rpx;
  border-radius: 46rpx;
  background: #c9a227;
  color: #16130b;
  font-size: 30rpx;
  font-weight: 800;
  margin-bottom: 18rpx;
}

.btn-phone {
  height: 90rpx;
  border-radius: 45rpx;
  color: #f6f2e8;
  font-size: 29rpx;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.055);
  border: 1rpx solid rgba(255, 255, 255, 0.16);
}

.btn-guest {
  height: 72rpx;
  margin-top: 14rpx;
  color: #8f887e;
  font-size: 24rpx;
  background: transparent;
}

.popup-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 99;
  display: flex;
  align-items: flex-end;
  background: rgba(0, 0, 0, 0.62);
}

.phone-login-popup {
  width: 100%;
  padding: 34rpx 28rpx calc(34rpx + env(safe-area-inset-bottom));
  border-radius: 24rpx 24rpx 0 0;
  background: #171719;
  border-top: 1rpx solid rgba(255, 255, 255, 0.08);
  box-sizing: border-box;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28rpx;
}

.popup-title {
  font-size: 34rpx;
  line-height: 42rpx;
  font-weight: 800;
  color: #f6f2e8;
}

.popup-close {
  font-size: 34rpx;
  color: #8f887e;
}

.phone-form {
  padding: 0;
}

.form-item {
  margin-bottom: 20rpx;
}

.phone-input,
.captcha-input {
  width: 100%;
  height: 88rpx;
  background: #202024;
  border-radius: 14rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #f6f2e8;
  box-sizing: border-box;
}

.captcha-item {
  display: flex;
  gap: 16rpx;
}

.captcha-input {
  flex: 1;
}

.captcha-btn {
  width: 200rpx;
  height: 88rpx;
  border-radius: 14rpx;
  color: #16130b;
  background: #c9a227;
  font-size: 25rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
}

.captcha-btn[disabled],
.btn-submit[disabled] {
  background: #343436;
  color: #777166;
}

.btn-submit {
  height: 90rpx;
  margin-top: 14rpx;
  border-radius: 45rpx;
  background: #c9a227;
  color: #16130b;
  font-size: 30rpx;
  font-weight: 800;
}

.wechat-tip {
  margin-top: 22rpx;
  padding: 18rpx;
  border-radius: 12rpx;
  background: rgba(255, 255, 255, 0.045);
}

.wechat-tip text {
  font-size: 22rpx;
  line-height: 32rpx;
  color: #8f887e;
  text-align: center;
  display: block;
}
</style>
