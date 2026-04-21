/**
 * 艺荐官/推广员相关 API
 */
import request from './request'

/**
 * 获取艺荐官中心首页数据
 * GET /promoter/center
 */
export const getPromoterCenter = () => {
  return request.get('/promoter/center')
}

/**
 * 获取艺荐官统计数据
 * GET /promoter/stats
 */
export const getPromoterStats = () => {
  return request.get('/promoter/stats')
}

/**
 * 获取收益趋势
 * GET /promoter/earnings/trend
 * @param {string} period - 时间周期: week/month/quarter/year
 */
export const getEarningsTrend = (period = 'month') => {
  return request.get('/promoter/earnings/trend', { period })
}

/**
 * 获取收益明细列表
 * GET /promoter/earnings
 * @param {Object} params
 * @param {string} params.type - 收益类型: order/invite/team
 * @param {number} params.page
 * @param {number} params.pageSize
 */
export const getEarningsList = (params) => {
  return request.get('/promoter/earnings', params)
}

/**
 * 获取团队列表
 * GET /promoter/team
 * @param {number} params.page
 * @param {number} params.pageSize
 */
export const getTeamList = (params) => {
  return request.get('/promoter/team', params)
}

/**
 * 获取团队成员详情
 * GET /promoter/team/{userId}
 */
export const getTeamMemberDetail = (userId) => {
  return request.get(`/promoter/team/${userId}`)
}

/**
 * 获取下级艺荐官列表
 * GET /promoter/team/subordinate
 * @param {number} params.level - 级别: 1/2
 */
export const getSubordinateList = (params) => {
  return request.get('/promoter/team/subordinate', params)
}

/**
 * 提现申请
 * POST /promoter/withdraw
 * @param {number} amount - 提现金额
 * @param {string} accountType - 账户类型: alipay/bank/wechat
 * @param {string} accountInfo - 账户信息
 */
export const withdrawApply = (data) => {
  return request.post('/promoter/withdraw', data)
}

/**
 * 获取提现记录
 * GET /promoter/withdraw/list
 */
export const getWithdrawList = (params) => {
  return request.get('/promoter/withdraw/list', params)
}

/**
 * 绑定艺荐官关系
 * POST /promoter/bind
 * @param {string} code - 推荐码
 */
export const bindPromoter = (code) => {
  return request.post('/promoter/bind', { code })
}

/**
 * 获取我的推荐码
 * GET /promoter/code
 */
export const getMyCode = () => {
  return request.get('/promoter/code')
}

/**
 * 获取佣金配置
 * GET /promoter/config
 */
export const getCommissionConfig = () => {
  return request.get('/promoter/config')
}

/**
 * 获取邀请海报
 * GET /promoter/poster
 */
export const getInvitePoster = () => {
  return request.get('/promoter/poster')
}

/**
 * 获取邀请二维码
 * GET /promoter/qrcode
 * @param {string} inviteCode - 邀请码
 */
export const getQrCode = (inviteCode) => {
  return request.get('/promoter/qrcode', inviteCode)
}

/**
 * 获取订单关联的佣金记录
 * GET /promoter/order-commission
 * @param {string} orderId - 订单ID
 */
export const getOrderCommission = (orderId) => {
  return request.get('/promoter/order-commission', { orderId })
}

/**
 * 获取商品佣金信息
 * GET /promoter/product-commission
 * @param {number} productId - 商品ID
 */
export const getProductCommission = (productId) => {
  return request.get('/promoter/product-commission', { productId })
}

/**
 * 获取佣金配置
 * GET /promoter/commission/config
 */
export const getCommissionConfig = () => {
  return request.get('/promoter/commission/config')
}

/**
 * 获取佣金明细列表
 * GET /promoter/commission/list
 * @param {Object} params
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {number} params.level - 佣金级别：1-一级 2-二级
 */
export const getCommissionList = (params) => {
  return request.get('/promoter/commission/list', params)
}