<template>
  <view class="realname-page">
    <view class="hero-card">
      <view class="hero-icon">{{ statusMeta.icon }}</view>
      <view class="hero-copy">
        <text class="hero-title">实名认证</text>
        <text class="hero-desc">{{ statusMeta.desc }}</text>
      </view>
      <text class="status-pill" :class="statusMeta.className">{{ statusMeta.text }}</text>
    </view>

    <view class="progress-card">
      <view class="progress-item" :class="{ active: true, done: form.realName && validIdCard }">
        <text class="progress-dot">1</text>
        <text class="progress-text">身份信息</text>
      </view>
      <view class="progress-line"></view>
      <view class="progress-item" :class="{ active: form.realName && validIdCard, done: hasIdImages }">
        <text class="progress-dot">2</text>
        <text class="progress-text">证件上传</text>
      </view>
      <view class="progress-line"></view>
      <view class="progress-item" :class="{ active: hasIdImages, done: form.faceVerified }">
        <text class="progress-dot">3</text>
        <text class="progress-text">人脸核验</text>
      </view>
    </view>

    <view class="form-section">
      <view class="section-title">身份信息</view>
      <view class="form-item">
        <text class="label">真实姓名</text>
        <input
          class="input"
          v-model="form.realName"
          placeholder="请输入与证件一致的姓名"
          placeholder-class="placeholder"
          :disabled="isReadonly"
        />
      </view>
      <view class="form-item">
        <text class="label">身份证号</text>
        <input
          class="input"
          v-model="form.idCard"
          maxlength="18"
          placeholder="请输入18位身份证号"
          placeholder-class="placeholder"
          :disabled="isReadonly"
          @input="onIdCardInput"
          @blur="validateIdCardField"
        />
        <text v-if="idCardError" class="error-text">{{ idCardError }}</text>
      </view>
    </view>

    <view class="form-section">
      <view class="section-title">证件照片</view>
      <text class="section-desc">请上传清晰、完整、无反光的身份证正反面照片。</text>
      <view class="upload-grid">
        <view class="upload-card" @click="chooseImage('front')">
          <image v-if="form.idFront" class="upload-image" :src="form.idFront" mode="aspectFill" />
          <view v-else class="upload-placeholder">
            <text class="upload-icon">+</text>
            <text class="upload-title">身份证正面</text>
            <text class="upload-subtitle">国徽面</text>
          </view>
        </view>
        <view class="upload-card" @click="chooseImage('back')">
          <image v-if="form.idBack" class="upload-image" :src="form.idBack" mode="aspectFill" />
          <view v-else class="upload-placeholder">
            <text class="upload-icon">+</text>
            <text class="upload-title">身份证背面</text>
            <text class="upload-subtitle">人像面</text>
          </view>
        </view>
      </view>
    </view>

    <view class="form-section">
      <view class="section-title">人脸识别</view>
      <view class="face-card" :class="{ verified: form.faceVerified }" @click="startFaceVerify">
        <view class="face-icon">{{ form.faceVerified ? '✓' : '脸' }}</view>
        <view class="face-copy">
          <text class="face-title">{{ form.faceVerified ? '已完成真人核验' : '开始人脸识别认证' }}</text>
          <text class="face-desc">{{ form.faceVerified ? '核验结果将与实名资料一并提交审核。' : '请由证件本人操作，确保光线充足并正对屏幕。' }}</text>
        </view>
        <text class="face-action">{{ form.faceVerified ? '已认证' : '去认证' }}</text>
      </view>
    </view>

    <view class="notice-card">
      <view class="notice-title">认证说明</view>
      <text class="notice-line">仅用于平台账户实名校验、提现和发票等合规场景。</text>
      <text class="notice-line">身份证号本地仅展示脱敏结果；正式环境应接入微信或第三方实名核验接口。</text>
    </view>

    <view class="bottom-bar">
      <button class="submit-btn" :class="{ disabled: !canSubmit || isReadonly }" :disabled="!canSubmit || isReadonly" @click="submitForm">
        {{ submitText }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'

const STORAGE_KEY = 'realname_certification'

const form = ref({
  realName: '',
  idCard: '',
  idFront: '',
  idBack: '',
  faceVerified: false,
  idCardValid: false,
  status: 0,
  submittedAt: ''
})

const idCardError = ref('')

const isReadonly = computed(() => form.value.status === 1 || form.value.status === 2)
const validIdCard = computed(() => form.value.idCardValid || validateIdCard(form.value.idCard))
const hasIdImages = computed(() => Boolean(form.value.idFront && form.value.idBack))
const canSubmit = computed(() => {
  return Boolean(form.value.realName.trim() && validIdCard.value && hasIdImages.value && form.value.faceVerified)
})

const statusMeta = computed(() => {
  if (form.value.status === 1) {
    return { text: '已认证', desc: '您的实名认证已完成，可用于提现、发票和身份校验。', icon: '✓', className: 'success' }
  }
  if (form.value.status === 2) {
    return { text: '审核中', desc: '资料已提交，平台将在 1-3 个工作日内完成审核。', icon: '审', className: 'pending' }
  }
  return { text: '未认证', desc: '完成身份信息、证件上传和人脸识别后提交审核。', icon: '认', className: 'idle' }
})

const submitText = computed(() => {
  if (form.value.status === 1) return '已完成认证'
  if (form.value.status === 2) return '认证审核中'
  return '提交实名认证'
})

const validateIdCard = (value) => {
  const id = String(value || '').trim().toUpperCase()
  if (!/^\d{17}[\dX]$/.test(id)) return false

  const birth = id.slice(6, 14)
  const year = Number(birth.slice(0, 4))
  const month = Number(birth.slice(4, 6))
  const day = Number(birth.slice(6, 8))
  const date = new Date(year, month - 1, day)
  if (date.getFullYear() !== year || date.getMonth() + 1 !== month || date.getDate() !== day) return false

  const weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
  const checks = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']
  const sum = id.slice(0, 17).split('').reduce((total, num, index) => total + Number(num) * weights[index], 0)
  return checks[sum % 11] === id[17]
}

const maskIdCard = (value) => {
  const id = String(value || '').trim().toUpperCase()
  if (id.length < 18) return id
  return `${id.slice(0, 6)}********${id.slice(-4)}`
}

const onIdCardInput = (e) => {
  form.value.idCard = String(e.detail.value || '').toUpperCase()
  form.value.idCardValid = false
  if (idCardError.value && validateIdCard(form.value.idCard)) idCardError.value = ''
}

const validateIdCardField = () => {
  if (!form.value.idCard) {
    idCardError.value = ''
    return true
  }
  const valid = validateIdCard(form.value.idCard)
  idCardError.value = valid ? '' : '身份证号格式不正确，请检查号码、出生日期和校验位'
  return valid
}

const chooseImage = (type) => {
  if (isReadonly.value) return
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: (res) => {
      const path = res.tempFilePaths?.[0]
      if (!path) return
      if (type === 'front') form.value.idFront = path
      if (type === 'back') form.value.idBack = path
    }
  })
}

