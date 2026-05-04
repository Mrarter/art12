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
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  error => {
    if (error.code === 'ERR_NETWORK' || error.message === 'Network Error' || error.code === 'ECONNABORTED') {
      console.warn('后端服务未启动，部分功能可能不可用')
      return Promise.reject(new Error('backend_offline'))
    }
    if (error.response?.status === 401) {
      localStorage.removeItem('admin_token')
      window.location.href = '/login'
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export { requestApi }

// 文件上传方法（直接调用后端上传接口）
export const uploadFile = async (file, onProgress) => {
  const formData = new FormData()
  formData.append('file', file)

  const response = await axios.post('/api/product/upload', formData, {
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
  // 如果已经是完整 URL（包含 http），直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  // 否则拼接 CDN 地址
  return CDN_URL + url
}

export default request
