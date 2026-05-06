<template>
  <view class="page circulation-page">
    <!-- 自定义导航栏 -->
    <view class="custom-header">
      <view class="header-left" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <text class="header-title">流通记录详情</text>
      <view class="header-right">
        <text class="more-icon">⋯</text>
      </view>
    </view>

    <scroll-view class="scroll-content" scroll-y>
      <!-- 作品信息 -->
      <view class="artwork-card">
        <image class="artwork-cover" :src="circulation.artworkCover" mode="aspectFill" />
        <view class="artwork-info">
          <text class="artwork-name gold">{{ circulation.artworkName }}</text>
          <text class="artwork-artist">{{ circulation.artistName }}</text>
          <view class="artwork-tags">
            <text class="tag">{{ circulation.size }}</text>
            <text class="tag">{{ circulation.year }}</text>
          </view>
        </view>
      </view>

      <!-- 持有人状态 -->
      <view class="status-card">
        <view class="status-row">
          <text class="status-label">当前持有人</text>
          <text class="status-value">{{ circulation.currentHolder }}</text>
        </view>
        <view class="status-row">
          <text class="status-label">流通次数</text>
          <text class="status-value gold">{{ circulation.circulationCount }} 次</text>
        </view>
        <view class="status-row">
          <text class="status-label">首次流通</text>
          <text class="status-value">{{ circulation.firstDate }}</text>
        </view>
        <view class="status-row">
          <text class="status-label">最近流通</text>
          <text class="status-value">{{ circulation.lastDate }}</text>
        </view>
      </view>

      <!-- 流通时间线 -->
      <view class="timeline-card">
        <text class="section-title">流通记录</text>
        <view class="timeline">
          <view
            v-for="(record, index) in circulation.history"
            :key="index"
            class="timeline-item"
          >
            <view class="timeline-dot" :class="{ active: index === 0 }"></view>
            <view class="timeline-content">
              <text class="timeline-event">{{ record.event }}</text>
              <text class="timeline-date">{{ record.date }}</text>
              <text class="timeline-desc" v-if="record.description">
                {{ record.description }}
              </text>
            </view>
          </view>
        </view>
      </view>

      <view style="height: 180rpx"></view>
    </scroll-view>

    <!-- 底部操作栏 -->
    <view class="footer-bar">
      <button class="btn secondary" @click="changeCustody">变更保管</button>
      <button class="btn primary" @click="recirculate">再次流通</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const circulation = ref({
  artworkName: '',
  artistName: '',
  artworkCover: '',
  size: '',
  year: '',
  currentHolder: '',
  circulationCount: 0,
  firstDate: '',
  lastDate: '',
  history: []
})

onMounted(async () => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const id = currentPage.options?.id || currentPage.options?.artworkId
  if (id) {
    try {
      // 使用订单数据构建流通记录
      const { getProductDetail } = await import('@/api/product')
      const product = await getProductDetail(id)
      if (product) {
        const data = product.data || product
        circulation.value = {
          artworkName: data.title || data.artworkName || '',
          artistName: data.authorName || data.artistName || '',
          artworkCover: data.cover || data.coverImage || data.coverUrl || '',
          size: data.size || '',
          year: data.year || data.createYear || '',
          currentHolder: data.holderName || data.holder || '未知',
          circulationCount: data.circulationCount || data.saleCount || 0,
          firstDate: data.firstSaleDate || '',
          lastDate: data.lastSaleDate || (data.updatedAt || '').slice(0, 10),
          history: (data.circulationHistory || []).map(h => ({
            event: h.event || '流通',
            date: (h.date || '').slice(0, 10),
            description: h.description || ''
          }))
        }
      }
    } catch (e) {
      console.error('加载流通记录失败', e)
    }
  }
})

const goBack = () => {
  uni.navigateBack()
}

const changeCustody = () => {
  uni.showActionSheet({
    itemList: ['申请实物保管', '申请数字保管', '委托平台保管'],
    success: (res) => {
      uni.showToast({
        title: ['已提交实物保管申请', '已提交数字保管申请', '已提交委托保管申请'][res.tapIndex],
        icon: 'none'
      })
    }
  })
}

const recirculate = () => {
  uni.navigateTo({
    url: '/pages/order/intent?mode=recirculate'
  })
}
</script>

<style scoped>
.circulation-page {
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

/* 作品信息卡片 */
.artwork-card {
  background: #141414;
  border-radius: 20rpx;
  overflow: hidden;
  margin-bottom: 20rpx;
}
.artwork-cover {
  width: 100%;
  height: 360rpx;
  display: block;
}
.artwork-info { padding: 24rpx; }
.artwork-name { font-size: 34rpx; font-weight: 600; display: block; }
.artwork-artist { font-size: 26rpx; color: #999; margin-top: 8rpx; display: block; }
.artwork-tags { display: flex; gap: 12rpx; margin-top: 16rpx; }
.tag {
  padding: 6rpx 18rpx;
  background: #222;
  border-radius: 20rpx;
  font-size: 22rpx;
  color: #ccc;
}

/* 状态卡片 */
.status-card, .timeline-card {
  background: #141414;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}
.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14rpx 0;
}
.status-row + .status-row { border-top: 1px solid #222; }
.status-label { font-size: 26rpx; color: #999; }
.status-value { font-size: 26rpx; color: #fff; }

/* 时间线 */
.section-title {
  font-size: 28rpx;
  font-weight: 600;
  margin-bottom: 24rpx;
  display: block;
}
.timeline { position: relative; }
.timeline::before {
  content: '';
  position: absolute;
  left: 10rpx;
  top: 16rpx;
  bottom: 16rpx;
  width: 2px;
  background: #333;
}
.timeline-item {
  display: flex;
  gap: 24rpx;
  padding-bottom: 32rpx;
  position: relative;
}
.timeline-item:last-child { padding-bottom: 0; }
.timeline-dot {
  width: 22rpx;
  height: 22rpx;
  border-radius: 50%;
  background: #333;
  flex-shrink: 0;
  margin-top: 8rpx;
  position: relative;
  z-index: 1;
}
.timeline-dot.active { background: #e6c38a; }
.timeline-content { flex: 1; }
.timeline-event { font-size: 28rpx; font-weight: 500; display: block; }
.timeline-date { font-size: 22rpx; color: #666; margin-top: 4rpx; display: block; }
.timeline-desc { font-size: 24rpx; color: #999; margin-top: 8rpx; display: block; }

/* 底部 */
.footer-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 24rpx 40rpx;
  background: #0b0b0b;
  display: flex;
  gap: 20rpx;
}
.btn {
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 28rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
}
.btn.primary {
  background: #e6c38a;
  color: #000;
  border: none;
}
.btn.secondary {
  background: #1f1f1f;
  color: #e6c38a;
  border: 1px solid #e6c38a;
}
</style>
