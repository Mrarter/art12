<template>
  <view class="page">
    <view class="nav-bar">
      <view class="nav-icon" @click="goBack">‹</view>
      <view class="nav-title">转售市场</view>
      <view class="nav-icon" @click="toMyResales">我的</view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-if="isEmpty">
      <image class="empty-icon" src="/static/images/artwork-fallback.png" mode="aspectFit" />
      <text class="empty-text">暂无转售作品</text>
      <text class="empty-sub">当前没有藏家发布转售</text>
    </view>

    <view class="content" v-if="!isEmpty">
      <view class="stats-bar">
        <view class="stat-item">
          <text class="stat-num">{{ list.length }}</text>
          <text class="stat-label">在售作品</text>
        </view>
      </view>

      <view class="resale-list">
        <view class="resale-card" v-for="item in list" :key="item.id" @click="toDetail(item.id)">
          <view class="card-header">
            <text class="artwork-id">作品 #{{ item.artworkId }}</text>
            <text class="status pending">在售</text>
          </view>
          <view class="card-body">
            <view class="price-row">
              <text class="label">转售价</text>
              <text class="price">¥{{ formatPrice(item.resalePrice) }}</text>
            </view>
            <view class="fee-row">
              <text class="label">艺术家收益 <text class="pct">5%</text></text>
              <text class="value">¥{{ formatPrice(item.artistIncome) }}</text>
            </view>
            <view class="fee-row">
              <text class="label">平台服务费 <text class="pct">10%</text></text>
              <text class="value">¥{{ formatPrice(item.platformFee) }}</text>
            </view>
            <view class="seller-row">
              <text class="label">卖家ID</text>
              <text class="value">{{ item.sellerUserId }}</text>
            </view>
          </view>
          <view class="card-footer">
            <text class="time">{{ formatTime(item.createdTime) }}</text>
            <button class="buy-btn" @click.stop="toDetail(item.id)">查看</button>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getResaleList } from '@/api/resale'

export default {
  data() {
    return {
      list: [],
      page: 1,
      pageSize: 20,
      isEmpty: false,
      loading: false
    }
  },
  onLoad() { this.fetchList() },
  onPullDownRefresh() { this.page = 1; this.fetchList() },
  onReachBottom() { if (!this.loading) { this.page++; this.fetchList() } },
  methods: {
    async fetchList() {
      if (this.loading) return
      this.loading = true
      try {
        const data = await getResaleList({ page: this.page, pageSize: this.pageSize }) || {}
        const records = data.records || []
        if (this.page === 1) {
          this.list = records
        } else {
          this.list = [...this.list, ...records]
        }
        this.isEmpty = this.list.length === 0
      } catch (e) { /* */ }
      finally { this.loading = false; uni.stopPullDownRefresh() }
    },
    formatPrice(p) { return p ? Number(p).toFixed(2) : '0.00' },
    formatTime(t) { return t ? t.substring(0, 16).replace('T', ' ') : '' },
    goBack() { uni.navigateBack() },
    toMyResales() { uni.navigateTo({ url: '/pages/resale/my' }) },
    toDetail(id) { uni.navigateTo({ url: '/pages/resale/detail?id=' + id }) }
  }
}
</script>

<style>
.page { min-height: 100vh; background: #0D0D0D; color: #E8E0D0; }
.nav-bar { display: flex; align-items: center; justify-content: space-between; padding: 20rpx 30rpx; background: #1A1A1A; }
.nav-icon { font-size: 36rpx; color: #D4AF37; padding: 10rpx; }
.nav-title { font-size: 32rpx; color: #E8E0D0; font-weight: 500; }
.stats-bar { padding: 20rpx 30rpx; display: flex; gap: 40rpx; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-size: 40rpx; color: #D4AF37; font-weight: 600; }
.stat-label { font-size: 22rpx; color: #999; margin-top: 6rpx; }
.resale-list { padding: 0 20rpx 30rpx; }
.resale-card { background: #1A1A1A; border-radius: 16rpx; margin-bottom: 20rpx; overflow: hidden; border: 1rpx solid #333; }
.card-header { display: flex; justify-content: space-between; align-items: center; padding: 20rpx 24rpx; background: #222; }
.artwork-id { font-size: 26rpx; color: #D4AF37; font-weight: 500; }
.status { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 8rpx; }
.status.pending { background: #3A3500; color: #D4AF37; }
.card-body { padding: 20rpx 24rpx; }
.price-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.price-row .price { font-size: 36rpx; color: #D4AF37; font-weight: 600; }
.label { font-size: 24rpx; color: #999; }
.fee-row { display: flex; justify-content: space-between; padding: 6rpx 0; }
.fee-row .value { font-size: 24rpx; color: #CCC; }
.pct { color: #D4AF37; font-size: 22rpx; }
.seller-row { display: flex; justify-content: space-between; padding: 6rpx 0; margin-top: 8rpx; border-top: 1rpx solid #2A2A2A; padding-top: 12rpx; }
.seller-row .value { font-size: 24rpx; color: #CCC; }
.card-footer { display: flex; justify-content: space-between; align-items: center; padding: 16rpx 24rpx; border-top: 1rpx solid #2A2A2A; }
.time { font-size: 22rpx; color: #666; }
.buy-btn { font-size: 26rpx; color: #D4AF37; background: #2A2A2A; border: 1rpx solid #D4AF37; border-radius: 12rpx; padding: 8rpx 28rpx; }
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding-top: 200rpx; }
.empty-icon { width: 160rpx; height: 160rpx; opacity: 0.5; }
.empty-text { font-size: 28rpx; color: #666; margin-top: 20rpx; }
.empty-sub { font-size: 24rpx; color: #444; margin-top: 10rpx; }
</style>
