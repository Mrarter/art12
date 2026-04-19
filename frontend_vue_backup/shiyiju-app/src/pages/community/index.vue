<template>
  <view class="community-page">
    <!-- Tab切换 -->
    <view class="community-tabs">
      <view 
        class="tab-item" 
        :class="{ active: activeTab === 'follow' }"
        @click="switchTab('follow')"
      >
        关注
      </view>
      <view 
        class="tab-item" 
        :class="{ active: activeTab === 'recommend' }"
        @click="switchTab('recommend')"
      >
        推荐
      </view>
    </view>

    <!-- 话题标签 -->
    <view class="topic-bar">
      <scroll-view scroll-x>
        <view 
          v-for="topic in topics" 
          :key="topic.id"
          class="topic-tag"
          :class="{ active: currentTopic === topic.id }"
          @click="switchTopic(topic.id)"
        >
          #{{ topic.name }}
        </view>
      </scroll-view>
    </view>

    <!-- 帖子列表 -->
    <scroll-view 
      scroll-y 
      class="post-list"
      refresher-enabled
      @refresherrefresh="onRefresh"
    >
      <view 
        v-for="post in posts" 
        :key="post.id"
        class="post-card"
      >
        <view class="post-header">
          <image class="post-avatar" :src="'/static/default-avatar.png'" />
          <view class="post-author">
            <text class="author-name">{{ post.authorName || '用户' + post.userId }}</text>
            <text class="post-time">{{ formatTime(post.createTime) }}</text>
          </view>
        </view>
        
        <view class="post-content" @click="goDetail(post.id)">
          {{ post.content }}
        </view>
        
        <view class="post-images" v-if="post.images?.length" @click="previewImages(post)">
          <image 
            v-for="(img, idx) in post.images.slice(0, 9)" 
            :key="idx"
            :src="img" 
            mode="aspectFill"
            class="post-image"
          />
        </view>
        
        <view class="post-actions">
          <view class="action-item" @click="toggleLike(post)">
            <text>{{ post.isLiked ? '❤️' : '🤍' }}</text>
            <text>{{ post.likeCount || 0 }}</text>
          </view>
          <view class="action-item" @click="goDetail(post.id)">
            <text>💬</text>
            <text>{{ post.commentCount || 0 }}</text>
          </view>
          <view class="action-item" @click="sharePost(post)">
            <text>↗️</text>
            <text>分享</text>
          </view>
        </view>
      </view>
      
      <view class="loading-more" v-if="isLoading">
        <text>加载中...</text>
      </view>
    </scroll-view>

    <!-- 发帖按钮 -->
    <view class="fab" @click="showPostModal = true" v-if="isLoggedIn">
      <text>+</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getPosts, getTopics, likePost, unlikePost } from '@/api/community'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const activeTab = ref('recommend')
const currentTopic = ref(null)
const posts = ref([])
const topics = ref([])
const isLoading = ref(false)
const showPostModal = ref(false)

const isLoggedIn = computed(() => userStore.isLoggedIn)

onMounted(() => {
  loadTopics()
  loadPosts()
})

async function loadTopics() {
  try {
    topics.value = await getTopics() || []
  } catch (e) {
    topics.value = [
      { id: 1, name: '艺术品交流' },
      { id: 2, name: '收藏心得' },
      { id: 3, name: '艺术资讯' },
      { id: 4, name: '画家故事' }
    ]
  }
}

async function loadPosts() {
  isLoading.value = true
  try {
    const params = {
      page: 1,
      pageSize: 20,
      topicId: currentTopic.value
    }
    const res = await getPosts(params)
    posts.value = res.records || []
  } catch (e) {
    posts.value = generateMockPosts()
  }
  isLoading.value = false
}

