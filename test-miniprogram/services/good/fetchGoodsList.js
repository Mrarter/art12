/* eslint-disable no-param-reassign */
import { config } from '../../config/index';

/** 获取商品列表 */
function mockFetchGoodsList(params) {
  const { delay } = require('../_utils/delay');
  const { getSearchResult } = require('../../model/search');

  const data = getSearchResult(params);

  if (data.spuList.length) {
    data.spuList.forEach((item) => {
      item.spuId = item.spuId;
      item.thumb = item.primaryImage;
      item.title = item.title;
      item.price = item.minSalePrice;
      item.originPrice = item.maxLinePrice;
      item.desc = '';
      if (item.spuTagList) {
        item.tags = item.spuTagList.map((tag) => tag.title);
      } else {
        item.tags = [];
      }
    });
  }
  return delay().then(() => {
    return data;
  });
}

/** 获取商品列表 - 真实API */
function realFetchGoodsList(params) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${config.apiBase}/product/list`,
      data: params,
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200) {
          const records = res.data?.data?.records || [];
          resolve({
            spuList: records.map(item => ({
              spuId: item.id,
              thumb: item.coverImage,
              title: item.title,
              price: item.price || 0,
              originPrice: item.originalPrice || 0,
              authorName: item.authorName,
              artType: item.artType,
              tags: [],
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

/** 获取商品列表 */
export function fetchGoodsList(params) {
  if (config.useMock) {
    return mockFetchGoodsList(params);
  }
  return realFetchGoodsList(params);
}
