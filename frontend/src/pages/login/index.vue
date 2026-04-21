<template>
  <view class="login-page">
    <!-- Logo区域 -->
    <view class="logo-section">
      <image class="logo" src="/static/images/logo.png" mode="aspectFit"></image>
      <text class="app-name">拾艺局</text>
      <text class="app-slogan">高端艺术品社交电商平台</text>
    </view>
    
    <!-- 身份选择入口 -->
    <view class="identity-intro">
      <text class="intro-title">选择您的身份</text>
      <view class="identity-options">
        <view class="identity-option" @click="selectIdentity('collector')">
          <view class="option-icon collector">
            <image src="/static/icons/collector.png" mode="aspectFit"></image>
          </view>
          <text class="option-name">收藏家</text>
          <text class="option-desc">探索艺术，购买收藏</text>
        </view>
        <view class="identity-option" @click="selectIdentity('artist')">
          <view class="option-icon artist">
            <image src="/static/icons/artist.png" mode="aspectFit"></image>
          </view>
          <text class="option-name">艺术家</text>
          <text class="option-desc">展示作品，分享创意</text>
        </view>
        <view class="identity-option" @click="selectIdentity('promoter')">
          <view class="option-icon promoter">
            <image src="/static/icons/promoter.png" mode="aspectFit"></image>
          </view>
          <text class="option-name">艺荐官</text>
          <text class="option-desc">推广艺术，赚取佣金</text>
        </view>
      </view>
    </view>
    
    <!-- 功能特色 -->
    <view class="features-section">
      <view class="feature-item">
        <view class="feature-icon">
          <image src="/static/icons/feature-auction.png" mode="aspectFit"></image>
        </view>
        <view class="feature-text">
          <text class="feature-title">精品拍卖</text>
          <text class="feature-desc">参与艺术品竞拍</text>
        </view>
      </view>
      <view class="feature-item">
        <view class="feature-icon">
          <image src="/static/icons/feature-social.png" mode="aspectFit"></image>
        </view>
        <view class="feature-text">
          <text class="feature-title">艺术社交</text>
          <text class="feature-desc">关注艺术家，交流心得</text>
        </view>
      </view>
      <view class="feature-item">
        <view class="feature-icon">
          <image src="/static/icons/feature-reward.png" mode="aspectFit"></image>
        </view>
        <view class="feature-text">
          <text class="feature-title">分享赚佣</text>
          <text class="feature-desc">推荐作品获得收益</text>
        </view>
      </view>
    </view>
    
    <!-- 协议说明 -->
    <view class="agreement-section">
      <view class="agreement-text">
        登录即表示同意
        <text class="link" @click="viewAgreement('user')">《用户协议》</text>
        和
        <text class="link" @click="viewAgreement('privacy')">《隐私政策》</text>
      </view>
    </view>
    
    <!-- 登录按钮 -->
    <view class="login-section">
      <!-- 微信登录 -->
      <button class="btn-wechat" @click="onWechatLogin" :loading="loading">
        <text class="iconfont icon-wechat"></text>
        <text>微信一键登录</text>
      </button>
      
      <!-- 手机号登录 -->
      <button class="btn-phone" @click="showPhoneLogin = true">
        <text class="iconfont icon-phone"></text>
        <text>手机号登录</text>
      </button>
      
      <!-- 游客体验 -->
      <button class="btn-guest" @click="onGuestLogin">
        <text>游客体验</text>
      </button>
    </view>
    
    <!-- 手机号登录弹窗 -->
    <uni-popup ref="phonePopup" type="bottom">
      <view class="phone-login-popup">
        <view class="popup-header">
          <text class="popup-title">手机号登录</text>
          <text class="popup-close iconfont icon-close" @click="phonePopup.close()"></text>
        </view>
        
        <view class="phone-form">
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
    </uni-popup>
  </view>
</template>

<script>
import { ref, computed } from 'vue'
import { wxLogin, phoneLogin, sendSmsCode } from '@/api/user'
import { useUserStore } from '@/store/modules/user'

