<template>
  <view class="publish-page">
    <!-- 导航栏 -->
    <view class="nav-bar">
      <text class="nav-cancel" @click="handleCancel">取消</text>
      <text class="nav-title">发布动态</text>
      <text
        class="nav-publish"
        :class="{ active: canPublish }"
        @click="handlePublish"
      >发布</text>
    </view>

    <!-- 内容输入 -->
    <view class="content-section">
      <textarea
        class="content-input"
        v-model="content"
        placeholder="分享你的想法，与艺术同好交流..."
        maxlength="500"
        @focus="onFocus"
      ></textarea>
      <view class="char-count">
        <text>{{ content.length }}</text>/500
      </view>
    </view>

    <!-- 话题选择 -->
    <view class="topic-section">
      <view class="section-title">选择话题</view>
      <scroll-view class="topic-list" scroll-x>
        <view
          class="topic-item"
          :class="{ active: selectedTopicId === null }"
          @click="selectedTopicId = null"
        >不选择</view>
        <view
          class="topic-item"
          :class="{ active: selectedTopicId === topic.id }"
          v-for="topic in topics"
          :key="topic.id"
          @click="selectedTopicId = topic.id"
        ># {{ topic.name }}</view>
      </scroll-view>
    </view>

    <!-- 图片上传 -->
    <view class="images-section">
      <view class="section-title">添加图片（可选，最多9张）</view>
      <view class="images-grid">
        <view
          class="image-item"
          v-for="(img, index) in images"
          :key="index"
        >
          <image :src="img" mode="aspectFill" @click="previewImages(index)"></image>
          <view class="remove-btn" @click="removeImage(index)">
            <text>×</text>
          </view>
        </view>
        <view
          class="add-image"
          v-if="images.length < 9"
          @click="chooseImage"
        >
          
          <text>添加图片</text>
        </view>
      </view>
    </view>

    <!-- 关联作品 -->
    <view class="artwork-section">
      <view class="section-title">关联作品（可选）</view>
      <view class="artwork-selector" @click="selectArtwork">
        <image
          v-if="selectedArtwork"
          :src="selectedArtwork.cover"
          mode="aspectFill"
          class="artwork-cover"
        ></image>
        <view class="artwork-placeholder" v-else>
          <text>🖼</text>
          <text>选择作品</text>
        </view>
        <view class="artwork-info" v-if="selectedArtwork">
          <text class="artwork-title">{{ selectedArtwork.title }}</text>
          <text class="artwork-price">¥{{ formatPrice(selectedArtwork.price) }}</text>
        </view>
        <view class="clear-artwork" v-if="selectedArtwork" @click.stop="clearArtwork">
          
        </view>
      </view>
    </view>

    <!-- 协议提示 -->
    <view class="agreement-tip">
      <text>发布即表示你同意</text>
      <text class="link" @click="goAgreement">《拾艺局社区公约》</text>
    </view>
  </view>
</template>

<script>
import { createPost, getTopics } from '@/api/community'
import { uploadFile } from '@/api/file'

