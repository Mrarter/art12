/**
 * =====================================================
 *  统一网络请求封装 - api/request.js
 *  支持 H5 (Vite 代理) 和 微信小程序 (直连IP)
 *  艺本艺术品流通平台 v2.0
 * =====================================================
 *
 * 核心修复：
 *  1. 401时先尝试刷新，刷新失败则强制跳转登录
 *  2. refresh 401时立即重置状态，不再等待冷却
 *  3. 防止重复触发401处理
 *  4. 页面初始化失败不阻塞UI
 */

import {
  getAccessToken,
  getTokenData,
  isGuestUser,
  checkTokenValid,
  executeTokenRefresh,
  handleAuthFailure,
  saveRedirectUrl,
  getCurrentPagePath,
  resetRefreshState
} from '@/utils/auth'

// ==================== 常量 ====================

const PLATFORM = process.env.UNI_PLATFORM || 'h5'
const IS_MP = PLATFORM === 'mp-weixin'
const IS_DEV = process.env.NODE_ENV !== 'production'

const DEV_LAN_HOST = import.meta.env?.VITE_DEV_LAN_HOST || '192.168.1.144'

const GATEWAY_ORIGIN = IS_MP
  ? (import.meta.env?.VITE_MP_GATEWAY_ORIGIN || `http://${DEV_LAN_HOST}:9443`)
  : ''

const FILE_ORIGIN = IS_MP
  ? (import.meta.env?.VITE_MP_FILE_ORIGIN || `http://${DEV_LAN_HOST}:9447`)
  : ''

const BASE_URL = IS_MP ? `${GATEWAY_ORIGIN}/api` : '/api'
const TIMEOUT = 30000
const MAX_RETRIES = 2
const RETRY_DELAY = 1000

// 请求去重
const pendingRequests = new Map()

// 防止重复触发401处理
let isHandling401 = false

// ==================== 日志 ====================

const DEBUG = IS_DEV

const log = {
  api: (...args) => DEBUG && console.log('[API]', ...args),
  warn: (...args) => console.warn('[API WARN]', ...args),
  error: (...args) => console.error('[API ERROR]', ...args),
  req: (method, url, data) => {
    if (!DEBUG) return
    console.groupCollapsed(`[API] ➡ ${method} ${url}`)
    console.log('平台:', PLATFORM)
    console.log('BASE_URL:', BASE_URL)
    if (data) console.log('请求体:', typeof data === 'string' ? data : JSON.stringify(data, null, 2).substring(0, 500))
    console.groupEnd()
  },
  res: (method, url, status, data) => {
    if (!DEBUG) return
    console.groupCollapsed(`[API] ⬅ ${method} ${url} → ${status}`)
    console.log('响应数据:', typeof data === 'string' ? data : JSON.stringify(data, null, 2).substring(0, 500))
    console.groupEnd()
  },
  err: (method, url, error, retryCount) => {
    console.group(`[API] ❌ ${method} ${url}`)
    console.log('错误:', error)
    console.log('重试次数:', retryCount)
    console.groupEnd()
  },
  config: () => {
    if (!DEBUG) return
    console.log('%c========== 网络请求配置 ==========', 'font-weight:bold')
    console.log('  平台:', PLATFORM)
    console.log('  环境:', IS_DEV ? '开发' : '生产')
    console.log('  微信小程序:', IS_MP ? '是' : '否')
    console.log('  BASE_URL:', BASE_URL)
    console.log('  超时:', TIMEOUT + 'ms')
    console.log('%c=================================', 'font-weight:bold')
  }
}

log.config()

// ==================== 网络检测 ====================

const checkNetworkStatus = () => {
  return new Promise((resolve) => {
    if (!IS_MP) return resolve(true)
    uni.getNetworkType({
      success: (res) => {
        const available = res.networkType !== 'none'
        log.api('网络状态:', res.networkType, available ? '✅' : '❌')
        if (!available) uni.showToast({ title: '网络不可用，请检查连接', icon: 'none' })
        resolve(available)
      },
      fail: () => { resolve(true) }
    })
  })
}

if (IS_MP) {
  uni.onNetworkStatusChange((res) => {
    log.api('网络状态变化:', res.networkType, res.isConnected ? '已连接' : '已断开')
  })
}

// ==================== 工具函数 ====================

const buildQueryString = (data) => {
  if (!data) return ''
  const params = []
  for (const key in data) {
    if (data[key] !== undefined && data[key] !== null && data[key] !== '') {
      params.push(`${encodeURIComponent(key)}=${encodeURIComponent(data[key])}`)
    }
  }
  return params.length > 0 ? '?' + params.join('&') : ''
}

