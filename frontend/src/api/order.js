import request from './request'

/**
 * 前端订单/意向单 API
 */
export const createPurchaseIntent = (data) => {
  return request({
    url: '/order/intent/create',
    method: 'POST',
    data
  })
}

export const getIntentDetail = (id) => {
  return request({
    url: `/order/intent/${id}`
  })
}

export const getCertDetail = (id) => {
  return request({
    url: `/order/certificate/${id}`
  })
}

export const getCirculationDetail = (artworkId) => {
  return request({
    url: `/order/circulation/${artworkId}`
  })
}

/**
 * 转售购买 - 创建转售订单
 * POST /order/orders/resale
 */
export const createResaleOrder = (data) => {
  return request({ url: '/order/orders/resale', method: 'POST', data, requireAuth: true })
}

// ===== 以下为 confirm.vue 等已有页面依赖的 API =====

export const getCartList = (params) => {
  return request({ url: '/order/cart/list', data: params })
}

export const getAddressList = () => {
  return request({ url: '/order/user/addresses' })
}

export const createOrderFromCart = (data) => {
  return request({ url: '/order/orders/create', method: 'POST', data })
}

export const directBuy = (data) => {
  return request({ url: '/order/orders/direct', method: 'POST', data, requireAuth: true })
}

export const getOrderList = (params) => {
  return request({ url: '/order/orders', data: params })
}

export const getSoldOrderList = (params) => {
  return request({ url: '/order/orders', data: { ...params, type: 'sold' } })
}

export const getOrderDetail = (orderId) => {
  return request({ url: `/order/orders/${orderId}` })
}

export const getOrderLogistics = (orderId) => {
  return request({ url: `/order/logistics/order/${orderId}` })
}

export const getLogisticsByTrackingNo = (trackingNo) => {
  return request({ url: `/order/logistics/tracking/${trackingNo}` })
}

export const getLogisticsCompanies = () => {
  return request({ url: '/order/logistics/companies' })
}

export const shipOrder = (data) => {
  return request({ url: '/order/logistics/ship', method: 'POST', data })
}

export const getOrderCounts = () => {
  const defaults = {
    pending: 0,
    pendingPayment: 0,
    paid: 0,
    shipped: 0,
    completed: 0,
    received: 0,
    review: 0
  }
  return getOrderList({ page: 1, pageSize: 1 })
    .then((result) => result?.counts || {
      ...defaults,
      pendingPayment: result?.pendingPayCount || 0
    })
    .catch((error) => {
      console.warn('获取订单数量失败，使用默认统计', error)
      return defaults
    })
}

export const cancelOrder = (orderId) => {
  return request({ url: `/order/orders/${orderId}/cancel`, method: 'PUT' })
}

export const confirmReceive = (orderId) => {
  return request({ url: `/order/orders/${orderId}/confirm`, method: 'PUT' })
}

export const refundApply = (data) => {
  return request({ url: `/order/orders/${data.orderId}/refund`, method: 'POST', data })
}

export const submitRefundReturnLogistics = (data) => {
  return request({
    url: `/order/orders/${data.orderId}/refund-return-logistics`,
    method: 'POST',
    data
  })
}

export const submitReview = (data) => {
  return request({ url: '/order/review', method: 'POST', data })
}

// ===== 微信支付 API =====

/**
 * 获取JSAPI支付参数（小程序调起支付）
 * @param {number} orderId - 订单ID
 * @param {string} openId - 用户openId
 * @param {string} payScene - 支付场景 mini/h5
 */
export const getJsApiPayParams = (orderId, openId, payScene = 'mini') => {
  return request({
    url: '/order/pay/jsapi-params',
    method: 'POST',
    data: { orderId, openId, payScene }
  })
}

/**
 * 创建支付宝手机网站支付
 * @param {number} orderId - 订单ID
 * @param {object} options - 支付场景等附加参数
 */
export const createAlipayWapPay = (orderId, options = {}) => {
  return request({
    url: '/order/pay/alipay/wap',
    method: 'POST',
    data: { orderId, ...options }
  })
}

/**
 * 创建支付宝 App 支付
 * @param {number} orderId - 订单ID
 */
export const createAlipayAppPay = (orderId) => {
  return request({
    url: '/order/pay/alipay/app',
    method: 'POST',
    data: { orderId }
  })
}

/**
 * 查询支付状态
 * @param {number} orderId - 订单ID
 */
export const queryPayStatus = (orderId) => {
  return request({ url: `/order/pay/query/${orderId}` })
}

/**
 * 本地开发模拟支付成功
 * @param {number} orderId - 订单ID
 */
export const mockPaySuccess = (orderId) => {
  return request({ url: `/order/pay/mock-success/${orderId}`, method: 'POST' })
}
