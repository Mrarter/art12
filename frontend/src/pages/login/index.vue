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

    <!-- Tab 切换栏 -->
    <view class="tab-bar">
      <view
        class="tab-item"
        :class="{ active: activeTab === 'login' }"
        @click="switchTab('login')"
      >登录</view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'register' }"
        @click="switchTab('register')"
      >注册</view>
      <view class="tab-indicator" :style="{ left: indicatorLeft }"></view>
    </view>

    <!-- 表单容器 -->
    <view class="form-container">
      <!-- 登录表单 -->
      <view class="form-panel" :class="{ active: activeTab === 'login' }">
        <view class="form-title">账号登录</view>

        <view class="login-mode-switch">
          <view
            class="mode-option"
            :class="{ active: loginMode === 'password' }"
            @click="switchLoginMode('password')"
          >密码登录</view>
          <view
            class="mode-option"
            :class="{ active: loginMode === 'sms' }"
            @click="switchLoginMode('sms')"
          >验证码登录</view>
        </view>

        <view class="form-item" :class="{ error: loginErrors.phone }">
          <view class="input-wrapper">
            <text class="input-icon">📱</text>
            <input
              class="form-input"
              type="number"
              v-model="loginForm.phone"
              placeholder="请输入手机号"
              maxlength="11"
              @input="clearError('login', 'phone')"
            />
          </view>
          <text class="error-text" v-if="loginErrors.phone">{{ loginErrors.phone }}</text>
        </view>

        <view v-if="loginMode === 'password'" class="form-item" :class="{ error: loginErrors.password }">
          <view class="input-wrapper">
            <text class="input-icon">🔒</text>
            <input
              class="form-input"
              :password="!showLoginPassword"
              v-model="loginForm.password"
              placeholder="请输入登录密码"
              maxlength="32"
              @input="clearError('login', 'password')"
            />
            <text class="password-toggle" @click="showLoginPassword = !showLoginPassword">
              {{ showLoginPassword ? '隐藏' : '显示' }}
            </text>
          </view>
          <text class="error-text" v-if="loginErrors.password">{{ loginErrors.password }}</text>
        </view>

        <view v-else class="form-item captcha-item" :class="{ error: loginErrors.captcha }">
          <view class="input-wrapper captcha-wrapper">
            <text class="input-icon">🔢</text>
            <input
              class="form-input captcha-input"
              type="number"
              v-model="loginForm.captcha"
              placeholder="验证码"
              maxlength="6"
              @input="clearError('login', 'captcha')"
            />
            <button
              class="captcha-btn"
              :class="{ disabled: !canSendLoginCaptcha || loginCountdown > 0 }"
              :disabled="!canSendLoginCaptcha || loginCountdown > 0"
              @click="sendLoginCaptcha"
            >{{ loginCountdown > 0 ? `${loginCountdown}s` : '获取验证码' }}</button>
          </view>
          <text class="error-text" v-if="loginErrors.captcha">{{ loginErrors.captcha }}</text>
        </view>

        <button
          class="submit-btn"
          :class="{ disabled: !canLogin }"
          :loading="loginLoading"
          :disabled="!canLogin"
          @click="handleLogin"
        >登录</button>
      </view>

      <!-- 注册表单 -->
      <view class="form-panel" :class="{ active: activeTab === 'register' }">
        <view class="form-title">新用户注册</view>

        <view class="form-item" :class="{ error: registerErrors.nickname }">
          <view class="input-wrapper">
            <text class="input-icon">👤</text>
            <input
              class="form-input"
              type="text"
              v-model="registerForm.nickname"
              placeholder="请输入昵称（2-20字符）"
              maxlength="20"
              @input="clearError('register', 'nickname')"
            />
          </view>
          <text class="error-text" v-if="registerErrors.nickname">{{ registerErrors.nickname }}</text>
        </view>

        <view class="form-item" :class="{ error: registerErrors.phone }">
          <view class="input-wrapper">
            <text class="input-icon">📱</text>
            <input
              class="form-input"
              type="number"
              v-model="registerForm.phone"
              placeholder="请输入手机号"
              maxlength="11"
              @input="clearError('register', 'phone')"
            />
          </view>
          <text class="error-text" v-if="registerErrors.phone">{{ registerErrors.phone }}</text>
        </view>

        <view class="form-item" :class="{ error: registerErrors.password }">
          <view class="input-wrapper">
            <text class="input-icon">🔒</text>
            <input
              class="form-input"
              :password="!showRegisterPassword"
              v-model="registerForm.password"
              placeholder="设置登录密码（至少6位）"
              maxlength="32"
              @input="clearError('register', 'password')"
            />
            <text class="password-toggle" @click="showRegisterPassword = !showRegisterPassword">
              {{ showRegisterPassword ? '隐藏' : '显示' }}
            </text>
          </view>
          <text class="error-text" v-if="registerErrors.password">{{ registerErrors.password }}</text>
        </view>

        <view class="form-item captcha-item" :class="{ error: registerErrors.captcha }">
          <view class="input-wrapper captcha-wrapper">
            <text class="input-icon">🔢</text>
            <input
              class="form-input captcha-input"
              type="number"
              v-model="registerForm.captcha"
              placeholder="验证码"
              maxlength="6"
              @input="clearError('register', 'captcha')"
            />
            <button
              class="captcha-btn"
              :class="{ disabled: !canSendRegisterCaptcha || registerCountdown > 0 }"
              :disabled="!canSendRegisterCaptcha || registerCountdown > 0"
              @click="sendRegisterCaptcha"
            >{{ registerCountdown > 0 ? `${registerCountdown}s` : '获取验证码' }}</button>
          </view>
          <text class="error-text" v-if="registerErrors.captcha">{{ registerErrors.captcha }}</text>
        </view>

        <button
          class="submit-btn"
          :class="{ disabled: !canRegister }"
          :loading="registerLoading"
          :disabled="!canRegister"
          @click="handleRegister"
        >注册</button>
      </view>
    </view>

    <view class="divider-text">
      <view class="divider-line"></view>
      <text class="divider-label">其他方式</text>
      <view class="divider-line"></view>
    </view>

    <view class="third-party-section">
      <button class="btn-wechat" @click="onWechatLogin" :loading="wechatLoading">
        <text class="btn-icon">💬</text>
        <text>微信登录</text>
      </button>

      <button class="btn-guest" @click="onGuestLogin">
        <text>游客体验</text>
      </button>
    </view>

    <view class="agreement-footer">
      <text>登录即表示同意</text>
      <text class="link" @click="viewAgreement('user')">《用户协议》</text>
      <text>和</text>
      <text class="link" @click="viewAgreement('privacy')">《隐私政策》</text>
    </view>
  </view>
