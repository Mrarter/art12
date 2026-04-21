<template>
  <view class="settings-page">
    <!-- 账号安全 -->
    <view class="settings-section">
      <text class="section-title">账号安全</text>
      <view class="setting-item" @click="changePassword">
        <text class="item-name">修改密码</text>
        <view class="item-right">
          <text class="arrow">></text>
        </view>
      </view>
      <view class="setting-item" @click="bindPhone">
        <text class="item-name">绑定手机</text>
        <view class="item-right">
          <text class="item-value">138****5678</text>
          <text class="arrow">></text>
        </view>
      </view>
      <view class="setting-item" @click="bindWechat">
        <text class="item-name">绑定微信</text>
        <view class="item-right">
          <text class="status-bind">已绑定</text>
          <text class="arrow">></text>
        </view>
      </view>
    </view>

    <!-- 偏好设置 -->
    <view class="settings-section">
      <text class="section-title">偏好设置</text>
      <view class="setting-item">
        <text class="item-name">消息推送</text>
        <view class="item-right">
          <switch checked color="#667eea" />
        </view>
      </view>
      <view class="setting-item">
        <text class="item-name">允许查看收藏</text>
        <view class="item-right">
          <switch checked color="#667eea" />
        </view>
      </view>
      <view class="setting-item">
        <text class="item-name">允许被推荐</text>
        <view class="item-right">
          <switch checked color="#667eea" />
        </view>
      </view>
    </view>

    <!-- 缓存与存储 -->
    <view class="settings-section">
      <text class="section-title">缓存与存储</text>
      <view class="setting-item" @click="clearCache">
        <text class="item-name">清除缓存</text>
        <view class="item-right">
          <text class="item-value">23.5 MB</text>
          <text class="arrow">></text>
        </view>
      </view>
      <view class="setting-item" @click="viewStorage">
        <text class="item-name">存储空间</text>
        <view class="item-right">
          <text class="item-value">已用 1.2 GB</text>
          <text class="arrow">></text>
        </view>
      </view>
    </view>

    <!-- 关于 -->
    <view class="settings-section">
      <text class="section-title">关于</text>
      <view class="setting-item" @click="goToAbout">
        <text class="item-name">关于我们</text>
        <view class="item-right">
          <text class="arrow">></text>
        </view>
      </view>
      <view class="setting-item" @click="checkUpdate">
        <text class="item-name">检查更新</text>
        <view class="item-right">
          <text class="item-value version">v1.0.0</text>
          <text class="arrow">></text>
        </view>
      </view>
      <view class="setting-item" @click="goToPrivacy">
        <text class="item-name">隐私政策</text>
        <view class="item-right">
          <text class="arrow">></text>
        </view>
      </view>
      <view class="setting-item" @click="goToTerms">
        <text class="item-name">用户协议</text>
        <view class="item-right">
          <text class="arrow">></text>
        </view>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-btn" @click="logout">
      <text class="btn-text">退出登录</text>
    </view>

    <!-- 版本信息 -->
    <view class="version-info">
      <text class="version-text">拾艺局 v1.0.0</text>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {}
  },
  methods: {
    changePassword() {
      uni.navigateTo({ url: '/pages/user/change-password' })
    },
    bindPhone() {
      uni.navigateTo({ url: '/pages/user/bind-phone' })
    },
    bindWechat() {
      uni.showToast({ title: '微信已绑定', icon: 'success' })
    },
    clearCache() {
      uni.showModal({
        title: '清除缓存',
        content: '确定清除所有缓存数据？',
        success: (res) => {
          if (res.confirm) {
            uni.showToast({ title: '已清除', icon: 'success' })
          }
        }
      })
    },
    viewStorage() {
      uni.showToast({ title: '存储详情', icon: 'none' })
    },
    goToAbout() {
      uni.navigateTo({ url: '/pages/about/index' })
    },
    checkUpdate() {
      uni.showToast({ title: '已是最新版本', icon: 'success' })
    },
    goToPrivacy() {
      uni.navigateTo({ url: '/pages/user/agreement?type=privacy' })
    },
    goToTerms() {
      uni.navigateTo({ url: '/pages/user/agreement?type=terms' })
    },
    logout() {
      uni.showModal({
        title: '退出登录',
        content: '确定退出当前账号？',
        success: (res) => {
          if (res.confirm) {
            uni.reLaunch({ url: '/pages/login/index' })
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
  background: #f5f5f5;
  padding: 24rpx;
  padding-bottom: 150rpx;
}

.settings-section {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 24rpx;
  overflow: hidden;
}

.section-title {
  font-size: 26rpx;
  color: #999;
  padding: 20rpx 24rpx;
  background: #fafafa;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 24rpx;
  border-bottom: 2rpx solid #f5f5f5;
}

.setting-item:last-child {
  border-bottom: none;
}

.item-name {
  font-size: 28rpx;
  color: #333;
}

.item-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.item-value {
  font-size: 26rpx;
  color: #999;
}

.item-value.version {
  color: #667eea;
}

.status-bind {
  font-size: 24rpx;
  color: #52c41a;
  background: #f6ffed;
  padding: 4rpx 12rpx;
  border-radius: 4rpx;
}

.arrow {
  color: #ccc;
  font-size: 28rpx;
}

.logout-btn {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  text-align: center;
  margin-bottom: 24rpx;
}

.btn-text {
  font-size: 32rpx;
  color: #e63946;
}

.version-info {
  text-align: center;
}

.version-text {
  font-size: 24rpx;
  color: #ccc;
}
</style>