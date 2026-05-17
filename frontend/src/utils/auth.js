/**
 * =====================================================
 *  Token 管理模块 - utils/auth.js
 *  解决登录状态频繁过期问题
 * =====================================================
 *
 * 功能清单：
 *  1. Token 结构化存储（包含过期时间）
 *  2. Token 有效性检测
 *  3. 无感刷新机制（即将过期自动续期）
 *  4. 智能重定向（保存当前路由，登录后恢复）
 *  5. 并发刷新防抖（避免多个请求同时刷新）
 *  6. 游客模式识别
 */

import { refreshToken } from '@/api/user'

// ==================== 常量配置 ====================

const TOKEN_KEY = 'auth_token'
const USER_KEY = 'userInfo'
const REDIRECT_KEY = 'login_redirect'
const TOKEN_EXPIRE_BUFFER = 5 * 60 * 1000  // 提前 5 分钟刷新
const REFRESH_COOLDOWN = 60 * 1000        // 刷新冷却 60 秒

// ==================== Token 存储结构 ====================

/**
 * Token 存储格式
 * {
 *   accessToken: string,      // 访问令牌
 *   expiresAt: number,         // 过期时间戳 (ms)
 *   tokenType: 'bearer',      // 令牌类型
 *   userId: string,           // 用户ID
 *   isGuest: boolean          // 是否游客
 * }
 */

// ==================== Token 存储操作 ====================

/**
 * 获取完整的 Token 对象
 */
export function getTokenData() {
  try {
    const raw = uni.getStorageSync(TOKEN_KEY)
    if (!raw) return null

    // 兼容旧格式（直接存字符串）
    if (typeof raw === 'string') {
      return {
        accessToken: raw,
        expiresAt: 0,  // 未知过期时间，假设即将过期
        tokenType: 'bearer',
        isGuest: false
      }
    }

    return raw
  } catch (e) {
    console.error('[Auth] 读取 Token 失败:', e)
    return null
  }
}

/**
 * 保存 Token 数据
 */
export function setTokenData(data) {
  try {
    uni.setStorageSync(TOKEN_KEY, {
      accessToken: data.accessToken,
      expiresAt: data.expiresAt || 0,
      tokenType: data.tokenType || 'bearer',
      userId: data.userId || '',
      isGuest: data.isGuest || false,
      _savedAt: Date.now()  // 用于调试
    })
    return true
  } catch (e) {
    console.error('[Auth] 保存 Token 失败:', e)
    return false
  }
}

/**
 * 清除 Token
 */
export function clearTokenData() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}

// ==================== Token 有效性判断 ====================

/**
 * 检查 Token 是否有效
 * @returns {object} { valid: boolean, reason: string, isGuest: boolean }
 */
export function checkTokenValid() {
  const tokenData = getTokenData()

  if (!tokenData) {
    return { valid: false, reason: 'no_token', isGuest: false }
  }

  // 游客 token 视为有效（但不执行刷新）
  if (tokenData.isGuest || tokenData.accessToken === 'guest_token') {
    return { valid: true, reason: 'guest', isGuest: true }
  }

  if (!tokenData.accessToken) {
    return { valid: false, reason: 'empty_token', isGuest: false }
  }

  // 检查过期时间
  if (tokenData.expiresAt > 0) {
    const now = Date.now()
    if (tokenData.expiresAt <= now) {
      return { valid: false, reason: 'expired', isGuest: false }
    }
    // 即将过期（提前 buffer 时间）
    if (tokenData.expiresAt - now < TOKEN_EXPIRE_BUFFER) {
      return { valid: true, reason: 'about_to_expire', isGuest: false, expiresAt: tokenData.expiresAt }
    }
  }

  return { valid: true, reason: 'ok', isGuest: false, expiresAt: tokenData.expiresAt }
}

/**
 * 获取有效的 Access Token
 */
export function getAccessToken() {
  const tokenData = getTokenData()
  return tokenData?.accessToken || ''
}

/**
 * 获取用户信息
 */
export function getUserInfo() {
  try {
    return uni.getStorageSync(USER_KEY) || null
  } catch (e) {
    return null
  }
}

/**
 * 检查是否为游客
 */
export function isGuestUser() {
  const tokenData = getTokenData()
  return tokenData?.isGuest || tokenData?.accessToken === 'guest_token'
}

// ==================== 刷新 Token 机制 ====================

let isRefreshing = false
let refreshSubscribers = []
let lastRefreshTime = 0

/**
 * 添加到刷新等待队列
 */
function subscribeTokenRefresh(callback) {
  refreshSubscribers.push(callback)
}

/**
 * 通知所有等待者
 */
function onTokenRefreshed(newToken) {
  refreshSubscribers.forEach(callback => callback(newToken))
  refreshSubscribers = []
}

/**
 * 执行 Token 刷新
 * 使用单例模式 + 防抖，避免并发刷新
 */
