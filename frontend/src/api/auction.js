/**
 * 拍卖模块 API
 */
import request from './request'

const decodeMaybeMojibake = (value) => {
  if (typeof value !== 'string') return value
  if (!/[ÃÂåäæçèéêëìíîïðñòóôõöøùúûüýÿ]/.test(value)) return value
  try {
    return decodeURIComponent(escape(value))
  } catch (e) {
    return value
  }
}

const normalizeText = (value) => {
  if (Array.isArray(value)) return value.map(normalizeText)
  if (value && typeof value === 'object') {
    const result = {}
    Object.keys(value).forEach(key => {
      result[key] = normalizeText(value[key])
    })
    return result
  }
  return decodeMaybeMojibake(value)
}

/**
 * 获取拍卖专场列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 * @param {number} params.status - 状态：0-预展中 1-进行中 2-已结束
 */
export function getAuctionSessions(params) {
  return request({
    url: '/auction/sessions',
    method: 'GET',
    data: params
  }).then(normalizeText)
}

/**
 * 获取专场详情
 * @param {number} sessionId - 专场ID
 */
export function getSessionDetail(sessionId) {
  return request({
    url: `/auction/sessions/${sessionId}`,
    method: 'GET'
  }).then(normalizeText)
}

/**
 * 获取专场下的拍品
 * @param {number} sessionId - 专场ID
 */
export function getSessionLots(sessionId) {
  return request({
    url: `/auction/sessions/${sessionId}/lots`,
    method: 'GET'
  }).then(normalizeText)
}

/**
 * 获取拍品详情
 * @param {number} lotId - 拍品ID
 */
export function getLotDetail(lotId) {
  return request({
    url: `/auction/lots/${lotId}`,
    method: 'GET'
  })
}

/**
 * 缴纳保证金
 * @param {number} sessionId - 专场ID
 */
export function payDeposit(sessionId) {
  return request({
    url: `/auction/sessions/${sessionId}/deposit`,
    method: 'POST'
  })
}

/**
 * 竞拍出价
 * @param {number} lotId - 拍品ID
 * @param {number} bidPrice - 出价金额
 */
export function placeBid(lotId, bidPrice) {
  return request({
    url: `/auction/lots/${lotId}/bid`,
    method: 'POST',
    data: { bidPrice }
  })
}

/**
 * 获取出价记录
 * @param {number} lotId - 拍品ID
 * @param {number} page - 页码
 * @param {number} pageSize - 每页数量
 */
export function getLotBids(lotId, page = 1, pageSize = 20) {
  return request({
    url: `/auction/lots/${lotId}/bids`,
    method: 'GET',
    data: { page, pageSize }
  })
}

/**
 * 获取我的竞拍
 * @param {number} page - 页码
 * @param {number} pageSize - 每页数量
 */
export function getMyBids(page = 1, pageSize = 20) {
  return request({
    url: '/auction/my-bids',
    method: 'GET',
    data: { page, pageSize }
  })
}

/**
 * 获取竞拍提醒列表
 */
export function getAuctionReminders() {
  return request({
    url: '/auction/reminders',
    method: 'GET'
  })
}

/**
 * 设置竞拍提醒
 * @param {Object} data - 提醒设置
 */
export function setReminder(data) {
  return request({
    url: '/auction/reminders',
    method: 'POST',
    data
  })
}

/**
 * 更新竞拍提醒
 * @param {number} id - 提醒ID
 * @param {Object} data - 更新内容
 */
export function updateReminder(id, data) {
  return request({
    url: `/auction/reminders/${id}`,
    method: 'PUT',
    data
  })
}

/**
 * 删除竞拍提醒
 * @param {number} id - 提醒ID
 */
export function deleteReminder(id) {
  return request({
    url: `/auction/reminders/${id}`,
    method: 'DELETE'
  })
}
