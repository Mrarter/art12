<template>
  <view class="feedback-page">
    <!-- 反馈类型 -->
    <view class="section">
      <view class="section-title">反馈类型</view>
      <view class="type-grid">
        <view 
          class="type-item" 
          v-for="type in feedbackTypes" 
          :key="type.value"
          :class="{ active: currentType === type.value }"
          @click="selectType(type.value)"
        >
          
          <text class="type-text">{{ type.label }}</text>
        </view>
      </view>
    </view>

    <!-- 问题描述 -->
    <view class="section">
      <view class="section-title">问题描述</view>
      <view class="textarea-wrap">
        <textarea 
          class="content-textarea"
          v-model="content"
          placeholder="请详细描述您遇到的问题或建议..."
          maxlength="500"
          @input="onContentInput"
        />
        <view class="word-count">{{ content.length }}/500</view>
      </view>
    </view>

    <!-- 上传图片 -->
    <view class="section">
      <view class="section-title">上传图片（选填）</view>
      <view class="upload-section">
        <view class="upload-list">
          <view class="upload-item" v-for="(img, index) in images" :key="index">
            <image :src="img" mode="aspectFill"></image>
            <view class="delete-btn" @click="deleteImage(index)">
              
            </view>
          </view>
          <view class="upload-btn" v-if="images.length < 6" @click="chooseImage">
            
            <text class="upload-text">添加图片</text>
          </view>
        </view>
        <view class="upload-hint">最多上传6张图片，每张不超过5MB</view>
      </view>
    </view>

    <!-- 联系方式 -->
    <view class="section">
      <view class="section-title">联系方式（选填）</view>
      <view class="input-item">
        <text class="input-label">手机号</text>
        <input 
          class="input-field"
          type="number"
          v-model="phone"
          placeholder="请输入手机号码"
          maxlength="11"
        />
      </view>
      <view class="input-item">
        <text class="input-label">邮箱</text>
        <input 
          class="input-field"
          type="text"
          v-model="email"
          placeholder="请输入邮箱地址"
        />
      </view>
    </view>

    <!-- 常见问题 -->
    <view class="section faq-section">
      <view class="section-header">
        <text class="section-title">常见问题</text>
        <view class="more-btn" @click="goFaq">
          <text>查看更多</text>
          
        </view>
      </view>
      <view class="faq-list">
        <view 
          class="faq-item" 
          v-for="(faq, index) in faqList" 
          :key="index"
          @click="goFaqDetail(faq)"
        >
          <view class="faq-q">
            <text class="q-icon">Q</text>
            <text class="q-text">{{ faq.question }}</text>
          </view>
          
        </view>
      </view>
    </view>

    <!-- 提交按钮 -->
    <view class="submit-section">
      <button class="submit-btn" :class="{ disabled: !canSubmit }" @click="submitFeedback">
        提交反馈
      </button>
    </view>

    <!-- 提交成功弹窗 -->
    <!-- 弹窗开始 -->
      <view class="success-modal">
        <view class="success-icon">
          
        </view>
        <view class="success-title">反馈提交成功</view>
        <view class="success-desc">
          <text>感谢您的反馈，我们会尽快处理</text>
          <text>处理结果将发送至您的消息通知</text>
        </view>
        <view class="success-actions">
          <button class="action-btn primary" @click="goMessage">查看消息</button>
          <button class="action-btn secondary" @click="goBack">返回</button>
        </view>
      </view>
<!-- 弹窗结束 -->
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'

// 反馈类型
const feedbackTypes = [
  { value: 'function', label: '功能异常', icon: 'error' },
  { value: 'suggest', label: '功能建议', icon: 'edit-pen' },
  { value: 'content', label: '内容问题', icon: 'file-text' },
  { value: 'complaint', label: '投诉建议', icon: 'bell' },
  { value: 'business', label: '商务合作', icon: 'bag' },
  { value: 'other', label: '其他问题', icon: 'more-dotfill' }
]

// 状态
const currentType = ref('')
const content = ref('')
const images = ref([])
const phone = ref('')
const email = ref('')
const showSuccessModal = ref(false)

// FAQ列表
const faqList = ref([
  { id: 1, question: '如何成为认证艺术家？' },
  { id: 2, question: '作品审核需要多长时间？' },
  { id: 3, question: '佣金提现多久到账？' },
  { id: 4, question: '如何参与拍卖？' },
  { id: 5, question: '保证金如何退还？' }
])

// 计算属性
const canSubmit = computed(() => {
  return currentType.value && content.value.trim().length >= 10
})

// 选择类型
const selectType = (type) => {
  currentType.value = type
}

// 内容输入
const onContentInput = () => {
  // 实时统计字数
}

// 选择图片
const chooseImage = () => {
  uni.chooseImage({
    count: 6 - images.value.length,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      images.value = [...images.value, ...res.tempFilePaths]
    }
  })
}

