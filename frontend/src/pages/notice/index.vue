<template>
  <view class="notice-page">
    <!-- Tab切换 -->
    <view class="tab-header">
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'announce' }"
        @click="switchTab('announce')"
      >
        平台公告
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 'activity' }"
        @click="switchTab('activity')"
      >
        活动中心
      </view>
    </view>

    <!-- 公告列表 -->
    <scroll-view class="notice-list" scroll-y @scrolltolower="loadMore">
      <view 
        class="notice-item"
        v-for="item in noticeList"
        :key="item.id"
        @click="goDetail(item)"
      >
        <view class="notice-header">
          <view class="notice-tag" :class="item.type">{{ getTypeName(item.type) }}</view>
          <text class="notice-time">{{ formatTime(item.publishTime) }}</text>
        </view>
        <text class="notice-title">{{ item.title }}</text>
        <text class="notice-summary" v-if="item.summary">{{ item.summary }}</text>
        <view class="notice-footer">
          <image 
            class="notice-cover" 
            v-if="item.cover" 
            :src="item.cover" 
            mode="aspectFill"
          ></image>
          <text class="notice-views">阅读 {{ item.viewCount }}</text>
        </view>
      </view>

      <!-- 加载状态 -->
      <view class="loading-more" v-if="loading">
        <text>加载中...</text>
      </view>
      <view class="no-more" v-if="!hasMore && noticeList.length > 0">
        <text>没有更多了</text>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="noticeList.length === 0 && !loading">
        <image class="empty-icon" src="/static/icons/empty-notice.png" mode="aspectFit"></image>
        <text class="empty-text">暂无{{ currentTab === 'announce' ? '公告' : '活动' }}</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      currentTab: 'announce',
      noticeList: [],
      loading: false,
      hasMore: true,
      page: 1
    }
  },

  onLoad() {
    this.loadNotice()
  },

  onShareAppMessage() {
    return {
      title: '艺本艺术公告',
      path: '/pages/notice/index'
    }
  },

  methods: {
    switchTab(tab) {
      this.currentTab = tab
      this.noticeList = []
      this.page = 1
      this.hasMore = true
      this.loadNotice()
    },

    loadNotice() {
      if (this.loading) return
      this.loading = true

      // TODO: 对接真实通知/公告 API
      this.loading = false
      this.noticeList = this.noticeList || []
    },

    loadMore() {
      if (!this.hasMore || this.loading) return
      this.loadNotice()
    },

    goDetail(item) {
      uni.navigateTo({
        url: `/pages/notice/detail?id=${item.id}&type=${this.currentTab}`
      })
    },

    getTypeName(type) {
      const names = {
        important: '重要',
        normal: '公告',
        system: '系统',
        activity: '活动'
      }
      return names[type] || '公告'
    },

    formatTime(timeStr) {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      const now = new Date()
      const diff = now - date
      
      if (diff < 86400000) return '今天'
      if (diff < 172800000) return '昨天'
      if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
      
      return `${date.getMonth() + 1}/${date.getDate()}`
    }
  }
}
</script>

<style lang="scss" scoped>
.notice-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.tab-header {
  display: flex;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 10;

  .tab-item {
    flex: 1;
    height: 96rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 30rpx;
    color: #666;
    position: relative;

    &.active {
      color: #667eea;
      font-weight: 500;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 60rpx;
        height: 4rpx;
        background: #667eea;
        border-radius: 2rpx;
      }
    }
  }
}

.notice-list {
  padding: 20rpx;

  .notice-item {
    background: #fff;
    border-radius: 16rpx;
    padding: 28rpx;
    margin-bottom: 20rpx;

    &:active {
      background: #fafafa;
    }

    .notice-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16rpx;

      .notice-tag {
        padding: 4rpx 16rpx;
        border-radius: 6rpx;
        font-size: 22rpx;
        color: #fff;

        &.important {
          background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
        }

        &.normal {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }

        &.system {
          background: linear-gradient(135deg, #95a5a6 0%, #7f8c8d 100%);
        }

        &.activity {
          background: linear-gradient(135deg, #f39c12 0%, #e67e22 100%);
        }
      }

      .notice-time {
        font-size: 24rpx;
        color: #999;
      }
    }

    .notice-title {
      font-size: 32rpx;
      font-weight: 600;
      color: #333;
      display: block;
      margin-bottom: 12rpx;
      line-height: 1.4;
    }

    .notice-summary {
      font-size: 26rpx;
      color: #666;
      line-height: 1.6;
      display: block;
      margin-bottom: 16rpx;
    }

    .notice-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .notice-cover {
        width: 160rpx;
        height: 100rpx;
        border-radius: 8rpx;
        background: #f0f0f0;
      }

      .notice-views {
        font-size: 24rpx;
        color: #999;
      }
    }
  }
}

.loading-more,
.no-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: #999;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;

  .empty-icon {
    width: 200rpx;
    height: 200rpx;
    opacity: 0.5;
  }

  .empty-text {
    font-size: 28rpx;
    color: #999;
    margin-top: 20rpx;
  }
}
</style>
