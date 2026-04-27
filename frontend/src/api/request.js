// API 地址 - 使用固定地址
const BASE_URL = 'http://192.168.1.176:8080/api'

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
        'Authorization': uni.getStorageSync('token') ? 'Bearer ' + uni.getStorageSync('token') : ''
      },
      timeout: TIMEOUT,
      success: (res) => {
        if (res.statusCode === 200) {
          if (res.data.code === 200) {
            resolve(res.data.data)
          } else if (res.data.code === 401) {
            uni.showToast({ title: '请先登录', icon: 'none' })
            uni.navigateTo({ url: '/pages/login/index' })
            reject(res.data)
          } else {
            // 其他错误码不弹 toast，直接返回空数据
            console.warn('API 返回错误:', res.data)
            resolve(null)
          }
        } else {
          console.warn('HTTP 错误:', res.statusCode)
          resolve(null)
        }
      },
      fail: (err) => {
        console.warn('请求失败:', err)
        resolve(null)
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

export default request
