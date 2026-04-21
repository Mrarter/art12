<template>
  <view class="team-detail-page">
    <!-- 成员信息 -->
    <view class="member-header">
      <image class="member-avatar" :src="memberInfo.avatar || '/static/avatar/default.jpg'" mode="aspectFill"></image>
      <view class="member-info">
        <text class="member-name">{{ memberInfo.nickname || '未知用户' }}</text>
        <text class="member-level">{{ memberInfo.level === 1 ? '一级成员' : '二级成员' }}</text>
      </view>
    </view>

    <!-- 贡献统计 -->
    <view class="contribution-stats">
      <view class="stat-item">
        <text class="stat-value">{{ memberInfo.orderCount || 0 }}</text>
        <text class="stat-label">推广订单</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">¥{{ memberInfo.totalCommission || 0 }}</text>
        <text class="stat-label">贡献佣金</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ memberInfo.subCount || 0 }}</text>
        <text class="stat-label">下线成员</text>
      </view>
    </view>

    <!-- 佣金明细 -->
    <view class="commission-section">
      <view class="section-header">
        <text class="section-title">佣金明细</text>
      </view>
      <view class="commission-list">
        <view class="commission-item" v-for="item in commissionList" :key="item.id">
          <view class="item-info">
            <text class="item-title">{{ item.title }}</text>
            <text class="item-time">{{ item.createTime }}</text>
          </view>
          <text class="item-amount">+¥{{ item.amount }}</text>
        </view>
        <view class="empty-state" v-if="commissionList.length === 0">
          <text>暂无佣金记录</text>
        </view>
      </view>
      <view class="load-more" v-if="hasMore" @click="loadMore">
        <text>{{ loading ? '加载中...' : '加载更多' }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getTeamMemberDetail } from '@/api/promoter.js'

export default {
  data() {
    return {
      userId: '',
      memberInfo: {},
      commissionList: [],
      page: 1,
      pageSize: 20,
      hasMore: false,
      loading: false
    }
  },

  onLoad(options) {
    if (options.userId) {
      this.userId = options.userId
      this.loadData()
    }
  },

  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await getTeamMemberDetail(this.userId)
        this.memberInfo = res
        this.commissionList = res.commissionList || []
        this.hasMore = res.hasMore || false
      } catch (e) {
        console.error('加载成员详情失败', e)
      }
      this.loading = false
    },

    loadMore() {
      if (this.hasMore && !this.loading) {
        this.page++
        this.loadData()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.team-detail-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.member-header {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40rpx 30rpx;
  
  .member-avatar {
    width: 100rpx;
    height: 100rpx;
    border-radius: 50%;
    margin-right: 24rpx;
    border: 4rpx solid rgba(255, 255, 255, 0.3);
  }
  
  .member-info {
    .member-name {
      display: block;
      font-size: 36rpx;
      font-weight: 600;
      color: #fff;
      margin-bottom: 8rpx;
    }
    
    .member-level {
      font-size: 24rpx;
      color: rgba(255, 255, 255, 0.7);
    }
  }
}

.contribution-stats {
  display: flex;
  background: #fff;
  padding: 30rpx;
  margin: 20rpx;
  border-radius: 16rpx;
  
  .stat-item {
    flex: 1;
    text-align: center;
    
    .stat-value {
      display: block;
      font-size: 36rpx;
      font-weight: 600;
      color: #333;
      margin-bottom: 8rpx;
    }
    
    .stat-label {
      font-size: 22rpx;
      color: #999;
    }
  }
}

.commission-section {
  margin: 20rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
}

.section-header {
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.commission-list {
  .commission-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f5f5f5;
    
    &:last-child {
      border-bottom: none;
    }
    
    .item-info {
      .item-title {
        display: block;
        font-size: 28rpx;
        color: #333;
        margin-bottom: 6rpx;
      }
      
      .item-time {
        font-size: 22rpx;
        color: #999;
      }
    }
    
    .item-amount {
      font-size: 30rpx;
      font-weight: 600;
      color: #e74c3c;
    }
  }
  
  .empty-state {
    text-align: center;
    padding: 60rpx;
    color: #ccc;
    font-size: 26rpx;
  }
}

.load-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: #999;
}
</style>