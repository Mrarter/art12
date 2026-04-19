<template>
  <view class="topic-page">
    <!-- 话题头部 -->
    <view class="topic-header">
      <view class="topic-info">
        <text class="topic-name">#{{ topic.name }}</text>
        <text class="post-count">{{ topic.postCount }} 帖子</text>
      </view>
      <button class="btn-join" v-if="!topic.isJoined" @click="handleJoin">加入</button>
      <text class="joined" v-else>已加入</text>
    </view>

    <!-- 帖子列表 -->
    <scroll-view class="post-list" scroll-y @scrolltolower="loadMore">
      <view class="post-item" v-for="post in postList" :key="post.id" @click="goPostDetail(post.id)">
        <!-- 作者信息 -->
        <view class="post-header">
          <image class="avatar" :src="post.authorAvatar || 'https://picsum.photos/100/100?random=50'" mode="aspectFill" />
          <view class="author-info">
            <text class="nickname">{{ post.authorName }}</text>
            <text class="time">{{ post.createTime }}</text>
          </view>
        </view>

        <!-- 帖子内容 -->
        <view class="post-content">
          <text class="post-text">{{ post.content }}</text>
          
          <!-- 图片 -->
          <view class="image-list" v-if="post.images?.length > 0">
            <image 
              v-for="(img, idx) in post.images" 
              :key="idx"
              class="post-image"
              :class="{ 'single': post.images.length === 1 }"
              :src="img"
              mode="aspectFill"
            />
          </view>
        </view>

        <!-- 互动栏 -->
        <view class="interaction-bar">
          <view class="action-item">
            <text>{{ post.isLiked ? '❤️' : '🤍' }}</text>
            <text>{{ post.likeCount }}</text>
          </view>
          <view class="action-item">
            <text>💬</text>
            <text>{{ post.commentCount }}</text>
          </view>
          <view class="action-item">
            <text>🔗</text>
            <text>分享</text>
          </view>
        </view>
      </view>

      <!-- 加载更多 -->
      <view class="load-more" v-if="hasMore">
        <text v-if="loading">加载中...</text>
        <text v-else @click="loadMore">加载更多</text>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="postList.length === 0 && !loading">
        <text class="empty-icon">📝</text>
        <text class="empty-text">暂无相关帖子</text>
        <button class="btn-first-post" @click="goPublish">发第一条帖子</button>
      </view>
    </scroll-view>

    <!-- 发布按钮 -->
    <view class="fab-button safe-area-bottom" @click="goPublish">
      <text>+</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const topic = ref({
  id: '',
  name: '',
  postCount: 0,
  isJoined: false
})

const postList = ref([])
const loading = ref(false)
const page = ref(1)
const hasMore = ref(true)

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = currentPage.options || {}

  if (options.id) {
    topic.value.id = options.id
    topic.value.name = options.name || '艺术欣赏'
    topic.value.postCount = 1234
  }

  loadData(true)
})

