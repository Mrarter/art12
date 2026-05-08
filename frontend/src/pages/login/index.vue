<template>
  <view class="login-page">
    <image class="hero-bg" src="/static/images/museum-v12-hero-bg.png" mode="aspectFill"></image>
    <view class="page-shade"></view>

    <view class="brand-section">
      <view class="brand-mark">
        <image class="brand-logo" src="/static/images/logo.png" mode="aspectFit"></image>
      </view>
      <view class="brand-copy">
        <text class="app-name">拾艺局</text>
        <text class="app-slogan">艺术收藏、发布与分享的一站式入口</text>
      </view>
    </view>

    <view class="identity-panel">
      <view class="panel-head">
        <text class="panel-title">选择登录身份</text>
        <text class="panel-subtitle">{{ selectedIdentityInfo.hint }}</text>
      </view>
      <view class="identity-list">
        <view
          v-for="item in identityOptions"
          :key="item.value"
          class="identity-option"
          :class="{ active: selectedIdentity === item.value }"
          @click="selectIdentity(item.value)"
        >
          <view class="option-icon" :class="item.tone">
            <image :src="item.icon" mode="aspectFit"></image>
          </view>
          <view class="option-copy">
            <text class="option-name">{{ item.label }}</text>
            <text class="option-desc">{{ item.desc }}</text>
          </view>
          <view class="option-check">
            <text v-if="selectedIdentity === item.value">✓</text>
          </view>
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

export default {
  data() {
    return {
      loading: false,
      showPhoneLogin: false,
      selectedIdentity: 'collector', // 默认收藏家
      redirect: '',
      phoneForm: {
        phone: '',
        captcha: ''
      },
      captchaCountdown: 0
    }
  },
  
  computed: {
    identityOptions() {
      return [
        {
          value: 'collector',
          label: '收藏家',
          desc: '管理收藏、订单与关注的艺术家',
          hint: '以收藏者身份进入，探索作品与拍卖',
          icon: '/static/art-icons/icon-collector.svg',
          tone: 'gold'
        },
        {
          value: 'artist',
          label: '艺术家',
          desc: '发布作品、完善认证与维护主页',
          hint: '以艺术家身份进入，管理创作与展陈',
          icon: '/static/art-icons/icon-artist.svg',
          tone: 'green'
        },
        {
          value: 'promoter',
          label: '艺荐官',
          desc: '分享作品、查看团队与佣金收益',
          hint: '以艺荐官身份进入，跟踪推广收益',
          icon: '/static/art-icons/icon-share.svg',
          tone: 'blue'
        }
      ]
    },
    selectedIdentityInfo() {
      return this.identityOptions.find(item => item.value === this.selectedIdentity) || this.identityOptions[0]
    },
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
      const identities = ['collector', 'artist', 'promoter']
      if (identities.includes(options.identity)) {
        this.selectedIdentity = options.identity
      }
      this.redirect = options.redirect ? decodeURIComponent(options.redirect) : ''
      const userStore = useUserStore()
      // 如果已登录，直接跳转
      if (userStore.isAuthenticated) {
        this.afterLogin()
      }
    },
    
    // 选择身份
    selectIdentity(identity) {
      this.selectedIdentity = identity
    },
    
    // 微信登录
    async onWechatLogin() {
      if (this.loading) return
      this.loading = true
      
      try {
        // 获取微信登录code
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
        
        const { code } = loginRes
        
        // 调用后端登录接口
        const data = await wxLogin({ 
          code, 
          identity: this.selectedIdentity 
        })
        
        // 保存Token和用户信息
        const userStore = useUserStore()
        userStore.setToken(data.token)
        userStore.setUserInfo({
          ...data.userInfo,
          currentIdentity: this.selectedIdentity
        })
        
        // 如果选择了艺术家或艺荐官身份，保存偏好
        if (this.selectedIdentity !== 'collector') {
          uni.setStorageSync('preferredIdentity', this.selectedIdentity)
        }
        
        uni.showToast({ title: '登录成功', icon: 'success' })
        
        setTimeout(() => {
          this.afterLogin()
        }, 1500)
      } catch (e) {
        console.error('微信登录失败', e)
        uni.showToast({ title: '微信登录失败，请重试', icon: 'none' })
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
          code: this.phoneForm.captcha,
          identity: this.selectedIdentity
        })
        
        const userStore = useUserStore()
        userStore.setToken(data.token)
        userStore.setUserInfo({
          ...data.userInfo,
          currentIdentity: this.selectedIdentity
        })
        
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
  border-radius: 30rpx;
  background: rgba(246, 242, 232, 0.92);
  border: 2rpx solid rgba(201, 162, 39, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.brand-logo {
  width: 92rpx;
  height: 92rpx;
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

.identity-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14rpx;
}

.identity-option {
  min-height: 112rpx;
  padding: 18rpx;
  border-radius: 14rpx;
  background: #202024;
  border: 1rpx solid transparent;
  display: flex;
  align-items: center;
  gap: 16rpx;
  box-sizing: border-box;
}

.identity-option.active {
  background: rgba(201, 162, 39, 0.12);
  border-color: rgba(201, 162, 39, 0.48);
}

.option-icon {
  width: 58rpx;
  height: 58rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: rgba(201, 162, 39, 0.15);
}

.option-icon.green {
  background: rgba(88, 185, 130, 0.16);
}

.option-icon.blue {
  background: rgba(95, 143, 199, 0.16);
}

.option-icon image {
  width: 34rpx;
  height: 34rpx;
}

.option-copy {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.option-name {
  font-size: 28rpx;
  line-height: 36rpx;
  color: #f6f2e8;
  font-weight: 700;
}

.option-desc {
  font-size: 22rpx;
  line-height: 32rpx;
  color: #8f887e;
}

.option-check {
  width: 42rpx;
  height: 42rpx;
  border-radius: 50%;
  border: 1rpx solid rgba(255, 255, 255, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c9a227;
  font-size: 24rpx;
  flex-shrink: 0;
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
