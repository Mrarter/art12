/**
 * 订单相关 API
 */
import { get, post, put, del } from './request'

/**
 * 获取购物车列表
 */
export function getCartList() {
  return get('/order/cart/list')
}

/**
 * 添加到购物车
 */
export function addToCart(artworkId, quantity = 1) {
  return post('/order/cart/add', { artworkId, quantity })
}

/**
 * 从购物车移除
 */
export function removeFromCart(cartIds) {
  return post('/order/cart/remove', { cartIds })
}

/**
 * 创建订单（从购物车）
 */
export function createOrder(data) {
  return post('/order/create', data)
}

/**
 * 直接购买
 */
export function directBuy(data) {
  return post('/order/direct', data)
}

/**
 * 获取订单列表
 */
export function getOrderList(status, page = 1, pageSize = 20) {
  return get('/order/list', { status, page, pageSize })
}

/**
 * 获取订单详情
 */
export function getOrderDetail(orderId) {
  return get('/order/detail/' + orderId)
}

/**
 * 取消订单
 */
export function cancelOrder(orderId) {
  return put('/order/cancel/' + orderId)
}

/**
 * 确认收货
 */
export function confirmReceive(orderId) {
  return put('/order/confirm/' + orderId)
}

/**
 * 微信支付统一下单
 */
export function unifiedOrder(orderId) {
  return post('/order/pay/unified-order', { orderId })
}

/**
 * 获取收货地址列表
 */
export function getAddressList() {
  return get('/order/address/list')
}

/**
 * 添加收货地址
 */
export function addAddress(data) {
  return post('/order/address/add', data)
}

/**
 * 删除收货地址
 */
export function deleteAddress(addressId) {
  return del('/order/address/delete/' + addressId)
}

/**
 * 设置默认地址
 */
export function setDefaultAddress(addressId) {
  return put('/order/address/default/' + addressId)
}
