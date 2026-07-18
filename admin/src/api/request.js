import axios from 'axios'
import { ElMessage } from 'element-plus'

// 图片 CDN 域名（从环境变量读取，默认使用七牛云）
const CDN_URL = import.meta.env.VITE_CDN_URL || ''

// 管理员请求实例
const request = axios.create({
  baseURL: '/api/admin',
  timeout: 15000
})

// 小程序 API 请求实例（用于商品、用户等服务）
const requestApi = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 不显示错误消息的请求方法
request.silentGet = (url, config = {}) => {
  return request.get(url, { ...config, silent: true })
}

request.silentPut = (url, data, config = {}) => {
  return request.put(url, data, { ...config, silent: true })
}

requestApi.silentGet = (url, config = {}) => {
  return requestApi.get(url, { ...config, silent: true })
}

requestApi.silentPut = (url, data, config = {}) => {
  return requestApi.put(url, data, { ...config, silent: true })
}

// 请求拦截
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('admin_token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

requestApi.interceptors.request.use(
  config => {
    const token = localStorage.getItem('admin_token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截
request.interceptors.response.use(
  response => {
    const res = response.data
    const silent = response.config.silent
    if (res.code !== 200) {
      if (!silent) {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  error => {
    const silent = error.config?.silent
    if (error.code === 'ERR_NETWORK' || error.message === 'Network Error' || error.code === 'ECONNABORTED') {
      if (!silent) {
        console.warn('后端服务未启动，部分功能可能不可用')
      }
      return Promise.reject(new Error('backend_offline'))
    }
    if (error.response?.status === 401) {
      localStorage.removeItem('admin_token')
      window.location.href = '/login'
    }
    if (!silent) {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

requestApi.interceptors.response.use(
  response => {
    const res = response.data
    const silent = response.config.silent
    if (res.code !== 200) {
      if (!silent) {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  error => {
    const silent = error.config?.silent
    if (error.code === 'ERR_NETWORK' || error.message === 'Network Error' || error.code === 'ECONNABORTED') {
      if (!silent) {
        console.warn('后端服务未启动，部分功能可能不可用')
      }
      return Promise.reject(new Error('backend_offline'))
    }
    if (error.response?.status === 401) {
      localStorage.removeItem('admin_token')
      window.location.href = '/login'
    }
    if (!silent) {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export { requestApi }

// 文件上传方法（统一走 file 服务，避免不同容器写入各自本地目录）
export const uploadFile = async (file, onProgress) => {
  const formData = new FormData()
  formData.append('file', file)

  const response = await axios.post('/api/file/upload/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      ...(localStorage.getItem('admin_token') && {
        'Authorization': `Bearer ${localStorage.getItem('admin_token')}`
      })
    },
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    }
  })

  if (response.data.code !== 200) {
    throw new Error(response.data.message || '上传失败')
  }

  return response.data.data
}

// 图片完整 URL 处理
export const getFullImageUrl = (url) => {
  if (!url) return ''
  if (url === '/images/default-artwork.png') {
    return '/images/default-artwork.png?v=20260617'
  }
  const isLocalRuntime = typeof window !== 'undefined'
    && ['localhost', '127.0.0.1', '0.0.0.0'].includes(window.location.hostname)

  // 已有完整域名（包括 http://, https://）
  if (url.startsWith('http://') || url.startsWith('https://')) {
    // 历史遗留的本地/内网文件服务绝对路径 → 转为相对路径走站点代理
    const localUploadPath = normalizePrivateUploadUrl(url)
    if (localUploadPath) {
      return ensureUploadPrefix(localUploadPath)
    }
    if (isLocalRuntime && /^https?:\/\/(?:a\.)?art1\.cn\/upload\//.test(url)) {
      const path = url.replace(/^https?:\/\/[^/]+/, '')
      return ensureUploadPrefix(path)
    }
    return url
  }
  if (url.startsWith('//')) {
    // 协议相对 URL，补全 https
    return 'https:' + url
  }
  // 相对路径：拼接 CDN 地址或当前域名
  if (CDN_URL && !isLocalRuntime) {
    return CDN_URL + url
  }
  // 无 CDN 时，确保路径以 /upload/ 或 /uploads/ 开头走 Vite 代理
  return ensureUploadPrefix(url)
}

export const getImageThumbnailUrl = (url, width = 360) => {
  const source = getFullImageUrl(url)
  if (!source) return ''
  if (source.startsWith('/images/')) return source

  try {
    const parsed = new URL(source, window.location.origin)
    if (!parsed.pathname.startsWith('/upload/')) {
      return source
    }
    return `${parsed.origin}/api/file/upload/thumb?url=${encodeURIComponent(source)}&w=${width}`
  } catch (error) {
    if (source.startsWith('/upload/') || source.startsWith('upload/')) {
      return `/api/file/upload/thumb?url=${encodeURIComponent(source)}&w=${width}`
    }
    return source
  }
}

// 确保路径经过 upload 代理
const ensureUploadPrefix = (path) => {
  if (path.startsWith('/upload/') || path.startsWith('/uploads/')) return path
  // /images/ 走 admin 服务代理
  if (path.startsWith('/images/')) return path
  // 其他情况补 /upload/ 前缀
  return '/upload' + (path.startsWith('/') ? path : '/' + path)
}

const normalizePrivateUploadUrl = (url) => {
  try {
    const parsed = new URL(url)
    const { hostname, pathname } = parsed
    if (!pathname.startsWith('/upload/') && !pathname.startsWith('/uploads/')) {
      return ''
    }
    const isLoopbackHost = ['localhost', '127.0.0.1', '0.0.0.0'].includes(hostname)
    const isPrivateIpv4Host =
      hostname.startsWith('192.168.') ||
      hostname.startsWith('10.') ||
      /^172\.(1[6-9]|2\d|3[0-1])\./.test(hostname)
    return (isLoopbackHost || isPrivateIpv4Host) ? pathname : ''
  } catch (error) {
    return ''
  }
}

export default request
