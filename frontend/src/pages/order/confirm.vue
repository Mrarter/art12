<template>
  <view class="confirm-page">
    <view class="address-card" @click="goAddress">
      <view class="address-main" v-if="selectedAddress">
        <view class="address-user">
          <text>{{ selectedAddress.receiverName }}</text>
          <text>{{ selectedAddress.phone || selectedAddress.receiverPhone }}</text>
        </view>
        <text class="address-text">收货地址：{{ fullAddress }}</text>
        <view class="copy-btn" @click.stop="copyAddress">一键复制</view>
      </view>
      <view class="address-empty" v-else>
        <text class="add-icon">+</text>
        <text>添加收货地址</text>
      </view>
      <text class="arrow-icon">›</text>
    </view>

    <view class="order-card" v-if="goodsList.length > 0">
      <view class="artist-line">
        <image class="artist-avatar" :src="goodsList[0].authorAvatar || goodsList[0].coverImage" mode="aspectFill"></image>
        <text>{{ goodsList[0].authorName || '艺术家' }}</text>
      </view>

      <view class="goods-row" v-for="item in goodsList" :key="item.id">
        <image class="goods-image" :src="item.coverImage || '/static/images/artwork-fallback.png'" mode="aspectFill"></image>
        <view class="goods-info">
          <text class="goods-title">{{ item.title }}</text>
          <text class="goods-meta">{{ itemMeta(item) }}</text>
          <view class="goods-price-row">
            <text class="goods-price">¥{{ formatPrice(item.price) }}</text>
            <text class="goods-qty">x{{ item.quantity }}</text>
          </view>
        </view>
      </view>

      <view class="message-row">
        <text>买家留言</text>
        <input class="message-input" v-model="remark" placeholder="选填，请输入" placeholder-class="placeholder" maxlength="200" />
      </view>
    </view>

    <view class="price-card">
      <view class="price-row">
        <text>商品金额</text>
        <text>¥{{ formatPrice(goodsAmount) }}</text>
      </view>
      <view class="price-row">
        <text>优惠券</text>
        <text class="muted">无可用 ›</text>
      </view>
      <view class="price-row">
        <text>装裱费</text>
        <text>¥{{ formatPrice(framingFee) }}</text>
      </view>
      <view class="price-row">
        <text>打包费</text>
        <text>¥{{ formatPrice(packingFee) }}</text>
      </view>
      <view class="price-row">
        <text>邮费</text>
        <text>{{ postageFee > 0 ? '¥' + formatPrice(postageFee) : '包邮' }}</text>
      </view>
      <view class="price-row total">
        <text>合计</text>
        <text>¥{{ formatPrice(payableAmount) }}</text>
      </view>
    </view>

    <view class="pay-card">
      <view class="section-title">支付方式</view>
      <view
        v-if="isMpWeixin"
        class="pay-option"
        :class="{ active: paymentMethod === 'wechat' }"
        @click="selectPaymentMethod('wechat')"
      >
        <view class="pay-left">
          <text class="pay-icon wechat">微</text>
          <text>微信支付</text>
        </view>
        <text class="radio">{{ paymentMethod === 'wechat' ? '●' : '○' }}</text>
      </view>
      <view
        class="pay-option"
        :class="{ active: paymentMethod === 'alipay' }"
        @click="selectPaymentMethod('alipay')"
      >
        <view class="pay-left">
          <text class="pay-icon alipay">支</text>
          <text>支付宝</text>
        </view>
        <text class="radio">{{ paymentMethod === 'alipay' ? '●' : '○' }}</text>
      </view>
    </view>

    <view class="submit-bar">
      <view class="agreement-card submit-agreement" :class="{ active: agreedNoReason }" @click="agreedNoReason = !agreedNoReason">
        <text class="check">{{ agreedNoReason ? '●' : '○' }}</text>
        <view class="agreement-copy">
          <text class="agreement-title">同意艺术品不支持 7 天无理由退货</text>
          <text class="agreement-desc">提示：鉴于艺术品具有特殊性质，一旦勾选并提交订单付款后，即视为您同意。</text>
        </view>
      </view>
      <view class="submit-row">
        <view class="submit-info">
          <text class="submit-label">合计（包邮）</text>
          <text class="submit-price">¥{{ formatPrice(payableAmount) }}</text>
        </view>
        <button class="btn-submit" @click="onSubmit" :loading="submitting" :disabled="submitting">
          {{ submitting ? '支付中...' : '立即支付' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import { getCartList, getAddressList, createOrderFromCart, directBuy, createResaleOrder, createAlipayWapPay, createAlipayAppPay } from '@/api/order'
import { getProductDetail } from '@/api/product'
import { getResaleDetail } from '@/api/resale'
import { getRealnameCertStatus } from '@/api/user'
import { fenToYuan, formatYuanNumber, getArtworkDisplayPriceFen } from '@/utils/price'
import { IS_MP_WEIXIN, getAlipayReturnScene, isAppRuntime } from '@/utils/platform'
import { hasNativeAlipayPayBridge, requestNativeAlipayPay } from '@/utils/native'

export default {
  data() {
    return {
      goodsType: 'direct',
      goodsList: [],
      selectedAddress: null,
      remark: '',
      submitting: false,
      cartIds: [],
      artworkId: null,
      resaleId: null,
      resaleRecord: null,
      loading: false,
      paymentMethod: IS_MP_WEIXIN ? 'wechat' : 'alipay',
      agreedNoReason: false,
      couponDiscount: 0,
      framingFee: 0,
      packingFee: 0,
      postageFee: 0
    }
  },

  computed: {
    goodsAmount() {
      return this.goodsList.reduce((sum, item) => sum + (item.price || 0) * (item.quantity || 1), 0)
    },
    payableAmount() {
      return Math.max(this.goodsAmount + this.framingFee + this.packingFee + this.postageFee - this.couponDiscount, 0)
    },
    fullAddress() {
      if (!this.selectedAddress) return ''
      return `${this.selectedAddress.province || ''}${this.selectedAddress.city || ''}${this.selectedAddress.district || ''}${this.selectedAddress.detailAddress || this.selectedAddress.detail || ''}`
    },
    commissionAmount() {
      return this.payableAmount * 0.05
    },
    hasPromoter() {
      return false
    },
    isMpWeixin() {
      return IS_MP_WEIXIN
    }
  },

  onLoad(options) {
    if (options.resaleId) {
      this.goodsType = 'resale'
      this.resaleId = parseInt(options.resaleId)
      this.applyResaleQuery(options)
      this.fetchResaleGoods(this.resaleId)
    } else if (options.artworkId) {
      this.goodsType = 'direct'
      this.artworkId = parseInt(options.artworkId)
      this.fetchGoodsDetail(options.artworkId, parseInt(options.quantity) || 1)
    } else if (options.cartIds) {
      this.goodsType = 'cart'
      this.cartIds = options.cartIds.split(',').map(Number)
      this.fetchCartGoods()
    }
    this.fetchAddress()
  },

  methods: {
    safeDecode(value) {
      if (!value && value !== 0) return ''
      try {
        return decodeURIComponent(String(value))
      } catch (e) {
        return String(value)
      }
    },

    applyResaleQuery(options) {
      const record = {
        id: this.resaleId,
        artworkId: options.artworkId ? Number(options.artworkId) : null,
        resalePrice: options.resalePrice ? Number(this.safeDecode(options.resalePrice)) : 0,
        artworkUid: this.safeDecode(options.artworkUid),
        sellerUid: this.safeDecode(options.sellerUid)
      }
      this.resaleRecord = record
      this.artworkId = record.artworkId
      if (record.resalePrice || record.artworkUid) {
        this.applyResaleGoods(record)
      }
    },

    applyResaleGoods(resale, detail = null) {
      const resalePrice = Number(resale?.resalePrice || 0)
      const title = detail?.title || resale?.title || resale?.artworkName || resale?.artworkUid || '转售艺术品'
      const authorName = this.safeDecode(detail?.authorName || detail?.artistName || resale?.sellerName || resale?.sellerUid || '藏家转售')
      this.goodsList = [{
        id: resale?.artworkId,
        title: this.safeDecode(title),
        coverImage: detail?.coverImage || detail?.cover || resale?.coverImage || resale?.cover,
        price: Math.round(resalePrice * 100),
        resalePrice,
        quantity: 1,
        artType: detail?.artType || detail?.category || resale?.artType || resale?.category,
        size: detail?.size || resale?.size,
        year: detail?.year || detail?.createYear || resale?.year,
        authorName,
        authorAvatar: detail?.authorAvatar,
        material: detail?.material || detail?.medium || resale?.material,
        framingFee: 0,
        packingFee: 0,
        postageFee: 0
      }]
      this.syncFeesFromGoods()
    },

    async fetchGoodsDetail(id, quantity) {
      this.loading = true
      try {
        const res = await getProductDetail(id)
        if (res && res.id) {
          this.goodsList = [{
            id: res.id,
            title: res.title,
            coverImage: res.coverImage || res.cover,
            price: getArtworkDisplayPriceFen(res),
            quantity: quantity,
            artType: res.artType || res.category,
            size: res.size,
            year: res.year || res.createYear,
            authorName: res.authorName || res.artistName,
            authorAvatar: res.authorAvatar,
            material: res.material || res.medium,
            framingFee: res.framingFee || 0,
            packingFee: res.packingFee || 0,
            postageFee: res.postageFee || res.freight || 0
          }]
          this.syncFeesFromGoods()
        } else {
          this.loadMockGoodsData(id)
        }
      } catch (e) {
        console.error('获取商品详情失败', e)
        this.loadMockGoodsData(id)
      } finally {
        this.loading = false
      }
    },

    async fetchResaleGoods(resaleId) {
      this.loading = true
      try {
        const resale = await getResaleDetail(resaleId)
        if (!resale || !resale.id) {
          throw new Error('转售记录不存在')
        }
        this.resaleRecord = resale
        this.artworkId = Number(resale.artworkId)

        let detail = null
        try {
          detail = await getProductDetail(resale.artworkId)
        } catch (e) {
          console.warn('获取转售作品详情失败，使用转售记录兜底', e)
        }

        this.applyResaleGoods(resale, detail)
      } catch (e) {
        console.error('获取转售详情失败', e)
        if (this.goodsList.length === 0) {
          uni.showToast({ title: e.message || '转售记录加载失败', icon: 'none' })
        }
      } finally {
        this.loading = false
      }
    },

    async fetchCartGoods() {
      this.loading = true
      try {
        const list = await getCartList()
        if (list && list.length > 0) {
          const selectedItems = list.filter(item => this.cartIds.includes(item.id))
          this.goodsList = selectedItems.map(item => ({
            id: item.artworkId,
            title: item.title,
            coverImage: item.coverImage || item.cover,
        price: item.price,
            quantity: item.quantity,
            artType: item.artType || item.category,
            size: item.size,
            authorName: item.authorName || item.artistName,
            authorAvatar: item.authorAvatar,
            year: item.year || item.createYear
          }))
          this.syncFeesFromGoods()
        }
        if (this.goodsList.length === 0) {
          this.loadMockGoodsData()
        }
      } catch (e) {
        console.error('获取购物车商品失败', e)
        this.loadMockGoodsData()
      } finally {
        this.loading = false
      }
    },

    loadMockGoodsData(id) {
      const mockData = [
        { id: 1, title: '山水长卷 · 张大千', coverImage: 'https://picsum.photos/200/200?random=1', price: 128000, quantity: 1, artType: '国画', size: '180x98cm' },
        { id: 2, title: '奔马图 · 徐悲鸿', coverImage: 'https://picsum.photos/200/200?random=2', price: 256000, quantity: 1, artType: '油画', size: '120x80cm' },
        { id: 3, title: '虾趣图 · 齐白石', coverImage: 'https://picsum.photos/200/200?random=3', price: 88000, quantity: 1, artType: '国画', size: '68x136cm' }
      ]
      
      if (id) {
        this.goodsList = [{
          id: parseInt(id),
          title: '当代名家书画作品',
          coverImage: 'https://picsum.photos/200/200?random=art' + id,
          price: 128000,
          quantity: 1,
          artType: '油画',
          size: '100x80cm'
        }]
      } else if (this.cartIds.length > 0) {
        this.goodsList = mockData.filter(item => this.cartIds.includes(item.id))
        if (this.goodsList.length === 0) {
          this.goodsList = [mockData[0]]
        }
      } else {
        this.goodsList = [mockData[0]]
      }
      this.syncFeesFromGoods()
    },

    async fetchAddress() {
      try {
        const list = await getAddressList()
        if (list && list.length > 0) {
          this.selectedAddress = list.find(addr => addr.isDefault) || list[0] || null
        }
        if (!this.selectedAddress) {
          this.loadMockAddress()
        }
      } catch (e) {
        console.error('获取收货地址失败', e)
        this.loadMockAddress()
      }
    },

    loadMockAddress() {
      this.selectedAddress = {
        id: 1,
        receiverName: '张三',
        phone: '138****8888',
        province: '北京市',
        city: '朝阳区',
        district: '三里屯街道',
        detailAddress: 'SOHO现代城A座1201室'
      }
    },

    syncFeesFromGoods() {
      const first = this.goodsList[0] || {}
      this.framingFee = Number(first.framingFee || 0)
      this.packingFee = Number(first.packingFee || 0)
      this.postageFee = Number(first.postageFee || 0)
    },

    itemMeta(item) {
      const parts = [
        item.artType || item.material || '艺术品',
        item.size || '标准尺寸',
        item.year || '2024'
      ]
      return parts.map(v => String(v).replace(/分类[:：]?\s*/g, '')).filter(Boolean).join(' / ')
    },

    copyAddress() {
      if (!this.fullAddress) return
      uni.setClipboardData({
        data: this.fullAddress,
        success: () => uni.showToast({ title: '地址已复制', icon: 'success' })
      })
    },

    goAddress() {
      uni.navigateTo({
        url: '/pages/user/address?select=true',
        events: {
          selectAddress: (address) => {
            this.selectedAddress = address
          }
        }
      })
    },

    selectPaymentMethod(method) {
      if (method === 'wechat' && this.isMpWeixin) {
        this.paymentMethod = 'wechat'
        return
      }
      this.paymentMethod = method === 'alipay' ? 'alipay' : this.paymentMethod
    },

    currentPageUrl() {
      const pages = getCurrentPages()
      const page = pages[pages.length - 1]
      if (!page) return '/pages/order/confirm'
      const route = page.route?.startsWith('/') ? page.route : `/${page.route || 'pages/order/confirm'}`
      const options = page.options || {}
      const query = Object.keys(options)
        .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(options[key])}`)
        .join('&')
      return query ? `${route}?${query}` : route
    },

    async ensureRealnameVerified() {
      try {
        const status = await getRealnameCertStatus()
        if (Number(status?.status) === 1) return true
        const redirect = encodeURIComponent(this.currentPageUrl())
        uni.showToast({ title: '请先完成实名认证', icon: 'none' })
        setTimeout(() => {
          uni.navigateTo({ url: `/pages/user-extra/realname?redirect=${redirect}` })
        }, 300)
        return false
      } catch (e) {
        uni.showToast({ title: e.message || '实名认证状态校验失败', icon: 'none' })
        return false
      }
    },

    async createPendingOrder() {
      const params = {
        addressId: this.selectedAddress.id,
        remark: this.remark
      }

      if (this.goodsType === 'resale') {
        if (!this.resaleRecord) {
          throw new Error('转售记录未加载')
        }
        return createResaleOrder({
          resaleId: this.resaleRecord.id,
          resalePrice: this.resaleRecord.resalePrice,
          artworkId: this.resaleRecord.artworkId,
          addressId: this.selectedAddress.id
        })
      }

      if (this.goodsType === 'direct') {
        return directBuy({
          artworkId: this.artworkId,
          quantity: this.goodsList[0].quantity,
          ...params
        })
      }

      return createOrderFromCart({
        cartIds: this.cartIds,
        ...params
      })
    },

    getOrderId(order = {}) {
      return order.id || order.orderId || order.order_id
    },

    goPayPage(order) {
      const id = this.getOrderId(order)
      uni.navigateTo({
        url: `/pages/order/pay?orderId=${id}&amount=${this.resolveOrderPayAmountYuan(order)}&paymentMethod=${this.paymentMethod}`
      })
    },

    openAlipayApp(payUrl = '') {
      if (!payUrl || typeof window === 'undefined') return false
      const schemeUrl = `alipays://platformapi/startapp?appId=20000067&url=${encodeURIComponent(payUrl)}`
      window.location.href = schemeUrl
      setTimeout(() => {
        if (document.visibilityState !== 'hidden') {
          uni.showToast({ title: '如未自动打开，请在系统浏览器中打开后重试', icon: 'none' })
        }
      }, 2000)
      return true
    },

    submitAlipayForm(payForm, payUrl = '') {
      // #ifdef H5
      if (payUrl) {
        if (this.openAlipayApp(payUrl)) return
        return
      }
      const container = document.createElement('div')
      container.style.display = 'none'
      container.innerHTML = payForm
      document.body.appendChild(container)
      const form = container.querySelector('form')
      if (!form) {
        document.body.removeChild(container)
        throw new Error('支付宝支付表单异常')
      }
      form.method = 'POST'
      form.acceptCharset = 'UTF-8'
      form.enctype = 'application/x-www-form-urlencoded'
      form.submit()
      // #endif
      // #ifndef H5
      throw new Error('当前环境请使用支付宝 App 支付')
      // #endif
    },

    async startAlipayPay(order) {
      const id = this.getOrderId(order)
      if (!id) {
        throw new Error('订单创建成功，但订单号异常')
      }

      if (isAppRuntime() && hasNativeAlipayPayBridge()) {
        const payParams = await createAlipayAppPay(id)
        const orderInfo = payParams?.order_string || payParams?.orderInfo
        if (!orderInfo) {
          throw new Error('支付宝支付参数异常')
        }
        await requestNativeAlipayPay(orderInfo)
        uni.redirectTo({ url: `/pages/order/pay?orderId=${id}&amount=${this.resolveOrderPayAmountYuan(order)}&paymentMethod=alipay&checkPay=1` })
        return
      }

      // #ifdef APP-PLUS
      if (isAppRuntime()) {
        const payParams = await createAlipayAppPay(id)
        const orderInfo = payParams?.order_string || payParams?.orderInfo
        if (!orderInfo) {
          throw new Error('支付宝支付参数异常')
        }
        await new Promise((resolve, reject) => {
          uni.requestPayment({
            provider: 'alipay',
            orderInfo,
            success: resolve,
            fail: reject
          })
        })
        uni.redirectTo({ url: `/pages/order/pay?orderId=${id}&amount=${this.resolveOrderPayAmountYuan(order)}&paymentMethod=alipay` })
        return
      }
      // #endif

      const payParams = await createAlipayWapPay(id, { returnScene: getAlipayReturnScene() })
      if (!payParams?.pay_form && !payParams?.pay_url) {
        throw new Error('支付宝支付参数异常')
      }
      this.submitAlipayForm(payParams.pay_form, payParams.pay_url)
    },

    async onSubmit() {
      if (!this.selectedAddress) {
        uni.showToast({ title: '请选择收货地址', icon: 'none' })
        return
      }
      if (!this.paymentMethod) {
        uni.showToast({ title: '请选择支付方式', icon: 'none' })
        return
      }
      if (!this.agreedNoReason) {
        uni.showToast({ title: '请先同意退货规则', icon: 'none' })
        return
      }

      if (this.submitting) return
      this.submitting = true

      let order = null
      try {
        const realnameVerified = await this.ensureRealnameVerified()
        if (!realnameVerified) return

        order = await this.createPendingOrder()
        if (this.paymentMethod === 'alipay') {
          await this.startAlipayPay(order)
          return
        }

        this.goPayPage(order)
      } catch (e) {
        console.error('创建订单或拉起支付失败', e)
        if (order && this.getOrderId(order)) {
          uni.showToast({ title: e.message || '支付拉起失败，请重试', icon: 'none' })
          setTimeout(() => this.goPayPage(order), 600)
        } else {
          uni.showToast({ title: e.message || '订单提交失败', icon: 'none' })
        }
      } finally {
        this.submitting = false
      }
    },

    formatPrice(price) {
      return formatYuanNumber(fenToYuan(price))
    },

    resolveOrderPayAmountYuan(order = {}) {
      const apiAmount = order.payAmount ?? order.pay_amount
      if (apiAmount !== null && apiAmount !== undefined && apiAmount !== '') {
        return fenToYuan(apiAmount)
      }
      return fenToYuan(this.payableAmount)
    }
  }
}
</script>

<style lang="scss" scoped>
/* 深色主题色 */
$bg-primary: #0D0D0D;
$bg-secondary: #1A1A1A;
$bg-card: #242424;
$text-primary: #FFFFFF;
$text-secondary: #B3B3B3;
$text-muted: #666666;
$accent-gold: #D4AF37;
$accent-orange: #E8A838;

.confirm-page {
  min-height: 100vh;
  padding-top: 20rpx;
  padding-bottom: calc(300rpx + env(safe-area-inset-bottom));
  background-color: $bg-primary;
  color: $text-primary;
}

.section-title {
  padding: 20rpx 30rpx 16rpx;
  
  .title-text {
    font-size: 28rpx;
    font-weight: 600;
    color: $text-secondary;
  }
}

.order-card,
.pay-card,
.agreement-card {
  background-color: $bg-card;
  margin: 16rpx 20rpx 0;
  padding: 24rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.04);
}

