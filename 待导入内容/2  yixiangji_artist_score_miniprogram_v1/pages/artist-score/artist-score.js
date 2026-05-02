const { getArtistScore } = require('../../services/artist-score-api');

Page({
  data: {
    artistId: null,
    score: {},
    scoreItems: []
  },

  onLoad(options) {
    const artistId = Number(options.artistId || 1);
    this.setData({ artistId });
    this.loadScore(artistId);
  },

  async loadScore(artistId) {
    const score = await getArtistScore(artistId);

    const scoreItems = [
      { key: 'sales', name: '销售表现', desc: '成交金额、成交数量、销售增长', value: score.salesScore || 0 },
      { key: 'influence', name: '市场影响力', desc: '粉丝、收藏、浏览、分享', value: score.influenceScore || 0 },
      { key: 'activity', name: '活跃度', desc: '上新频率、登录、互动', value: score.activityScore || 0 },
      { key: 'quality', name: '作品质量', desc: '平台评审、作品完整度', value: score.qualityScore || 0 },
      { key: 'review', name: '藏家评价', desc: '购买评价、复购、评论质量', value: score.reviewScore || 0 },
      { key: 'academic', name: '学术资质', desc: '美院、职称、展览、获奖', value: score.academicScore || 0 },
      { key: 'internet', name: '互联网资质', desc: '艺术博主身份、粉丝、内容转化', value: score.internetScore || 0 }
    ];

    this.setData({ score, scoreItems });
  }
});
