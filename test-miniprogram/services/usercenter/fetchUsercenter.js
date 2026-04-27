import { config } from '../../config/index';

/** 获取个人中心信息 */
function mockFetchUserCenter() {
  const { delay } = require('../_utils/delay');
  const { genUsercenter } = require('../../model/usercenter');
  return delay(200).then(() => genUsercenter());
}

/** 个人中心 - 真实API */
function realFetchUserCenter() {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    wx.request({
      url: `${config.apiBase}/user/center`,
      method: 'GET',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        if (res.statusCode === 200) {
          const data = res.data?.data || {};
          resolve({
            userInfo: {
              avatarUrl: data.avatar || '',
              nickName: data.nickname || '用户',
              phoneNumber: '',
            },
            countsData: [
              { type: 'address', num: data.addressCount || 0 },
              { type: 'coupon', num: data.couponCount || 0 },
              { type: 'point', num: data.points || 0 },
            ],
            orderTagInfos: [
              { title: '待付款', iconName: 'wallet', orderNum: data.pendingPayCount || 0, tabType: 5, status: 1 },
              { title: '待发货', iconName: 'deliver', orderNum: data.pendingShipCount || 0, tabType: 10, status: 1 },
              { title: '待收货', iconName: 'package', orderNum: data.pendingReceiveCount || 0, tabType: 40, status: 1 },
              { title: '待评价', iconName: 'comment', orderNum: data.pendingReviewCount || 0, tabType: 60, status: 1 },
              { title: '退款/售后', iconName: 'exchang', orderNum: data.refundCount || 0, tabType: 0, status: 1 },
            ],
            customerServiceInfo: {
              servicePhone: '400-888-8888'
            }
          });
        } else {
          reject(res);
        }
      },
      fail: reject
    });
  });
}

/** 获取个人中心信息 */
export function fetchUserCenter() {
  if (config.useMock) {
    return mockFetchUserCenter();
  }
  return realFetchUserCenter();
}