.artist-line {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding-bottom: 22rpx;
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.06);
  font-size: 30rpx;
  font-weight: 600;
}

.artist-avatar {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background-color: $bg-secondary;
}

.goods-row {
  display: flex;
  padding: 24rpx 0;
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.06);
}

.goods-image {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  background-color: $bg-secondary;
  margin-right: 20rpx;
}

.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.goods-title {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-row {
  display: flex;
  align-items: center;
  gap: 22rpx;
  padding-top: 22rpx;
  color: $text-secondary;
  font-size: 28rpx;
}

.message-input {
  flex: 1;
  min-width: 0;
  height: 52rpx;
  color: $text-primary;
  font-size: 28rpx;
}

.goods-meta {
  font-size: 24rpx;
  color: $text-muted;
  margin-bottom: auto;
}

.goods-price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.goods-price {
  font-size: 30rpx;
  color: $accent-gold;
  font-weight: 600;
}

.goods-qty {
  font-size: 26rpx;
  color: $text-muted;
}

/* 收货地址 */
.address-section {
  margin-bottom: 16rpx;
}

.address-card {
  display: flex;
  align-items: center;
  position: relative;
  background-color: $bg-card;
  margin: 0 20rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.04);
  overflow: hidden;

  &::before,
  &::after {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    height: 6rpx;
    background: repeating-linear-gradient(135deg, #7bc4f3 0 28rpx, transparent 28rpx 56rpx, #ff8589 56rpx 84rpx, transparent 84rpx 112rpx);
  }

  &::before { top: 0; }
  &::after { bottom: 0; }
}

.address-main {
  flex: 1;
  min-width: 0;
}

.address-user {
  display: flex;
  gap: 24rpx;
  margin-bottom: 14rpx;
  color: $text-secondary;
  font-size: 28rpx;
}

.copy-btn {
  height: 64rpx;
  margin-top: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #07111d;
  color: #7b8390;
  font-size: 26rpx;
}

.address-empty {
  display: flex;
  align-items: center;
  flex: 1;
  
  .add-icon {
    width: 48rpx;
    height: 48rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: $accent-gold;
    color: $bg-primary;
    font-size: 32rpx;
    font-weight: 600;
    border-radius: 50%;
    margin-right: 16rpx;
  }
  
  .add-text {
    font-size: 28rpx;
    color: $text-secondary;
  }
}

.address-info {
  flex: 1;
  
  .address-header {
    display: flex;
    align-items: center;
    margin-bottom: 12rpx;
    
    .receiver {
      font-size: 30rpx;
      font-weight: 600;
      color: $text-primary;
      margin-right: 20rpx;
    }
    
    .phone {
      font-size: 28rpx;
      color: $text-secondary;
    }
  }
  
  .address-text {
    font-size: 26rpx;
    color: $text-muted;
    line-height: 1.5;
  }
}

.arrow-icon {
  font-size: 40rpx;
  color: $text-muted;
  margin-left: 16rpx;
}

/* 分成说明 */
.commission-section {
  margin-bottom: 16rpx;
}

.commission-card {
  display: flex;
  align-items: center;
  background-color: $bg-card;
  margin: 0 20rpx;
  padding: 20rpx 24rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(212, 175, 55, 0.2);
  
  .commission-icon {
    font-size: 40rpx;
    margin-right: 16rpx;
  }
  
  .commission-content {
    flex: 1;
    
    .commission-title {
      display: block;
      font-size: 28rpx;
      color: $text-primary;
      font-weight: 500;
      margin-bottom: 6rpx;
    }
    
    .commission-desc {
      font-size: 24rpx;
      color: $text-muted;
    }
  }
}

/* 价格明细 */
.price-section {
  margin-bottom: 16rpx;
}

.price-card {
  background-color: $bg-card;
  margin: 16rpx 20rpx 0;
  padding: 24rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.04);
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  font-size: 28rpx;
  color: $text-secondary;

  > text:last-child {
    color: $text-primary;
  }

  .muted {
    color: $text-muted;
  }
  
  .price-label {
    font-size: 28rpx;
    color: $text-secondary;
  }
  
  .price-value {
    font-size: 28rpx;
    color: $text-primary;
    
    &.free {
      color: $accent-gold;
    }
  }
  
  &.total {
    border-top: 1rpx solid rgba(255, 255, 255, 0.06);
    margin-top: 8rpx;
    padding-top: 24rpx;
    
    > text:first-child {
      color: $text-secondary;
    }

    > text:last-child {
      font-size: 36rpx;
      font-weight: 600;
      color: $accent-gold;
    }
  }
}