export default {
  data() {
    return {
      content: '',
      images: [],
      topics: [],
      selectedTopicId: null,
      selectedArtwork: null,
      isSubmitting: false
    }
  },

  computed: {
    canPublish() {
      return this.content.trim().length > 0 && !this.isSubmitting
    }
  },

  onLoad() {
    this.loadTopics()
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

    handleCancel() {
      if (this.content.trim() || this.images.length > 0) {
        uni.showModal({
          title: '提示',
          content: '确定放弃发布吗？',
          success: (res) => {
            if (res.confirm) {
              uni.navigateBack()
            }
          }
        })
      } else {
        uni.navigateBack()
      }
    },

    onFocus() {
      // 滚动到输入框
    },

    async handlePublish() {
      if (!this.canPublish) {
        uni.showToast({ title: '请输入内容', icon: 'none' })
        return
      }

      if (this.isSubmitting) return
      this.isSubmitting = true

      try {
        // 上传图片
        let imagesStr = ''
        if (this.images.length > 0) {
          const uploadPromises = this.images.map(img => {
            // 如果是本地路径，需要上传
            if (img.startsWith('blob:') || img.startsWith('file:') || img.startsWith('/tmp')) {
              return uploadFile(img)
            }
            return Promise.resolve(img)
          })
          const uploadedUrls = await Promise.all(uploadPromises)
          imagesStr = uploadedUrls.join(',')
        }

        // 创建帖子
        const res = await createPost({
          content: this.content.trim(),
          images: imagesStr,
          topicId: this.selectedTopicId,
          artworkId: this.selectedArtwork ? this.selectedArtwork.id : null
        })

        if (res.code === 200) {
          uni.showToast({ title: '发布成功', icon: 'success' })
          setTimeout(() => {
            uni.navigateBack()
          }, 1500)
        } else {
          uni.showToast({ title: res.message || '发布失败', icon: 'none' })
        }
      } catch (e) {
        console.error('发布失败', e)
        uni.showToast({ title: '发布失败', icon: 'none' })
      } finally {
        this.isSubmitting = false
      }
    },

    chooseImage() {
      uni.chooseImage({
        count: 9 - this.images.length,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          this.images = [...this.images, ...res.tempFilePaths]
        }
      })
    },

    removeImage(index) {
      this.images.splice(index, 1)
    },

    previewImages(index) {
      uni.previewImage({
        current: index,
        urls: this.images
      })
    },

    selectArtwork() {
      // 可以跳转到作品选择页面
      uni.showToast({ title: '请到画廊选择作品', icon: 'none' })
    },

    clearArtwork() {
      this.selectedArtwork = null
    },

    goAgreement() {
      uni.showToast({ title: '查看社区公约', icon: 'none' })
    },

    formatPrice(price) {
      if (!price) return '0'
      const yuan = price / 100  // 分转元
      if (yuan >= 10000) {
        return (yuan / 10000).toFixed(yuan % 10000 === 0 ? 0 : 1) + '万'
      }
      return yuan.toLocaleString()
    }
  }
}
</script>

<style lang="scss" scoped>
.publish-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: env(safe-area-inset-bottom);
}

.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
}

.nav-cancel {
  font-size: 30rpx;
  color: #666;
}

.nav-title {
  font-size: 34rpx;
  color: #333;
  font-weight: 600;
}

.nav-publish {
  font-size: 30rpx;
  color: #ccc;
}

.nav-publish.active {
  color: #667eea;
}

.content-section {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.content-input {
  width: 100%;
  height: 300rpx;
  font-size: 32rpx;
  color: #333;
  line-height: 1.6;
}

.char-count {
  text-align: right;
  font-size: 24rpx;
  color: #999;
  margin-top: 16rpx;
}

.section-title {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  margin-bottom: 20rpx;
}

.topic-section {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.topic-list {
  white-space: nowrap;
}

.topic-item {
  display: inline-block;
  padding: 14rpx 28rpx;
  margin-right: 16rpx;
  font-size: 26rpx;
  color: #666;
  background: #f5f5f5;
  border-radius: 30rpx;
}

.topic-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.images-section {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.images-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.image-item {
  position: relative;
  width: 200rpx;
  height: 200rpx;
}

.image-item image {
  width: 100%;
  height: 100%;
  border-radius: 12rpx;
}

.remove-btn {
  position: absolute;
  top: -16rpx;
  right: -16rpx;
  width: 44rpx;
  height: 44rpx;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-btn text {
  color: #fff;
  font-size: 32rpx;
}

.add-image {
  width: 200rpx;
  height: 200rpx;
  background: #f5f5f5;
  border: 2rpx dashed #ddd;
  border-radius: 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.add-image text {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}

.artwork-section {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.artwork-selector {
  display: flex;
  align-items: center;
  padding: 20rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
}

.artwork-cover {
  width: 120rpx;
  height: 120rpx;
  border-radius: 8rpx;
  margin-right: 20rpx;
}

.artwork-placeholder {
  width: 120rpx;
  height: 120rpx;
  background: #fff;
  border-radius: 8rpx;
  margin-right: 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.artwork-placeholder text {
  font-size: 22rpx;
  color: #ccc;
  margin-top: 8rpx;
}

.artwork-info {
  flex: 1;
}

.artwork-title {
  display: block;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 8rpx;
}

.artwork-price {
  font-size: 28rpx;
  color: #e74c3c;
}

.clear-artwork {
  padding: 10rpx;
}

.agreement-tip {
  text-align: center;
  padding: 30rpx;
  font-size: 24rpx;
  color: #999;
}

.agreement-tip .link {
  color: #667eea;
  margin-left: 8rpx;
}
</style>
