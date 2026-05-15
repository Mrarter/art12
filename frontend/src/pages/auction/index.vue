<template>
  <view class="auction-page">
    <!-- 顶部Tab切换 -->
    <view class="auction-header">
      <view class="header-tabs">
        <view
          class="tab-item"
          :class="{ active: currentTab === 1 }"
          @click="switchTab(1)"
        >
          <text>正在拍卖</text>
          <view class="tab-line" v-if="currentTab === 1"></view>
        </view>
        <view
          class="tab-item"
          :class="{ active: currentTab === 0 }"
          @click="switchTab(0)"
        >
          <text>即将开始</text>
          <view class="tab-line" v-if="currentTab === 0"></view>
        </view>
        <view
          class="tab-item"
          :class="{ active: currentTab === 2 }"
          @click="switchTab(2)"
        >
          <text>已结束</text>
          <view class="tab-line" v-if="currentTab === 2"></view>
        </view>
      </view>
    </view>

    <!-- 拍卖列表 -->
    <scroll-view
      class="auction-list"
      scroll-y
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="auction-card" v-for="item in sessionList" :key="item.id" @click="goSessionDetail(item.id)">
        <image class="auction-image" :src="item.coverImage || 'https://picsum.photos/300/300?random=auction'" mode="aspectFill"></image>
        <view class="auction-content">
          <view class="auction-name">{{ item.name }}</view>
          <view class="auction-desc">{{ item.description || '精彩拍卖，不容错过' }}</view>

          <view class="auction-meta">
            <view class="meta-item">
              <text class="meta-label">开始时间</text>
              <text class="meta-value">{{ formatTime(item.startTime) }}</text>
            </view>
            <view class="meta-item">
              <text class="meta-label">结束时间</text>
              <text class="meta-value">{{ formatTime(item.endTime) }}</text>
            </view>
          </view>

          <view class="auction-footer">
            <view class="status-badge" :class="getStatusClass(item.status)">
              <text v-if="item.status === 1">
                <text v-if="item.remainSeconds > 0">{{ formatCountdown(item.remainSeconds) }}</text>
                <text v-else>竞拍中</text>
              </text>
              <text v-else-if="item.status === 0">即将开始</text>
              <text v-else>已结束</text>
            </view>
            <view class="bid-info">
              <text class="bid-count">{{ item.lotCount || 0 }}件拍品</text>
              <text class="bid-price" v-if="item.currentPrice">¥{{ formatPrice(item.currentPrice) }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 加载更多 -->
      <view class="load-more" v-if="loading && sessionList.length > 0">
        <text>加载中...</text>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="!loading && sessionList.length === 0">
        <text class="empty-icon">🔨</text>
        <text class="empty-text">暂无{{ getTabLabel() }}的拍卖</text>
        <view class="empty-btn" @click="switchTab(1)">查看正在拍卖</view>
      </view>

      <!-- 底部安全区 -->
      <view class="safe-area-bottom"></view>
    </scroll-view>
    
    <!-- 自定义TabBar -->
    <CustomTabBar :currentIndex="1" />
  </view>
</template>

<script>
import CustomTabBar from '@/components/custom-tab-bar/index.vue'
import { getAuctionSessions } from '@/api/auction'

export default {
  components: {
    CustomTabBar
  },
  data() {
    return {
      currentTab: 1,
      sessionList: [],
      page: 1,
      pageSize: 10,
      loading: false,
      refreshing: false,
      noMore: false
    }
  },

  onLoad() {
    this.loadSessions()
  },

  onShow() {
    this.refresh()
  },

  methods: {
    async loadSessions(isReset = false) {
      if (this.loading) return

      if (isReset) {
        this.page = 1
        this.noMore = false
      }

      this.loading = true
      try {
        const res = await getAuctionSessions({
          page: this.page,
          pageSize: this.pageSize
        })

        let list = []
        if (res) {
          // 兼容不同的返回格式
          if (Array.isArray(res)) {
            list = res
          } else if (res.records) {
            list = res.records
          } else if (res.list) {
            list = res.list
          }
        }

        const normalizedList = list
          .map(this.normalizeSession)
          .filter(item => item.status === this.currentTab)

        if (normalizedList.length === 0) {
          if (isReset) this.sessionList = []
          this.noMore = true
        } else {
          if (isReset) {
            this.sessionList = normalizedList
          } else {
            this.sessionList = [...this.sessionList, ...normalizedList]
          }

          if (list.length < this.pageSize) {
            this.noMore = true
          } else {
            this.page++
          }
        }
      } catch (e) {
        console.error('加载拍卖专场失败', e)
        if (isReset) this.sessionList = []
        uni.showToast({ title: '拍卖数据加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },

    normalizeSession(item) {
      const status = this.getEffectiveStatus(item)
      return {
        ...item,
        status,
        name: item.name || item.title || '拍卖专场',
        coverImage: item.coverImage || item.cover || '',
        lotCount: item.lotCount || item.totalLots || 0,
        remainSeconds: status === 1 ? this.getRemainSeconds(item.endTime) : 0
      }
    },

    getEffectiveStatus(item) {
      const rawStatus = Number(item.status)
      // 优先使用后端状态，保持与运营后台一致
      if (!Number.isNaN(rawStatus)) {
        if (rawStatus === 3) return 2
        if (rawStatus === 2) return 1
        if (rawStatus === 1) return 0
        if (rawStatus === 0) return 0
      }

      const start = item.startTime || item.auctionStart || item.previewStart
      const end = item.endTime || item.auctionEnd || item.previewEnd
      const startTime = start ? new Date(start).getTime() : 0
      const endTime = end ? new Date(end).getTime() : 0
      const now = Date.now()

      if (startTime && now < startTime) return 0
      if (endTime && now > endTime) return 2
      if (startTime || endTime) return 1

      return 0
    },

    getRemainSeconds(endTime) {
      if (!endTime) return 0
      return Math.max(0, Math.floor((new Date(endTime).getTime() - Date.now()) / 1000))
    },

    refresh() {
      this.loadSessions(true)
    },

    onRefresh() {
      this.refreshing = true
      this.loadSessions(true).then(() => {
        this.refreshing = false
      })
    },

    switchTab(status) {
      if (this.currentTab === status) return
      this.currentTab = status
      this.loadSessions(true)
    },

    loadMore() {
      if (!this.noMore && !this.loading) {
        this.loadSessions()
      }
    },

    goSessionDetail(id) {
      uni.navigateTo({ url: `/pages/auction/session?id=${id}` })
    },

    getStatusClass(status) {
      return {
        'status-live': status === 1,
        'status-upcoming': status === 0,
        'status-ended': status === 2
      }
    },

    getTabLabel() {
      const labels = { 1: '正在拍卖', 0: '即将开始', 2: '已结束' }
      return labels[this.currentTab] || ''
    },

    formatTime(timeStr) {
      if (!timeStr) return '--'
      const date = new Date(timeStr)
      return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
    },

    formatCountdown(seconds) {
      if (seconds <= 0) return '竞拍中'
      const hours = Math.floor(seconds / 3600)
      const minutes = Math.floor((seconds % 3600) / 60)
      if (hours > 24) {
        const days = Math.floor(hours / 24)
        return `${days}天${hours % 24}时`
      }
      return `${hours}时${minutes}分`
    },

    formatPrice(price) {
      if (!price) return '0'
      const yuan = Math.round(price / 100)
      return yuan.toLocaleString()
    }
  }
}
</script>

<style lang="scss" scoped>
/* 深色主题色 */
$bg-primary: #0D0D0D;
$bg-secondary: #1A1A1A;
$bg-card: #242424;
$text-primary: #FFFFFF;
$text-secondary: #B3B3B3;
$text-muted: #666666;
$accent-gold: #D4AF37;
$accent-orange: #E8A838;
$accent-red: #FF6B6B;
$accent-blue: #4A90D9;

.auction-page {
  min-height: 100vh;
  background-color: $bg-primary;
}

.auction-header {
  position: sticky;
  top: 0;
  z-index: 99;
  background-color: $bg-primary;
  padding: 20rpx 30rpx;
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.06);
}

.header-tabs {
  display: flex;
}

.tab-item {
  position: relative;
  font-size: 30rpx;
  color: $text-muted;
  padding-bottom: 12rpx;
  margin-right: 50rpx;

  &.active {
    color: $text-primary;
    font-weight: 600;
  }

  .tab-line {
    position: absolute;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    width: 36rpx;
    height: 4rpx;
    background-color: $accent-gold;
    border-radius: 2rpx;
  }
}

.auction-list {
  padding: 20rpx;
  padding-bottom: calc(100rpx + env(safe-area-inset-bottom));
  height: calc(100vh - 120rpx);
}

.auction-card {
  display: flex;
  background-color: $bg-card;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.04);
}

.auction-image {
  width: 200rpx;
  height: 200rpx;
  border-radius: 12rpx;
  margin-right: 24rpx;
  background-color: $bg-secondary;
}

.auction-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.auction-name {
  font-size: 30rpx;
  color: $text-primary;
  font-weight: 600;
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auction-desc {
  font-size: 24rpx;
  color: $text-muted;
  margin-bottom: 12rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auction-meta {
  display: flex;
  margin-bottom: 16rpx;
}

.meta-item {
  margin-right: 30rpx;
}

.meta-label {
  display: block;
  font-size: 20rpx;
  color: $text-muted;
  margin-bottom: 4rpx;
}

.meta-value {
  font-size: 24rpx;
  color: $text-secondary;
}

.auction-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.status-badge {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  font-weight: 500;

  &.status-live {
    background: linear-gradient(135deg, $accent-red, #E55555);
    color: #fff;
  }

  &.status-upcoming {
    background: linear-gradient(135deg, $accent-blue, #3A7BC8);
    color: #fff;
  }

  &.status-ended {
    background: rgba(255, 255, 255, 0.1);
    color: $text-muted;
  }
}

.bid-info {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.bid-count {
  font-size: 22rpx;
  color: $text-muted;
}

.bid-price {
  font-size: 26rpx;
  color: $accent-gold;
  font-weight: 600;
}

.load-more {
  text-align: center;
  padding: 40rpx 0;
  font-size: 24rpx;
  color: $text-muted;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 200rpx 0;

  .empty-icon {
    font-size: 120rpx;
    margin-bottom: 30rpx;
    opacity: 0.5;
  }

  .empty-text {
    font-size: 28rpx;
    color: $text-muted;
    margin-bottom: 40rpx;
  }

  .empty-btn {
    padding: 16rpx 48rpx;
    background: linear-gradient(135deg, $accent-gold 0%, $accent-orange 100%);
    border-radius: 36rpx;
    font-size: 28rpx;
    color: $bg-primary;
    font-weight: 500;
  }
}

.safe-area-bottom {
  height: calc(100rpx + env(safe-area-inset-bottom));
}
</style>
