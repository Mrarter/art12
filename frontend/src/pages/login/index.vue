<template>
  <view class="login-page">
    <image class="hero-bg" src="/static/images/museum-v12-hero-bg.png" mode="aspectFill"></image>
    <view class="page-shade"></view>

    <view class="brand-section">
      <view class="brand-mark">
        <image class="brand-logo" :src="loginBrandLogo" mode="aspectFit"></image>
      </view>
      <view class="brand-copy">
        <text class="app-name">艺本艺术</text>
        <text class="app-slogan">让创作永远有价值。</text>
      </view>
    </view>

    <view class="login-card">
      <view class="card-heading">
        <view>
          <text class="card-title">{{ loginMode === 'password' ? '账号密码登录' : '手机号验证码登录' }}</text>
        </view>
        <text class="card-switch" @click="switchLoginMode(loginMode === 'password' ? 'sms' : 'password')">
          {{ loginMode === 'password' ? '验证码登录' : '账号密码登录' }}
        </text>
      </view>

      <view class="form-body">
        <view class="field-row" :class="{ error: loginErrors.phone }">
          <view class="country-code">
            <text>+86</text>
            <text class="country-arrow">⌄</text>
          </view>
          <input
            class="field-input"
            type="number"
            v-model="loginForm.phone"
            placeholder="请输入手机号"
            placeholder-class="field-placeholder"
            maxlength="11"
            @input="clearError('login', 'phone')"
          />
        </view>
        <text class="error-text" v-if="loginErrors.phone">{{ loginErrors.phone }}</text>

        <view v-if="loginMode === 'sms'" class="field-row" :class="{ error: loginErrors.captcha }">
          <input
            class="field-input"
            type="number"
            v-model="loginForm.captcha"
            placeholder="请输入验证码"
            placeholder-class="field-placeholder"
            maxlength="6"
            @input="clearError('login', 'captcha')"
          />
          <button
            class="code-action"
            :class="{ disabled: !canSendLoginCaptcha || loginCountdown > 0 }"
            :disabled="!canSendLoginCaptcha || loginCountdown > 0"
            @click="sendLoginCaptcha"
          >{{ loginCountdown > 0 ? `${loginCountdown}s` : '获取验证码' }}</button>
        </view>

        <view v-else class="field-row" :class="{ error: loginErrors.password }">
          <input
            class="field-input"
            :password="!showLoginPassword"
            v-model="loginForm.password"
            placeholder="请输入登录密码"
            placeholder-class="field-placeholder"
            maxlength="32"
            @input="clearError('login', 'password')"
          />
          <text class="password-toggle" @click="showLoginPassword = !showLoginPassword">
            {{ showLoginPassword ? '隐藏' : '显示' }}
          </text>
        </view>
        <text class="error-text" v-if="loginMode === 'sms' && loginErrors.captcha">{{ loginErrors.captcha }}</text>
        <text class="error-text" v-if="loginMode === 'password' && loginErrors.password">{{ loginErrors.password }}</text>

        <text class="help-text">收不到验证码？请发邮件至官方邮箱 info@art1.cn</text>

        <button
          class="primary-login"
          :class="{ disabled: !canLogin }"
          :loading="loginLoading"
          :disabled="!canLogin"
          @click="handleLogin"
        >登录</button>

        <button
          class="local-phone-login"
          :open-type="localPhoneButtonOpenType"
          @click="onLocalPhoneLogin"
          @getphonenumber="onMiniLocalPhoneLogin"
        >本机号码一键登录</button>

        <view class="agreement-row" @click="toggleAgreementAccepted">
          <text :class="['agreement-check', { checked: agreementAccepted }]">{{ agreementAccepted ? '✓' : '' }}</text>
          <text class="agreement-copy">登录代表您已阅读并同意</text>
          <text class="agreement-link" @click.stop.prevent="viewAgreement('user')">用户协议</text>
          <text class="agreement-copy">和</text>
          <text class="agreement-link" @click.stop.prevent="viewAgreement('privacy')">隐私条款</text>
        </view>
      </view>

      <view class="quick-login">
        <view class="quick-line"></view>
        <text class="quick-title">快捷登录</text>
        <view class="quick-line"></view>
      </view>
      <button
        class="wechat-login"
        @click="onWechatLogin"
        :loading="wechatLoading"
        :open-type="miniWechatButtonOpenType"
        @getphonenumber="onMiniWechatPhoneLogin"
      >
        <image class="wechat-icon" src="/static/share-icons/wechat.svg" mode="aspectFit"></image>
        <text>{{ wechatLoginLabel }}</text>
      </button>
    </view>
  </view>