.pay-card {
  padding: 8rpx 24rpx 14rpx;
}

.pay-card .section-title {
  padding: 16rpx 0 10rpx;
  color: $text-secondary;
  font-size: 28rpx;
  font-weight: 600;
}

.pay-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
  color: $text-secondary;
  font-size: 28rpx;
}

.pay-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.pay-icon {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24rpx;
  font-weight: 700;

  &.wechat { background: #18b55f; }
  &.alipay { background: #1677ff; }
}

.pay-option.active,
.radio {
  color: $accent-gold;
}

.agreement-card {
  display: flex;
  gap: 12rpx;
  background: rgba(33, 120, 56, 0.32);
  border-color: rgba(79, 197, 107, 0.28);
}

.check {
  color: $text-secondary;
  font-size: 24rpx;
  line-height: 32rpx;
}

.agreement-card.active .check {
  color: #7bd28b;
}

.agreement-title,
.agreement-desc {
  display: block;
}

.agreement-title {
  color: #88d58e;
  font-size: 24rpx;
  font-weight: 700;
}

.agreement-desc {
  margin-top: 4rpx;
  color: rgba(136, 213, 142, 0.76);
  font-size: 20rpx;
  line-height: 1.35;
}

/* 备注 */
.remark-section {
  margin-bottom: 16rpx;
}

.remark-card {
  background-color: $bg-card;
  margin: 0 20rpx;
  padding: 20rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.04);
  position: relative;
}

.remark-input {
  width: 100%;
  min-height: 160rpx;
  font-size: 28rpx;
  color: $text-primary;
  background: transparent;
  line-height: 1.6;
}

.placeholder {
  color: $text-muted;
}

.remark-count {
  position: absolute;
  bottom: 16rpx;
  right: 20rpx;
  font-size: 22rpx;
  color: $text-muted;
}

/* 底部提交栏 */
.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  padding: 16rpx 20rpx 20rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background-color: $bg-secondary;
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.submit-agreement {
  width: 100%;
  box-sizing: border-box;
  margin: 0;
  padding: 14rpx 18rpx;
  border-radius: 14rpx;
  align-items: flex-start;
}

.agreement-copy {
  flex: 1;
  min-width: 0;
}

.submit-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.submit-info {
  display: flex;
  flex-direction: column;
  
  .submit-label {
    font-size: 24rpx;
    color: $text-muted;
    margin-bottom: 4rpx;
  }
  
  .submit-price {
    font-size: 44rpx;
    font-weight: 600;
    color: $accent-gold;
  }
}

.btn-submit {
  width: 260rpx;
  height: 88rpx;
  margin-left: auto;
  margin-right: 0;
  background: linear-gradient(135deg, $accent-gold 0%, $accent-orange 100%);
  color: $bg-primary;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  
  &::after {
    border: none;
  }
  
  &:active {
    opacity: 0.9;
  }
  
  &[disabled] {
    opacity: 0.6;
  }
}
</style>
