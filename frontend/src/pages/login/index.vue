<template>
  <view class="login-page">
    <!-- Logo区域 -->
    <view class="logo-section">
      <image class="logo" src="/static/images/logo.png" mode="aspectFit"></image>
      <text class="app-name">拾艺局</text>
      <text class="app-slogan">高端艺术品社交电商平台</text>
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
      </view>
    </uni-popup>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { wxLogin, bindPhone } from '@/api/user'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()

// 状态
const loading = ref(false)
const showPhoneLogin = ref(false)
const phonePopup = ref(null)
const phoneForm = ref({
  phone: '',
  captcha: ''
})
const captchaCountdown = ref(0)

// 是否可提交
const canSubmitPhone = computed(() => {
  return /^1[3-9]\d{9}$/.test(phoneForm.value.phone) && 
         phoneForm.value.captcha.length === 6
})

// 微信登录
const onWechatLogin = async () => {
  if (loading.value) return
  loading.value = true
  
  try {
    // 获取微信登录code
    const { code } = await new Promise((resolve, reject) => {
      uni.login({
        provider: 'weixin',
        success: resolve,
        fail: reject
      })
    })
    
    // 调用后端登录接口
    const data = await wxLogin(code)
    
    // 保存Token和用户信息
    userStore.setToken(data.token)
    userStore.setUserInfo(data.userInfo)
    
    uni.showToast({ title: '登录成功', icon: 'success' })
    
    // 返回上一页或首页
    setTimeout(() => {
      uni.navigateBack() || uni.switchTab({ url: '/pages/index/index' })
    }, 1500)
  } catch (e) {
    console.error('微信登录失败', e)
    uni.showToast({ title: '登录失败，请重试', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 发送验证码
const sendCaptcha = async () => {
  if (!/^1[3-9]\d{9}$/.test(phoneForm.value.phone)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  
  // TODO: 调用发送验证码接口
  uni.showToast({ title: '验证码已发送', icon: 'success' })
  
  // 开始倒计时
  captchaCountdown.value = 60
  const timer = setInterval(() => {
    captchaCountdown.value--
    if (captchaCountdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

// 手机号登录
const onPhoneLogin = async () => {
  if (!canSubmitPhone.value || loading.value) return
  loading.value = true
  
  try {
    const data = await bindPhone(phoneForm.value.phone, phoneForm.value.captcha)
    
    userStore.setToken(data.token)
    userStore.setUserInfo(data.userInfo)
    
    uni.showToast({ title: '登录成功', icon: 'success' })
    phonePopup.value.close()
    
    setTimeout(() => {
      uni.navigateBack() || uni.switchTab({ url: '/pages/index/index' })
    }, 1500)
  } catch (e) {
    console.error('手机号登录失败', e)
    uni.showToast({ title: e.message || '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 查看协议
const viewAgreement = (type) => {
  uni.navigateTo({
    url: `/pages/user/agreement?type=${type}`
  })
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background-color: #ffffff;
  padding: 100rpx 60rpx;
}

.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 100rpx;
  
  .logo {
    width: 160rpx;
    height: 160rpx;
    margin-bottom: 30rpx;
  }
  
  .app-name {
    font-size: 48rpx;
    font-weight: 600;
    color: #333333;
    margin-bottom: 16rpx;
  }
  
  .app-slogan {
    font-size: 28rpx;
    color: #999999;
  }
}

.agreement-section {
  margin-bottom: 60rpx;
  
  .agreement-text {
    font-size: 24rpx;
    color: #999999;
    text-align: center;
    line-height: 1.6;
    
    .link {
      color: #e6be8a;
    }
  }
}

.login-section {
  button {
    width: 100%;
    height: 96rpx;
    border-radius: 48rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32rpx;
    margin-bottom: 30rpx;
    border: none;
    
    .iconfont {
      font-size: 44rpx;
      margin-right: 16rpx;
    }
  }
  
  .btn-wechat {
    background-color: #07c160;
    color: #ffffff;
  }
  
  .btn-phone {
    background-color: #f5f5f5;
    color: #333333;
  }
}

.phone-login-popup {
  background-color: #ffffff;
  border-radius: 32rpx 32rpx 0 0;
  padding: 40rpx 40rpx;
  padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
  
  .popup-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 50rpx;
    
    .popup-title {
      font-size: 36rpx;
      font-weight: 600;
      color: #333333;
    }
    
    .popup-close {
      font-size: 40rpx;
      color: #999999;
    }
  }
  
  .phone-form {
    .form-item {
      margin-bottom: 30rpx;
      
      input {
        width: 100%;
        height: 96rpx;
        padding: 0 30rpx;
        background-color: #f5f5f5;
        border-radius: 12rpx;
        font-size: 30rpx;
      }
    }
    
    .captcha-item {
      display: flex;
      
      .captcha-input {
        flex: 1;
        margin-right: 20rpx;
      }
      
      .captcha-btn {
        width: 240rpx;
        height: 96rpx;
        background-color: #333333;
        color: #ffffff;
        font-size: 28rpx;
        border-radius: 12rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        
        &[disabled] {
          background-color: #cccccc;
        }
      }
    }
    
    .btn-submit {
      width: 100%;
      height: 96rpx;
      background-color: #333333;
      color: #ffffff;
      font-size: 32rpx;
      border-radius: 48rpx;
      margin-top: 40rpx;
      
      &[disabled] {
        background-color: #cccccc;
      }
    }
  }
}
</style>
