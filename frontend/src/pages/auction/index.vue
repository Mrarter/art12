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
    <CustomTabBar :currentIndex="2" />
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
          pageSize: this.pageSize,
          status: this.currentTab
        })

        let list = []
        if (res && res.code === 200) {
          list = res.data?.records || res.data?.list || []
        }

        if (isReset) {
          this.sessionList = list
        } else {
          this.sessionList = [...this.sessionList, ...list]
        }

        if (list.length < this.pageSize) {
          this.noMore = true
        } else {
          this.page++
        }
      } catch (e) {
        console.error('加载拍卖专场失败，使用Mock数据', e)
        this.loadMockData(isReset)
      } finally {
        this.loading = false
      }
    },

    loadMockData(isReset = false) {
      const now = Date.now()
      const mockData = [
        { id: 1, name: '当代名家书画专场', description: '汇聚当代一流书画艺术家精品', coverImage: 'https://picsum.photos/300/300?random=1', startTime: new Date(now - 86400000).toISOString(), endTime: new Date(now + 86400000 * 2).toISOString(), status: 1, lotCount: 48, currentPrice: 1280000, remainSeconds: 3600 * 5 },
        { id: 2, name: '油画精品专场', description: '国内知名油画艺术家作品精选', coverImage: 'https://picsum.photos/300/300?random=2', startTime: new Date(now + 86400000).toISOString(), endTime: new Date(now + 86400000 * 3).toISOString(), status: 0, lotCount: 36, currentPrice: 0, remainSeconds: 0 },
        { id: 3, name: '瓷杂珍玩专场', description: '明清瓷器、文房雅玩精品', coverImage: 'https://picsum.photos/300/300?random=3', startTime: new Date(now - 86400000 * 5).toISOString(), endTime: new Date(now - 86400000).toISOString(), status: 2, lotCount: 120, currentPrice: 580000, remainSeconds: 0 },
        { id: 4, name: '当代雕塑专场', description: '学院派雕塑艺术家作品展', coverImage: 'https://picsum.photos/300/300?random=4', startTime: new Date(now - 3600000).toISOString(), endTime: new Date(now + 86400000).toISOString(), status: 1, lotCount: 28, currentPrice: 350000, remainSeconds: 7200 },
        { id: 5, name: '古籍善本专场', description: '珍稀古籍、名人信札、手稿', coverImage: 'https://picsum.photos/300/300?random=5', startTime: new Date(now - 86400000 * 3).toISOString(), endTime: new Date(now - 86400000 * 2).toISOString(), status: 2, lotCount: 85, currentPrice: 2800000, remainSeconds: 0 },
        { id: 6, name: '现当代艺术专场', description: '二十世纪至今的现当代艺术精品', coverImage: 'https://picsum.photos/300/300?random=6', startTime: new Date(now + 86400000 * 2).toISOString(), endTime: new Date(now + 86400000 * 4).toISOString(), status: 0, lotCount: 52, currentPrice: 0, remainSeconds: 0 }
      ]

      let list = mockData.filter(item => item.status === this.currentTab)

      if (isReset) {
        this.sessionList = list
      } else {
        this.sessionList = [...this.sessionList, ...list]
      }

      this.noMore = true
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
      const yuan = price / 100
      if (yuan >= 10000) {
        return (yuan / 10000).toFixed(yuan % 10000 === 0 ? 0 : 1) + '万'
      }
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
