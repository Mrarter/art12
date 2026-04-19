<template>
  <view class="auction-page">
    <!-- 拍卖头部信息 -->
    <view class="auction-header">
      <view class="header-info">
        <text class="auction-title">正在拍卖</text>
        <text class="auction-count">{{ auctionList.length }}件作品</text>
      </view>
      <view class="header-tabs">
        <view class="tab-item" :class="{ active: currentTab === 'live' }" @click="switchTab('live')">正在拍卖</view>
        <view class="tab-item" :class="{ active: currentTab === 'upcoming' }" @click="switchTab('upcoming')">即将开始</view>
        <view class="tab-item" :class="{ active: currentTab === 'ended' }" @click="switchTab('ended')">已结束</view>
      </view>
    </view>

    <!-- 拍卖列表 -->
    <scroll-view class="auction-list" scroll-y>
      <view class="auction-card" v-for="item in filteredList" :key="item.id" @click="goAuctionDetail(item.id)">
        <image class="auction-image" :src="item.cover" mode="aspectFill"></image>
        <view class="auction-content">
          <view class="auction-name">{{ item.title }}</view>
          <view class="auction-author">{{ item.artistName }}</view>
          
          <view class="auction-price">
            <view class="price-item">
              <text class="price-label">当前价</text>
              <text class="current-price">¥{{ item.currentPrice }}</text>
            </view>
            <view class="price-item">
              <text class="price-label">起拍价</text>
              <text class="start-price">¥{{ item.startPrice }}</text>
            </view>
          </view>
          
          <view class="auction-status">
            <view class="status-badge" :class="getStatusClass(item)">
              <text v-if="item.status === 'live'">
                <u-count-to :start-val="0" :end-val="item.remainTime" :decimals="0" :duration="0"></u-count-to>
                秒
              </text>
              <text v-else-if="item.status === 'upcoming'">即将开始</text>
              <text v-else>已结束</text>
            </view>
            <view class="bid-count">{{ item.bidCount }}次出价</view>
          </view>
        </view>
        
        <view class="bid-btn" v-if="item.status === 'live'" @click.stop="goAuctionDetail(item.id)">
          去出价
        </view>
      </view>
      
      <!-- 空状态 -->
      <view class="empty-state" v-if="filteredList.length === 0">
        <image class="empty-icon" src="/static/icons/auction-empty.png" mode="aspectFit"></image>
        <text class="empty-text">暂无{{ getTabLabel() }}的拍卖</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      currentTab: 'live',
      auctionList: []
    }
  },
  
  computed: {
    filteredList() {
      return this.auctionList.filter(item => item.status === this.currentTab)
    }
  },
  
  onLoad() {
    this.loadAuctionList()
  },
  
  methods: {
    async loadAuctionList() {
      // 模拟数据
      this.auctionList = [
        {
          id: 1,
          title: '山水长卷',
          artistName: '张大千',
          cover: '/static/product/demo1.jpg',
          currentPrice: 150000,
          startPrice: 100000,
          bidCount: 23,
          status: 'live',
          remainTime: 3600
        },
        {
          id: 2,
          title: '虾趣图',
          artistName: '齐白石',
          cover: '/static/product/demo2.jpg',
          currentPrice: 98000,
          startPrice: 50000,
          bidCount: 15,
          status: 'upcoming',
          remainTime: 0
        },
        {
          id: 3,
          title: '奔马图',
          artistName: '徐悲鸿',
          cover: '/static/product/demo3.jpg',
          currentPrice: 280000,
          startPrice: 150000,
          bidCount: 45,
          status: 'ended',
          remainTime: 0
        }
      ]
    },
    
    switchTab(tab) {
      this.currentTab = tab
    },
    
    getStatusClass(item) {
      return {
        'status-live': item.status === 'live',
        'status-upcoming': item.status === 'upcoming',
        'status-ended': item.status === 'ended'
      }
    },
    
    getTabLabel() {
      const labels = {
        live: '正在拍卖',
        upcoming: '即将开始',
        ended: '已结束'
      }
      return labels[this.currentTab]
    },
    
    goAuctionDetail(id) {
      uni.navigateTo({ url: `/pages/auction/detail?id=${id}` })
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
}

.tab-item.active {
  background: #fff;
  color: #333;
  font-weight: 500;
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
  position: relative;
}

.auction-image {
  width: 200rpx;
  height: 200rpx;
  border-radius: 12rpx;
  margin-right: 24rpx;
}

.auction-content {
  flex: 1;
}

.auction-name {
  font-size: 30rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 10rpx;
}

.auction-author {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 20rpx;
}

.auction-price {
  display: flex;
  margin-bottom: 16rpx;
}

.price-item {
  margin-right: 40rpx;
}

.price-label {
  display: block;
  font-size: 22rpx;
  color: #999;
  margin-bottom: 6rpx;
}

.current-price {
  font-size: 32rpx;
  color: #e74c3c;
  font-weight: 600;
}

.start-price {
  font-size: 26rpx;
  color: #666;
}

.auction-status {
  display: flex;
  align-items: center;
}

.status-badge {
  padding: 6rpx 16rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
  margin-right: 16rpx;
}

.status-live {
  background: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
}

.status-upcoming {
  background: rgba(52, 152, 219, 0.1);
  color: #3498db;
}

.status-ended {
  background: #f0f0f0;
  color: #999;
}

.bid-count {
  font-size: 22rpx;
  color: #999;
}

.bid-btn {
  position: absolute;
  right: 24rpx;
  bottom: 24rpx;
  padding: 12rpx 30rpx;
  background: #e74c3c;
  color: #fff;
  border-radius: 30rpx;
  font-size: 24rpx;
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
