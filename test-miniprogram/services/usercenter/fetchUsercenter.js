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
      url: `${config.apiBase}/user/info`,
      method: 'GET',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        if (res.statusCode === 200) {
          const user = res.data?.data || {};
          resolve({
            userInfo: {
              avatarUrl: user.avatar,
              nickName: user.nickname,
              phoneNumber: user.phone,
            },
            // 统计数据需要从其他接口获取
            infoList: [
              { title: '我的收藏', tip: '0', url: '/pages/user/favorites/index' },
              { title: '我的关注', tip: '0', url: '/pages/user/following/index' },
            ]
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
