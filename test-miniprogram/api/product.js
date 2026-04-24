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

// 获取首页 Banner
export const getBanners = () => 
  request({ url: '/product/homepage/banners' }).then(data => data || []);

export const getBannersDirect = () => request({ url: '/product/homepage/banners' });

// 获取推荐作品列表
export const getRecommend = (params) => 
  request({ url: '/product/recommend', data: params }).then(data => data?.records || []);

export const getRecommendDirect = (params) => request({ url: '/product/recommend', data: params });

// 获取关注艺术家的作品
export const getFollowingWorks = (params) => 
  request({ url: '/product/following', data: params }).then(data => data?.records || []);

export const getFollowingWorksDirect = (params) => request({ url: '/product/following', data: params });

// 获取作品列表
export const getProductList = (params) => 
  request({ url: '/product/list', data: params }).then(data => data?.records || []);

export const getProductListDirect = (params) => request({ url: '/product/list', data: params });

// 获取作品详情
export const getProductDetail = (id) => request({ url: `/product/${id}` });

// 搜索作品
export const searchProducts = (params) => request({ url: '/product/search', data: params });

// 获取分类列表
export const getCategories = () => request({ url: '/product/categories' });

// 获取作品评论
export const getProductComments = (params) => request({ url: '/review/artwork', data: params });

// 获取作品评论统计
export const getProductCommentStats = (artworkId) => request({ url: `/review/artwork/${artworkId}/stats` });

// 收藏作品
export const favoriteProduct = (artworkId) => request({ url: '/product/favorite', method: 'POST', data: artworkId });

// 取消收藏
export const unfavoriteProduct = (artworkId) => request({ url: `/product/favorite/${artworkId}`, method: 'DELETE' });

// 获取我的收藏
export const getMyFavorites = (params) => request({ url: '/product/favorites', data: params });
