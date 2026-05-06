<template>
  <view class="page certificate-page">
    <!-- 自定义导航栏 -->
    <view class="custom-header">
      <view class="header-left" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <text class="header-title">收藏证书详情</text>
      <view class="header-right">
        <text class="more-icon">⋯</text>
      </view>
    </view>

    <scroll-view class="scroll-content" scroll-y>
      <!-- 证书卡面 -->
      <view class="certificate-card">
        <view class="cert-badge">
          <text class="cert-badge-icon">✦</text>
        </view>
        <text class="cert-title">艺术品收藏证书</text>
        <text class="cert-number">编号：{{ certInfo.certNo }}</text>
        <view class="cert-divider"></view>
        <view class="cert-body">
          <view class="cert-artwork">
            <image class="cert-artwork-img" :src="certInfo.artworkCover" mode="aspectFill" />
          </view>
          <text class="cert-artwork-name">{{ certInfo.artworkName }}</text>
          <text class="cert-artist">{{ certInfo.artistName }}</text>
          <view class="cert-meta">
            <view class="cert-meta-item">
              <text class="meta-label">尺寸</text>
              <text class="meta-value">{{ certInfo.size }}</text>
            </view>
            <view class="cert-meta-item">
              <text class="meta-label">创作年份</text>
              <text class="meta-value">{{ certInfo.year }}</text>
            </view>
            <view class="cert-meta-item">
              <text class="meta-label">材料</text>
              <text class="meta-value">{{ certInfo.material }}</text>
            </view>
          </view>
        </view>
        <view class="cert-footer-text">
          <text class="cert-issuer">{{ certInfo.issuer }}</text>
          <text class="cert-date">签发日期：{{ certInfo.issueDate }}</text>
        </view>
      </view>

      <!-- 证书信息卡 -->
      <view class="info-card">
        <view class="info-row">
          <text class="info-label">当前持有人</text>
          <text class="info-value">{{ certInfo.holder }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">作品状态</text>
          <text class="info-value gold">{{ certInfo.statusText }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">收藏时间</text>
          <text class="info-value">{{ certInfo.collectDate }}</text>
        </view>
      </view>

      <!-- 区块链信息 -->
      <view class="info-card">
        <view class="blockchain-row">
          <text class="blockchain-icon">🔗</text>
          <view class="blockchain-info">
            <text class="blockchain-title">区块链存证</text>
            <text class="blockchain-hash" @click="copyHash">{{ certInfo.blockchainHash }}</text>
          </view>
          <text class="copy-btn" @click="copyHash">复制</text>
        </view>
      </view>

      <view style="height: 160rpx"></view>
    </scroll-view>

    <!-- 底部操作栏 -->
    <view class="footer-bar">
      <button class="btn primary" @click="downloadCertificate">下载证书</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCertDetail } from '@/api/order'

const certInfo = ref({
  certNo: '',
  artworkName: '',
  artistName: '',
  artworkCover: '',
  size: '',
  year: '',
  material: '',
  issuer: '拾艺局·艺术品认证中心',
  issueDate: '',
  holder: '',
  statusText: '',
  collectDate: '',
  blockchainHash: ''
})

onMounted(async () => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const id = currentPage.options?.id
  if (id) {
    try {
      const res = await getCertDetail(id)
      const data = res?.data || res
      if (data) {
        certInfo.value = {
          certNo: data.cert_no || data.certNo || data.certificateNo || '',
          artworkName: data.artwork_name || data.artworkName || '',
          artistName: data.artist_name || data.artistName || '',
          artworkCover: data.artwork_cover || data.artworkCover || data.cover || '',
          size: data.size || '',
          year: data.year || data.createYear || '',
          material: data.material || '',
          issuer: data.issuer || '拾艺局·艺术品认证中心',
          issueDate: data.issue_date || data.issueDate || '',
          holder: data.holder_name || data.holderName || data.holder || '',
          statusText: data.status === 'ISSUED' ? '已认证 · 流通中' : (data.statusText || ''),
          collectDate: data.collect_date || data.collectDate || data.issueDate || '',
          blockchainHash: data.blockchain_hash || data.blockchainHash || ''
        }
      }
    } catch (e) {
      console.error('加载证书详情失败', e)
    }
  }
})

