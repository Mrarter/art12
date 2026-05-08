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
  return request({ url: '/order/orders/direct', method: 'POST', data })
}

export const getOrderList = (params) => {
  return request({ url: '/order/orders', data: params })
}

export const getOrderDetail = (orderNo) => {
  return request({ url: `/order/orders/${orderNo}` })
}

export const getOrderCounts = () => {
  return getOrderList({ page: 1, pageSize: 1 }).then((result) => result?.counts || {
    pendingPayment: result?.pendingPayCount || 0,
    paid: 0,
    shipped: 0,
    completed: 0
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

export const submitReview = (data) => {
  return request({ url: '/order/review', method: 'POST', data })
}
