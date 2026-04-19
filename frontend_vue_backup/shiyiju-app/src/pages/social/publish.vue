<template>
  <view class="publish-page">
    <!-- 富文本输入 -->
    <view class="input-section">
      <textarea 
        class="content-input" 
        v-model="content" 
        placeholder="分享你的艺术见解..."
        :maxlength="2000"
      />
      <view class="char-count">{{ content.length }}/2000</view>
    </view>

    <!-- 图片上传 -->
    <view class="upload-section">
      <view class="upload-header">
        <text class="section-title">图片</text>
        <text class="upload-tip">{{ imageList.length }}/9</text>
      </view>
      <view class="image-grid">
        <view class="image-item" v-for="(img, idx) in imageList" :key="idx">
          <image :src="img" mode="aspectFill" />
          <view class="remove-btn" @click="removeImage(idx)">×</view>
        </view>
        <view class="upload-btn" v-if="imageList.length < 9" @click="chooseImage">
          <text class="icon">+</text>
          <text class="text">添加图片</text>
        </view>
      </view>
    </view>

    <!-- 话题选择 -->
    <view class="topic-section">
      <view class="section-header">
        <text class="section-title">添加话题</text>
      </view>
      <view class="topic-list">
        <view 
          class="topic-item" 
          :class="{ selected: selectedTopics.includes(topic.id) }"
          v-for="topic in hotTopics" 
          :key="topic.id"
          @click="toggleTopic(topic)"
        >
          <text class="topic-name">#{{ topic.name }}</text>
          <text class="topic-count">{{ topic.postCount }}帖子</text>
        </view>
      </view>
    </view>

    <!-- 定位 -->
    <view class="location-section">
      <view class="section-header">
        <text class="section-title">位置</text>
      </view>
      <view class="location-item" @click="chooseLocation">
        <text class="icon">📍</text>
        <text class="location-text" v-if="location">{{ location }}</text>
        <text class="location-text default" v-else>添加位置</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- @好友 -->
    <view class="mention-section">
      <view class="section-header">
        <text class="section-title">@好友</text>
      </view>
      <view class="mention-item" @click="chooseMention">
        <text class="icon">@</text>
        <text class="mention-text" v-if="mentionedUsers.length > 0">
          已@{{ mentionedUsers.length }}位好友
        </text>
        <text class="mention-text default" v-else>添加@好友</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 发布按钮 -->
    <view class="submit-bar safe-area-bottom">
      <button class="btn-publish" @click="handlePublish" :disabled="!content.trim()" :loading="publishing">
        发布
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'

const content = ref('')
const imageList = ref([])
const hotTopics = ref([
  { id: 1, name: '艺术欣赏', postCount: 1234 },
  { id: 2, name: '当代艺术', postCount: 856 },
  { id: 3, name: '收藏心得', postCount: 654 },
  { id: 4, name: '展览打卡', postCount: 432 },
  { id: 5, name: '艺术资讯', postCount: 321 },
  { id: 6, name: '创作灵感', postCount: 256 }
])
const selectedTopics = ref([])
const location = ref('')
const mentionedUsers = ref([])
const publishing = ref(false)

function chooseImage() {
  uni.chooseImage({
    count: 9 - imageList.value.length,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      imageList.value.push(...res.tempFilePaths)
    }
  })
}

function removeImage(index) {
  imageList.value.splice(index, 1)
}

function toggleTopic(topic) {
  const idx = selectedTopics.value.indexOf(topic.id)
  if (idx > -1) {
    selectedTopics.value.splice(idx, 1)
  } else {
    if (selectedTopics.value.length < 3) {
      selectedTopics.value.push(topic.id)
    } else {
      uni.showToast({ title: '最多选择3个话题', icon: 'none' })
    }
  }
}

function chooseLocation() {
  uni.chooseLocation({
    success: (res) => {
      location.value = res.name || res.address
    }
  })
}

function chooseMention() {
  uni.showToast({ title: '选择好友功能开发中', icon: 'none' })
}