const normalizeResourceUrls = (value) => {
  if (!IS_MP) return value

  if (typeof value === 'string') {
    if (value.startsWith('/upload/')) return FILE_ORIGIN + value
    if (value.startsWith('upload/')) return FILE_ORIGIN + '/' + value
    if (value.startsWith('http://localhost:8087') || value.startsWith('http://127.0.0.1:8087')) {
      return FILE_ORIGIN + value.slice(value.indexOf(':8087') + 5)
    }
    // 注意：不再将 192.168.* 地址重写为 FILE_ORIGIN/GATEWAY_ORIGIN
    // 原因：图片服务器地址由后端直接返回（如 192.168.1.109:8087），不应被覆盖
    // 如果该地址不可达，由页面的 @error 处理（显示占位图），而非强制替换地址
    return value
  }
  if (Array.isArray(value)) return value.map(normalizeResourceUrls)
  if (value && typeof value === 'object') {
    const n = {}
    for (const k in value) n[k] = normalizeResourceUrls(value[k])
    return n
  }
  return value
}

// ==================== 错误处理 ====================

const errorMessages = {
  timeout: '请求超时，请检查网络或稍后重试',
  enotfound: '无法连接服务器，请确认后端服务已启动',
  econnrefused: '服务器连接被拒绝，请确认后端服务已启动',
  certificate: 'HTTPS 证书错误，请在开发者工具中勾选「不校验合法域名」',
  tls: 'TLS 握手失败，请改用 HTTP 开发模式',
  ssl: 'SSL 错误，请改用 HTTP',
  abort: '请求被中断',
  fail: (url) => url.includes('https://')
    ? 'HTTPS 请求失败，请改用 HTTP'
    : '网络请求失败，请检查网络连接'
}

const friendlyError = (errMsg, url) => {
  const m = errMsg.toLowerCase()
  if (!errMsg) return '网络请求失败'
  if (m.includes('timeout')) return errorMessages.timeout
  if (m.includes('enotfound') || m.includes('getaddrinfo')) return errorMessages.enotfound
  if (m.includes('econnrefused') || m.includes('connection refused')) return errorMessages.econnrefused
  if (m.includes('certificate')) return errorMessages.certificate
  if (m.includes('tls')) return errorMessages.tls
  if (m.includes('ssl')) return errorMessages.ssl
  if (m.includes('abort')) return errorMessages.abort
  if (m.includes('fail')) return errorMessages.fail(url)
  return errMsg.substring(0, 120)
}

const showErrorToast = (msg) => {
  const short = msg.substring(0, 60)
  if (msg.includes('timeout') || msg.includes('连接') || msg.includes('网络') || msg.includes('HTTPS')) {
    uni.showToast({ title: short, icon: 'none', duration: 3000 })
  }
}

// ==================== 请求去重 ====================

const requestKey = (url, method) => `${method}:${url}`

// ==================== 401 处理（统一入口）====================

// 防止并发请求多次触发登录跳转
let _isNavigatingToLogin = false
let _navigateTimer = null

// 等待刷新结果的待处理请求队列
let _pending401Queue = []

/**
 * 统一跳转登录页（防重复触发）
 */
const navigateToLogin = () => {
  if (_isNavigatingToLogin) return
  _isNavigatingToLogin = true

  const currentPath = getCurrentPagePath()
  if (currentPath && !currentPath.includes('/pages/login')) {
    log.warn('[Auth] 401 无效，跳转登录页')
    uni.navigateTo({ url: '/pages/login/index' })
  }

  // 3 秒后允许下一次跳转
  clearTimeout(_navigateTimer)
  _navigateTimer = setTimeout(() => {
    _isNavigatingToLogin = false
    // 重置全局 401 处理标记
    isHandling401 = false
  }, 3000)
}

/**
 * 处理所有排队的请求（刷新成功或失败时统一调用）
 */
const flushPending401Queue = (success, message) => {
  const queue = _pending401Queue.slice()
  _pending401Queue = []
  queue.forEach(({ options, resolve, reject }) => {
    if (success) {
      resolve(requestWithRetry(options))
    } else {
      reject(new Error(message || '登录已过期'))
    }
  })
}

/**
 * 处理 401 并尝试刷新 Token
 *
 * 场景 1：无 Token / 游客 → 拒绝 + 跳转登录（不清除 guest token，仅清空登录态）
 * 场景 2：已有刷新进行中 → 加入队列等待刷新完成
 * 场景 3：首次 401 → 触发刷新，成功后重试所有排队请求
 */
