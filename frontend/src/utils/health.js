/**
 * =====================================================
 *  接口健康检查 - utils/health.js
 *  检测后端服务是否可访问
 *  艺本艺术品流通平台
 * =====================================================
 *
 *  用法：
 *    import { checkHealth, checkAllServices } from '@/utils/health'
 *
 *    // 简单检测
 *    const ok = await checkHealth()
 *
 *    // 全部服务检测
 *    const result = await checkAllServices()
 *    console.log(result)
 *    // → { gateway: true, file: true, user: true, product: true }
 */

const IS_MP = process.env.UNI_PLATFORM === 'mp-weixin'
const IS_DEV = process.env.NODE_ENV !== 'production'

const DEV_LAN_HOST = import.meta.env?.VITE_DEV_LAN_HOST || '192.168.1.144'

// 从环境变量获取各服务地址
const GATEWAY_ORIGIN = IS_MP
  ? (import.meta.env?.VITE_MP_GATEWAY_ORIGIN || `http://${DEV_LAN_HOST}:9443`)
  : ''

const FILE_ORIGIN = IS_MP
  ? (import.meta.env?.VITE_MP_FILE_ORIGIN || `http://${DEV_LAN_HOST}:9447`)
  : ''

const BASE_URL = IS_MP ? `${GATEWAY_ORIGIN}/api` : '/api'

/**
 * 检测单个服务是否可访问
 */
const checkService = (url, timeout = 5000) => {
  return new Promise((resolve) => {
    if (!IS_MP) {
      // H5 用 fetch
      fetch(url, { method: 'HEAD', mode: 'no-cors' })
        .then(() => resolve(true))
        .catch(() => resolve(false))
      return
    }

    // 小程序用 uni.request
    const startTime = Date.now()
    uni.request({
      url,
      method: 'GET',
      timeout,
      success: (res) => {
        const elapsed = Date.now() - startTime
        if (IS_DEV) console.log(`[Health] ${url} → ${res.statusCode} (${elapsed}ms)`)
        resolve(true)  // 只要服务器有响应就算健康
      },
      fail: (err) => {
        if (IS_DEV) console.warn(`[Health] ${url} → ❌ ${err.errMsg || '不可达'}`)
        resolve(false)
      }
    })
  })
}

/**
 * 网关健康检查 (GET /api/health)
 * 后端 gateway 模块应暴露此端点
 */
export const checkHealth = async () => {
  const url = `${BASE_URL}/health`
  const ok = await checkService(url)
  if (IS_DEV) {
    console.log(`[Health] 网关健康检查: ${ok ? '✅ 正常' : '❌ 不可达'}`)
    console.log(`[Health] 地址: ${url}`)
  }
  return ok
}

/**
 * 全服务检测
 * 返回各服务的可用状态
 */
export const checkAllServices = async () => {
  const results = {
    gateway: false,
    user: false,
    product: false,
    file: false,
    network: false
  }

  // 网络状态
  if (IS_MP) {
    try {
      const net = await new Promise((r) => {
        uni.getNetworkType({ success: (res) => r(res.networkType), fail: () => r('unknown') })
      })
      results.network = net !== 'none'
    } catch { /* ignore */ }
  } else {
    results.network = true
  }

  // 并行检测各服务
  const checks = await Promise.allSettled([
    checkService(`${BASE_URL}/health`),                         // gateway
    checkService(`${BASE_URL}/user/info`, 3000),                // user
    checkService(`${BASE_URL}/product/list?page=1&pageSize=1`), // product
    checkService(FILE_ORIGIN, 3000)                              // file
  ])

  results.gateway = checks[0].status === 'fulfilled' && checks[0].value
  results.user = checks[1].status === 'fulfilled' && checks[1].value
  results.product = checks[2].status === 'fulfilled' && checks[2].value
  results.file = checks[3].status === 'fulfilled' && checks[3].value

  return results
}

/**
 * 输出健康检查报告
 */
export const printHealthReport = async () => {
  const results = await checkAllServices()
  console.log('%c========== 服务健康检查报告 ==========', 'font-weight:bold;font-size:14px')
  console.log(`  网络连接: ${results.network ? '✅' : '❌'}`)
  console.log(`  Gateway:  ${results.gateway ? '✅' : '❌'} (${BASE_URL}/health)`)
  console.log(`  User:     ${results.user ? '✅' : '❌'} (${BASE_URL}/user/info)`)
  console.log(`  Product:  ${results.product ? '✅' : '❌'} (${BASE_URL}/product/list)`)
  console.log(`  File:     ${results.file ? '✅' : '❌'} (${FILE_ORIGIN})`)
  console.log('%c=====================================', 'font-weight:bold')

  const allOk = Object.values(results).every(v => v === true)
  if (allOk) {
    console.log('%c🎉 所有服务运行正常！', 'color:green;font-weight:bold')
  } else {
    const failed = Object.entries(results).filter(([, v]) => !v).map(([k]) => k)
    console.log(`%c⚠️ 以下服务不可用: ${failed.join(', ')}`, 'color:orange;font-weight:bold')
  }

  return results
}

export default { checkHealth, checkAllServices, printHealthReport }
