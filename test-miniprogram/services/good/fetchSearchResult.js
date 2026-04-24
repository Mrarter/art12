/* eslint-disable no-param-reassign */
import { config } from '../../config/index';

/** 获取搜索历史 */
function mockSearchResult(params) {
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
      if (item.spuTagList) {
        item.tags = item.spuTagList.map((tag) => ({ title: tag.title }));
      } else {
        item.tags = [];
      }
    });
  }
  return delay().then(() => {
    return data;
  });
}

/** 搜索结果 - 真实API */
function realSearchResult(params) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${config.apiBase}/product/search`,
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

/** 获取搜索历史 */
export function getSearchResult(params) {
  if (config.useMock) {
    return mockSearchResult(params);
  }
  return realSearchResult(params);
}
