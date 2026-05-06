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
  return request({ url: '/cart/list', data: params })
}

export const getAddressList = () => {
  return request({ url: '/user/address/list' })
}

export const createOrderFromCart = (data) => {
  return request({ url: '/order/create', method: 'POST', data })
}

export const directBuy = (data) => {
  return request({ url: '/order/direct', method: 'POST', data })
}

export const getOrderList = (params) => {
  return request({ url: '/order/list', data: params })
}

export const getOrderDetail = (orderNo) => {
  return request({ url: `/order/detail/${orderNo}` })
}

export const getOrderCounts = () => {
  return request({ url: '/order/counts' })
}

export const cancelOrder = (orderId) => {
  return request({ url: `/order/cancel/${orderId}`, method: 'POST' })
}

export const confirmReceive = (orderId) => {
  return request({ url: `/order/confirm/${orderId}`, method: 'POST' })
}

export const refundApply = (data) => {
  return request({ url: '/order/refund', method: 'POST', data })
}

export const submitReview = (data) => {
  return request({ url: '/order/review', method: 'POST', data })
}
