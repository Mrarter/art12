// API 地址
// H5/模拟器：使用相对路径走 Vite 代理 (/api -> localhost:8082)
// 微信真机调试：必须使用电脑在同一 Wi-Fi 下的局域网 IP
// 生产预览/上传版：需要在微信公众平台配置 HTTPS 合法域名
const DEV_LAN_HOST = '192.168.1.109'
const API_ORIGIN = process.env.UNI_PLATFORM === 'mp-weixin'
  ? `http://${DEV_LAN_HOST}:8082`
  : ''
const BASE_URL = API_ORIGIN + '/api'
const LOCAL_FILE_ORIGIN = 'http://localhost:8087'
const FILE_BASE_URL = process.env.UNI_PLATFORM === 'mp-weixin'
  ? `http://${DEV_LAN_HOST}:8087`
  : ''

// 增加超时时间到 30 秒
const TIMEOUT = 30000

// 兼容小程序的 URL 参数拼接函数
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
  if (typeof value === 'string') {
    if (value.startsWith(LOCAL_FILE_ORIGIN)) {
      return FILE_BASE_URL + value.slice(LOCAL_FILE_ORIGIN.length)
    }
    if (value.startsWith('http://127.0.0.1:8087')) {
      return FILE_BASE_URL + value.slice('http://127.0.0.1:8087'.length)
    }
    if (value.startsWith('http://192.168.')) {
      return value.replace(/^http:\/\/192\.168\.\d+\.\d+:(8080|8087)/, (_, port) => {
        return port === '8087' ? FILE_BASE_URL : API_ORIGIN
      })
    }
    return value
  }

  if (Array.isArray(value)) {
    return value.map(normalizeResourceUrls)
  }

  if (value && typeof value === 'object') {
    const normalized = {}
    for (const key in value) {
      normalized[key] = normalizeResourceUrls(value[key])
    }
    return normalized
  }

  return value
}

const request = (options) => {
  return new Promise((resolve, reject) => {
    // 判断是否是 GET 请求且有参数
    let url = BASE_URL + options.url
    if (options.method !== 'POST' && options.method !== 'PUT' && options.method !== 'DELETE' && options.data) {
      const queryString = buildQueryString(options.data)
      if (queryString) {
        url += queryString
      }
    }

    uni.request({
      url: url,
      method: options.method || 'GET',
      data: options.method === 'POST' || options.method === 'PUT' ? options.data : undefined,
      header: {
        'Content-Type': 'application/json',
        'Authorization': uni.getStorageSync('token') ? 'Bearer ' + uni.getStorageSync('token') : '',
        'X-User-Id': uni.getStorageSync('userInfo')?.id || ''
      },
      timeout: TIMEOUT,
      success: (res) => {
        if (res.statusCode === 200) {
          if (res.data && res.data.code === 200) {
            resolve(normalizeResourceUrls(res.data.data))
          } else if (res.data && res.data.code === 401) {
            uni.showToast({ title: '请先登录', icon: 'none' })
            uni.navigateTo({ url: '/pages/login/index' })
            reject(new Error('未授权'))
          } else {
            // 其他错误码不弹 toast，抛出错误让调用方处理
            console.warn('API 返回错误:', res.data)
            reject(new Error(res.data?.message || 'API错误'))
          }
        } else {
          console.warn('HTTP 错误:', res.statusCode)
          reject(new Error('HTTP错误: ' + res.statusCode))
        }
      },
      fail: (err) => {
        console.warn('请求失败:', { url, err })
        reject(new Error('网络请求失败'))
      }
    })
  })
}

// 封装 GET 请求
request.get = function(url, data) {
  return request({ url, method: 'GET', data })
}

// 封装 POST 请求
request.post = function(url, data) {
  return request({ url, method: 'POST', data })
}

// 封装 PUT 请求
request.put = function(url, data) {
  return request({ url, method: 'PUT', data })
}

// 封装 DELETE 请求
request.delete = function(url, data) {
  return request({ url, method: 'DELETE', data })
}

export default request
