/**
 * 拍卖相关 API
 */
import { get, post } from './request'

/**
 * 获取拍卖专场列表
 */
export function getAuctionSessions(params) {
  return get('/auction/sessions', params)
}

/**
 * 获取专场详情
 */
export function getSessionDetail(sessionId) {
  return get('/auction/sessions/' + sessionId)
}

/**
 * 获取专场下的拍品
 */
export function getSessionLots(sessionId) {
  return get('/auction/sessions/' + sessionId + '/lots')
}

/**
 * 获取拍品详情
 */
export function getLotDetail(lotId) {
  return get('/auction/lots/' + lotId)
}

/**
 * 出价
 */
export function placeBid(lotId, bidPrice) {
  return post('/auction/lots/' + lotId + '/bid', { bidPrice })
}

/**
 * 缴纳保证金
 */
export function payDeposit(lotId) {
  return post('/auction/lots/' + lotId + '/deposit')
}

/**
 * 获取我的竞拍
 */
export function getMyBids() {
  return get('/auction/my-bids')
}
