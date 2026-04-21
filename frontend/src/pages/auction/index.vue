<template>
  <view class="auction-page">
    <!-- 拍卖头部信息 -->
    <view class="auction-header">
      <view class="header-info">
        <text class="auction-title">拍卖专场</text>
        <text class="auction-count">{{ total }}个专场</text>
      </view>
      <view class="header-tabs">
        <view
          class="tab-item"
          :class="{ active: currentTab === 1 }"
          @click="switchTab(1)"
        >正在拍卖</view>
        <view
          class="tab-item"
          :class="{ active: currentTab === 0 }"
          @click="switchTab(0)"
        >即将开始</view>
        <view
          class="tab-item"
          :class="{ active: currentTab === 2 }"
          @click="switchTab(2)"
        >已结束</view>
      </view>
    </view>

    <!-- 拍卖列表 -->
    <scroll-view
      class="auction-list"
      scroll-y
      @scrolltolower="loadMore"
    >
      <view
        class="auction-card"
        v-for="item in sessionList"
        :key="item.id"
        @click="goSessionDetail(item.id)"
      >
        <image class="auction-image" :src="item.coverImage || '/static/icons/auction-default.png'" mode="aspectFill"></image>
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

          <view class="auction-status">
            <view class="status-badge" :class="getStatusClass(item.status)">
              <text v-if="item.status === 1">
                <text v-if="item.remainSeconds > 0">{{ formatCountdown(item.remainSeconds) }}</text>
                <text v-else>竞拍中</text>
              </text>
              <text v-else-if="item.status === 0">即将开始</text>
              <text v-else>已结束</text>
            </view>
            <view class="bid-count">{{ item.lotCount || 0 }}件拍品</view>
          </view>
        </view>
      </view>

      <!-- 加载状态 -->
      <view class="loading-more" v-if="loading">
        <text>加载中...</text>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="!loading && sessionList.length === 0">
        <image class="empty-icon" src="/static/icons/auction-empty.png" mode="aspectFit"></image>
        <text class="empty-text">暂无{{ getTabLabel() }}的拍卖</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { getAuctionSessions } from '@/api/auction'

export default {
  data() {
    return {
      currentTab: 1, // 0-即将开始 1-进行中 2-已结束
      sessionList: [],
      page: 1,
      pageSize: 10,
      total: 0,
      loading: false,
      noMore: false
    }
  },

  onLoad() {
    this.loadSessions()
  },

  onShow() {
    // 每次显示时刷新数据
    this.refresh()
  },

  methods: {
    async loadSessions() {
      if (this.loading || this.noMore) return

      this.loading = true
      try {
        const res = await getAuctionSessions({
          page: this.page,
          pageSize: this.pageSize,
          status: this.currentTab
        })

        if (res.code === 200) {
          const data = res.data
          if (this.page === 1) {
            this.sessionList = data.records || []
          } else {
            this.sessionList = [...this.sessionList, ...(data.records || [])]
          }
          this.total = data.total || 0
          this.noMore = this.sessionList.length >= this.total
        }
      } catch (e) {
        console.error('加载拍卖专场失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },

    refresh() {
      this.page = 1
      this.noMore = false
      this.loadSessions()
    },

    switchTab(status) {
      if (this.currentTab === status) return
      this.currentTab = status
      this.refresh()
    },

    loadMore() {
      if (!this.noMore) {
        this.page++
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
      return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
    },

    formatCountdown(seconds) {
      if (seconds <= 0) return '竞拍中'
      const hours = Math.floor(seconds / 3600)
      const minutes = Math.floor((seconds % 3600) / 60)
      const secs = seconds % 60
      if (hours > 24) {
        const days = Math.floor(hours / 24)
        return `${days}天${hours % 24}小时`
      }
      return `${hours}时${minutes}分${secs}秒`
    }
  }
}
</script>

<style lang="scss" scoped>
.auction-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.auction-header {
  background: #fff;
  padding: 30rpx;
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-info {
  display: flex;
  align-items: baseline;
  margin-bottom: 30rpx;
}

.auction-title {
  font-size: 36rpx;
  color: #333;
  font-weight: 600;
  margin-right: 16rpx;
}

.auction-count {
  font-size: 24rpx;
  color: #999;
}

.header-tabs {
  display: flex;
  background: #f5f5f5;
  border-radius: 12rpx;
  padding: 6rpx;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  font-size: 26rpx;
  color: #666;
  border-radius: 8rpx;
  transition: all 0.3s;
}

.tab-item.active {
  background: #fff;
  color: #333;
  font-weight: 500;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.08);
}

.auction-list {
  height: calc(100vh - 220rpx);
  padding: 20rpx;
}

.auction-card {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.auction-image {
  width: 220rpx;
  height: 220rpx;
  border-radius: 12rpx;
  margin-right: 24rpx;
  background: #f0f0f0;
}

.auction-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.auction-name {
  font-size: 32rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auction-desc {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 12rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auction-meta {
  display: flex;
  margin-bottom: 12rpx;
}

.meta-item {
  margin-right: 30rpx;
}

.meta-label {
  display: block;
  font-size: 22rpx;
  color: #999;
  margin-bottom: 4rpx;
}

.meta-value {
  font-size: 24rpx;
  color: #666;
}

.auction-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.status-badge {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
}

.status-live {
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  color: #fff;
}

.status-upcoming {
  background: linear-gradient(135deg, #3498db, #2980b9);
  color: #fff;
}

.status-ended {
  background: #e0e0e0;
  color: #999;
}

.bid-count {
  font-size: 24rpx;
  color: #999;
}

.loading-more {
  text-align: center;
  padding: 30rpx;
  color: #999;
  font-size: 26rpx;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
}

.empty-icon {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 30rpx;
  opacity: 0.5;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}
</style>
