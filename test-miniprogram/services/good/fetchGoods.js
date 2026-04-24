import { config } from '../../config/index';

/** 获取商品列表 */
function mockFetchGoodsList(pageIndex = 1, pageSize = 20) {
  const { delay } = require('../_utils/delay');
  const { getGoodsList } = require('../../model/goods');
  return delay().then(() =>
    getGoodsList(pageIndex, pageSize).map((item) => {
      return {
        spuId: item.spuId,
        thumb: item.primaryImage,
        title: item.title,
        price: item.minSalePrice,
        originPrice: item.maxLinePrice,
        tags: item.spuTagList.map((tag) => tag.title),
      };
    }),
  );
}

/** 获取商品列表 - 真实API */
function realFetchGoodsList(pageIndex = 1, pageSize = 20) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${config.apiBase}/product/list`,
      data: { page: pageIndex, size: pageSize, status: 1 },
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200) {
          // 后端返回格式: { code: 0, data: { records: [], total: n } }
          const records = res.data?.data?.records || [];
          resolve(records.map(item => ({
            spuId: item.id,
            thumb: item.coverImage,
            title: item.title,
            price: item.price || 0,
            originPrice: item.originalPrice || 0,
            authorName: item.authorName,
            artType: item.artType,
            size: item.size,
            isHot: item.isHot,
            isNew: item.isNew,
          })));
        } else {
          reject(res);
        }
      },
      fail: reject
    });
  });
}

/** 获取商品列表 */
export function fetchGoodsList(pageIndex = 1, pageSize = 20) {
  if (config.useMock) {
    return mockFetchGoodsList(pageIndex, pageSize);
  }
  return realFetchGoodsList(pageIndex, pageSize);
}
