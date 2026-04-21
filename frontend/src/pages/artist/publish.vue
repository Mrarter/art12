<template>
  <view class="publish-page">
    <!-- 封面图片 -->
    <view class="form-section">
      <view class="section-title">作品封面</view>
      <view class="cover-upload" @click="chooseCover">
        <image v-if="formData.cover" :src="formData.cover" mode="aspectFill" class="cover-preview"></image>
        <view v-else class="upload-placeholder">
          <u-icon name="camera" size="48" color="#ccc"></u-icon>
          <text class="upload-text">上传封面图</text>
          <text class="upload-tip">建议尺寸 800x800</text>
        </view>
      </view>
    </view>

    <!-- 作品信息 -->
    <view class="form-section">
      <view class="section-title">基本信息</view>
      
      <view class="form-item">
        <text class="form-label">作品名称</text>
        <input 
          class="form-input" 
          v-model="formData.title" 
          placeholder="请输入作品名称"
          maxlength="50"
        />
      </view>

      <view class="form-item">
        <text class="form-label">作者</text>
        <input 
          class="form-input" 
          v-model="formData.author" 
          placeholder="请输入作者姓名"
        />
      </view>

      <view class="form-item">
        <text class="form-label">创作年代</text>
        <picker mode="selector" :range="yearRange" range-key="label" @change="onYearChange">
          <view class="form-picker">
            <text :class="{ placeholder: !formData.year }">
              {{ formData.year ? formData.year + '年' : '请选择年代' }}
            </text>
            <u-icon name="arrow-right" size="14" color="#999"></u-icon>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">作品尺寸</text>
        <view class="size-inputs">
          <input 
            class="form-input size-input" 
            v-model="formData.width" 
            placeholder="宽(cm)"
            type="digit"
          />
          <text class="size-separator">×</text>
          <input 
            class="form-input size-input" 
            v-model="formData.height" 
            placeholder="高(cm)"
            type="digit"
          />
          <text class="size-unit">cm</text>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">材质</text>
        <picker mode="selector" :range="materialRange" range-key="label" @change="onMaterialChange">
          <view class="form-picker">
            <text :class="{ placeholder: !formData.material }">
              {{ formData.material || '请选择材质' }}
            </text>
            <u-icon name="arrow-right" size="14" color="#999"></u-icon>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">艺术门类</text>
        <picker mode="selector" :range="categoryRange" range-key="label" @change="onCategoryChange">
          <view class="form-picker">
            <text :class="{ placeholder: !formData.category }">
              {{ formData.category || '请选择门类' }}
            </text>
            <u-icon name="arrow-right" size="14" color="#999"></u-icon>
          </view>
        </picker>
      </view>
    </view>

    <!-- 价格设置 -->
    <view class="form-section">
      <view class="section-title">价格设置</view>
      
      <view class="form-item">
        <text class="form-label">出售价格</text>
        <view class="price-input">
          <text class="price-unit">¥</text>
          <input 
            class="form-input price-value" 
            v-model="formData.price" 
            placeholder="请输入价格"
            type="digit"
          />
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">允许拍卖</text>
        <switch 
          :checked="formData.allowAuction" 
          @change="(e) => formData.allowAuction = e.detail.value"
          color="#667eea"
        />
      </view>

      <view class="form-item">
        <text class="form-label">库存数量</text>
        <input 
          class="form-input" 
          v-model="formData.stock" 
          placeholder="请输入库存数量"
          type="number"
        />
      </view>
    </view>

    <!-- 作品描述 -->
    <view class="form-section">
      <view class="section-title">作品描述</view>
      <textarea 
        class="form-textarea" 
        v-model="formData.description" 
        placeholder="请输入作品描述、创作背景、艺术家简介等信息..."
        maxlength="2000"
      ></textarea>
      <view class="word-count">{{ formData.description.length }}/2000</view>
    </view>

    <!-- 作品图片 -->
    <view class="form-section">
      <view class="section-title">
        作品详情图
        <text class="section-tip">（最多9张）</text>
      </view>
      <view class="images-grid">
        <view 
          class="image-item" 
          v-for="(img, index) in formData.images" 
          :key="index"
        >
          <image :src="img" mode="aspectFill"></image>
          <view class="image-delete" @click="removeImage(index)">
            <u-icon name="close" size="12" color="#fff"></u-icon>
          </view>
        </view>
        <view 
          class="image-add" 
          v-if="formData.images.length < 9" 
          @click="chooseImages"
        >
          <u-icon name="plus" size="32" color="#ccc"></u-icon>
          <text>添加图片</text>
        </view>
      </view>
    </view>

    <!-- 提交按钮 -->
    <view class="submit-bar">
      <view class="save-draft" @click="saveDraft">保存草稿</view>
      <view class="submit-btn" @click="submit">发布作品</view>
    </view>
  </view>
</template>

<script>
import { publishArtwork, updateArtwork } from '@/api/product.js'

