<template>
  <view class="works-page">
    <view class="topbar">
      <text class="back" @click="goBack">‹</text>
      <text class="title">艺术家全部作品</text>
      <text class="spacer"></text>
    </view>

    <!-- 加载中 -->
    <view v-if="loading" class="status-box">
      <view class="loading-spinner"></view>
      <text class="status-text">加载中...</text>
    </view>

    <!-- 加载失败 -->
    <view v-else-if="error" class="status-box">
      <text class="status-icon">⚠</text>
      <text class="status-text error">{{ error }}</text>
      <text class="retry-btn" @click="loadData">点击重试</text>
    </view>

    <!-- 数据加载完成 -->
    <template v-else>
      <!-- 艺术家信息条 -->
      <view class="artist-strip">
        <image class="avatar" :src="artistAvatar" mode="aspectFill"></image>
        <view>
          <text class="artist-name">{{ artistName }}</text>
          <text class="artist-desc">{{ works.length }} 件作品正在平台展示与流通</text>
        </view>
      </view>

      <scroll-view scroll-x class="tabs-scroll">
        <view class="tabs">
          <text
            v-for="tab in tabs"
            :key="tab.value"
            class="tab"
            :class="{ active: activeTab === tab.value }"
            @click="activeTab = tab.value"
          >{{ tab.label }}</text>
        </view>
      </scroll-view>

      <!-- 作品网格 -->
      <view v-if="filteredWorks.length > 0" class="works-grid">
        <view
          v-for="work in filteredWorks"
          :key="work.id"
          class="work-card"
          @click="goWork(work.id)"
        >
          <image class="work-image" :src="work.cover" mode="aspectFill"></image>
          <view class="work-body">
            <view class="work-line">
              <text class="work-title">{{ work.title }}</text>
              <text class="status-label">{{ work.statusLabel }}</text>
            </view>
            <text class="work-meta">{{ work.metaText }}</text>
            <text class="price">{{ work.priceText }}</text>
          </view>
        </view>
      </view>

      <!-- 空数据 -->
      <view v-else class="status-box">
        <text class="status-icon">📭</text>
        <text class="status-text">暂无作品</text>
      </view>
    </template>
  </view>
</template>

<script>
import { getProductList } from '@/api/product'
import { getArtistInfo } from '@/api/user'
import { formatArtworkPriceNumber } from '@/utils/price'
import { getFullImageUrl } from '@/utils/image'