</template>

<script>
import { wxLogin, phoneLogin, passwordLogin, register, sendSmsCode } from '@/api/user'
import { useUserStore } from '@/store/modules/user'
import { getAndClearRedirectUrl } from '@/utils/auth'
import loginBrandLogo from '@/static/logo.png'

const TAB_BAR_PAGES = new Set([
  '/pages/index/index',
  '/pages/gallery/index',
  '/pages/auction/index',
  '/pages/cart/index',
  '/pages/user/index'
])

export default {
  data() {
    return {
      activeTab: 'login', // 'login' | 'register'
      loginBrandLogo,
      loginMode: 'password',
      showLoginPassword: false,
      showRegisterPassword: false,
      loginLoading: false,
      registerLoading: false,
      wechatLoading: false,
      loginCountdown: 0,
      registerCountdown: 0,
      loginForm: {
        phone: '',
        captcha: '',
        password: ''
      },
      registerForm: {
        nickname: '',
        phone: '',
        captcha: '',
        password: ''
      },
      loginErrors: {
        phone: '',
        captcha: '',
        password: ''
      },
      registerErrors: {
        nickname: '',
        phone: '',
        captcha: '',
        password: ''
      }
    }
  },

  computed: {
    indicatorLeft() {
      return this.activeTab === 'login' ? '0%' : '50%'
    },

    canSendLoginCaptcha() {
      return /^1[3-9]\d{9}$/.test(this.loginForm.phone)
    },

    canSendRegisterCaptcha() {
      return /^1[3-9]\d{9}$/.test(this.registerForm.phone)
    },

    canLogin() {
      if (!/^1[3-9]\d{9}$/.test(this.loginForm.phone)) return false
      if (this.loginMode === 'password') {
        return this.loginForm.password.length >= 6
      }
      return /^\d{6}$/.test(this.loginForm.captcha)
    },

    canRegister() {
      const nickname = this.registerForm.nickname.trim()
      return nickname.length >= 2 && nickname.length <= 20 &&
             /^1[3-9]\d{9}$/.test(this.registerForm.phone) &&
             this.registerForm.password.length >= 6 &&
             /^\d{6}$/.test(this.registerForm.captcha)
    }
  },

  onLoad(options = {}) {
    this.initLogin(options)
  },

  methods: {
    // ============ Tab 切换 ===========
    switchTab(tab) {
      if (this.activeTab === tab) return
      this.activeTab = tab
      // 切换时清除错误状态
      this.clearAllErrors()
    },

    switchLoginMode(mode) {
      if (this.loginMode === mode) return
      this.loginMode = mode
      this.clearAllErrors()
    },

    // ============ 错误处理 ===========
    clearError(form, field) {
      this[`${form}Errors`][field] = ''
    },

    clearAllErrors() {
      this.loginErrors = { phone: '', captcha: '', password: '' }
      this.registerErrors = { nickname: '', phone: '', captcha: '', password: '' }
    },

    setError(form, field, message) {
      this[`${form}Errors`][field] = message
    },

    // ============ 登录 ===========
    async handleLogin() {
      if (!this.canLogin || this.loginLoading) return

      // 表单验证
      if (!/^1[3-9]\d{9}$/.test(this.loginForm.phone)) {
        this.setError('login', 'phone', '请输入正确的手机号')
        return
      }
      if (this.loginMode === 'password' && this.loginForm.password.length < 6) {
        this.setError('login', 'password', '请输入至少6位密码')
        return
      }
      if (this.loginMode === 'sms' && !/^\d{6}$/.test(this.loginForm.captcha)) {
        this.setError('login', 'captcha', '请输入6位验证码')
        return
      }

      this.loginLoading = true
      uni.showLoading({ title: '登录中...', mask: true })

      try {
        const data = this.loginMode === 'password'
          ? await passwordLogin({
              phone: this.loginForm.phone,
              password: this.loginForm.password
            })
          : await phoneLogin({
              phone: this.loginForm.phone,
              code: this.loginForm.captcha
            })

        const userStore = useUserStore()
        const userInfo = this.buildLoginUserInfo(data)
        userStore.onLoginSuccess(data.token, userInfo)

        uni.hideLoading()
        uni.showToast({ title: '登录成功', icon: 'success' })

        setTimeout(() => this.afterLogin(), 1500)
      } catch (e) {
        uni.hideLoading()
        this.handleRequestError(e, '登录失败')
      } finally {
        this.loginLoading = false
      }
    },

    // ============ 注册 ===========
    async handleRegister() {
      if (!this.canRegister || this.registerLoading) return

      // 表单验证
      const nickname = this.registerForm.nickname.trim()
      if (nickname.length < 2 || nickname.length > 20) {
        this.setError('register', 'nickname', '昵称需要2-20个字符')
        return
      }
      if (!/^1[3-9]\d{9}$/.test(this.registerForm.phone)) {
        this.setError('register', 'phone', '请输入正确的手机号')
        return
      }
      if (this.registerForm.password.length < 6) {
        this.setError('register', 'password', '请设置至少6位密码')
        return
      }

      this.registerLoading = true
      uni.showLoading({ title: '注册中...', mask: true })

      try {
        const data = await register({
          phone: this.registerForm.phone,
          code: this.registerForm.captcha,
          nickname: nickname,
          password: this.registerForm.password
        })

        const userStore = useUserStore()
        const userInfo = this.buildRegisterUserInfo(data)
        userStore.onLoginSuccess(data.token, userInfo)

        uni.hideLoading()
        uni.showToast({ title: '注册成功', icon: 'success' })

        setTimeout(() => this.afterLogin(), 1500)
      } catch (e) {
        uni.hideLoading()
        this.handleRequestError(e, '注册失败')
      } finally {
        this.registerLoading = false
      }
    },

    // ============ 发送验证码 ===========
    async sendLoginCaptcha() {
      if (!this.canSendLoginCaptcha || this.loginCountdown > 0) return

      try {
        const result = await sendSmsCode(this.loginForm.phone, 'login')
        if (result?.mock) {
          uni.showToast({ title: `测试验证码 ${result.code}`, icon: 'none', duration: 2500 })
        } else {
          uni.showToast({ title: '验证码已发送', icon: 'success' })
        }
        this.startCountdown('login')
      } catch (e) {
        this.handleRequestError(e, '发送失败')
      }
    },

    async sendRegisterCaptcha() {
      if (!this.canSendRegisterCaptcha || this.registerCountdown > 0) return

      try {
        const result = await sendSmsCode(this.registerForm.phone, 'register')
        if (result?.mock) {
          uni.showToast({ title: `测试验证码 ${result.code}`, icon: 'none', duration: 2500 })
        } else {
          uni.showToast({ title: '验证码已发送', icon: 'success' })
        }
        this.startCountdown('register')
      } catch (e) {
        this.handleRequestError(e, '发送失败')
      }
    },

    startCountdown(type) {
      const key = `${type}Countdown`
      this[key] = 60
      const timer = setInterval(() => {
        this[key]--
        if (this[key] <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    },

    // ============ 微信登录 ===========
    async onWechatLogin() {
      if (this.wechatLoading) return
      this.wechatLoading = true

      try {
        const profile = await this.resolveWechatProfile()
        const { code } = await this.resolveWechatLoginCode()

        const data = await wxLogin({
          code,
          ...profile
        })

        const userStore = useUserStore()
        const userInfo = this.buildLoginUserInfo(data, profile)
        userStore.onLoginSuccess(data.token, userInfo)
        userStore.setOpenId(data.openId || '')

        uni.showToast({
          title: data.phone ? '微信登录成功' : '已同步微信头像昵称',
          icon: 'success'
        })

        setTimeout(() => this.afterLogin(), 1500)
      } catch (e) {
        this.handleRequestError(e, '微信登录失败')
      } finally {
        this.wechatLoading = false
      }
    },

    // ============ 游客登录 ===========
    onGuestLogin() {
      const userStore = useUserStore()
      const guestInfo = {
        id: 0,
        nickname: '游客',
        avatar: '',
        isGuest: true,
        currentIdentity: 'collector'
      }
      userStore.onLoginSuccess('guest_token', guestInfo)
      uni.showToast({ title: '已进入游客模式', icon: 'success' })
      setTimeout(() => this.afterLogin(), 1500)
    },

    // ============ 登录后处理 ===========
    afterLogin() {
      if (this.redirect) {
        this.safeNavigate(this.redirect)
        return
      }

      const savedRedirect = getAndClearRedirectUrl()
      if (savedRedirect && savedRedirect !== '/pages/index/index') {
        this.safeNavigate(savedRedirect)
        return
      }

      uni.switchTab({ url: '/pages/index/index' })
    },

    safeNavigate(url) {
      const purePath = url.split('?')[0]
      if (TAB_BAR_PAGES.has(purePath)) {
        uni.switchTab({ url: purePath })
      } else {
        uni.navigateTo({ url })
      }
    },

    // ============ 工具方法 ===========
    initLogin(options = {}) {
      this.redirect = this.decodeRedirect(options.redirect || '')
      const userStore = useUserStore()
      if (userStore.isLogin) {
        this.afterLogin()
      }
    },

    decodeRedirect(value) {
      let text = value || ''
      for (let i = 0; i < 2; i++) {
        try {
          const decoded = decodeURIComponent(text)
          if (decoded === text) break
          text = decoded
        } catch (e) {
          break
        }
      }
      return text
    },

    resolveWechatLoginCode() {
      return new Promise((resolve, reject) => {
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
            if (typeof window !== 'undefined') {
              resolve({ code: `h5_dev_${Date.now()}` })
            } else {
              reject(err || new Error('微信登录不可用'))
            }
          }
        })
      })
    },

    resolveWechatProfile() {
      if (typeof wx === 'undefined' || typeof wx.getUserProfile !== 'function') {
        return Promise.resolve({
          nickname: '微信用户',
          avatar: loginBrandLogo,
          gender: 0,
          region: '本地调试'
        })
      }

      return new Promise((resolve, reject) => {
        wx.getUserProfile({
          desc: '用于完善头像、昵称与身份资料',
          success: (res) => {
            const userInfo = res?.userInfo || {}
            resolve({
              nickname: userInfo.nickName || '微信用户',
              avatar: userInfo.avatarUrl || '',
              gender: typeof userInfo.gender === 'number' ? userInfo.gender : 0,
              region: [userInfo.country, userInfo.province, userInfo.city].filter(Boolean).join(' ')
            })
          },
          fail: () => {
            if (typeof window !== 'undefined') {
              resolve({
                nickname: '微信用户',
                avatar: loginBrandLogo,
                gender: 0,
                region: '本地调试'
              })
            } else {
              reject(new Error('未完成微信头像授权'))
            }
          }
        })
      })
    },

    buildLoginUserInfo(data, profile = {}) {
      const identities = this.normalizeIdentityList(data.identities)
      return {
        id: data.userId,
        userId: data.userId,
        uid: data.uid || '',
        nickname: data.nickname || profile.nickname || '微信用户',
        avatar: data.avatar || profile.avatar || loginBrandLogo,
        phone: data.phone || '',
        identities,
        openId: data.openId || '',
        currentIdentity: identities[0] || 'collector'
      }
    },

    buildRegisterUserInfo(data) {
      const source = data.userInfo || data || {}
      const identities = this.normalizeIdentityList(source.identities || source.identity)
      return {
        ...source,
        id: source.id || source.userId || data.userId || 0,
        userId: source.userId || source.id || data.userId || 0,
        uid: source.uid || data.uid || '',
        nickname: source.nickname || source.nickName || this.registerForm.nickname,
        avatar: source.avatar || loginBrandLogo,
        phone: source.phone || this.registerForm.phone,
        identities,
        currentIdentity: identities[0] || 'collector'
      }
    },

    normalizeIdentityList(rawValue) {
      if (Array.isArray(rawValue)) return rawValue
      if (typeof rawValue === 'string' && rawValue.trim()) {
        return rawValue.split(',').map(item => item.trim()).filter(Boolean)
      }
      return ['collector']
    },

    handleRequestError(e, defaultMsg) {
      const errMsg = e?.message || defaultMsg
      console.error('[Login]', errMsg, e)

      // 特殊处理：手机号未注册
      if (errMsg.includes('未注册')) {
        uni.showModal({
          title: '提示',
          content: '该手机号未注册，是否立即注册？',
          confirmText: '去注册',
          success: (res) => {
            if (res.confirm) {
              this.activeTab = 'register'
              this.registerForm.phone = this.loginForm.phone
            }
          }
        })
        return
      }

      // 验证码错误
      if (errMsg.includes('验证码')) {
        if (this.activeTab === 'login') {
          this.setError('login', 'captcha', errMsg)
        } else {
          this.setError('register', 'captcha', errMsg)
        }
        return
      }

      if (errMsg.includes('密码')) {
        if (this.activeTab === 'login') {
          this.setError('login', 'password', errMsg)
        } else {
          this.setError('register', 'password', errMsg)
        }
        return
      }

      // 通用错误提示
      uni.showToast({
        title: errMsg.substring(0, 50),
        icon: 'none',
        duration: 3000
      })
    },

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
.tab-bar,
.form-container,
.third-party-section,
.agreement-footer {
  position: relative;
  z-index: 1;
}

/* ============ 品牌区域 =========== */
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

/* ============ Tab 栏 =========== */
.tab-bar {
  display: flex;
  position: relative;
  margin-bottom: 32rpx;
  border-bottom: 2rpx solid rgba(255, 255, 255, 0.08);
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  font-size: 32rpx;
  font-weight: 700;
  color: #777166;
  transition: color 0.3s ease;
  position: relative;
  z-index: 1;
}

.tab-item.active {
  color: #f6f2e8;
}

.tab-indicator {
  position: absolute;
  bottom: -2rpx;
  left: 0;
  width: 50%;
  height: 4rpx;
  background: #c9a227;
  border-radius: 2rpx;
  transition: left 0.3s ease;
}

/* ============ 表单容器 =========== */
.form-container {
  position: relative;
  min-height: 560rpx;
}

.form-panel {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  opacity: 0;
  transform: translateY(20rpx);
  transition: opacity 0.3s ease, transform 0.3s ease;
  pointer-events: none;
}

.form-panel.active {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.form-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #f6f2e8;
  margin-bottom: 18rpx;
}

.login-mode-switch {
  display: flex;
  height: 68rpx;
  padding: 6rpx;
  margin-bottom: 24rpx;
  border-radius: 14rpx;
  background: rgba(255, 255, 255, 0.055);
  border: 1rpx solid rgba(255, 255, 255, 0.09);
  box-sizing: border-box;
}

.mode-option {
  flex: 1;
  height: 100%;
  border-radius: 10rpx;
  color: #8f887e;
  font-size: 25rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mode-option.active {
  color: #16130b;
  background: #c9a227;
}

/* ============ 表单项 =========== */
.form-item {
  margin-bottom: 24rpx;
  transition: transform 0.2s ease;
}

.form-item:focus-within {
  transform: scale(1.02);
}

.form-item.error .input-wrapper {
  border-color: #ff6b6b;
}

.input-wrapper {
  display: flex;
  align-items: center;
  height: 88rpx;
  padding: 0 24rpx;
  background: #202024;
  border-radius: 14rpx;
  border: 2rpx solid transparent;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.input-wrapper:focus-within {
  border-color: #c9a227;
  box-shadow: 0 0 0 4rpx rgba(201, 162, 39, 0.15);
}

.input-icon {
  font-size: 32rpx;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.form-input {
  flex: 1;
  height: 100%;
  background: transparent;
  color: #f6f2e8;
  font-size: 28rpx;
}

.password-toggle {
  padding-left: 18rpx;
  color: #c9a227;
  font-size: 24rpx;
  line-height: 32rpx;
  flex-shrink: 0;
}

.captcha-wrapper {
  gap: 16rpx;
}

.captcha-input {
  flex: 1;
}

.captcha-btn {
  width: 200rpx;
  height: 64rpx;
  border-radius: 10rpx;
  background: #c9a227;
  color: #16130b;
  font-size: 24rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: none;
  transition: background 0.2s ease, opacity 0.2s ease;
}

.captcha-btn::after {
  border: none;
}

.captcha-btn.disabled,
.captcha-btn[disabled] {
  background: #343436;
  color: #777166;
}

/* ============ 错误提示 =========== */
.error-text {
  display: block;
  margin-top: 8rpx;
  padding-left: 24rpx;
  font-size: 22rpx;
  color: #ff6b6b;
  line-height: 32rpx;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ============ 提交按钮 =========== */
.submit-btn {
  width: 100%;
  height: 92rpx;
  border-radius: 46rpx;
  background: #c9a227;
  color: #16130b;
  font-size: 30rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  transition: transform 0.2s ease, opacity 0.2s ease, box-shadow 0.2s ease;
  margin-top: 32rpx;
}

.submit-btn::after {
  border: none;
}

.submit-btn[loading] {
  opacity: 0.8;
}

.submit-btn.disabled,
.submit-btn[disabled] {
  opacity: 0.5;
}

.submit-btn:active:not([disabled]) {
  transform: scale(0.98);
  box-shadow: 0 8rpx 24rpx rgba(201, 162, 39, 0.3);
}

/* ============ 分隔线 =========== */
.divider-text {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin: 40rpx 0 32rpx;
}

.divider-line {
  flex: 1;
  height: 1rpx;
  background: rgba(255, 255, 255, 0.08);
}

.divider-label {
  font-size: 22rpx;
  color: #777166;
  flex-shrink: 0;
}

/* ============ 第三方登录 =========== */
.third-party-section {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.btn-wechat,
.btn-guest {
  width: 100%;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
}

.btn-wechat::after,
.btn-guest::after {
  border: none;
}

.btn-wechat {
  height: 90rpx;
  border-radius: 45rpx;
  color: #f6f2e8;
  font-size: 29rpx;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.055);
  border: 1rpx solid rgba(255, 255, 255, 0.16);
  transition: background 0.2s ease, transform 0.2s ease;
}

.btn-wechat:active {
  transform: scale(0.98);
  background: rgba(255, 255, 255, 0.1);
}

.btn-icon {
  font-size: 36rpx;
}

.btn-guest {
  height: 72rpx;
  margin-top: 8rpx;
  color: #8f887e;
  font-size: 24rpx;
  background: transparent;
  transition: opacity 0.2s ease;
}

.btn-guest:active {
  opacity: 0.7;
}

/* ============ 协议声明 =========== */
.agreement-footer {
  margin-top: auto;
  padding-top: 40rpx;
  text-align: center;
}

.agreement-footer text {
  font-size: 22rpx;
  color: #8f887e;
}

.link {
  color: #c9a227;
}

/* ============ 响应式适配 =========== */
@media screen and (max-width: 375px) {
  .brand-section {
    min-height: 200rpx;
  }
  .app-name {
    font-size: 42rpx;
  }
}

@media screen and (min-width: 428px) {
  .brand-section {
    min-height: 350rpx;
  }
}

@media screen and (min-width: 768px) {
  .login-page {
    padding: 64rpx 100rpx;
  }
  .form-container {
    max-width: 500rpx;
    margin: 0 auto;
  }
}
</style>
