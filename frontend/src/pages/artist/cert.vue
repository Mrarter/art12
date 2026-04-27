<template>
  <view class="cert-page">
    <!-- 顶部说明 -->
    <view class="cert-header">
      <view class="header-icon">
        
      </view>
      <view class="header-text">
        <text class="title">成为认证艺术家</text>
        <text class="desc">认证后可享受专属权益，包括专属标识、优先推荐、数据分析等功能</text>
      </view>
    </view>

    <!-- 认证状态 -->
    <view class="cert-status" v-if="certStatus.status !== 'none'">
      <view class="status-badge" :class="certStatus.status">
        
        <text class="status-text">{{ certStatus.text }}</text>
      </view>
      <view class="status-detail" v-if="certStatus.status === 'pending'">
        <text>您的认证申请正在审核中，预计1-3个工作日完成</text>
      </view>
    </view>

    <!-- 认证表单 -->
    <view class="cert-form" v-if="certStatus.status === 'none' || certStatus.status === 'rejected'">
      <!-- 基本信息 -->
      <view class="form-section">
        <view class="section-title">基本信息</view>
        
        <view class="form-item">
          <view class="item-label">
            <text class="required">*</text>
            <text>真实姓名</text>
          </view>
          <input class="item-input" v-model="form.realName" placeholder="请输入真实姓名" placeholder-class="placeholder" />
        </view>

        <view class="form-item">
          <view class="item-label">
            <text class="required">*</text>
            <text>身份证号</text>
          </view>
          <input class="item-input" v-model="form.idCard" placeholder="请输入18位身份证号" placeholder-class="placeholder" maxlength="18" />
        </view>

        <view class="form-item">
          <view class="item-label">
            <text class="required">*</text>
            <text>艺术领域</text>
          </view>
          <picker class="item-picker" mode="selector" :range="artFields" range-key="name" @change="onFieldChange">
            <view class="picker-content">
              <text :class="{ placeholder: !form.artFieldName }">{{ form.artFieldName || '请选择擅长领域' }}</text>
              
            </view>
          </picker>
        </view>

        <view class="form-item">
          <view class="item-label">
            <text class="required">*</text>
            <text>个人简介</text>
          </view>
          <textarea class="item-textarea" v-model="form.resume" placeholder="请简单介绍一下您的艺术背景和创作风格..." placeholder-class="placeholder" maxlength="200" show-word-limit></textarea>
        </view>
      </view>

      <!-- 证件上传 -->
      <view class="form-section">
        <view class="section-title">证件信息</view>
        
        <view class="upload-group">
          <view class="upload-title">
            <text class="required">*</text>
            <text>身份证照片</text>
          </view>
          <view class="upload-tips">请上传清晰、完整的身份证正反面照片</view>
          <view class="upload-grid">
            <view class="upload-item" @click="chooseImage('idCardFront')">
              <image v-if="form.idCardFront" :src="form.idCardFront" mode="aspectFill" class="upload-image"></image>
              <view v-else class="upload-placeholder">
                
                <text class="upload-text">身份证正面</text>
              </view>
              <view class="upload-label">正面</view>
            </view>
            <view class="upload-item" @click="chooseImage('idCardBack')">
              <image v-if="form.idCardBack" :src="form.idCardBack" mode="aspectFill" class="upload-image"></image>
              <view v-else class="upload-placeholder">
                
                <text class="upload-text">身份证背面</text>
              </view>
              <view class="upload-label">背面</view>
            </view>
          </view>
        </view>
      </view>

      <!-- 作品展示 -->
      <view class="form-section">
        <view class="section-title">作品展示<text class="section-hint">（可选）</text></view>
        <view class="upload-tips">上传3-5张代表作品，有助于审核通过</view>
        
        <view class="works-uploader">
          <view class="works-list">
            <view class="work-item" v-for="(item, index) in form.artworks" :key="index">
              <image :src="item" mode="aspectFill"></image>
              <view class="work-delete" @click="removeWork(index)">
                
              </view>
            </view>
            <view class="work-add" @click="chooseImage('artworks')" v-if="form.artworks.length < 5">
              
              <text>添加作品</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 提交按钮 -->
      <view class="form-footer">
        <button class="submit-btn" :class="{ disabled: !canSubmit }" @click="submitForm">提交认证申请</button>
        <view class="submit-tips">
          <text>点击提交即表示同意</text>
          <text class="link" @click="showAgreement">《艺术家认证协议》</text>
        </view>
      </view>
    </view>

    <!-- 重新申请按钮 -->
    <view class="form-footer" v-if="certStatus.status === 'rejected'">
      <button class="submit-btn" @click="reApply">重新申请认证</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { becomeArtist } from '@/api/user.js'

