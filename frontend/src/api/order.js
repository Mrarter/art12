/**
 * 订单 API
 * 接口: 5.5-5.6节 订单模块
 */
import request from './request'

/**
 * 创建订单(从购物车)
 * POST /orders/create
 * @param {Object} data
 * @param {Array<number>} data.cartIds - 购物车项ID数组
 * @param {number} data.addressId - 地址ID
 * @param {string} data.remark - 备注
 */
export const createOrderFromCart = (data) => {
  return request.post('/orders/create', data)
}

/**
 * 直接购买
 * POST /orders/direct
 * @param {Object} data
 * @param {number} data.productId - 作品ID
 * @param {number} data.quantity - 数量
 * @param {number} data.addressId - 地址ID
 * @param {string} data.remark - 备注
 */
export const directBuy = (data) => {
  return request.post('/orders/direct', data)
}

/**
 * 获取订单列表
 * GET /orders
 * @param {string} status - 订单状态: all/pending/paid/shipped/completed/refund
 * @param {number} page - 页码
 * @param {number} pageSize - 每页数量
 */
export const getOrderList = (params) => {
  return request.get('/orders', params)
}

/**
 * 获取订单详情
 * GET /orders/{id}
 */
export const getOrderDetail = (id) => {
  return request.get(`/orders/${id}`)
}

/**
 * 取消订单
 * PUT /orders/{id}/cancel
 */
export const cancelOrder = (id) => {
  return request.put(`/orders/${id}/cancel`)
}

/**
 * 确认收货
 * PUT /orders/{id}/confirm
 */
export const confirmReceive = (id) => {
  return request.put(`/orders/${id}/confirm`)
}

/**
 * 申请售后/退款
 * POST /orders/{id}/refund
 * @param {Object} data
 * @param {string} data.reason - 退款原因
 * @param {Array<string>} data.images - 凭证图片
 */
export const applyRefund = (id, data) => {
  return request.post(`/orders/${id}/refund`, data)
}

/**
 * 微信支付统一下单
 * POST /pay/unified-order
 * @param {Object} data
 * @param {number} data.orderId - 订单ID
 * @param {string} data.payType - 支付类型: wechat
 */
export const unifiedOrder = (data) => {
  return request.post('/pay/unified-order', data)
}

/**
 * 获取收货地址列表
 * GET /user/addresses
 */
export const getAddressList = () => {
  return request.get('/user/addresses')
}

/**
 * 添加收货地址
 * POST /user/addresses
 */
export const addAddress = (data) => {
  return request.post('/user/addresses', data)
}

/**
 * 更新收货地址
 * PUT /user/addresses/{id}
 */
export const updateAddress = (id, data) => {
  return request.put(`/user/addresses/${id}`, data)
}

/**
 * 删除收货地址
 * DELETE /user/addresses/{id}
 */
export const deleteAddress = (id) => {
  return request.delete(`/user/addresses/${id}`)
}