const handle401WithRefresh = (options, resolve, reject, message) => {
  const token = getAccessToken()
  const isGuest = isGuestUser()

  // 场景 1：无 Token → 拒绝 + 跳转登录（仅清除无效 Token，保留 guest token）
  if (!token) {
    log.warn('[401] 无 Token，直接拒绝请求:', options.url)
    handleAuthFailure(true, true) // 清除 Token + 保存重定向 URL
    navigateToLogin()
    reject(new Error('请先登录'))
    return
  }

  // 场景 1b：游客 → 拒绝 + 跳转登录（不调用 handleAuthFailure，避免清除 guest token）
  if (isGuest) {
    log.warn('[401] 游客，直接拒绝请求:', options.url)
    navigateToLogin()
    reject(new Error('请先登录'))
    return
  }

  // 场景 2：已有其他请求正在处理 401 → 加入队列，等待刷新完成
  if (isHandling401) {
    log.warn('[401] 正在处理中，加入等待队列:', options.url)
    _pending401Queue.push({ options, resolve, reject })
    return
  }

  // 场景 3：首次 401，尝试刷新
  isHandling401 = true
  log.api('[401] 尝试刷新 Token...')

  tryRefreshToken().then((result) => {
    if (result.success && result.token) {
      // 刷新成功 → 重试当前请求 + 所有排队请求
      log.api('[401] 刷新成功，重试请求:', options.url)
      flushPending401Queue(true)
      isHandling401 = false
      resolve(requestWithRetry(options))
    } else {
      // 刷新失败 → 所有排队请求一起拒绝
      log.warn('[401] 刷新失败，跳转登录页')
      handleAuthFailure(true, true)
      navigateToLogin()
      flushPending401Queue(false, message || '登录已过期')
      isHandling401 = false
      reject(new Error(message || '登录已过期'))
    }
  }).catch((e) => {
    log.error('[401] 刷新异常:', e)
    handleAuthFailure(true, true)
    navigateToLogin()
    flushPending401Queue(false, '登录状态异常')
    isHandling401 = false
    reject(new Error('登录状态异常'))
  })
}

// ==================== Token 刷新检查 ====================

/**
 * 尝试刷新 Token（供 request.js 内部使用）
 * @returns {Promise<{success: boolean, token: string|null, canRetry: boolean}>}
 */
const tryRefreshToken = async () => {
  // 游客模式不刷新
  if (isGuestUser()) {
    return { success: false, token: null, canRetry: false }
  }

  try {
    const result = await executeTokenRefresh()
    if (result.success) {
      log.api('[Refresh] 刷新成功')
      return result
    }
    // 刷新失败（已由 executeTokenRefresh 设置 _refreshPermanentlyDead）
    return result
  } catch (e) {
    log.error('[Refresh] 刷新异常:', e)
    return { success: false, token: null, canRetry: false }
  }
}

const checkAndRefreshToken = async () => {
  const check = checkTokenValid()
  if (check.reason === 'about_to_expire' && !check.isGuest) {
    log.api('检测到 Token 即将过期，触发无感刷新')
    await tryRefreshToken()
  }
}

// ==================== 原始请求（供 Token 刷新专用）====================

/**
 * 使用原生 uni.request 发送请求，完全绕过 requestWithRetry 拦截器。
 * 关键：遇到 401 时不会递归调用 401 处理逻辑，不会进入 executeTokenRefresh。
 * @param {object} opts { url, method, data }
 * @returns {Promise<any>}
 */
export const rawRefreshRequest = (opts) => {
  return new Promise((resolve, reject) => {
    const url = BASE_URL + opts.url
    const token = getAccessToken()
    const tokenData = getTokenData()
    const userId = tokenData?.userId || ''

    const header = { 'Content-Type': 'application/json' }
    if (token) header['Authorization'] = 'Bearer ' + token
    if (userId) header['X-User-Id'] = String(userId)

    log.api('[Raw] ➡', opts.method || 'GET', url)

    uni.request({
      url,
      method: opts.method || 'GET',
      data: opts.data,
      header,
      timeout: TIMEOUT,
      success: (res) => {
        log.res(opts.method || 'GET', url, res.statusCode, res.data)
        if (res.statusCode === 200) {
          const body = res.data
          // 业务 code 为 401（如 refresh token 失效），不递归处理，直接 reject
          if (body?.code === 401) {
            log.warn('[Raw] 刷新返回 401，直接 reject，不走拦截器')
            reject(new Error(body?.message || 'refresh_expired'))
            return
          }
          resolve(body?.data)
        } else {
          // HTTP 401/其他错误，不递归处理，直接 reject
          log.warn('[Raw] HTTP', res.statusCode, '直接 reject，不走拦截器')
          reject(new Error(`HTTP ${res.statusCode}`))
        }
      },
      fail: (err) => {
        const errMsg = err?.errMsg || err?.message || 'network_error'
        log.error('[Raw] 请求失败:', errMsg)
        reject(new Error(errMsg))
      }
    })
  })
}

