import { config } from '../config/index';

/**
 * 通用请求方法
 * @param {string} url - 请求路径
 * @param {object} data - 请求数据
 * @param {string} method - 请求方法
 */
export function request(url, data = {}, method = 'GET') {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${config.apiBase}${url}`,
      data,
      method,
      header: {
        'Content-Type': 'application/json',
      },
      success: (res) => {
        if (res.statusCode === 200 && res.data.code === 0) {
          resolve(res.data.data);
        } else if (res.data.code === 401) {
          wx.navigateTo({ url: '/pages/login/index' });
          reject(res.data);
        } else {
          wx.showToast({ title: res.data.message || '请求失败', icon: 'none' });
          reject(res.data);
        }
      },
      fail: (err) => {
        console.error('请求失败:', err);
        wx.showToast({ title: '网络请求失败', icon: 'none' });
        reject(err);
      }
    });
  });
}