function generateMockPosts() {
  const contents = [
    '今天在画廊看到一幅特别震撼的作品，分享给大家~',
    '新入的收藏，求各位大神鉴赏一下',
    '艺术品投资有什么建议吗？',
    '分享一下我最近的收藏心得',
    '罗中立老师的作品真的太赞了！'
  ]
  
  return contents.map((content, i) => ({
    id: i + 1,
    userId: 1000 + i,
    authorName: '用户' + (1000 + i),
    content: content,
    images: i % 2 === 0 ? [`https://picsum.photos/400/300?random=${i + 30}`] : [],
    likeCount: Math.floor(Math.random() * 100),
    commentCount: Math.floor(Math.random() * 30),
    createTime: new Date(Date.now() - i * 3600000).toISOString(),
    isLiked: false
  }))
}

async function onRefresh() {
  await loadPosts()
}

function switchTab(tab) {
  activeTab.value = tab
  loadPosts()
}

function switchTopic(id) {
  currentTopic.value = currentTopic.value === id ? null : id
  loadPosts()
}

async function toggleLike(post) {
  if (!isLoggedIn.value) {
    uni.navigateTo({ url: '/pages/index/login' })
    return
  }
  
  try {
    if (post.isLiked) {
      await unlikePost(post.id)
      post.likeCount--
    } else {
      await likePost(post.id)
      post.likeCount++
    }
    post.isLiked = !post.isLiked
  } catch (e) {
    post.isLiked = !post.isLiked
    post.likeCount += post.isLiked ? 1 : -1
  }
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/community/detail?id=${id}` })
}

function previewImages(post) {
  uni.previewImage({ urls: post.images })
}

function sharePost(post) {
  uni.showShareMenu()
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  const hours = Math.floor(diff / 3600000)
  
  if (hours < 1) return '刚刚'
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  return `${date.getMonth() + 1}-${date.getDate()}`
}
</script>

<style lang="scss" scoped>
.community-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.community-tabs {
  display: flex;
  background: #fff;
  border-bottom: 1rpx solid #eee;
  
  .tab-item {
    flex: 1;
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 30rpx;
    color: #666;
    position: relative;
    
    &.active {
      color: #333;
      font-weight: 600;
      
      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        width: 48rpx;
        height: 6rpx;
        background: #333;
        border-radius: 3rpx;
      }
    }
  }
}

.topic-bar {
  background: #fff;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #eee;
  
  scroll-view {
    white-space: nowrap;
    padding: 0 24rpx;
  }
  
  .topic-tag {
    display: inline-block;
    padding: 12rpx 24rpx;
    font-size: 26rpx;
    color: #666;
    background: #f5f5f5;
    border-radius: 32rpx;
    margin-right: 16rpx;
    
    &.active {
      background: #333;
      color: #fff;
    }
  }
}

.post-list {
  height: calc(100vh - 180rpx);
  padding: 16rpx;
}

.post-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  
  .post-header {
    display: flex;
    align-items: center;
    margin-bottom: 16rpx;
    
    .post-avatar {
      width: 72rpx;
      height: 72rpx;
      border-radius: 50%;
    }
    
    .post-author {
      margin-left: 16rpx;
      
      .author-name {
        display: block;
        font-size: 28rpx;
        color: #333;
      }
      
      .post-time {
        font-size: 22rpx;
        color: #999;
      }
    }
  }
  
  .post-content {
    font-size: 28rpx;
    color: #333;
    line-height: 1.6;
    margin-bottom: 16rpx;
  }
  
  .post-images {
    display: flex;
    flex-wrap: wrap;
    gap: 8rpx;
    margin-bottom: 16rpx;
    
    .post-image {
      width: calc(33.33% - 6rpx);
      height: 200rpx;
      border-radius: 8rpx;
    }
  }
  
  .post-actions {
    display: flex;
    justify-content: space-around;
    padding-top: 16rpx;
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

.fab {
  position: fixed;
  right: 40rpx;
  bottom: 200rpx;
  width: 100rpx;
  height: 100rpx;
  background: #333;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  color: #fff;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.2);
}
</style>
