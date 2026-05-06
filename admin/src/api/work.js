import request from './request'

/**
 * 作品管理 API
 */
export const getWorkList = (params) => {
  return request.get('/product/list', { params })
}

export const updateWorkStatus = (id, status) => {
  return request.post('/product/update-status', { id, status })
}
