<template>
  <view class="order-list-page">
    <view class="seller-workbench" v-if="isSoldMode">
      <view class="seller-workbench-head">
        <view>
          <text class="seller-title">已卖出订单</text>
          <text class="seller-desc">处理发货、物流追踪和售后履约</text>
        </view>
        <button class="seller-quick-btn" @click="switchStatus('paid')">待发货</button>
      </view>
      <view class="seller-stat-row">
        <view class="seller-stat">
          <text class="seller-stat-value">{{ sellerStats.pendingShip }}</text>
          <text class="seller-stat-label">待发货</text>
        </view>
        <view class="seller-stat">
          <text class="seller-stat-value">{{ sellerStats.shipped }}</text>
          <text class="seller-stat-label">运输中</text>
        </view>
        <view class="seller-stat">
          <text class="seller-stat-value">{{ sellerStats.afterSale }}</text>
          <text class="seller-stat-label">售后</text>
        </view>
      </view>
    </view>

    <!-- 状态Tab -->
    <view class="status-tabs">
      <view
        v-for="tab in visibleTabs"
        :key="tab.key"
        class="tab-item"
        :class="{ active: currentStatus === tab.key }"
        @click="switchStatus(tab.key)"
      >
        {{ tab.label }}
        <view class="tab-badge" v-if="tab.key === 'pending' && pendingPayCount">{{ pendingPayCount }}</view>
      </view>
    </view>

    <!-- 订单列表 -->
    <scroll-view class="order-list" scroll-y @scrolltolower="loadMore">
      <view class="order-card" v-for="order in orderList" :key="order.id" @click="goDetail(order.id)">
        <view class="order-header" :class="{ 'sold-order-header': isSoldMode }">
          <view class="seller-info sold-buyer-info" v-if="isSoldMode && order.buyerName" @click.stop>
            <image class="seller-avatar" :src="getFullImageUrl(order.buyerAvatar)" mode="aspectFill"></image>
            <view class="seller-copy">
              <text class="seller-name">买家：{{ order.buyerName }}</text>
              <text class="order-no sold-order-no">订单号：{{ order.orderNo }}</text>
            </view>
          </view>
          <view class="seller-info" v-else-if="order.sellerName" @click.stop>
            <image class="seller-avatar" :src="getFullImageUrl(order.sellerAvatar)" mode="aspectFill"></image>
            <text class="seller-name">{{ order.sellerName }}</text>
          </view>
          <text class="order-no" v-if="!isSoldMode">订单号: {{ order.orderNo }}</text>
          <text class="order-status" :class="'status-' + order.status">
            {{ getStatusText(order.status) }}
          </text>
        </view>
        
        <!-- 订单商品 -->
        <view class="order-goods">
          <view class="goods-item" v-for="item in order.items" :key="item.id">
            <image class="goods-image" :src="getOrderItemImage(item)" mode="aspectFill" @error="onImageError($event, item)"></image>
            <view class="goods-info">
              <text class="goods-title">{{ item.title }}</text>
              <text class="goods-meta">{{ item.artType }}</text>
            </view>
            <view class="goods-right">
              <text class="goods-price">¥{{ formatPrice(item.price) }}</text>
              <text class="goods-qty">x{{ item.quantity }}</text>
            </view>
          </view>
          <view class="goods-empty" v-if="!order.items?.length">
            <text>暂无商品明细，请查看订单详情核对履约信息</text>
          </view>
        </view>
        
        <view class="order-footer">
          <view class="order-info">
            <text class="order-time">{{ formatTime(order.createTime) }}</text>
            <text class="order-count">共{{ order.items?.length || 0 }}件商品</text>
          </view>
          <view class="order-amount">
            <text class="amount-label">实付款:</text>
            <text class="amount-value">¥{{ formatPrice(order.payAmount) }}</text>
          </view>
        </view>
        
        <!-- 操作按钮 -->
        <view class="order-actions" @click.stop>
          <template v-if="isSoldMode">
            <button class="action-btn primary" v-if="canShip(order)" @click="onShip(order)">录入发货</button>
            <button class="action-btn" v-if="canViewLogistics(order)" @click="viewLogistics(order)">查看物流</button>
            <button class="action-btn warning" v-if="isRefundOrder(order)" @click="handleAfterSale(order)">售后处理</button>
          </template>
          <template v-else-if="order.status === 'PENDING_PAYMENT'">
            <button class="action-btn cancel" @click="onCancel(order)">取消订单</button>
            <button class="action-btn primary" @click="onPay(order)">去支付</button>
          </template>
          <template v-else-if="order.status === 'SHIPPED'">
            <button class="action-btn primary" @click="onConfirm(order)">确认收货</button>
          </template>
          <template v-else-if="order.status === 'COMPLETED' || order.status === 'REFUNDED'">
            <button class="action-btn" @click="onDelete(order)">删除订单</button>
          </template>
          <button class="action-btn" @click="goDetail(order.id)">查看详情</button>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="!loading && orderList.length === 0">
        <image class="empty-image" src="/static/images/empty-order.png" mode="aspectFit"></image>
        <text class="empty-text">暂无相关订单</text>
      </view>
      
      <!-- 加载状态 -->
      <view class="load-more" v-if="orderList.length > 0">
        <text v-if="loading">加载中...</text>
        <text v-else-if="noMore">没有更多了</text>
      </view>
    </scroll-view>

    <view class="ship-modal-mask" v-if="shipModalVisible" @click="closeShipModal">
      <view class="ship-modal" @click.stop>
        <view class="ship-modal-head">
          <view>
            <text class="ship-title">录入发货</text>
            <text class="ship-subtitle">提交后订单将进入已发货状态</text>
          </view>
          <button class="ship-close" @click="closeShipModal">×</button>
        </view>

        <view class="ship-order-summary" v-if="shippingOrder">
          <image class="ship-cover" :src="getOrderItemImage(firstShippingItem)" mode="aspectFill"></image>
          <view class="ship-summary-main">
            <text class="ship-goods-name">{{ firstShippingItem.title || '订单商品' }}</text>
            <text class="ship-order-no">订单号：{{ shippingOrder.orderNo }}</text>
            <text class="ship-buyer" v-if="shippingOrder.buyerName">买家：{{ shippingOrder.buyerName }}</text>
          </view>
        </view>

        <view class="ship-receiver-card">
          <view class="ship-card-title">收货信息</view>
          <text class="ship-receiver-line">{{ receiverDisplayName }}</text>
          <text class="ship-receiver-line muted">{{ receiverDisplayAddress }}</text>
        </view>

        <view class="ship-form">
          <view class="ship-field">
            <text class="ship-label">物流公司</text>
            <picker mode="selector" :range="shipCompanies" range-key="name" :value="shipForm.companyIndex" @change="onCompanyChange">
              <view class="ship-picker">
                <text>{{ selectedShipCompany.name || '请选择物流公司' }}</text>
                <text class="ship-picker-arrow">{{ shipCompanyLoading ? '加载中' : '›' }}</text>
              </view>
            </picker>
          </view>

          <view class="ship-field" v-if="selectedShipCompany.code === 'OTHER'">
            <text class="ship-label">物流名称</text>
            <input
              class="ship-input"
              v-model.trim="shipForm.customCompanyName"
              maxlength="20"
              placeholder="请输入物流公司名称"
              placeholder-class="ship-placeholder"
            />
          </view>

          <view class="ship-field">
            <text class="ship-label">运单号</text>
            <input
              class="ship-input"
              v-model.trim="shipForm.trackingNo"
              maxlength="32"
              placeholder="请输入运单号"
              placeholder-class="ship-placeholder"
            />
          </view>

          <view class="ship-field">
            <text class="ship-label">发货备注</text>
            <textarea
              class="ship-textarea"
              v-model.trim="shipForm.remark"
              maxlength="80"
              placeholder="可填写包装、保价或交接说明"
              placeholder-class="ship-placeholder"
            />
          </view>
        </view>

        <view class="ship-modal-actions">
          <button class="ship-action secondary" @click="closeShipModal">取消</button>
          <button class="ship-action primary" :disabled="shipSubmitting" @click="submitShipOrder">
            {{ shipSubmitting ? '提交中...' : '确认发货' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { getOrderList, getSoldOrderList, cancelOrder, confirmReceive, shipOrder, getLogisticsCompanies } from '@/api/order'
import { getFullImageUrl } from '@/utils/image'
import { fenToYuan, formatYuanNumber } from '@/utils/price'

// 状态映射（后端使用字符串）
const statusMap = {
  all: null,
  pending: 'PENDING_PAYMENT',
  paid: 'PAID',
  shipped: 'SHIPPED',
  received: 'RECEIVED',
  completed: 'COMPLETED',
  refund: 'REFUNDING'
}

// 状态
const currentStatus = ref('all')
const orderList = ref([])
const loading = ref(false)
const noMore = ref(false)
const page = ref(1)
const pageSize = 10
const pendingPayCount = ref(0)
const pageType = ref('')
const shipModalVisible = ref(false)
const shipSubmitting = ref(false)
const shipCompanyLoading = ref(false)
const shippingOrder = ref(null)
const shipForm = ref({
  companyIndex: 0,
  customCompanyName: '',
  trackingNo: '',
  remark: ''
})

const fallbackCompanies = [
  { code: 'SF', name: '顺丰速运' },
  { code: 'YTO', name: '圆通速递' },
  { code: 'ZTO', name: '中通快递' },
  { code: 'STO', name: '申通快递' },
  { code: 'YD', name: '韵达快递' },
  { code: 'JTSD', name: '极兔速递' },
  { code: 'EMS', name: 'EMS' },
  { code: 'YZPY', name: '邮政快递包裹' },
  { code: 'OTHER', name: '其他物流' }
]
const shipCompanies = ref([...fallbackCompanies])

const buyerTabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待付款' },
  { key: 'paid', label: '已付款' },
  { key: 'completed', label: '已完成' },
  { key: 'refund', label: '退款' }
]

const sellerTabs = [
  { key: 'all', label: '全部' },
  { key: 'paid', label: '待发货' },
  { key: 'shipped', label: '已发货' },
  { key: 'completed', label: '已完成' },
  { key: 'refund', label: '售后' }
]

const isSoldMode = computed(() => pageType.value === 'sold')
const visibleTabs = computed(() => isSoldMode.value ? sellerTabs : buyerTabs)
const sellerStats = computed(() => ({
  pendingShip: orderList.value.filter(order => order.status === 'PAID').length,
  shipped: orderList.value.filter(order => order.status === 'SHIPPED').length,
  afterSale: orderList.value.filter(order => ['REFUNDING', 'REFUNDED'].includes(order.status)).length
}))

const selectedShipCompany = computed(() => shipCompanies.value[shipForm.value.companyIndex] || shipCompanies.value[0] || {})
const firstShippingItem = computed(() => shippingOrder.value?.items?.[0] || {})
const receiverInfo = computed(() => shippingOrder.value?.address || {})
const receiverDisplayName = computed(() => {
  const receiver = receiverInfo.value
  const name = receiver.receiverName || shippingOrder.value?.receiverName || '收货人待核对'
  const phone = receiver.receiverPhone || shippingOrder.value?.receiverPhone || ''
  return phone ? `${name} ${phone}` : name
})
const receiverDisplayAddress = computed(() => {
  const receiver = receiverInfo.value
  const parts = [
    receiver.province,
    receiver.city,
    receiver.district,
    receiver.detailAddress,
    shippingOrder.value?.receiverAddress
  ].filter(Boolean)
  return parts.join('') || '暂无收货地址，请先在订单详情核对'
})

const readRouteOptionsFromLocation = () => {
  if (typeof window === 'undefined') return {}
  const query = window.location.href.split('?')[1]?.split('#')[0] || window.location.hash.split('?')[1] || ''
  return Object.fromEntries(new URLSearchParams(query))
}

const normalizeOrderItem = (item) => ({
  ...item,
  title: item.title || item.goodsName || item.itemTitle || '',
  coverImage: item.coverImage ||
    item.goodsImage ||
    item.cover ||
    item.coverUrl ||
    item.image ||
    item.imageUrl ||
    item.artworkCover ||
    item.artworkCoverUrl ||
    '',
  quantity: item.quantity || item.count || 1,
  artType: item.artType || item.specName || item.categoryName || ''
})

const getOrderItemImage = (item = {}) => {
  return getFullImageUrl(item.coverImage, '/static/images/artwork-fallback.png')
}

// Tab切换
const switchStatus = (status) => {
  currentStatus.value = status
  orderList.value = []
  page.value = 1
  noMore.value = false
  fetchOrderList(true)
}

// 获取订单列表
const fetchOrderList = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    page.value = 1
    noMore.value = false
  }
  
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize,
      status: statusMap[currentStatus.value]
    }
    
    const result = await (isSoldMode.value ? getSoldOrderList(params) : getOrderList(params))
    
    // 处理 PageResult 格式：{ records: [], total: xxx } 或直接数组
    const rawList = result?.records || result?.list || result || []
    const list = Array.isArray(rawList) ? rawList : []
    
    // 数据归一化：确保每项有 items 数组、合理的默认值
    const normalized = list.map(item => ({
      ...item,
      items: (item.items || item.goodsList || item.goods || item.orderItems || []).map(normalizeOrderItem),
      payAmount: item.payAmount || item.amount || item.totalAmount || 0,
      orderNo: item.orderNo || item.orderNumber || item.id || ''
    }))
    
    if (reset) {
      orderList.value = normalized
    } else {
      orderList.value = [...orderList.value, ...normalized]
    }
    
    pendingPayCount.value = result?.pendingPayCount || 0
    
    if (list.length < pageSize) {
      noMore.value = true
    } else {
      page.value++
    }
  } catch (e) {
    console.warn('[订单列表] 加载失败:', e)
    if (reset) orderList.value = []
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = () => {
  if (!noMore.value) {
    fetchOrderList()
  }
}

// 跳转详情
const goDetail = (id) => {
  uni.navigateTo({ url: `/pages/order/detail?id=${id}` })
}

// 取消订单
const onCancel = (order) => {
  uni.showModal({
    title: '提示',
    content: '确定取消该订单？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await cancelOrder(order.id)
          uni.showToast({ title: '订单已取消', icon: 'success' })
          fetchOrderList(true)
        } catch (e) {
          uni.showToast({ title: '取消失败', icon: 'none' })
        }
      }
    }
  })
}

