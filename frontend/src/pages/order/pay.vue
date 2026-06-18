<template>
  <view class="pay-page">
    <view class="pay-hero">
      <text class="hero-label">{{ isPaidOrder ? '已付款金额' : '待支付金额' }}</text>
      <text class="hero-amount">¥{{ formatMoney(payAmount) }}</text>
      <view class="hero-meta">
        <text>{{ orderStatusText }}</text>
        <text v-if="countdownText">剩余 {{ countdownText }}</text>
      </view>
    </view>

    <view class="card order-card">
      <view class="card-head">
        <text class="card-title">订单信息</text>
        <text class="order-no">订单号：{{ orderInfo.orderNo || orderId || '-' }}</text>
      </view>
      <view class="goods-row" v-for="item in goodsList" :key="item.id || item.goodsId || item.artworkId">
        <image class="goods-image" :src="getGoodsImage(item)" mode="aspectFill"></image>
        <view class="goods-main">
          <text class="goods-title">{{ item.title || item.goodsName || '艺术品' }}</text>
          <text class="goods-author" v-if="getGoodsAuthor(item)">作者：{{ getGoodsAuthor(item) }}</text>
          <text class="goods-meta">{{ itemMeta(item) }}</text>
          <view class="goods-price-row">
            <text class="goods-price">¥{{ formatMoney(getGoodsLineAmountYuan(item)) }}</text>
            <text class="goods-qty">x{{ item.quantity || item.count || 1 }}</text>
          </view>
        </view>
      </view>
      <view class="empty-goods" v-if="goodsList.length === 0">
        <text>订单作品信息加载中</text>
      </view>
    </view>

    <view class="card address-card" v-if="hasAddress">
      <view class="card-head compact">
        <text class="card-title">收货信息</text>
      </view>
      <view class="address-user">
        <text>{{ address.receiverName || address.name || '-' }}</text>
        <text>{{ address.receiverPhone || address.phone || '-' }}</text>
      </view>
      <text class="address-text">{{ fullAddress }}</text>
    </view>

    <view class="card fee-card">
      <view class="card-head compact">
        <text class="card-title">费用明细</text>
      </view>
      <view class="fee-row">
        <text>商品金额</text>
        <text>¥{{ formatMoney(goodsAmount) }}</text>
      </view>
      <view class="fee-row">
        <text>运费</text>
        <text>{{ freightAmount > 0 ? '¥' + formatMoney(freightAmount) : '包邮' }}</text>
      </view>
      <view class="fee-row" v-if="discountAmount > 0">
        <text>优惠抵扣</text>
        <text class="discount">-¥{{ formatMoney(discountAmount) }}</text>
      </view>
      <view class="fee-row total">
        <text>应付金额</text>
        <text>¥{{ formatMoney(payAmount) }}</text>
      </view>
    </view>

    <view class="card pay-methods" v-if="canPayOrder">
      <view class="card-head compact">
        <text class="card-title">选择支付方式</text>
      </view>
      <view
        class="method-item"
        v-for="item in payMethods"
        :key="item.id"
        :class="{ active: selectedPay === item.id }"
        @click="selectPay(item)"
      >
        <view class="method-icon" :class="item.id">{{ item.mark }}</view>
        <view class="method-info">
          <text class="method-name">{{ item.name }}</text>
          <text class="method-desc">{{ item.desc }}</text>
        </view>
        <text class="method-check">{{ selectedPay === item.id ? '●' : '○' }}</text>
      </view>
    </view>

    <view class="security-tips" v-if="canPayOrder">
      <text class="lock">▣</text>
      <text>{{ selectedPayName }}安全支付保障，平台托管交易资金</text>
    </view>

    <view class="pay-footer">
      <view class="footer-left">
        <text class="amount-label">应付</text>
        <text class="amount-value">¥{{ formatMoney(payAmount) }}</text>
      </view>
      <view class="footer-btns">
        <button v-if="canPayOrder" class="pay-btn" @click="doPay" :loading="paying">
          {{ paying ? '支付中...' : '立即支付' }}
        </button>
        <button v-else class="pay-btn paid-btn" @click="goOrderDetail">
          查看订单
        </button>
        <button class="pay-btn mock-btn" v-if="payFailed && canPayOrder" @click="devMockPay">
          模拟支付
        </button>
      </view>
    </view>

    <view class="pay-success-modal" v-if="showSuccess">
      <view class="success-content">
        <view class="success-icon">✓</view>
        <text class="success-title">支付成功</text>
        <text class="success-desc">您的订单已支付成功</text>
        <view class="success-actions">
          <button class="action-btn primary" @click="goOrderDetail">查看订单</button>
          <button class="action-btn secondary" @click="goHome">返回首页</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getOrderDetail, getJsApiPayParams, createAlipayWapPay, mockPaySuccess } from '@/api/order'