// ==================== 核心请求封装 ====================

const requestWithRetry = (options, retryCount = 0) => {
  return new Promise((resolve, reject) => {
    let url = BASE_URL + options.url
    if (!['POST', 'PUT', 'DELETE'].includes(options.method) && options.data) {
      const qs = buildQueryString(options.data)
      if (qs) url += qs
    }

    // 请求去重保护
    const key = requestKey(url, options.method || 'GET')
    if (pendingRequests.has(key)) {
      log.warn('重复请求拦截:', key)
      return reject(new Error('请求已提交，请勿重复操作'))
    }
    pendingRequests.set(key, true)
    const cleanup = () => pendingRequests.delete(key)

    // Token 注入
    const token = getAccessToken()
    const tokenData = getTokenData()
    const userId = tokenData?.userId || ''

    const header = { 'Content-Type': 'application/json' }
    if (token) header['Authorization'] = 'Bearer ' + token
    if (userId) header['X-User-Id'] = String(userId)

    log.req(options.method || 'GET', url, options.data)

    uni.request({
      url,
      method: options.method || 'GET',
      data: ['POST', 'PUT', 'DELETE'].includes(options.method) ? options.data : undefined,
      header,
      timeout: TIMEOUT,

      success: (res) => {
        cleanup()
        log.res(options.method || 'GET', url, res.statusCode, res.data)

        if (res.statusCode === 200) {
          const body = res.data
          if (body && body.code === 200) {
            resolve(normalizeResourceUrls(body.data))
          } else if (body && body.code === 401) {
            // ===== API 返回 401 =====
            log.warn('[API] 业务 401:', {
              url,
              body,
              tokenAttached: !!token
            })
            handle401WithRefresh(options, resolve, reject, body?.message || '登录已过期')
          } else {
            log.warn('API 错误:', body?.code, body?.message)
            reject(new Error(body?.message || '请求失败'))
          }
        } else if (res.statusCode === 401) {
          // ===== HTTP 401 =====
          log.warn('[API] HTTP 401:', {
            url,
            body: res.data,
            tokenAttached: !!token,
            tokenPrefix: token ? token.substring(0, 20) : '无'
          })
          handle401WithRefresh(options, resolve, reject, '登录已过期，请重新登录')
        } else if (res.statusCode === 404) {
          log.api('API 404 (新用户无数据或接口未就绪):', url)
          reject(new Error('NOT_FOUND'))
        } else if (res.statusCode === 500) {
          log.error('服务器错误:', url)
          uni.showToast({ title: '服务器内部错误', icon: 'none' })
          reject(new Error('服务器内部错误 (500)'))
        } else if (res.statusCode === 502 || res.statusCode === 503) {
          log.error('网关错误:', res.statusCode)
          reject(new Error(`服务暂不可用 (${res.statusCode})`))
        } else {
          log.error('HTTP 错误:', res.statusCode)
          reject(new Error(`HTTP ${res.statusCode}`))
        }
      },

      fail: (err) => {
        cleanup()
        const errMsg = err?.errMsg || err?.message || ''

        const isRetryable = errMsg.includes('timeout') || errMsg.includes('fail') || errMsg.includes('abort')

        if (isRetryable && retryCount < MAX_RETRIES) {
          log.warn(`请求失败，${RETRY_DELAY * (retryCount + 1)}ms 后第 ${retryCount + 2} 次重试`)
          setTimeout(() => {
            resolve(requestWithRetry(options, retryCount + 1))
          }, RETRY_DELAY * (retryCount + 1))
          return
        }

        const friendly = friendlyError(errMsg, url)
        log.err(options.method || 'GET', url, errMsg, retryCount)
        checkNetworkStatus()
        showErrorToast(friendly)
        reject(new Error(friendly))
      }
    })
  })
}

// ==================== 导出 ====================

/**
 * request(options)
 * @param {object} options
 * @param {boolean} [options.requireAuth=false] - true 表示该接口需要登录态，无 Token 时直接拒绝
 */
const request = (options) => {
  checkAndRefreshToken()

  // 需要鉴权但无 Token → 直接拒绝，避免无效的网络往返和 401 流程
  if (options.requireAuth && !getAccessToken()) {
    log.warn('[Request] 需鉴权但无 Token，跳过请求:', options.url)
    return Promise.reject(new Error('请先登录'))
  }

  return requestWithRetry(options)
}

request.get = (url, data) => {
  return request({ url, method: 'GET', data })
}
request.post = (url, data) => {
  return request({ url, method: 'POST', data })
}
request.put = (url, data) => {
  return request({ url, method: 'PUT', data })
}
request.delete = (url, data) => {
  return request({ url, method: 'DELETE', data })
}

export default request
