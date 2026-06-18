/**
 * 图片URL处理工具
 * 自动处理相对路径和绝对路径
 */

// 图片CDN基础URL
const IMAGE_BASE_URL = import.meta.env?.VITE_IMAGE_BASE_URL || 'https://shiyiju.online'

/**
 * 获取完整的图片URL
 * @param {string} url - 图片URL（相对路径或绝对路径）
 * @param {string} defaultUrl - 默认图片URL
 * @returns {string} 完整的图片URL
 */
export const getFullImageUrl = (url, defaultUrl = '/static/images/placeholder.png') => {
  if (!url) {
    return defaultUrl
  }

  const value = String(url).trim()
  const missingArtworkPlaceholders = [
    '/images/default-artwork.png',
    'images/default-artwork.png',
    '/static/icons/artwork-default.png'
  ]

  if (missingArtworkPlaceholders.includes(value)) {
    return defaultUrl
  }
  
  // 如果已经是完整URL（以 http:// 或 https:// 开头），直接返回
  if (value.startsWith('http://') || value.startsWith('https://')) {
    return value
  }
  
  // 如果是本地静态资源（以 / 开头），直接返回
  if (value.startsWith('/static/') || value.startsWith('/')) {
    return value
  }
  
  // 相对路径，拼接CDN基础URL
  return IMAGE_BASE_URL + '/' + value
}

/**
 * 批量处理图片URL
 * @param {Array} items - 包含图片URL的对象数组
 * @param {string} imageField - 图片字段名
 * @param {string} defaultUrl - 默认图片URL
 * @returns {Array} 处理后的数组
 */
export const processImageUrls = (items, imageField = 'coverImage', defaultUrl = '/static/icons/artwork-default.png') => {
  if (!Array.isArray(items)) {
    return items
  }
  
  return items.map(item => {
    if (item && item[imageField]) {
      return {
        ...item,
        [imageField]: getFullImageUrl(item[imageField], defaultUrl)
      }
    }
    return item
  })
}

export default {
  getFullImageUrl,
  processImageUrls
}
