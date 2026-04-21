<template>
  <view class="artcircle-page">
    <!-- 顶部话题栏 -->
    <scroll-view class="topic-scroll" scroll-x>
      <view
        class="topic-item"
        :class="{ active: !currentTopicId }"
        @click="selectTopic(null)"
      >全部</view>
      <view
        class="topic-item"
        :class="{ active: currentTopicId === topic.id }"
        v-for="topic in topics"
        :key="topic.id"
        @click="selectTopic(topic.id)"
      >{{ topic.name }}</view>
    </scroll-view>

    <!-- 内容列表 -->
    <scroll-view
      class="content-list"
      scroll-y
      @scrolltolower="loadMore"
    >
      <view class="post-card" v-for="post in postList" :key="post.id" @click="goDetail(post.id)">
        <!-- 用户信息 -->
        <view class="post-header">
          <image
            class="user-avatar"
            :src="post.userAvatar || '/static/icons/avatar-default.png'"
            mode="aspectFill"
          ></image>
          <view class="user-info">
            <view class="user-name-row">
              <text class="user-name">{{ post.authorName || '用户' + post.userId }}</text>
              <view class="identity-badge" v-if="post.identityType">
                {{ getIdentityLabel(post.identityType) }}
              </view>
            </view>
            <text class="post-time">{{ formatTime(post.createTime) }}</text>
          </view>
          <view class="more-btn" @click.stop="showMore(post)">
            <text>...</text>
          </view>
        </view>

        <!-- 内容 -->
        <view class="post-content">
          <text class="content-text">{{ post.content }}</text>

          <!-- 图片网格 -->
          <view
            class="content-images"
            :class="'images-' + (post.images ? Math.min(post.images.length, 4) : 0)"
            v-if="post.images && post.images.length > 0"
          >
            <image
              v-for="(img, idx) in post.images.slice(0, 4)"
              :key="idx"
              :src="img"
              mode="aspectFill"
              class="content-image"
              @click.stop="previewImages(post.images, idx)"
            ></image>
            <view class="more-images" v-if="post.images.length > 4" @click.stop>
              +{{ post.images.length - 4 }}
            </view>
          </view>

          <!-- 话题标签 -->
          <view class="topic-tag" v-if="post.topicId && getTopicName(post.topicId)">
            # {{ getTopicName(post.topicId) }}
          </view>
        </view>

        <!-- 作品关联 -->
        <view class="post-product" v-if="post.artworkId" @click.stop="goProduct(post.artworkId)">
          <image
            class="product-image"
            :src="post.artworkCover || '/static/icons/artwork-default.png'"
            mode="aspectFill"
          ></image>
          <view class="product-info">
            <text class="product-title">{{ post.artworkTitle || '关联作品' }}</text>
            <text class="product-price" v-if="post.artworkPrice">¥{{ formatPrice(post.artworkPrice) }}</text>
          </view>
        </view>

        <!-- 互动栏 -->
        <view class="interaction-bar">
          <view class="interaction-item" @click.stop="toggleLike(post)">
            <u-icon
              :name="post.isLiked ? 'thumb-up-fill' : 'thumb-up'"
              :color="post.isLiked ? '#e74c3c' : '#999'"
              size="22"
            ></u-icon>
            <text :style="{ color: post.isLiked ? '#e74c3c' : '#999' }">
              {{ post.likeCount || 0 }}
            </text>
          </view>
          <view class="interaction-item" @click.stop="goDetail(post.id)">
            <u-icon name="chat" size="22" color="#999"></u-icon>
            <text>{{ post.commentCount || 0 }}</text>
          </view>
          <view class="interaction-item" @click.stop="sharePost(post)">
            <u-icon name="share" size="22" color="#999"></u-icon>
            <text>{{ post.shareCount || 0 }}</text>
          </view>
        </view>
      </view>

      <!-- 加载更多 -->
      <view class="load-more" v-if="loading">
        <u-loading mode="circle"></u-loading>
        <text>加载中...</text>
      </view>
      <view class="no-more" v-else-if="postList.length > 0">
        <text>— 没有更多了 —</text>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="!loading && postList.length === 0">
        <image class="empty-icon" src="/static/icons/post-empty.png" mode="aspectFit"></image>
        <text class="empty-text">暂无动态，快来发布第一条吧</text>
      </view>
    </scroll-view>

    <!-- 发布按钮 -->
    <view class="publish-btn" @click="goPublish">
      <u-icon name="plus" size="32" color="#fff"></u-icon>
    </view>
  </view>
