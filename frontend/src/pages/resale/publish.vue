<template>
  <view class="page">
    <view class="form">
      <view class="card">
        <view class="work-info-row" v-if="artworkTitle">
          <text class="work-info-label">作品</text>
          <text class="work-info-value">{{ artworkTitle }}</text>
          <text class="work-info-id">{{ artworkUid }}</text>
        </view>
        <view class="field" v-else>
          <text class="field-label">作品ID</text>
          <input class="field-input" v-model="artworkId" type="text" placeholder="请输入作品ID" :disabled="!!artworkTitle" />
        </view>
        <view class="field">
          <text class="field-label">转售价格（元）</text>
          <view class="price-input">
            <text class="currency">¥</text>
            <input class="field-input" v-model="resalePrice" type="digit" placeholder="请输入转售价格" />
          </view>
        </view>
        <view class="estimate-row" v-if="estimatedPriceFen > 0">
          <text class="estimate-label">作品评估价格</text>
          <text class="estimate-value">¥{{ formatFenPrice(estimatedPriceFen) }}</text>
        </view>
      </view>

      <view class="card fee-preview" v-if="resalePrice && Number(resalePrice) > 0">
        <view class="section-title">费用预览</view>
        <view class="preview-row">
          <text>转售价格</text>
          <text>¥{{ formatPrice(computedPrice) }}</text>
        </view>
        <view class="preview-row divider">
          <text>卖家实际收入</text>
          <text style="color:#409EFF;">¥{{ formatPrice(sellerIncome) }}</text>
        </view>
      </view>

      <view class="tips">
        <text class="tip-title">温馨提示</text>
        <text class="tip-text">• 发布转售即表示您同意平台服务协议</text>
        <text class="tip-text">• 转售成功后，艺术家将获得转售价格5%的持续收益</text>
        <text class="tip-text">• 平台将收取转售价格15%的服务费</text>
        <text class="tip-text">• 您将获得转售价格扣除收益和费用后的80%</text>
      </view>

      <button class="publish-btn" :disabled="!canPublish" @click="handlePublish">发布转售</button>
    </view>
  </view>
</template>

<script>
import { publishResale } from '@/api/resale'
import { getProductDetail } from '@/api/product'
import { fenToYuan, formatYuanNumber } from '@/utils/price'

export default {
  data() {
    return {
      artworkId: '',
      artworkUid: '',
      resalePrice: '',
      artworkTitle: '',
      estimatedPriceFen: 0,
      submitting: false,
      loadingInfo: false
    }
  },
  computed: {
    computedPrice() { return Number(this.resalePrice) || 0 },
    artistFee() { return (this.computedPrice * 0.05).toFixed(2) },
    platformFee() { return (this.computedPrice * 0.15).toFixed(2) },
    sellerIncome() { return (this.computedPrice * 0.80).toFixed(2) },
    canPublish() { return this.artworkId && this.resalePrice > 0 && !this.submitting }
  },

  onLoad(options) {
    if (options.artworkId) {
      this.artworkId = options.artworkId
      // 兼容历史入口：有的页面传分，有的页面直接传元
      if (options.price) {
        this.resalePrice = formatYuanNumber(this.toYuanAmount(options.price))
      }
      this.loadArtworkInfo(options.artworkId)
    }
  },

  methods: {
    formatPrice(p) { return Number(p).toFixed(2) },
    formatFenPrice(price) {
      return formatYuanNumber(this.toYuanAmount(price))
    },
    toYuanAmount(value) {
      if (value === null || value === undefined || value === '') return 0
      const num = Number(value)
      if (!Number.isFinite(num)) return 0
      if (String(value).includes('.')) return num
      return num >= 1000 ? fenToYuan(num) : num
    },

    async loadArtworkInfo(id) {
      this.loadingInfo = true
      try {
        const detail = await getProductDetail(id)
        if (detail) {
          this.artworkTitle = detail.title || ''
          this.artworkUid = detail.uid || detail.artworkUid || detail.artworkCode || this.formatFallbackUid('ART', id)
          this.estimatedPriceFen = Number(
            detail.currentPrice ||
            detail.current_price ||
            detail.displayPrice ||
            detail.price ||
            0
          )
          // 未传 price 参数时从作品信息中预填
          if (!this.resalePrice && detail.currentPrice) {
            this.resalePrice = formatYuanNumber(this.toYuanAmount(detail.currentPrice))
          }
        }
      } catch (e) {
        console.warn('[发布转售] 加载作品信息失败:', e)
      } finally {
        this.loadingInfo = false
      }
    },

    formatFallbackUid(prefix, id) {
      if (!id && id !== 0) return '-'
      return `${prefix}${String(id).padStart(16, '0')}`
    },

    async handlePublish() {
      if (this.submitting) return
      if (!this.artworkId || !this.resalePrice) {
        uni.showToast({ title: '请填写完整信息', icon: 'none' })
        return
      }
      const price = Number(this.resalePrice)
      if (price <= 0) {
        uni.showToast({ title: '价格必须大于0', icon: 'none' })
        return
      }
      this.submitting = true
      try {
        const res = await publishResale({
          artworkId: parseInt(this.artworkId),
          resalePrice: price
        })
        if (res) {
          uni.showToast({ title: '发布成功', icon: 'success' })
          setTimeout(() => {
            // 跳转到转售市场首页，让用户看到自己的发布
            uni.navigateTo({ url: '/pages/resale/market' })
          }, 1500)
        }
      } catch (e) {
        uni.showToast({ title: e.message || '发布失败', icon: 'none' })
      }
      finally { this.submitting = false }
    }
  }
}
</script>

