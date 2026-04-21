<template>
  <view class="detail-page">
    <!-- 帖子内容 -->
    <view class="post-section">
      <!-- 用户信息 -->
      <view class="post-header">
        <image
          class="user-avatar"
          :src="post.userAvatar || '/static/icons/avatar-default.png'"
          mode="aspectFill"
          @click="goUserProfile(post.userId)"
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
        <view class="follow-btn" v-if="!post.isFollowed" @click="followUser">
          + 关注
        </view>
      </view>

      <!-- 内容 -->
      <view class="post-content">
        <text class="content-text">{{ post.content }}</text>

        <!-- 图片 -->
        <view
          class="content-images"
          :class="'images-' + Math.min(post.images ? post.images.length : 0, 4)"
          v-if="post.images && post.images.length > 0"
        >
          <image
            v-for="(img, idx) in post.images.slice(0, 4)"
            :key="idx"
            :src="img"
            mode="aspectFill"
            class="content-image"
            @click="previewImages(post.images, idx)"
          ></image>
        </view>

        <!-- 话题 -->
        <view class="topic-tag" v-if="post.topicId && topicName">
          # {{ topicName }}
        </view>
      </view>

      <!-- 作品关联 -->
      <view class="post-product" v-if="post.artworkId" @click="goProduct(post.artworkId)">
        <image
          class="product-image"
          :src="post.artworkCover || '/static/icons/artwork-default.png'"
          mode="aspectFill"
        ></image>
        <view class="product-info">
          <text class="product-title">{{ post.artworkTitle || '关联作品' }}</text>
          <text class="product-price" v-if="post.artworkPrice">
            ¥{{ formatPrice(post.artworkPrice) }}
          </text>
        </view>
        <text class="product-arrow">></text>
      </view>
    </view>

    <!-- 互动栏 -->
    <view class="interaction-bar">
      <view class="interaction-item" @click="toggleLike">
        <u-icon
          :name="post.isLiked ? 'thumb-up-fill' : 'thumb-up'"
          :color="post.isLiked ? '#e74c3c' : '#999'"
          size="24"
        ></u-icon>
        <text :style="{ color: post.isLiked ? '#e74c3c' : '#999' }">
          {{ post.likeCount || 0 }}
        </text>
      </view>
      <view class="interaction-item">
        <u-icon name="chat" size="24" color="#999"></u-icon>
        <text>{{ post.commentCount || 0 }}</text>
      </view>
      <view class="interaction-item" @click="sharePost">
        <u-icon name="share" size="24" color="#999"></u-icon>
        <text>{{ post.shareCount || 0 }}</text>
      </view>
    </view>

    <!-- 评论区域 -->
    <view class="comments-section">
      <view class="section-header">
        <text class="section-title">评论</text>
        <text class="comment-count">{{ comments.length }}条评论</text>
      </view>

      <scroll-view class="comments-list" scroll-y>
        <view
          class="comment-item"
          v-for="comment in comments"
          :key="comment.id"
        >
          <image
            class="comment-avatar"
            :src="comment.userAvatar || '/static/icons/avatar-default.png'"
            mode="aspectFill"
          ></image>
          <view class="comment-content">
            <view class="comment-header">
              <text class="comment-name">{{ comment.userName || '用户' + comment.userId }}</text>
              <text class="comment-time">{{ formatTime(comment.createTime) }}</text>
            </view>
            <text class="comment-text">{{ comment.content }}</text>
            <view class="comment-actions">
              <text class="action-btn" @click="replyComment(comment)">回复</text>
            </view>
          </view>
        </view>

        <!-- 空状态 -->
        <view class="empty-comments" v-if="comments.length === 0 && !loading">
          <text>暂无评论，快来抢沙发吧~</text>
        </view>
      </scroll-view>
    </view>

    <!-- 底部输入框 -->
    <view class="input-bar">
      <input
        class="comment-input"
        v-model="commentText"
        :placeholder="replyTo ? '回复 @' + replyToName : '写评论...'"
        @focus="onInputFocus"
      />
      <view class="send-btn" :class="{ active: commentText.trim() }" @click="submitComment">
        <text>发送</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getPostDetail, getComments, likePost, unlikePost, commentPost } from '@/api/community'

