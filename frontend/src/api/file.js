/**
 * 文件上传 API
 */
import request from './request'

const DEV_LAN_HOST = '192.168.1.109'
const API_ORIGIN = process.env.UNI_PLATFORM === 'mp-weixin'
  ? `http://${DEV_LAN_HOST}:8080`
  : 'http://127.0.0.1:8080'
const BASE_URL = API_ORIGIN + '/api'
const LOCAL_FILE_ORIGIN = 'http://localhost:8087'
const FILE_BASE_URL = process.env.UNI_PLATFORM === 'mp-weixin'
  ? `http://${DEV_LAN_HOST}:8087`
  : 'http://127.0.0.1:8087'

const normalizeFileUrl = (url) => {
  return typeof url === 'string' && url.startsWith(LOCAL_FILE_ORIGIN)
    ? FILE_BASE_URL + url.slice(LOCAL_FILE_ORIGIN.length)
    : url
}

const parseUploadResponse = (responseText) => {
  const data = typeof responseText === 'string' ? JSON.parse(responseText) : responseText
  if (data.code === 200 && data.data) {
    return normalizeFileUrl(data.data.url || data.data)
  }
  throw new Error(data.message || '上传失败')
}

const uploadFileByXHR = async (filePath, type, token) => {
  const blob = await fetch(filePath).then(res => {
    if (!res.ok) throw new Error('读取图片失败')
    return res.blob()
  })
  const filename = `artwork-${Date.now()}.${(blob.type.split('/')[1] || 'jpg').replace('jpeg', 'jpg')}`
  const formData = new FormData()
  formData.append('file', blob, filename)
  formData.append('type', type)

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', BASE_URL + '/product/upload')
    if (token) {
      xhr.setRequestHeader('Authorization', 'Bearer ' + token)
    }
    xhr.onload = () => {
      try {
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve(parseUploadResponse(xhr.responseText))
        } else {
          reject(new Error(`上传失败: HTTP ${xhr.status}`))
        }
      } catch (e) {
        reject(e)
      }
    }
    xhr.onerror = () => reject(new Error('上传失败: 网络错误'))
    xhr.send(formData)
  })
}

/**
 * 上传文件
 * @param {string} filePath - 文件临时路径
 * @param {string} type - 文件类型 (image/audio/video)
 * @returns {Promise<string>} 文件URL
 */
export const uploadFile = (filePath, type = 'image') => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')

    // H5 选择图片得到的是 blob: 临时地址，uni.uploadFile 在该环境下容易直接 fail。
    if (process.env.UNI_PLATFORM === 'h5') {
      uploadFileByXHR(filePath, type, token)
        .then(resolve)
        .catch((err) => {
          console.error('上传失败:', err)
          uni.showToast({ title: err.message || '上传失败', icon: 'none' })
          reject(err)
        })
      return
    }
    
    uni.uploadFile({
      url: BASE_URL + '/product/upload',
      filePath: filePath,
      name: 'file',
      header: {
        'Authorization': token ? 'Bearer ' + token : ''
      },
      formData: {
        type: type
      },
      success: (res) => {
        try {
          resolve(parseUploadResponse(res.data))
        } catch (e) {
          uni.showToast({ title: e.message || '上传失败', icon: 'none' })
          reject(e)
        }
      },
      fail: (err) => {
        console.error('上传失败:', err)
        uni.showToast({ title: '上传失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

/**
 * 获取文件上传凭证
 * @param {string} filename - 文件名
 * @param {string} type - 文件类型
 */
export const getUploadToken = (filename, type = 'image') => {
  return request.get('/file/token', { filename, type })
}

/**
 * 上传到七牛云
 * @param {string} token - 上传凭证
 * @param {string} filePath - 文件路径
 */
export const uploadToQiniu = (token, filePath) => {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: 'https://up-z2.qiniup.com',
      filePath: filePath,
      name: 'file',
      formData: {
        token: token
      },
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          if (data.key) {
            resolve(data.key)
          } else {
            reject(new Error('上传失败'))
          }
        } catch (e) {
          reject(e)
        }
      },
      fail: reject
    })
  })
}
