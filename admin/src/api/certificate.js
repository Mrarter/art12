import request from './request'

/**
 * 收藏证书管理 API
 */
export const getCertList = (params) => {
  return request.get('/certificate/list', { params })
}

export const getCertDetail = (id) => {
  return request.get(`/certificate/${id}`)
}

export const generateCert = (data) => {
  return request.post('/certificate/generate', data)
}
