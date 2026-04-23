<template>
  <view class="settings-page">
    <!-- 用户信息卡片 -->
    <view class="user-card card">
      <view class="user-info" @click="goProfile">
        <image class="user-avatar" :src="userInfo.avatar || '/static/images/avatar.png'" mode="aspectFill"></image>
        <view class="user-detail">
          <text class="user-name">{{ userInfo.nickname || '未登录' }}</text>
          <text class="user-id" v-if="userInfo.id">ID: {{ userInfo.id }}</text>
        </view>
        
      </view>
    </view>

    <!-- 账号安全 -->
    <view class="section card">
      <view class="section-title">账号安全</view>
      <view class="menu-list">
        <view class="menu-item" @click="goPage('/pages/common/coming-soon?title=绑定手机号&desc=手机号绑定页正在开发中，后续会补充验证码和换绑流程。')">
          
          <text class="menu-text">手机号绑定</text>
          <view class="menu-value" v-if="userInfo.phone">{{ formatPhone(userInfo.phone) }}</view>
          
        </view>
        <view class="menu-item" @click="goPage('/pages/common/coming-soon?title=密码管理&desc=密码设置页正在开发中，后续会补充修改与找回能力。')">
          <text>🔒</text>
          <text class="menu-text">登录密码</text>
          <view class="menu-value">修改</view>
          
        </view>
        <view class="menu-item" @click="goPage('/pages/common/coming-soon?title=实名认证&desc=实名认证页正在开发中，后续会补充实名校验流程。')">
          
          <text class="menu-text">实名认证</text>
          <view class="menu-value" :class="{ 'status-certified': userInfo.realNameStatus === 1 }">
            {{ getRealNameStatus(userInfo.realNameStatus) }}
          </view>
          
        </view>
      </view>
    </view>

    <!-- 偏好设置 -->
    <view class="section card">
      <view class="section-title">偏好设置</view>
      <view class="menu-list">
        <view class="menu-item">
          
          <text class="menu-text">消息推送</text>
          <switch :checked="settings.pushEnabled" @change="togglePush" color="#667eea"></switch>
        </view>
        <view class="menu-item">
          
          <text class="menu-text">声音</text>
          <switch :checked="settings.soundEnabled" @change="toggleSound" color="#667eea"></switch>
        </view>
        <view class="menu-item">
          
          <text class="menu-text">仅WiFi加载图片</text>
          <switch :checked="settings.wifiOnly" @change="toggleWifiOnly" color="#667eea"></switch>
        </view>
        <view class="menu-item" @click="showLanguagePicker">
          
          <text class="menu-text">语言</text>
          <view class="menu-value">简体中文</view>
          
        </view>
      </view>
    </view>

    <!-- 通用设置 -->
    <view class="section card">
      <view class="section-title">通用设置</view>
      <view class="menu-list">
        <view class="menu-item" @click="clearCache">
          
          <text class="menu-text">清理缓存</text>
          <view class="menu-value">{{ cacheSize }}</view>
          
        </view>
        <view class="menu-item" @click="checkUpdate">
          
          <text class="menu-text">检查更新</text>
          <view class="menu-value version">v{{ version }}</view>
          
        </view>
      </view>
    </view>

    <!-- 关于 -->
    <view class="section card">
      <view class="section-title">关于</view>
      <view class="menu-list">
        <view class="menu-item" @click="goPage('/pages/user/agreement?type=user')">
          
          <text class="menu-text">用户协议</text>
          
        </view>
        <view class="menu-item" @click="goPage('/pages/user/agreement?type=privacy')">
          
          <text class="menu-text">隐私政策</text>
          
        </view>
        <view class="menu-item" @click="goPage('/pages/user/feedback')">
          
          <text class="menu-text">意见反馈</text>
          
        </view>
        <view class="menu-item" @click="showAbout">
          
          <text class="menu-text">关于我们</text>
          
        </view>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-btn card" v-if="isLoggedIn" @click="handleLogout">
      退出登录
    </view>

    <view class="logout-btn card" v-else @click="goLogin">
      登录/注册
    </view>

    <!-- 关于弹窗 -->
    <!-- 弹窗开始 -->
      <view class="about-popup">
        <image class="app-logo" src="/static/logo.png" mode="aspectFit"></image>
        <text class="app-name">拾艺局</text>
        <text class="app-desc">让艺术走进生活</text>
        <view class="about-content">
          <text>拾艺局是一个专注于艺术品交易的平台，为艺术家、收藏家和艺术爱好者提供便捷的艺术品交易服务。</text>
        </view>
        <view class="about-links">
          <text @click="goPage('/pages/user/agreement?type=user')">用户协议</text>
          <text class="link-divider">|</text>
          <text @click="goPage('/pages/user/agreement?type=privacy')">隐私政策</text>
        </view>
        <view class="about-copyright">© 2024 拾艺局 All Rights Reserved</view>
      </view>
<!-- 弹窗结束 -->
  </view>
</template>

<script>
import { useUserStore } from '@/store/modules/user.js'

