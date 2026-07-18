<template>
  <view class="order-detail">
    <view class="status-banner" :class="statusClass">
      <view>
        <text class="status-label">订单状态</text>
        <text class="status-text">{{ getStatusText(orderInfo.status) }}</text>
      </view>
      <text class="status-no">{{ orderInfo.orderNo || '--' }}</text>
    </view>

    <view v-if="loading" class="state-panel">订单加载中...</view>
    <view v-else-if="loadError" class="state-panel error">
      <text>{{ loadError }}</text>
      <view class="retry-btn" @click="loadOrderDetail">重新加载</view>
    </view>

    <template v-else>
      <view class="card address-card" v-if="hasAddress">
        <text class="card-title">收货信息</text>
        <view class="address-user">
          <text class="user-name">{{ orderInfo.address.receiverName || '--' }}</text>
          <text class="user-phone">{{ orderInfo.address.receiverPhone || '--' }}</text>
        </view>
        <text class="address-detail">{{ fullAddress }}</text>
      </view>

      <view class="card goods-card">
        <view class="goods-header">
          <image class="shop-logo" :src="orderInfo.sellerAvatar || '/static/images/avatar.png'" mode="aspectFill"></image>
          <text class="shop-name">{{ orderInfo.sellerName || '艺本艺术旗舰店' }}</text>
        </view>
        <view
          class="goods-item"
          v-for="item in orderInfo.goodsList"
          :key="item.id"
          @click="goGoodsDetail(item.goodsId)"
        >
          <image class="goods-image" :src="resolveGoodsImage(item)" mode="aspectFill"></image>
          <view class="goods-info">
            <text class="goods-title">{{ item.goodsName }}</text>
            <text class="goods-artist" v-if="resolveArtistName(item)">艺术家：{{ resolveArtistName(item) }}</text>
            <view class="goods-meta" v-if="goodsMetaList(item).length">
              <text
                v-for="meta in goodsMetaList(item)"
                :key="meta"
                class="goods-meta-item"
              >{{ meta }}</text>
            </view>
            <text class="goods-spec" v-if="item.specName && !goodsMetaList(item).length">{{ item.specName }}</text>
            <view class="goods-bottom">
              <text class="goods-price">¥{{ formatMoney(item.price) }}</text>
              <text class="goods-count">x{{ item.count }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="card detail-card">
        <text class="card-title">订单信息</text>
        <view class="info-row">
          <text class="info-label">订单编号</text>
          <view class="info-value">
            <text>{{ orderInfo.orderNo || '--' }}</text>
            <text class="copy-btn" @click="copyOrderNo">复制</text>
          </view>
        </view>
        <view class="info-row">
          <text class="info-label">下单时间</text>
          <text class="info-value">{{ formatDateTime(orderInfo.createTime) }}</text>
        </view>
        <view class="info-row" v-if="orderInfo.payTime">
          <text class="info-label">支付时间</text>
          <text class="info-value">{{ formatDateTime(orderInfo.payTime) }}</text>
        </view>
        <view class="info-row" v-if="orderInfo.deliveryTime">
          <text class="info-label">发货时间</text>
          <text class="info-value">{{ formatDateTime(orderInfo.deliveryTime) }}</text>
        </view>
        <view class="info-row" v-if="orderInfo.completeTime">
          <text class="info-label">完成时间</text>
          <text class="info-value">{{ formatDateTime(orderInfo.completeTime) }}</text>
        </view>
        <view class="info-row" v-if="orderInfo.remark">
          <text class="info-label">订单备注</text>
          <text class="info-value remark">{{ orderInfo.remark }}</text>
        </view>
      </view>

      <view class="card amount-card">
        <text class="card-title">金额明细</text>
        <view class="amount-row">
          <text class="amount-label">商品金额</text>
          <text class="amount-value">¥{{ formatMoney(orderInfo.goodsAmount) }}</text>
        </view>
        <view class="amount-row">
          <text class="amount-label">运费</text>
          <text class="amount-value">¥{{ formatMoney(orderInfo.freight) }}</text>
        </view>
        <view class="amount-row" v-if="Number(orderInfo.discountAmount) > 0">
          <text class="amount-label">优惠</text>
          <text class="amount-value discount">-¥{{ formatMoney(orderInfo.discountAmount) }}</text>
        </view>
        <view class="amount-row total">
          <text class="amount-label">实付款</text>
          <text class="amount-value">¥{{ formatMoney(orderInfo.payAmount) }}</text>
        </view>
      </view>

      <view class="card refund-card" v-if="showRefundSection">
        <view class="refund-card-head">
          <text class="card-title">售后信息</text>
          <text class="refund-type-badge" :class="{ gold: orderInfo.refundType === 2 }">
            {{ orderInfo.refundType === 2 ? '退款退货' : '仅退款' }}
          </text>
        </view>
        <view class="info-row" v-if="orderInfo.refundReason">
          <text class="info-label">退款原因</text>
          <text class="info-value remark">{{ orderInfo.refundReason }}</text>
        </view>
        <view class="info-row" v-if="Number(orderInfo.refundAmount || 0) > 0">
          <text class="info-label">申请金额</text>
          <text class="info-value refund-amount">¥{{ formatMoney(orderInfo.refundAmount) }}</text>
        </view>
        <view class="return-logistics-card" v-if="orderInfo.refundType === 2">
          <view class="return-logistics-head">
            <view>
              <text class="return-logistics-title">退货运单</text>
              <text class="return-logistics-tip">{{ returnLogisticsTip }}</text>
            </view>
            <view
              v-if="canSubmitReturnLogistics"
              class="mini-action-btn"
              @click="openReturnModal"
            >提交运单</view>
          </view>
          <view v-if="hasReturnTracking">
            <view class="info-row">
              <text class="info-label">物流公司</text>
              <text class="info-value">{{ orderInfo.returnCompanyName || '--' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">运单号</text>
              <view class="info-value">
                <text>{{ orderInfo.returnTrackingNo }}</text>
                <text class="copy-btn" @click="copyText(orderInfo.returnTrackingNo, '运单号已复制')">复制</text>
              </view>
            </view>
            <view class="info-row" v-if="orderInfo.returnShipTime">
              <text class="info-label">提交时间</text>
              <text class="info-value">{{ formatDateTime(orderInfo.returnShipTime) }}</text>
            </view>
            <view class="info-row" v-if="Number(orderInfo.returnStatus || 0) > 0">
              <text class="info-label">物流状态</text>
              <text class="info-value">{{ getReturnStatusText(orderInfo.returnStatus) }}</text>
            </view>
            <view class="info-row" v-if="orderInfo.returnReceiveTime">
              <text class="info-label">签收时间</text>
              <text class="info-value">{{ formatDateTime(orderInfo.returnReceiveTime) }}</text>
            </view>
          </view>
        </view>
      </view>
    </template>

    <view class="bottom-bar" v-if="!loading && !loadError">
      <view class="ghost-btn" @click="contactSeller">联系卖家</view>
      <view class="ghost-btn" @click="shareOrderArtwork" v-if="shareArtworkId">分享作品</view>
      <view class="ghost-btn" @click="viewLogistics" v-if="['SHIPPED', 'COMPLETED'].includes(orderInfo.status)">查看物流</view>
      <view class="ghost-btn" @click="cancelOrder" v-if="orderInfo.status === 'PENDING_PAYMENT'">取消订单</view>
      <view class="primary-btn" @click="payOrder" v-if="canPayOrder">去支付</view>
      <view class="primary-btn" @click="confirmReceive" v-if="orderInfo.status === 'SHIPPED'">确认收货</view>
      <view class="ghost-btn" @click="applyRefund" v-if="canApplyRefund">申请退款</view>
      <view class="primary-btn" @click="reviewOrder" v-if="orderInfo.status === 'COMPLETED'">去评价</view>
    </view>

    <view class="ship-modal-mask" v-if="returnModalVisible" @click="closeReturnModal">
      <view class="ship-modal" @click.stop>
        <view class="ship-modal-head">
          <view>
            <text class="ship-title">提交退货运单</text>
            <text class="ship-subtitle">提交后卖家可根据运单跟进退货进度</text>
          </view>
          <button class="ship-close" @click="closeReturnModal">×</button>
        </view>

        <view class="ship-order-summary">
          <image class="ship-cover" :src="summaryGoodsImage" mode="aspectFill"></image>
          <view class="ship-summary-main">
            <text class="ship-goods-name">{{ summaryGoodsTitle }}</text>
            <text class="ship-order-no">订单号：{{ orderInfo.orderNo || '--' }}</text>
            <text class="ship-buyer">退款类型：退款退货</text>
          </view>
        </view>

        <view class="ship-form">
          <view class="ship-field">
            <text class="ship-label">运单号</text>
            <input
              class="ship-input"
              v-model.trim="returnShipForm.trackingNo"
              maxlength="32"
              placeholder="请输入退货运单号"
              placeholder-class="ship-placeholder"
              @input="onReturnTrackingInput"
            />
            <text class="ship-helper" v-if="returnShipDetectState.message">{{ returnShipDetectState.message }}</text>
          </view>

          <view class="ship-field">
            <text class="ship-label">物流公司</text>
            <picker mode="selector" :range="shipCompanies" range-key="name" :value="returnShipForm.companyIndex" @change="onReturnCompanyChange">
              <view class="ship-picker">
                <text>{{ selectedReturnCompany.name || '请选择物流公司' }}</text>
                <text class="ship-picker-arrow">{{ shipCompanyLoading ? '加载中' : '›' }}</text>
              </view>
            </picker>
          </view>

          <view class="ship-field" v-if="selectedReturnCompany.code === 'OTHER'">
            <text class="ship-label">物流名称</text>
            <input
              class="ship-input"
              v-model.trim="returnShipForm.customCompanyName"
              maxlength="20"
              placeholder="请输入物流公司名称"
              placeholder-class="ship-placeholder"
            />
          </view>
        </view>

        <view class="ship-modal-actions">
          <button class="ship-action secondary" @click="closeReturnModal">取消</button>
          <button class="ship-action primary" :disabled="returnSubmitting" @click="submitReturnLogistics">
            {{ returnSubmitting ? '提交中...' : '确认提交' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getOrderDetail, cancelOrder, confirmReceive, getLogisticsCompanies, getLogisticsByTrackingNo, submitRefundReturnLogistics } from '@/api/order.js'
import { getFullImageUrl } from '@/utils/image.js'
import { fenToYuan, formatYuanNumber } from '@/utils/price.js'
import { buildH5ShareUrl, setH5ShareMeta, shareH5OrCopy } from '@/utils/share.js'

export default {
  data() {
    return {
      loading: false,
      loadError: '',
      orderId: '',
      orderInfo: this.emptyOrder(),
      shipCompanyLoading: false,
      shipCompanies: this.defaultShipCompanies(),
      returnModalVisible: false,
      returnSubmitting: false,
      returnShipDetectTimer: null,
      returnShipDetectState: {
        message: '',
        source: ''
      },
      returnShipForm: this.emptyReturnShipForm()
    }
  },

  computed: {
    statusClass() {
      return `status-${String(this.orderInfo.status || 'unknown').toLowerCase()}`
    },
    canPayOrder() {
      return this.orderInfo.status === 'PENDING_PAYMENT' && Number(this.orderInfo.payAmount || 0) > 0
    },
    canApplyRefund() {
      return ['PAID', 'SHIPPED'].includes(this.orderInfo.status)
    },
    showRefundSection() {
      return ['REFUNDING', 'REFUNDED'].includes(this.orderInfo.status) || Number(this.orderInfo.refundType || 0) > 0
    },
    hasReturnTracking() {
      return Boolean(this.orderInfo.returnTrackingNo)
    },
    canSubmitReturnLogistics() {
      return this.orderInfo.status === 'REFUNDING' &&
        Number(this.orderInfo.refundType || 0) === 2 &&
        !this.hasReturnTracking
    },
    returnLogisticsTip() {
      if (!this.hasReturnTracking) {
        return '请回寄后尽快提交运单号，方便卖家确认。'
      }
      if (Number(this.orderInfo.returnStatus || 0) === 4) {
        return '退货包裹已签收，系统将自动处理退款。'
      }
      return '已提交回寄信息，等待卖家确认收货。'
    },
    selectedReturnCompany() {
      return this.shipCompanies[this.returnShipForm.companyIndex] || this.shipCompanies[0] || {}
    },
    summaryGoodsTitle() {
      return this.orderInfo.goodsList?.[0]?.goodsName || '订单商品'
    },
    summaryGoodsImage() {
      return this.resolveGoodsImage(this.orderInfo.goodsList?.[0] || {})
    },
    shareArtworkId() {
      return this.orderInfo.goodsList?.[0]?.goodsId || ''
    },
    shareArtworkTitle() {
      return this.orderInfo.goodsList?.[0]?.goodsName || '艺本艺术作品'
    },
    fullAddress() {
      const address = this.orderInfo.address || {}
      return address.fullAddress || [
        address.province,
        address.city,
        address.district,
        address.detail
      ].filter(Boolean).join('')
    },
    hasAddress() {
      const address = this.orderInfo.address || {}
      return Boolean(
        address.receiverName ||
        address.receiverPhone ||
        address.fullAddress ||
        address.province ||
        address.city ||
        address.district ||
        address.detail
      )
    },
    derivedGoodsAmount() {
      return (this.orderInfo.goodsList || []).reduce((sum, item) => {
        const subtotal = item.subtotal ?? ((item.price || 0) * (item.count || item.quantity || 1))
        return sum + Number(subtotal || 0)
      }, 0)
    }
  },

  onLoad(options) {
    this.orderId = options.id || options.orderId || ''
    if (this.orderId) {
      this.loadOrderDetail()
    } else {
      this.loadError = '订单参数缺失'
    }
  },

  onShareAppMessage() {
    return {
      title: `${this.shareArtworkTitle}｜艺本艺术`,
      path: this.buildArtworkShareRoute(),
      imageUrl: this.summaryGoodsImage
    }
  },

  methods: {
    emptyOrder() {
      return {
        status: '',
        address: null,
        goodsList: [],
        goodsAmount: 0,
        freight: 0,
        discountAmount: 0,
        payAmount: 0,
        refundType: null,
        refundStatus: null,
        refundReason: '',
        refundAmount: 0,
        refundImages: '',
        returnCompanyCode: '',
        returnCompanyName: '',
        returnTrackingNo: '',
        returnStatus: null,
        returnShipTime: '',
        returnReceiveTime: ''
      }
    },

    defaultShipCompanies() {
      return [
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
    },

    emptyReturnShipForm() {
      return {
        companyIndex: 0,
        customCompanyName: '',
        trackingNo: ''
      }
    },

    async loadOrderDetail() {
      this.loading = true
      this.loadError = ''
      try {
        const detail = await getOrderDetail(this.orderId)
        if (!detail) throw new Error('订单不存在')
        const sourceGoodsList = detail.goodsList || detail.items || []
        const goodsList = sourceGoodsList.map((item) => this.normalizeGoodsItem(item, detail))
        const derivedGoodsAmount = goodsList.reduce((sum, item) => {
          const subtotal = item.subtotal ?? ((item.price || 0) * (item.count || item.quantity || 1))
          return sum + Number(subtotal || 0)
        }, 0)
        const rawFreight = detail.freight ?? detail.freightAmount ?? detail.freight_amount ?? 0
        const rawDiscount = detail.discountAmount ?? detail.discount_amount ?? 0
        const derivedPayAmount = Math.max(derivedGoodsAmount + Number(rawFreight || 0) - Number(rawDiscount || 0), 0)
        this.orderInfo = {
          ...this.emptyOrder(),
          ...detail,
          orderNo: detail.orderNo || detail.order_no || '',
          goodsAmount: this.normalizeFenAmount(
            detail.goodsAmount ?? detail.goods_amount ?? detail.totalAmount ?? detail.payAmount ?? detail.pay_amount ?? 0,
            derivedGoodsAmount
          ),
          freight: detail.freight ?? detail.freightAmount ?? detail.freight_amount ?? 0,
          discountAmount: detail.discountAmount ?? detail.discount_amount ?? 0,
          payAmount: this.normalizeFenAmount(
            detail.payAmount ?? detail.pay_amount ?? detail.totalAmount ?? detail.goodsAmount ?? 0,
            derivedPayAmount
          ),
          refundAmount: this.normalizeFenAmount(detail.refundAmount ?? detail.refund_amount ?? 0),
          status: detail.status ?? detail.orderStatus ?? detail.paymentStatus ?? '',
          statusText: detail.statusText || detail.status_text || '',
          address: detail.address || null,
          goodsList,
          refundType: detail.refundType ?? detail.refund_type ?? null,
          refundStatus: detail.refundStatus ?? detail.refund_status ?? null,
          refundReason: detail.refundReason ?? detail.refund_reason ?? '',
          refundImages: detail.refundImages ?? detail.refund_images ?? '',
          returnCompanyCode: detail.returnCompanyCode ?? detail.return_company_code ?? '',
          returnCompanyName: detail.returnCompanyName ?? detail.return_company_name ?? '',
          returnTrackingNo: detail.returnTrackingNo ?? detail.return_tracking_no ?? '',
          returnStatus: detail.returnStatus ?? detail.return_status ?? null,
          returnShipTime: detail.returnShipTime ?? detail.return_ship_time ?? '',
          returnReceiveTime: detail.returnReceiveTime ?? detail.return_receive_time ?? ''
        }
        this.updateShareMeta()
      } catch (e) {
        this.orderInfo = this.emptyOrder()
        this.loadError = e?.message || '订单加载失败'
      } finally {
        this.loading = false
      }
    },

    formatMoney(value) {
      return formatYuanNumber(fenToYuan(value))
    },

    normalizeFenAmount(rawValue, derivedValue = 0) {
      const amountFen = Number(rawValue || 0)
      const derivedFen = Number(derivedValue || 0)
      if (amountFen > 0 && derivedFen > 0 && amountFen > derivedFen * 10) {
        return derivedFen
      }
      return amountFen
    },

    formatDateTime(value) {
      if (!value) return '--'
      return String(value).replace('T', ' ')
    },

    getStatusText(status) {
      const map = {
        PENDING_PAYMENT: '待支付',
        PAID: '待发货',
        SHIPPED: '运输中',
        RECEIVED: '已收货',
        COMPLETED: '已完成',
        CANCELLED: '已取消',
        REFUNDING: '退款中',
        REFUNDED: '已退款'
      }
      return map[status] || '未知状态'
    },

    getReturnStatusText(status) {
      const map = {
        1: '已寄回',
        2: '运输中',
        3: '派送中',
        4: '已签收',
        5: '拒收',
        6: '退件'
      }
      return map[Number(status)] || '待回寄'
    },

    copyText(value, title = '复制成功') {
      if (!value) return
      uni.setClipboardData({
        data: String(value),
        success: () => uni.showToast({ title, icon: 'success' })
      })
    },

    copyOrderNo() {
      this.copyText(this.orderInfo.orderNo)
    },

    goGoodsDetail(id) {
      if (id) uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` })
    },

    buildArtworkShareRoute() {
      return this.shareArtworkId
        ? `/pages/gallery/detail?id=${encodeURIComponent(this.shareArtworkId)}&from=order-share`
        : '/pages/gallery/index?from=order-share'
    },

    async shareOrderArtwork() {
      if (!this.shareArtworkId) return
      await shareH5OrCopy({
        title: `${this.shareArtworkTitle}｜艺本艺术`,
        text: '来看看这件艺术作品的详情与流通记录',
        route: this.buildArtworkShareRoute()
      })
    },

    updateShareMeta() {
      if (!this.shareArtworkId) return
      setH5ShareMeta({
        title: `${this.shareArtworkTitle}｜艺本艺术`,
        description: '查看作品详情与流通记录',
        imageUrl: this.summaryGoodsImage,
        url: buildH5ShareUrl(this.buildArtworkShareRoute())
      })
    },

    normalizeGoodsItem(item = {}, detail = {}) {
      const artistName = item.artistName || item.authorName || detail.sellerName || this.orderInfo.sellerName || ''
      const goodsId = item.goodsId || item.artworkId || ''
      const count = item.count || item.quantity || item.num || 1
      const price = item.price ?? item.unitPrice ?? item.unit_price ?? 0
      const subtotal = item.subtotal ?? item.subtotalAmount ?? item.subtotal_amount ?? 0
      return {
        ...item,
        goodsId,
        goodsName: item.goodsName || item.title || item.itemTitle || '作品',
        artistName,
        authorName: item.authorName || artistName,
        goodsImage: item.goodsImage || item.coverImage || '',
        specName: item.specName || '',
        count,
        quantity: count,
        price,
        subtotal: Number(subtotal || 0) > Number(price || 0) * count * 10
          ? Number(price || 0) * count
          : subtotal
      }
    },

    resolveArtistName(item = {}) {
      return item.artistName || item.authorName || this.orderInfo.sellerName || ''
    },

    resolveGoodsImage(item = {}) {
      const image = item.goodsImage || item.coverImage || ''
      if (image) return getFullImageUrl(image)
      return '/static/images/artwork-fallback.png'
    },

    goodsMetaList(item = {}) {
      const fields = [
        item.material || item.artType,
        item.size,
        item.year ? `${item.year}` : ''
      ]
      if (item.specName && !fields.filter(Boolean).length) {
        fields.push(item.specName)
      }
      if (!fields.filter(Boolean).length && item.goodsId) {
        fields.push(`作品编号 #${item.goodsId}`)
      }
      return fields.filter(Boolean)
    },

    contactSeller() {
      uni.navigateTo({ url: `/pages/message/chat?type=seller&orderId=${this.orderId}` })
    },

    viewLogistics() {
      uni.navigateTo({ url: `/pages/order/logistics?id=${this.orderId}` })
    },

    cancelOrder() {
      uni.showModal({
        title: '提示',
        content: '确定要取消该订单吗？',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await cancelOrder(this.orderId)
            uni.showToast({ title: '订单已取消', icon: 'success' })
            this.orderInfo.status = 'CANCELLED'
          } catch (e) {
            uni.showToast({ title: '取消失败', icon: 'none' })
          }
        }
      })
    },

    payOrder() {
      uni.navigateTo({ url: `/pages/order/pay?orderId=${this.orderId}&amount=${fenToYuan(this.orderInfo.payAmount)}` })
    },

    confirmReceive() {
      uni.showModal({
        title: '确认收货',
        content: '请确认您已收到商品且商品完好无损',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await confirmReceive(this.orderId)
            uni.showToast({ title: '已确认收货', icon: 'success' })
            this.orderInfo.status = 'COMPLETED'
          } catch (e) {
            uni.showToast({ title: '确认失败', icon: 'none' })
          }
        }
      })
    },

    applyRefund() {
      uni.navigateTo({ url: `/pages/order/refund?id=${this.orderId}` })
    },

    ensureOtherCompanyOption(companies = []) {
      const normalized = companies
        .filter(item => item?.code && item?.name)
        .map(item => ({ code: item.code, name: item.name }))
      return normalized.some(item => item.code === 'OTHER')
        ? normalized
        : [...normalized, { code: 'OTHER', name: '其他物流' }]
    },

    async loadShipCompanies() {
      if (this.shipCompanies.length > this.defaultShipCompanies().length) return
      this.shipCompanyLoading = true
      try {
        const result = await getLogisticsCompanies()
        const list = Array.isArray(result) ? result : (result?.records || result?.list || [])
        this.shipCompanies = this.ensureOtherCompanyOption(list.length ? list : this.defaultShipCompanies())
      } catch (e) {
        this.shipCompanies = this.defaultShipCompanies()
      } finally {
        this.shipCompanyLoading = false
      }
    },

    openReturnModal() {
      this.returnModalVisible = true
      this.resetReturnShipForm()
      this.loadShipCompanies()
    },

    closeReturnModal() {
      if (this.returnSubmitting) return
      this.returnModalVisible = false
      this.resetReturnShipForm()
    },

    onReturnCompanyChange(event) {
      this.returnShipForm.companyIndex = Number(event.detail.value || 0)
      this.returnShipDetectState = {
        message: this.selectedReturnCompany?.name ? `已选择${this.selectedReturnCompany.name}` : '',
        source: 'manual'
      }
    },

    resetReturnShipForm() {
      this.returnShipForm = this.emptyReturnShipForm()
      this.returnShipDetectState = {
        message: '',
        source: ''
      }
      if (this.returnShipDetectTimer) {
        clearTimeout(this.returnShipDetectTimer)
        this.returnShipDetectTimer = null
      }
    },

    matchTrackingCompany(trackingNo = '') {
      const value = String(trackingNo || '').replace(/\s+/g, '').toUpperCase()
      if (!value || value.length < 6) return ''
      if (value.startsWith('SF') || /^4\d{14,15}$/.test(value)) return 'SF'
      if (/^(YT|DD)\w{8,}$/.test(value)) return 'YTO'
      if (/^(ZTO|ZT)\w{8,}$/.test(value) || /^75\d{11,13}$/.test(value)) return 'ZTO'
      if (/^(STO|ST)\w{8,}$/.test(value) || /^77\d{11,13}$/.test(value)) return 'STO'
      if (/^(YD|YDWL)\w{8,}$/.test(value) || /^43\d{13,15}$/.test(value)) return 'YD'
      if (/^(JT|JTSD)\w{8,}$/.test(value) || /^78\d{11,13}$/.test(value)) return 'JTSD'
      if (/^(EMS|E[A-Z0-9]{9}CN|9\d{11,19})$/.test(value)) return 'EMS'
      if (/^(YZ|YP|POST)\w{8,}$/.test(value)) return 'YZPY'
      return ''
    },

    applyReturnDetectedCompany(companyCode, message, source = 'auto') {
      if (!companyCode) return false
      const index = this.shipCompanies.findIndex(item => item.code === companyCode)
      if (index < 0) return false
      this.returnShipForm.companyIndex = index
      if (companyCode !== 'OTHER') {
        this.returnShipForm.customCompanyName = ''
      }
      this.returnShipDetectState = {
        message: message || `已识别为${this.shipCompanies[index].name}`,
        source
      }
      return true
    },

    async detectReturnTrackingCompany(trackingNo) {
      const value = String(trackingNo || '').replace(/\s+/g, '')
      if (!value || value.length < 6) {
        this.returnShipDetectState = { message: '', source: '' }
        return
      }

      const guessedCode = this.matchTrackingCompany(value)
      if (guessedCode) {
        this.applyReturnDetectedCompany(
          guessedCode,
          `已识别为${this.shipCompanies.find(item => item.code === guessedCode)?.name || '对应物流公司'}`
        )
        return
      }

      try {
        const result = await getLogisticsByTrackingNo(value)
        if (this.applyReturnDetectedCompany(
          result?.companyCode,
          `已匹配历史物流：${result?.companyName || '已识别物流公司'}`,
          'history'
        )) {
          return
        }
      } catch (error) {
        // keep manual selection available
      }

      this.returnShipDetectState = {
        message: '暂未识别物流公司，请手动选择',
        source: 'unknown'
      }
    },

    onReturnTrackingInput(event) {
      const value = event?.detail?.value ?? this.returnShipForm.trackingNo
      this.returnShipForm.trackingNo = value
      if (this.returnShipDetectTimer) {
        clearTimeout(this.returnShipDetectTimer)
      }
      this.returnShipDetectTimer = setTimeout(() => {
        this.detectReturnTrackingCompany(value)
      }, 250)
    },

    validateReturnShipForm() {
      const company = this.selectedReturnCompany
      const trackingNo = this.returnShipForm.trackingNo.replace(/\s+/g, '')
      const customName = this.returnShipForm.customCompanyName.trim()
      if (!company.code) return '请选择物流公司'
      if (company.code === 'OTHER' && !customName) return '请输入物流公司名称'
      if (!trackingNo) return '请输入退货运单号'
      if (!/^[A-Za-z0-9-]{6,32}$/.test(trackingNo)) return '运单号需为6-32位字母、数字或横线'
      return ''
    },

    async submitReturnLogistics() {
      if (this.returnSubmitting) return
      const error = this.validateReturnShipForm()
      if (error) {
        uni.showToast({ title: error, icon: 'none' })
        return
      }
      const company = this.selectedReturnCompany
      const trackingNo = this.returnShipForm.trackingNo.replace(/\s+/g, '')
      const companyName = company.code === 'OTHER'
        ? this.returnShipForm.customCompanyName.trim()
        : company.name
      this.returnSubmitting = true
      try {
        await submitRefundReturnLogistics({
          orderId: this.orderId,
          companyCode: company.code,
          companyName,
          trackingNo
        })
        uni.showToast({ title: '运单已提交', icon: 'success' })
        this.returnModalVisible = false
        await this.loadOrderDetail()
      } catch (e) {
        uni.showToast({ title: e?.message || '提交失败', icon: 'none' })
      } finally {
        this.returnSubmitting = false
      }
    },

    reviewOrder() {
      uni.navigateTo({ url: `/pages/order/review?id=${this.orderId}` })
    },

    getFullImageUrl
  }
}
</script>

