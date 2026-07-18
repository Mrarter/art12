<template>
  <view class="about-page">
    <view class="hero-card">
      <image class="app-logo" src="/static/logo.png" mode="aspectFit" />
      <text class="app-name">艺本艺术</text>
      <text class="app-version">版本 v{{ appVersion }}</text>
      <text class="app-desc">连接艺术家、收藏者与经纪人的原创艺术品展示与交易平台。</text>
    </view>

    <view class="section-card">
      <text class="section-title">平台简介</text>
      <text
        v-for="line in sloganLines"
        :key="line"
        class="section-text"
      >{{ line }}</text>
    </view>

    <view class="section-card">
      <text class="section-title">备案与合规</text>
      <view class="info-row">
        <text class="info-label">备案主体</text>
        <text class="info-value">孟儒（个人）</text>
      </view>
      <view class="info-row info-row-link" @click="openBeianSite">
        <text class="info-label">备案号</text>
        <view class="info-link-wrap">
          <text class="info-link">浙ICP备2026041466号-1</text>
          <text class="arrow">›</text>
        </view>
      </view>
      <text class="section-tip">点击备案号可前往工业和信息化部备案管理系统。</text>
    </view>

    <view class="section-card">
      <text class="section-title">协议与政策</text>
      <view class="menu-item" @click="goToAgreement('user')">
        <text class="menu-name">用户协议</text>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item" @click="goToAgreement('privacy')">
        <text class="menu-name">隐私政策</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="section-card">
      <text class="section-title">联系我们</text>
      <view class="info-row">
        <text class="info-label">客服电话</text>
        <text class="info-value">400-888-8888</text>
      </view>
      <view class="info-row">
        <text class="info-label">联系邮箱</text>
        <text class="info-value">service@shiyiju.com</text>
      </view>
      <view class="info-row">
        <text class="info-label">联系地址</text>
        <text class="info-value">杭州市余杭区景兴路896号</text>
      </view>
    </view>

    <view class="footer-section">
      <text class="copyright">© 2024 艺本艺术 All Rights Reserved</text>
    </view>
  </view>
</template>

<script>
import { APP_SLOGAN_LINES, APP_VERSION } from '@/constants/app.js'

const BEIAN_URL = 'https://beian.miit.gov.cn/'

export default {
  data() {
    return {
      appVersion: APP_VERSION,
      sloganLines: APP_SLOGAN_LINES
    }
  },
  methods: {
    goToAgreement(type) {
      uni.navigateTo({ url: `/pages/user-extra/agreement?type=${type}` })
    },
    openBeianSite() {
      // #ifdef H5
      window.open(BEIAN_URL, '_blank')
      return
      // #endif

      // #ifdef APP-PLUS
      plus.runtime.openURL(BEIAN_URL)
      return
      // #endif

      uni.setClipboardData({
        data: BEIAN_URL,
        success: () => {
          uni.showToast({ title: '备案网址已复制', icon: 'none' })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.about-page {
  min-height: 100vh;
  padding: 24rpx;
  background: #0d0d0d;
  box-sizing: border-box;
}

.hero-card,
.section-card {
  background: #171719;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
  border-radius: 20rpx;
  margin-bottom: 24rpx;
}

.hero-card {
  padding: 56rpx 36rpx;
  text-align: center;
  background: linear-gradient(135deg, #2b2414 0%, #181818 58%, #101010 100%);
}

.app-logo {
  width: 160rpx;
  height: 160rpx;
  margin: 0 auto;
  background: rgba(212, 175, 55, 0.12);
  border: 1rpx solid rgba(212, 175, 55, 0.25);
  border-radius: 32rpx;
}

.app-name {
  display: block;
  margin-top: 24rpx;
  font-size: 46rpx;
  font-weight: 700;
  color: #f7f2e7;
}

.app-version {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: rgba(247, 242, 231, 0.76);
}

.app-desc {
  display: block;
  margin-top: 18rpx;
  font-size: 26rpx;
  line-height: 1.8;
  color: #d4ccb9;
}

.section-card {
  padding: 28rpx 24rpx;
}

.section-title {
  display: block;
  margin-bottom: 20rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #f5f1e8;
}

.section-text,
.section-tip {
  display: block;
  font-size: 26rpx;
  line-height: 1.8;
  color: #b6afa3;
}

.section-tip {
  margin-top: 14rpx;
  color: #908a80;
}

.info-row,
.menu-item {
  min-height: 92rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.info-row:first-of-type,
.menu-item:first-of-type {
  border-top: none;
}

.info-label,
.menu-name {
  font-size: 28rpx;
  color: #f5f5f5;
}

.info-value {
  max-width: 60%;
  text-align: right;
  font-size: 25rpx;
  line-height: 1.6;
  color: #b6afa3;
}

.info-row-link {
  cursor: pointer;
}

.info-link-wrap {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.info-link {
  font-size: 25rpx;
  color: #d4af37;
}

.arrow {
  color: #7e776d;
  font-size: 30rpx;
}

.footer-section {
  padding: 18rpx 0 40rpx;
  text-align: center;
}

.copyright {
  font-size: 24rpx;
  color: #77716a;
}
</style>