const startFaceVerify = () => {
  if (isReadonly.value || form.value.faceVerified) return
  if (!form.value.realName.trim() || !validateIdCardField()) {
    uni.showToast({ title: '请先填写正确的身份信息', icon: 'none' })
    return
  }
  uni.showModal({
    title: '人脸识别认证',
    content: '请确认由本人操作。当前为本地调试流程，确认后将标记为已完成。',
    confirmText: '开始认证',
    success: (res) => {
      if (!res.confirm) return
      uni.showLoading({ title: '认证中...' })
      setTimeout(() => {
        uni.hideLoading()
        form.value.faceVerified = true
        uni.showToast({ title: '人脸认证完成', icon: 'success' })
      }, 700)
    }
  })
}

const saveLocalStatus = () => {
  const payload = {
    ...form.value,
    idCardValid: true,
    idCardMasked: maskIdCard(form.value.idCard),
    idCard: '',
    updatedAt: new Date().toISOString()
  }
  uni.setStorageSync(STORAGE_KEY, payload)
}

const submitForm = () => {
  if (!validateIdCardField()) {
    uni.showToast({ title: '身份证号格式不正确', icon: 'none' })
    return
  }
  if (!hasIdImages.value) {
    uni.showToast({ title: '请上传身份证正反面', icon: 'none' })
    return
  }
  if (!form.value.faceVerified) {
    uni.showToast({ title: '请先完成人脸识别认证', icon: 'none' })
    return
  }

  uni.showLoading({ title: '提交中...' })
  setTimeout(() => {
    uni.hideLoading()
    form.value.status = 2
    form.value.submittedAt = new Date().toISOString()
    saveLocalStatus()
    uni.showToast({ title: '提交成功，等待审核', icon: 'success' })
  }, 600)
}

onMounted(() => {
  const saved = uni.getStorageSync(STORAGE_KEY)
  if (!saved) return
  form.value = {
    ...form.value,
    ...saved,
    idCard: saved.idCard || saved.idCardMasked || ''
  }
})
</script>

<style lang="scss" scoped>
.realname-page {
  min-height: 100vh;
  padding: 24rpx 24rpx 160rpx;
  box-sizing: border-box;
  background: #0b0b0c;
  color: #f6f2e8;
}

.hero-card,
.progress-card,
.form-section,
.notice-card {
  background: #171719;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 20rpx;
  box-shadow: 0 18rpx 42rpx rgba(0, 0, 0, 0.22);
}

.hero-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 28rpx;
  background: linear-gradient(135deg, rgba(201, 162, 39, 0.18), rgba(23, 23, 25, 0.98));
}

.hero-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: rgba(201, 162, 39, 0.18);
  color: #f2c85b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  font-weight: 800;
  flex-shrink: 0;
}