export default {
  data() {
    return {
      artistId: null,
      activeTab: 'ALL',
      tabs: [
        { label: '全部', value: 'ALL' },
        { label: '可喜欢', value: 'ON_SALE' },
        { label: '已喜欢', value: 'COLLECTED' },
        { label: '再次流通', value: 'CAN_APPLY' },
        { label: '代表作', value: 'FEATURED' }
      ],
      works: [],
      artistInfo: null,
      loading: true,
      error: ''
    }
  },
  computed: {
    artistName() {
      return this.artistInfo?.nickname || this.artistInfo?.name || '艺术家'
    },
    artistAvatar() {
      return this.artistInfo?.avatar || '/static/images/avatar.png'
    },
    filteredWorks() {
      if (this.activeTab === 'ALL') return this.works
      return this.works.filter(w => this.matchTab(w, this.activeTab))
    }
  },
  onLoad(query) {
    this.artistId = query.id
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      this.error = ''
      try {
        const info = await getArtistInfo(this.artistId)
        this.artistInfo = info || null
        const artistName = info?.nickname || info?.name
        if (artistName) {
          const res = await getProductList({ authorName: artistName, pageSize: 100 })
          const records = res?.records || []
          this.works = records.map(this.normalizeWork)
        }
      } catch (e) {
        console.warn('[artist/works] 加载失败:', e)
        this.error = '加载失败，请检查网络后重试'
      } finally {
        this.loading = false
      }
    },
    normalizeWork(item) {
      const statusMap = {
        0: '已下架',
        1: '可收藏',
        2: '已售罄'
      }
      const label = statusMap[item.status] || '未知'
      const metaParts = [item.material, item.size, item.year].filter(Boolean)
      return {
        ...item,
        cover: getFullImageUrl(item.coverImage || item.cover || '', '/static/images/artwork-fallback.png'),
        statusLabel: label,
        metaText: metaParts.join(' / '),
        priceText: item.price ? `¥${formatArtworkPriceNumber(item.price)}` : '待估值'
      }
    },
    matchTab(work, tab) {
      switch (tab) {
        case 'ON_SALE':
          return work.status === 1
        case 'COLLECTED':
          return work.isFavorite || work.isFavorited
        case 'CAN_APPLY':
          return work.status === 1 && work.ownershipType === 2
        case 'FEATURED':
          return !!work.authorBadge
        default:
          return true
      }
    },
    switchTab(value) {
      this.activeTab = value
    },
    goBack() {
      uni.navigateBack()
    },
    goWork(id) {
      uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` })
    }
  }
}
</script>

<style lang="scss" scoped>
$page-bg: #050505;
$card-bg: #171717;
$gold: #d6a827;
$gold-light: #f1c84b;
$text-main: #fff;
$text-secondary: #b7b7b7;
$text-muted: #7d7d7d;
$border-dark: rgba(214, 168, 39, 0.35);

.works-page {
  min-height: 100vh;
  padding: 0 28rpx 50rpx;
  background: $page-bg;
  color: $text-main;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 98rpx;
}

.back,
.spacer {
  width: 72rpx;
}

.back {
  font-size: 44rpx;
}

.title {
  font-size: 32rpx;
  font-weight: 700;
}

/* ===== 艺术家信息条 ===== */
.artist-strip {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx;
  border: 1rpx solid $border-dark;
  border-radius: 28rpx;
  background: radial-gradient(circle at 0 0, rgba(214, 168, 39, 0.18), transparent 45%), $card-bg;
}

.avatar {
  width: 92rpx;
  height: 92rpx;
  border-radius: 50%;
}

.artist-name,
.artist-desc {
  display: block;
}

.artist-name {
  font-size: 34rpx;
  font-weight: 700;
}

.artist-desc,
.work-meta {
  color: $text-secondary;
  font-size: 24rpx;
  line-height: 36rpx;
}

/* ===== 加载 / 错误 / 空状态 ===== */
.status-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 40rpx;
}

.status-icon {
  font-size: 60rpx;
  margin-bottom: 20rpx;
}

.status-text {
  font-size: 26rpx;
  color: $text-muted;
  text-align: center;
  line-height: 40rpx;
}

.status-text.error {
  color: #e74c3c;
}

.retry-btn {
  margin-top: 24rpx;
  padding: 14rpx 48rpx;
  border: 1rpx solid $border-dark;
  border-radius: 999rpx;
  color: $gold-light;
  font-size: 26rpx;
}

.loading-spinner {
  width: 48rpx;
  height: 48rpx;
  border: 4rpx solid rgba(214, 168, 39, 0.2);
  border-top-color: $gold;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 24rpx;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== Tab ===== */
.tabs-scroll {
  width: 100%;
  margin: 28rpx 0;
  white-space: nowrap;
}

.tabs {
  display: inline-flex;
  gap: 16rpx;
}

.tab {
  flex-shrink: 0;
  white-space: nowrap;
  padding: 16rpx 26rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.09);
  border-radius: 999rpx;
  color: $text-muted;
  font-size: 24rpx;
  line-height: 1;
}

.tab.active {
  border-color: $border-dark;
  color: $gold-light;
  background: rgba(214, 168, 39, 0.12);
}

/* ===== 作品网格 ===== */
.works-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
}

.work-card {
  overflow: hidden;
  border: 1rpx solid rgba(255, 255, 255, 0.07);
  border-radius: 24rpx;
  background: $card-bg;
}

.work-image {
  width: 100%;
  height: 280rpx;
}

.work-body {
  padding: 18rpx;
}

.work-line {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10rpx;
}

.work-title {
  font-size: 28rpx;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 260rpx;
}

.status-label {
  flex-shrink: 0;
  color: $gold-light;
  font-size: 20rpx;
}

.price {
  display: block;
  margin-top: 12rpx;
  color: $gold-light;
  font-size: 32rpx;
  font-weight: 700;
}
</style>
