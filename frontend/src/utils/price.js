/**
 * 价格格式化工具
 * 价格以分为单位存储，显示时转换为元，精确到个位数
 */

/**
 * 格式化价格（分 → 元，精确到个位数，带千分位）
 * @param {number|string} price - 价格（分）
 * @param {boolean} withSymbol - 是否带 ¥ 符号，默认 true
 * @returns {string} 格式化后的价格字符串
 */
export function formatPrice(price, withSymbol = true) {
  if (price === null || price === undefined || price === '' || isNaN(price)) {
    return withSymbol ? '¥0' : '0'
  }
  const yuan = Math.round(Number(price) / 100)
  const formatted = yuan.toLocaleString()
  return withSymbol ? `¥${formatted}` : formatted
}

/**
 * 格式化价格（纯数字，不带符号）
 * @param {number|string} price - 价格（分）
 * @returns {string} 格式化后的价格数字
 */
export function formatPriceNumber(price) {
  if (price === null || price === undefined || price === '' || isNaN(price)) {
    return '0'
  }
  const yuan = Math.round(Number(price) / 100)
  return yuan.toLocaleString()
}

/**
 * 格式化元金额（已为元单位的金额）
 * @param {number|string} amount - 金额（元）
 * @param {boolean} withSymbol - 是否带 ¥ 符号，默认 true
 * @returns {string} 格式化后的金额字符串
 */
export function formatYuanAmount(amount, withSymbol = true) {
  if (amount === null || amount === undefined || amount === '' || isNaN(amount)) {
    return withSymbol ? '¥0' : '0'
  }
  const yuan = Math.round(Number(amount))
  const formatted = yuan.toLocaleString()
  return withSymbol ? `¥${formatted}` : formatted
}

export default {
  formatPrice,
  formatPriceNumber,
  formatYuanAmount
}
