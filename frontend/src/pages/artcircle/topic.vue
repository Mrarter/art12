<template>
  <view class="topic-square-page">
    <!-- 热门话题 -->
    <view class="hot-topics">
      <view class="section-header">
        <view class="section-title">
          
          <text>热门话题</text>
        </view>
      </view>

      <view class="hot-list">
        <view
          class="hot-item"
          v-for="(topic, index) in hotTopics"
          :key="topic.id"
          @click="goTopicPosts(topic)"
        >
          <view class="rank-badge" :class="'rank-' + (index + 1)">{{ index + 1 }}</view>
          <view class="topic-info">
            <text class="topic-name"># {{ topic.name }} #</text>
            <text class="topic-count">{{ topic.postCount || 0 }}条动态</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 全部话题 -->
    <view class="all-topics">
      <view class="section-header">
        <view class="section-title">
          
          <text>全部话题</text>
        </view>
        <text class="topic-total">{{ allTopics.length }}个话题</text>
      </view>

      <view class="topic-grid">
        <view
          class="topic-card"
          v-for="topic in allTopics"
          :key="topic.id"
          @click="goTopicPosts(topic)"
        >
          <view class="card-icon" :style="{ background: getTopicColor(topic.id) }">
            <text>{{ topic.name.charAt(0) }}</text>
          </view>
          <text class="card-name">{{ topic.name }}</text>
          <text class="card-count">{{ topic.postCount || 0 }}条</text>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="!loading && allTopics.length === 0">
        <image class="empty-icon" src="/static/icons/topic-empty.png" mode="aspectFit"></image>
        <text class="empty-text">暂无话题</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getTopics } from '@/api/community'

export default {
  data() {
    return {
      hotTopics: [],
      allTopics: [],
      loading: false
    }
  },

  onLoad() {
    this.loadTopics()
  },

  onShow() {
    // 刷新数据
    this.loadTopics()
  },

  onShareAppMessage() {
    return {
      title: '拾艺局艺术圈 - 话题广场',
      path: '/pages/artcircle/topic'
    }
  },

  methods: {
    async loadTopics() {
      this.loading = true
      try {
        const res = await getTopics()
        if (res.code === 200) {
          const topics = res.data || []
          // 热门话题：取前5个（按帖子数排序）
          this.hotTopics = topics.slice(0, 5)
          // 全部话题
          this.allTopics = topics
        }
      } catch (e) {
        console.error('加载话题失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },

    goTopicPosts(topic) {
      // 跳转到艺术圈并筛选该话题
      uni.switchTab({ url: '/pages/artcircle/index' })
      // 可以通过事件或缓存传递话题筛选条件
      uni.setStorageSync('selectedTopicId', topic.id)
    },

    getTopicColor(id) {
      const colors = [
        'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
        'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
        'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
        'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
        'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
        'linear-gradient(135deg, #d299c2 0%, #fef9d7 100%)',
        'linear-gradient(135deg, #89f7fe 0%, #66a6ff 100%)'
      ]
      return colors[id % colors.length]
    }
  }
}
</script>

<style lang="scss" scoped>
.topic-square-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: env(safe-area-inset-bottom);
}

/* 热门话题 */
.hot-topics {
  background: #fff;
  margin-bottom: 20rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.section-title {
  display: flex;
  align-items: center;
  font-size: 32rpx;
  color: #333;
  font-weight: 600;

  text {
    margin-left: 12rpx;
  }
}

.topic-total {
  font-size: 24rpx;
  color: #999;
}

.hot-list {
  padding: 0 30rpx 30rpx;
}

.hot-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.hot-item:last-child {
  border-bottom: none;
}

.rank-badge {
  width: 48rpx;
  height: 48rpx;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 600;
  margin-right: 24rpx;
  background: #f0f0f0;
  color: #999;
}

.rank-badge.rank-1 {
  background: linear-gradient(135deg, #f6d365 0%, #fda085 100%);
  color: #fff;
}

.rank-badge.rank-2 {
  background: linear-gradient(135deg, #c9d6ff 0%, #e2e2e2 100%);
  color: #666;
}

.rank-badge.rank-3 {
  background: linear-gradient(135deg, #ffeaa7 0%, #dfe6e9 100%);
  color: #666;
}

.topic-info {
  flex: 1;
}

.topic-name {
  display: block;
  font-size: 30rpx;
  color: #333;
  font-weight: 500;
  margin-bottom: 8rpx;
}

.topic-count {
  font-size: 24rpx;
  color: #999;
}

/* 全部话题 */
.all-topics {
  background: #fff;
}

.topic-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 20rpx 20rpx 30rpx;
}

.topic-card {
  width: 33.33%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30rpx 10rpx;
}

.card-icon {
  width: 100rpx;
  height: 100rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16rpx;

  text {
    font-size: 40rpx;
    color: #fff;
    font-weight: 600;
  }
}

.card-name {
  font-size: 26rpx;
  color: #333;
  margin-bottom: 8rpx;
  text-align: center;
  max-width: 180rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-count {
  font-size: 22rpx;
  color: #999;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
}

.empty-icon {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 30rpx;
  opacity: 0.5;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}
</style>
