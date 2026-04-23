<template>
  <view class="review-page">
    <!-- 商品信息 -->
    <view class="product-info">
      <image class="product-image" :src="order.cover || '/static/icons/artwork-default.png'" mode="aspectFill"></image>
      <view class="product-detail">
        <text class="product-name">{{ order.title }}</text>
        <text class="product-author">{{ order.artistName }}</text>
      </view>
    </view>

    <!-- 评分 -->
    <view class="rating-section card">
      <view class="section-title">商品评分</view>
      <view class="rating-stars">
        <view
          class="star-item"
          v-for="star in 5"
          :key="star"
          @click="setRating(star)"
        >
          <text>›</text>
            :name="star <= rating ? 'star-fill' : 'star'"
            :color="star <= rating ? '#f6d365' : '#ddd'"
            size="48"
          >
        </view>
      </view>
      <text class="rating-label">{{ ratingLabels[rating - 1] || '请评分' }}</text>
    </view>

    <!-- 评价内容 -->
    <view class="content-section card">
      <view class="section-title">评价内容</view>
      <textarea
        class="content-input"
        v-model="content"
        placeholder="分享你的购买体验，帮助其他收藏家做出更好的选择..."
        maxlength="500"
      ></textarea>
      <view class="char-count">{{ content.length }}/500</view>
    </view>

    <!-- 上传图片 -->
    <view class="images-section card">
      <view class="section-title">上传图片（选填，最多9张）</view>
      <view class="images-grid">
        <view class="image-item" v-for="(img, index) in images" :key="index">
          <image :src="img" mode="aspectFill"></image>
          <view class="remove-btn" @click="removeImage(index)">
            <text>×</text>
          </view>
        </view>
        <view class="add-image" v-if="images.length < 9" @click="chooseImage">
          
          <text>添加图片</text>
        </view>
      </view>
    </view>

    <!-- 匿名评价 -->
    <view class="anonymous-section card">
      <view class="anonymous-row">
        <view class="anonymous-info">
          <text class="anonymous-title">匿名评价</text>
          <text class="anonymous-desc">匿名后，其他用户看不到你的头像和昵称</text>
        </view>
        <switch
          :checked="isAnonymous"
          @change="isAnonymous = !isAnonymous"
          color="#667eea"
        />
      </view>
    </view>

    <!-- 提交按钮 -->
    <view class="submit-bar">
      <button class="submit-btn" @click="submitReview">提交评价</button>
    </view>
  </view>
</template>

<script>
import { submitReview } from '@/api/order'
import { uploadFile } from '@/api/file'

export default {
  data() {
    return {
      orderId: null,
      order: {
        title: '',
        artistName: '',
        cover: ''
      },
      rating: 5,
      ratingLabels: ['很差', '较差', '一般', '满意', '非常满意'],
      content: '',
      images: [],
      isAnonymous: false
    }
  },

  onLoad(options) {
    if (options.orderId) {
      this.orderId = Number(options.orderId)
    }
    if (options.orderData) {
      try {
        this.order = JSON.parse(decodeURIComponent(options.orderData))
      } catch (e) {
        console.error('解析订单数据失败', e)
      }
    }
  },

  methods: {
    setRating(star) {
      this.rating = star
    },

    async submitReview() {
      if (!this.content.trim()) {
        uni.showToast({ title: '请输入评价内容', icon: 'none' })
        return
      }

      // 上传图片
      let imagesStr = ''
      if (this.images.length > 0) {
        uni.showLoading({ title: '上传中...' })
        try {
          const uploadPromises = this.images.map(img => {
            if (img.startsWith('blob:') || img.startsWith('file:') || img.startsWith('/tmp')) {
              return uploadFile(img)
            }
            return Promise.resolve(img)
          })
          const uploadedUrls = await Promise.all(uploadPromises)
          imagesStr = uploadedUrls.join(',')
        } catch (e) {
          console.error('上传图片失败', e)
        }
        uni.hideLoading()
      }

      try {
        uni.showLoading({ title: '提交中...' })
        const res = await submitReview({
          orderId: this.orderId,
          rating: this.rating,
          content: this.content.trim(),
          images: imagesStr,
          isAnonymous: this.isAnonymous ? 1 : 0
        })

        if (res.code === 200) {
          uni.showToast({ title: '评价成功', icon: 'success' })
          setTimeout(() => {
            uni.navigateBack()
          }, 1500)
        } else {
          uni.showToast({ title: res.message || '提交失败', icon: 'none' })
        }
      } catch (e) {
        uni.showToast({ title: '提交失败', icon: 'none' })
      } finally {
        uni.hideLoading()
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
    }
  }
}
</script>

<style lang="scss" scoped>
.review-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 140rpx;
}

.card {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.section-title {
  font-size: 28rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 20rpx;
}

/* 商品信息 */
.product-info {
  display: flex;
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.product-image {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  margin-right: 24rpx;
  background: #f0f0f0;
}

.product-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.product-name {
  display: block;
  font-size: 30rpx;
  color: #333;
  font-weight: 500;
  margin-bottom: 8rpx;
}

.product-author {
  display: block;
  font-size: 24rpx;
  color: #999;
}

/* 评分 */
.rating-section {
  text-align: center;
}

.rating-stars {
  display: flex;
  justify-content: center;
  margin-bottom: 16rpx;
}

.star-item {
  padding: 10rpx;
}

.rating-label {
  font-size: 28rpx;
  color: #f6d365;
  font-weight: 500;
}

/* 评价内容 */
.content-input {
  width: 100%;
  height: 240rpx;
  padding: 20rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #333;
  line-height: 1.6;
}

.char-count {
  text-align: right;
  font-size: 22rpx;
  color: #999;
  margin-top: 12rpx;
}

/* 上传图片 */
.images-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.image-item {
  position: relative;
  width: 180rpx;
  height: 180rpx;

  image {
    width: 100%;
    height: 100%;
    border-radius: 12rpx;
  }
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

  text {
    color: #fff;
    font-size: 32rpx;
  }
}

.add-image {
  width: 180rpx;
  height: 180rpx;
  background: #f9f9f9;
  border: 2rpx dashed #ddd;
  border-radius: 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  text {
    font-size: 22rpx;
    color: #999;
    margin-top: 8rpx;
  }
}

/* 匿名评价 */
.anonymous-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.anonymous-info {
  flex: 1;
}

.anonymous-title {
  display: block;
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  margin-bottom: 8rpx;
}

.anonymous-desc {
  display: block;
  font-size: 22rpx;
  color: #999;
}

/* 提交按钮 */
.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2rpx 20rpx rgba(0, 0, 0, 0.1);
}

.submit-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 44rpx;
  font-size: 32rpx;
  font-weight: 500;
}
</style>
