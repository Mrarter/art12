/**
 * 收款账户 API
 */
import request from './request'

/** 添加收款账户 */
export const addPayAccount = (data) => {
  return request({
    url: '/user/pay-account/add',
    method: 'POST',
    requireAuth: true,
    data
  })
}

/** 绑定当前登录微信为收款账户 */
export const bindWechatPayAccount = (data = {}) => {
  return request({
    url: '/user/pay-account/bind-wechat',
    method: 'POST',
    requireAuth: true,
    data
  })
}

/** 通过微信授权 code 绑定当前登录账号 */
export const bindWechatPayAccountByCode = (data = {}) => {
  return request({
    url: '/user/pay-account/bind-wechat-code',
    method: 'POST',
    requireAuth: true,
    data
  })
}

/** 获取账户列表 */
export const getPayAccountList = () => {
  return request({
    url: '/user/pay-account/list',
    requireAuth: true
  })
}

/** 删除账户 */
export const deletePayAccount = (id) => {
  return request({
    url: '/user/pay-account/delete',
    method: 'POST',
    requireAuth: true,
    data: { id }
  })
}

/** 设置默认账户 */
export const setDefaultPayAccount = (id) => {
  return request({
    url: '/user/pay-account/default',
    method: 'POST',
    requireAuth: true,
    data: { id }
  })
}

/** 获取默认账户 */
export const getDefaultPayAccount = () => {
  return request({
    url: '/user/pay-account/default',
    requireAuth: true
  })
}
