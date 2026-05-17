/**
 * 微信端手机设备信息采集工具
 * 
 * 适配环境：
 *   - 微信小程序 (mp-weixin) → uni.getSystemInfoSync() + uni.getNetworkType()
 *   - 微信内置浏览器 H5   → navigator.userAgent 解析
 *   - 其他 H5 环境         → 标准 UA 解析（降级）
 * 
 * 返回结构化 JSON，含：
 *   os, osVersion, deviceModel, brand, screenWidth, screenHeight,
 *   pixelRatio, networkType, platform, ua
 * 
 * 兼容 iOS / Android，无法获取精确型号时有降级逻辑。
 */

/**
 * Android 设备型号映射表（UA 中常见的型号标识 → 商用名称）
 * 用于从 userAgent 中提取精确型号
 */
const ANDROID_MODEL_MAP = {
  'iPhone': 'iPhone', // 兜底
  // 华为
  'huawei|aln|alt|brd|lna|bal|mna|pal|bwn': '华为',
  'honor': '荣耀',
  'harmonyos': '华为',
  'mate': '华为 Mate',
  'pura': '华为 Pura',
  'nova': '华为 Nova',
  'p\\s?\\d+': '华为 P 系列',
  // 小米
  'mi\\s?\\d+': '小米',
  'xiaomi': '小米',
  'redmi': '红米',
  'mix': '小米 MIX',
  // OPPO
  'oppo|pfjm|pffm|pgfm|phj': 'OPPO',
  'find\\s?x': 'OPPO Find X',
  'reno': 'OPPO Reno',
  // vivo
  'vivo': 'vivo',
  'iqoo': 'iQOO',
  'x\\s?\\d+': 'vivo X 系列',
  // 三星
  'samsung|sm-': '三星',
  'galaxy': '三星 Galaxy',
  // 其他
  'oneplus': '一加',
  'meizu': '魅族',
  'realme': '真我',
  'lenovo': '联想',
  'nubia': '努比亚',
  'pixel': 'Google Pixel',
  'sony': '索尼',
  'lg': 'LG',
  'zte': '中兴',
  'smartisan': '锤子',
  'blackshark': '黑鲨'
}

/** 操作系统映射 */
const OS_MAP = {
  ios: 'iOS',
  android: 'Android',
  mac: 'macOS',
  windows: 'Windows',
  linux: 'Linux'
}

/**
 * 解析 userAgent 字符串
 * @param {string} ua
 * @returns {{ os: string, osVersion: string, deviceModel: string, brand: string, isWechat: boolean, isMiniProgram: boolean }}
 */
