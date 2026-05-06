import request from './request'

/**
 * 涨价规则配置 API
 */

// 获取价格增长配置
export const getPriceRule = () => {
  return request.get('/config/priceGrowth')
}

// 保存价格增长配置
export const savePriceRule = (data) => {
  return request.post('/config/priceGrowth', data)
}