const artFields = [
  { id: 1, name: '油画' },
  { id: 2, name: '国画/书法' },
  { id: 3, name: '版画' },
  { id: 4, name: '雕塑' },
  { id: 5, name: '水彩/水墨' },
  { id: 6, name: '插画/动漫' },
  { id: 7, name: '摄影' },
  { id: 8, name: '装置艺术' },
  { id: 9, name: '综合材料' },
  { id: 10, name: '其他' }
]

const form = ref({
  realName: '',
  idCard: '',
  artField: '',
  artFieldName: '',
  resume: '',  // 后端字段名统一为resume
  idCardFront: '',
  idCardBack: '',
  artworks: []  // 后端字段名统一为artworks
})

const certStatus = ref({
  status: 'none',
  text: '',
  icon: '',
  color: ''
})

const canSubmit = computed(() => {
  return form.value.realName && 
         form.value.idCard.length === 18 && 
         form.value.artField && 
         form.value.resume &&  // 改为resume
         form.value.idCardFront && 
         form.value.idCardBack
})

const onFieldChange = (e) => {
  const index = e.detail.value
  form.value.artField = artFields[index].id
  form.value.artFieldName = artFields[index].name
}

const chooseImage = (type) => {
  uni.chooseImage({
    count: type === 'artworks' ? 5 - form.value.artworks.length : 1,
    sizeType: ['compressed'],
    success: (res) => {
      if (type === 'idCardFront') {
        form.value.idCardFront = res.tempFilePaths[0]
      } else if (type === 'idCardBack') {
        form.value.idCardBack = res.tempFilePaths[0]
      } else if (type === 'artworks') {
        form.value.artworks.push(...res.tempFilePaths)  // 改为artworks
      }
    }
  })
}

const removeWork = (index) => {
  form.value.artworks.splice(index, 1)  // 改为artworks
}

const showAgreement = () => {
  uni.navigateTo({ url: '/pages/user/agreement?type=artist_cert' })
}

const submitForm = async () => {
  if (!canSubmit.value) {
    uni.showToast({ title: '请完善必填信息', icon: 'none' })
    return
  }

  uni.showLoading({ title: '提交中...' })
  
  try {
    // 模拟上传和提交
    await new Promise(resolve => setTimeout(resolve, 1500))
    
    // 这里应该是实际的上传文件和提交逻辑
    // const res = await becomeArtist(form.value)
    
    uni.hideLoading()
    uni.showToast({ title: '提交成功，请等待审核', icon: 'success' })
    
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: '提交失败，请重试', icon: 'none' })
  }
}

const reApply = () => {
  certStatus.value.status = 'none'
}

onMounted(() => {
  // 模拟获取认证状态
  // 实际应该从API获取
})
</script>

<style lang="scss" scoped>
.cert-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: env(safe-area-inset-bottom);
}

.cert-header {
  display: flex;
  align-items: flex-start;
  padding: 30rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;

  .header-icon {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    background: rgba(255,255,255,0.2);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 20rpx;
  }

  .header-text {
    flex: 1;

    .title {
      font-size: 32rpx;
      font-weight: 600;
      display: block;
      margin-bottom: 12rpx;
    }

    .desc {
      font-size: 24rpx;
      opacity: 0.9;
      line-height: 1.5;
    }
  }
}