export default {
  data() {
    return {
      version: '1.0.0',
      cacheSize: '0 KB',
      showAboutPopup: false,
      settings: {
        pushEnabled: true,
        soundEnabled: true,
        wifiOnly: false
      }
    }
  },

  computed: {
    userStore() {
      return useUserStore()
    },
    userInfo() {
      return this.userStore.userInfo || {}
    },
    isLoggedIn() {
      return this.userStore.isLoggedIn
    }
  },

  onLoad() {
    this.calculateCacheSize()
  },

  onShow() {
    // 刷新用户信息
    if (this.isLoggedIn) {
      this.userStore.fetchUserInfo()
    }
  },

  methods: {
    formatPhone(phone) {
      if (!phone) return ''
      return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
    },

    getRealNameStatus(status) {
      const map = {
        0: '未认证',
        1: '已认证',
        2: '审核中'
      }
      return map[status] || '未认证'
    },

    goProfile() {
      if (!this.isLoggedIn) {
        this.goLogin()
        return
      }
      uni.navigateTo({
        url: '/pages/user/profile'
      })
    },

    goLogin() {
      uni.navigateTo({
        url: '/pages/login/index'
      })
    },

    goPage(url) {
      if (!this.isLoggedIn && url.includes('coming-soon')) {
        this.goLogin()
        return
      }
      uni.navigateTo({ url })
    },

    togglePush(e) {
      this.settings.pushEnabled = e.detail.value
      uni.setStorageSync('settings', this.settings)
      if (this.settings.pushEnabled) {
        uni.showToast({ title: '已开启推送', icon: 'success' })
      }
    },

    toggleSound(e) {
      this.settings.soundEnabled = e.detail.value
      uni.setStorageSync('settings', this.settings)
    },

    toggleWifiOnly(e) {
      this.settings.wifiOnly = e.detail.value
      uni.setStorageSync('settings', this.settings)
    },

    showLanguagePicker() {
      uni.showToast({ title: '当前仅支持简体中文', icon: 'none' })
    },

    async clearCache() {
      uni.showModal({
        title: '提示',
        content: '确定要清理缓存吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              // 清理本地存储
              const keys = ['searchHistory', 'browseHistory', 'cacheData']
              keys.forEach(key => {
                uni.removeStorageSync(key)
              })
              this.cacheSize = '0 KB'
              uni.showToast({ title: '清理成功', icon: 'success' })
            } catch (e) {
              uni.showToast({ title: '清理失败', icon: 'none' })
            }
          }
        }
      })
    },

    calculateCacheSize() {
      try {
        const info = uni.getStorageInfoSync()
        const size = info.currentSize
        if (size < 1024) {
          this.cacheSize = size + ' KB'
        } else {
          this.cacheSize = (size / 1024).toFixed(2) + ' MB'
        }
      } catch (e) {
        this.cacheSize = '未知'
      }
    },

    checkUpdate() {
      uni.showLoading({ title: '检查中...' })
      setTimeout(() => {
        uni.hideLoading()
        uni.showToast({ title: '已是最新版本', icon: 'success' })
      }, 1000)
    },

    showAbout() {
      this.showAboutPopup = true
    },

    handleLogout() {
      uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            this.userStore.logout()
            uni.showToast({ title: '已退出登录', icon: 'success' })
            setTimeout(() => {
              uni.reLaunch({ url: '/pages/index/index' })
            }, 1000)
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.settings-page {
  min-height: 100vh;
  background: #f5f6f7;
  padding-bottom: 40rpx;
}

.user-card {
  margin: 20rpx;
  padding: 30rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16rpx;

  .user-info {
    display: flex;
    align-items: center;

    .user-avatar {
      width: 120rpx;
      height: 120rpx;
      border-radius: 60rpx;
      border: 4rpx solid rgba(255, 255, 255, 0.5);
    }

    .user-detail {
      flex: 1;
      margin-left: 24rpx;

      .user-name {
        font-size: 36rpx;
        font-weight: 600;
        color: #fff;
      }

      .user-id {
        display: block;
        margin-top: 8rpx;
        font-size: 24rpx;
        color: rgba(255, 255, 255, 0.7);
      }
    }
  }
}

.section {
  margin: 20rpx;
  padding: 0;

  .section-title {
    padding: 24rpx 30rpx;
    font-size: 28rpx;
    font-weight: 600;
    color: #333;
    border-bottom: 1rpx solid #f0f0f0;
  }

  .menu-list {
    padding: 0 30rpx;
  }

  .menu-item {
    display: flex;
    align-items: center;
    padding: 28rpx 0;
    border-bottom: 1rpx solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    .menu-text {
      flex: 1;
      margin-left: 20rpx;
      font-size: 30rpx;
      color: #333;
    }

    .menu-value {
      font-size: 26rpx;
      color: #999;
      margin-right: 10rpx;

      &.version {
        color: #667eea;
      }

      &.status-certified {
        color: #52c41a;
      }
    }
  }
}

.logout-btn {
  margin: 40rpx 20rpx;
  padding: 30rpx;
  text-align: center;
  font-size: 32rpx;
  color: #ff4d4f;
  background: #fff;
  border-radius: 16rpx;
}

.about-popup {
  width: 600rpx;
  padding: 60rpx 40rpx 40rpx;
  text-align: center;

  .app-logo {
    width: 160rpx;
    height: 160rpx;
    border-radius: 32rpx;
  }

  .app-name {
    display: block;
    margin-top: 24rpx;
    font-size: 40rpx;
    font-weight: 600;
    color: #333;
  }

  .app-desc {
    display: block;
    margin-top: 8rpx;
    font-size: 26rpx;
    color: #999;
  }

  .about-content {
    margin-top: 40rpx;
    padding: 30rpx;
    background: #f9f9f9;
    border-radius: 12rpx;

    text {
      font-size: 26rpx;
      color: #666;
      line-height: 1.8;
      text-align: left;
    }
  }

  .about-links {
    margin-top: 40rpx;
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 20rpx;

    text {
      font-size: 26rpx;
      color: #667eea;
    }

    .link-divider {
      color: #ddd;
    }
  }

  .about-copyright {
    margin-top: 40rpx;
    font-size: 22rpx;
    color: #ccc;
  }
}
</style>
