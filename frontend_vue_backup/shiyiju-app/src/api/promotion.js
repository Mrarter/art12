/**
 * 分销相关 API
 */
import { get, post } from './request'

/**
 * 获取艺荐官中心数据
 */
export function getPromoterCenter() {
  return get('/promotion/center')
}

/**
 * 获取佣金明细
 */
export function getCommissionLogs(page = 1, pageSize = 20) {
  return get('/promotion/commission-log', { page, pageSize })
}

/**
 * 获取团队列表
 */
export function getTeamList() {
  return get('/promotion/team')
}

/**
 * 申请提现
 */
export function applyWithdraw(data) {
  return post('/promotion/withdraw', data)
}

/**
 * 获取提现记录
 */
export function getWithdrawLogs() {
  return get('/promotion/withdraw-log')
}

/**
 * 获取推广素材
 */
export function getMaterials() {
  return get('/promotion/materials')
}