// 去支付
const onPay = (order) => {
  uni.navigateTo({
    url: `/pages/order/pay?orderId=${order.id}&amount=${fenToYuan(order.payAmount)}`
  })
}

const canShip = (order) => isSoldMode.value && order.status === 'PAID'
const canViewLogistics = (order) => isSoldMode.value && ['SHIPPED', 'RECEIVED', 'COMPLETED'].includes(order.status)
const isRefundOrder = (order) => ['REFUNDING', 'REFUNDED'].includes(order.status)

const ensureOtherCompanyOption = (companies) => {
  const normalized = companies
    .filter(item => item?.code && item?.name)
    .map(item => ({ code: item.code, name: item.name }))
  return normalized.some(item => item.code === 'OTHER')
    ? normalized
    : [...normalized, { code: 'OTHER', name: '其他物流' }]
}

const loadShipCompanies = async () => {
  if (shipCompanies.value.length > fallbackCompanies.length) return
  shipCompanyLoading.value = true
  try {
    const result = await getLogisticsCompanies()
    const list = Array.isArray(result) ? result : (result?.records || result?.list || [])
    shipCompanies.value = ensureOtherCompanyOption(list.length ? list : fallbackCompanies)
  } catch (e) {
    console.warn('[卖出订单] 获取物流公司失败，使用默认列表:', e)
    shipCompanies.value = [...fallbackCompanies]
  } finally {
    shipCompanyLoading.value = false
  }
}