export async function executeTokenRefresh() {
  const now = Date.now()

  // 冷却期内直接返回失败
  if (now - lastRefreshTime < REFRESH_COOLDOWN && lastRefreshTime > 0) {
    console.log('[Auth] Token 刷新冷却中，等待...')
    return null
  }

  // 已有刷新在进行，等待结果
  if (isRefreshing) {
    return new Promise((resolve) => {
      subscribeTokenRefresh((newToken) => {
        resolve(newToken)
      })
    })
  }

  isRefreshing = true
  lastRefreshTime = now

  try {
    console.log('[Auth] 开始刷新 Token...')
    const result = await refreshToken()

    if (result?.token) {
      // 假设新 token 有效期为 7 天（根据后端实际配置调整）
      const expiresAt = Date.now() + 7 * 24 * 60 * 60 * 1000

      setTokenData({
        accessToken: result.token,
        expiresAt,
        userId: result.userId || getUserInfo()?.id || '',
        isGuest: false
      })

      console.log('[Auth] Token 刷新成功')
      onTokenRefreshed(result.token)
      isRefreshing = false
      return result.token
    }

    throw new Error('刷新返回数据异常')
  } catch (e) {
    console.error('[Auth] Token 刷新失败:', e)
    isRefreshing = false
    onTokenRefreshed(null)
    return null
  }
}

/**
 * 确保 Token 有效（如果即将过期则自动刷新）
 * @returns {Promise<string|null>} 有效 token 或 null
 */
export async function ensureValidToken() {
  const check = checkTokenValid()

  // 无 token 或已过期
  if (!check.valid && check.reason !== 'guest') {
    return null
  }

  // 游客直接返回
  if (check.isGuest) {
    return getAccessToken()
  }

  // 即将过期，触发刷新
  if (check.reason === 'about_to_expire') {
    console.log('[Auth] Token 即将过期，触发自动刷新')
    const newToken = await executeTokenRefresh()
    return newToken || getAccessToken()  // 刷新失败也返回原 token
  }

  return getAccessToken()
}

// ==================== 路由恢复机制 ====================

/**
 * 保存登录后需要跳转的页面
 */
export function saveRedirectUrl(url) {
  if (!url || url.includes('/pages/login')) return

  // 避免循环重定向
  if (url.includes('/pages/index') && !url.includes('redirect')) return

  try {
    uni.setStorageSync(REDIRECT_KEY, {
      url,
      timestamp: Date.now()
    })
    console.log('[Auth] 保存重定向页面:', url)
  } catch (e) {
    console.error('[Auth] 保存重定向 URL 失败:', e)
  }
}

/**
 * 获取并清除保存的重定向页面
 */
export function getAndClearRedirectUrl(defaultUrl = '/pages/index/index') {
  try {
    const redirect = uni.getStorageSync(REDIRECT_KEY)
    uni.removeStorageSync(REDIRECT_KEY)

    if (redirect?.url) {
      // 5 分钟内有效
      if (Date.now() - redirect.timestamp < 5 * 60 * 1000) {
        console.log('[Auth] 恢复重定向页面:', redirect.url)
        return redirect.url
      }
    }
  } catch (e) {
    // ignore
  }

  return defaultUrl
}

/**
 * 获取当前页面路径（用于登录后恢复）
 */
export function getCurrentPagePath() {
  const pages = getCurrentPages()
  if (!pages.length) return ''

  const currentPage = pages[pages.length - 1]
  const { route, options } = currentPage

  let path = `/${route}`
  const queryParts = []

  for (const key in options) {
    if (key && options[key] !== undefined && key !== 'scene') {
      queryParts.push(`${key}=${encodeURIComponent(options[key])}`)
    }
  }

  if (queryParts.length) {
    path += '?' + queryParts.join('&')
  }

  return path
}

// ==================== 登录状态管理 ====================

/**
 * 处理登录成功
 */
export function handleLoginSuccess(token, userInfo, isGuest = false) {
  // 假设 token 有效期 7 天
  const expiresAt = Date.now() + 7 * 24 * 60 * 60 * 1000

  setTokenData({
    accessToken: token,
    expiresAt,
    userId: userInfo?.id || userInfo?.userId || '',
    isGuest
  })

  if (userInfo) {
    uni.setStorageSync(USER_KEY, userInfo)
  }
}

/**
 * 处理登录过期/失效
 */
export function handleAuthFailure(redirectToLogin = true) {
  const currentPath = getCurrentPagePath()
  clearTokenData()

  // 保存当前页面，登录后恢复
  if (redirectToLogin && currentPath) {
    saveRedirectUrl(currentPath)
  }

  return currentPath
}

/**
 * 处理退出登录
 */
export function handleLogout() {
  clearTokenData()
  uni.removeStorageSync(REDIRECT_KEY)
}

// ==================== 导出快捷方法 ====================

export default {
  // Token 操作
  getTokenData,
  setTokenData,
  clearTokenData,
  getAccessToken,
  getUserInfo,
  isGuestUser,
  checkTokenValid,
  ensureValidToken,
  executeTokenRefresh,

  // 路由恢复
  saveRedirectUrl,
  getAndClearRedirectUrl,
  getCurrentPagePath,

  // 登录状态
  handleLoginSuccess,
  handleAuthFailure,
  handleLogout
}