</template>

<script>
import { wxLogin, phoneLogin, passwordLogin, register, sendSmsCode } from '@/api/user'
import { useUserStore } from '@/store/modules/user'
import { getAndClearRedirectUrl, saveRedirectUrl } from '@/utils/auth'
import { AUCTION_ENABLED } from '@/utils/platform.js'
import { hasNativeWechatLoginBridge, requestNativeWechatLogin } from '@/utils/native'
import loginBrandLogo from '@/static/logo.png'

const TAB_BAR_PAGES = new Set([
  '/pages/index/index',
  '/pages/gallery/index',
  '/pages/cart/index',
  '/pages/user/index'
])
if (AUCTION_ENABLED) {
  TAB_BAR_PAGES.add('/pages/auction/index')
}
const IS_MP_WEIXIN = process.env.UNI_PLATFORM === 'mp-weixin'
const H5_WECHAT_OFFICIAL_APP_ID = import.meta.env?.VITE_WECHAT_OFFICIAL_APP_ID || 'wx02fc79a8dd2d9f20'
const H5_WECHAT_OAUTH_CALLBACK_PATH = '/'
const REGISTER_SMS_REQUIRED = true

export default {
  data() {
    return {
      activeTab: 'login', // 'login' | 'register'
      loginBrandLogo,
      loginMode: 'sms',
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
      registerSmsRequired: REGISTER_SMS_REQUIRED,
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
      },
      agreementAccepted: false,
      isH5Wechat: false,
      isIOSAppWebView: false,
      isNativeWechatBridgeReady: false,
      oauthCode: '',
      oauthHandled: false
    }
  },

  computed: {
    wechatLoginSupported() {
      return IS_MP_WEIXIN || this.isH5Wechat || this.isNativeWechatBridgeReady
    },

    wechatLoginLabel() {
      if (IS_MP_WEIXIN) return '快捷登录'
      if (this.isH5Wechat) return '微信授权登录'
      if (this.isNativeWechatBridgeReady) return '微信登录'
      return '微信登录'
    },

    miniWechatButtonOpenType() {
      return IS_MP_WEIXIN ? 'getPhoneNumber' : ''
    },

    localPhoneButtonOpenType() {
      return IS_MP_WEIXIN ? 'getPhoneNumber' : ''
    },

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
             (!this.registerSmsRequired || /^\d{6}$/.test(this.registerForm.captcha))
    }
  },

  onLoad(options = {}) {
    this.initLogin(options)
  },

  methods: {
    loadAgreementAccepted() {
      this.agreementAccepted = false
      uni.removeStorageSync('login_agreement_accepted')
    },

    persistAgreementAccepted() {
      this.agreementAccepted = !!this.agreementAccepted
    },

    toggleAgreementAccepted() {
      this.agreementAccepted = !this.agreementAccepted
      this.persistAgreementAccepted()
    },

    ensureAgreementAccepted() {
      if (this.agreementAccepted) return true

      uni.showToast({
        title: '请先勾选登录协议',
        icon: 'none'
      })
      return false
    },

    goBack() {
      const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
      if (pages.length > 1) {
        uni.navigateBack()
        return
      }
      uni.switchTab({ url: '/pages/index/index' })
    },

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

    onLocalPhoneLogin() {
      if (IS_MP_WEIXIN) return
      uni.showToast({
        title: '本机号码一键登录仅支持微信小程序',
        icon: 'none'
      })
    },

    onMiniLocalPhoneLogin(event) {
      if (!IS_MP_WEIXIN) return
      this.onMiniWechatPhoneLogin(event, true)
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
      if (!(await this.ensureAgreementAccepted())) return

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
      if (!(await this.ensureAgreementAccepted())) return

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
          code: this.registerSmsRequired ? this.registerForm.captcha : '',
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
      if (!(await this.ensureAgreementAccepted())) return

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
      if (!(await this.ensureAgreementAccepted())) return

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
      if (IS_MP_WEIXIN) return
      if (this.wechatLoading) return
      if (!(await this.ensureAgreementAccepted())) return

      if (!this.wechatLoginSupported) {
        uni.showModal({
          title: '当前页面不支持微信授权',
          content: '当前浏览器无法直接拉起微信授权，请使用手机号登录，或在微信内打开后使用微信授权登录。',
          showCancel: false
        })
        return
      }

      this.wechatLoading = true

      try {
        if (!IS_MP_WEIXIN && this.isH5Wechat) {
          this.startOfficialWechatOauth()
          return
        }

        if (this.isIOSAppWebView && !this.isNativeWechatBridgeReady) {
          uni.showModal({
            title: 'APP 暂未接入原生微信登录',
            content: '当前 APP 容器内不能直接走微信网页授权，所以会被微信拦截。请先使用手机号登录，或等我们接入原生微信 SDK 后再启用。',
            showCancel: false
          })
          return
        }

        if (this.isNativeWechatBridgeReady) {
          const { code, loginScene } = await requestNativeWechatLogin({ source: 'login' })
          const data = await wxLogin({
            code,
            loginScene: loginScene || 'app'
          })
          this.finishWechatLogin(data, {}, '微信登录成功')
          return
        }

        const profile = await this.resolveWechatProfileOrFallback()
        const { code } = await this.resolveWechatLoginCode()

        const data = await wxLogin({
          code,
          loginScene: 'mini',
          ...profile
        })
        this.finishWechatLogin(data, profile, data.phone ? '微信登录成功' : '已同步微信头像昵称')
      } catch (e) {
        this.handleRequestError(e, '微信登录失败')
      } finally {
        this.wechatLoading = false
      }
    },

    async onMiniWechatPhoneLogin(event, requirePhone = false) {
      if (!IS_MP_WEIXIN) return
      if (this.wechatLoading) return
      if (!(await this.ensureAgreementAccepted())) return

      const phoneCode = event?.detail?.code || ''
      const errMsg = event?.detail?.errMsg || ''

      this.wechatLoading = true
      try {
        const profile = await this.resolveWechatProfileOrFallback()
        const { code } = await this.resolveWechatLoginCode()

        const payload = {
          code,
          loginScene: 'mini',
          ...profile
        }

        if (phoneCode) {
          payload.phoneCode = phoneCode
        } else {
          if (requirePhone) {
            throw new Error('未完成手机号授权')
          }
          console.warn('[Login] 微信手机号授权未完成，改为普通微信登录', errMsg)
        }

        const data = await wxLogin(payload)
        if (requirePhone && !data.phone) {
          throw new Error('微信手机号授权失败，请重试')
        }
        const successTitle = phoneCode
          ? (data.phone ? '微信一键登录成功' : '已登录，手机号待补充')
          : '微信登录成功'
        this.finishWechatLogin(data, profile, successTitle)
      } catch (e) {
        this.handleRequestError(e, '微信一键登录失败')
      } finally {
        this.wechatLoading = false
      }
    },

    // ============ 游客登录 ===========
    async onGuestLogin() {
      if (!(await this.ensureAgreementAccepted())) return
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
      setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 1500)
    },

    // ============ 登录后处理 ===========
    finishWechatLogin(data, profile = {}, successTitle = '微信登录成功') {
      const userStore = useUserStore()
      const userInfo = this.buildLoginUserInfo(data, profile)
      userStore.onLoginSuccess(data.token, userInfo)
      userStore.setOpenId(data.openId || '')

      uni.showToast({ title: successTitle, icon: 'success' })

      if (data.phone) {
        setTimeout(() => this.afterLogin(), 1500)
        return
      }

      setTimeout(() => {
        uni.showModal({
          title: '补充手机号',
          content: '当前账号还没有绑定手机号。为方便下单、收货与账号找回，建议现在完成手机号绑定。',
          confirmText: '去绑定',
          cancelText: '稍后再说',
          success: (res) => {
            if (res.confirm) {
              uni.navigateTo({ url: '/pages/user/account-security?tab=phone' })
              return
            }
            this.afterLogin()
          },
          fail: () => this.afterLogin()
        })
      }, 1200)
    },

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
      this.loadAgreementAccepted()
      this.syncH5WechatContext()
      this.captureOauthCode(options)
      const userStore = useUserStore()
      if (this.shouldHandleOauthCallback()) {
        this.handleOfficialWechatLogin()
        return
      }
      if (userStore.isLogin) {
        this.afterLogin()
      }
    },

    syncH5WechatContext() {
      if (typeof window === 'undefined') return
      const ua = window.navigator?.userAgent || ''
      this.isH5Wechat = /MicroMessenger/i.test(ua) && !/miniProgram/i.test(ua)
      this.isIOSAppWebView = /YibenArt/i.test(ua)
      this.isNativeWechatBridgeReady = hasNativeWechatLoginBridge()
    },

    captureOauthCode(options = {}) {
      const query = this.readRouteOptions()
      this.oauthCode = query.code || options.code || ''
      if (!this.redirect) {
        this.redirect = this.decodeRedirect(query.redirect || '')
      }
    },

    readRouteOptions() {
      if (typeof window === 'undefined') return {}
      const url = new URL(window.location.href)
      const searchEntries = Array.from(url.searchParams.entries())
      const hashQuery = window.location.hash.split('?')[1] || ''
      const hashEntries = Array.from(new URLSearchParams(hashQuery).entries())
      return Object.fromEntries([...searchEntries, ...hashEntries])
    },

    shouldHandleOauthCallback() {
      return !IS_MP_WEIXIN && this.isH5Wechat && !!this.oauthCode && !this.oauthHandled
    },

    startOfficialWechatOauth() {
      if (typeof window === 'undefined') {
        this.wechatLoading = false
        return
      }
      if (this.redirect) {
        saveRedirectUrl(this.redirect)
      }
      const callbackUrl = new URL(H5_WECHAT_OAUTH_CALLBACK_PATH, window.location.origin)
      callbackUrl.searchParams.set('oauth', 'wechat')
      const targetRedirect = this.redirect || '/pages/index/index'
      callbackUrl.searchParams.set('redirect', targetRedirect)
      const authUrl = `https://open.weixin.qq.com/connect/oauth2/authorize?appid=${encodeURIComponent(H5_WECHAT_OFFICIAL_APP_ID)}&redirect_uri=${encodeURIComponent(callbackUrl.toString())}&response_type=code&scope=snsapi_userinfo&state=shiyiju_h5_login#wechat_redirect`
      window.location.replace(authUrl)
    },

    async handleOfficialWechatLogin() {
      this.oauthHandled = true
      this.wechatLoading = true
      try {
        const data = await wxLogin({
          code: this.oauthCode,
          loginScene: 'h5'
        })
        if (this.redirect) {
          saveRedirectUrl(this.redirect)
        }
        this.finishWechatLogin(data, {}, '微信授权成功')
      } catch (e) {
        this.oauthHandled = false
        this.handleRequestError(e, '微信授权失败')
      } finally {
        this.wechatLoading = false
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
      if (!this.wechatLoginSupported) {
        return Promise.reject(new Error('当前环境不支持微信授权登录'))
      }

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
          fail: (err) => reject(err || new Error('微信登录不可用'))
        })
      })
    },

    resolveWechatProfile() {
      if (!this.wechatLoginSupported) {
        return Promise.reject(new Error('当前环境不支持拉取微信资料'))
      }

      if (typeof wx === 'undefined' || typeof wx.getUserProfile !== 'function') {
        return Promise.reject(new Error('当前微信能力不可用，请在微信小程序内重试'))
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
          fail: () => reject(new Error('未完成微信头像授权'))
        })
      })
    },

    async resolveWechatProfileOrFallback() {
      try {
        return await this.resolveWechatProfile()
      } catch (e) {
        console.warn('[Login] 微信头像昵称授权未完成，改用默认资料继续登录', e)
        return {
          nickname: '',
          avatar: '',
          gender: 0,
          region: ''
        }
      }
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
        unionId: data.unionId || '',
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

    isNativeWechatSdkMissing(message = '') {
      return /WechatOpenSDK|旧包|卸载 App 后重新从 Xcode 安装|未加载 WechatOpenSDK/i.test(message)
    },

    showNativeWechatRepairGuide(message = '') {
      uni.showModal({
        title: 'App 微信登录组件未生效',
        content: message || '当前手机里的艺本艺术 App 仍是旧安装包，未带上微信登录 SDK。请先卸载手机上的旧 App，再从当前 Xcode 工程重新安装。',
        confirmText: '知道了',
        showCancel: false
      })
    },

    handleRequestError(e, defaultMsg) {
      const errMsg = e?.message || defaultMsg
      console.error('[Login]', errMsg, e)

      if (this.isNativeWechatSdkMissing(errMsg)) {
        this.showNativeWechatRepairGuide(errMsg)
        return
      }

      if (IS_MP_WEIXIN && /getPhoneNumber|手机号授权|phone number/i.test(errMsg)) {
        uni.showModal({
          title: '需要手机号授权',
          content: '微信一键登录需要同时授权微信手机号。若不授权手机号，可改用短信登录或账号密码登录。',
          showCancel: false
        })
        return
      }

      // 特殊处理：手机号未注册
      if (errMsg.includes('未注册')) {
        uni.showToast({
          title: '新手机号验证码登录会自动注册，请重新获取验证码',
          icon: 'none',
          duration: 3000
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
        user: '/pages/user-extra/agreement?type=user',
        privacy: '/pages/user-extra/agreement?type=privacy'
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
  padding: 0 28rpx calc(34rpx + env(safe-area-inset-bottom));
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
  height: 500rpx;
  opacity: 0.45;
}

.page-shade {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(11, 11, 12, 0.16) 0%, rgba(11, 11, 12, 0.86) 45%, #0b0b0c 100%),
    radial-gradient(circle at 18% 8%, rgba(201, 162, 39, 0.2), transparent 38%);
}

.brand-section,
.login-card {
  position: relative;
  z-index: 1;
}

.brand-section {
  min-height: 250rpx;
  display: flex;
  align-items: flex-end;
  gap: 24rpx;
  padding-bottom: 18rpx;
  box-sizing: border-box;
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

.login-card {
  padding: 30rpx 24rpx 26rpx;
  border-radius: 28rpx;
  background: rgba(19, 18, 18, 0.88);
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 28rpx 80rpx rgba(0, 0, 0, 0.34);
  backdrop-filter: blur(16rpx);
}

.card-heading {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  align-items: flex-start;
  margin-bottom: 18rpx;
}

.card-title {
  display: block;
}

.card-title {
  font-size: 32rpx;
  line-height: 44rpx;
  font-weight: 800;
  color: #f6f2e8;
}

.card-switch {
  flex-shrink: 0;
  padding-top: 7rpx;
  font-size: 27rpx;
  line-height: 38rpx;
  font-weight: 600;
  color: #c9a227;
}

.field-row {
  min-height: 88rpx;
  margin-top: 18rpx;
  padding: 0 20rpx;
  border-radius: 16rpx;
  background: #202024;
  border: 2rpx solid transparent;
  display: flex;
  align-items: center;
  gap: 18rpx;
  transition: border-color 0.18s ease;
  box-sizing: border-box;
}

.field-row.error {
  border-color: rgba(255, 91, 91, 0.75);
}

.country-code {
  min-width: 72rpx;
  display: flex;
  align-items: center;
  gap: 6rpx;
  color: #b9b1a5;
  font-size: 28rpx;
  flex-shrink: 0;
}

.country-arrow {
  font-size: 20rpx;
  color: #777;
  transform: translateY(-2rpx);
}

.field-input {
  flex: 1;
  min-width: 0;
  height: 88rpx;
  color: #f6f2e8;
  font-size: 28rpx;
  background: transparent;
}

.field-placeholder {
  color: #777166;
}

.code-action,
.password-toggle {
  padding: 0;
  background: transparent;
  color: #c9a227;
  border: none;
  font-size: 25rpx;
  line-height: 88rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.code-action::after {
  border: none;
}

.code-action.disabled,
.code-action[disabled] {
  color: rgba(201, 162, 39, 0.36);
  background: transparent;
}

.error-text {
  display: block;
  margin: 8rpx 0 0;
  padding-left: 20rpx;
  font-size: 23rpx;
  color: #ff6868;
  line-height: 32rpx;
}

.help-text {
  display: block;
  margin: 16rpx 0 28rpx;
  font-size: 22rpx;
  line-height: 34rpx;
  color: #8f887e;
}

.primary-login,
.local-phone-login {
  width: 100%;
  height: 88rpx;
  border-radius: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 800;
  border: none;
  box-sizing: border-box;
}

.primary-login::after,
.local-phone-login::after,
.quick-icon::after {
  border: none;
}

.primary-login {
  color: #16130b;
  background: #c9a227;
  margin-bottom: 18rpx;
}

.primary-login:not([disabled]) {
  background: #c9a227;
  color: #16130b;
}

.primary-login.disabled,
.primary-login[disabled] {
  color: #777166;
  background: #343436;
}

.local-phone-login {
  margin-bottom: 24rpx;
  color: #f6f2e8;
  background: rgba(255, 255, 255, 0.055);
  border: 1rpx solid rgba(255, 255, 255, 0.12);
}

.agreement-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8rpx;
  color: #8f887e;
  font-size: 22rpx;
  line-height: 34rpx;
  padding: 0 6rpx;
}

.agreement-check {
  width: 28rpx;
  height: 28rpx;
  border-radius: 50%;
  border: 2rpx solid #c9a227;
  color: #16130b;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20rpx;
  font-weight: 800;
  box-sizing: border-box;
  flex-shrink: 0;
}

.agreement-check.checked {
  background: #c9a227;
}

.agreement-link {
  color: #c9a227;
}

.quick-login {
  margin: 28rpx 0 22rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.quick-title {
  color: #777166;
  font-size: 22rpx;
  white-space: nowrap;
}

.quick-line {
  flex: 1;
  height: 1rpx;
  background: rgba(255, 255, 255, 0.08);
}

.wechat-login {
  width: 100%;
  height: 84rpx;
  border-radius: 42rpx;
  color: #f6f2e8;
  font-size: 28rpx;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.055);
  border: 1rpx solid rgba(255, 255, 255, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14rpx;
}

.wechat-login::after {
  border: none;
}

.wechat-icon {
  width: 38rpx;
  height: 38rpx;
  flex-shrink: 0;
}

@media screen and (max-width: 375px) {
  .login-page {
    padding-left: 24rpx;
    padding-right: 24rpx;
  }
  .brand-section {
    min-height: 250rpx;
  }
  .app-name {
    font-size: 44rpx;
  }
  .login-card {
    padding-left: 20rpx;
    padding-right: 20rpx;
  }
}

@media screen and (min-width: 768px) {
  .login-page {
    margin: 0 auto;
    max-width: 750rpx;
    padding-left: 80rpx;
    padding-right: 80rpx;
  }
}
</style>
