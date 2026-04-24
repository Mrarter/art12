import { config, cdnBase } from '../../config/index';

/** 获取首页数据 */
function mockFetchHome() {
  const { delay } = require('../_utils/delay');
  const { genSwiperImageList } = require('../../model/swiper');
  return delay().then(() => {
    return {
      swiper: genSwiperImageList(),
      tabList: [
        {
          text: '精选推荐',
          key: 0,
        },
        {
          text: '夏日防晒',
          key: 1,
        },
        {
          text: '二胎大作战',
          key: 2,
        },
        {
          text: '人气榜',
          key: 3,
        },
        {
          text: '好评榜',
          key: 4,
        },
        {
          text: 'RTX 30',
          key: 5,
        },
        {
          text: '手机也疯狂',
          key: 6,
        },
      ],
      activityImg: `${cdnBase}/activity/banner.png`,
    };
  });
}

/** 获取首页数据 - 真实API */
function realFetchHome() {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${config.apiBase}/product/homepage/banners`,
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200) {
          const banners = res.data?.data || [];
          resolve({
            swiper: banners.map(item => ({
              imgUrl: item.imageUrl,
              title: item.title,
              linkType: item.linkType,
              linkValue: item.linkValue,
            })),
            tabList: [
              { text: '精选推荐', key: 0 },
              { text: '新品上市', key: 1 },
              { text: '热门作品', key: 2 },
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

/** 获取首页数据 */
export function fetchHome() {
  if (config.useMock) {
    return mockFetchHome();
  }
  return realFetchHome();
}