function parseUserAgent(ua) {
  const result = {
    os: '未知',
    osVersion: '',
    deviceModel: '未知设备',
    brand: '',
    isWechat: false,
    isMiniProgram: false
  }

  if (!ua) return result

  // 检测微信环境
  result.isWechat = /MicroMessenger/i.test(ua)
  result.isMiniProgram = /miniProgram/i.test(ua) || /miniprogram/i.test(ua)

  // ---- 操作系统识别 ----
  if (/iPhone|iPad|iPod/i.test(ua)) {
    result.os = 'iOS'
    const ver = ua.match(/OS\s([\d_]+)/i)
    if (ver) result.osVersion = ver[1].replace(/_/g, '.')
    // iPhone 型号判断
    if (/iPhone/i.test(ua)) {
      // 尝试从 UA 中提取 iPhone 型号
      const match = ua.match(/iPhone(\d+),(\d+)/i)
      if (match) {
        result.deviceModel = resolveIphoneModel(match[1], match[2])
      } else {
        result.deviceModel = 'iPhone'
      }
      result.brand = 'Apple'
    } else if (/iPad/i.test(ua)) {
      result.deviceModel = 'iPad'
      result.brand = 'Apple'
    } else if (/iPod/i.test(ua)) {
      result.deviceModel = 'iPod touch'
      result.brand = 'Apple'
    }
  } else if (/Android/i.test(ua)) {
    result.os = 'Android'
    const ver = ua.match(/Android\s([\d.]+)/i)
    if (ver) result.osVersion = ver[1]

    // 提取设备型号：尝试多种 UA 模式
    let model = ''
    // 1. 标准 Android UA 模式:  (xxx; xxx; xxx Build/xxx)
    const buildMatch = ua.match(/;\s*([^;]+?)\s+Build\//i)
    if (buildMatch) {
      model = buildMatch[1].trim()
    }
    // 2. 部分微信 UA:  MQQBrowser/xxx/Mz3/xxx
    const mzMatch = ua.match(/\/Mz\d+\/([^;\s]+)/i)
    if (!model && mzMatch) {
      model = mzMatch[1].trim()
    }
    // 3. 无 Build/ 的 UA（如华为 HarmonyOS: "HarmonyOS; ALN-AL00"）
    if (!model) {
      const altMatch = ua.match(/Android[^;]+;\s*([^);]+)/i)
      if (altMatch) model = altMatch[1].trim()
    }
    // 清理多余信息
    if (model) {
      model = model.replace(/;.*$/, '').replace(/\(.*$/, '').trim()
      result.deviceModel = matchAndroidModel(model)
      result.brand = resolveAndroidBrand(model)
    } else {
      result.deviceModel = 'Android 设备'
    }
  } else if (/Mac OS X/i.test(ua) || /Macintosh/i.test(ua)) {
    result.os = 'macOS'
    const ver = ua.match(/Mac OS X\s([\d_]+)/i)
    if (ver) result.osVersion = ver[1].replace(/_/g, '.')
  } else if (/Windows/i.test(ua)) {
    result.os = 'Windows'
    const ver = ua.match(/Windows\sNT\s([\d.]+)/i)
    if (ver) result.osVersion = ver[1]
  }

  return result
}

/**
 * iPhone 型号标识转商用名
 * e.g. iPhone12,1 → iPhone 11
 */
function resolveIphoneModel(major, minor) {
  const map = {
    '10': { '1': 'iPhone X', '2': 'iPhone X', '3': 'iPhone XS', '4': 'iPhone XS Max', '5': 'iPhone XR' },
    '11': { '2': 'iPhone XS', '4': 'iPhone XS Max', '6': 'iPhone XR', '8': 'iPhone 11 Pro', '9': 'iPhone 11 Pro Max', '10': 'iPhone 11' },
    '12': { '1': 'iPhone 12 mini', '3': 'iPhone 12', '5': 'iPhone 12 Pro', '7': 'iPhone 12 Pro Max', '9': 'iPhone 13', '10': 'iPhone 13 mini', '11': 'iPhone 13 Pro', '13': 'iPhone 13 Pro Max' },
    '13': { '1': 'iPhone 14', '2': 'iPhone 14 Plus', '3': 'iPhone 14 Pro', '4': 'iPhone 14 Pro Max' },
    '14': { '2': 'iPhone 15', '3': 'iPhone 15 Pro', '4': 'iPhone 15 Pro Max', '5': 'iPhone 15 Plus' },
    '15': { '1': 'iPhone 16', '2': 'iPhone 16 Pro', '3': 'iPhone 16 Pro Max', '4': 'iPhone 16 Plus' },
    '16': { '1': 'iPhone 16e', '2': 'iPhone 17', '3': 'iPhone 17 Pro', '4': 'iPhone 17 Pro Max' }
  }
  const series = map[major]
  if (!series) return 'iPhone (' + major + ',' + minor + ')'
  return series[minor] || 'iPhone (' + major + ',' + minor + ')'
}

/**
 * 从 UA 提取的原始型号文本中匹配已知品牌/系列
 */
function matchAndroidModel(raw) {
  if (!raw) return 'Android 设备'
  const lower = raw.toLowerCase()

  // 按优先级尝试匹配
  for (const [pattern, name] of Object.entries(ANDROID_MODEL_MAP)) {
    const regex = new RegExp(pattern, 'i')
    if (regex.test(lower)) {
      // 提取具体型号数字
      const digits = raw.match(/\d+/)
      return name + (digits ? ' ' + digits[0] : '')
    }
  }

  // 未匹配到已知品牌：返回原始标识（截断过长字符串）
  return raw.length > 30 ? raw.substring(0, 30) + '...' : raw
}

/**
 * 从原始型号文本中提取品牌
 */
function resolveAndroidBrand(raw) {
  if (!raw) return ''
  const lower = raw.toLowerCase()
  if (/huawei|honor|harmonyos|aln|alt|brd/i.test(lower)) return '华为'
  if (/xiaomi|mi\s/i.test(lower)) return '小米'
  if (/redmi/i.test(lower)) return '红米'
  if (/oppo/i.test(lower)) return 'OPPO'
  if (/vivo/i.test(lower)) return 'vivo'
  if (/samsung|sm-/i.test(lower)) return '三星'
  if (/oneplus/i.test(lower)) return '一加'
  if (/meizu/i.test(lower)) return '魅族'
  if (/lenovo/i.test(lower)) return '联想'
  if (/google|pixel/i.test(lower)) return 'Google'
  if (/sony/i.test(lower)) return '索尼'
  if (/lg/i.test(lower)) return 'LG'
  if (/realme/i.test(lower)) return '真我'
  return ''
}

/**
 * 网络类型中文映射
 */
const NETWORK_LABEL = {
  wifi: 'Wi-Fi',
  '4g': '4G',
  '3g': '3G',
  '2g': '2G',
  '5g': '5G',
  unknown: '未知网络',
  none: '无网络连接'
}

/**
 * 主函数：获取完整设备信息
 * 在微信小程序/UniApp环境下优先使用原生 API，
 * H5 环境使用 userAgent 解析作为降级。
 * 
 * @returns {Promise<Object>} 设备信息 JSON
 *
 * 返回示例（iOS）:
 * {
 *   "os": "iOS",
 *   "osVersion": "16.3",
 *   "deviceModel": "iPhone 14 Pro",
 *   "brand": "Apple",
 *   "screenWidth": 390,
 *   "screenHeight": 844,
 *   "pixelRatio": 3,
 *   "networkType": "Wi-Fi",
 *   "platform": "ios",
 *   "isWechat": true,
 *   "isMiniProgram": true,
 *   "ua": "Mozilla/5.0 ... MicroMessenger/..."
 * }
 *
 * 返回示例（Android - 降级）:
 * {
 *   "os": "Android",
 *   "osVersion": "13",
 *   "deviceModel": "Android 设备",
 *   "brand": "",
 *   "screenWidth": 360,
 *   "screenHeight": 780,
 *   "pixelRatio": 2.75,
 *   "networkType": "5G",
 *   "platform": "android",
 *   "isWechat": true,
 *   "isMiniProgram": false,
 *   "ua": "Mozilla/5.0 ..."
 * }
 */
export function getDeviceInfo() {
  return new Promise((resolve) => {
    const info = createEmptyInfo()
    const ua = getUa()
    info.ua = ua

    // ---- 1. 尝试 UniApp 原生 API（微信小程序/App端） ----
    if (typeof uni !== 'undefined' && uni.getSystemInfoSync) {
      try {
        const sys = uni.getSystemInfoSync()
        info.os = mapOs(sys.platform || sys.os || '')
        info.osVersion = sys.system || ''
        info.brand = sys.brand || ''
        // deviceModel：优先使用 deviceModel / model，备选 brand+model
        info.deviceModel = sys.deviceModel || sys.model || ''
        info.screenWidth = sys.screenWidth || sys.windowWidth || 0
        info.screenHeight = sys.screenHeight || sys.windowHeight || 0
        info.pixelRatio = sys.pixelRatio || 1
        info.platform = sys.platform || ''
        info.isWechat = true
        info.isMiniProgram = ua ? /miniProgram/i.test(ua) : false
      } catch (e) {
        // API 报错时降级到 UA 解析
        console.warn('[getDeviceInfo] uni.getSystemInfoSync 失败，降级到 UA:', e)
      }
    }

    // ---- 2. UniApp 原生 API 未获取到型号时，用 UA 补充 ----
    if (!info.deviceModel || info.deviceModel === 'Unknown' || info.deviceModel === 'unknown') {
      const parsed = parseUserAgent(ua)
      if (info.deviceModel === '' || info.deviceModel === 'Unknown') {
        info.deviceModel = parsed.deviceModel
      }
      if (!info.brand) info.brand = parsed.brand || ''
      if (!info.os || info.os === '未知') info.os = parsed.os
      if (!info.osVersion) info.osVersion = parsed.osVersion
    }

    // ---- 3. 屏幕分辨率补充（UA 解析的兜底） ----
    if (!info.screenWidth || !info.screenHeight) {
      if (typeof window !== 'undefined') {
        info.screenWidth = window.screen?.width || window.innerWidth || 0
        info.screenHeight = window.screen?.height || window.innerHeight || 0
        if (!info.pixelRatio || info.pixelRatio < 1) {
          info.pixelRatio = window.devicePixelRatio || 1
        }
      }
    }

    // ---- 4. 获取网络类型 ----
    getNetworkType().then((netType) => {
      info.networkType = netType
      // 添加采集时间戳
      info.collectTime = new Date().toISOString()
      resolve(info)
    }).catch(() => {
      info.networkType = '未知'
      info.collectTime = new Date().toISOString()
      resolve(info)
    })
  })
}

/**
 * 同步获取设备信息（不含网络类型，立即返回）
 */
export function getDeviceInfoSync() {
  const info = createEmptyInfo()
  const ua = getUa()
  info.ua = ua

  if (typeof uni !== 'undefined' && uni.getSystemInfoSync) {
    try {
      const sys = uni.getSystemInfoSync()
      info.os = mapOs(sys.platform || '')
      info.osVersion = sys.system || ''
      info.brand = sys.brand || ''
      info.deviceModel = sys.deviceModel || sys.model || ''
      info.screenWidth = sys.screenWidth || 0
      info.screenHeight = sys.screenHeight || 0
      info.pixelRatio = sys.pixelRatio || 1
      info.platform = sys.platform || ''
    } catch (e) {
      // fallback to UA
    }
  }

  if (!info.deviceModel || info.deviceModel === 'Unknown') {
    const parsed = parseUserAgent(ua)
    info.deviceModel = parsed.deviceModel
    if (!info.brand) info.brand = parsed.brand || ''
    if (!info.os || info.os === '未知') info.os = parsed.os
    if (!info.osVersion) info.osVersion = parsed.osVersion
  }

  if (!info.screenWidth || !info.screenHeight) {
    if (typeof window !== 'undefined') {
      info.screenWidth = window.screen?.width || window.innerWidth || 0
      info.screenHeight = window.screen?.height || window.innerHeight || 0
      info.pixelRatio = window.devicePixelRatio || 1
    }
  }

  info.collectTime = new Date().toISOString()
  return info
}

// ==================== 内部工具函数 ====================

function createEmptyInfo() {
  return {
    os: '未知',
    osVersion: '',
    deviceModel: '',
    brand: '',
    screenWidth: 0,
    screenHeight: 0,
    pixelRatio: 1,
    networkType: '',
    platform: '',
    isWechat: false,
    isMiniProgram: false,
    ua: '',
    collectTime: ''
  }
}

function getUa() {
  try {
    return navigator?.userAgent || ''
  } catch (e) {
    // 微信小程序中 navigator 不可用
    return ''
  }
}

function mapOs(platform) {
  const p = (platform || '').toLowerCase()
  if (p.includes('ios')) return 'iOS'
  if (p.includes('android')) return 'Android'
  if (p.includes('mac')) return 'macOS'
  if (p.includes('win')) return 'Windows'
  if (p.includes('linux')) return 'Linux'
  if (p === 'devtools') return '开发工具'
  return OS_MAP[platform] || platform || '未知'
}

function getNetworkType() {
  return new Promise((resolve) => {
    if (typeof uni !== 'undefined' && uni.getNetworkType) {
      uni.getNetworkType({
        success: (res) => {
          resolve(NETWORK_LABEL[res.networkType] || res.networkType || '未知')
        },
        fail: () => {
          resolve(fallbackNetworkType())
        }
      })
    } else {
      resolve(fallbackNetworkType())
    }
  })
}

function fallbackNetworkType() {
  if (typeof navigator === 'undefined' || !navigator.connection) {
    return '未知'
  }
  const conn = navigator.connection
  if (conn.effectiveType) {
    return (conn.effectiveType + '').toUpperCase().replace('SLOW-2G', '2G')
  }
  if (conn.type) {
    return conn.type === 'wifi' ? 'Wi-Fi' : conn.type
  }
  return '未知'
}