async function loadData(refresh = false) {
  if (loading.value) return
  if (refresh) {
    page.value = 1
    hasMore.value = true
  }
  if (!hasMore.value) return

  loading.value = true

  try {
    // 模拟加载数据
    await new Promise(resolve => setTimeout(resolve, 500))
    
    if (refresh) {
      postList.value = generateMockPosts()
    } else {
      postList.value.push(...generateMockPosts())
    }
    
    hasMore.value = false
    page.value++
  } catch (e) {
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

function generateMockPosts() {
  return [
    {
      id: Date.now() + Math.random(),
      authorName: '艺术爱好者',
      authorAvatar: 'https://picsum.photos/100/100?random=51',
      content: '今天参观了一个当代艺术展，感受到了艺术的无限可能性。艺术家的创作理念非常独特，作品充满了对生活的思考。',
      images: ['https://picsum.photos/400/400?random=30'],
      likeCount: 128,
      commentCount: 23,
      isLiked: false,
      createTime: '2小时前'
    },
    {
      id: Date.now() + Math.random(),
      authorName: '收藏达人',
      authorAvatar: 'https://picsum.photos/100/100?random=52',
      content: '分享一下我最近收藏的一件艺术品，非常喜欢这种风格。',
      images: [
        'https://picsum.photos/400/400?random=31',
        'https://picsum.photos/400/400?random=32'
      ],
      likeCount: 256,
      commentCount: 45,
      isLiked: true,
      createTime: '5小时前'
    },
    {
      id: Date.now() + Math.random(),
      authorName: '画家李明',
      authorAvatar: 'https://picsum.photos/100/100?random=53',
      content: '创作分享，这幅画我用了三个月时间完成，每一个细节都是精心雕琢的。',
      images: [
        'https://picsum.photos/400/400?random=33',
        'https://picsum.photos/400/400?random=34',
        'https://picsum.photos/400/400?random=35'
      ],
      likeCount: 512,
      commentCount: 89,
      isLiked: false,
      createTime: '1天前'
    }
  ]
}

function loadMore() {
  if (hasMore.value) {
    loadData(false)
  }
}

function handleJoin() {
  topic.value.isJoined = true
  uni.showToast({ title: '加入成功', icon: 'success' })
}

function goPostDetail(postId) {
  uni.navigateTo({ url: `/pages/social/post-detail?postId=${postId}` })
}

function goPublish() {
  uni.navigateTo({ url: '/pages/social/publish' })
}
</script>

<style lang="scss" scoped>
.topic-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.topic-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  background: #fff;
  border-bottom: 1rpx solid #eee;

  .topic-info {
    display: flex;
    flex-direction: column;
    gap: 8rpx;

    .topic-name {
      font-size: 36rpx;
      font-weight: 600;
      color: #333;
    }

    .post-count {
      font-size: 26rpx;
      color: #999;
    }
  }

  .btn-join {
    width: 140rpx;
    height: 64rpx;
    background: #333;
    color: #fff;
    border-radius: 32rpx;
    font-size: 28rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .joined {
    font-size: 28rpx;
    color: #999;
  }
}

.post-list {
  flex: 1;
}

.post-item {
  background: #fff;
  margin-bottom: 16rpx;
  padding: 24rpx 32rpx;

  .post-header {
    display: flex;
    align-items: center;
    margin-bottom: 20rpx;

    .avatar {
      width: 72rpx;
      height: 72rpx;
      border-radius: 50%;
    }

    .author-info {
      flex: 1;
      margin-left: 16rpx;
      display: flex;
      flex-direction: column;
      gap: 4rpx;

      .nickname {
        font-size: 28rpx;
        font-weight: 500;
        color: #333;
      }

      .time {
        font-size: 24rpx;
        color: #999;
      }
    }
  }

  .post-content {
    .post-text {
      font-size: 28rpx;
      color: #333;
      line-height: 1.6;
      display: block;
      margin-bottom: 16rpx;
    }

    .image-list {
      display: flex;
      flex-wrap: wrap;
      gap: 8rpx;

      .post-image {
        width: 200rpx;
        height: 200rpx;
        border-radius: 8rpx;

        &.single {
          width: 400rpx;
          height: 400rpx;
        }
      }
    }
  }

  .interaction-bar {
    display: flex;
    gap: 48rpx;
    margin-top: 24rpx;
    padding-top: 20rpx;
    border-top: 1rpx solid #f0f0f0;

    .action-item {
      display: flex;
      align-items: center;
      gap: 8rpx;
      font-size: 26rpx;
      color: #666;
    }
  }
}

.load-more {
  text-align: center;
  padding: 32rpx;
  font-size: 26rpx;
  color: #999;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;

  .empty-icon {
    font-size: 120rpx;
  }

  .empty-text {
    font-size: 28rpx;
    color: #999;
    margin: 24rpx 0;
  }

  .btn-first-post {
    width: 240rpx;
    height: 72rpx;
    background: #333;
    color: #fff;
    border-radius: 36rpx;
    font-size: 28rpx;
  }
}

.fab-button {
  position: fixed;
  bottom: 60rpx;
  right: 40rpx;
  width: 100rpx;
  height: 100rpx;
  background: #333;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.2);

  text {
    font-size: 60rpx;
    color: #fff;
    line-height: 1;
  }
}
</style>
