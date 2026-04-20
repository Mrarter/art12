/**
 * 艺荐官 API
 * 接口: 5.11节 艺荐官模块
 * 注意: 网关路由 /api/promotion/** -> shiyiju-promotion (路径: /promotion/**)
 */
import request from './request'

/**
 * 获取推广中心数据
 * GET /promotion/center
 */
export const getPromoterCenter = () => {
  return request.get('/promotion/center')
}

/**
 * 获取业绩详情
 * GET /promotion/performance
 * @param {Object} params
 * @param {string} params.period - 时间周期: today/week/month
 */
export const getPromoterPerformance = (params) => {
  return request.get('/promotion/performance', params)
}

/**
 * 获取佣金明细
 * GET /promotion/commission-log
 * @param {Object} params
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 */
export const getCommissionLog = (params) => {
  return request.get('/promotion/commission-log', params)
}

/**
 * 获取团队列表
 * GET /promotion/team
 * @param {Object} params
 * @param {number} params.level - 团队层级: 1-一级, 2-二级
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 */
export const getPromoterTeam = (params) => {
  return request.get('/promotion/team', params)
}

/**
 * 获取提现账户信息
 * GET /promotion/withdraw-account
 */
export const getWithdrawAccount = () => {
  return request.get('/promotion/withdraw-account')
}

/**
 * 申请提现
 * POST /promotion/withdraw
 * @param {Object} data
 * @param {number} data.amount - 提现金额
 * @param {number} data.type - 提现方式: 1-微信, 2-支付宝, 3-银行卡
 * @param {string} data.account - 收款账户
 * @param {string} data.realName - 真实姓名
 */
export const applyWithdraw = (data) => {
  return request.post('/promotion/withdraw', data)
}

/**
 * 获取提现记录
 * GET /promotion/withdraw-log
 * @param {Object} params
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 */
export const getWithdrawLog = (params) => {
  return request.get('/promotion/withdraw-log', params)
}

/**
 * 获取推广素材
 * GET /promotion/materials
 */
export const getPromoterMaterials = () => {
  return request.get('/promotion/materials')
}

/**
 * 签署分销协议
 * POST /promotion/sign-agreement
 */
export const signAgreement = () => {
  return request.post('/promotion/sign-agreement')
}

// 获取艺荐官统计数据
export const getPromoterStats = () => {
  return getPromoterCenter()
}

// 获取推广首页数据
export const getPromoterHome = () => {
  return getPromoterCenter()
}

// 获取佣金列表
export const getCommissionList = (params) => {
  return getCommissionLog(params)
}

// 获取团队列表
export const getTeamList = (params) => {
  return getPromoterTeam(params)
}
