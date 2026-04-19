<template>
  <view class="post-detail-page">
    <!-- 帖子内容 -->
    <view class="post-content">
      <!-- 作者信息 -->
      <view class="author-section">
        <image class="avatar" :src="post.authorAvatar || 'https://picsum.photos/100/100?random=100'" mode="aspectFill" />
        <view class="author-info">
          <text class="nickname">{{ post.authorName }}</text>
          <text class="time">{{ post.createTime }}</text>
        </view>
        <button class="btn-follow" v-if="!post.isFollowed" @click="handleFollow">关注</button>
        <text class="followed" v-else>已关注</text>
      </view>

      <!-- 帖子正文 -->
      <view class="post-body">
        <text class="post-text">{{ post.content }}</text>
        
        <!-- 图片列表 -->
        <view class="image-list" v-if="post.images?.length > 0">
          <image 
            v-for="(img, idx) in post.images" 
            :key="idx"
            class="post-image"
            :src="img"
            mode="aspectFill"
            @click="previewImage(idx)"
          />
        </view>

        <!-- 话题标签 -->
        <view class="topic-tags" v-if="post.topics?.length > 0">
          <text class="topic-tag" v-for="topic in post.topics" :key="topic.id" @click="goTopic(topic.id)">
            #{{ topic.name }}
          </text>
        </view>
      </view>

      <!-- 互动栏 -->
      <view class="interaction-bar">
        <view class="action-item" @click="handleLike">
          <text class="icon">{{ post.isLiked ? '❤️' : '🤍' }}</text>
          <text class="count">{{ post.likeCount }}</text>
        </view>
        <view class="action-item">
          <text class="icon">💬</text>
          <text class="count">{{ post.commentCount }}</text>
        </view>
        <view class="action-item" @click="handleShare">
          <text class="icon">🔗</text>
          <text class="count">分享</text>
        </view>
      </view>
    </view>

    <!-- 热门评论 -->
    <view class="comment-section">
      <view class="section-header">
        <text class="section-title">热门评论</text>
        <text class="total-count">共 {{ post.commentCount }} 条评论</text>
      </view>

      <view class="comment-list">
        <view class="comment-item" v-for="comment in comments" :key="comment.id">
          <image class="comment-avatar" :src="comment.avatar || 'https://picsum.photos/100/100?random=101'" mode="aspectFill" />
          <view class="comment-body">
            <view class="comment-header">
              <text class="comment-author">{{ comment.authorName }}</text>
              <text class="comment-time">{{ comment.createTime }}</text>
            </view>
            <text class="comment-text">{{ comment.content }}</text>
            <view class="comment-actions">
              <view class="action-btn" @click="handleCommentLike(comment)">
                <text>{{ comment.isLiked ? '❤️' : '🤍' }}</text>
                <text>{{ comment.likeCount }}</text>
              </view>
              <view class="action-btn" @click="handleReply(comment)">
                <text>回复</text>
              </view>
            </view>

            <!-- 回复列表 -->
            <view class="reply-list" v-if="comment.replies?.length > 0">
              <view class="reply-item" v-for="reply in comment.replies" :key="reply.id">
                <text class="reply-author">{{ reply.authorName }}:</text>
                <text class="reply-text">{{ reply.content }}</text>
              </view>
              <view class="more-replies" v-if="comment.replyCount > 3" @click="loadMoreReplies(comment)">
                查看全部 {{ comment.replyCount }} 条回复 >
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部评论输入 -->
    <view class="comment-input-bar safe-area-bottom">
      <input 
        class="input-field" 
        v-model="commentText" 
        placeholder="写评论..."
        @focus="showCommentInput = true"
      />
      <button class="btn-send" v-if="showCommentInput" @click="submitComment">发送</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const post = ref({
  id: '',
  authorName: '',
  authorAvatar: '',
  content: '',
  images: [],
  topics: [],
  likeCount: 0,
  commentCount: 0,
  isLiked: false,
  isFollowed: false,
  createTime: ''
})

const comments = ref([])
const commentText = ref('')
const showCommentInput = ref(false)

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = currentPage.options || {}

  if (options.postId) {
    loadPostDetail(options.postId)
  } else {
    loadMockData()
  }
})

function loadMockData() {
  post.value = {
    id: '1',
    authorName: '艺术收藏家',
    authorAvatar: 'https://picsum.photos/100/100?random=100',
    content: '今天在画廊看到这幅作品，色彩运用非常独特，艺术家对光影的处理让人印象深刻。这幅作品展现了艺术家对自然的深刻理解和对色彩的大胆运用。强烈推荐大家去现场感受！',
    images: [
      'https://picsum.photos/400/400?random=20',
      'https://picsum.photos/400/400?random=21',
      'https://picsum.photos/400/400?random=22'
    ],
    topics: [
      { id: 1, name: '艺术欣赏' },
      { id: 2, name: '当代艺术' }
    ],
    likeCount: 256,
    commentCount: 45,
    isLiked: false,
    isFollowed: false,
    createTime: '2小时前'
  }

  comments.value = [
    {
      id: 1,
      authorName: '张三',
      avatar: 'https://picsum.photos/100/100?random=101',
      content: '这幅作品确实很棒！艺术家对细节的处理非常到位',
      likeCount: 12,
      isLiked: false,
      createTime: '1小时前',
      replies: [
        { id: 101, authorName: '艺术收藏家', content: '确实如此！' },
        { id: 102, authorName: '李四', content: '同感！' }
      ],
      replyCount: 5
    },
    {
      id: 2,
      authorName: '李四',
      avatar: 'https://picsum.photos/100/100?random=102',
      content: '请问这幅作品在哪里可以看到？',
      likeCount: 5,
      isLiked: true,
      createTime: '30分钟前',
      replies: [],
      replyCount: 0
    }
  ]
}