async function handlePublish() {
  if (!content.value.trim()) {
    uni.showToast({ title: '请输入内容', icon: 'none' })
    return
  }

  publishing.value = true

  try {
    // 模拟发布成功
    setTimeout(() => {
      uni.showToast({ title: '发布成功', icon: 'success' })
      
      setTimeout(() => {
        uni.navigateBack()
      }, 1500)
    }, 1000)
  } catch (e) {
    uni.showToast({ title: '发布失败', icon: 'none' })
  } finally {
    publishing.value = false
  }
}
</script>

<style lang="scss" scoped>
.publish-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 120rpx;
}

.input-section {
  background: #fff;
  padding: 32rpx;
  margin-bottom: 16rpx;
  position: relative;

  .content-input {
    width: 100%;
    height: 300rpx;
    font-size: 30rpx;
    line-height: 1.6;
    color: #333;
  }

  .char-count {
    position: absolute;
    bottom: 16rpx;
    right: 32rpx;
    font-size: 24rpx;
    color: #999;
  }
}

.upload-section {
  background: #fff;
  padding: 24rpx 32rpx;
  margin-bottom: 16rpx;

  .upload-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20rpx;

    .section-title {
      font-size: 28rpx;
      color: #333;
      font-weight: 500;
    }

    .upload-tip {
      font-size: 24rpx;
      color: #999;
    }
  }

  .image-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;

    .image-item {
      width: 200rpx;
      height: 200rpx;
      border-radius: 8rpx;
      position: relative;
      overflow: hidden;

      image {
        width: 100%;
        height: 100%;
      }

      .remove-btn {
        position: absolute;
        top: 8rpx;
        right: 8rpx;
        width: 40rpx;
        height: 40rpx;
        background: rgba(0, 0, 0, 0.5);
        color: #fff;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28rpx;
      }
    }

    .upload-btn {
      width: 200rpx;
      height: 200rpx;
      background: #f5f5f5;
      border-radius: 8rpx;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8rpx;

      .icon {
        font-size: 64rpx;
        color: #999;
      }

      .text {
        font-size: 24rpx;
        color: #999;
      }
    }
  }
}

.topic-section {
  background: #fff;
  padding: 24rpx 32rpx;
  margin-bottom: 16rpx;

  .section-header {
    margin-bottom: 20rpx;

    .section-title {
      font-size: 28rpx;
      color: #333;
      font-weight: 500;
    }
  }

  .topic-list {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;

    .topic-item {
      display: flex;
      align-items: center;
      gap: 12rpx;
      padding: 16rpx 24rpx;
      background: #f5f5f5;
      border-radius: 32rpx;
      border: 2rpx solid transparent;

      &.selected {
        background: #f0f9ff;
        border-color: #409eff;
      }

      .topic-name {
        font-size: 26rpx;
        color: #333;
      }

      .topic-count {
        font-size: 22rpx;
        color: #999;
      }
    }
  }
}

.location-section,
.mention-section {
  background: #fff;
  padding: 24rpx 32rpx;
  margin-bottom: 16rpx;

  .section-header {
    margin-bottom: 20rpx;

    .section-title {
      font-size: 28rpx;
      color: #333;
      font-weight: 500;
    }
  }

  .location-item,
  .mention-item {
    display: flex;
    align-items: center;
    gap: 12rpx;

    .icon {
      font-size: 32rpx;
    }

    .location-text,
    .mention-text {
      flex: 1;
      font-size: 28rpx;
      color: #333;

      &.default {
        color: #999;
      }
    }

    .arrow {
      font-size: 36rpx;
      color: #ccc;
    }
  }
}

.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16rpx 32rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #eee;

  .btn-publish {
    width: 100%;
    height: 96rpx;
    background: #333;
    color: #fff;
    border-radius: 48rpx;
    font-size: 32rpx;
    font-weight: 500;
    display: flex;
    align-items: center;
    justify-content: center;

    &[disabled] {
      background: #ccc;
    }
  }
}
</style>