</template>

<script>
import { getPosts, getTopics, likePost, unlikePost } from '@/api/community'

export default {
  data() {
    return {
      currentTopicId: null,
      topics: [],
      postList: [],
      page: 1,
      pageSize: 10,
      loading: false,
      noMore: false
    }
  },

  onLoad() {
    this.loadTopics()
    this.loadPosts()
  },

  onShow() {
    // 刷新数据
  },

  onPullDownRefresh() {
    this.refresh()
    setTimeout(() => uni.stopPullDownRefresh(), 500)
  },

  methods: {
    async loadTopics() {
      try {
        const res = await getTopics()
        if (res.code === 200) {
          this.topics = res.data || []
        }
      } catch (e) {
        console.error('加载话题失败', e)
      }
    },

    async loadPosts() {
      if (this.loading || this.noMore) return

      this.loading = true
      try {
        const res = await getPosts({
          page: this.page,
          pageSize: this.pageSize,
          topicId: this.currentTopicId
        })

        if (res.code === 200) {
          const data = res.data
          if (this.page === 1) {
            this.postList = data.records || []
          } else {
            this.postList = [...this.postList, ...(data.records || [])]
          }
          this.noMore = this.postList.length >= (data.total || 0)
        }
      } catch (e) {
        console.error('加载帖子失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },

    refresh() {
      this.page = 1
      this.noMore = false
      this.loadPosts()
    },

    selectTopic(topicId) {
      if (this.currentTopicId === topicId) return
      this.currentTopicId = topicId
      this.refresh()
    },

    loadMore() {
      if (!this.noMore) {
        this.page++
        this.loadPosts()
      }
    },

    async toggleLike(post) {
      try {
        if (post.isLiked) {
          await unlikePost(post.id)
          post.isLiked = false
          post.likeCount = Math.max(0, (post.likeCount || 0) - 1)
        } else {
          await likePost(post.id)
          post.isLiked = true
          post.likeCount = (post.likeCount || 0) + 1
        }
      } catch (e) {
        console.error('点赞失败', e)
      }
    },

    getIdentityLabel(type) {
      const labels = {
        artist: '艺术家',
        gallery: '画廊',
        dealer: '画商',
        promoter: '艺荐官',
        collector: '收藏家'
      }
      return labels[type] || ''
    },

    getTopicName(topicId) {
      const topic = this.topics.find(t => t.id === topicId)
      return topic ? topic.name : ''
    },

    formatTime(timeStr) {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      const now = new Date()
      const diff = now - date

      if (diff < 60000) return '刚刚'
      if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
      if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
      if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`

      return `${date.getMonth() + 1}/${date.getDate()}`
    },

    formatPrice(price) {
      if (!price) return '0'
      return Number(price).toLocaleString()
    },

    previewImages(images, index) {
      uni.previewImage({
        current: index,
        urls: images
      })
    },

    goDetail(postId) {
      uni.navigateTo({ url: `/pages/artcircle/detail?id=${postId}` })
    },

    goProduct(artworkId) {
      uni.navigateTo({ url: `/pages/gallery/detail?id=${artworkId}` })
    },

    sharePost(post) {
      uni.showShareMenu({
        withShareTicket: true
      })
    },

    showMore(post) {
      uni.showActionSheet({
        itemList: ['举报', '分享', '取消'],
        success: (res) => {
          if (res.tapIndex === 0) {
            this.reportPost(post)
          } else if (res.tapIndex === 1) {
            this.sharePost(post)
          }
        }
      })
    },

    reportPost(post) {
      uni.showToast({ title: '举报成功', icon: 'success' })
    },

    goPublish() {
      uni.navigateTo({ url: '/pages/artcircle/publish' })
    }
  }
}
</script>

<style lang="scss" scoped>
.artcircle-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.topic-scroll {
  white-space: nowrap;
  background: #fff;
  padding: 20rpx 0;
}

.topic-item {
  display: inline-block;
  padding: 12rpx 32rpx;
  margin-left: 20rpx;
  font-size: 28rpx;
  color: #666;
  background: #f5f5f5;
  border-radius: 30rpx;
}

.topic-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.content-list {
  height: calc(100vh - 120rpx);
  padding: 20rpx;
}

.post-card {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
}

.post-header {
  display: flex;
  align-items: center;
  padding: 24rpx;
}

.user-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  margin-right: 20rpx;
  background: #f0f0f0;
}

.user-info {
  flex: 1;
}

.user-name-row {
  display: flex;
  align-items: center;
  margin-bottom: 6rpx;
}

.user-name {
  font-size: 30rpx;
  color: #333;
  font-weight: 500;
}

.identity-badge {
  font-size: 18rpx;
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 4rpx 12rpx;
  border-radius: 20rpx;
  margin-left: 12rpx;
}

.post-time {
  font-size: 22rpx;
  color: #999;
}

.more-btn {
  padding: 10rpx 20rpx;
  color: #999;
  font-size: 36rpx;
  font-weight: bold;
}

.post-content {
  padding: 0 24rpx 24rpx;
}

.content-text {
  font-size: 30rpx;
  color: #333;
  line-height: 1.6;
  margin-bottom: 16rpx;
}

.content-images {
  display: grid;
  gap: 8rpx;
  margin-bottom: 16rpx;
}

.images-1 {
  grid-template-columns: 1fr;
}

.images-2, .images-3 {
  grid-template-columns: repeat(2, 1fr);
}

.images-4 {
  grid-template-columns: repeat(2, 1fr);
}

.content-image {
  width: 100%;
  height: 220rpx;
  border-radius: 8rpx;
  background: #f0f0f0;
}

.images-1 .content-image {
  height: 400rpx;
}

.more-images {
  position: absolute;
  right: 0;
  top: 0;
  width: 100%;
  height: 220rpx;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
  color: #fff;
  border-radius: 8rpx;
}

.images-4 {
  position: relative;
}

.topic-tag {
  display: inline-block;
  font-size: 26rpx;
  color: #667eea;
}

.post-product {
  display: flex;
  align-items: center;
  margin: 0 24rpx 24rpx;
  padding: 20rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
}

.product-image {
  width: 120rpx;
  height: 120rpx;
  border-radius: 8rpx;
  margin-right: 20rpx;
  background: #f0f0f0;
}

.product-info {
  flex: 1;
}

.product-title {
  display: block;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 10rpx;
}

.product-price {
  font-size: 28rpx;
  color: #e74c3c;
  font-weight: 600;
}

.interaction-bar {
  display: flex;
  padding: 20rpx 24rpx;
  border-top: 1rpx solid #f0f0f0;
}

.interaction-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.interaction-item text {
  margin-left: 10rpx;
  font-size: 26rpx;
}

.load-more, .no-more {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 30rpx 0;
  font-size: 26rpx;
  color: #999;
}

.load-more text {
  margin-left: 12rpx;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
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

.publish-btn {
  position: fixed;
  right: 40rpx;
  bottom: 80rpx;
  width: 110rpx;
  height: 110rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 30rpx rgba(102, 126, 234, 0.4);
}
</style>