async function loadPostDetail(postId) {
  try {
    // const res = await getPostDetail(postId)
    // post.value = res
    loadMockData()
  } catch (e) {
    loadMockData()
  }
}

function handleLike() {
  post.value.isLiked = !post.value.isLiked
  post.value.likeCount += post.value.isLiked ? 1 : -1
  
  uni.showToast({
    title: post.value.isLiked ? '点赞成功' : '取消点赞',
    icon: 'success'
  })
}

function handleFollow() {
  post.value.isFollowed = true
  uni.showToast({ title: '关注成功', icon: 'success' })
}

function handleCommentLike(comment) {
  comment.isLiked = !comment.isLiked
  comment.likeCount += comment.isLiked ? 1 : -1
}

function handleReply(comment) {
  showCommentInput.value = true
  commentText.value = `回复 @${comment.authorName}：`
}

function submitComment() {
  if (!commentText.value.trim()) {
    uni.showToast({ title: '请输入评论内容', icon: 'none' })
    return
  }

  uni.showToast({ title: '评论成功', icon: 'success' })
  post.value.commentCount++
  commentText.value = ''
  showCommentInput.value = false
}

function previewImage(index) {
  uni.previewImage({
    current: index,
    urls: post.value.images
  })
}

function goTopic(topicId) {
  uni.navigateTo({ url: `/pages/social/topic?id=${topicId}` })
}

function loadMoreReplies(comment) {
  uni.showToast({ title: '加载更多回复', icon: 'none' })
}

function handleShare() {
  uni.showToast({ title: '分享功能开发中', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.post-detail-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 120rpx;
}

.post-content {
  background: #fff;
  margin-bottom: 16rpx;
}

.author-section {
  display: flex;
  align-items: center;
  padding: 32rpx;

  .avatar {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
  }

  .author-info {
    flex: 1;
    margin-left: 20rpx;
    display: flex;
    flex-direction: column;
    gap: 4rpx;

    .nickname {
      font-size: 30rpx;
      font-weight: 500;
      color: #333;
    }

    .time {
      font-size: 24rpx;
      color: #999;
    }
  }

  .btn-follow {
    width: 120rpx;
    height: 56rpx;
    background: #333;
    color: #fff;
    border-radius: 28rpx;
    font-size: 26rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .followed {
    font-size: 26rpx;
    color: #999;
  }
}

.post-body {
  padding: 0 32rpx 32rpx;

  .post-text {
    font-size: 30rpx;
    color: #333;
    line-height: 1.6;
  }

  .image-list {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
    margin-top: 24rpx;

    .post-image {
      width: 220rpx;
      height: 220rpx;
      border-radius: 8rpx;
    }
  }

  .topic-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
    margin-top: 24rpx;

    .topic-tag {
      font-size: 28rpx;
      color: #409eff;
    }
  }
}

.interaction-bar {
  display: flex;
  padding: 24rpx 32rpx;
  border-top: 1rpx solid #f0f0f0;

  .action-item {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8rpx;

    .icon {
      font-size: 36rpx;
    }

    .count {
      font-size: 26rpx;
      color: #666;
    }
  }
}

.comment-section {
  background: #fff;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx 32rpx;
    border-bottom: 1rpx solid #f0f0f0;

    .section-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
    }

    .total-count {
      font-size: 26rpx;
      color: #999;
    }
  }

  .comment-list {
    padding: 0 32rpx;
  }

  .comment-item {
    display: flex;
    padding: 32rpx 0;
    border-bottom: 1rpx solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .comment-avatar {
      width: 72rpx;
      height: 72rpx;
      border-radius: 50%;
    }

    .comment-body {
      flex: 1;
      margin-left: 20rpx;

      .comment-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 12rpx;

        .comment-author {
          font-size: 28rpx;
          font-weight: 500;
          color: #333;
        }

        .comment-time {
          font-size: 24rpx;
          color: #999;
        }
      }

      .comment-text {
        font-size: 28rpx;
        color: #333;
        line-height: 1.5;
      }

      .comment-actions {
        display: flex;
        gap: 32rpx;
        margin-top: 16rpx;

        .action-btn {
          display: flex;
          align-items: center;
          gap: 6rpx;
          font-size: 24rpx;
          color: #999;
        }
      }

      .reply-list {
        margin-top: 20rpx;
        padding: 20rpx;
        background: #f9f9f9;
        border-radius: 8rpx;

        .reply-item {
          margin-bottom: 12rpx;
          font-size: 26rpx;
          line-height: 1.5;

          .reply-author {
            color: #666;
            margin-right: 8rpx;
          }

          .reply-text {
            color: #333;
          }
        }

        .more-replies {
          font-size: 24rpx;
          color: #409eff;
          margin-top: 8rpx;
        }
      }
    }
  }
}

.comment-input-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 32rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #eee;

  .input-field {
    flex: 1;
    height: 72rpx;
    padding: 0 24rpx;
    background: #f5f5f5;
    border-radius: 36rpx;
    font-size: 28rpx;
  }

  .btn-send {
    width: 120rpx;
    height: 64rpx;
    background: #333;
    color: #fff;
    border-radius: 32rpx;
    font-size: 26rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