.hero-copy {
  flex: 1;
  min-width: 0;
}

.hero-title {
  display: block;
  font-size: 38rpx;
  line-height: 48rpx;
  font-weight: 800;
}

.hero-desc {
  display: block;
  margin-top: 10rpx;
  color: #b7b0a4;
  font-size: 25rpx;
  line-height: 36rpx;
}

.status-pill {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  font-weight: 800;
  flex-shrink: 0;
}

.status-pill.idle {
  background: rgba(255, 255, 255, 0.08);
  color: #b7b0a4;
}

.status-pill.pending {
  background: rgba(201, 162, 39, 0.18);
  color: #f2c85b;
}

.status-pill.success {
  background: rgba(82, 196, 26, 0.16);
  color: #69d37b;
}

.progress-card {
  margin-top: 24rpx;
  padding: 24rpx;
  display: flex;
  align-items: center;
}

.progress-item {
  width: 150rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  color: #68645c;
}

.progress-dot {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: #202024;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 800;
}

.progress-text {
  font-size: 23rpx;
}

.progress-item.active {
  color: #f2c85b;
}

.progress-item.done .progress-dot {
  background: #c9a227;
  color: #16130b;
}

.progress-line {
  flex: 1;
  height: 2rpx;
  background: rgba(255, 255, 255, 0.08);
  margin: 0 -18rpx 36rpx;
}

.form-section,
.notice-card {
  margin-top: 24rpx;
  padding: 28rpx;
}

.section-title {
  padding-left: 18rpx;
  border-left: 6rpx solid #c9a227;
  font-size: 32rpx;
  line-height: 40rpx;
  font-weight: 800;
}

.section-desc {
  display: block;
  margin-top: 18rpx;
  color: #8f8a80;
  font-size: 25rpx;
  line-height: 36rpx;
}

.form-item {
  margin-top: 28rpx;
}

.label {
  display: block;
  margin-bottom: 14rpx;
  font-size: 27rpx;
  color: #f6f2e8;
  font-weight: 700;
}

.input {
  height: 92rpx;
  padding: 0 24rpx;
  border-radius: 14rpx;
  background: #202024;
  color: #f6f2e8;
  font-size: 28rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  box-sizing: border-box;
}

.placeholder {
  color: #68645c;
}

.error-text {
  display: block;
  margin-top: 12rpx;
  color: #ff6b6b;
  font-size: 24rpx;
  line-height: 32rpx;
}

.upload-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
  margin-top: 22rpx;
}

.upload-card {
  height: 210rpx;
  border-radius: 16rpx;
  background: #202024;
  border: 1rpx dashed rgba(201, 162, 39, 0.44);
  overflow: hidden;
}

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
  gap: 10rpx;
}

.upload-icon {
  color: #f2c85b;
  font-size: 42rpx;
  line-height: 42rpx;
}

.upload-title {
  color: #f6f2e8;
  font-size: 26rpx;
  font-weight: 700;
}

.upload-subtitle {
  color: #8f8a80;
  font-size: 23rpx;
}

.face-card {
  margin-top: 22rpx;
  padding: 24rpx;
  border-radius: 18rpx;
  background: linear-gradient(135deg, rgba(201, 162, 39, 0.12), rgba(32, 32, 36, 0.98));
  border: 1rpx solid rgba(201, 162, 39, 0.34);
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.face-card.verified {
  border-color: rgba(82, 196, 26, 0.48);
  background: linear-gradient(135deg, rgba(82, 196, 26, 0.12), rgba(32, 32, 36, 0.98));
}

.face-icon {
  width: 74rpx;
  height: 74rpx;
  border-radius: 50%;
  background: rgba(201, 162, 39, 0.2);
  color: #f2c85b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 800;
  flex-shrink: 0;
}

.face-copy {
  flex: 1;
  min-width: 0;
}

.face-title {
  display: block;
  color: #f6f2e8;
  font-size: 28rpx;
  font-weight: 800;
}

.face-desc {
  display: block;
  margin-top: 8rpx;
  color: #9b958a;
  font-size: 24rpx;
  line-height: 34rpx;
}

.face-action {
  color: #f2c85b;
  font-size: 25rpx;
  font-weight: 800;
  flex-shrink: 0;
}

.notice-title {
  color: #f2c85b;
  font-size: 28rpx;
  font-weight: 800;
  margin-bottom: 14rpx;
}

.notice-line {
  display: block;
  color: #9b958a;
  font-size: 24rpx;
  line-height: 36rpx;
  margin-top: 8rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(11, 11, 12, 0.94);
  border-top: 1rpx solid rgba(255, 255, 255, 0.08);
  z-index: 10;
}

.submit-btn {
  height: 88rpx;
  border-radius: 999rpx;
  background: #c9a227;
  color: #16130b;
  font-size: 30rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.submit-btn.disabled {
  background: #3a3528;
  color: #7c7464;
}
</style>
