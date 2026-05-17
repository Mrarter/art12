<template>
  <view class="page">
    <view class="nav-bar">
      <view class="nav-icon" @click="goBack">‹</view>
      <view class="nav-title">发布转售</view>
      <view class="nav-icon">&nbsp;</view>
    </view>

    <view class="form">
      <view class="card">
        <view class="field">
          <text class="field-label">作品ID</text>
          <input class="field-input" v-model="artworkId" type="text" placeholder="请输入作品ID" />
        </view>
        <view class="field">
          <text class="field-label">转售价格</text>
          <view class="price-input">
            <text class="currency">¥</text>
            <input class="field-input" v-model="resalePrice" type="digit" placeholder="请输入转售价格" />
          </view>
        </view>
      </view>

      <view class="card fee-preview" v-if="resalePrice && Number(resalePrice) > 0">
        <view class="section-title">费用预览</view>
        <view class="preview-row">
          <text>转售价格</text>
          <text>¥{{ formatPrice(computedPrice) }}</text>
        </view>
        <view class="preview-row">
          <text>艺术家收益 (5%)</text>
          <text style="color:#67C23A;">- ¥{{ formatPrice(artistFee) }}</text>
        </view>
        <view class="preview-row">
          <text>平台服务费 (10%)</text>
          <text style="color:#D4AF37;">- ¥{{ formatPrice(platformFee) }}</text>
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
        <text class="tip-text">• 平台将收取转售价格10%的服务费</text>
        <text class="tip-text">• 您将获得转售价格扣除收益和费用后的85%</text>
      </view>

      <button class="publish-btn" :disabled="!canPublish" @click="handlePublish">发布转售</button>
    </view>
  </view>
</template>

<script>
import { publishResale } from '@/api/resale'

export default {
  data() {
    return {
      artworkId: '',
      resalePrice: '',
      submitting: false
    }
  },
  computed: {
    computedPrice() { return Number(this.resalePrice) || 0 },
    artistFee() { return (this.computedPrice * 0.05).toFixed(2) },
    platformFee() { return (this.computedPrice * 0.10).toFixed(2) },
    sellerIncome() { return (this.computedPrice * 0.85).toFixed(2) },
    canPublish() { return this.artworkId && this.resalePrice > 0 && !this.submitting }
  },
  methods: {
    formatPrice(p) { return Number(p).toFixed(2) },
    goBack() { uni.navigateBack() },
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
            uni.navigateTo({ url: '/pages/resale/my' })
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
.nav-bar { display: flex; align-items: center; justify-content: space-between; padding: 20rpx 30rpx; background: #1A1A1A; }
.nav-icon { font-size: 36rpx; color: #D4AF37; padding: 10rpx; }
.nav-title { font-size: 32rpx; color: #E8E0D0; font-weight: 500; }
.form { padding: 20rpx; }
.card { background: #1A1A1A; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; border: 1rpx solid #333; }
.field { margin-bottom: 24rpx; }
.field-label { font-size: 26rpx; color: #999; display: block; margin-bottom: 10rpx; }
.field-input { width: 100%; background: #0D0D0D; border: 1rpx solid #333; border-radius: 12rpx; padding: 20rpx; font-size: 28rpx; color: #E8E0D0; }
.price-input { display: flex; align-items: center; }
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
