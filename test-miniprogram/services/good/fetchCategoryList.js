import { config } from '../../config/index';

/** 获取商品列表 */
function mockFetchGoodCategory() {
  const { delay } = require('../_utils/delay');
  const { getCategoryList } = require('../../model/category');
  return delay().then(() => getCategoryList());
}

/** 分类列表 - 真实API */
function realFetchGoodCategory() {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${config.apiBase}/product/categories`,
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data?.data || []);
        } else {
          reject(res);
        }
      },
      fail: reject
    });
  });
}

/** 获取商品列表 */
export function getCategoryList() {
  if (config.useMock) {
    return mockFetchGoodCategory();
  }
  return realFetchGoodCategory();
}
