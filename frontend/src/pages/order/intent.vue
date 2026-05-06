<template>
  <view class="page">
    <view class="intro">
      <view class="title">确认收藏意向</view>
      <view class="sub">提交后，艺术顾问将确认作品状态、交付方式与收藏证书信息。</view>
    </view>

    <!-- 收藏作品信息 -->
    <view class="card">
      <view class="between">
        <view class="card-title">收藏作品</view>
        <view class="sub" @click="handleViewWork">查看详情 ›</view>
      </view>
      <view class="row">
        <image class="work-img" :src="work.coverUrl" mode="aspectFill" />
        <view class="work-info">
          <view class="work-title">《{{ work.title }}》</view>
          <view class="sub">{{ work.artistName }}｜{{ work.material }}</view>
          <view class="sub">{{ work.size }}｜{{ work.year }}</view>
          <view class="price">{{ work.priceText }}</view>
        </view>
      </view>
    </view>

    <!-- 收藏方式 -->
    <view class="card">
      <view class="card-title">收藏方式</view>
      <view class="option" :class="{ active: collectType === 'WORK_AND_CERT' }" @click="selectCollectType('WORK_AND_CERT')">
        <view><b>收藏作品 + 收藏证书</b><p>获得作品实物，并生成平台收藏证书。</p></view>
        <text class="check">✓</text>
      </view>
      <view class="option" :class="{ active: collectType === 'CERT_ONLY' }" @click="selectCollectType('CERT_ONLY')">
        <view><b>仅寄送收藏证书</b><p>作品由平台或艺术家保管，藏家获得收藏证书。</p></view>
        <text class="check">✓</text>
      </view>
    </view>

    <!-- 交付方式 -->
    <view class="card">
      <view class="card-title">作品交付方式</view>
      <view class="option" :class="{ active: deliveryType === 'SHIP_WORK' }" @click="selectDeliveryType('SHIP_WORK')">
        <view><b>寄送作品</b><p>确认包装、保险和物流后发货。</p></view>
        <text class="check">✓</text>
      </view>
      <view class="option" :class="{ active: deliveryType === 'PLATFORM_CUSTODY' }" @click="selectDeliveryType('PLATFORM_CUSTODY')">
        <view><b>平台保管</b><p>作品保存在平台仓库，可后续寄送或再次流通。</p></view>
        <text class="check">✓</text>
      </view>
    </view>

    <!-- 藏家信息 -->
    <view class="card">
      <view class="card-title">藏家信息</view>
      <input class="input" placeholder="联系人" v-model="form.contactName" data-field="contactName" @input="handleInput" />
      <input class="input" placeholder="手机号" type="number" maxlength="11" v-model="form.phone" data-field="phone" @input="handleInput" />
      <textarea class="textarea" placeholder="备注" v-model="form.remark" data-field="remark" @input="handleInput" />
    </view>

    <!-- 协议确认 -->
    <view class="agreement" @click="toggleAgreement">
      <text :class="['check', { checked: agreed }]">✓</text>
      <view>我已了解收藏流程、保管规则与收藏证书说明</view>
    </view>

    <!-- 底部栏 -->
    <view class="bottom-bar">
      <view class="bottom-price">
        <text>预计 </text>
        <text class="price-lg">{{ estimatedTotalText }}</text>
      </view>
      <button class="btn primary" :disabled="!agreed" @click="handleSubmit">提交意向</button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      workId: null,
      loading: true,
      work: {
        id: 0, title: '', artistName: '',
        coverUrl: '',
        year: '', material: '', size: '', price: 0, priceText: ''
      },
      collectType: 'WORK_AND_CERT',
      deliveryType: 'SHIP_WORK',
      custodyRate: 0.01,
      custodyFee: 0,
      estimatedTotal: 0,
      estimatedTotalText: '',
      form: { contactName: '', phone: '', remark: '' },
      agreed: false
    }
  },

  async onLoad(options) {
    this.workId = options.workId
    if (this.workId) {
      try {
        const { getProductDetail } = await import('@/api/product')
        const res = await getProductDetail(this.workId)
        const data = res?.data || res
        if (data) {
          const price = Number(data.price || data.currentPrice || 0)
          this.work = {
            id: data.id || this.workId,
            title: data.title || '',
            artistName: data.authorName || data.artistName || '',
            coverUrl: data.cover || data.coverImage || data.coverUrl || '',
            year: data.year || data.createYear || '',
            material: data.material || '',
            size: data.size || '',
            price: price,
            priceText: price ? '¥' + String(Math.round(price / 100)).replace(/\B(?=(\d{3})+(?!\d))/g, ',') : ''
          }
        }
      } catch (e) {
        console.error('加载作品信息失败', e)
      }
    }
    this.loading = false
    this.calculateFees()
  },

  methods: {
    selectCollectType(type) {
      this.collectType = type
      if (type === 'CERT_ONLY') {
        this.deliveryType = 'PLATFORM_CUSTODY'
      }
      this.calculateFees()
    },

    selectDeliveryType(type) {
      this.deliveryType = type
      this.calculateFees()
    },

    calculateFees() {
      this.custodyFee = this.deliveryType === 'PLATFORM_CUSTODY' ? Math.round(this.work.price * this.custodyRate) : 0
      this.estimatedTotal = this.work.price + this.custodyFee
      this.estimatedTotalText = '¥' + this.formatMoney(this.estimatedTotal)
    },

    formatMoney(v) {
      return String(v).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    },

    handleInput(e) {
      this.form[e.currentTarget.dataset.field] = e.detail.value
    },

    toggleAgreement() {
      this.agreed = !this.agreed
    },

    async handleSubmit() {
      if (!this.agreed) {
        uni.showToast({ title: '请确认收藏流程说明', icon: 'none' })
        return
      }
      uni.showLoading({ title: '提交中...' })
      try {
        const { createPurchaseIntent } = await import('@/api/order')
        const res = await createPurchaseIntent({
          workId: this.workId,
          collectType: this.collectType,
          deliveryType: this.deliveryType,
          contactName: this.form.contactName,
          phone: this.form.phone,
          remark: this.form.remark,
          estimatedAmount: this.estimatedTotal
        })
        const intentId = res?.intentId || res?.data?.intentId || `INTENT_${Date.now()}`
        uni.hideLoading()
        uni.redirectTo({ url: `/pages/order/intent-result?intentId=${intentId}` })
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: '提交失败，请重试', icon: 'none' })
      }
    },

    handleViewWork() {
      uni.navigateTo({ url: `/pages/gallery/detail?id=${this.workId}` })
    }
  }
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f7f3eb; padding-bottom: 160rpx; }
.intro { padding: 32rpx 32rpx 0;
  .title { font-size: 38rpx; font-weight: 700; color: #1d1d1f; }
  .sub { margin-top: 12rpx; font-size: 26rpx; color: #8a8178; line-height: 1.5; }
}
.card { margin: 20rpx; padding: 28rpx; background: #fff; border-radius: 24rpx; }
.between { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
.card-title { font-size: 30rpx; font-weight: 700; color: #1d1d1f; margin-bottom: 20rpx; }
.sub { font-size: 24rpx; color: #8a8178; }
.row { display: flex; gap: 20rpx; }
.work-img { width: 160rpx; height: 160rpx; border-radius: 16rpx; background: #e8e2d8; flex-shrink: 0; }
.work-info { flex: 1; }
.work-title { font-size: 30rpx; font-weight: 600; color: #1d1d1f; margin-bottom: 8rpx; }
.price { margin-top: 12rpx; font-size: 32rpx; font-weight: 700; color: #b3261e; }
.option { display: flex; justify-content: space-between; align-items: flex-start; padding: 20rpx; border: 2rpx solid #e2d8cc; border-radius: 16rpx; margin-bottom: 16rpx;
  &.active { border-color: #b3261e; background: rgba(179,38,30,.04); }
  b { display: block; font-size: 28rpx; margin-bottom: 6rpx; }
  p { margin: 0; font-size: 24rpx; color: #8a8178; line-height: 1.4; }
  .check { font-size: 28rpx; color: #ccc; margin-left: 16rpx; flex-shrink: 0; &.checked { color: #b3261e; } }
}
.input, .textarea { width: 100%; border: 2rpx solid #e2d8cc; border-radius: 16rpx; padding: 20rpx; margin-bottom: 16rpx; box-sizing: border-box; font-size: 26rpx; }
.textarea { min-height: 140rpx; }
.agreement { display: flex; align-items: center; gap: 16rpx; margin: 20rpx; padding: 24rpx; background: #fff; border-radius: 16rpx;
  .check { width: 40rpx; height: 40rpx; border: 2rpx solid #ccc; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 22rpx; color: transparent; flex-shrink: 0;
    &.checked { border-color: #b3261e; background: #b3261e; color: #fff; }
  }
  view { font-size: 24rpx; color: #8a8178; }
}
.bottom-bar { position: fixed; left: 0; right: 0; bottom: 0; display: flex; align-items: center; padding: 20rpx 28rpx; padding-bottom: calc(20rpx + env(safe-area-inset-bottom)); background: #fff; box-shadow: 0 -8rpx 24rpx rgba(0,0,0,.06);
  .bottom-price { flex: 1; font-size: 24rpx; color: #8a8178; }
  .price-lg { font-size: 36rpx; font-weight: 700; color: #b3261e; }
  .btn { height: 80rpx; border-radius: 999rpx; border: none; font-size: 28rpx; padding: 0 48rpx; &.primary { background: #b3261e; color: #fff; } &[disabled] { opacity: .4; } }
}
</style>
