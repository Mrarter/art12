/**
 * 艺荐官 API
 * 接口: 5.11节 艺荐官模块
 */
import request from './request'

/**
 * 获取推广中心数据
 * GET /promoter/center
 */
export const getPromoterCenter = () => {
  return request.get('/promoter/center')
}

/**
 * 获取业绩详情
 * GET /promoter/performance
 * @param {Object} params
 * @param {string} params.period - 时间周期: today/week/month
 */
export const getPromoterPerformance = (params) => {
  return request.get('/promoter/performance', params)
}

/**
 * 获取佣金明细
 * GET /promoter/commission-log
 * @param {Object} params
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 */
export const getCommissionLog = (params) => {
  return request.get('/promoter/commission-log', params)
}

/**
 * 获取团队列表
 * GET /promoter/team
 * @param {Object} params
 * @param {number} params.level - 团队层级: 1-一级, 2-二级
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 */
export const getPromoterTeam = (params) => {
  return request.get('/promoter/team', params)
}

/**
 * 获取提现账户信息
 * GET /promoter/withdraw-account
 */
export const getWithdrawAccount = () => {
  return request.get('/promoter/withdraw-account')
}

/**
 * 申请提现
 * POST /promoter/withdraw
 * @param {Object} data
 * @param {number} data.amount - 提现金额
 * @param {number} data.type - 提现方式: 1-微信, 2-支付宝, 3-银行卡
 * @param {string} data.account - 收款账户
 * @param {string} data.realName - 真实姓名
 */
export const applyWithdraw = (data) => {
  return request.post('/promoter/withdraw', data)
}

/**
 * 获取提现记录
 * GET /promoter/withdraw-log
 * @param {Object} params
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 */
export const getWithdrawLog = (params) => {
  return request.get('/promoter/withdraw-log', params)
}

/**
 * 获取推广素材
 * GET /promoter/materials
 */
export const getPromoterMaterials = () => {
  return request.get('/promoter/materials')
}

/**
 * 签署分销协议
 * POST /promoter/sign-agreement
 */
export const signAgreement = () => {
  return request.post('/promoter/sign-agreement')
}
