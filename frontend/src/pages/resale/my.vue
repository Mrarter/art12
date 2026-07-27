<template>
  <view class="page">
    <view class="nav-bar">
      <view class="nav-icon" @click="goBack">‹</view>
      <view class="nav-title">我的转售</view>
      <view class="nav-icon" @click="toMarket">市场</view>
    </view>

    <view class="tabs">
      <view class="tab" :class="{ active: tab === 'all' }" @click="switchTab('all')">全部</view>
      <view class="tab" :class="{ active: tab === 'pending' }" @click="switchTab('pending')">在售</view>
      <view class="tab" :class="{ active: tab === 'completed' }" @click="switchTab('completed')">已完成</view>
      <view class="tab" :class="{ active: tab === 'cancel' }" @click="switchTab('cancel')">已取消</view>
    </view>

    <view class="empty-state" v-if="isEmpty && !loading">
      <text class="empty-text">暂无转售记录</text>
      <text class="empty-sub">您还没有发布过转售</text>
      <button class="empty-btn" @click="toPublish">发布转售</button>
    </view>

    <view class="list" v-if="!isEmpty">
      <view class="item" v-for="item in list" :key="item.id" @click="toDetail(item.id)">
        <view class="item-header">
          <text class="artwork-label">作品 {{ displayArtworkUid(item) }}</text>
          <text class="item-status" :class="item.status">{{ statusLabel(item.status) }}</text>
        </view>
        <view class="item-body">
          <text class="price">¥{{ formatPrice(item.resalePrice) }}</text>
          <text class="seller-income">预计收入 ¥{{ formatPrice(item.sellerIncome) }}</text>
        </view>
        <view class="item-footer">
          <text class="time">{{ formatTime(item.createdTime) }}</text>
          <text class="cancel-btn" v-if="item.status === 'pending'" @click.stop="handleCancel(item.id)">取消转售</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getMyResales, cancelResale } from '@/api/resale'
import { fenToYuan, formatYuanNumber } from '@/utils/price'

export default {
  data() {
    return {
      list: [],
      tab: 'all',
      page: 1,
      pageSize: 20,
      isEmpty: false,
      loading: false
    }
  },
  onLoad() { this.fetchList() },
  methods: {
    async fetchList() {
      if (this.loading) return
      this.loading = true
      try {
        const params = { page: this.page, pageSize: this.pageSize }
        if (this.tab !== 'all') params.status = this.tab
        const data = await getMyResales(params) || {}
        const records = data.records || []
        this.list = this.page === 1 ? records : [...this.list, ...records]
        this.isEmpty = this.list.length === 0
      } catch (e) { /* */ }
      finally { this.loading = false }
    },
    switchTab(t) {
      if (this.tab === t) return
      this.tab = t
      this.page = 1
      this.list = []
      this.fetchList()
    },
    formatPrice(price) {
      return formatYuanNumber(fenToYuan(price))
    },
    formatTime(t) { return t ? t.substring(0, 16).replace('T', ' ') : '' },
    displayArtworkUid(item) {
      return item?.artworkUid || item?.artworkCode || this.formatFallbackUid('ART', item?.artworkId)
    },
    formatFallbackUid(prefix, id) {
      if (!id && id !== 0) return '-'
      return `${prefix}${String(id).padStart(16, '0')}`
    },
    statusLabel(s) {
      const map = { pending: '在售', paid: '已支付', completed: '已完成', cancel: '已取消' }
      return map[s] || s
    },
    goBack() { uni.navigateBack() },
    toMarket() { uni.switchTab({ url: '/pages/resale/market' }) },
    toDetail(id) { uni.navigateTo({ url: '/pages/resale/detail?id=' + id }) },
    toPublish() { uni.navigateTo({ url: '/pages/resale/publish' }) },
    async handleCancel(id) {
      uni.showModal({
        title: '取消转售',
        content: '确认取消此转售？',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await cancelResale(id)
            uni.showToast({ title: '已取消', icon: 'success' })
            this.page = 1
            this.list = []
            this.fetchList()
          } catch (e) {
            uni.showToast({ title: e.message || '取消失败', icon: 'none' })
          }
        }
      })
    }
  }
}
</script>

<style>
.page { min-height: 100vh; background: #0D0D0D; color: #E8E0D0; }
.nav-bar { display: flex; align-items: center; justify-content: space-between; padding: 20rpx 30rpx; background: #1A1A1A; }
.nav-icon { font-size: 36rpx; color: #D4AF37; padding: 10rpx; }
.nav-title { font-size: 32rpx; color: #E8E0D0; font-weight: 500; }
.tabs { display: flex; background: #1A1A1A; padding: 0 10rpx; }
.tab { flex: 1; text-align: center; padding: 20rpx 0; font-size: 26rpx; color: #666; border-bottom: 3rpx solid transparent; }
.tab.active { color: #D4AF37; border-bottom-color: #D4AF37; }
.list { padding: 20rpx; }
.item { background: #1A1A1A; border-radius: 16rpx; margin-bottom: 16rpx; padding: 20rpx 24rpx; border: 1rpx solid #333; }
.item-header { display: flex; justify-content: space-between; align-items: center; gap: 20rpx; }
.artwork-label { flex: 1; min-width: 0; font-size: 26rpx; color: #D4AF37; font-weight: 500; word-break: break-all; overflow-wrap: anywhere; }
.item-status { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 8rpx; }
.item-status.pending { background: #3A3500; color: #D4AF37; }
.item-status.paid { background: #1A3A1A; color: #67C23A; }
.item-status.completed { background: #1A3A1A; color: #67C23A; }
.item-status.cancel { background: #2A2A2A; color: #999; }
.item-body { display: flex; justify-content: space-between; align-items: center; padding: 16rpx 0; }
.price { font-size: 34rpx; color: #D4AF37; font-weight: 600; }
.seller-income { font-size: 24rpx; color: #409EFF; }
.item-footer { display: flex; justify-content: space-between; align-items: center; border-top: 1rpx solid #2A2A2A; padding-top: 14rpx; }
.time { font-size: 22rpx; color: #666; }
.cancel-btn { font-size: 24rpx; color: #F56C6C; }
.empty-state { display: flex; flex-direction: column; align-items: center; padding-top: 200rpx; }
.empty-text { font-size: 28rpx; color: #666; }
.empty-sub { font-size: 24rpx; color: #444; margin-top: 10rpx; }
.empty-btn { margin-top: 30rpx; background: #D4AF37; color: #1A1A1A; font-size: 28rpx; border-radius: 12rpx; padding: 16rpx 50rpx; }
</style>
