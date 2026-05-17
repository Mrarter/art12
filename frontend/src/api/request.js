/**
 * =====================================================
 *  统一网络请求封装 - utils/request.js
 *  支持 H5 (Vite 代理) 和 微信小程序 (直连IP)
 *  艺本艺术品流通平台
 * =====================================================
 *
 *  功能清单：
 *    1. BASE_URL 配置（环境变量 → 默认值）      2. TIMEOUT 配置（30s）
 *    3. DEBUG 日志模式                           4. 统一请求封装 (request)
 *    5. GET/POST/PUT/DELETE 快捷方法              6. Token 自动注入
 *    7. 网络状态检测 (getNetworkType)             8. 错误提示 (Toast)
 *    9. Promise 返回                              10. 请求日志输出
 *   11. 响应日志输出                              12. 超时处理
 *   13. 统一异常处理                             14. 自动重试机制
 *   15. 请求去重保护                             16. 资源 URL 归一化
 */

// ==================== 常量 ====================

const PLATFORM = process.env.UNI_PLATFORM || 'h5'
const IS_MP = PLATFORM === 'mp-weixin'
const IS_DEV = process.env.NODE_ENV !== 'production'

// ==================== 1. BASE_URL & 2. TIMEOUT 配置 ====================

const DEV_LAN_HOST = import.meta.env?.VITE_DEV_LAN_HOST || '192.168.1.144'

// 小程序用 Caddy HTTP 代理（开发环境），H5 用 Vite 相对路径代理
const GATEWAY_ORIGIN = IS_MP
  ? (import.meta.env?.VITE_MP_GATEWAY_ORIGIN || `http://${DEV_LAN_HOST}:9443`)
  : ''

const FILE_ORIGIN = IS_MP
  ? (import.meta.env?.VITE_MP_FILE_ORIGIN || `http://${DEV_LAN_HOST}:9447`)
  : ''

const BASE_URL = IS_MP ? `${GATEWAY_ORIGIN}/api` : '/api'
const TIMEOUT = 30000         // 30 秒超时
const MAX_RETRIES = 2          // 最大重试次数
const RETRY_DELAY = 1000       // 重试间隔 (ms)

// 请求去重：存储进行中的请求标识，防止同一 URL 并发重复请求
const pendingRequests = new Map()

// ==================== 3. DEBUG 日志模式 ====================

const DEBUG = IS_DEV  // 开发环境开启详细日志

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
    console.log('平台:', PLATFORM)
    console.log('BASE_URL:', BASE_URL)
    console.groupEnd()
  },
  config: () => {
    if (!DEBUG) return
    console.log('%c========== 网络请求配置 ==========', 'font-weight:bold')
    console.log('  平台:', PLATFORM)
    console.log('  环境:', IS_DEV ? '开发' : '生产')
    console.log('  微信小程序:', IS_MP ? '是' : '否')
    console.log('  BASE_URL:', BASE_URL)
    console.log('  FILE_ORIGIN:', FILE_ORIGIN)
    console.log('  超时:', TIMEOUT + 'ms')
    console.log('  最大重试:', MAX_RETRIES)
    console.log('  DEBUG:', DEBUG)
    console.log('%c=================================', 'font-weight:bold')
  }
}

// 启动时打印配置
log.config()

// ==================== 7. 网络状态检测 ====================

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

// 监听网络变化
if (IS_MP) {
  uni.onNetworkStatusChange((res) => {
    log.api('网络状态变化:', res.networkType, res.isConnected ? '已连接' : '已断开')
    if (!res.isConnected) uni.showToast({ title: '网络已断开', icon: 'none' })
  })
}

// ==================== URL 工具函数 ====================

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

// ==================== 资源 URL 归一化（小程序替换文件域名） ====================

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

// ==================== 13. 统一异常处理 & 8. 错误提示 ====================

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
  // 只显示非技术类的简短错误
  const short = msg.substring(0, 60)
  if (msg.includes('timeout') || msg.includes('连接') || msg.includes('网络') || msg.includes('HTTPS')) {
    uni.showToast({ title: short, icon: 'none', duration: 3000 })
  }
}

// ==================== 15. 请求去重 ====================

const requestKey = (url, method) => `${method}:${url}`

// ==================== 4-6. 核心请求封装 ====================

/**
 * 发起请求（含自动重试、去重、Token注入、日志）
 */
