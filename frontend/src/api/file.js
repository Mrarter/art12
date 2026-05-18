/**
 * 文件上传 API
 */
import request from './request'

const IS_MP = process.env.UNI_PLATFORM === 'mp-weixin'
const DEV_LAN_HOST = import.meta.env?.VITE_DEV_LAN_HOST || '192.168.1.144'

// 小程序用 HTTP 直连（开发环境），H5 用相对路径
const MP_GATEWAY_ORIGIN = import.meta.env?.VITE_MP_GATEWAY_ORIGIN || `http://${DEV_LAN_HOST}:9443`
const MP_FILE_ORIGIN = import.meta.env?.VITE_MP_FILE_ORIGIN || `http://${DEV_LAN_HOST}:9447`

const API_ORIGIN = IS_MP ? `${MP_GATEWAY_ORIGIN}/api` : ''
const FILE_BASE_URL = IS_MP ? MP_FILE_ORIGIN : ''

const normalizeFileUrl = (url) => {
  if (!IS_MP) return url
  if (typeof url !== 'string') return url
  if (url.startsWith('http://localhost:8087') || url.startsWith('http://127.0.0.1:8087')) {
    return FILE_BASE_URL + url.slice(url.indexOf(':8087') + 5)
  }
  if (url.startsWith('/upload/')) return FILE_BASE_URL + url
  if (url.startsWith('upload/')) return FILE_BASE_URL + '/' + url
  if (url.match(/^http:\/\/192\.168\.\d+\.\d+:8087/)) {
    return url.replace(/^http:\/\/192\.168\.\d+\.\d+:8087/, FILE_BASE_URL)
  }
  return url
}

const parseUploadResponse = (responseText) => {
  const data = typeof responseText === 'string' ? JSON.parse(responseText) : responseText
  if (data.code === 200 && data.data) {
    return normalizeFileUrl(data.data.url || data.data)
  }
  throw new Error(data.message || '上传失败')
}

/**
 * H5 上传（使用 fetch）
 */
const uploadFileByFetch = async (filePath, type, token) => {
  const response = await fetch(filePath)
  if (!response.ok) throw new Error('读取图片失败: HTTP ' + response.status)
  const blob = await response.blob()
  const filename = `artwork-${Date.now()}.${(blob.type.split('/')[1] || 'jpg').replace('jpeg', 'jpg')}`
  const formData = new FormData()
  formData.append('file', blob, filename)
  formData.append('type', type)

  const headers = {}
  if (token) headers['Authorization'] = 'Bearer ' + token

  const uploadUrl = API_ORIGIN ? API_ORIGIN + '/product/upload' : '/api/product/upload'
  const uploadRes = await fetch(uploadUrl, { method: 'POST', headers, body: formData })
  if (!uploadRes.ok) throw new Error('上传失败: HTTP ' + uploadRes.status)
  return parseUploadResponse(await uploadRes.text())
}

/**
 * 打开图片裁剪器
 */
export const openCropper = (src, opts = {}) => {
  return new Promise((resolve, reject) => {
    const { ratio = 'auto', shape = 'square' } = opts
    const route = encodeURIComponent(src)
    const handler = (result) => { uni.$off('cropResult', handler); resolve(result) }
    uni.$on('cropResult', handler)
    uni.navigateTo({
      url: `/pages/common/cropper?src=${route}&ratio=${ratio}&shape=${shape}`,
      events: { onCrop: (result) => { uni.$off('cropResult', handler); resolve(result) } },
      fail: (err) => { uni.$off('cropResult', handler); reject(err) }
    })
  })
}

/**
 * 上传文件
 */
export const uploadFile = (filePath, type = 'image') => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    const uploadUrl = API_ORIGIN ? API_ORIGIN + '/product/upload' : '/api/product/upload'
    console.log('[UPLOAD] 上传地址:', uploadUrl, '平台:', process.env.UNI_PLATFORM)

    if (process.env.UNI_PLATFORM === 'h5') {
      return uploadFileByFetch(filePath, type, token).then(resolve).catch((err) => {
        console.error('[UPLOAD] 失败:', err)
        uni.showToast({ title: err.message || '上传失败', icon: 'none' })
        reject(err)
      })
    }

    // 小程序上传
    uni.uploadFile({
      url: uploadUrl,
      filePath: filePath,
      name: 'file',
      header: { 'Authorization': token ? 'Bearer ' + token : '' },
      formData: { type },
      success: (res) => {
        try {
          resolve(parseUploadResponse(res.data))
        } catch (e) {
          uni.showToast({ title: e.message || '上传失败', icon: 'none' })
          reject(e)
        }
      },
      fail: (err) => {
        console.error('[UPLOAD] 失败:', err)
        uni.showToast({ title: '上传失败: ' + (err.errMsg || '网络错误'), icon: 'none' })
        reject(err)
      }
    })
  })
}

/**
 * 获取文件上传凭证
 */
export const getUploadToken = (filename, type = 'image') => {
  return request.get('/file/token', { filename, type })
}

/**
 * 上传到七牛云
 */
export const uploadToQiniu = (token, filePath) => {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: 'https://up-z2.qiniup.com',
      filePath: filePath,
      name: 'file',
      formData: { token },
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          data.key ? resolve(data.key) : reject(new Error('上传失败'))
        } catch (e) { reject(e) }
      },
      fail: reject
    })
  })
}
