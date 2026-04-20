/**
 * 订单 API
 * 接口: 5.5-5.6节 订单模块
 * 注意: 网关路由 /api/order/** -> shiyiju-order
 */
import request from './request'

/**
 * 创建订单(从购物车)
 * POST /order/orders/create
 * @param {Object} data
 * @param {Array<number>} data.cartIds - 购物车项ID数组
 * @param {number} data.addressId - 地址ID
 * @param {string} data.remark - 备注
 */
export const createOrderFromCart = (data) => {
  return request.post('/order/orders/create', data)
}

/**
 * 直接购买
 * POST /order/orders/direct
 * @param {Object} data
 * @param {number} data.productId - 作品ID
 * @param {number} data.quantity - 数量
 * @param {number} data.addressId - 地址ID
 * @param {string} data.remark - 备注
 */
export const directBuy = (data) => {
  return request.post('/order/orders/direct', data)
}

/**
 * 获取订单列表
 * GET /order/orders
 * @param {string} status - 订单状态: all/pending/paid/shipped/completed/refund
 * @param {number} page - 页码
 * @param {number} pageSize - 每页数量
 */
export const getOrderList = (params) => {
  return request.get('/order/orders', params)
}

/**
 * 获取订单详情
 * GET /order/orders/{id}
 */
export const getOrderDetail = (id) => {
  return request.get(`/order/orders/${id}`)
}

/**
 * 取消订单
 * PUT /order/orders/{id}/cancel
 */
export const cancelOrder = (id) => {
  return request.put(`/order/orders/${id}/cancel`)
}

/**
 * 确认收货
 * PUT /order/orders/{id}/confirm
 */
export const confirmReceive = (id) => {
  return request.put(`/order/orders/${id}/confirm`)
}

/**
 * 申请售后/退款
 * POST /order/orders/{id}/refund
 * @param {Object} data
 * @param {string} data.reason - 退款原因
 * @param {Array<string>} data.images - 凭证图片
 */
export const applyRefund = (id, data) => {
  return request.post(`/order/orders/${id}/refund`, data)
}

/**
 * 微信支付统一下单
 * POST /order/pay/unified-order
 * @param {Object} data
 * @param {number} data.orderId - 订单ID
 * @param {string} data.payType - 支付类型: wechat
 */
export const unifiedOrder = (data) => {
  return request.post('/order/pay/unified-order', data)
}

/**
 * 获取收货地址列表
 * GET /order/user/addresses
 */
export const getAddressList = () => {
  return request.get('/order/user/addresses')
}

/**
 * 添加收货地址
 * POST /order/user/addresses
 */
export const addAddress = (data) => {
  return request.post('/order/user/addresses', data)
}

/**
 * 更新收货地址
 * PUT /order/user/addresses/{id}
 */
export const updateAddress = (id, data) => {
  return request.put(`/order/user/addresses/${id}`, data)
}

/**
 * 删除收货地址
 * DELETE /order/user/addresses/{id}
 */
export const deleteAddress = (id) => {
  return request.delete(`/order/user/addresses/${id}`)
}

// 导入购物车相关函数
export { getCartList } from './cart'
