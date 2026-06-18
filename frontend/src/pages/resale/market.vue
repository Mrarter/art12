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
            <image class="artwork-cover" :src="displayArtworkCover(item)" mode="aspectFill" />
            <view class="artwork-heading">
              <text class="artwork-title">{{ displayArtworkTitle(item) }}</text>
              <text class="artwork-id">作品 {{ displayArtworkUid(item) }}</text>
            </view>
            <text class="status pending">在售</text>
          </view>
          <view class="artwork-meta" v-if="displayArtworkMeta(item)">
            <text>{{ displayArtworkMeta(item) }}</text>
          </view>
          <view class="card-body">
            <view class="price-row">
              <text class="label">转售价</text>
              <text class="price">¥{{ formatPrice(item.resalePrice) }}</text>
            </view>
            <view class="seller-row">
              <text class="label">卖家UID</text>
              <text class="value">{{ displaySellerUid(item) }}</text>
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
import { normalizeImageUrl } from '@/api/product'
import { fenToYuan, formatYuanNumber } from '@/utils/price'

const FALLBACK_COVER = '/static/images/artwork-fallback.png'

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
    formatPrice(price) {
      return formatYuanNumber(fenToYuan(price))
    },
    formatTime(t) { return t ? t.substring(0, 16).replace('T', ' ') : '' },
    displayArtworkUid(item) {
      return item?.artworkUid || item?.artworkCode || this.formatFallbackUid('ART', item?.artworkId)
    },
    displayArtworkTitle(item) {
      return item?.artworkTitle || item?.title || item?.artworkName || '未命名作品'
    },
    displayArtworkCover(item) {
      return normalizeImageUrl(item?.artworkCoverImage || item?.coverImage || item?.cover || item?.image) || FALLBACK_COVER
    },
    displayArtworkMeta(item) {
      const parts = [
        item?.artistName,
        item?.categoryName || item?.artworkArtType || item?.artType || item?.category,
        item?.artworkMedium || item?.medium,
        item?.artworkSize || item?.size,
        item?.artworkYear || item?.year
      ].filter(Boolean)
      return parts.join(' · ')
    },
    displaySellerUid(item) {
      return item?.sellerUid || item?.sellerUserUid || this.formatFallbackUid('USR', item?.sellerUserId)
    },
    formatFallbackUid(prefix, id) {
      if (!id && id !== 0) return '-'
      return `${prefix}${String(id).padStart(16, '0')}`
    },
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
.card-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 20rpx; padding: 20rpx 24rpx 14rpx; background: #222; }
.artwork-cover { flex: 0 0 136rpx; width: 136rpx; height: 136rpx; border-radius: 10rpx; background: #111; border: 1rpx solid #333; }
.artwork-heading { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8rpx; }
.artwork-title { font-size: 28rpx; color: #E8E0D0; font-weight: 600; line-height: 1.35; word-break: break-word; overflow-wrap: anywhere; }
.artwork-id { font-size: 22rpx; color: #D4AF37; line-height: 1.35; word-break: break-all; overflow-wrap: anywhere; }
.artwork-meta { padding: 0 24rpx 18rpx; background: #222; color: #999; font-size: 22rpx; line-height: 1.5; word-break: break-word; overflow-wrap: anywhere; }
.status { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 8rpx; }
.status.pending { background: #3A3500; color: #D4AF37; }
.card-body { padding: 20rpx 24rpx; }
.price-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.price-row .price { font-size: 36rpx; color: #D4AF37; font-weight: 600; }
.label { font-size: 24rpx; color: #999; }
.seller-row { display: flex; justify-content: space-between; gap: 20rpx; padding: 6rpx 0; margin-top: 8rpx; border-top: 1rpx solid #2A2A2A; padding-top: 12rpx; }
.seller-row .value { min-width: 0; font-size: 24rpx; color: #CCC; text-align: right; word-break: break-all; overflow-wrap: anywhere; }
.card-footer { display: flex; justify-content: space-between; align-items: center; padding: 16rpx 24rpx; border-top: 1rpx solid #2A2A2A; }
.time { font-size: 22rpx; color: #666; }
.buy-btn { font-size: 26rpx; color: #D4AF37; background: #2A2A2A; border: 1rpx solid #D4AF37; border-radius: 12rpx; padding: 8rpx 28rpx; }
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding-top: 200rpx; }
.empty-icon { width: 160rpx; height: 160rpx; opacity: 0.5; }
.empty-text { font-size: 28rpx; color: #666; margin-top: 20rpx; }
.empty-sub { font-size: 24rpx; color: #444; margin-top: 10rpx; }
</style>