const resetShipForm = () => {
  shipForm.value = {
    companyIndex: 0,
    customCompanyName: '',
    trackingNo: '',
    remark: ''
  }
}

const onShip = async (order) => {
  shippingOrder.value = order
  resetShipForm()
  shipModalVisible.value = true
  await loadShipCompanies()
}

const closeShipModal = (force = false) => {
  if (shipSubmitting.value && !force) return
  shipModalVisible.value = false
  shippingOrder.value = null
  resetShipForm()
}

const onCompanyChange = (event) => {
  shipForm.value.companyIndex = Number(event.detail.value || 0)
}

const validateShipForm = () => {
  const company = selectedShipCompany.value
  const trackingNo = shipForm.value.trackingNo.replace(/\s+/g, '')
  const customName = shipForm.value.customCompanyName.trim()

  if (!company.code) {
    return '请选择物流公司'
  }
  if (company.code === 'OTHER' && !customName) {
    return '请输入物流公司名称'
  }
  if (!trackingNo) {
    return '请输入运单号'
  }
  if (!/^[A-Za-z0-9-]{6,32}$/.test(trackingNo)) {
    return '运单号需为6-32位字母、数字或横线'
  }
  return ''
}

const submitShipOrder = async () => {
  if (!shippingOrder.value || shipSubmitting.value) return
  const error = validateShipForm()
  if (error) {
    uni.showToast({ title: error, icon: 'none' })
    return
  }

  const company = selectedShipCompany.value
  const trackingNo = shipForm.value.trackingNo.replace(/\s+/g, '')
  const companyName = company.code === 'OTHER' ? shipForm.value.customCompanyName.trim() : company.name
  shipSubmitting.value = true
  try {
    await shipOrder({
      orderId: shippingOrder.value.id,
      companyCode: company.code,
      companyName,
      trackingNo,
      remark: shipForm.value.remark || '艺术家工作台发货'
    })
    uni.showToast({ title: '发货成功', icon: 'success' })
    closeShipModal(true)
    fetchOrderList(true)
  } catch (e) {
    uni.showToast({ title: e.message || '发货失败', icon: 'none' })
  } finally {
    shipSubmitting.value = false
  }
}

