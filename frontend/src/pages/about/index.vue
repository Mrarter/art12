<template>
  <view class="about-page">
    <!-- Logo区域 -->
    <view class="logo-section">
      <image class="app-logo" src="/static/logo.png" mode="aspectFit" />
      <text class="app-name">拾艺局</text>
      <text class="app-version">v1.0.0</text>
    </view>

    <!-- 功能列表 -->
    <view class="menu-section">
      <view class="menu-item" @click="goToAbout">
        <text class="menu-name">关于我们</text>
        <text class="arrow">></text>
      </view>
      <view class="menu-item" @click="checkUpdate">
        <text class="menu-name">检查更新</text>
        <view class="menu-right">
          <text class="version-text">已是最新版本</text>
          <text class="arrow">></text>
        </view>
      </view>
      <view class="menu-item" @click="goToPrivacy">
        <text class="menu-name">隐私政策</text>
        <text class="arrow">></text>
      </view>
      <view class="menu-item" @click="goToTerms">
        <text class="menu-name">用户协议</text>
        <text class="arrow">></text>
      </view>
      <view class="menu-item" @click="goToLicenses">
        <text class="menu-name">开源许可</text>
        <text class="arrow">></text>
      </view>
    </view>

    <!-- 联系方式 -->
    <view class="contact-section">
      <text class="section-title">联系我们</text>
      <view class="contact-item">
        <image class="contact-icon" src="/static/icon-phone.png" mode="aspectFit" />
        <text class="contact-text">400-888-8888</text>
      </view>
      <view class="contact-item">
        <image class="contact-icon" src="/static/icon-email.png" mode="aspectFit" />
        <text class="contact-text">service@shiyiju.com</text>
      </view>
      <view class="contact-item">
        <image class="contact-icon" src="/static/icon-location.png" mode="aspectFit" />
        <text class="contact-text">北京市朝阳区艺术大道88号</text>
      </view>
    </view>

    <!-- 底部信息 -->
    <view class="footer-section">
      <text class="copyright">© 2024 拾艺局 All Rights Reserved</text>
      <text class="desc">让艺术走进生活</text>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {}
  },
  methods: {
    goToAbout() {
      uni.navigateTo({ url: '/pages/common/coming-soon?title=关于拾艺局&desc=详细介绍页还在整理中，当前版本先保留入口与基础信息。' })
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
    goToLicenses() {
      uni.navigateTo({ url: '/pages/common/coming-soon?title=开源许可&desc=许可清单页还在整理中，后续会补齐第三方依赖说明。' })
    }
  }
}
</script>

<style lang="scss" scoped>
.about-page {
  min-height: 100vh;
  background: #0d0d0d;
}

.logo-section {
  background: linear-gradient(135deg, #2b2414 0%, #181818 58%, #0d0d0d 100%);
  color: #f7f2e7;
  padding: 96rpx 0 78rpx;
  text-align: center;
  border-bottom: 1rpx solid rgba(212, 175, 55, 0.18);
}

.app-logo {
  width: 160rpx;
  height: 160rpx;
  background: rgba(212, 175, 55, 0.12);
  border: 1rpx solid rgba(212, 175, 55, 0.25);
  border-radius: 32rpx;
}

.app-name {
  font-size: 48rpx;
  font-weight: 700;
  display: block;
  margin-top: 24rpx;
}

.app-version {
  font-size: 28rpx;
  opacity: 0.8;
  display: block;
  margin-top: 8rpx;
}

.menu-section {
  background: #1a1a1a;
  margin: 24rpx;
  border-radius: 18rpx;
  overflow: hidden;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx 24rpx;
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.06);
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-name {
  font-size: 28rpx;
  color: #f5f5f5;
}

.menu-right {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.version-text {
  font-size: 24rpx;
  color: #a0a0a0;
}

.arrow {
  color: #d4af37;
  font-size: 28rpx;
}

.contact-section {
  background: #1a1a1a;
  margin: 24rpx;
  border-radius: 16rpx;
  padding: 30rpx 24rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #d4af37;
  display: block;
  margin-bottom: 20rpx;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 0;
}

.contact-icon {
  width: 40rpx;
  height: 40rpx;
  background: rgba(212, 175, 55, 0.12);
  border-radius: 8rpx;
}

.contact-text {
  font-size: 28rpx;
  color: #b3b3b3;
}

.footer-section {
  text-align: center;
  padding: 60rpx 0;
}

.copyright {
  font-size: 24rpx;
  color: #777;
  display: block;
}

.desc {
  font-size: 24rpx;
  color: #bbb;
  margin-top: 8rpx;
  display: block;
}
</style>
