const { getArtistScore } = require('../../services/artist-score-api');

Page({
  data: {
    artistId: null,
    artist: {
      name: '示例艺术家',
      avatar: '',
      summary: '学院派艺术家 · 平台认证'
    },
    score: {
      level: 'D',
      totalScore: 0
    },
    certTags: [],
    growthPercent: 0
  },

  onLoad(options) {
    const artistId = Number(options.artistId || 1);
    this.setData({ artistId });
    this.loadArtistScore(artistId);
  },

  async loadArtistScore(artistId) {
    try {
      const score = await getArtistScore(artistId);
      const certTags = this.buildCertTags(score);
      this.setData({
        score,
        certTags,
        growthPercent: Math.min(Math.floor((score.totalScore || 0) / 10), 100)
      });
    } catch (e) {
      console.error(e);
    }
  },

  buildCertTags(score) {
    const tags = [];
    if ((score.academicScore || 0) >= 60) tags.push('学院派认证');
    if ((score.internetScore || 0) >= 30) tags.push('艺术内容创作者');
    if ((score.salesScore || 0) >= 100) tags.push('作品流通中');
    if ((score.qualityScore || 0) >= 80) tags.push('平台精选艺术家');
    return tags.length ? tags : ['平台认证艺术家'];
  },

  goScoreDetail() {
    wx.navigateTo({
      url: `/pages/artist-score/artist-score?artistId=${this.data.artistId}`
    });
  }
});
