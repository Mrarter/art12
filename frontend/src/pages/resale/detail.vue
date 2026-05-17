<template>
  <view class="page">
    <view class="nav-bar">
      <view class="nav-icon" @click="goBack">‹</view>
      <view class="nav-title">转售详情</view>
      <view class="nav-icon">&nbsp;</view>
    </view>

    <view class="loading" v-if="loading">加载中...</view>

    <view class="content" v-if="!loading && record">
      <view class="hero-card">
        <view class="price-block">
          <text class="price-label">转售价格</text>
          <text class="price">¥{{ formatPrice(record.resalePrice) }}</text>
        </view>
      </view>

      <view class="card">
        <view class="section-title">交易信息</view>
        <view class="info-row"><text class="label">作品ID</text><text class="value">{{ record.artworkId }}</text></view>
        <view class="info-row"><text class="label">卖家ID</text><text class="value">{{ record.sellerUserId }}</text></view>
        <view class="info-row"><text class="label">状态</text><text class="value status-text">{{ statusLabel(record.status) }}</text></view>
        <view class="info-row"><text class="label">发布时间</text><text class="value">{{ formatTime(record.createdTime) }}</text></view>
      </view>

      <view class="card">
        <view class="section-title">费用明细</view>
        <view class="fee-item">
          <text class="fee-label">转售价格</text>
          <text class="fee-value">¥{{ formatPrice(record.resalePrice) }}</text>
        </view>
        <view class="fee-item">
          <text class="fee-label">- 艺术家收益 (5%)</text>
          <text class="fee-value artist-color">¥{{ formatPrice(record.artistIncome) }}</text>
        </view>
        <view class="fee-item">
          <text class="fee-label">- 平台服务费 (10%)</text>
          <text class="fee-value platform-color">¥{{ formatPrice(record.platformFee) }}</text>
        </view>
        <view class="fee-item divider">
          <text class="fee-label">= 卖家实际收入</text>
          <text class="fee-value seller-color">¥{{ formatPrice(record.sellerIncome) }}</text>
        </view>
      </view>

      <view class="action-section" v-if="record.status === 'pending'">
        <button class="action-btn buy" @click="handleBuy">立即购买</button>
        <text class="action-hint">购买后款项将分配给艺术家、平台和卖家</text>
      </view>
      <view class="action-section" v-else-if="record.status === 'paid'">
        <text class="status-info">等待交易完成...</text>
      </view>
      <view class="action-section" v-else>
        <text class="status-info">该转售已{{ statusLabel(record.status) }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getResaleDetail } from '@/api/resale'
import { createResaleOrder } from '@/api/order'

export default {
  data() {
    return {
      id: null,
      record: null,
      loading: true,
      buying: false
    }
  },
  onLoad(options) { this.id = options.id; this.fetchDetail() },
  methods: {
    async fetchDetail() {
      this.loading = true
      try {
        const data = await getResaleDetail(this.id)
        this.record = data
      } catch (e) { uni.showToast({ title: '获取详情失败', icon: 'none' }) }
      finally { this.loading = false }
    },
    formatPrice(p) { return p ? Number(p).toFixed(2) : '0.00' },
    formatTime(t) { return t ? t.substring(0, 16).replace('T', ' ') : '' },
    statusLabel(s) {
      const map = { pending: '待售', paid: '已支付', completed: '已完成', cancel: '已取消' }
      return map[s] || s
    },
    goBack() { uni.navigateBack() },
    async handleBuy() {
      if (this.buying || !this.record) return
      uni.showModal({
        title: '确认购买',
        content: `确认以 ¥${this.formatPrice(this.record.resalePrice)} 购买此转售作品？`,
        success: async (res) => {
          if (!res.confirm) return
          this.buying = true
          try {
            const order = await createResaleOrder({
              resaleId: this.record.id,
              resalePrice: this.record.resalePrice,
              artworkId: this.record.artworkId,
              addressId: -1 // 使用默认地址
            })
            if (order && order.id) {
              uni.showToast({ title: '订单创建成功', icon: 'success' })
              setTimeout(() => {
                uni.navigateTo({ url: `/pages/order/pay?orderId=${order.id}` })
              }, 1000)
            }
          } catch (e) {
            uni.showToast({ title: e.message || '购买失败', icon: 'none' })
          }
          finally { this.buying = false }
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
.loading { text-align: center; padding: 100rpx; color: #666; font-size: 28rpx; }
.hero-card { background: linear-gradient(135deg, #1A1A1A 0%, #2A1F0E 100%); padding: 40rpx; text-align: center; }
.price-block { padding: 30rpx 0; }
.price-label { font-size: 26rpx; color: #999; }
.price { font-size: 60rpx; color: #D4AF37; font-weight: 700; display: block; margin-top: 10rpx; }
.card { background: #1A1A1A; border-radius: 16rpx; margin: 20rpx; padding: 24rpx; border: 1rpx solid #333; }
.section-title { font-size: 28rpx; color: #D4AF37; font-weight: 500; margin-bottom: 20rpx; border-bottom: 1rpx solid #2A2A2A; padding-bottom: 12rpx; }
.info-row { display: flex; justify-content: space-between; padding: 12rpx 0; font-size: 26rpx; }
.info-row .label { color: #999; }
.info-row .value { color: #E8E0D0; }
.status-text { color: #D4AF37; }
.fee-item { display: flex; justify-content: space-between; padding: 14rpx 0; font-size: 26rpx; }
.fee-item .fee-label { color: #999; }
.fee-item .fee-value { color: #E8E0D0; font-weight: 500; }
.fee-item.divider { border-top: 1rpx solid #2A2A2A; margin-top: 10rpx; padding-top: 20rpx; }
.artist-color { color: #67C23A; }
.platform-color { color: #D4AF37; }
.seller-color { color: #409EFF; }
.action-section { padding: 30rpx; text-align: center; }
.action-btn { width: 100%; padding: 24rpx; border-radius: 12rpx; font-size: 30rpx; border: none; }
.action-btn.buy { background: #D4AF37; color: #1A1A1A; font-weight: 600; }
.action-hint { display: block; font-size: 22rpx; color: #666; margin-top: 16rpx; }
.status-info { font-size: 28rpx; color: #666; text-align: center; display: block; padding: 40rpx; }
</style>
