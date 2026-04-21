<template>
  <view class="about-page">
    <!-- 页面标题 -->
    <view class="page-header">
      <view class="header-left" @click="goBack">
        <u-icon name="arrow-left" size="20" color="#333"></u-icon>
      </view>
      <text class="header-title">关于我们</text>
      <view class="header-right"></view>
    </view>

    <!-- Logo区域 -->
    <view class="logo-section">
      <image class="app-logo" src="/static/images/logo.png" mode="aspectFit"></image>
      <text class="app-name">拾艺局</text>
      <text class="app-version">v1.0.0</text>
    </view>

    <!-- 介绍 -->
    <view class="intro-section">
      <view class="intro-card">
        <text class="intro-text">
          拾艺局是一款高端艺术品社交电商平台，致力于为艺术品爱好者、收藏家和艺术家提供一个交流、交易、分享的平台。
        </text>
        <text class="intro-text">
          我们相信，艺术是人类精神的瑰宝，每一件艺术品都承载着艺术家的情感与智慧。拾艺局希望通过科技的力量，让艺术走进更多人的生活。
        </text>
      </view>
    </view>

    <!-- 功能特点 -->
    <view class="features-section">
      <view class="section-title">核心功能</view>
      <view class="feature-list">
        <view class="feature-item">
          <image class="feature-icon" src="/static/icons/feature-gallery.png" mode="aspectFit"></image>
          <view class="feature-content">
            <text class="feature-title">艺术画廊</text>
            <text class="feature-desc">汇聚国内外优秀艺术家作品，涵盖油画、雕塑、水墨等多种艺术门类</text>
          </view>
        </view>
        <view class="feature-item">
          <image class="feature-icon" src="/static/icons/feature-auction.png" mode="aspectFit"></image>
          <view class="feature-content">
            <text class="feature-title">精品拍卖</text>
            <text class="feature-desc">定期举办线上艺术品拍卖会，让艺术收藏更加便捷有趣</text>
          </view>
        </view>
        <view class="feature-item">
          <image class="feature-icon" src="/static/icons/feature-social.png" mode="aspectFit"></image>
          <view class="feature-content">
            <text class="feature-title">艺术社交</text>
            <text class="feature-desc">关注喜欢的艺术家，与志同道合的藏家交流心得</text>
          </view>
        </view>
        <view class="feature-item">
          <image class="feature-icon" src="/static/icons/feature-reward.png" mode="aspectFit"></image>
          <view class="feature-content">
            <text class="feature-title">分享赚佣</text>
            <text class="feature-desc">成为艺荐官，分享艺术作品，获得佣金收益</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 数据统计 -->
    <view class="stats-section">
      <view class="section-title">平台数据</view>
      <view class="stats-grid">
        <view class="stats-item">
          <text class="stats-value">10,000+</text>
          <text class="stats-label">注册用户</text>
        </view>
        <view class="stats-item">
          <text class="stats-value">5,000+</text>
          <text class="stats-label">艺术作品</text>
        </view>
        <view class="stats-item">
          <text class="stats-value">1,200+</text>
          <text class="stats-label">认证艺术家</text>
        </view>
        <view class="stats-item">
          <text class="stats-value">8,000+</text>
          <text class="stats-label">成交订单</text>
        </view>
      </view>
    </view>

    <!-- 合作机构 -->
    <view class="partner-section">
      <view class="section-title">合作伙伴</view>
      <view class="partner-list">
        <view class="partner-item">
          <text class="partner-name">中央美术学院</text>
        </view>
        <view class="partner-item">
          <text class="partner-name">中国美术馆</text>
        </view>
        <view class="partner-item">
          <text class="partner-name">今日美术馆</text>
        </view>
        <view class="partner-item">
          <text class="partner-name">UCCA尤伦斯</text>
        </view>
      </view>
    </view>

    <!-- 联系方式 -->
    <view class="contact-section">
      <view class="section-title">联系我们</view>
      <view class="contact-list">
        <view class="contact-item" @click="copyText('400-888-8888')">
          <image class="contact-icon" src="/static/icons/contact-phone.png" mode="aspectFit"></image>
          <view class="contact-content">
            <text class="contact-label">客服电话</text>
            <text class="contact-value">400-888-8888</text>
          </view>
          <view class="contact-action">
            <text>点击复制</text>
          </view>
        </view>
        <view class="contact-item" @click="copyText('service@shiyiju.com')">
          <image class="contact-icon" src="/static/icons/contact-email.png" mode="aspectFit"></image>
          <view class="contact-content">
            <text class="contact-label">邮箱地址</text>
            <text class="contact-value">service@shiyiju.com</text>
          </view>
          <view class="contact-action">
            <text>点击复制</text>
          </view>
        </view>
        <view class="contact-item" @click="openMap">
          <image class="contact-icon" src="/static/icons/contact-location.png" mode="aspectFit"></image>
          <view class="contact-content">
            <text class="contact-label">公司地址</text>
            <text class="contact-value">北京市朝阳区798艺术区</text>
          </view>
          <u-icon name="arrow-right" size="14" color="#ccc"></u-icon>
        </view>
      </view>
    </view>

    <!-- 关注我们 -->
    <view class="follow-section">
      <view class="section-title">关注我们</view>
      <view class="follow-list">
        <view class="follow-item" @click="openUrl('weixin')">
          <image class="follow-icon" src="/static/icons/wx.png" mode="aspectFit"></image>
          <text class="follow-name">微信公众号</text>
        </view>
        <view class="follow-item" @click="openUrl('weibo')">
          <image class="follow-icon" src="/static/icons/wb.png" mode="aspectFit"></image>
          <text class="follow-name">官方微博</text>
        </view>
        <view class="follow-item" @click="openUrl('douyin')">
          <image class="follow-icon" src="/static/icons/dy.png" mode="aspectFit"></image>
          <text class="follow-name">抖音号</text>
        </view>
      </view>
    </view>

    <!-- 底部信息 -->
    <view class="footer">
      <text class="footer-text">© 2024 拾艺局 All Rights Reserved</text>
      <text class="footer-text">京ICP备XXXXXXXX号</text>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {}
  },
  
  methods: {
    goBack() {
      uni.navigateBack()
    },
    
    copyText(text) {
      uni.setClipboardData({
        data: text,
        success: () => {
          uni.showToast({ title: '已复制到剪贴板', icon: 'success' })
        }
      })
    },
    
    openMap() {
      uni.openLocation({
        latitude: 39.983,
        longitude: 116.473,
        name: '拾艺局',
        address: '北京市朝阳区798艺术区'
      })
    },
    
    openUrl(type) {
      uni.showToast({ title: '即将开放', icon: 'none' })
    }
  }
}
</script>

