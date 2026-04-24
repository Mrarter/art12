import { config } from '../../config/index';

/** 获取商品详情页评论数 */
function mockFetchGoodDetailsCommentsCount(spuId = 0) {
  const { delay } = require('../_utils/delay');
  const {
    getGoodsDetailsCommentsCount,
  } = require('../../model/detailsComments');
  return delay().then(() => getGoodsDetailsCommentsCount(spuId));
}

/** 评论数 - 真实API */
function realFetchGoodDetailsCommentsCount(spuId = 0) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${config.apiBase}/product/comment/count`,
      data: { artworkId: spuId },
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data?.data || { totalCount: 0 });
        } else {
          reject(res);
        }
      },
      fail: reject
    });
  });
}

/** 获取商品详情页评论数 */
export function getGoodsDetailsCommentsCount(spuId = 0) {
  if (config.useMock) {
    return mockFetchGoodDetailsCommentsCount(spuId);
  }
  return realFetchGoodDetailsCommentsCount(spuId);
}

/** 获取商品详情页评论 */
function mockFetchGoodDetailsCommentList(spuId = 0) {
  const { delay } = require('../_utils/delay');
  const { getGoodsDetailsComments } = require('../../model/detailsComments');
  return delay().then(() => getGoodsDetailsCommentList(spuId));
}

/** 评论列表 - 真实API */
function realFetchGoodDetailsCommentList(spuId = 0) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${config.apiBase}/product/comment/list`,
      data: { artworkId: spuId },
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data?.data || []);
        } else {
          reject(res);
        }
      },
      fail: reject
    });
  });
}

/** 获取商品详情页评论 */
export function getGoodsDetailsCommentList(spuId = 0) {
  if (config.useMock) {
    return mockFetchGoodDetailsCommentList(spuId);
  }
  return realFetchGoodDetailsCommentList(spuId);
}