const requestWithRetry = (options, retryCount = 0) => {
  return new Promise((resolve, reject) => {
    // ---- 构建 URL ----
    let url = BASE_URL + options.url
    if (!['POST', 'PUT', 'DELETE'].includes(options.method) && options.data) {
      const qs = buildQueryString(options.data)
      if (qs) url += qs
    }

    // ---- 15. 请求去重保护 ----
    const key = requestKey(url, options.method || 'GET')
    if (pendingRequests.has(key)) {
      log.warn('重复请求拦截:', key)
      return reject(new Error('请求已提交，请勿重复操作'))
    }
    pendingRequests.set(key, true)
    const cleanup = () => pendingRequests.delete(key)

    // ---- 6. Token 自动注入 ----
    const token = uni.getStorageSync('token') || ''
    const userInfo = uni.getStorageSync('userInfo')
    const userId = userInfo?.id || ''

    const header = { 'Content-Type': 'application/json' }
    if (token) header['Authorization'] = 'Bearer ' + token
    if (userId) header['X-User-Id'] = userId

    // ---- 10. 请求日志 ----
    log.req(options.method || 'GET', url, options.data)

    // ---- 发起请求 ----
    uni.request({
      url,
      method: options.method || 'GET',
      data: ['POST', 'PUT', 'DELETE'].includes(options.method) ? options.data : undefined,
      header,
      timeout: TIMEOUT,     // 2. TIMEOUT 配置

      // ---- 11. 响应日志 ----
      success: (res) => {
        cleanup()
        log.res(options.method || 'GET', url, res.statusCode, res.data)

        if (res.statusCode === 200) {
          const body = res.data
          if (body && body.code === 200) {
            resolve(normalizeResourceUrls(body.data)) // 9. Promise resolve
          } else if (body && body.code === 401) {
            // ---- 6. Token 过期处理 ----
            log.warn('Token 过期或未授权，清除登录态')
            uni.removeStorageSync('token')
            uni.removeStorageSync('userInfo')
            uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
            setTimeout(() => uni.navigateTo({ url: '/pages/login/index' }), 1500)
            reject(new Error(body?.message || '未授权')) // 9. Promise reject
          } else {
            log.warn('API 错误:', body?.code, body?.message)
            reject(new Error(body?.message || '请求失败')) // 9. Promise reject
          }
        } else if (res.statusCode === 401) {
          log.warn('未授权，清除登录态并跳转登录')
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          setTimeout(() => uni.navigateTo({ url: '/pages/login/index' }), 1500)
          reject(new Error('登录已过期，请重新登录'))
        } else if (res.statusCode === 404) {
          log.error('接口不存在:', url)
          reject(new Error('接口不存在 (404)'))
        } else if (res.statusCode === 500) {
          log.error('服务器错误:', url)
          uni.showToast({ title: '服务器内部错误，请稍后重试', icon: 'none' })
          reject(new Error('服务器内部错误 (500)'))
        } else if (res.statusCode === 502 || res.statusCode === 503) {
          log.error('网关错误:', res.statusCode, url)
          uni.showToast({ title: '服务暂不可用 (502/503)', icon: 'none' })
          reject(new Error(`服务暂不可用 (${res.statusCode})`))
        } else {
          log.error('HTTP 错误:', res.statusCode, url)
          reject(new Error(`HTTP ${res.statusCode}`))
        }
      },

      // ---- 12. 超时处理 & 13. 统一异常处理 ----
      fail: (err) => {
        cleanup()
        const errMsg = err?.errMsg || err?.message || ''

        // 判断是否可重试
        const isRetryable =
          errMsg.includes('timeout') || errMsg.includes('fail') || errMsg.includes('abort')

        if (isRetryable && retryCount < MAX_RETRIES) {
          log.warn(`请求失败，${RETRY_DELAY * (retryCount + 1)}ms 后第 ${retryCount + 2} 次重试`)
          setTimeout(() => {
            resolve(requestWithRetry(options, retryCount + 1))
          }, RETRY_DELAY * (retryCount + 1))
          return
        }

        // ---- 13. 最终失败处理 ----
        const friendly = friendlyError(errMsg, url)
        log.err(options.method || 'GET', url, errMsg, retryCount)

        // ---- 7. 网络检测 ----
        checkNetworkStatus().then((online) => {
          if (!online) {
            uni.showToast({ title: '网络不可用，请检查连接', icon: 'none' })
          } else {
            showErrorToast(friendly)
          }
        })

        reject(new Error(friendly)) // 9. Promise reject
      }
    })
  })
}

// ==================== 5. GET/POST/PUT/DELETE 方法 ====================

const request = (options) => requestWithRetry(options)

request.get = (url, data) => requestWithRetry({ url, method: 'GET', data })
request.post = (url, data) => requestWithRetry({ url, method: 'POST', data })
request.put = (url, data) => requestWithRetry({ url, method: 'PUT', data })
request.delete = (url, data) => requestWithRetry({ url, method: 'DELETE', data })

export default request
