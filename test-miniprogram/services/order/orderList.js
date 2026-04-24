import { config } from '../../config/index';

/** 获取订单列表mock数据 */
function mockFetchOrders(params) {
  const { delay } = require('../_utils/delay');
  const { genOrders } = require('../../model/order/orderList');

  return delay(200).then(() => genOrders(params));
}

/** 订单列表 - 真实API */
function realFetchOrders(params) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    wx.request({
      url: `${config.apiBase}/order/list`,
      data: params,
      method: 'GET',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        if (res.statusCode === 200) {
          const records = res.data?.data?.records || [];
          resolve({
            orderList: records.map(item => ({
              id: item.id,
              orderNo: item.orderNo,
              status: item.status,
              statusText: item.statusText,
              totalPrice: item.totalPrice,
              goodsList: item.items || [],
              createTime: item.createTime,
            })),
            totalCount: res.data?.data?.total || 0,
          });
        } else {
          reject(res);
        }
      },
      fail: reject
    });
  });
}

/** 获取订单列表数据 */
export function fetchOrders(params) {
  if (config.useMock) {
    return mockFetchOrders(params);
  }
  return realFetchOrders(params);
}

/** 获取订单列表mock数据 */
function mockFetchOrdersCount(params) {
  const { delay } = require('../_utils/delay');
  const { genOrdersCount } = require('../../model/order/orderList');

  return delay().then(() => genOrdersCount(params));
}

/** 订单统计 - 真实API */
function realFetchOrdersCount(params) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    wx.request({
      url: `${config.apiBase}/order/count`,
      method: 'GET',
      data: params,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data?.data || {});
        } else {
          reject(res);
        }
      },
      fail: reject
    });
  });
}

/** 获取订单列表统计 */
export function fetchOrdersCount(params) {
  if (config.useMock) {
    return mockFetchOrdersCount(params);
  }
  return realFetchOrdersCount(params);
}