<style lang="scss" scoped>
.order-detail {
  min-height: 100vh;
  background: #0d0d0f;
  color: #f5f0e8;
  padding: 20rpx 20rpx 150rpx;
  box-sizing: border-box;
}

.status-banner,
.card,
.state-panel {
  border: 1rpx solid rgba(214, 170, 76, 0.18);
  border-radius: 18rpx;
  background: #171719;
}

.status-banner {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  height: 149rpx;
  padding: 22rpx 28rpx;
  margin-bottom: 20rpx;
  box-sizing: border-box;
  background: linear-gradient(135deg, rgba(212, 158, 45, 0.24), rgba(23, 23, 25, 0.95));
}

.status-label {
  display: block;
  color: #a9a39a;
  font-size: 24rpx;
  margin-bottom: 8rpx;
}

.status-text {
  display: block;
  color: #f2c65e;
  font-size: 38rpx;
  font-weight: 700;
}

.status-no {
  max-width: 320rpx;
  color: #8d877f;
  font-size: 22rpx;
  text-align: right;
  word-break: break-all;
}

.state-panel {
  padding: 42rpx 28rpx;
  text-align: center;
  color: #c8c2b9;
}

.state-panel.error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
}

.retry-btn {
  color: #f2c65e;
}

.card {
  margin-bottom: 20rpx;
  padding: 26rpx;
}

