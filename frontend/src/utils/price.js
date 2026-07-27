/**
 * 价格格式化工具
 * 交易/支付金额仍按“分”处理；作品接口金额按“元”处理。
 */

/**
 * 格式化价格（分 → 元，保留 2 位小数，带千分位）
 * @param {number|string} price - 价格（分）
 * @param {boolean} withSymbol - 是否带 ¥ 符号，默认 true
 * @returns {string} 格式化后的价格字符串
 */
export function formatPrice(price, withSymbol = true) {
  if (price === null || price === undefined || price === '' || isNaN(price)) {
    return withSymbol ? '¥0.00' : '0.00'
  }
  const yuan = fenToYuan(price)
  const formatted = formatYuanNumber(yuan)
  return withSymbol ? `¥${formatted}` : formatted
}

/**
 * 格式化价格（纯数字，不带符号）
 * @param {number|string} price - 价格（分）
 * @returns {string} 格式化后的价格数字
 */
export function formatPriceNumber(price) {
  if (price === null || price === undefined || price === '' || isNaN(price)) {
    return '0.00'
  }
  return formatYuanNumber(fenToYuan(price))
}

/**
 * 分金额转元金额。
 * @param {number|string} price - 价格（分）
 * @returns {number} 元金额
 */
export function fenToYuan(price) {
  if (price === null || price === undefined || price === '' || isNaN(price)) return 0
  const num = Number(price)
  return Number.isFinite(num) ? num / 100 : 0
}

/**
 * 元金额格式化。
 * @param {number|string} amount - 金额（元）
 * @returns {string} 格式化后的金额数字
 */
export function formatYuanNumber(amount) {
  if (amount === null || amount === undefined || amount === '' || isNaN(amount)) return '0.00'
  return Number(amount).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/**
 * 格式化作品金额数字。作品接口金额已为元单位，不再除以 100。
 * @param {number|string} amount - 作品金额（元）
 * @returns {string} 格式化后的金额数字
 */
export function formatArtworkPriceNumber(amount) {
  return formatYuanNumber(amount)
}

/**
 * 格式化作品金额。作品接口金额已为元单位，不再除以 100。
 * @param {number|string} amount - 作品金额（元）
 * @param {boolean} withSymbol - 是否带 ¥ 符号，默认 true
 * @returns {string} 格式化后的金额字符串
 */
export function formatArtworkPrice(amount, withSymbol = true) {
  const formatted = formatArtworkPriceNumber(amount)
  return withSymbol ? `¥${formatted}` : formatted
}

/**
 * 作品接口优先展示 currentPrice；入参按“元”处理。
 * @param {Object} item - 作品对象
 * @returns {number} 价格（元）
 */
export function getArtworkDisplayPriceYuan(item = {}) {
  const listing = item.activeResaleListing || item.resaleListing
  const resaleStatus = String(listing?.status || '').toLowerCase()
  const resalePrice = Number(listing?.resalePrice || 0)
  if (resalePrice > 0 && (!resaleStatus || resaleStatus === 'pending')) {
    return resalePrice
  }
  const currentPrice = Number(item.currentPrice || item.current_price || item.displayPrice || 0)
  if (currentPrice > 0) return currentPrice
  return Number(item.price || 0)
}

/**
 * 订单创建等老链路仍需要“分”单位，统一在边界处转换。
 * @param {Object} item - 作品对象
 * @returns {number} 价格（分）
 */
export function getArtworkDisplayPriceFen(item = {}) {
  return Math.round(getArtworkDisplayPriceYuan(item) * 100)
}

/**
 * 格式化元金额（已为元单位的金额）
 * @param {number|string} amount - 金额（元）
 * @param {boolean} withSymbol - 是否带 ¥ 符号，默认 true
 * @returns {string} 格式化后的金额字符串
 */
export function formatYuanAmount(amount, withSymbol = true) {
  if (amount === null || amount === undefined || amount === '' || isNaN(amount)) {
    return withSymbol ? '¥0.00' : '0.00'
  }
  const formatted = formatYuanNumber(amount)
  return withSymbol ? `¥${formatted}` : formatted
}

export default {
  formatPrice,
  formatPriceNumber,
  fenToYuan,
  formatYuanNumber,
  formatArtworkPriceNumber,
  formatArtworkPrice,
  getArtworkDisplayPriceYuan,
  getArtworkDisplayPriceFen,
  formatYuanAmount
}