<style scoped>
.about-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 60rpx;
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

/* Logo区域 */
.logo-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 80rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.app-logo {
  width: 160rpx;
  height: 160rpx;
  border-radius: 40rpx;
  background: rgba(255, 255, 255, 0.2);
}

.app-name {
  font-size: 48rpx;
  font-weight: 700;
  color: #fff;
  margin-top: 24rpx;
  letter-spacing: 8rpx;
}

.app-version {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 12rpx;
}

/* 介绍 */
.intro-section {
  padding: 30rpx;
}

.intro-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 40rpx;
}

.intro-text {
  font-size: 28rpx;
  color: #666;
  line-height: 1.8;
  display: block;
  text-align: justify;
}

.intro-text:last-child {
  margin-top: 24rpx;
}

/* 功能特点 */
.features-section {
  padding: 0 30rpx 30rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 24rpx;
}

.feature-list {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
}

.feature-item {
  display: flex;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.feature-item:last-child {
  border-bottom: none;
}

.feature-icon {
  width: 80rpx;
  height: 80rpx;
  margin-right: 24rpx;
}

.feature-content {
  flex: 1;
}

.feature-title {
  font-size: 30rpx;
  font-weight: 500;
  color: #333;
  display: block;
}

.feature-desc {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
  line-height: 1.6;
  display: block;
}

/* 数据统计 */
.stats-section {
  padding: 0 30rpx 30rpx;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
}

.stats-item {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  text-align: center;
}

.stats-value {
  font-size: 36rpx;
  font-weight: 700;
  color: #667eea;
  display: block;
}

.stats-label {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
  display: block;
}

/* 合作机构 */
.partner-section {
  padding: 0 30rpx 30rpx;
}

.partner-list {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.partner-item {
  background: #fff;
  border-radius: 12rpx;
  padding: 20rpx 30rpx;
}

.partner-name {
  font-size: 26rpx;
  color: #666;
}

/* 联系方式 */
.contact-section {
  padding: 0 30rpx 30rpx;
}

.contact-list {
  background: #fff;
  border-radius: 20rpx;
  padding: 0 30rpx;
}

.contact-item {
  display: flex;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.contact-item:last-child {
  border-bottom: none;
}

.contact-icon {
  width: 48rpx;
  height: 48rpx;
  margin-right: 20rpx;
}

.contact-content {
  flex: 1;
}

.contact-label {
  font-size: 24rpx;
  color: #999;
  display: block;
}

.contact-value {
  font-size: 28rpx;
  color: #333;
  display: block;
  margin-top: 4rpx;
}

.contact-action {
  padding: 8rpx 20rpx;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 20rpx;
}

.contact-action text {
  font-size: 22rpx;
  color: #667eea;
}

/* 关注我们 */
.follow-section {
  padding: 0 30rpx 30rpx;
}

.follow-list {
  display: flex;
  justify-content: space-around;
}

.follow-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.follow-icon {
  width: 80rpx;
  height: 80rpx;
  margin-bottom: 12rpx;
}

.follow-name {
  font-size: 24rpx;
  color: #666;
}

/* 底部信息 */
.footer {
  padding: 40rpx 30rpx;
  text-align: center;
}

.footer-text {
  font-size: 22rpx;
  color: #ccc;
  display: block;
}

.footer-text:last-child {
  margin-top: 8rpx;
}
</style>
