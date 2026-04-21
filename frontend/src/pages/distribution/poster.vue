<template>
  <view class="poster-page">
    <!-- 模板选择 -->
    <view class="template-section">
      <text class="section-title">选择海报模板</text>
      <scroll-view class="template-scroll" scroll-x>
        <view class="template-item" 
              v-for="(template, index) in templates" 
              :key="index"
              :class="{ active: selectedTemplate === index }"
              @click="selectTemplate(index)">
          <image class="template-img" :src="template.preview" mode="aspectFill" />
          <text class="template-name">{{ template.name }}</text>
        </view>
      </scroll-view>
    </view>

    <!-- 海报预览 -->
    <view class="preview-section">
      <view class="poster-preview">
        <image class="poster-bg" :src="currentTemplate.bg" mode="aspectFill" />
        <view class="poster-content">
          <!-- Logo -->
          <image class="logo" :src="currentTemplate.logo" mode="aspectFit" />
          <!-- 标题 -->
          <text class="title">{{ currentTemplate.title }}</text>
          <!-- 二维码区域 -->
          <view class="qrcode-area">
            <image class="qrcode" :src="qrcodeUrl" mode="aspectFit" />
            <text class="qrcode-hint">扫码关注领取优惠券</text>
          </view>
          <!-- 邀请码 -->
          <view class="invite-code-box">
            <text class="label">邀请码</text>
            <text class="code">ART2024</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="action-buttons">
      <view class="btn-item" @click="savePoster">
        <text class="btn-text">保存海报</text>
      </view>
      <view class="btn-item primary" @click="sharePoster">
        <text class="btn-text">分享海报</text>
      </view>
    </view>

    <!-- 分享提示 -->
    <view class="share-tip">
      <text>分享海报给好友，好友扫码关注后成为您的客户</text>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      selectedTemplate: 0,
      qrcodeUrl: '/static/qrcode-placeholder.png',
      templates: [
        {
          name: '经典雅致',
          preview: '/static/poster-classic.png',
          bg: '/static/poster-bg-classic.jpg',
          logo: '/static/logo.png',
          title: '艺12 - 让艺术走进生活'
        },
        {
          name: '高端奢华',
          preview: '/static/poster-luxury.png',
          bg: '/static/poster-bg-luxury.jpg',
          logo: '/static/logo-gold.png',
          title: '艺12 - 品味艺术人生'
        },
        {
          name: '简约现代',
          preview: '/static/poster-modern.png',
          bg: '/static/poster-bg-modern.jpg',
          logo: '/static/logo-dark.png',
          title: '艺12 - 发现艺术之美'
        },
        {
          name: '节日限定',
          preview: '/static/poster-festival.png',
          bg: '/static/poster-bg-festival.jpg',
          logo: '/static/logo-festival.png',
          title: '艺12 - 艺术嘉年华'
        }
      ]
    }
  },
  computed: {
    currentTemplate() {
      return this.templates[this.selectedTemplate] || this.templates[0]
    }
  },
  methods: {
    selectTemplate(index) {
      this.selectedTemplate = index
    },
    savePoster() {
      uni.showToast({ title: '保存成功', icon: 'success' })
    },
    sharePoster() {
      uni.showToast({ title: '分享功能开发中', icon: 'none' })
    }
  }
}
</script>

<style lang="scss" scoped>
.poster-page {
  min-height: 100vh;
  background: #1a1a2e;
  padding-bottom: 200rpx;
}

.template-section {
  padding: 30rpx;
}

.section-title {
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  display: block;
  margin-bottom: 20rpx;
}

.template-scroll {
  white-space: nowrap;
}

.template-item {
  display: inline-block;
  width: 180rpx;
  margin-right: 20rpx;
  text-align: center;
}

.template-img {
  width: 180rpx;
  height: 260rpx;
  border-radius: 16rpx;
  border: 4rpx solid transparent;
}

.template-item.active .template-img {
  border-color: #667eea;
}

.template-name {
  color: #fff;
  font-size: 24rpx;
  margin-top: 12rpx;
  display: block;
}

.preview-section {
  padding: 0 30rpx;
}

.poster-preview {
  position: relative;
  width: 100%;
  aspect-ratio: 0.7;
  border-radius: 20rpx;
  overflow: hidden;
}

.poster-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.poster-content {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 40rpx;
}

.logo {
  width: 120rpx;
  height: 60rpx;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8rpx;
}

.title {
  color: #fff;
  font-size: 36rpx;
  font-weight: 600;
  margin-top: 40rpx;
  text-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.3);
}

.qrcode-area {
  margin-top: auto;
  text-align: center;
}

.qrcode {
  width: 200rpx;
  height: 200rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 10rpx;
}

.qrcode-hint {
  color: #fff;
  font-size: 24rpx;
  margin-top: 16rpx;
  display: block;
}

.invite-code-box {
  margin-top: 30rpx;
  background: rgba(255, 255, 255, 0.2);
  padding: 16rpx 40rpx;
  border-radius: 30rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.invite-code-box .label {
  color: rgba(255, 255, 255, 0.8);
  font-size: 24rpx;
}

.invite-code-box .code {
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
}

.action-buttons {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #1a1a2e;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  display: flex;
  gap: 20rpx;
}

.btn-item {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: #333;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 44rpx;
}

.btn-item.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.share-tip {
  position: fixed;
  bottom: 120rpx;
  left: 0;
  right: 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
  font-size: 24rpx;
}
</style>