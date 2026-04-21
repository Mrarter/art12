<template>
  <view class="earnings-page">
    <!-- 类型筛选 -->
    <view class="filter-tabs">
      <view 
        v-for="item in typeOptions" 
        :key="item.value"
        :class="['filter-tab', { active: selectedType === item.value }]"
        @click="changeType(item.value)"
      >{{ item.label }}</view>
    </view>

    <!-- 收益列表 -->
    <view class="earnings-list" v-if="earningsList.length > 0">
      <view class="earnings-item" v-for="item in earningsList" :key="item.id">
        <view class="item-left">
          <text class="item-title">{{ item.title }}</text>
          <text class="item-time">{{ item.createTime }}</text>
        </view>
        <view class="item-right">
          <text class="item-amount" :class="{ positive: item.amount > 0 }">
            {{ item.amount > 0 ? '+' : '' }}{{ item.amount }}
          </text>
          <text class="item-type">{{ item.typeText }}</text>
        </view>
      </view>
      
      <!-- 加载更多 -->
      <view class="load-more" v-if="hasMore" @click="loadMore">
        <text>{{ loading ? '加载中...' : '加载更多' }}</text>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-else>
      <image class="empty-icon" src="/static/icons/empty-earnings.png" mode="aspectFit"></image>
      <text class="empty-text">暂无收益记录</text>
    </view>
  </view>
</template>

<script>
import { getEarningsList } from '@/api/promoter.js'

export default {
  data() {
    return {
      selectedType: 'order',
      typeOptions: [
        { label: '订单佣金', value: 'order' },
        { label: '邀请奖励', value: 'invite' },
        { label: '团队奖励', value: 'team' }
      ],
      earningsList: [],
      page: 1,
      pageSize: 20,
      hasMore: true,
      loading: false
    }
  },

  onLoad(options) {
    if (options.type) {
      this.selectedType = options.type
    }
    this.loadData()
  },

  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await getEarningsList({
          type: this.selectedType,
          page: this.page,
          pageSize: this.pageSize
        })
        
        if (this.page === 1) {
          this.earningsList = res.list || []
        } else {
          this.earningsList = [...this.earningsList, ...(res.list || [])]
        }
        
        this.hasMore = res.list && res.list.length === this.pageSize
      } catch (e) {
        console.error('加载收益列表失败', e)
      }
      this.loading = false
    },

    changeType(type) {
      this.selectedType = type
      this.page = 1
      this.loadData()
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
.earnings-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.filter-tabs {
  display: flex;
  background: #fff;
  padding: 20rpx 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.filter-tab {
  flex: 1;
  text-align: center;
  font-size: 28rpx;
  color: #999;
  padding: 16rpx 0;
  border-radius: 8rpx;
  
  &.active {
    background: #667eea;
    color: #fff;
  }
}

.earnings-list {
  margin: 20rpx;
}

.earnings-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}

.item-left {
  .item-title {
    display: block;
    font-size: 28rpx;
    color: #333;
    margin-bottom: 8rpx;
  }
  
  .item-time {
    font-size: 22rpx;
    color: #999;
  }
}

.item-right {
  text-align: right;
  
  .item-amount {
    display: block;
    font-size: 32rpx;
    font-weight: 600;
    color: #999;
    margin-bottom: 4rpx;
  }
  
  .item-amount.positive {
    color: #e74c3c;
  }
  
  .item-type {
    font-size: 22rpx;
    color: #999;
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
}
</style>