<template>
  <view class="apply-page">
    <!-- 申请须知 -->
    <view class="notice-section card">
      <view class="notice-header">
        
        <text>申请须知</text>
      </view>
      <view class="notice-content">
        <view class="notice-item">1. 申请人须为原创艺术作品的作者</view>
        <view class="notice-item">2. 需提供真实有效的个人身份信息</view>
        <view class="notice-item">3. 上传的作品需为本人原创，禁止抄袭或盗用</view>
        <view class="notice-item">4. 审核结果将在3-5个工作日内通知</view>
        <view class="notice-item">5. 认证成功后可享受艺术家专属权益</view>
      </view>
    </view>

    <!-- 申请表单 -->
    <view class="form-section card">
      <view class="form-title">基本信息</view>

      <!-- 头像上传 -->
      <view class="form-item">
        <view class="form-label">头像</view>
        <view class="avatar-upload" @click="chooseAvatar">
          <image v-if="form.avatar" :src="form.avatar" mode="aspectFill"></image>
          <view v-else class="upload-placeholder">
            
            <text>上传头像</text>
          </view>
        </view>
      </view>

      <!-- 姓名 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>姓名</view>
        <input
          class="form-input"
          v-model="form.name"
          placeholder="请输入真实姓名"
          maxlength="20"
        />
      </view>

      <!-- 艺名 -->
      <view class="form-item">
        <view class="form-label">艺名</view>
        <input
          class="form-input"
          v-model="form.artistName"
          placeholder="选填，用于展示在作品上"
          maxlength="20"
        />
      </view>

      <!-- 性别 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>性别</view>
        <view class="radio-group">
          <view
            class="radio-item"
            :class="{ active: form.gender === 1 }"
            @click="form.gender = 1"
          >
            <text>男</text>
          </view>
          <view
            class="radio-item"
            :class="{ active: form.gender === 2 }"
            @click="form.gender = 2"
          >
            <text>女</text>
          </view>
        </view>
      </view>

      <!-- 出生年份 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>出生年份</view>
        <picker mode="date" :value="form.birthYear" fields="year" @change="onBirthYearChange">
          <view class="picker-value">
            {{ form.birthYear || '请选择' }}
            
          </view>
        </picker>
      </view>

      <!-- 所在城市 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>所在城市</view>
        <picker mode="region" @change="onCityChange">
          <view class="picker-value">
            {{ form.city || '请选择' }}
            
          </view>
        </picker>
      </view>
    </view>

    <!-- 专业信息 -->
    <view class="form-section card">
      <view class="form-title">专业信息</view>

      <!-- 艺术门类 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>艺术门类</view>
        <picker :value="form.artCategoryIndex" :range="artCategories" range-key="name" @change="onArtCategoryChange">
          <view class="picker-value">
            {{ form.artCategory || '请选择' }}
            
          </view>
        </picker>
      </view>

      <!-- 创作风格 -->
      <view class="form-item">
        <view class="form-label">创作风格</view>
        <input
          class="form-input"
          v-model="form.style"
          placeholder="如：抽象派、现实主义等"
          maxlength="50"
        />
      </view>

      <!-- 个人简介 -->
      <view class="form-item vertical">
        <view class="form-label"><text class="required">*</text>个人简介</view>
        <textarea
          class="form-textarea"
          v-model="form.introduction"
          placeholder="请介绍一下您的艺术经历、代表作品等（50-500字）"
          maxlength="500"
        ></textarea>
        <view class="word-count">{{ form.introduction.length }}/500</view>
      </view>

      <!-- 代表作品 -->
      <view class="form-item vertical">
        <view class="form-label"><text class="required">*</text>代表作品</view>
        <view class="works-upload">
          <view class="work-item" v-for="(work, index) in form.works" :key="index">
            <image :src="work" mode="aspectFill"></image>
            <view class="work-delete" @click="deleteWork(index)">
              
            </view>
          </view>
          <view class="work-add" v-if="form.works.length < 9" @click="chooseWork">
            
            <text>上传作品</text>
          </view>
        </view>
        <view class="upload-tip">上传1-9张代表作品图片</view>
      </view>
    </view>

    <!-- 联系方式 -->
    <view class="form-section card">
      <view class="form-title">联系方式</view>

      <!-- 手机号 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>手机号</view>
        <input
          class="form-input"
          v-model="form.phone"
          type="number"
          placeholder="请输入手机号"
          maxlength="11"
        />
      </view>

      <!-- 验证码 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>验证码</view>
        <view class="code-input">
          <input
            class="form-input"
            v-model="form.code"
            type="number"
            placeholder="请输入验证码"
            maxlength="6"
          />
          <view
            class="code-btn"
            :class="{ disabled: countdown > 0 }"
            @click="sendCode"
          >
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </view>
        </view>
      </view>

      <!-- 邮箱 -->
      <view class="form-item">
        <view class="form-label">邮箱</view>
        <input
          class="form-input"
          v-model="form.email"
          type="text"
          placeholder="选填，用于接收审核通知"
        />
      </view>

      <!-- 社交媒体 -->
      <view class="form-item">
        <view class="form-label">社交媒体</view>
        <input
          class="form-input"
          v-model="form.socialMedia"
          placeholder="选填，如微博/小红书账号"
        />
      </view>
    </view>

    <!-- 身份证明 -->
    <view class="form-section card">
      <view class="form-title">身份证明</view>

      <!-- 身份证正面 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>身份证正面</view>
        <view class="idcard-upload" @click="chooseIdCard('front')">
          <image v-if="form.idCardFront" :src="form.idCardFront" mode="aspectFill"></image>
          <view v-else class="upload-placeholder">
            
            <text>点击上传</text>
          </view>
        </view>
      </view>

      <!-- 身份证反面 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>身份证反面</view>
        <view class="idcard-upload" @click="chooseIdCard('back')">
          <image v-if="form.idCardBack" :src="form.idCardBack" mode="aspectFill"></image>
          <view v-else class="upload-placeholder">
            
            <text>点击上传</text>
          </view>
        </view>
      </view>

      <view class="idcard-tip">请确保身份证信息清晰可辨，信息仅用于身份验证</view>
    </view>

    <!-- 协议确认 -->
    <view class="agreement-section">
      <view class="agreement-check" @click="agreeAgreement">
        
        <text>我已阅读并同意</text>
      </view>
      <text class="agreement-link" @click="showAgreement">《艺术家入驻协议》</text>
      <text class="agreement-link" @click="showPrivacy">《隐私政策》</text>
    </view>

    <!-- 提交按钮 -->
    <view class="submit-section">
      <view
        class="submit-btn"
        :class="{ disabled: !canSubmit }"
        @click="submitApplication"
      >
        提交申请
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      form: {
        avatar: '',
        name: '',
        artistName: '',
        gender: 1,
        birthYear: '',
        city: '',
        artCategory: '',
        artCategoryIndex: 0,
        style: '',
        introduction: '',
        works: [],
        phone: '',
        code: '',
        email: '',
        socialMedia: '',
        idCardFront: '',
        idCardBack: ''
      },
      artCategories: [
        { id: 1, name: '国画' },
        { id: 2, name: '油画' },
        { id: 3, name: '版画' },
        { id: 4, name: '雕塑' },
        { id: 5, name: '书法' },
        { id: 6, name: '摄影' },
        { id: 7, name: '水彩' },
        { id: 8, name: '插画' },
        { id: 9, name: '数字艺术' },
        { id: 10, name: '其他' }
      ],
      countdown: 0,
      agreed: false
    }
  },

  computed: {
    canSubmit() {
      return (
        this.agreed &&
        this.form.name &&
        this.form.birthYear &&
        this.form.city &&
        this.form.artCategory &&
        this.form.introduction &&
        this.form.works.length > 0 &&
        this.form.phone &&
        this.form.code &&
        this.form.idCardFront &&
        this.form.idCardBack
      )
    }
  },

  methods: {
    chooseAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          this.form.avatar = res.tempFilePaths[0]
        }
      })
    },

    onBirthYearChange(e) {
      this.form.birthYear = e.detail.value
    },

    onCityChange(e) {
      this.form.city = e.detail.value.join('')
    },

    onArtCategoryChange(e) {
      this.form.artCategoryIndex = e.detail.value
      this.form.artCategory = this.artCategories[e.detail.value].name
    },

    chooseWork() {
      const remain = 9 - this.form.works.length
      uni.chooseImage({
        count: remain,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          this.form.works = [...this.form.works, ...res.tempFilePaths]
        }
      })
    },

    deleteWork(index) {
      this.form.works.splice(index, 1)
    },

    chooseIdCard(type) {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          if (type === 'front') {
            this.form.idCardFront = res.tempFilePaths[0]
          } else {
            this.form.idCardBack = res.tempFilePaths[0]
          }
        }
      })
    },

    sendCode() {
      if (this.countdown > 0) return
      if (!this.form.phone || this.form.phone.length !== 11) {
        uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
        return
      }
      this.countdown = 60
      const timer = setInterval(() => {
        this.countdown--
        if (this.countdown <= 0) {
          clearInterval(timer)
        }
      }, 1000)
      uni.showToast({ title: '验证码已发送', icon: 'success' })
    },

    agreeAgreement() {
      this.agreed = !this.agreed
    },

    showAgreement() {
      uni.navigateTo({ url: '/pages/user/agreement?type=artist' })
    },

    showPrivacy() {
      uni.navigateTo({ url: '/pages/user/agreement?type=privacy' })
    },

    submitApplication() {
      if (!this.canSubmit) {
        uni.showToast({ title: '请填写完整信息', icon: 'none' })
        return
      }

      uni.showModal({
        title: '确认提交',
        content: '确认提交艺术家认证申请？',
        success: (res) => {
          if (res.confirm) {
            uni.showLoading({ title: '提交中...' })
            setTimeout(() => {
              uni.hideLoading()
              uni.showToast({ title: '提交成功', icon: 'success' })
              setTimeout(() => {
                uni.redirectTo({
                  url: '/pages/common/coming-soon?title=申请状态&desc=申请状态页正在开发中，当前可稍后在艺术家中心查看审核结果。'
                })
              }, 1000)
            }, 1500)
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.apply-page {
  min-height: 100vh;
  background: #f5f6f7;
  padding: 20rpx;
  padding-bottom: 140rpx;
}

.notice-section {
  padding: 30rpx;

  .notice-header {
    display: flex;
    align-items: center;
    margin-bottom: 20rpx;

    text {
      margin-left: 12rpx;
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
    }
  }

  .notice-content {
    .notice-item {
      font-size: 26rpx;
      color: #666;
      line-height: 1.8;
      margin-bottom: 12rpx;
    }
  }
}

.form-section {
  padding: 30rpx;
  margin-bottom: 20rpx;

  .form-title {
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 30rpx;
    padding-bottom: 20rpx;
    border-bottom: 1rpx solid #f0f0f0;
  }

  .form-item {
    display: flex;
    align-items: center;
    margin-bottom: 30rpx;

    &.vertical {
      flex-direction: column;
      align-items: flex-start;
    }

    .form-label {
      width: 160rpx;
      font-size: 28rpx;
      color: #333;
      flex-shrink: 0;

      .required {
        color: #ff4d4f;
        margin-right: 4rpx;
      }
    }

    .form-input {
      flex: 1;
      height: 80rpx;
      padding: 0 20rpx;
      font-size: 28rpx;
      color: #333;
      background: #f5f5f5;
      border-radius: 8rpx;
    }

    .radio-group {
      display: flex;
      gap: 40rpx;

      .radio-item {
        padding: 16rpx 40rpx;
        font-size: 28rpx;
        color: #666;
        background: #f5f5f5;
        border-radius: 8rpx;
        border: 2rpx solid transparent;

        &.active {
          color: #667eea;
          background: rgba(102, 126, 234, 0.1);
          border-color: #667eea;
        }
      }
    }

    .picker-value {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 80rpx;
      padding: 0 20rpx;
      font-size: 28rpx;
      color: #333;
      background: #f5f5f5;
      border-radius: 8rpx;
    }

    .form-textarea {
      width: 100%;
      height: 200rpx;
      padding: 20rpx;
      font-size: 28rpx;
      color: #333;
      background: #f5f5f5;
      border-radius: 8rpx;
      box-sizing: border-box;
    }

    .word-count {
      align-self: flex-end;
      font-size: 24rpx;
      color: #999;
      margin-top: 8rpx;
    }
  }

  .avatar-upload {
    width: 160rpx;
    height: 160rpx;
    border-radius: 50%;
    overflow: hidden;
    background: #f5f5f5;

    .upload-placeholder {
      width: 100%;
      height: 100%;
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
  }

  .works-upload {
    width: 100%;
    display: flex;
    flex-wrap: wrap;
    gap: 20rpx;

    .work-item {
      width: 200rpx;
      height: 200rpx;
      border-radius: 12rpx;
      overflow: hidden;
      position: relative;

      .work-delete {
        position: absolute;
        top: 8rpx;
        right: 8rpx;
        width: 40rpx;
        height: 40rpx;
        background: rgba(0, 0, 0, 0.5);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }

    .work-add {
      width: 200rpx;
      height: 200rpx;
      background: #f5f5f5;
      border-radius: 12rpx;
      border: 2rpx dashed #ddd;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      text {
        font-size: 24rpx;
        color: #999;
        margin-top: 8rpx;
      }
    }
  }

  .upload-tip {
    font-size: 24rpx;
    color: #999;
    margin-top: 12rpx;
  }

  .idcard-upload {
    width: 320rpx;
    height: 200rpx;
    border-radius: 12rpx;
    overflow: hidden;
    background: #f5f5f5;

    .upload-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      text {
        font-size: 24rpx;
        color: #999;
        margin-top: 8rpx;
      }
    }
  }

  .idcard-tip {
    font-size: 24rpx;
    color: #999;
    margin-top: 16rpx;
  }

  .code-input {
    flex: 1;
    display: flex;
    gap: 16rpx;

    .form-input {
      flex: 1;
    }

    .code-btn {
      padding: 0 24rpx;
      font-size: 26rpx;
      color: #667eea;
      background: rgba(102, 126, 234, 0.1);
      border-radius: 8rpx;
      display: flex;
      align-items: center;

      &.disabled {
        color: #999;
        background: #f5f5f5;
      }
    }
  }
}

.agreement-section {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30rpx;
  flex-wrap: wrap;

  .agreement-check {
    display: flex;
    align-items: center;

    text {
      margin-left: 8rpx;
      font-size: 26rpx;
      color: #666;
    }
  }

  .agreement-link {
    font-size: 26rpx;
    color: #667eea;
    margin-left: 8rpx;
  }
}

.submit-section {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;

  .submit-btn {
    height: 88rpx;
    line-height: 88rpx;
    text-align: center;
    font-size: 32rpx;
    font-weight: 600;
    color: #fff;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 44rpx;

    &.disabled {
      background: #ccc;
    }
  }
}
</style>
