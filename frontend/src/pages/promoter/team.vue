<template>
  <view class="team-page">
    <!-- 团队统计 -->
    <view class="team-stats">
      <view class="stat-item">
        <text class="stat-value">{{ stats.totalCount || 0 }}</text>
        <text class="stat-label">团队总人数</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ stats.level1Count || 0 }}</text>
        <text class="stat-label">一级成员</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ stats.level2Count || 0 }}</text>
        <text class="stat-label">二级成员</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">¥{{ stats.totalCommission || 0 }}</text>
        <text class="stat-label">累计佣金</text>
      </view>
    </view>

    <!-- 成员列表 -->
    <view class="member-list">
      <view class="member-group" v-for="group in groupedMembers" :key="group.level">
        <view class="group-header">
          <text class="group-title">{{ group.level === 1 ? '一级成员' : '二级成员' }}</text>
          <text class="group-count">{{ group.count }}人</text>
        </view>
        <view class="group-members">
          <view class="member-item" v-for="item in group.members" :key="item.userId" @click="goDetail(item.userId)">
            <image class="member-avatar" :src="item.avatar || '/static/avatar/default.jpg'" mode="aspectFill"></image>
            <view class="member-info">
              <text class="member-name">{{ item.nickname }}</text>
              <text class="member-time">加入于 {{ item.joinTime }}</text>
            </view>
            <view class="member-stats">
              <text class="member-order-count">{{ item.orderCount }}单</text>
              <text class="member-commission">¥{{ item.commission || 0 }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 加载更多 -->
      <view class="load-more" v-if="hasMore" @click="loadMore">
        <text>{{ loading ? '加载中...' : '加载更多' }}</text>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-if="members.length === 0 && !loading">
      <image class="empty-icon" src="/static/icons/empty-team.png" mode="aspectFit"></image>
      <text class="empty-text">暂无比员</text>
      <text class="empty-hint">邀请好友加入，获得更多团队佣金奖励</text>
    </view>
  </view>
</template>

<script>
import { getTeamList } from '@/api/promoter.js'

export default {
  data() {
    return {
      stats: {
        totalCount: 0,
        level1Count: 0,
        level2Count: 0,
        totalCommission: 0
      },
      members: [],
      page: 1,
      pageSize: 20,
      hasMore: true,
      loading: false
    }
  },

  computed: {
    groupedMembers() {
      const level1 = this.members.filter(m => m.level === 1)
      const level2 = this.members.filter(m => m.level === 2)
      
      return [
        { level: 1, count: level1.length, members: level1 },
        { level: 2, count: level2.length, members: level2 }
      ].filter(g => g.count > 0)
    }
  },

  onLoad() {
    this.loadData()
  },

  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await getTeamList({
          page: this.page,
          pageSize: this.pageSize
        })
        
        if (this.page === 1) {
          this.members = res.list || []
          if (res.stats) {
            this.stats = res.stats
          }
        } else {
          this.members = [...this.members, ...(res.list || [])]
        }
        
        this.hasMore = res.list && res.list.length === this.pageSize
      } catch (e) {
        console.error('加载团队列表失败', e)
      }
      this.loading = false
    },

    loadMore() {
      if (this.hasMore && !this.loading) {
        this.page++
        this.loadData()
      }
    },

    goDetail(userId) {
      uni.navigateTo({ url: `/pages/promoter/team-detail?userId=${userId}` })
    }
  }
}
</script>

<style lang="scss" scoped>
.team-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.team-stats {
  display: flex;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 30rpx 20rpx;
}

.stat-item {
  flex: 1;
  text-align: center;
  
  .stat-value {
    display: block;
    font-size: 32rpx;
    font-weight: 600;
    color: #fff;
    margin-bottom: 8rpx;
  }
  
  .stat-label {
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.7);
  }
}

.member-list {
  padding: 20rpx;
}

.member-group {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx;
  background: #fafafa;
  border-bottom: 1rpx solid #f0f0f0;
  
  .group-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #333;
  }
  
  .group-count {
    font-size: 24rpx;
    color: #999;
  }
}

.group-members {
  .member-item {
    display: flex;
    align-items: center;
    padding: 24rpx;
    border-bottom: 1rpx solid #f5f5f5;
    
    &:last-child {
      border-bottom: none;
    }
  }
}

.member-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  margin-right: 16rpx;
}

.member-info {
  flex: 1;
  
  .member-name {
    display: block;
    font-size: 28rpx;
    color: #333;
    margin-bottom: 4rpx;
  }
  
  .member-time {
    font-size: 22rpx;
    color: #999;
  }
}

.member-stats {
  text-align: right;
  
  .member-order-count {
    display: block;
    font-size: 24rpx;
    color: #667eea;
    margin-bottom: 4rpx;
  }
  
  .member-commission {
    font-size: 26rpx;
    color: #e74c3c;
    font-weight: 600;
  }
}

.load-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: #999;
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
}

.empty-text {
  font-size: 28rpx;
  color: #999;
  margin-bottom: 16rpx;
}

.empty-hint {
  font-size: 24rpx;
  color: #ccc;
}
</style>