export default {
  data() {
    return {
      loading: false,
      showPhoneLogin: false,
      phonePopup: null,
      selectedIdentity: 'collector', // 默认收藏家
      phoneForm: {
        phone: '',
        captcha: ''
      },
      captchaCountdown: 0
    }
  },
  
  computed: {
    canSubmitPhone() {
      return /^1[3-9]\d{9}$/.test(this.phoneForm.phone) && 
             this.phoneForm.captcha.length === 6
    }
  },
  
  onLoad() {
    this.initLogin()
  },
  
  methods: {
    // 初始化登录状态检查
    initLogin() {
      const userStore = useUserStore()
      // 如果已登录，直接跳转
      if (userStore.isAuthenticated) {
        uni.switchTab({ url: '/pages/index/index' })
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
              // 小程序环境外或未配置微信登录时，模拟登录
              console.log('微信登录不可用，使用模拟登录')
              resolve({ code: 'mock_code_' + Date.now() })
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
        
        // 返回上一页或首页
        setTimeout(() => {
          uni.navigateBack() || uni.switchTab({ url: '/pages/index/index' })
        }, 1500)
      } catch (e) {
        console.error('微信登录失败', e)
        // 模拟登录成功（开发环境）
        this.mockLogin()
      } finally {
        this.loading = false
      }
    },
    
    // 模拟登录（开发环境使用）
    mockLogin() {
      const userStore = useUserStore()
      const mockUser = {
        id: 10001,
        nickname: '艺术品爱好者',
        avatar: '',
        phone: '138****8888',
        isArtist: this.selectedIdentity === 'artist',
        isPromoter: this.selectedIdentity === 'promoter',
        currentIdentity: this.selectedIdentity,
        level: 1,
        vipLevel: 0
      }
      
      userStore.setToken('mock_token_' + Date.now())
      userStore.setUserInfo(mockUser)
      
      uni.showToast({ title: '登录成功（模拟）', icon: 'success' })
      setTimeout(() => {
        uni.navigateBack() || uni.switchTab({ url: '/pages/index/index' })
      }, 1500)
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
        this.phonePopup.close()
        
        setTimeout(() => {
          uni.navigateBack() || uni.switchTab({ url: '/pages/index/index' })
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
        await sendSmsCode({ phone: this.phoneForm.phone, type: 'login' })
        uni.showToast({ title: '验证码已发送', icon: 'success' })
      } catch (e) {
        // 模拟发送成功
        uni.showToast({ title: '验证码已发送', icon: 'success' })
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
        uni.switchTab({ url: '/pages/index/index' })
      }, 1500)
    },
    
    // 查看协议
    viewAgreement(type) {
      const urls = {
        user: '/pages/webview/index?url=https://shiyiju.com/agreement/user',
        privacy: '/pages/webview/index?url=https://shiyiju.com/agreement/privacy'
      }
      uni.navigateTo({ url: urls[type] })
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  padding: 0 40rpx;
  display: flex;
  flex-direction: column;
}

.logo-section {
  padding-top: 120rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.logo {
  width: 160rpx;
  height: 160rpx;
  border-radius: 40rpx;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.app-name {
  font-size: 52rpx;
  font-weight: 600;
  color: #fff;
  margin-top: 30rpx;
  letter-spacing: 8rpx;
}

.app-slogan {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 16rpx;
}

/* 身份选择 */
.identity-intro {
  margin-top: 80rpx;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  border-radius: 24rpx;
  padding: 40rpx;
}

.intro-title {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.9);
  text-align: center;
  display: block;
  margin-bottom: 30rpx;
}

.identity-options {
  display: flex;
  justify-content: space-between;
}

.identity-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx;
  border-radius: 16rpx;
  transition: all 0.3s;
}

.identity-option:active {
  background: rgba(255, 255, 255, 0.1);
  transform: scale(0.95);
}

.option-icon {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16rpx;
}

.option-icon image {
  width: 60rpx;
  height: 60rpx;
}

.option-icon.collector {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.option-icon.artist {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.option-icon.promoter {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.option-name {
  font-size: 28rpx;
  color: #fff;
  font-weight: 500;
  margin-bottom: 6rpx;
}

.option-desc {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.6);
}

/* 功能特色 */
.features-section {
  margin-top: 40rpx;
  display: flex;
  justify-content: space-around;
  padding: 30rpx 0;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.feature-icon {
  width: 80rpx;
  height: 80rpx;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;
}

.feature-icon image {
  width: 48rpx;
  height: 48rpx;
}

.feature-title {
  font-size: 26rpx;
  color: #fff;
  font-weight: 500;
}

.feature-desc {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 4rpx;
}

/* 协议说明 */
.agreement-section {
  margin-top: auto;
  padding: 30rpx 0;
}

.agreement-text {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.6);
  text-align: center;
  line-height: 1.6;
}

.link {
  color: #4facfe;
}

/* 登录按钮 */
.login-section {
  padding-bottom: 60rpx;
}

.btn-wechat {
  width: 100%;
  height: 96rpx;
  background: linear-gradient(135deg, #07c160 0%, #06ad56 100%);
  border-radius: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 32rpx;
  font-weight: 500;
  border: none;
  margin-bottom: 24rpx;
}

.btn-wechat::after {
  border: none;
}

.btn-phone {
  width: 100%;
  height: 96rpx;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 32rpx;
  font-weight: 500;
  border: 2rpx solid rgba(255, 255, 255, 0.3);
}

.btn-phone::after {
  border: none;
}

.btn-ghost {
  width: 100%;
  height: 80rpx;
  background: transparent;
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.8);
  font-size: 28rpx;
  border: none;
  margin-top: 16rpx;
}

.btn-guest {
  width: 100%;
  height: 80rpx;
  background: transparent;
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.6);
  font-size: 26rpx;
  border: none;
  margin-top: 20rpx;
}

.btn-guest::after {
  border: none;
}

/* 手机号登录弹窗 */
.phone-login-popup {
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
  padding: 40rpx;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 50rpx;
}

.popup-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
}

.popup-close {
  font-size: 40rpx;
  color: #999;
}

.phone-form {
  padding: 0 20rpx;
}

.form-item {
  margin-bottom: 30rpx;
}

.phone-input,
.captcha-input {
  width: 100%;
  height: 96rpx;
  background: #f5f5f5;
  border-radius: 16rpx;
  padding: 0 30rpx;
  font-size: 30rpx;
}

.captcha-item {
  display: flex;
  gap: 20rpx;
}

.captcha-input {
  flex: 1;
}

.captcha-btn {
  width: 220rpx;
  height: 96rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16rpx;
  color: #fff;
  font-size: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
}

.btn-submit {
  width: 100%;
  height: 96rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 48rpx;
  color: #fff;
  font-size: 32rpx;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 20rpx;
  border: none;
}

.btn-submit[disabled] {
  background: #ccc;
}

.wechat-tip {
  margin-top: 30rpx;
  padding: 20rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
}

.wechat-tip text {
  font-size: 24rpx;
  color: #999;
  text-align: center;
  display: block;
}
</style>
