/**
 * 环境检测工具
 * 自动识别当前运行环境（开发/测试/生产）
 */

// 环境检测缓存
let _envChecked = false
let _envInfo = null

/**
 * 获取当前运行环境信息
 * @returns {{ mode: string, label: string, isTest: boolean, isDev: boolean, isProd: boolean }}
 */
export function getEnvInfo() {
  if (_envChecked && _envInfo) return _envInfo

  const mode = import.meta.env?.MODE || 'development'
  const envFlag = import.meta.env?.VITE_ENV || ''
  const apiUrl = import.meta.env?.VITE_API_BASE_URL || ''
  const hostname = typeof window !== 'undefined' ? window.location.hostname : ''

  // 判断是否为测试环境
  const isTest = mode === 'test' || envFlag === 'test' || apiUrl.includes('test-api') || hostname.includes('test')

  _envInfo = {
    mode,
    label: import.meta.env?.VITE_ENV_LABEL || (isTest ? '测试环境' : ''),
    isTest,
    isDev: mode === 'development',
    isProd: mode === 'production' && !isTest
  }
  _envChecked = true
  return _envInfo
}

/**
 * 当前是否为测试环境
 */
export function isTestEnv() {
  return getEnvInfo().isTest
}
