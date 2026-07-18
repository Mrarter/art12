export const IS_MP_WEIXIN = process.env.UNI_PLATFORM === 'mp-weixin'
export const IS_APP_BUILD = process.env.UNI_PLATFORM === 'app-plus' || process.env.UNI_PLATFORM === 'app'
export const AUCTION_ENABLED = !IS_MP_WEIXIN
export const COMMUNITY_POST_PUBLISH_ENABLED = !IS_MP_WEIXIN
export const AUCTION_DISABLED_MESSAGE = '小程序版暂不开放拍卖功能'
export const COMMUNITY_POST_DISABLED_MESSAGE = '小程序版暂不开放发帖功能'

export function isAppRuntime() {
  if (isYibenIOSAppShell()) return true
  if (IS_APP_BUILD) return true
  try {
    const info = typeof uni !== 'undefined' && typeof uni.getSystemInfoSync === 'function'
      ? uni.getSystemInfoSync()
      : {}
    if (String(info.uniPlatform || '').toLowerCase() === 'app') return true
  } catch (e) {
    // ignore runtime probing failures
  }
  if (typeof window === 'undefined') return false
  const ua = window.navigator?.userAgent || ''
  return !!window.plus || /Html5Plus|DCloud|uni-app/i.test(ua)
}

export function isYibenIOSAppShell() {
  if (typeof window === 'undefined') return false
  const ua = window.navigator?.userAgent || ''
  return /YibenArtIOSApp/i.test(ua)
}

export function getAlipayReturnScene() {
  return isYibenIOSAppShell() ? 'ios-app' : 'h5'
}

export function isMobileRuntime() {
  if (typeof window === 'undefined') return false
  const ua = window.navigator?.userAgent || ''
  return /Android|iPhone|iPad|iPod|Mobile/i.test(ua)
}

export function isProductionShiyijuHost() {
  if (typeof window === 'undefined') return false
  const host = window.location?.hostname || ''
  return host === 'art1.cn'
    || host === 'www.art1.cn'
    || host.endsWith('.art1.cn')
}

export function isAppWebViewFallback() {
  if (isAppRuntime() || isH5WechatRuntime() || IS_MP_WEIXIN) return false
  // Some packaged WebViews do not expose plus/uniPlatform to H5.
  // On the production mobile shell we still allow WeChat and let the pay bridge decide.
  return isProductionShiyijuHost() && isMobileRuntime()
}

export function isH5WechatRuntime() {
  if (IS_MP_WEIXIN) return false
  if (typeof window === 'undefined') return false
  const ua = window.navigator?.userAgent || ''
  return /MicroMessenger/i.test(ua) && !/miniProgram/i.test(ua)
}

export function isWechatPayRuntimeSupported() {
  return IS_MP_WEIXIN || isH5WechatRuntime() || isAppRuntime() || isAppWebViewFallback()
}

export function getWechatPayScene() {
  if (isAppRuntime() || isAppWebViewFallback()) return 'app'
  if (IS_MP_WEIXIN) return 'mini'
  return 'h5'
}

export function isAuctionPath(path = '') {
  const normalizedPath = String(path || '').split('?')[0]
  return normalizedPath === '/pages/auction/index' || normalizedPath.startsWith('/pages/auction/')
}

export function isAuctionLinkType(linkType = '') {
  return String(linkType || '').toLowerCase() === 'auction'
}

export function showAuctionDisabledToast(message = AUCTION_DISABLED_MESSAGE) {
  if (typeof uni === 'undefined' || typeof uni.showToast !== 'function') return
  uni.showToast({
    title: message,
    icon: 'none',
    duration: 2200
  })
}

export function showCommunityPostDisabledToast(message = COMMUNITY_POST_DISABLED_MESSAGE) {
  if (typeof uni === 'undefined' || typeof uni.showToast !== 'function') return
  uni.showToast({
    title: message,
    icon: 'none',
    duration: 2200
  })
}

export function guardAuctionAccess(options = {}) {
  if (AUCTION_ENABLED || typeof uni === 'undefined') return false

  const {
    redirect = '/pages/index/index',
    useReLaunch = false,
    delay = 160
  } = options

  showAuctionDisabledToast()

  setTimeout(() => {
    const method = useReLaunch ? 'reLaunch' : 'switchTab'
    if (typeof uni[method] === 'function') {
      uni[method]({ url: redirect })
    }
  }, delay)

  return true
}

export function guardCommunityPostPublishAccess(options = {}) {
  if (COMMUNITY_POST_PUBLISH_ENABLED || typeof uni === 'undefined') return false

  const {
    redirect = '/pages/artcircle/index',
    useReLaunch = false,
    delay = 160
  } = options

  showCommunityPostDisabledToast()

  setTimeout(() => {
    const method = useReLaunch ? 'reLaunch' : 'redirectTo'
    if (typeof uni[method] === 'function') {
      uni[method]({ url: redirect })
    }
  }, delay)

  return true
}
