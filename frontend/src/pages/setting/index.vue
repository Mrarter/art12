<template>
  <view class="settings-page">
    <!-- 页面标题 -->
    <view class="page-header">
      <view class="header-left" @click="goBack">
        <u-icon name="arrow-left" size="20" color="#333"></u-icon>
      </view>
      <text class="header-title">设置</text>
      <view class="header-right"></view>
    </view>

    <!-- 用户信息 -->
    <view class="user-card" v-if="userStore.isAuthenticated">
      <view class="user-info">
        <image class="user-avatar" :src="userInfo.avatar || '/static/avatar/default.jpg'" mode="aspectFill"></image>
        <view class="user-detail">
          <text class="user-name">{{ userInfo.nickname }}</text>
          <text class="user-id">ID: {{ userInfo.id }}</text>
        </view>
      </view>
      <u-icon name="arrow-right" size="16" color="#ccc"></u-icon>
    </view>

    <!-- 设置分组 -->
    <view class="settings-group">
      <view class="group-title">账号设置</view>
      <view class="group-list">
        <view class="group-item" @click="goPage('/pages/user/address')">
          <view class="item-left">
            <image class="item-icon" src="/static/icons/settings-address.png" mode="aspectFit"></image>
            <text class="item-name">收货地址</text>
          </view>
          <view class="item-right">
            <u-icon name="arrow-right" size="14" color="#ccc"></u-icon>
          </view>
        </view>
        <view class="group-item" @click="goPage('/pages/setting/notification')">
          <view class="item-left">
            <image class="item-icon" src="/static/icons/settings-notify.png" mode="aspectFit"></image>
            <text class="item-name">消息通知</text>
          </view>
          <view class="item-right">
            <view class="switch-wrapper">
              <switch :checked="notifyEnabled" @change="onNotifyChange" color="#667eea"></switch>
            </view>
          </view>
        </view>
        <view class="group-item" @click="goBindPhone">
          <view class="item-left">
            <image class="item-icon" src="/static/icons/settings-phone.png" mode="aspectFit"></image>
            <text class="item-name">绑定手机</text>
          </view>
          <view class="item-right">
            <text class="item-value">{{ userInfo.phone || '未绑定' }}</text>
            <u-icon name="arrow-right" size="14" color="#ccc"></u-icon>
          </view>
        </view>
        <view class="group-item" @click="goPage('/pages/user/following')">
          <view class="item-left">
            <image class="item-icon" src="/static/icons/settings-follow.png" mode="aspectFit"></image>
            <text class="item-name">我的关注</text>
          </view>
          <view class="item-right">
            <text class="item-value">{{ followCount }}人</text>
            <u-icon name="arrow-right" size="14" color="#ccc"></u-icon>
          </view>
        </view>
      </view>
    </view>

    <view class="settings-group">
      <view class="group-title">通用设置</view>
      <view class="group-list">
        <view class="group-item" @click="toggleTheme">
          <view class="item-left">
            <image class="item-icon" src="/static/icons/settings-theme.png" mode="aspectFit"></image>
            <text class="item-name">深色模式</text>
          </view>
          <view class="item-right">
            <view class="switch-wrapper">
              <switch :checked="isDarkMode" @change="onDarkModeChange" color="#667eea"></switch>
            </view>
          </view>
        </view>
        <view class="group-item" @click="clearCache">
          <view class="item-left">
            <image class="item-icon" src="/static/icons/settings-cache.png" mode="aspectFit"></image>
            <text class="item-name">清除缓存</text>
          </view>
          <view class="item-right">
            <text class="item-value">{{ cacheSize }}</text>
            <u-icon name="arrow-right" size="14" color="#ccc"></u-icon>
          </view>
        </view>
        <view class="group-item" @click="checkUpdate">
          <view class="item-left">
            <image class="item-icon" src="/static/icons/settings-update.png" mode="aspectFit"></image>
            <text class="item-name">版本更新</text>
          </view>
          <view class="item-right">
            <text class="item-value version">v1.0.0</text>
            <u-icon name="arrow-right" size="14" color="#ccc"></u-icon>
          </view>
        </view>
      </view>
    </view>

    <view class="settings-group">
      <view class="group-title">支持</view>
      <view class="group-list">
        <view class="group-item" @click="goPage('/pages/help/index')">
          <view class="item-left">
            <image class="item-icon" src="/static/icons/settings-help.png" mode="aspectFit"></image>
            <text class="item-name">帮助中心</text>
          </view>
          <view class="item-right">
            <u-icon name="arrow-right" size="14" color="#ccc"></u-icon>
          </view>
        </view>
        <view class="group-item" @click="goPage('/pages/about/index')">
          <view class="item-left">
            <image class="item-icon" src="/static/icons/settings-about.png" mode="aspectFit"></image>
            <text class="item-name">关于我们</text>
          </view>
          <view class="item-right">
            <u-icon name="arrow-right" size="14" color="#ccc"></u-icon>
          </view>
        </view>
        <view class="group-item" @click="openService">
          <view class="item-left">
            <image class="item-icon" src="/static/icons/settings-service.png" mode="aspectFit"></image>
            <text class="item-name">联系客服</text>
          </view>
          <view class="item-right">
            <u-icon name="arrow-right" size="14" color="#ccc"></u-icon>
          </view>
        </view>
        <view class="group-item" @click="openFeedback">
          <view class="item-left">
            <image class="item-icon" src="/static/icons/settings-feedback.png" mode="aspectFit"></image>
            <text class="item-name">意见反馈</text>
          </view>
          <view class="item-right">
            <u-icon name="arrow-right" size="14" color="#ccc"></u-icon>
          </view>
        </view>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-section" v-if="userStore.isAuthenticated">
      <button class="btn-logout" @click="onLogout">退出登录</button>
    </view>

    <!-- 协议链接 -->
    <view class="agreement-links">
      <text class="link" @click="viewAgreement('user')">《用户协议》</text>
      <text class="divider">|</text>
      <text class="link" @click="viewAgreement('privacy')">《隐私政策》</text>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '@/store/modules/user'