const goBack = () => {
  uni.navigateBack()
}

const copyHash = () => {
  uni.setClipboardData({
    data: certInfo.value.blockchainHash,
    success: () => uni.showToast({ title: '已复制', icon: 'none' })
  })
}

const downloadCertificate = () => {
  uni.showLoading({ title: '生成中...' })
  // TODO: 调用后端生成证书图片并下载
  setTimeout(() => {
    uni.hideLoading()
    uni.showToast({ title: '证书已保存到相册', icon: 'none' })
  }, 1500)
}
</script>

<style scoped>
.certificate-page {
  min-height: 100vh;
  background: #0b0b0b;
  color: #fff;
}
.gold { color: #e6c38a; }

.custom-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 100rpx 24rpx 20rpx;
  height: 100rpx;
}
.header-left, .header-right { width: 80rpx; }
.header-title { font-size: 30rpx; font-weight: 600; text-align: center; flex: 1; }
.back-icon, .more-icon { font-size: 36rpx; }

.scroll-content { padding: 0 24rpx; }

/* 证书卡面 */
.certificate-card {
  margin-bottom: 24rpx;
  padding: 48rpx 32rpx 32rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #f5e6c8, #e6c38a, #d4af37);
  display: flex;
  flex-direction: column;
  align-items: center;
}
.cert-badge {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
}
.cert-badge-icon { font-size: 40rpx; color: #8b7500; }
.cert-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #4a3a00;
  letter-spacing: 4rpx;
}
.cert-number {
  font-size: 22rpx;
  color: #6b5a00;
  margin-top: 12rpx;
}
.cert-divider {
  width: 60%;
  height: 1px;
  background: rgba(74, 58, 0, 0.2);
  margin: 28rpx 0;
}
.cert-body { width: 100%; }
.cert-artwork {
  width: 100%;
  height: 320rpx;
  border-radius: 16rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
}
.cert-artwork-img { width: 100%; height: 100%; }
.cert-artwork-name {
  font-size: 34rpx;
  font-weight: 600;
  color: #3a2a00;
  text-align: center;
  display: block;
}
.cert-artist {
  font-size: 26rpx;
  color: #5a4a10;
  text-align: center;
  display: block;
  margin-top: 8rpx;
}
.cert-meta {
  display: flex;
  justify-content: center;
  gap: 40rpx;
  margin-top: 24rpx;
}
.cert-meta-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.meta-label { font-size: 20rpx; color: #8a7a30; }
.meta-value { font-size: 24rpx; color: #3a2a00; font-weight: 500; margin-top: 4rpx; }
.cert-footer-text {
  margin-top: 28rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.cert-issuer { font-size: 22rpx; color: #6b5a00; }
.cert-date { font-size: 20rpx; color: #8a7a30; margin-top: 4rpx; }

/* 信息卡片 */
.info-card {
  background: #141414;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
}
.info-row + .info-row {
  border-top: 1px solid #222;
}
.info-label { font-size: 26rpx; color: #999; }
.info-value { font-size: 26rpx; color: #fff; }

/* 区块链 */
.blockchain-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.blockchain-icon { font-size: 36rpx; }
.blockchain-info { flex: 1; }
.blockchain-title { font-size: 26rpx; color: #fff; display: block; }
.blockchain-hash { font-size: 22rpx; color: #666; word-break: break-all; margin-top: 6rpx; display: block; }
.copy-btn {
  font-size: 24rpx;
  color: #e6c38a;
  padding: 8rpx 20rpx;
  border: 1px solid #e6c38a;
  border-radius: 30rpx;
  flex-shrink: 0;
}

/* 底部 */
.footer-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 24rpx 40rpx;
  background: #0b0b0b;
}
.btn {
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 28rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}
.btn.primary {
  background: #e6c38a;
  color: #000;
  border: none;
}
</style>
