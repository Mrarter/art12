/**
 * 文件上传 API
 */
import request from './request'

// API 地址
const BASE_URL = (typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE_URL) || 'http://localhost:8080/api'

/**
 * 上传文件
 * @param {string} filePath - 文件临时路径
 * @param {string} type - 文件类型 (image/audio/video)
 * @returns {Promise<string>} 文件URL
 */
export const uploadFile = (filePath, type = 'image') => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    
    uni.uploadFile({
      url: BASE_URL + '/file/upload',
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
          const data = JSON.parse(res.data)
          if (data.code === 200 && data.data) {
            resolve(data.data)
          } else {
            uni.showToast({ title: '上传失败', icon: 'none' })
            reject(new Error('上传失败'))
          }
        } catch (e) {
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
