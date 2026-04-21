<template>
  <view class="customer-page">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <input class="search-input" placeholder="搜索客户名称/手机号" v-model="searchKeyword" />
      <text class="search-btn" @click="doSearch">搜索</text>
    </view>

    <!-- 客户统计 -->
    <view class="customer-stats">
      <view class="stats-item">
        <text class="value">156</text>
        <text class="label">总客户数</text>
      </view>
      <view class="stats-item">
        <text class="value">28</text>
        <text class="label">本月新增</text>
      </view>
      <view class="stats-item">
        <text class="value">¥8,560</text>
        <text class="label">客户贡献收益</text>
      </view>
    </view>

    <!-- 筛选排序 -->
    <view class="filter-bar">
      <view class="filter-item" @click="toggleSort('time')">
        <text>最近成交</text>
        <text class="arrow" :class="{ active: sortBy === 'time' }">↓</text>
      </view>
      <view class="filter-item" @click="toggleSort('amount')">
        <text>累计消费</text>
        <text class="arrow" :class="{ active: sortBy === 'amount' }">↓</text>
      </view>
      <view class="filter-item">
        <text>筛选</text>
        <text class="arrow">▼</text>
      </view>
    </view>

    <!-- 客户列表 -->
    <scroll-view class="customer-list" scroll-y>
      <view class="customer-card" v-for="(customer, index) in customerList" :key="index">
        <image class="avatar" :src="customer.avatar" mode="aspectFill" />
        <view class="customer-info">
          <view class="name-row">
            <text class="name">{{ customer.name }}</text>
            <text class="phone">{{ customer.phone }}</text>
          </view>
          <view class="tags">
            <text class="tag" v-for="(tag, idx) in customer.tags" :key="idx">{{ tag }}</text>
          </view>
          <view class="stats-row">
            <text>订单 {{ customer.orderCount }} 单</text>
            <text>累计 ¥{{ customer.totalAmount }}</text>
            <text>最近 {{ customer.lastOrderDays }}天</text>
          </view>
        </view>
        <view class="action-btn" @click="contactCustomer(customer)">联系</view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="customerList.length === 0">
        <text>暂无客户数据</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      searchKeyword: '',
      sortBy: 'time',
      customerList: [
        {
          avatar: '/static/avatar-default.png',
          name: '陈先生',
          phone: '138****5678',
          tags: ['高消费', 'VIP'],
          orderCount: 12,
          totalAmount: '156,800',
          lastOrderDays: 3
        },
        {
          avatar: '/static/avatar-default.png',
          name: '刘女士',
          phone: '139****8765',
          tags: ['艺术品爱好者'],
          orderCount: 8,
          totalAmount: '89,200',
          lastOrderDays: 7
        },
        {
          avatar: '/static/avatar-default.png',
          name: '王先生',
          phone: '136****1234',
          tags: ['新客户', '活跃'],
          orderCount: 3,
          totalAmount: '28,500',
          lastOrderDays: 1
        }
      ]
    }
  },
  methods: {
    doSearch() {
      uni.showToast({ title: '搜索功能', icon: 'none' })
    },
    toggleSort(type) {
      this.sortBy = type
    },
    contactCustomer(customer) {
      uni.showToast({ title: '联系 ' + customer.name, icon: 'none' })
    }
  }
}
</script>

<style lang="scss" scoped>
.customer-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.search-bar {
  background: #fff;
  padding: 20rpx 24rpx;
  display: flex;
  gap: 16rpx;
}

.search-input {
  flex: 1;
  height: 72rpx;
  background: #f5f5f5;
  border-radius: 36rpx;
  padding: 0 30rpx;
  font-size: 28rpx;
}

.search-btn {
  width: 120rpx;
  height: 72rpx;
  line-height: 72rpx;
  background: #667eea;
  color: #fff;
  text-align: center;
  border-radius: 36rpx;
  font-size: 28rpx;
}

.customer-stats {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  justify-content: space-around;
  padding: 30rpx 20rpx;
}

.stats-item {
  text-align: center;
}

.stats-item .value {
  font-size: 36rpx;
  font-weight: 700;
  display: block;
}

.stats-item .label {
  font-size: 22rpx;
  opacity: 0.8;
  margin-top: 8rpx;
  display: block;
}

.filter-bar {
  background: #fff;
  display: flex;
  padding: 20rpx 24rpx;
  justify-content: space-around;
  border-bottom: 2rpx solid #eee;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 28rpx;
  color: #666;
}

.arrow {
  font-size: 20rpx;
}

.arrow.active {
  color: #667eea;
}

.customer-list {
  padding: 24rpx;
  height: calc(100vh - 400rpx);
}

.customer-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  display: flex;
  align-items: center;
}

.avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  margin-right: 20rpx;
}

.customer-info {
  flex: 1;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.phone {
  font-size: 24rpx;
  color: #999;
}

.tags {
  display: flex;
  gap: 12rpx;
  margin-top: 10rpx;
}

.tag {
  font-size: 20rpx;
  background: #f0f0f0;
  color: #666;
  padding: 4rpx 12rpx;
  border-radius: 4rpx;
}

.stats-row {
  display: flex;
  gap: 20rpx;
  margin-top: 12rpx;
}

.stats-row text {
  font-size: 24rpx;
  color: #999;
}

.action-btn {
  padding: 16rpx 24rpx;
  background: #667eea;
  color: #fff;
  font-size: 24rpx;
  border-radius: 8rpx;
}

.empty-state {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
  font-size: 28rpx;
}
</style>