export default {
  data() {
    return {
      postId: null,
      post: {},
      comments: [],
      topicName: '',
      commentText: '',
      replyTo: null,
      replyToName: '',
      loading: false,
      topics: []
    }
  },

  onLoad(options) {
    if (options.id) {
      this.postId = Number(options.id)
      this.loadPostDetail()
      this.loadComments()
    }
  },

  onShareAppMessage() {
    return {
      title: this.post.content ? this.post.content.substring(0, 50) : '拾艺局艺术圈',
      path: `/pages/artcircle/detail?id=${this.postId}`
    }
  },

  methods: {
    async loadPostDetail() {
      this.loading = true
      try {
        const res = await getPostDetail(this.postId)
        if (res.code === 200) {
          this.post = res.data || {}
          // 获取话题名称
          this.topicName = this.getTopicName(this.post.topicId)
        }
      } catch (e) {
        console.error('加载帖子详情失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },

    async loadComments() {
      try {
        const res = await getComments(this.postId, 1, 50)
        if (res.code === 200) {
          this.comments = res.data.records || []
        }
      } catch (e) {
        console.error('加载评论失败', e)
      }
    },

    async toggleLike() {
      try {
        if (this.post.isLiked) {
          await unlikePost(this.postId)
          this.post.isLiked = false
          this.post.likeCount = Math.max(0, (this.post.likeCount || 0) - 1)
        } else {
          await likePost(this.postId)
          this.post.isLiked = true
          this.post.likeCount = (this.post.likeCount || 0) + 1
        }
      } catch (e) {
        console.error('点赞失败', e)
      }
    },

    followUser() {
      this.post.isFollowed = true
      uni.showToast({ title: '关注成功', icon: 'success' })
    },

    replyComment(comment) {
      this.replyTo = comment.id
      this.replyToName = comment.userName || '用户' + comment.userId
      // 聚焦输入框
      uni.showToast({ title: '回复 @' + this.replyToName, icon: 'none' })
    },

    async submitComment() {
      if (!this.commentText.trim()) {
        uni.showToast({ title: '请输入评论内容', icon: 'none' })
        return
      }

      try {
        const res = await commentPost(this.postId, {
          content: this.commentText.trim(),
          parentId: this.replyTo
        })

        if (res.code === 200) {
          uni.showToast({ title: '评论成功', icon: 'success' })
          this.commentText = ''
          this.replyTo = null
          this.replyToName = ''
          // 刷新评论
          this.loadComments()
          this.post.commentCount = (this.post.commentCount || 0) + 1
        }
      } catch (e) {
        console.error('评论失败', e)
        uni.showToast({ title: '评论失败', icon: 'none' })
      }
    },

    onInputFocus() {
      // 处理输入框聚焦
    },

    previewImages(images, index) {
      uni.previewImage({
        current: index,
        urls: images
      })
    },

    goUserProfile(userId) {
      uni.navigateTo({ url: `/pages/artist/home?id=${userId}` })
    },

    goProduct(artworkId) {
      uni.navigateTo({ url: `/pages/gallery/detail?id=${artworkId}` })
    },

    sharePost() {
      uni.showShareMenu({
        withShareTicket: true
      })
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
      // 可以从 topics 列表中查找
      return ''
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
    }
  }
}
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 120rpx;
}

.post-section {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
}

.user-avatar {
  width: 88rpx;
  height: 88rpx;
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
  font-size: 32rpx;
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
  font-size: 24rpx;
  color: #999;
}

.follow-btn {
  padding: 12rpx 28rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 30rpx;
  font-size: 26rpx;
}

.post-content {
  margin-bottom: 24rpx;
}

.content-text {
  font-size: 32rpx;
  color: #333;
  line-height: 1.7;
  margin-bottom: 20rpx;
}

.content-images {
  display: grid;
  gap: 8rpx;
  margin-bottom: 20rpx;
}

.images-1 {
  grid-template-columns: 1fr;
}

.images-2, .images-3, .images-4 {
  grid-template-columns: repeat(2, 1fr);
}

.content-image {
  width: 100%;
  height: 300rpx;
  border-radius: 8rpx;
  background: #f0f0f0;
}

.images-1 .content-image {
  height: 500rpx;
}

.topic-tag {
  display: inline-block;
  font-size: 28rpx;
  color: #667eea;
}

.post-product {
  display: flex;
  align-items: center;
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
  margin-bottom: 8rpx;
}

.product-price {
  font-size: 28rpx;
  color: #e74c3c;
  font-weight: 600;
}

.product-arrow {
  color: #ccc;
  font-size: 32rpx;
}

.interaction-bar {
  display: flex;
  background: #fff;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.interaction-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.interaction-item text {
  margin-left: 12rpx;
  font-size: 28rpx;
}

.comments-section {
  background: #fff;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.section-title {
  font-size: 32rpx;
  color: #333;
  font-weight: 600;
}

.comment-count {
  font-size: 26rpx;
  color: #999;
}

.comments-list {
  max-height: 600rpx;
  padding: 0 30rpx;
}

.comment-item {
  display: flex;
  padding: 30rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  margin-right: 20rpx;
  background: #f0f0f0;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.comment-name {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

.comment-time {
  font-size: 22rpx;
  color: #999;
}

.comment-text {
  font-size: 28rpx;
  color: #333;
  line-height: 1.5;
  margin-bottom: 12rpx;
}

.comment-actions {
  display: flex;
}

.action-btn {
  font-size: 24rpx;
  color: #999;
  margin-right: 30rpx;
}

.empty-comments {
  text-align: center;
  padding: 80rpx 0;
  font-size: 28rpx;
  color: #999;
}

.input-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2rpx 20rpx rgba(0, 0, 0, 0.05);
}

.comment-input {
  flex: 1;
  height: 80rpx;
  padding: 0 30rpx;
  background: #f5f5f5;
  border-radius: 40rpx;
  font-size: 28rpx;
}

.send-btn {
  padding: 20rpx 36rpx;
  margin-left: 20rpx;
  background: #ccc;
  color: #fff;
  border-radius: 40rpx;
  font-size: 28rpx;
}

.send-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
</style>