.card-title {
  display: block;
  color: #f5f0e8;
  font-size: 28rpx;
  font-weight: 600;
  margin-bottom: 22rpx;
}

.address-user {
  display: flex;
  gap: 18rpx;
  align-items: center;
  margin-bottom: 12rpx;
}

.user-name {
  font-size: 30rpx;
  font-weight: 600;
}

.user-phone,
.address-detail {
  color: #aaa39a;
  font-size: 25rpx;
}

.goods-header {
  display: flex;
  align-items: center;
  margin-bottom: 18rpx;
}

.shop-logo {
  width: 42rpx;
  height: 42rpx;
  border-radius: 50%;
  margin-right: 12rpx;
}

.shop-name {
  color: #e8dfd0;
  font-size: 26rpx;
}

.goods-item {
  display: flex;
  gap: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.goods-image {
  width: 138rpx;
  height: 138rpx;
  flex: 0 0 138rpx;
  border-radius: 10rpx;
  background: #232326;
}

.goods-info {
  flex: 1;
  min-width: 0;
}

.goods-title {
  display: -webkit-box;
  overflow: hidden;
  color: #f5f0e8;
  font-size: 29rpx;
  line-height: 1.35;
  font-weight: 600;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.goods-artist {
  display: block;
  margin-top: 8rpx;
  color: #c8c0b5;
  font-size: 23rpx;
  line-height: 1.35;
}

.goods-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 10rpx;
}

.goods-meta-item {
  color: #9d958a;
  font-size: 22rpx;
  line-height: 1.3;
}

.goods-meta-item:not(:last-child)::after {
  content: "·";
  margin-left: 8rpx;
  color: rgba(157, 149, 138, 0.65);
}

.goods-spec {
  display: block;
  color: #8e877d;
  font-size: 23rpx;
  margin-top: 8rpx;
}

.goods-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 22rpx;
}

