<template>
  <view class="auction-page">
    <!-- 拍卖专场列表 -->
    <view class="session-list">
      <view 
        v-for="session in sessions" 
        :key="session.id"
        class="session-card"
        @click="goSessionDetail(session)"
      >
        <image class="session-cover" :src="session.coverImage" mode="aspectFill" />
        <view class="session-info">
          <view class="session-name">{{ session.name }}</view>
          <view class="session-time">
            <text class="time-label">{{ getStatusText(session.status) }}</text>
            <text class="time-value">{{ formatTime(session.startTime) }}</text>
          </view>
          <view class="session-meta">
            <text>{{ session.lotCount }}件拍品</text>
          </view>
        </view>
        <view class="session-status" :class="'status-' + session.status">
          {{ getStatusText(session.status) }}
        </view>
      </view>
    </view>

    <!-- 无数据 -->
    <view class="empty-state" v-if="sessions.length === 0 && !isLoading">
      <text class="empty-icon">🔨</text>
      <text class="empty-text">暂无拍卖专场</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAuctionSessions } from '@/api/auction'

const sessions = ref([])
const isLoading = ref(false)

onMounted(() => {
  loadSessions()
})

async function loadSessions() {
  isLoading.value = true
  try {
    const res = await getAuctionSessions({ page: 1, pageSize: 20 })
    sessions.value = res.records || []
  } catch (e) {
    // 使用模拟数据
    sessions.value = generateMockData()
  }
  isLoading.value = false
}

function generateMockData() {
  return [
    {
      id: 1,
      name: '当代艺术专场',
      coverImage: 'https://picsum.photos/750/400?random=10',
      startTime: '2026-04-20 14:00:00',
      status: 2,
      lotCount: 28
    },
    {
      id: 2,
      name: '经典油画专场',
      coverImage: 'https://picsum.photos/750/400?random=11',
      startTime: '2026-04-22 19:00:00',
      status: 1,
      lotCount: 35
    },
    {
      id: 3,
      name: '当代雕塑拍卖',
      coverImage: 'https://picsum.photos/750/400?random=12',
      startTime: '2026-04-18 14:00:00',
      status: 3,
      lotCount: 20
    }
  ]
}

function getStatusText(status) {
  const map = { 1: '预告', 2: '进行中', 3: '已结束' }
  return map[status] || '未知'
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes().toString().padStart(2, '0')
  return `${month}月${day}日 ${hour}:${minute}`
}

function goSessionDetail(session) {
  uni.navigateTo({ url: `/pages/auction/session?id=${session.id}` })
}
</script>

<style lang="scss" scoped>
.auction-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 16rpx;
}

.session-list {
  .session-card {
    background: #fff;
    border-radius: 16rpx;
    overflow: hidden;
    margin-bottom: 24rpx;
    position: relative;
    
    .session-cover {
      width: 100%;
      height: 360rpx;
    }
    
    .session-info {
      padding: 24rpx;
      
      .session-name {
        font-size: 32rpx;
        font-weight: 600;
        color: #333;
        margin-bottom: 12rpx;
      }
      
      .session-time {
        display: flex;
        align-items: center;
        gap: 12rpx;
        margin-bottom: 8rpx;
        
        .time-label {
          font-size: 22rpx;
          color: #fff;
          background: #c9a962;
          padding: 4rpx 12rpx;
          border-radius: 4rpx;
        }
        
        .time-value {
          font-size: 26rpx;
          color: #666;
        }
      }
      
      .session-meta {
        font-size: 24rpx;
        color: #999;
      }
    }
    
    .session-status {
      position: absolute;
      top: 20rpx;
      right: 20rpx;
      padding: 8rpx 20rpx;
      border-radius: 8rpx;
      font-size: 24rpx;
      
      &.status-1 {
        background: rgba(0, 0, 0, 0.5);
        color: #fff;
      }
      
      &.status-2 {
        background: #e53935;
        color: #fff;
      }
      
      &.status-3 {
        background: #999;
        color: #fff;
      }
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
  
  .empty-icon {
    font-size: 120rpx;
  }
  
  .empty-text {
    font-size: 28rpx;
    color: #999;
    margin-top: 32rpx;
  }
}
</style>
