const { triggerCollectIncrease, triggerSaleIncrease } = require('../../services/artwork-price-api');

Page({
  data: {
    artworkId: null,
    artwork: {
      title: '示例作品',
      artistName: '示例艺术家',
      cover: '',
      currentPrice: 12800,
      growthRate: '+0.3%',
      collectCount: 36,
      nextCondition: '每新增10位藏家收藏，作品价格可能上涨0.5%'
    }
  },

  onLoad(options) {
    const artworkId = Number(options.artworkId || 1);
    this.setData({ artworkId });
    // 真实项目中这里调用作品详情接口
  },

  async handleCollect() {
    try {
      const newPrice = await triggerCollectIncrease(this.data.artworkId);

      this.setData({
        'artwork.currentPrice': newPrice,
        'artwork.collectCount': this.data.artwork.collectCount + 1,
        'artwork.growthRate': '+0.5%'
      });

      wx.showToast({
        title: '收藏成功，作品热度提升',
        icon: 'none'
      });
    } catch (e) {
      console.error(e);
    }
  },

  async handleBuy() {
    try {
      const newPrice = await triggerSaleIncrease(this.data.artworkId);

      this.setData({
        'artwork.currentPrice': newPrice,
        'artwork.growthRate': '+2%'
      });

      wx.showToast({
        title: '成交后作品价格已更新',
        icon: 'none'
      });

      // 后续跳转订单确认页
    } catch (e) {
      console.error(e);
    }
  }
});