import { getProductDetail, normalizeImageUrl } from '@/api/product'
import { useUserStore } from '@/store/modules/user'
import { getFullImageUrl } from '@/utils/image'
import { fenToYuan, formatYuanNumber } from '@/utils/price'
import { getAccessToken, isGuestUser } from '@/utils/auth'

const userStore = useUserStore()

const orderInfo = ref({
  orderNo: '',
  goodsAmount: 0,
  freight: 0,
  discountAmount: 0,
  payAmount: 0,
  address: null,
  goodsList: [],
  statusText: '',
  createTime: ''
})

const payMethods = ref([
  { id: 'wechat', name: '微信支付', desc: '推荐使用微信支付', mark: '微' },
  { id: 'alipay', name: '支付宝', desc: '支持支付宝余额与银行卡', mark: '支' }
])

const selectedPay = ref('wechat')
const paying = ref(false)
const showSuccess = ref(false)
const orderId = ref(null)
const payFailed = ref(false)
const fallbackAmount = ref(0)

const toNumber = (value) => {
  if (value === null || value === undefined || value === '') return 0
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

const routeAmountToYuan = (value, fallbackYuan = 0) => {
  if (value === null || value === undefined || value === '') return fallbackYuan
  const raw = String(value).trim()
  if (!raw) return fallbackYuan
  return raw.includes('.') ? toNumber(raw) : fenToYuan(raw)
}

const orderAmountToYuan = (value, fallbackYuan = 0) => {
  if (value === null || value === undefined || value === '') return fallbackYuan
  return fenToYuan(value)
}

const reconcileYuanAmount = (rawValue, options = {}) => {
  const amountYuan = orderAmountToYuan(rawValue, 0)
  const references = [options.fallbackYuan, options.derivedYuan]
    .map((value) => toNumber(value))
    .filter((value) => value > 0)
  for (const reference of references) {
    if (Math.abs(amountYuan - reference * 100) < 0.01 || Math.abs(amountYuan - reference / 100) < 0.01) {
      return reference
    }
  }
  return amountYuan
}

const formatMoney = (value) => {
  return formatYuanNumber(value)
}

const isLocalDev = () => {
  const host = typeof location !== 'undefined' ? location.hostname : ''
  return process.env.NODE_ENV !== 'production' || host === 'localhost' || host === '127.0.0.1'
}

const hasRealLogin = () => {
  const token = userStore.token || getAccessToken()
  return !!token && !userStore.userInfo?.isGuest && !isGuestUser()
}

const goodsList = computed(() => orderInfo.value.goodsList || orderInfo.value.items || [])
const derivedGoodsAmount = computed(() => goodsList.value.reduce((sum, item) => sum + getGoodsLineAmountYuan(item), 0))
const goodsAmount = computed(() => reconcileYuanAmount(
  orderInfo.value.goodsAmount || orderInfo.value.totalAmount || orderInfo.value.payAmount,
  { fallbackYuan: fallbackAmount.value, derivedYuan: derivedGoodsAmount.value }
))
const freightAmount = computed(() => orderAmountToYuan(orderInfo.value.freight || orderInfo.value.freightAmount, 0))
const discountAmount = computed(() => orderAmountToYuan(orderInfo.value.discountAmount || orderInfo.value.couponAmount, 0))
const payAmount = computed(() => {
  const derivedPayAmount = Math.max(goodsAmount.value + freightAmount.value - discountAmount.value, 0)
  return reconcileYuanAmount(orderInfo.value.payAmount, {
    fallbackYuan: fallbackAmount.value || derivedPayAmount,
    derivedYuan: derivedPayAmount
  })
})
const address = computed(() => orderInfo.value.address || {})
const hasAddress = computed(() => !!(address.value.receiverName || address.value.name || fullAddress.value))
const fullAddress = computed(() => {
  const addr = address.value
  return addr.fullAddress || [addr.province, addr.city, addr.district, addr.detail || addr.detailAddress].filter(Boolean).join('')
})
const selectedPayName = computed(() => payMethods.value.find(item => item.id === selectedPay.value)?.name || '支付')
const rawOrderStatus = computed(() => orderInfo.value.status || orderInfo.value.orderStatus || orderInfo.value.paymentStatus || orderInfo.value.statusText || '')
const isPaidStatus = (value) => {
  const text = String(value || '').trim()
  const token = text.toUpperCase()
  return ['PAID', 'SHIPPED', 'RECEIVED', 'COMPLETED', 'FINISHED', 'SUCCESS'].includes(token)
    || /已付款|已支付|支付成功|支付完成|已发货|已完成/.test(text)
}
const isPaidOrder = computed(() => isPaidStatus(rawOrderStatus.value) || isPaidStatus(orderInfo.value.statusText))
const canPayOrder = computed(() => !isPaidOrder.value && Number(payAmount.value) > 0)
const orderStatusText = computed(() => orderInfo.value.statusText || (isPaidOrder.value ? '支付完成' : '等待支付'))
const countdownText = computed(() => {
  if (!orderInfo.value.createTime) return ''
  const created = new Date(orderInfo.value.createTime.replace(/-/g, '/')).getTime()
  if (!created) return ''
  const expire = created + 30 * 60 * 1000
  const left = Math.max(0, expire - Date.now())
  if (left <= 0) return ''
  const minutes = Math.floor(left / 60000)
  const seconds = Math.floor((left % 60000) / 1000)
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const itemMeta = (item) => {
  const parts = [
    item.artType || item.material || item.categoryName,
    item.size,
    item.year,
    item.specName
  ].filter((value, index, list) => value && list.indexOf(value) === index)
  return parts.length ? parts.join(' / ') : '艺术品'
}

const getGoodsAuthor = (item = {}) => {
  return item.authorName ||
    item.artistName ||
    item.creatorName ||
    item.authorRealName ||
    item.artistRealName ||
    ''
}

const getGoodsLineAmountYuan = (item = {}) => {
  const amount = item.subtotal ?? item.price ?? 0
  return orderAmountToYuan(amount, 0)
}

const firstImage = (images) => {
  if (Array.isArray(images)) return images.find(Boolean) || ''
  if (typeof images === 'string') return images.split(',').map(item => item.trim()).find(Boolean) || ''
  return ''
}

const resolveGoodsImage = (item = {}) => {
  const raw = item.coverImage ||
    item.goodsImage ||
    item.cover ||
    item.coverUrl ||
    item.image ||
    item.imageUrl ||
    item.artworkCover ||
    item.artworkCoverUrl ||
    firstImage(item.images) ||
    ''
  return normalizeImageUrl(raw)
}

const getGoodsImage = (item = {}) => {
  return getFullImageUrl(resolveGoodsImage(item), '/static/images/artwork-fallback.png')
}

const normalizeOrderItems = async (items = []) => {
  const normalized = items.map(item => ({ ...item }))
  await Promise.all(normalized.map(async (item) => {
    const artworkId = item.artworkId || item.goodsId
    if (!artworkId) return
    try {
      const detail = await getProductDetail(artworkId)
      item.coverImage = item.coverImage || detail.coverImage || detail.cover || firstImage(detail.images) || ''
      item.images = item.images || detail.images || ''
      item.title = item.title || item.goodsName || detail.title || detail.name || ''
      item.authorName = getGoodsAuthor(item) || getGoodsAuthor(detail)
      item.artistName = item.artistName || detail.artistName || detail.authorName || ''
      item.artType = item.artType || detail.artType || detail.medium || ''
      item.size = item.size || detail.size || ''
      item.year = item.year || detail.year || detail.creationYear || ''
      const detailCurrentPriceYuan = orderAmountToYuan(detail.currentPrice || detail.current_price || detail.price || 0, 0)
      const itemAmountYuan = orderAmountToYuan(item.subtotal ?? item.price ?? 0, 0)
      if (detailCurrentPriceYuan > 0 && itemAmountYuan > detailCurrentPriceYuan * 10) {
        item.price = detail.currentPrice || detail.current_price || detail.price || item.price
        item.subtotal = (detail.currentPrice || detail.current_price || detail.price || item.subtotal) * (item.quantity || item.count || 1)
      }
    } catch (e) {
      console.warn('补充订单作品信息失败:', artworkId, e)
    }
  }))
  return normalized
}

const readRouteOptionsFromLocation = () => {
  if (typeof window === 'undefined') return {}
  const query = window.location.href.split('?')[1]?.split('#')[0] || window.location.hash.split('?')[1] || ''
  return Object.fromEntries(new URLSearchParams(query))
}

const selectPay = (item) => {
  selectedPay.value = item.id
}

const devMockPay = async () => {
  if (!orderId.value || paying.value) return
  paying.value = true
  try {
    await mockPaySuccess(orderId.value)
    const detail = await getOrderDetail(orderId.value).catch(() => null)
    if (detail) {
      const items = await normalizeOrderItems(detail.goodsList || detail.items || [])
      orderInfo.value = {
        ...orderInfo.value,
        ...detail,
        statusText: detail.statusText || '支付完成',
        goodsList: items
      }
    } else {
      orderInfo.value.statusText = '支付完成'
    }
    payFailed.value = false
    showSuccess.value = true
  } catch (e) {
    uni.showToast({ title: e.message || '模拟支付失败', icon: 'none' })
  } finally {
    paying.value = false
  }
}

const submitAlipayForm = (payForm) => {
  // #ifdef H5
  const container = document.createElement('div')
  container.style.display = 'none'
  container.innerHTML = payForm
  document.body.appendChild(container)
  const form = container.querySelector('form')
  if (!form) {
    document.body.removeChild(container)
    throw new Error('支付宝支付表单异常')
  }
  form.submit()
  // #endif
  // #ifndef H5
  uni.showToast({ title: '请在浏览器中使用支付宝支付', icon: 'none' })
  // #endif
}

const doAlipay = async () => {
  paying.value = true
  try {
    const payParams = await createAlipayWapPay(orderId.value)
    if (!payParams?.pay_form) {
      throw new Error('支付宝支付参数异常')
    }
    payFailed.value = false
    submitAlipayForm(payParams.pay_form)
  } catch (e) {
    paying.value = false
    const isDev = process.env.NODE_ENV !== 'production' || location.hostname === 'localhost'
    if (isDev) {
      payFailed.value = true
    }
    uni.showToast({ title: e.message || '支付宝下单失败', icon: 'none' })
  }
}

const doPay = async () => {
  if (paying.value) return
  if (!orderId.value) {
    uni.showToast({ title: '订单信息异常', icon: 'none' })
    return
  }
  if (isPaidOrder.value) {
    uni.showToast({ title: '订单已支付', icon: 'none' })
    return
  }
  if (!canPayOrder.value) {
    uni.showToast({ title: '该订单无需支付', icon: 'none' })
    return
  }
  if (!hasRealLogin()) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  if (selectedPay.value === 'alipay') {
    await doAlipay()
    return
  }

  paying.value = true
  try {
    const openId = userStore.openId || uni.getStorageSync('openId') || ''
    if (!openId) {
      if (isLocalDev()) {
        payFailed.value = true
        uni.showToast({ title: '本地调试可使用模拟支付', icon: 'none' })
      } else {
        uni.showToast({ title: '请在微信内完成支付', icon: 'none' })
      }
      paying.value = false
      return
    }

    let payParams
    try {
      payParams = await getJsApiPayParams(orderId.value, openId)
    } catch (e) {
      if (isLocalDev()) {
        payFailed.value = true
        paying.value = false
        return
      }
      throw e
    }

    uni.requestPayment({
      provider: 'wxpay',
      timeStamp: payParams.timeStamp,
      nonceStr: payParams.nonceStr,
      package: payParams.package,
      signType: payParams.signType || 'MD5',
      paySign: payParams.paySign,
      success: () => {
        paying.value = false
        showSuccess.value = true
      },
      fail: (err) => {
        paying.value = false
        if (err.errMsg?.includes('cancel')) {
          uni.showToast({ title: '支付已取消', icon: 'none' })
        } else {
          uni.showToast({ title: '支付失败，请重试', icon: 'none' })
        }
      }
    })
  } catch (e) {
    paying.value = false
    if (!isLocalDev()) {
      uni.showToast({ title: e.message || '支付下单失败', icon: 'none' })
    }
  }
}

const goOrderDetail = () => {
  uni.redirectTo({ url: `/pages/order/detail?id=${orderId.value}` })
}

const goHome = () => {
  uni.switchTab({ url: '/pages/index/index' })
}

onMounted(async () => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = { ...readRouteOptionsFromLocation(), ...(currentPage.options || {}) }

  if (options.orderId) orderId.value = options.orderId
  if (options.amount) fallbackAmount.value = routeAmountToYuan(options.amount)
  if (options.paymentMethod) selectedPay.value = options.paymentMethod

  if (!orderId.value) return
  try {
    const detail = await getOrderDetail(orderId.value)
    if (detail) {
      const items = await normalizeOrderItems(detail.goodsList || detail.items || [])
      orderInfo.value = {
        ...detail,
        orderNo: detail.orderNo || detail.order_no || orderId.value,
        goodsAmount: detail.goodsAmount ?? detail.goods_amount ?? detail.totalAmount ?? detail.payAmount ?? fallbackAmount.value,
        freight: detail.freight ?? detail.freightAmount ?? detail.freight_amount ?? 0,
        discountAmount: detail.discountAmount ?? detail.discount_amount ?? 0,
        payAmount: detail.payAmount ?? detail.pay_amount ?? fallbackAmount.value,
        status: detail.status ?? detail.orderStatus ?? detail.paymentStatus ?? '',
        statusText: detail.statusText || detail.status_text || '',
        goodsList: items,
        address: detail.address || null
      }
    }
  } catch (e) {
    console.warn('获取订单详情失败:', e)
    orderInfo.value.orderNo = orderId.value
    orderInfo.value.goodsAmount = fallbackAmount.value
    orderInfo.value.payAmount = fallbackAmount.value
  }
})
</script>

<style lang="scss" scoped>
.pay-page {
  min-height: 100vh;
  padding-bottom: calc(150rpx + env(safe-area-inset-bottom));
  background: #0d0d0d;
  color: #f7f2e8;
}

.pay-hero {
  padding: 42rpx 30rpx 34rpx;
  background: linear-gradient(135deg, #171717 0%, #30220b 100%);
}

.hero-label,
.hero-meta {
  color: rgba(247, 242, 232, 0.62);
  font-size: 24rpx;
}

.hero-amount {
  display: block;
  margin-top: 12rpx;
  color: #d8b84f;
  font-size: 58rpx;
  font-weight: 800;
}

.hero-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 16rpx;
}

.card {
  margin: 18rpx 20rpx 0;
  padding: 24rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 14rpx;
  background: #1a1a1a;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 18rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.07);
}

.card-head.compact {
  padding-bottom: 14rpx;
}

.card-title {
  color: #f0d36a;
  font-size: 30rpx;
  font-weight: 700;
}

.order-no {
  min-width: 0;
  color: rgba(247, 242, 232, 0.54);
  font-size: 22rpx;
  text-align: right;
  word-break: break-all;
}

.goods-row {
  display: flex;
  gap: 20rpx;
  padding-top: 22rpx;
}

.goods-image {
  width: 150rpx;
  height: 150rpx;
  flex: 0 0 150rpx;
  border-radius: 10rpx;
  background: #2a2a2a;
}

.goods-main {
  flex: 1;
  min-width: 0;
}

.goods-title {
  display: block;
  color: #fff;
  font-size: 30rpx;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-meta {
  display: block;
  margin-top: 6rpx;
  color: rgba(247, 242, 232, 0.56);
  font-size: 22rpx;
}

.goods-author {
  display: block;
  margin-top: 8rpx;
  color: rgba(240, 211, 106, 0.88);
  font-size: 23rpx;
}

.goods-price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16rpx;
}

.goods-price {
  color: #f0d36a;
  font-size: 30rpx;
  font-weight: 800;
}

.goods-qty,
.empty-goods {
  color: rgba(247, 242, 232, 0.5);
  font-size: 24rpx;
}

.empty-goods {
  padding-top: 22rpx;
}

.address-user {
  display: flex;
  gap: 24rpx;
  margin-top: 18rpx;
  color: #fff;
  font-size: 28rpx;
  font-weight: 700;
}

.address-text {
  display: block;
  margin-top: 12rpx;
  color: rgba(247, 242, 232, 0.62);
  font-size: 26rpx;
  line-height: 1.45;
}

.fee-row {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
  padding: 18rpx 0;
  color: rgba(247, 242, 232, 0.66);
  font-size: 28rpx;
}

.fee-row text:last-child {
  color: #fff;
  font-weight: 600;
}

.fee-row .discount {
  color: #67c23a;
}

.fee-row.total {
  margin-top: 8rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.07);
}

.fee-row.total text:last-child {
  color: #e85b73;
  font-size: 36rpx;
  font-weight: 800;
}

.method-item {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 24rpx 0;
  border-top: 1rpx solid rgba(255, 255, 255, 0.07);
}

.method-item:first-of-type {
  border-top: none;
}

.method-icon {
  width: 58rpx;
  height: 58rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24rpx;
  font-weight: 800;
}

.method-icon.wechat {
  background: #18b55f;
}

.method-icon.alipay {
  background: #1677ff;
}

.method-info {
  flex: 1;
  min-width: 0;
}

.method-name {
  display: block;
  color: #fff;
  font-size: 28rpx;
  font-weight: 700;
}

.method-desc {
  display: block;
  margin-top: 6rpx;
  color: rgba(247, 242, 232, 0.46);
  font-size: 23rpx;
}

.method-check {
  color: rgba(247, 242, 232, 0.38);
  font-size: 30rpx;
}

.method-item.active .method-check {
  color: #f0d36a;
}

.security-tips {
  display: flex;
  justify-content: center;
  gap: 10rpx;
  padding: 28rpx 30rpx;
  color: rgba(247, 242, 232, 0.46);
  font-size: 24rpx;
}

.pay-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: rgba(18, 18, 18, 0.96);
  border-top: 1rpx solid rgba(255, 255, 255, 0.08);
}

