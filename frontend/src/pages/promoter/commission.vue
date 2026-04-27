<template>
  <view class="commission-page">
    <!-- 佣金统计 -->
    <view class="commission-stats">
      <view class="stat-item total">
        <text class="stat-value">¥{{ formatPrice(stats.totalCommission) }}</text>
        <text class="stat-label">累计佣金</text>
      </view>
      <view class="stat-row">
        <view class="stat-item">
          <text class="stat-value">¥{{ formatPrice(stats.level1Commission) }}</text>
          <text class="stat-label">一级佣金</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">¥{{ formatPrice(stats.level2Commission) }}</text>
          <text class="stat-label">二级佣金</text>
        </view>
      </view>
      <view class="stat-row">
        <view class="stat-item">
          <text class="stat-value">¥{{ formatPrice(stats.withdrawn) }}</text>
          <text class="stat-label">已提现</text>
        </view>
        <view class="stat-item highlight">
          <text class="stat-value">¥{{ formatPrice(stats.withdrawable) }}</text>
          <text class="stat-label">可提现</text>
        </view>
      </view>
    </view>

    <!-- 佣金说明 -->
    <view class="info-card">
      <view class="card-header">
        
        <text class="card-title">佣金说明</text>
      </view>
      <view class="info-content">
        <view class="info-row">
          <view class="level-badge level-1">一级</view>
          <text class="info-text">直接推广订单，享受订单金额的 {{ config.level1Rate || 10 }}% 佣金</text>
        </view>
        <view class="info-row">
          <view class="level-badge level-2">二级</view>
          <text class="info-text">间接推广订单，享受订单金额的 {{ config.level2Rate || 5 }}% 佣金</text>
        </view>
      </view>
    </view>

    <!-- 筛选标签 -->
    <view class="filter-tabs">
      <view
        class="filter-tab"
        :class="{ active: filterType === 'all' }"
        @click="changeFilter('all')"
      >全部</view>
      <view
        class="filter-tab"
        :class="{ active: filterType === 'level1' }"
        @click="changeFilter('level1')"
      >一级佣金</view>
      <view
        class="filter-tab"
        :class="{ active: filterType === 'level2' }"
        @click="changeFilter('level2')"
      >二级佣金</view>
    </view>

    <!-- 佣金记录列表 -->
    <scroll-view class="record-list" scroll-y @scrolltolower="loadMore">
      <view class="record-item" v-for="item in records" :key="item.id">
        <view class="record-left">
          <view class="record-type">
            <view class="type-badge" :class="'level-' + item.level">
              {{ item.level === 1 ? '一级' : '二级' }}
            </view>
            <text class="record-desc">{{ item.orderTitle || '订单佣金' }}</text>
          </view>
          <text class="record-time">{{ item.createTime }}</text>
        </view>
        <view class="record-right">
          <text class="record-amount">+¥{{ formatPrice(item.commission) }}</text>
          <text class="record-order" v-if="item.orderNo">订单号: {{ item.orderNo }}</text>
        </view>
      </view>

      <!-- 加载更多 -->
      <view class="load-more" v-if="loading">
        <text class="loading-text">加载中...</text>
        <text>加载中...</text>
      </view>
      <view class="no-more" v-else-if="records.length > 0">
        <text>— 没有更多了 —</text>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="!loading && records.length === 0">
        <image class="empty-icon" src="/static/icons/commission-empty.png" mode="aspectFit"></image>
        <text class="empty-text">暂无佣金记录</text>
        <text class="empty-hint">推广订单即可获得佣金奖励</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { getCommissionList, getCommissionConfig } from '@/api/promoter'

