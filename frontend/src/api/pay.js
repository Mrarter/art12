/**
 * 收款账户 API
 */
import request from './request'

/** 添加收款账户 */
export const addPayAccount = (data) => {
  return request({
    url: '/user/pay-account/add',
    method: 'POST',
    data
  })
}

/** 获取账户列表 */
export const getPayAccountList = () => {
  return request({
    url: '/user/pay-account/list'
  })
}

/** 删除账户 */
export const deletePayAccount = (id) => {
  return request({
    url: '/user/pay-account/delete',
    method: 'POST',
    data: { id }
  })
}

/** 设置默认账户 */
export const setDefaultPayAccount = (id) => {
  return request({
    url: '/user/pay-account/default',
    method: 'POST',
    data: { id }
  })
}

/** 获取默认账户 */
export const getDefaultPayAccount = () => {
  return request({
    url: '/user/pay-account/default'
  })
}