// 删除图片
const deleteImage = (index) => {
  images.value.splice(index, 1)
}

// 提交反馈
const submitFeedback = () => {
  if (!canSubmit.value) {
    if (!currentType.value) {
      uni.showToast({ title: '请选择反馈类型', icon: 'none' })
    } else if (content.value.trim().length < 10) {
      uni.showToast({ title: '请详细描述问题（至少10个字）', icon: 'none' })
    }
    return
  }

  uni.showLoading({ title: '提交中...' })
  
  // 模拟提交
  setTimeout(() => {
    uni.hideLoading()
    showSuccessModal.value = true
  }, 1500)
}

// 查看FAQ
const goFaq = () => {
  uni.navigateTo({ url: '/pages/help/index' })
}

// FAQ详情
const goFaqDetail = (faq) => {
  uni.navigateTo({ url: `/pages/common/coming-soon?title=${encodeURIComponent(faq.title || 'FAQ详情')}&desc=常见问题详情页正在整理中，可先查看帮助中心列表。` })
}

// 查看消息
const goMessage = () => {
  showSuccessModal.value = false
  uni.navigateTo({ url: '/pages/user/message' })
}

// 返回
const goBack = () => {
  showSuccessModal.value = false
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.feedback-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding: 20rpx;
}

.section {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;

  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 20rpx;
  }
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20rpx;

  .type-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 30rpx 20rpx;
    background: #f5f6f8;
    border-radius: 16rpx;
    border: 2rpx solid transparent;
    transition: all 0.2s;

    &.active {
      background: rgba(102, 126, 234, 0.1);
      border-color: #667eea;

      .type-text {
        color: #667eea;
      }
    }

    .type-text {
      font-size: 26rpx;
      color: #666;
      margin-top: 12rpx;
    }
  }
}

.textarea-wrap {
  position: relative;

  .content-textarea {
    width: 100%;
    height: 240rpx;
    background: #f5f6f8;
    border-radius: 12rpx;
    padding: 24rpx;
    font-size: 28rpx;
    color: #333;
    line-height: 1.6;
  }

  .word-count {
    position: absolute;
    bottom: 16rpx;
    right: 16rpx;
    font-size: 24rpx;
    color: #999;
  }
}

.upload-section {
  .upload-list {
    display: flex;
    flex-wrap: wrap;
    gap: 20rpx;

    .upload-item {
      position: relative;
      width: 200rpx;
      height: 200rpx;
      border-radius: 12rpx;
      overflow: hidden;

      image {
        width: 100%;
        height: 100%;
      }

      .delete-btn {
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

    .upload-btn {
      width: 200rpx;
      height: 200rpx;
      background: #f5f6f8;
      border-radius: 12rpx;
      border: 2rpx dashed #ddd;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      .upload-text {
        font-size: 24rpx;
        color: #999;
        margin-top: 12rpx;
      }
    }
  }

  .upload-hint {
    font-size: 24rpx;
    color: #999;
    margin-top: 16rpx;
  }
}

.input-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }

  .input-label {
    width: 140rpx;
    font-size: 28rpx;
    color: #333;
  }

  .input-field {
    flex: 1;
    font-size: 28rpx;
    color: #333;
  }
}

.faq-section {
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .more-btn {
      display: flex;
      align-items: center;
      font-size: 26rpx;
      color: #999;
    }
  }

  .faq-list {
    .faq-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 24rpx 0;
      border-bottom: 1rpx solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .faq-q {
        flex: 1;
        display: flex;
        align-items: flex-start;

        .q-icon {
          width: 36rpx;
          height: 36rpx;
          background: #667eea;
          color: #fff;
          font-size: 22rpx;
          border-radius: 8rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: 16rpx;
          flex-shrink: 0;
        }

        .q-text {
          font-size: 28rpx;
          color: #333;
          line-height: 1.4;
        }
      }
    }
  }
}

.submit-section {
  padding: 30rpx 0;

  .submit-btn {
    width: 100%;
    height: 88rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    font-size: 32rpx;
    font-weight: 500;
    border-radius: 44rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;

    &.disabled {
      background: #ccc;
    }
  }
}

.success-modal {
  padding: 60rpx 40rpx;
  text-align: center;
  width: 600rpx;

  .success-icon {
    margin-bottom: 20rpx;
  }

  .success-title {
    font-size: 36rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 16rpx;
  }

  .success-desc {
    display: flex;
    flex-direction: column;
    gap: 8rpx;
    font-size: 26rpx;
    color: #666;
    margin-bottom: 40rpx;
  }

  .success-actions {
    display: flex;
    flex-direction: column;
    gap: 20rpx;

    .action-btn {
      height: 80rpx;
      border-radius: 40rpx;
      font-size: 28rpx;

      &.primary {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
      }

      &.secondary {
        background: #f5f6f8;
        color: #666;
      }
    }
  }
}
</style>
