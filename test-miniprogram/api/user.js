import { config } from '../config/index';

const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    wx.request({
      url: `${config.apiBase}${options.url}`,
      data: options.data,
      method: options.method || 'GET',
      header: {
        'Content-Type': 'application/json',
        ...(token && { Authorization: `Bearer ${token}` }),
        ...options.header
      },
      success: (res) => {
        if (res.statusCode === 200) {
          if (res.data.code === 0 || res.data.code === 200) {
            resolve(res.data.data);
          } else if (res.data.code === 401) {
            wx.navigateTo({ url: '/pages/login/index' });
            reject(res.data);
          } else {
            wx.showToast({ title: res.data.message || '请求失败', icon: 'none' });
            reject(res.data);
          }
        } else {
          reject(res);
        }
      },
      fail: (err) => {
        console.error('请求失败:', err);
        wx.showToast({ title: '网络请求失败', icon: 'none' });
        reject(err);
      }
    });
  });
};

// 用户登录
export const login = (data) => request({ url: '/user/login', method: 'POST', data });

// 微信登录
export const wxLogin = (data) => request({ url: '/user/wxlogin', method: 'POST', data });

// 获取用户信息
export const getUserInfo = () => request({ url: '/user/info' });

// 更新用户信息
export const updateUserInfo = (data) => request({ url: '/user/update', method: 'POST', data });

// 退出登录
export const logout = () => request({ url: '/user/logout', method: 'POST' });

// 获取收藏列表
export const getFavorites = (params) => request({ url: '/user/favorites', data: params });

// 获取已购作品
export const getPurchased = (params) => request({ url: '/user/purchased', data: params });

// 获取艺术家信息
export const getArtistInfo = (userId) => request({ url: `/user/artist/${userId}` });

// 成为艺术家
export const becomeArtist = (data) => request({ url: '/user/become-artist', method: 'POST', data });

// 获取个人中心数据
export const getUserCenter = () => request({ url: '/user/center' });

// 获取收货地址列表
export const getAddressList = () => request({ url: '/user/addresses' });

// 添加收货地址
export const addAddress = (data) => request({ url: '/user/address', method: 'POST', data });

// 删除收货地址
export const deleteAddress = (id) => request({ url: `/user/address/${id}`, method: 'DELETE' });

// 关注艺术家
export const followArtist = (artistId) => request({ url: '/user/follow', method: 'POST', data: { artistId } });

// 取消关注艺术家
export const unfollowArtist = (artistId) => request({ url: '/user/unfollow', method: 'POST', data: { artistId } });

// 绑定手机号
export const bindPhone = (data) => request({ url: '/user/bind-phone', method: 'POST', data });

// 获取关注列表
export const getFollowingList = (params) => request({ url: '/user/following', data: params });

// 手机号登录
export const phoneLogin = (data) => request({ url: '/user/phone-login', method: 'POST', data });

// 发送短信验证码
export const sendSmsCode = (data) => request({ url: '/user/sms-code', method: 'POST', data });