.footer-left {
  flex: 1;
  min-width: 0;
}

.amount-label {
  margin-right: 8rpx;
  color: rgba(247, 242, 232, 0.62);
  font-size: 24rpx;
}

.amount-value {
  color: #e85b73;
  font-size: 38rpx;
  font-weight: 800;
}

.footer-btns {
  display: flex;
  gap: 12rpx;
}

.pay-btn {
  width: 260rpx;
  height: 88rpx;
  border: none;
  border-radius: 44rpx;
  background: linear-gradient(135deg, #dfbd3f 0%, #b88a16 100%);
  color: #171717;
  font-size: 30rpx;
  font-weight: 800;
}

.pay-btn::after {
  border: none;
}

.mock-btn {
  width: 180rpx;
  background: #333;
  color: #f7f2e8;
  font-size: 24rpx;
}

.pay-success-modal {
  position: fixed;
  inset: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.72);
}

.success-content {
  width: 590rpx;
  padding: 58rpx 38rpx;
  border-radius: 22rpx;
  background: #1a1a1a;
  text-align: center;
}

.success-icon {
  width: 86rpx;
  height: 86rpx;
  margin: 0 auto 24rpx;
  border-radius: 50%;
  background: #18b55f;
  color: #fff;
  line-height: 86rpx;
  font-size: 48rpx;
  font-weight: 800;
}

.success-title,
.success-desc {
  display: block;
}

.success-title {
  color: #fff;
  font-size: 38rpx;
  font-weight: 800;
}

.success-desc {
  margin-top: 12rpx;
  color: rgba(247, 242, 232, 0.56);
  font-size: 26rpx;
}

.success-actions {
  display: flex;
  gap: 18rpx;
  margin-top: 42rpx;
}

.action-btn {
  flex: 1;
  height: 84rpx;
  border-radius: 42rpx;
  font-size: 28rpx;
  border: none;
}

.action-btn::after {
  border: none;
}

.action-btn.primary {
  background: #d8b84f;
  color: #151515;
}

.action-btn.secondary {
  background: #2a2a2a;
  color: #f7f2e8;
}
</style>