const viewLogistics = (order) => {
  uni.navigateTo({ url: `/pages/order/logistics?orderId=${order.id}` })
}

const handleAfterSale = () => {
  uni.showToast({ title: '售后处理功能完善中', icon: 'none' })
}

// 确认收货
const onConfirm = (order) => {
  uni.showModal({
    title: '提示',
    content: '确认已收到货物？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await confirmReceive(order.id)
          uni.showToast({ title: '已确认收货', icon: 'success' })
          fetchOrderList(true)
        } catch (e) {
          uni.showToast({ title: '操作失败', icon: 'none' })
        }
      }
    }
  })
}

// 删除订单
const onDelete = (order) => {
  uni.showModal({
    title: '提示',
    content: '确定删除该订单？',
    success: async (res) => {
      if (res.confirm) {
        // TODO: 调用删除接口
        orderList.value = orderList.value.filter(o => o.id !== order.id)
        uni.showToast({ title: '已删除', icon: 'success' })
      }
    }
  })
}

// 获取状态文字
const getStatusText = (status) => {
  const map = {
    'CANCELLED': '已取消',
    'PENDING_PAYMENT': '待付款',
    'PAID': '已付款',
    'SHIPPED': '已发货',
    'RECEIVED': '已收货',
    'COMPLETED': '已完成',
    'REFUNDING': '退款中',
    'REFUNDED': '已退款'
  }
  return map[status] || '未知'
}

