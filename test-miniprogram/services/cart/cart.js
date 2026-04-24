import { config } from '../../config/index';

/** 获取购物车mock数据 */
function mockFetchCartGroupData(params) {
  const { delay } = require('../_utils/delay');
  const { genCartGroupData } = require('../../model/cart');

  return delay().then(() => genCartGroupData(params));
}

/** 购物车 - 真实API */
function realFetchCartGroupData(params) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    wx.request({
      url: `${config.apiBase}/cart/list`,
      data: params,
      method: 'GET',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        if (res.statusCode === 200) {
          const records = res.data?.data || [];
          resolve({
            cartGroup: records,
          });
        } else {
          reject(res);
        }
      },
      fail: reject
    });
  });
}

/** 获取购物车数据 */
export function fetchCartGroupData(params) {
  if (config.useMock) {
    return mockFetchCartGroupData(params);
  }
  return realFetchCartGroupData(params);
}