export default {
  data() {
    return {
      notifyEnabled: true,
      isDarkMode: false,
      cacheSize: '12.5MB',
      followCount: 12
    }
  },
  
  computed: {
    userStore() {
      return useUserStore()
    },
    userInfo() {
      return this.userStore.userInfo || {}
    }
  },
  
  onLoad() {
    this.loadSettings()
  },
  
  methods: {
    goBack() {
      uni.navigateBack()
    },
    
    loadSettings() {
      // 加载设置
      this.isDarkMode = uni.getStorageSync('darkMode') || false
      this.notifyEnabled = uni.getStorageSync('notifyEnabled') !== 'false'
    },
    
    goPage(url) {
      uni.navigateTo({ url })
    },
    
    goBindPhone() {
      uni.navigateTo({ url: '/pages/user/bindPhone' })
    },
    
    onNotifyChange(e) {
      this.notifyEnabled = e.detail.value
      uni.setStorageSync('notifyEnabled', String(this.notifyEnabled))
      uni.showToast({
        title: this.notifyEnabled ? '已开启通知' : '已关闭通知',
        icon: 'success'
      })
    },
    
    onDarkModeChange(e) {
      this.isDarkMode = e.detail.value
      uni.setStorageSync('darkMode', this.isDarkMode)
      // 实际项目应该切换主题
      uni.showToast({
        title: this.isDarkMode ? '已开启深色模式' : '已关闭深色模式',
        icon: 'success'
      })
    },
    
    toggleTheme() {
      this.isDarkMode = !this.isDarkMode
      this.onDarkModeChange({ detail: { value: this.isDarkMode } })
    },
    
    clearCache() {
      uni.showModal({
        title: '清除缓存',
        content: '确定要清除缓存吗？',
        success: (res) => {
          if (res.confirm) {
            // 模拟清除
            uni.showToast({ title: '缓存已清除', icon: 'success' })
            this.cacheSize = '0MB'
          }
        }
      })
    },
    
    checkUpdate() {
      uni.showToast({ title: '已是最新版本', icon: 'success' })
    },
    
    openService() {
      uni.makePhoneCall({
        phoneNumber: '400-888-8888',
        fail: () => {
          uni.showToast({ title: '客服电话：400-888-8888', icon: 'none' })
        }
      })
    },
    
    openFeedback() {
      uni.navigateTo({ url: '/pages/feedback/index' })
    },
    
    viewAgreement(type) {
      const urls = {
        user: '/pages/webview/index?url=https://shiyiju.com/agreement/user',
        privacy: '/pages/webview/index?url=https://shiyiju.com/agreement/privacy'
      }
      uni.navigateTo({ url: urls[type] })
    },
    
    onLogout() {
      uni.showModal({
        title: '退出登录',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            const userStore = useUserStore()
            userStore.logout()
            uni.showToast({ title: '已退出登录', icon: 'success' })
            setTimeout(() => {
              uni.switchTab({ url: '/pages/index/index' })
            }, 1500)
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.settings-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 88rpx;
  padding: 0 30rpx;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left,
.header-right {
  width: 60rpx;
}

.header-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #333;
}

/* 用户卡片 */
.user-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: 20rpx;
  padding: 30rpx;
  border-radius: 20rpx;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
}

.user-detail {
  margin-left: 20rpx;
}

.user-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
  display: block;
}

.user-id {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 8rpx;
  display: block;
}

/* 设置分组 */
.settings-group {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  overflow: hidden;
}

.group-title {
  font-size: 26rpx;
  color: #999;
  padding: 24rpx 30rpx 16rpx;
}

.group-list {
  padding: 0 30rpx;
}

.group-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.group-item:last-child {
  border-bottom: none;
}

.item-left {
  display: flex;
  align-items: center;
}

.item-icon {
  width: 44rpx;
  height: 44rpx;
  margin-right: 20rpx;
}

.item-name {
  font-size: 30rpx;
  color: #333;
}

.item-right {
  display: flex;
  align-items: center;
}

.item-value {
  font-size: 28rpx;
  color: #999;
  margin-right: 12rpx;
}

.item-value.version {
  color: #667eea;
}

.switch-wrapper {
  transform: scale(0.8);
}

/* 退出登录 */
.logout-section {
  margin: 40rpx 20rpx;
}

.btn-logout {
  width: 100%;
  height: 96rpx;
  background: #fff;
  border-radius: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #F56C6C;
  font-size: 32rpx;
  border: none;
}

.btn-logout::after {
  border: none;
}

/* 协议链接 */
.agreement-links {
  display: flex;
  justify-content: center;
  padding: 30rpx;
}

.agreement-links .link {
  font-size: 24rpx;
  color: #667eea;
}

.agreement-links .divider {
  color: #ccc;
  margin: 0 20rpx;
}
</style>
