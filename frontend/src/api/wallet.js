/**
 * 钱包 API
 */
import request from './request'

/** 获取钱包信息 */
export const getWalletInfo = () => {
  return request({
    url: '/user/wallet/info'
  })
}

/** 获取钱包流水 */
export const getWalletBills = (page = 1, pageSize = 20) => {
  return request({
    url: `/user/wallet/bills?page=${page}&pageSize=${pageSize}`
  })
}