.cert-status {
  margin: 20rpx;
  padding: 30rpx;
  background: #fff;
  border-radius: 16rpx;
  text-align: center;

  .status-badge {
    display: inline-flex;
    align-items: center;
    gap: 12rpx;
    padding: 16rpx 32rpx;
    border-radius: 40rpx;
    font-size: 28rpx;
    font-weight: 500;

    &.pending {
      background: #fff3e0;
      color: #ff9800;
    }

    &.rejected {
      background: #ffebee;
      color: #f44336;
    }

    &.approved {
      background: #e8f5e9;
      color: #4caf50;
    }
  }

  .status-detail {
    margin-top: 20rpx;
    font-size: 24rpx;
    color: #666;
  }
}

.cert-form {
  padding: 0 20rpx 120rpx;
}

.form-section {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;

  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 30rpx;
    padding-left: 20rpx;
    border-left: 6rpx solid #667eea;
  }

  .section-hint {
    font-size: 24rpx;
    color: #999;
    font-weight: normal;
    margin-left: 12rpx;
  }
}

.form-item {
  margin-bottom: 30rpx;

  .item-label {
    font-size: 28rpx;
    color: #333;
    margin-bottom: 16rpx;

    .required {
      color: #ff4d4f;
      margin-right: 4rpx;
    }
  }

  .item-input {
    height: 88rpx;
    padding: 0 24rpx;
    background: #f8f9fa;
    border-radius: 12rpx;
    font-size: 28rpx;

    &::placeholder {
      color: #bbb;
    }
  }

  .item-picker {
    height: 88rpx;
    padding: 0 24rpx;
    background: #f8f9fa;
    border-radius: 12rpx;
    display: flex;
    align-items: center;

    .picker-content {
      flex: 1;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 28rpx;
      color: #333;
    }
  }

  .item-textarea {
    width: 100%;
    height: 160rpx;
    padding: 24rpx;
    background: #f8f9fa;
    border-radius: 12rpx;
    font-size: 28rpx;
    box-sizing: border-box;
  }
}

.placeholder {
  color: #bbb;
}

.upload-group {
  .upload-title {
    font-size: 28rpx;
    color: #333;
    margin-bottom: 12rpx;

    .required {
      color: #ff4d4f;
      margin-right: 4rpx;
    }
  }

  .upload-tips {
    font-size: 24rpx;
    color: #999;
    margin-bottom: 20rpx;
  }
}

.upload-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;

  .upload-item {
    position: relative;
    aspect-ratio: 3/2;
    border-radius: 12rpx;
    overflow: hidden;
    background: #f8f9fa;

    .upload-image {
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
      gap: 12rpx;

      .upload-text {
        font-size: 24rpx;
        color: #999;
      }
    }

    .upload-label {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 56rpx;
      background: linear-gradient(transparent, rgba(0,0,0,0.6));
      color: #fff;
      font-size: 24rpx;
      display: flex;
      align-items: flex-end;
      justify-content: center;
      padding-bottom: 8rpx;
    }
  }
}

.works-uploader {
  .works-list {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16rpx;

    .work-item {
      position: relative;
      aspect-ratio: 1;
      border-radius: 12rpx;
      overflow: hidden;

      image {
        width: 100%;
        height: 100%;
      }

      .work-delete {
        position: absolute;
        top: 8rpx;
        right: 8rpx;
        width: 40rpx;
        height: 40rpx;
        background: rgba(0,0,0,0.5);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }

    .work-add {
      aspect-ratio: 1;
      background: #f8f9fa;
      border: 2rpx dashed #ddd;
      border-radius: 12rpx;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8rpx;

      text {
        font-size: 24rpx;
        color: #999;
      }
    }
  }
}

.form-footer {
  padding: 30rpx 20rpx;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  box-shadow: 0 -2rpx 20rpx rgba(0,0,0,0.05);

  .submit-btn {
    width: 100%;
    height: 96rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    font-size: 32rpx;
    font-weight: 600;
    border-radius: 48rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;

    &.disabled {
      background: #ccc;
    }

    &::after {
      border: none;
    }
  }

  .submit-tips {
    text-align: center;
    font-size: 24rpx;
    color: #999;
    margin-top: 20rpx;

    .link {
      color: #667eea;
    }
  }
}
</style>