<template>
  <view class="page">
    <view class="nav-bar">
      <view class="nav-icon" @click="goBack">‹</view>
      <view class="nav-title">转售详情</view>
      <view class="nav-icon">&nbsp;</view>
    </view>

    <view class="loading" v-if="loading">加载中...</view>

    <view class="content" v-if="!loading && record">
      <view class="artwork-card">
        <image class="artwork-cover" :src="artworkCover" mode="aspectFill" />
        <view class="artwork-info">
          <text class="artwork-title">{{ artworkTitle }}</text>
          <text class="artwork-id">作品 {{ artworkUid }}</text>
          <text class="artwork-meta" v-if="artworkMeta">{{ artworkMeta }}</text>
        </view>
      </view>

      <view class="hero-card">
        <view class="price-block">
          <text class="price-label">转售价格</text>
          <text class="price">¥{{ formatPrice(record.resalePrice) }}</text>
        </view>
      </view>

      <view class="card">
        <view class="section-title">交易信息</view>
        <view class="info-row"><text class="label">作品UID</text><text class="value">{{ artworkUid }}</text></view>
        <view class="info-row"><text class="label">卖家UID</text><text class="value">{{ sellerUid }}</text></view>
        <view class="info-row" v-if="record.buyerUserId"><text class="label">买家UID</text><text class="value">{{ buyerUid }}</text></view>
        <view class="info-row"><text class="label">状态</text><text class="value status-text">{{ statusLabel(record.status) }}</text></view>
        <view class="info-row"><text class="label">发布时间</text><text class="value">{{ formatTime(record.createdTime) }}</text></view>
      </view>

      <view class="card manage-card" v-if="isMyResale">
        <view class="section-title">转售管理</view>
        <view class="info-row">
          <text class="label">当前状态</text>
          <text class="value status-text">{{ statusLabel(record.status) }}</text>
        </view>
        <view class="toggle-row" v-if="record.status === 'pending'">
          <view class="toggle-copy">
            <text class="toggle-title">使用平台评估与热度涨价机制</text>
            <text class="toggle-desc">同意后，将按后台全局价格调控机制自动更新转售价。</text>
          </view>
          <switch
            :checked="platformPricingEnabled"
            :disabled="togglingPricing"
            color="#D4AF37"
            @change="handlePlatformPricingToggle"
          />
        </view>
        <text class="income-tip">当前作品评估价格区间：¥{{ formatPrice(estimateMinPrice) }} - ¥{{ formatPrice(estimateMaxPrice) }}</text>
        <text class="income-tip secondary">平台服务费{{ formatPercent(record.platformFeeRate) }}%，鼓励艺术家创作，作者获得佣金{{ formatPercent(record.artistIncomeRate) }}%。</text>
        <view class="manage-actions" v-if="record.status === 'pending'">
          <button class="action-btn secondary" :class="{ disabled: platformPricingEnabled }" :disabled="platformPricingEnabled" @click="handleAdjustPrice">调整价格</button>
          <button class="action-btn danger" @click="handleCancel">取消转售</button>
        </view>
        <text class="action-hint" v-if="record.status === 'pending' && platformPricingEnabled">已启用平台评估与热度涨价机制，请先关闭后再手动调价。</text>
        <text class="action-hint" v-else-if="record.status !== 'pending'">已结束的转售不能再次调整价格。</text>
      </view>

      <view class="action-section" v-if="!isMyResale && record.status === 'pending'">
        <button class="action-btn buy" @click="handleBuy">立即购买</button>
        <text class="action-hint">购买后款项将分配给艺术家、平台和卖家</text>
      </view>
      <view class="action-section" v-else-if="!isMyResale && record.status === 'paid'">
        <text class="status-info">等待交易完成...</text>
      </view>
      <view class="action-section" v-else-if="!isMyResale">
        <text class="status-info">该转售已{{ statusLabel(record.status) }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { cancelResale, getResaleDetail, updatePlatformPricing, updateResalePrice } from '@/api/resale'
import { normalizeImageUrl } from '@/api/product'
import { getCurrentUserIdentity } from '@/utils/auth'
import { fenToYuan, formatYuanNumber } from '@/utils/price'

const FALLBACK_COVER = '/static/images/artwork-fallback.png'

export default {
  data() {
    return {
      id: null,
      record: null,
      loading: true,
      buying: false,
      togglingPricing: false,
      currentUserId: ''
    }
  },
  onLoad(options) {
    this.id = options.id
    this.currentUserId = String(getCurrentUserIdentity()?.id || '')
    this.fetchDetail()
  },
  computed: {
    artworkUid() {
      return this.record?.artworkUid || this.record?.artworkCode || this.record?.uid || this.formatFallbackUid('ART', this.record?.artworkId)
    },
    artworkTitle() {
      return this.record?.artworkTitle || this.record?.title || '未命名作品'
    },
    artworkCover() {
      return normalizeImageUrl(this.record?.artworkCoverImage) || FALLBACK_COVER
    },
    artworkMeta() {
      const parts = [
        this.record?.artistName,
        this.record?.categoryName || this.record?.artworkArtType,
        this.record?.artworkMedium,
        this.record?.artworkSize,
        this.record?.artworkYear
      ].filter(Boolean)
      return parts.join(' · ')
    },
    sellerUid() {
      return this.record?.sellerUid || this.record?.sellerUserUid || this.formatFallbackUid('USR', this.record?.sellerUserId)
    },
    buyerUid() {
      return this.record?.buyerUid || this.record?.buyerUserUid || this.formatFallbackUid('USR', this.record?.buyerUserId)
    },
    estimateMinPrice() {
      return this.record?.suggestedMinPrice || this.record?.artworkCurrentPrice || this.record?.resalePrice || 0
    },
    estimateMaxPrice() {
      return this.record?.suggestedMaxPrice || this.record?.artworkCurrentPrice || this.record?.resalePrice || 0
    },
    platformPricingEnabled() {
      return Boolean(this.record?.platformPricingEnabled)
    },
    isMyResale() {
      return Boolean(this.record?.sellerUserId) && String(this.record.sellerUserId) === this.currentUserId
    }
  },
  methods: {
    async fetchDetail() {
      this.loading = true
      try {
        const data = await getResaleDetail(this.id)
        this.record = data
      } catch (e) { uni.showToast({ title: '获取详情失败', icon: 'none' }) }
      finally { this.loading = false }
    },
    formatPrice(price) {
      return formatYuanNumber(fenToYuan(price))
    },
    formatPercent(value) {
      const num = Number(value)
      return Number.isFinite(num) && num > 0 ? num : 0
    },
    formatTime(t) { return t ? t.substring(0, 16).replace('T', ' ') : '' },
    formatFallbackUid(prefix, id) {
      if (!id && id !== 0) return '-'
      return `${prefix}${String(id).padStart(16, '0')}`
    },
    statusLabel(s) {
      const map = { pending: '在售', paid: '已支付', completed: '已完成', cancel: '已取消' }
      return map[s] || s
    },
    goBack() { uni.navigateBack() },
    handleAdjustPrice() {
      if (!this.record || this.record.status !== 'pending') return
      if (this.platformPricingEnabled) {
        uni.showToast({ title: '请先关闭平台调价机制', icon: 'none' })
        return
      }
      uni.showModal({
        title: '调整转售价',
        editable: true,
        placeholderText: '请输入新的转售价',
        content: String(this.record.resalePrice || ''),
        success: async (res) => {
          if (!res.confirm) return
          const resalePrice = Number(res.content)
          if (!Number.isFinite(resalePrice) || resalePrice <= 0) {
            uni.showToast({ title: '请输入有效价格', icon: 'none' })
            return
          }
          try {
            const data = await updateResalePrice(this.id, { resalePrice: resalePrice.toFixed(2) })
            this.record = data || this.record
            uni.showToast({ title: '价格已更新', icon: 'success' })
          } catch (e) {
            uni.showToast({ title: e.message || '调价失败', icon: 'none' })
          }
        }
      })
    },
    handlePlatformPricingToggle(e) {
      if (!this.record || this.record.status !== 'pending' || this.togglingPricing) return
      const enabled = Boolean(e?.detail?.value)
      if (!enabled) {
        this.submitPlatformPricing(false)
        return
      }
      uni.showModal({
        title: '启用平台调价',
        content: '启用后，系统将根据后台全局价格调控机制自动更新当前转售价。',
        success: (res) => {
          if (!res.confirm) {
            this.$nextTick(() => {
              this.record = { ...this.record, platformPricingEnabled: false }
            })
            return
          }
          this.submitPlatformPricing(true)
        }
      })
    },
    async submitPlatformPricing(enabled) {
      this.togglingPricing = true
      try {
        const data = await updatePlatformPricing(this.id, { enabled })
        this.record = data || this.record
        uni.showToast({ title: enabled ? '已启用平台调价' : '已关闭平台调价', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e.message || '切换失败', icon: 'none' })
        this.fetchDetail()
      } finally {
        this.togglingPricing = false
      }
    },
    handleCancel() {
      if (!this.record || this.record.status !== 'pending') return
      uni.showModal({
        title: '取消转售',
        content: '确认取消这条转售记录？',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await cancelResale(this.id)
            uni.showToast({ title: '已取消转售', icon: 'success' })
            this.fetchDetail()
          } catch (e) {
            uni.showToast({ title: e.message || '取消失败', icon: 'none' })
          }
        }
      })
    },
    handleBuy() {
      if (this.buying || !this.record) return
      const query = [
        `resaleId=${encodeURIComponent(this.record.id || '')}`,
        `artworkId=${encodeURIComponent(this.record.artworkId || '')}`,
        `resalePrice=${encodeURIComponent(this.record.resalePrice || 0)}`,
        `artworkUid=${encodeURIComponent(this.artworkUid || '')}`,
        `sellerUid=${encodeURIComponent(this.sellerUid || '')}`
      ].join('&')
      uni.navigateTo({
        url: `/pages/order/confirm?${query}`
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
.artwork-card { display: flex; gap: 20rpx; margin: 20rpx; padding: 24rpx; background: #1A1A1A; border-radius: 16rpx; border: 1rpx solid #333; }
.artwork-cover { flex: 0 0 160rpx; width: 160rpx; height: 160rpx; border-radius: 12rpx; background: #111; }
.artwork-info { flex: 1; min-width: 0; display: flex; flex-direction: column; justify-content: center; gap: 10rpx; }
.artwork-title { font-size: 30rpx; color: #E8E0D0; font-weight: 600; line-height: 1.35; word-break: break-word; }
.artwork-id { font-size: 22rpx; color: #D4AF37; line-height: 1.35; word-break: break-all; }
.artwork-meta { font-size: 22rpx; color: #999; line-height: 1.5; word-break: break-word; }
.hero-card { background: linear-gradient(135deg, #1A1A1A 0%, #2A1F0E 100%); padding: 40rpx; text-align: center; }
.price-block { padding: 30rpx 0; }
.price-label { font-size: 26rpx; color: #999; }
.price { font-size: 60rpx; color: #D4AF37; font-weight: 700; display: block; margin-top: 10rpx; }
.card { background: #1A1A1A; border-radius: 16rpx; margin: 20rpx; padding: 24rpx; border: 1rpx solid #333; }
.section-title { font-size: 28rpx; color: #D4AF37; font-weight: 500; margin-bottom: 20rpx; border-bottom: 1rpx solid #2A2A2A; padding-bottom: 12rpx; }
.info-row { display: flex; justify-content: space-between; gap: 24rpx; padding: 12rpx 0; font-size: 26rpx; }
.info-row .label { flex: 0 0 auto; color: #999; }
.info-row .value { min-width: 0; color: #E8E0D0; text-align: right; word-break: break-all; overflow-wrap: anywhere; }
.status-text { color: #D4AF37; }
.toggle-row { display: flex; align-items: center; justify-content: space-between; gap: 20rpx; padding: 8rpx 0 14rpx; }
.toggle-copy { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8rpx; }
.toggle-title { font-size: 25rpx; color: #B7B0A5; line-height: 1.5; word-break: break-word; }
.toggle-desc { font-size: 21rpx; color: #5F5A54; line-height: 1.6; word-break: break-word; }
.income-tip { display: block; margin-top: 6rpx; color: #767068; font-size: 21rpx; line-height: 1.6; word-break: break-word; }
.income-tip.secondary { color: #68625b; margin-top: 6rpx; }
.manage-actions { display: flex; gap: 16rpx; margin-top: 24rpx; }
.action-section { padding: 30rpx; text-align: center; }
.action-btn { width: 100%; padding: 24rpx; border-radius: 12rpx; font-size: 30rpx; border: none; }
.manage-actions .action-btn { flex: 1; width: auto; }
.action-btn.buy { background: #D4AF37; color: #1A1A1A; font-weight: 600; }
.action-btn.secondary { background: #2A2A2A; color: #D4AF37; border: 1rpx solid #D4AF37; }
.action-btn.danger { background: #2A1616; color: #F56C6C; border: 1rpx solid #F56C6C; }
.action-btn.disabled { opacity: 0.45; }
.action-hint { display: block; font-size: 22rpx; color: #666; margin-top: 16rpx; }
.status-info { font-size: 28rpx; color: #666; text-align: center; display: block; padding: 40rpx; }
</style>