<style>
.page { min-height: 100vh; background: #0D0D0D; color: #E8E0D0; }
.form { padding: 20rpx; }
.card { background: #1A1A1A; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; border: 1rpx solid #333; }
.field { margin-bottom: 24rpx; }
.field-label { font-size: 26rpx; color: #999; display: block; margin-bottom: 10rpx; }
.field-input { width: 100%; height: 80rpx; min-height: 80rpx; background: #0D0D0D; border: 1rpx solid #333; border-radius: 12rpx; padding: 0 20rpx; font-size: 28rpx; color: #E8E0D0; box-sizing: border-box; }
.field-input .uni-input-wrapper,
.field-input .uni-input-input { height: 100%; min-height: 100%; line-height: 80rpx; }
.price-input { display: flex; align-items: center; }
.price-input .field-input { flex: 1; min-width: 0; }
.work-info-row { display: flex; align-items: center; gap: 12rpx; padding: 10rpx 0 20rpx; border-bottom: 1rpx solid #2A2A2A; margin-bottom: 20rpx; }
.work-info-label { font-size: 24rpx; color: #999; margin-right: 12rpx; }
.work-info-value { min-width: 0; font-size: 28rpx; color: #D4AF37; flex: 1; font-weight: 500; }
.work-info-id { max-width: 300rpx; font-size: 22rpx; color: #666; text-align: right; word-break: break-all; overflow-wrap: anywhere; }
.estimate-row { display: flex; justify-content: space-between; align-items: center; padding-top: 8rpx; color: #E8E0D0; }
.estimate-label { font-size: 24rpx; color: #999; }
.estimate-value { font-size: 28rpx; color: #D4AF37; font-weight: 600; }
.currency { font-size: 32rpx; color: #D4AF37; margin-right: 12rpx; font-weight: 600; }
.section-title { font-size: 28rpx; color: #D4AF37; font-weight: 500; margin-bottom: 20rpx; border-bottom: 1rpx solid #2A2A2A; padding-bottom: 12rpx; }
.preview-row { display: flex; justify-content: space-between; padding: 10rpx 0; font-size: 26rpx; color: #E8E0D0; }
.preview-row.divider { border-top: 1rpx solid #2A2A2A; margin-top: 10rpx; padding-top: 16rpx; }
.tips { padding: 10rpx 10rpx 30rpx; }
.tip-title { font-size: 26rpx; color: #D4AF37; display: block; margin-bottom: 16rpx; }
.tip-text { font-size: 24rpx; color: #666; display: block; padding: 4rpx 0; }
.publish-btn { width: 100%; background: #D4AF37; color: #1A1A1A; font-size: 30rpx; font-weight: 600; border: none; border-radius: 12rpx; padding: 24rpx; margin-top: 10rpx; }
.publish-btn[disabled] { opacity: 0.5; }
</style>
