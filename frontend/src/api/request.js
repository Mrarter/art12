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
let handling401Timer = null

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
    if (value.startsWith('http://192.168.')) {
      return value.replace(/^http:\/\/192\.168\.\d+\.\d+:(8080|8087|9443|9447)/, (_, port) => {
        if (port === '8087' || port === '9447') return FILE_ORIGIN
        return GATEWAY_ORIGIN
      })
    }
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

// ==================== 401 处理 ====================

/**
 * 处理认证失败 - 强制跳转登录
 * @param {string} reason 失败原因
 */
const handleUnauthorized = (reason = '') => {
  // 防止多次触发
  if (isHandling401) {
    log.warn('正在处理401，跳过重复请求')
    return true
  }
  
  isHandling401 = true
  
  // 清除可能存在的延迟重置定时器
  if (handling401Timer) {
    clearTimeout(handling401Timer)
  }

  // 强制清除登录态和刷新状态
  handleAuthFailure(true, true)

  // 友好提示
  if (reason) {
    uni.showToast({ title: reason, icon: 'none', duration: 2000 })
  }

  // 延迟跳转登录页
  handling401Timer = setTimeout(() => {
    const currentPath = getCurrentPagePath()
    
    // 检查是否已登录页面，避免循环
    if (currentPath && !currentPath.includes('/pages/login')) {
      log.api('401处理：跳转到登录页')
      uni.navigateTo({ url: '/pages/login/index' })
    }
    
    // 重置标志（给足够的时间避免快速重复触发）
    handling401Timer = setTimeout(() => {
      isHandling401 = false
    }, 3000)
  }, 800)

  return true
}

/**
 * 尝试刷新 Token
 * @returns {Promise<{success: boolean, token: string|null, canRetry: boolean}>}
 */
const tryRefreshToken = async () => {
  // 游客模式不刷新
  if (isGuestUser()) {
    log.warn('游客模式，跳过 Token 刷新')
    return { success: false, token: null, canRetry: false }
  }

  try {
    const result = await executeTokenRefresh()
    if (result.success) {
      log.api('Token 刷新成功')
      return result
    }
    
    // 刷新失败，检查是否是终态
    if (!result.canRetry) {
      log.warn('Token 刷新失败且不可重试，标记为终态')
      // 立即重置刷新状态，不再等待冷却
      resetRefreshState()
    }
    
    return result
  } catch (e) {
    log.error('Token 刷新异常:', e)
    // 异常时也重置状态
    resetRefreshState()
    return { success: false, token: null, canRetry: false }
  }
}

// ==================== Token 刷新检查 ====================

const checkAndRefreshToken = async () => {
  const check = checkTokenValid()
  if (check.reason === 'about_to_expire' && !check.isGuest) {
    log.api('检测到 Token 即将过期，触发无感刷新')
    await tryRefreshToken()
  }
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
            log.warn('API 401: code=', body.code, body?.message)
            handle401WithRefresh(options, resolve, reject, body?.message || '登录已过期')
          } else {
            log.warn('API 错误:', body?.code, body?.message)
            reject(new Error(body?.message || '请求失败'))
          }
        } else if (res.statusCode === 401) {
          // ===== HTTP 401 =====
          log.warn('HTTP 401')
          handle401WithRefresh(options, resolve, reject, '登录已过期，请重新登录')
        } else if (res.statusCode === 404) {
          log.error('接口不存在:', url)
          reject(new Error('接口不存在 (404)'))
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

/**
 * 处理 401 并尝试刷新 Token
 */
const handle401WithRefresh = (options, resolve, reject, message) => {
  // 已经是处理中的状态，跳过
  if (isHandling401) {
    reject(new Error('正在处理登录状态'))
    return
  }

  // 游客直接跳转登录
  if (isGuestUser()) {
    handleUnauthorized('请先登录')
    reject(new Error('请先登录'))
    return
  }

  // 首次401，尝试刷新
  if (options._retryCount === undefined) {
    options._retryCount = 0
  }

  if (options._retryCount === 0) {
    options._retryCount = 1
    log.api('401处理：尝试刷新 Token...')

    tryRefreshToken().then((result) => {
      if (result.success && result.token) {
        // 刷新成功，重试请求
        log.api('Token 刷新成功，重试请求')
        resolve(requestWithRetry(options))
      } else {
        // 刷新失败，跳转登录
        log.warn('Token 刷新失败，跳转登录页')
        handleUnauthorized(message)
        reject(new Error(message))
      }
    })
    return
  }

  // 已经重试过，直接跳转登录
  log.warn('Token 刷新已重试过，直接跳转登录')
  handleUnauthorized(message)
  reject(new Error(message))
}

// ==================== 导出 ====================

const request = (options) => {
  // 清理内部重试计数
  delete options._retryCount
  checkAndRefreshToken()
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
