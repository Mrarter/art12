/**
 * 购物车 API
 * 接口: 5.4节 购物车模块
 */
import request from './request'

/**
 * 获取购物车列表
 * GET /cart/list
 */
export const getCartList = () => {
  return request.get('/cart/list')
}

/**
 * 添加到购物车
 * POST /cart/add
 * @param {number} artworkId - 作品ID
 * @param {number} quantity - 数量(默认1)
 */
export const addToCart = (artworkId, quantity = 1) => {
  return request.post('/cart/add', { artworkId, quantity })
}

/**
 * 更新购物车数量
 * PUT /cart/update
 * @param {number} id - 购物车项ID
 * @param {number} quantity - 数量
 */
export const updateCartQuantity = (id, quantity) => {
  return request.put('/cart/update', { id, quantity })
}

/**
 * 删除购物车项
 * DELETE /cart/delete
 * @param {Array<number>} ids - 购物车项ID数组
 */
export const deleteCartItems = (ids) => {
  return request.delete('/cart/delete', { ids })
}

/**
 * 锁定购物车项(结算前)
 * POST /cart/lock
 * @param {Array<number>} ids - 购物车项ID数组
 */
export const lockCartItems = (ids) => {
  return request.post('/cart/lock', { ids })
}

/**
 * 解锁购物车项
 * POST /cart/unlock
 * @param {Array<number>} ids - 购物车项ID数组
 */
export const unlockCartItems = (ids) => {
  return request.post('/cart/unlock', { ids })
}

// 更新购物车数量（别名）
export const updateCartNum = (id, quantity) => {
  return updateCartQuantity(id, quantity)
}

// 从购物车删除（别名）
export const removeFromCart = (ids) => {
  return deleteCartItems(ids)
}