export default {
  data() {
    return {
      stats: {
        totalCommission: 0,
        level1Commission: 0,
        level2Commission: 0,
        withdrawn: 0,
        withdrawable: 0
      },
      config: {
        level1Rate: 10,
        level2Rate: 5
      },
      filterType: 'all',
      records: [],
      page: 1,
      pageSize: 20,
      loading: false,
      hasMore: false
    }
  },

  onLoad(options) {
    if (options.type) {
      this.filterType = options.type
    }
    this.loadConfig()
    this.loadData()
  },

  methods: {
    async loadConfig() {
      try {
        const res = await getCommissionConfig()
        if (res) {
          this.config = res
        }
      } catch (e) {
        console.error('加载佣金配置失败', e)
      }
    },

    async loadData() {
      this.loading = true
      try {
        const res = await getCommissionList({
          page: this.page,
          pageSize: this.pageSize,
          level: this.filterType === 'all' ? '' : (this.filterType === 'level1' ? 1 : 2)
        })

        if (this.page === 1) {
          this.records = res.records || []
          if (res.stats) {
            this.stats = res.stats
          }
        } else {
          this.records = [...this.records, ...(res.records || [])]
        }

        this.hasMore = res.records?.length >= this.pageSize
      } catch (e) {
        console.error('加载佣金记录失败', e)
      } finally {
        this.loading = false
      }
    },

    changeFilter(type) {
      if (this.filterType === type) return
      this.filterType = type
      this.page = 1
      this.records = []
      this.loadData()
    },

    loadMore() {
      if (this.hasMore && !this.loading) {
        this.page++
        this.loadData()
      }
    },

    formatPrice(price) {
      if (!price) return '0.00'
      const yuan = price / 100  // 分转元
      if (yuan >= 10000) {
        return (yuan / 10000).toFixed(2) + '万'
      }
      return yuan.toFixed(2)
    }
  }
}
</script>

<style lang="scss" scoped>
.commission-page {
  min-height: 100vh;
  background: #f5f5f5;
}

/* 佣金统计 */
.commission-stats {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40rpx 30rpx;
}

.stat-item.total {
  text-align: center;
  margin-bottom: 30rpx;

  .stat-value {
    display: block;
    font-size: 56rpx;
    font-weight: 700;
    color: #fff;
    margin-bottom: 10rpx;
  }

  .stat-label {
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.8);
  }
}

.stat-row {
  display: flex;
  justify-content: space-around;
}

.stat-item {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;

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

.stat-item.highlight .stat-value {
  color: #ffe66d;
}

/* 佣金说明 */
.info-card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.card-header {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;

  .card-title {
    font-size: 28rpx;
    color: #333;
    font-weight: 600;
    margin-left: 10rpx;
  }
}

.info-content {
  padding-left: 10rpx;
}

.info-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16rpx;
}

.level-badge {
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
  font-size: 20rpx;
  color: #fff;
  margin-right: 16rpx;
}

.level-badge.level-1 {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.level-badge.level-2 {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.info-text {
  flex: 1;
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
}

/* 筛选标签 */
.filter-tabs {
  display: flex;
  background: #fff;
  padding: 20rpx 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.filter-tab {
  padding: 12rpx 30rpx;
  font-size: 26rpx;
  color: #666;
  background: #f5f5f5;
  border-radius: 30rpx;
  margin-right: 20rpx;
}

.filter-tab.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

/* 记录列表 */
.record-list {
  height: calc(100vh - 500rpx);
  padding: 20rpx;
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 12rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.record-left {
  flex: 1;
}

.record-type {
  display: flex;
  align-items: center;
  margin-bottom: 8rpx;
}

.type-badge {
  padding: 4rpx 12rpx;
  border-radius: 16rpx;
  font-size: 18rpx;
  color: #fff;
  margin-right: 12rpx;
}

.type-badge.level-1 {
  background: #667eea;
}

.type-badge.level-2 {
  background: #f5576c;
}

.record-desc {
  font-size: 28rpx;
  color: #333;
}

.record-time {
  font-size: 22rpx;
  color: #999;
}

.record-right {
  text-align: right;
}

.record-amount {
  display: block;
  font-size: 32rpx;
  color: #e74c3c;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.record-order {
  font-size: 20rpx;
  color: #999;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 150rpx;
}

.empty-icon {
  width: 180rpx;
  height: 180rpx;
  margin-bottom: 30rpx;
  opacity: 0.5;
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

.load-more, .no-more {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 30rpx 0;
  font-size: 26rpx;
  color: #999;
}

.load-more text {
  margin-left: 12rpx;
}
</style>
