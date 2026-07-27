const DEFAULT_TITLE = '艺本艺术'
const DEFAULT_DESCRIPTION = '发现艺术，收藏价值'

const normalizeRoute = (route) => {
  const value = String(route || '').trim()
  if (!value) return '/pages/index/index?from=share'
  return value.startsWith('/') ? value : `/${value}`
}

export const buildH5ShareUrl = (route) => {
  const normalizedRoute = normalizeRoute(route)
  if (typeof window === 'undefined' || !window.location?.origin) return normalizedRoute
  return `${window.location.origin}/#${normalizedRoute}`
}

const toAbsoluteUrl = (url) => {
  const value = String(url || '').trim()
  if (!value || typeof window === 'undefined') return value
  try {
    return new URL(value, window.location.origin).toString()
  } catch (error) {
    return value
  }
}

const upsertMeta = (selector, attributes, content) => {
  if (!content || typeof document === 'undefined') return
  let node = document.head.querySelector(selector)
  if (!node) {
    node = document.createElement('meta')
    Object.entries(attributes).forEach(([key, value]) => node.setAttribute(key, value))
    document.head.appendChild(node)
  }
  node.setAttribute('content', content)
}

export const setH5ShareMeta = ({ title, description, imageUrl, url } = {}) => {
  if (typeof document === 'undefined') return
  const safeTitle = String(title || DEFAULT_TITLE).trim()
  const safeDescription = String(description || DEFAULT_DESCRIPTION).trim()
  const safeUrl = toAbsoluteUrl(url || (typeof window !== 'undefined' ? window.location.href : ''))
  const safeImage = toAbsoluteUrl(imageUrl)

  document.title = safeTitle
  upsertMeta('meta[name="description"]', { name: 'description' }, safeDescription)
  upsertMeta('meta[property="og:title"]', { property: 'og:title' }, safeTitle)
  upsertMeta('meta[property="og:description"]', { property: 'og:description' }, safeDescription)
  upsertMeta('meta[property="og:url"]', { property: 'og:url' }, safeUrl)
  upsertMeta('meta[property="og:type"]', { property: 'og:type' }, 'website')
  if (safeImage) upsertMeta('meta[property="og:image"]', { property: 'og:image' }, safeImage)
}

export const shareH5OrCopy = async ({ title, text, route, url } = {}) => {
  const shareUrl = url || buildH5ShareUrl(route)
  const payload = {
    title: String(title || DEFAULT_TITLE),
    text: String(text || DEFAULT_DESCRIPTION),
    url: shareUrl
  }

  if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
    try {
      await navigator.share(payload)
      return { shared: true, url: shareUrl }
    } catch (error) {
      if (error?.name === 'AbortError') return { cancelled: true, url: shareUrl }
    }
  }

  return new Promise((resolve) => {
    uni.setClipboardData({
      data: shareUrl,
      success: () => {
        uni.showToast({ title: '链接已复制，可粘贴分享', icon: 'none' })
        resolve({ copied: true, url: shareUrl })
      },
      fail: () => {
        uni.showToast({ title: '当前环境暂不支持分享', icon: 'none' })
        resolve({ failed: true, url: shareUrl })
      }
    })
  })
}