const formatPrice = (price) => {
  return formatYuanNumber(fenToYuan(price))
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

// 图片加载失败处理
const onImageError = (e, item) => {
  console.warn('图片加载失败:', item.coverImage)
  if (e.target) {
    e.target.src = '/static/images/artwork-fallback.png'
  }
}

// 初始化
onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = { ...readRouteOptionsFromLocation(), ...(currentPage.options || {}) }
  pageType.value = options.type || ''
  uni.setNavigationBarTitle({
    title: isSoldMode.value ? '卖出订单' : '我的订单'
  })
  const status = options.status
  if (status) {
    currentStatus.value = status
  }
  fetchOrderList(true)
})
</script>

<style lang="scss" scoped>
.order-list-page {
  min-height: 100vh;
  background-color: #f8f8f8;
}

.status-tabs {
  display: flex;
  background-color: #ffffff;
  padding: 0 20rpx;
  position: sticky;
  top: 0;
  z-index: 10;
  
  .tab-item {
    flex: 1;
    text-align: center;
    padding: 24rpx 0;
    font-size: 28rpx;
    color: #666666;
    position: relative;
    
    &.active {
      color: #333333;
      font-weight: 600;
      
      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 48rpx;
        height: 4rpx;
        background-color: #333333;
        border-radius: 2rpx;
      }
    }
    
    .tab-badge {
      position: absolute;
      top: 12rpx;
      right: 20rpx;
      min-width: 32rpx;
      height: 32rpx;
      padding: 0 8rpx;
      background-color: #ff4d4f;
      color: #ffffff;
      font-size: 20rpx;
      border-radius: 16rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }
}

