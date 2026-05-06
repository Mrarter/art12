<template>
  <view class="page">
    <ArtistLevelBadge :level="score.level" :score="score.totalScore" />

    <view class="score-list">
      <view class="score-item" v-for="item in scoreItems" :key="item.key">
        <view class="score-left">
          <view class="score-name">{{ item.name }}</view>
          <view class="score-desc">{{ item.desc }}</view>
        </view>
        <view class="score-value">{{ item.value }}<text class="score-max">/{{ item.max }}</text></view>
      </view>
    </view>

    <view class="notice">
      评分由平台根据销售表现、市场影响力、活跃度、作品质量、藏家评价、学术资质、互联网资质综合生成。学术资质和互联网资质需经后台审核通过后方可计入。
    </view>
  </view>
</template>

<script>
import { getArtistScore } from '@/api/artistScore'
import ArtistLevelBadge from '@/components/ArtistLevelBadge'

export default {
  components: { ArtistLevelBadge },
  data() {
    return {
      artistId: null,
      score: {
        level: 'D',
        totalScore: 0
      },
      scoreItems: []
    }
  },

  onLoad(options) {
    this.artistId = Number(options.artistId)
    if (this.artistId) {
      this.loadScore(this.artistId)
    }
  },

  methods: {
    async loadScore(artistId) {
      try {
        const res = await getArtistScore(artistId)
        // 兼容后端返回包装结构 { code, data, message }
        const score = res?.data || res || {}
        this.score = score

        this.scoreItems = [
          { key: 'sales', name: '销售表现', desc: '成交金额、成交数量、销售增长', value: score.salesScore || 0, max: 300 },
          { key: 'influence', name: '市场影响力', desc: '粉丝、收藏、浏览、分享', value: score.influenceScore || 0, max: 200 },
          { key: 'activity', name: '活跃度', desc: '上新频率、登录、互动', value: score.activityScore || 0, max: 100 },
          { key: 'quality', name: '作品质量', desc: '平台评审、作品完整度', value: score.qualityScore || 0, max: 150 },
          { key: 'review', name: '藏家评价', desc: '购买评价、复购、评论质量', value: score.reviewScore || 0, max: 100 },
          { key: 'academic', name: '学术资质', desc: '毕业院校、职称、展览、获奖', value: score.academicScore || 0, max: 100 },
          { key: 'internet', name: '互联网资质', desc: '艺术博主身份、粉丝、内容转化', value: score.internetScore || 0, max: 50 }
        ]
      } catch (e) {
        console.error('加载评分失败', e)
        uni.showToast({ title: '加载评分失败', icon: 'none' })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 32rpx;
  background: #0d0d0d;
}

.score-list {
  margin-top: 32rpx;
  background: #1a1a1a;
  border-radius: 24rpx;
  overflow: hidden;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
}

.score-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx;
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.06);

  &:last-child {
    border-bottom: none;
  }
}

.score-left {
  flex: 1;
  margin-right: 20rpx;
}

.score-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #fff;
}

.score-desc {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.4);
}

.score-value {
  font-size: 34rpx;
  font-weight: 700;
  color: #d4af37;
  white-space: nowrap;
}

.score-max {
  font-size: 22rpx;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.3);
}

.notice {
  margin-top: 28rpx;
  padding: 28rpx;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 16rpx;
  color: rgba(255, 255, 255, 0.4);
  font-size: 24rpx;
  line-height: 1.6;
}
</style>