.goods-price {
  color: #f2c65e;
  font-size: 28rpx;
  font-weight: 600;
}

.goods-count {
  color: #8e877d;
  font-size: 24rpx;
}

.info-row,
.amount-row {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  align-items: center;
  min-height: 54rpx;
}

.info-label,
.amount-label {
  color: #8e877d;
  font-size: 25rpx;
}

.info-value,
.amount-value {
  color: #f5f0e8;
  font-size: 25rpx;
  text-align: right;
}

.info-value {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.info-value.remark {
  max-width: 420rpx;
  line-height: 1.45;
}

.copy-btn {
  color: #f2c65e;
  font-size: 22rpx;
  padding: 4rpx 10rpx;
  border: 1rpx solid rgba(242, 198, 94, 0.4);
  border-radius: 999rpx;
}

.refund-card-head,
.return-logistics-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.refund-type-badge {
  flex: 0 0 auto;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(245, 240, 232, 0.72);
  font-size: 22rpx;
}

.refund-type-badge.gold {
  background: rgba(242, 198, 94, 0.14);
  color: #f2c65e;
}

.refund-amount {
  color: #f2c65e;
}

.return-logistics-card {
  margin-top: 20rpx;
  padding: 20rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(242, 198, 94, 0.14);
  background: rgba(242, 198, 94, 0.05);
}

.return-logistics-title,
.return-logistics-tip {
  display: block;
}

.return-logistics-title {
  color: #f5f0e8;
  font-size: 26rpx;
  font-weight: 600;
}

.return-logistics-tip {
  margin-top: 8rpx;
  color: #a9a39a;
  font-size: 22rpx;
  line-height: 1.45;
}

.mini-action-btn {
  flex: 0 0 auto;
  padding: 0 22rpx;
  min-width: 136rpx;
  height: 60rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(242, 198, 94, 0.36);
  color: #f2c65e;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.amount-row.total {
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
  margin-top: 14rpx;
  padding-top: 16rpx;
}

.amount-row.total .amount-value {
  color: #f2c65e;
  font-size: 34rpx;
  font-weight: 700;
}

.amount-value.discount {
  color: #d97979;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 12rpx;
  align-items: center;
  justify-content: flex-end;
  min-height: 110rpx;
  padding: 18rpx 20rpx calc(18rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  background: rgba(14, 14, 16, 0.96);
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.ghost-btn,
.primary-btn {
  min-width: 128rpx;
  height: 70rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 22rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
}

.ghost-btn {
  color: #dfd6c8;
  border: 1rpx solid rgba(255, 255, 255, 0.14);
}

.primary-btn {
  color: #16120b;
  background: #d9aa3d;
  font-weight: 600;
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
.ship-buyer {
  display: block;
}

.ship-goods-name {
  font-size: 28rpx;
  line-height: 1.35;
  font-weight: 700;
  color: #f6f2e8;
}

.ship-order-no,
.ship-buyer {
  font-size: 22rpx;
  line-height: 1.35;
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
.ship-input {
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

.ship-helper {
  font-size: 22rpx;
  line-height: 1.4;
  color: rgba(242, 198, 94, 0.86);
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
  font-size: 28rpx;
  font-weight: 600;
}

.ship-action.secondary {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(246, 242, 232, 0.72);
}

.ship-action.primary {
  background: linear-gradient(135deg, #d6aa4c, #f2c65e);
  color: #1a1610;
}

.ship-action[disabled] {
  opacity: 0.6;
}
</style>
