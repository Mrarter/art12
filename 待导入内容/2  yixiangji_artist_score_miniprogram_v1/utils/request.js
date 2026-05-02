const BASE_URL = 'https://api.example.com';

function request(options) {
  const token = wx.getStorageSync('token') || '';

  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data);
        } else {
          wx.showToast({
            title: res.data?.message || '请求失败',
            icon: 'none'
          });
          reject(res);
        }
      },
      fail(err) {
        wx.showToast({
          title: '网络异常',
          icon: 'none'
        });
        reject(err);
      }
    });
  });
}

module.exports = request;