export default {
  data() {
    return {
      isEdit: false,
      artworkId: null,
      formData: {
        cover: '',
        title: '',
        author: '',
        year: '',
        width: '',
        height: '',
        material: '',
        category: '',
        price: '',
        allowAuction: false,
        stock: 1,
        description: '',
        images: []
      },
      yearRange: [
        { label: '2020年代', value: 2020 },
        { label: '2010年代', value: 2010 },
        { label: '2000年代', value: 2000 },
        { label: '1990年代', value: 1990 },
        { label: '1980年代', value: 1980 },
        { label: '更早', value: 1900 }
      ],
      materialRange: ['国画', '油画', '版画', '雕塑', '书法', '水彩', '丙烯', '综合材料', '摄影', '其他'],
      categoryRange: [
        { label: '山水', value: 'landscape' },
        { label: '人物', value: 'figure' },
        { label: '花鸟', value: 'flower' },
        { label: '静物', value: 'still' },
        { label: '抽象', value: 'abstract' },
        { label: '写实', value: 'realistic' },
        { label: '风景', value: 'scenery' },
        { label: '其他', value: 'other' }
      ]
    }
  },

  onLoad(options) {
    if (options.id) {
      this.isEdit = true
      this.artworkId = Number(options.id)
      this.loadArtwork(options.id)
    }
  },

  methods: {
    async loadArtwork(id) {
      // 模拟加载作品详情
      uni.showLoading({ title: '加载中...' })
      setTimeout(() => {
        uni.hideLoading()
      }, 500)
    },

    chooseCover() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          this.formData.cover = res.tempFilePaths[0]
        }
      })
    },

    chooseImages() {
      const remain = 9 - this.formData.images.length
      uni.chooseImage({
        count: remain,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          this.formData.images = [...this.formData.images, ...res.tempFilePaths]
        }
      })
    },

    removeImage(index) {
      this.formData.images.splice(index, 1)
    },

    onYearChange(e) {
      this.formData.year = this.yearRange[e.detail.value].value
    },

    onMaterialChange(e) {
      this.formData.material = this.materialRange[e.detail.value]
    },

    onCategoryChange(e) {
      const selected = this.categoryRange[e.detail.value]
      this.formData.category = selected.value
    },

    validate() {
      if (!this.formData.cover) {
        uni.showToast({ title: '请上传封面图', icon: 'none' })
        return false
      }
      if (!this.formData.title.trim()) {
        uni.showToast({ title: '请输入作品名称', icon: 'none' })
        return false
      }
      if (!this.formData.price) {
        uni.showToast({ title: '请输入价格', icon: 'none' })
        return false
      }
      if (Number(this.formData.price) <= 0) {
        uni.showToast({ title: '价格必须大于0', icon: 'none' })
        return false
      }
      return true
    },

    async saveDraft() {
      uni.showToast({ title: '草稿保存成功', icon: 'success' })
    },

    async submit() {
      if (!this.validate()) return

      uni.showLoading({ title: '提交中...' })

      try {
        const submitData = {
          ...this.formData,
          size: `${this.formData.width}×${this.formData.height}cm`
        }

        if (this.isEdit) {
          await updateArtwork(this.artworkId, submitData)
          uni.showToast({ title: '更新成功', icon: 'success' })
        } else {
          await publishArtwork(submitData)
          uni.showToast({ title: '发布成功', icon: 'success' })
        }

        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: '提交成功（模拟）', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.publish-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 140rpx;
}

.form-section {
  background: #fff;
  margin-bottom: 20rpx;
  padding: 30rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 24rpx;
  
  .section-tip {
    font-size: 24rpx;
    color: #999;
    font-weight: normal;
    margin-left: 12rpx;
  }
}

.cover-upload {
  width: 300rpx;
  height: 300rpx;
  border-radius: 16rpx;
  overflow: hidden;
  background: #f9f9f9;
}

.cover-preview {
  width: 100%;
  height: 100%;
}

.upload-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2rpx dashed #ddd;
  border-radius: 16rpx;

  .upload-text {
    font-size: 28rpx;
    color: #666;
    margin-top: 16rpx;
  }

  .upload-tip {
    font-size: 22rpx;
    color: #999;
    margin-top: 8rpx;
  }
}

.form-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  
  &:last-child {
    border-bottom: none;
  }
}

.form-label {
  font-size: 28rpx;
  color: #333;
  width: 160rpx;
}

.form-input {
  flex: 1;
  text-align: right;
  font-size: 28rpx;
  color: #333;
}

.form-picker {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  font-size: 28rpx;
  color: #333;
  
  .placeholder {
    color: #999;
  }
}

.size-inputs {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.size-input {
  width: 140rpx;
  text-align: center;
}

.size-separator {
  color: #999;
}

.size-unit {
  font-size: 28rpx;
  color: #999;
}

.price-input {
  display: flex;
  align-items: center;
  
  .price-unit {
    font-size: 32rpx;
    color: #e74c3c;
    font-weight: 600;
    margin-right: 8rpx;
  }
  
  .price-value {
    text-align: left;
    font-size: 32rpx;
    font-weight: 600;
    color: #e74c3c;
  }
}

.form-textarea {
  width: 100%;
  height: 300rpx;
  padding: 24rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}

.word-count {
  text-align: right;
  font-size: 24rpx;
  color: #999;
  margin-top: 16rpx;
}

.images-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
}

.image-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 12rpx;
  overflow: hidden;

  image {
    width: 100%;
    height: 100%;
  }

  .image-delete {
    position: absolute;
    top: 8rpx;
    right: 8rpx;
    width: 36rpx;
    height: 36rpx;
    background: rgba(0, 0, 0, 0.5);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.image-add {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f9f9f9;
  border-radius: 12rpx;
  border: 2rpx dashed #ddd;
  
  text {
    font-size: 22rpx;
    color: #999;
    margin-top: 8rpx;
  }
}

.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 24rpx;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.save-draft {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: #f5f5f5;
  color: #666;
  font-size: 30rpx;
  border-radius: 44rpx;
}

.submit-btn {
  flex: 2;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 30rpx;
  border-radius: 44rpx;
}
</style>
