/**
 * =====================================================
 *  Token 管理模块 - utils/auth.js
 *  解决登录状态频繁过期问题 v2.0
 * =====================================================
 *
 * 核心修复：
 *  1. refresh 401时立即清除冷却标记，不再等待
 *  2. 防止重定向URL重复保存
 *  3. 增强刷新状态机，防止死循环
 *  4. 明确区分"可刷新"和"不可刷新"的401
 */

import { refreshToken } from '@/api/user'

// ==================== 常量配置 ====================

const TOKEN_KEY = 'auth_token'
const USER_KEY = 'userInfo'
const REDIRECT_KEY = 'login_redirect'
const TOKEN_EXPIRE_BUFFER = 5 * 60 * 1000  // 提前 5 分钟刷新
const REFRESH_COOLDOWN = 30 * 1000         // 刷新冷却 30 秒
const REDIRECT_MAX_AGE = 5 * 60 * 1000    // 重定向URL有效期 5 分钟

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
  // 重置刷新状态
  resetRefreshState()
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

// 刷新状态机
let isRefreshing = false
let refreshSubscribers = []
let lastRefreshTime = 0
let lastRefreshResult = null  // null=未刷新, 'success'=成功, 'failed'=失败
let lastRefreshFailedTime = 0  // 上次刷新失败的时间

/**
 * 重置刷新状态（当确定无法刷新时调用）
 */
export function resetRefreshState() {
  isRefreshing = false
  refreshSubscribers = []
  lastRefreshTime = 0
  lastRefreshResult = null
  lastRefreshFailedTime = 0
}

/**
 * 添加到刷新等待队列
 */
function subscribeTokenRefresh(callback) {
  refreshSubscribers.push(callback)
}

/**
 * 通知所有等待者
 * @param {string|null} newToken 新token或null表示失败
 * @param {boolean} isFinal 是否为终态（不会再刷新）
 */
function onTokenRefreshed(newToken, isFinal = false) {
  refreshSubscribers.forEach(callback => callback(newToken, isFinal))
  refreshSubscribers = []
}

/**
 * 执行 Token 刷新
 * 使用单例模式 + 防抖，避免并发刷新
 * @returns {Promise<{success: boolean, token: string|null, canRetry: boolean}>}
 */
export async function executeTokenRefresh() {
  const now = Date.now()

  // 检查是否是终态失败（短时间内刷新失败过）
  if (lastRefreshResult === 'failed' && (now - lastRefreshFailedTime) < REFRESH_COOLDOWN) {
    console.log('[Auth] Token 刷新处于失败冷却期，不再尝试')
    return { success: false, token: null, canRetry: false }
  }

  // 已有刷新在进行，等待结果
  if (isRefreshing) {
    console.log('[Auth] 已有刷新在进行，等待结果...')
    return new Promise((resolve) => {
      subscribeTokenRefresh((newToken, isFinal) => {
        if (newToken) {
          resolve({ success: true, token: newToken, canRetry: true })
        } else {
          resolve({ success: false, token: null, canRetry: !isFinal })
        }
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
      lastRefreshResult = 'success'
      onTokenRefreshed(result.token, false)
      isRefreshing = false
      return { success: true, token: result.token, canRetry: true }
    }

    throw new Error('刷新返回数据异常')
  } catch (e) {
    console.error('[Auth] Token 刷新失败:', e.message || e)
    
    // 刷新失败，设置为终态
    lastRefreshResult = 'failed'
    lastRefreshFailedTime = now
    
    onTokenRefreshed(null, true)  // 通知等待者这是终态
    isRefreshing = false
    
    return { success: false, token: null, canRetry: false }
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
    const result = await executeTokenRefresh()
    return result.token || getAccessToken()
  }

  return getAccessToken()
}

// ==================== 路由恢复机制 ====================

// 防止重复保存的标志
let lastSavedRedirectUrl = ''
let lastSavedRedirectTime = 0

/**
 * 保存登录后需要跳转的页面
 * 增加去重逻辑，防止重复保存相同URL
 */
export function saveRedirectUrl(url) {
  if (!url || url.includes('/pages/login')) return

  // 避免循环重定向
  if (url.includes('/pages/index') && !url.includes('redirect')) return

  const now = Date.now()

  // 防重复：5秒内不重复保存同一URL
  if (url === lastSavedRedirectUrl && (now - lastSavedRedirectTime) < 5000) {
    console.log('[Auth] 跳过重复保存重定向URL:', url)
    return
  }

  // 检查是否已有有效的重定向（5分钟内保存过）
  try {
    const existing = uni.getStorageSync(REDIRECT_KEY)
    if (existing?.url && existing?.timestamp) {
      if (now - existing.timestamp < REDIRECT_MAX_AGE && existing.url === url) {
        console.log('[Auth] 重定向URL已存在且未过期，跳过保存:', url)
        return
      }
    }
  } catch (e) {
    // ignore
  }

  try {
    uni.setStorageSync(REDIRECT_KEY, {
      url,
      timestamp: now
    })
    lastSavedRedirectUrl = url
    lastSavedRedirectTime = now
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
    lastSavedRedirectUrl = ''
    lastSavedRedirectTime = 0

    if (redirect?.url) {
      // 检查是否在有效期内
      if (Date.now() - redirect.timestamp < REDIRECT_MAX_AGE) {
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
  try {
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
  } catch (e) {
    console.error('[Auth] 获取当前页面路径失败:', e)
    return ''
  }
}

// ==================== 登录状态管理 ====================

/**
 * 处理登录成功
 */
export function handleLoginSuccess(token, userInfo, isGuest = false) {
  // 重置刷新状态
  resetRefreshState()
  
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
 * @param {boolean} redirectToLogin 是否跳转到登录页
 * @param {boolean} forceClear 是否强制清除（用于refresh 401时）
 */
export function handleAuthFailure(redirectToLogin = true, forceClear = false) {
  // 强制清除时重置所有状态
  if (forceClear) {
    resetRefreshState()
  }
  
  clearTokenData()

  // 保存当前页面，登录后恢复（仅在需要跳转时）
  if (redirectToLogin) {
    const currentPath = getCurrentPagePath()
    if (currentPath) {
      saveRedirectUrl(currentPath)
    }
  }

  return getCurrentPagePath()
}

/**
 * 处理退出登录
 */
export function handleLogout() {
  clearTokenData()
  uni.removeStorageSync(REDIRECT_KEY)
  resetRefreshState()
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
  resetRefreshState,

  // 路由恢复
  saveRedirectUrl,
  getAndClearRedirectUrl,
  getCurrentPagePath,

  // 登录状态
  handleLoginSuccess,
  handleAuthFailure,
  handleLogout
}