.order-list {
  padding: 20rpx 30rpx;
  height: calc(100vh - 100rpx);
}

.order-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
  
    .order-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 24rpx;
      border-bottom: 1rpx solid #f5f5f5;

      &.sold-order-header {
        align-items: flex-start;
        gap: 16rpx;
      }

      .seller-info {
        display: flex;
        align-items: center;
        min-width: 0;

        &.sold-buyer-info {
          flex: 1;
          align-items: flex-start;
        }
      
        .seller-avatar {
          flex: 0 0 auto;
          width: 40rpx;
          height: 40rpx;
          border-radius: 50%;
          margin-right: 8rpx;
          background-color: #f5f5f5;
      }
      
        .seller-name {
          display: block;
          font-size: 26rpx;
          color: #333333;
          font-weight: 500;
          line-height: 1.25;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }

      .seller-copy {
        flex: 1;
        min-width: 0;
        overflow: hidden;
      }
    
      .order-no {
        font-size: 24rpx;
        color: #999999;
      }

      .sold-order-no {
        display: block;
        margin-top: 6rpx;
        font-size: 20rpx;
        line-height: 1.25;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    
      .order-status {
        flex: 0 0 auto;
        font-size: 26rpx;
        line-height: 1.25;
        white-space: nowrap;
      
        &.status-PENDING_PAYMENT { color: #ff4d4f; }
      &.status-PAID { color: #1890ff; }
      &.status-SHIPPED { color: #faad14; }
      &.status-COMPLETED { color: #52c41a; }
      &.status-REFUNDING, &.status-REFUNDED, &.status-CANCELLED { color: #999999; }
    }
  }
  
  .order-goods {
    padding: 20rpx 24rpx;
    
    .goods-item {
      display: flex;
      margin-bottom: 16rpx;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .goods-image {
        width: 140rpx;
        height: 140rpx;
        border-radius: 8rpx;
        background-color: #f5f5f5;
        margin-right: 16rpx;
      }
      
      .goods-info {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        
        .goods-title {
          font-size: 28rpx;
          color: #333333;
          @include ellipsis(1);
          margin-bottom: 8rpx;
        }
        
        .goods-meta {
          font-size: 24rpx;
          color: #999999;
        }
      }
      
      .goods-right {
        flex: 0 0 auto;
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        margin-left: 12rpx;

        .goods-price {
          font-size: 28rpx;
          color: #333333;
          white-space: nowrap;
        }
        
        .goods-qty {
          font-size: 24rpx;
          color: #999999;
          margin-top: auto;
        }
      }
    }

    .goods-empty {
      padding: 18rpx 20rpx;
      border: 1rpx dashed #dddddd;
      border-radius: 12rpx;
      color: #999999;
      font-size: 24rpx;
      line-height: 1.45;
    }
  }
  
  .order-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 24rpx 20rpx;
    
    .order-info {
      display: flex;
      align-items: center;
      
      .order-time {
        font-size: 24rpx;
        color: #999999;
        margin-right: 20rpx;
      }
      
      .order-count {
        font-size: 24rpx;
        color: #999999;
      }
    }
    
    .order-amount {
      .amount-label {
        font-size: 24rpx;
        color: #666666;
      }
      
      .amount-value {
        font-size: 30rpx;
        font-weight: 600;
        color: #333333;
        margin-left: 8rpx;
      }
    }
  }
  
  .order-actions {
    display: flex;
    justify-content: flex-end;
    padding: 16rpx 24rpx;
    border-top: 1rpx solid #f5f5f5;
    
    .action-btn {
      min-width: 160rpx;
      height: 64rpx;
      padding: 0 24rpx;
      font-size: 26rpx;
      background-color: #f5f5f5;
      color: #666666;
      border-radius: 32rpx;
      margin-left: 16rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      
      &.primary {
        background-color: #333333;
        color: #ffffff;
      }
      
      &.cancel {
        background-color: transparent;
        border: 1rpx solid #cccccc;
      }
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
  
  .empty-image {
    width: 200rpx;
    height: 200rpx;
    margin-bottom: 30rpx;
  }
  
  .empty-text {
    font-size: 28rpx;
    color: #999999;
  }
}

.load-more {
  text-align: center;
  padding: 30rpx 0;
  font-size: 24rpx;
  color: #999999;
}

/* 订单页暗色视觉优化 */
.order-list-page {
  background: #0b0b0c;
  color: #f6f2e8;
}

.status-tabs {
  background: rgba(11, 11, 12, 0.96);
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.08);
  padding: 0 24rpx;

  .tab-item {
    color: #8f8a80;
    font-weight: 600;

    &.active {
      color: #f6f2e8;

      &::after {
        background-color: #c9a227;
        width: 42rpx;
      }
    }
  }
}

.seller-workbench {
  margin: 24rpx 24rpx 0;
  padding: 26rpx;
  border: 1rpx solid rgba(201, 162, 39, 0.18);
  border-radius: 18rpx;
  background:
    radial-gradient(circle at 84% 20%, rgba(201, 162, 39, 0.18), transparent 36%),
    linear-gradient(135deg, rgba(28, 28, 30, 0.96), rgba(15, 15, 16, 0.98));
  box-shadow: 0 18rpx 42rpx rgba(0, 0, 0, 0.24);
}

.seller-workbench-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.seller-title,
.seller-desc {
  display: block;
}

.seller-title {
  color: #f6f2e8;
  font-size: 34rpx;
  font-weight: 800;
}

.seller-desc {
  margin-top: 8rpx;
  color: rgba(246, 242, 232, 0.58);
  font-size: 24rpx;
}

.seller-quick-btn {
  height: 58rpx;
  min-width: 132rpx;
  padding: 0 24rpx;
  border-radius: 29rpx;
  background: #c9a227;
  color: #15120a;
  font-size: 24rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.seller-quick-btn::after {
  border: none;
}

.seller-stat-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  margin-top: 24rpx;
}

.seller-stat {
  padding: 18rpx 12rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 14rpx;
  background: rgba(255, 255, 255, 0.04);
  text-align: center;
}

.seller-stat-value,
.seller-stat-label {
  display: block;
}

.seller-stat-value {
  color: #f2c85b;
  font-size: 34rpx;
  font-weight: 800;
}

.seller-stat-label {
  margin-top: 4rpx;
  color: #8f8a80;
  font-size: 22rpx;
}

.order-list {
  padding: 24rpx;
  box-sizing: border-box;
}

.order-card {
  background: #171719;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 18rpx;
  box-shadow: 0 18rpx 42rpx rgba(0, 0, 0, 0.22);

  .order-header {
    border-bottom-color: rgba(255, 255, 255, 0.08);

    .seller-info .seller-name {
      color: #f6f2e8;
    }

    .order-no {
      color: #8f8a80;
    }

    .order-status {
      font-weight: 700;

      &.status-PENDING_PAYMENT { color: #ff7a7a; }
      &.status-PAID { color: #c9a227; }
      &.status-SHIPPED { color: #f0b64d; }
      &.status-COMPLETED { color: #69d37b; }
    }
  }

  .order-goods .goods-item {
    .goods-image {
      background-color: #202024;
    }

    .goods-info {
      .goods-title {
        color: #f6f2e8;
        font-weight: 700;
      }

      .goods-meta {
        color: #8f8a80;
      }
    }

    .goods-right {
      .goods-price {
        color: #f2c85b;
        font-weight: 800;
      }

      .goods-qty {
        color: #8f8a80;
      }
    }
  }

  .order-goods .goods-empty {
    border-color: rgba(255, 255, 255, 0.1);
    background: rgba(255, 255, 255, 0.03);
    color: #8f8a80;
  }

  .order-footer {
    .order-info {
      .order-time,
      .order-count {
        color: #8f8a80;
      }
    }

    .order-amount {
      .amount-label {
        color: #8f8a80;
      }

      .amount-value {
        color: #f2c85b;
      }
    }
  }

  .order-actions {
    border-top-color: rgba(255, 255, 255, 0.08);

    .action-btn {
      background: #202024;
      color: #f6f2e8;

      &.primary {
        background: #c9a227;
        color: #16130b;
        font-weight: 800;
      }

      &.cancel {
        border-color: rgba(255, 255, 255, 0.16);
        color: #9b958a;
      }

      &.warning {
        background: rgba(255, 122, 122, 0.14);
        color: #ff9a9a;
      }
    }
  }
}

.empty-state {
  min-height: 56vh;
  justify-content: center;

  .empty-image {
    opacity: 0.62;
  }

  .empty-text {
    color: #8f8a80;
  }
}

.load-more {
  color: #68645c;
}

.ship-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 999;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(0, 0, 0, 0.68);
}

.ship-modal {
  width: 100%;
  max-height: 86vh;
  padding: 28rpx 28rpx calc(28rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  border-radius: 28rpx 28rpx 0 0;
  border: 1rpx solid rgba(255, 255, 255, 0.1);
  background:
    radial-gradient(circle at 86% 0%, rgba(201, 162, 39, 0.14), transparent 32%),
    #171719;
  color: #f6f2e8;
  box-shadow: 0 -24rpx 60rpx rgba(0, 0, 0, 0.44);
}

.ship-modal-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 22rpx;
}

.ship-title,
.ship-subtitle {
  display: block;
}

.ship-title {
  font-size: 34rpx;
  line-height: 1.2;
  font-weight: 800;
  color: #f6f2e8;
}

.ship-subtitle {
  margin-top: 8rpx;
  font-size: 23rpx;
  line-height: 1.35;
  color: rgba(246, 242, 232, 0.58);
}

.ship-close {
  width: 58rpx;
  height: 58rpx;
  padding: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(246, 242, 232, 0.72);
  font-size: 38rpx;
  line-height: 58rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ship-close::after,
.ship-action::after {
  border: none;
}

.ship-order-summary {
  display: flex;
  gap: 18rpx;
  padding: 18rpx;
  margin-bottom: 18rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.04);
}

.ship-cover {
  width: 116rpx;
  height: 116rpx;
  flex: 0 0 116rpx;
  border-radius: 12rpx;
  background: #202024;
}

.ship-summary-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8rpx;
}

.ship-goods-name,
.ship-order-no,
.ship-buyer,
.ship-receiver-line {
  display: block;
}

.ship-goods-name {
  font-size: 28rpx;
  line-height: 1.35;
  font-weight: 700;
  color: #f6f2e8;
  @include ellipsis(1);
}

.ship-order-no,
.ship-buyer {
  font-size: 22rpx;
  line-height: 1.35;
  color: rgba(246, 242, 232, 0.58);
}

.ship-receiver-card {
  padding: 18rpx;
  margin-bottom: 18rpx;
  border-radius: 18rpx;
  background: rgba(201, 162, 39, 0.08);
  border: 1rpx solid rgba(201, 162, 39, 0.16);
}

.ship-card-title {
  margin-bottom: 10rpx;
  font-size: 24rpx;
  font-weight: 700;
  color: #f2c85b;
}

.ship-receiver-line {
  font-size: 24rpx;
  line-height: 1.45;
  color: rgba(246, 242, 232, 0.86);
}

.ship-receiver-line.muted {
  margin-top: 4rpx;
  color: rgba(246, 242, 232, 0.58);
}

.ship-form {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.ship-field {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.ship-label {
  font-size: 24rpx;
  color: rgba(246, 242, 232, 0.72);
}

.ship-picker,
.ship-input,
.ship-textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1rpx solid rgba(255, 255, 255, 0.1);
  border-radius: 14rpx;
  background: #202024;
  color: #f6f2e8;
  font-size: 26rpx;
}

.ship-picker {
  min-height: 76rpx;
  padding: 0 22rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ship-picker-arrow {
  color: #8f8a80;
  font-size: 24rpx;
}

.ship-input {
  height: 76rpx;
  padding: 0 22rpx;
}

.ship-textarea {
  height: 132rpx;
  padding: 18rpx 22rpx;
  line-height: 1.45;
}

.ship-placeholder {
  color: rgba(246, 242, 232, 0.35);
}

.ship-modal-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
  margin-top: 24rpx;
}

.ship-action {
  height: 78rpx;
  border-radius: 39rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 27rpx;
  font-weight: 700;
}

.ship-action.secondary {
  border: 1rpx solid rgba(255, 255, 255, 0.12);
  background: #202024;
  color: #f6f2e8;
}

.ship-action.primary {
  background: #c9a227;
  color: #16130b;
}

.ship-action[disabled] {
  opacity: 0.62;
}
</style>
