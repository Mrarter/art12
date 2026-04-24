// 用户状态管理
let userInfo = null;
let token = null;

export function useUserStore() {
  return {
    // 获取 token
    getToken: () => token || wx.getStorageSync('token'),
    
    // 设置 token
    setToken: (newToken) => {
      token = newToken;
      wx.setStorageSync('token', newToken);
    },
    
    // 获取用户信息
    getUserInfo: () => userInfo || wx.getStorageSync('userInfo'),
    
    // 设置用户信息
    setUserInfo: (info) => {
      userInfo = info;
      wx.setStorageSync('userInfo', info);
    },
    
    // 是否已登录
    isAuthenticated: () => !!token || !!wx.getStorageSync('token'),
    
    // 清除登录信息
    clearAuth: () => {
      token = null;
      userInfo = null;
      wx.removeStorageSync('token');
      wx.removeStorageSync('userInfo');
    }
  };
}
