<template>
  <view class="realname-page">
    <view class="hero-card">
      <view class="hero-icon">{{ statusMeta.icon }}</view>
      <view class="hero-copy">
        <text class="hero-title">实名认证</text>
        <text class="hero-desc">{{ statusMeta.desc }}</text>
        <text v-if="rejectReason" class="reject-reason">拒绝原因：{{ rejectReason }}</text>
      </view>
      <text class="status-pill" :class="statusMeta.className">{{ statusMeta.text }}</text>
    </view>

    <view class="progress-card">
      <view class="progress-item" :class="{ active: true, done: form.realName && validIdCard }">
        <text class="progress-dot">1</text>
        <text class="progress-text">身份信息</text>
      </view>
      <view class="progress-line"></view>
      <view class="progress-item" :class="{ active: form.realName && validIdCard, done: useAlipayRealname ? form.faceVerified : hasIdImages }">
        <text class="progress-dot">2</text>
        <text class="progress-text">{{ useAlipayRealname ? '支付宝实名' : '证件上传' }}</text>
      </view>
      <view class="progress-line" v-if="!useAlipayRealname"></view>
      <view v-if="!useAlipayRealname" class="progress-item" :class="{ active: hasIdImages, done: form.faceVerified }">
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

    <view v-if="!useAlipayRealname" class="form-section">
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
          <view v-if="uploading === 'front'" class="upload-mask">
            <text class="upload-mask-text">上传中...</text>
          </view>
        </view>
        <view class="upload-card" @click="chooseImage('back')">
          <image v-if="form.idBack" class="upload-image" :src="form.idBack" mode="aspectFill" />
          <view v-else class="upload-placeholder">
            <text class="upload-icon">+</text>
            <text class="upload-title">身份证背面</text>
            <text class="upload-subtitle">人像面</text>
          </view>
          <view v-if="uploading === 'back'" class="upload-mask">
            <text class="upload-mask-text">上传中...</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="!useAlipayRealname" class="form-section">
      <view class="section-title">人脸识别</view>
      <view class="face-card" :class="{ verified: form.faceVerified }" @click="startFaceVerify">
        <view class="face-icon">{{ form.faceVerified ? '✓' : '脸' }}</view>
        <view class="face-copy">
          <text class="face-title">{{ form.faceVerified ? '已完成真人核验' : '开始人脸识别认证' }}</text>
          <text class="face-desc">{{ form.faceVerified ? '核验结果将与实名资料一并提交。' : '请由证件本人操作，确保光线充足并正对屏幕。' }}</text>
        </view>
        <text class="face-action">{{ form.faceVerified ? '已认证' : '去认证' }}</text>
      </view>
    </view>

    <view v-else class="form-section">
      <view class="section-title">支付宝实名认证</view>
      <text class="section-desc">提交真实姓名和身份证号后，将跳转支付宝完成实名校验。认证通过后会自动回到当前页面。</text>
      <view class="alipay-card">
        <view class="alipay-badge">支</view>
        <view class="alipay-copy">
          <text class="alipay-title">{{ form.status === 1 ? '支付宝实名认证已完成' : '使用支付宝完成实名校验' }}</text>
          <text class="alipay-desc">{{ form.status === 2 ? '认证进行中，完成后回到本页会自动同步结果。' : '不再需要手动上传身份证照片或点击假人脸认证。' }}</text>
        </view>
      </view>
    </view>

    <view class="notice-card">
      <view class="notice-title">认证说明</view>
      <text class="notice-line">仅用于平台账户实名校验、提现和发票等合规场景。</text>
      <text class="notice-line">身份证信息将加密存储，仅脱敏显示。</text>
      <text class="notice-line">{{ useAlipayRealname ? '若已配置支付宝实名能力，认证结果会自动同步；未配置时仍走人工审核。' : '审核通常需 1-3 个工作日，请耐心等待。' }}</text>
    </view>

    <view class="bottom-bar">
      <button class="submit-btn" :class="{ disabled: !canSubmit || isReadonly || submitting }" :disabled="!canSubmit || isReadonly || submitting" @click="submitForm">
        {{ submitting ? '提交中...' : submitText }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { submitRealnameCert, getRealnameCertStatus, startAlipayRealname, syncAlipayRealname } from '@/api/user.js'
import { uploadFile } from '@/api/file.js'

const ALIPAY_REALNAME_DRAFT_KEY = 'alipay_realname_draft'

const form = ref({
  realName: '',
  idCard: '',
  idFront: '',
  idBack: '',
  faceVerified: false,
  idCardValid: false,
  status: 0,
  verifyMode: 'manual',
  certifyId: '',
  rejectReason: '',
  submittedAt: ''
})

const idCardError = ref('')
const uploading = ref(null)
const submitting = ref(false)

const useAlipayRealname = computed(() => form.value.verifyMode === 'alipay')
const isReadonly = computed(() => form.value.status === 1 || (form.value.status === 2 && !useAlipayRealname.value))
const validIdCard = computed(() => form.value.idCardValid || validateIdCard(form.value.idCard))
const hasIdImages = computed(() => Boolean(form.value.idFront && form.value.idBack))
const rejectReason = computed(() => form.value.rejectReason)

const isMaskedValue = (value) => String(value || '').includes('*')

const canSubmit = computed(() => {
  if (form.value.status === 1) return false
  if (useAlipayRealname.value) {
    if (form.value.status === 2 && form.value.certifyId) return !submitting.value
    return Boolean(form.value.realName.trim() && validIdCard.value && !submitting.value)
  }
  return Boolean(form.value.realName.trim() && validIdCard.value && hasIdImages.value && form.value.faceVerified)
})

const statusMeta = computed(() => {
  if (form.value.status === 1) {
    return { text: '已认证', desc: '您的实名认证已完成，可用于提现、发票和身份校验。', icon: '✓', className: 'success' }
  }
  if (form.value.status === 2) {
    return {
      text: useAlipayRealname.value ? '认证中' : '审核中',
      desc: useAlipayRealname.value ? '已发起支付宝实名校验，完成后回到本页将自动同步结果。' : '资料已提交，平台将在 1-3 个工作日内完成审核。',
      icon: '审',
      className: 'pending'
    }
  }
  if (form.value.status === 3) {
    return { text: '已拒绝', desc: '认证未通过，请修改后重新提交。', icon: '✗', className: 'rejected' }
  }
  return {
    text: '未认证',
    desc: useAlipayRealname.value ? '填写真实姓名和身份证号后，将跳转支付宝完成实名校验。' : '完成身份信息、证件上传和人脸识别后提交审核。',
    icon: '认',
    className: 'idle'
  }
})

const submitText = computed(() => {
  if (form.value.status === 1) return '已完成认证'
  if (form.value.status === 2) return useAlipayRealname.value ? '同步认证结果' : '认证审核中'
  if (submitting.value) return '提交中...'
  if (useAlipayRealname.value) return '去支付宝认证'
  if (form.value.status === 3) return '重新提交认证'
  return '提交实名认证'
})

const saveAlipayDraft = () => {
  if (!useAlipayRealname.value) return
  uni.setStorageSync(ALIPAY_REALNAME_DRAFT_KEY, {
    realName: form.value.realName.trim(),
    idCard: form.value.idCard.toUpperCase(),
    certifyId: form.value.certifyId || '',
    updatedAt: Date.now()
  })
}

const restoreAlipayDraft = (fallbackCertifyId = '') => {
  const draft = uni.getStorageSync(ALIPAY_REALNAME_DRAFT_KEY)
  if (!draft || typeof draft !== 'object') return false
  if (draft.certifyId && fallbackCertifyId && draft.certifyId !== fallbackCertifyId) return false
  if (draft.realName) form.value.realName = draft.realName
  if (draft.idCard) {
    form.value.idCard = draft.idCard
    form.value.idCardValid = validateIdCard(draft.idCard)
  }
  return true
}

const clearAlipayDraft = () => {
  uni.removeStorageSync(ALIPAY_REALNAME_DRAFT_KEY)
}

const launchAlipayRealname = async () => {
  saveAlipayDraft()
  const result = await startAlipayRealname({
    realName: form.value.realName.trim(),
    idCard: form.value.idCard.toUpperCase()
  })
  form.value.status = 2
  form.value.certifyId = result?.certifyId || ''
  saveAlipayDraft()
  if (result?.redirectUrl) {
    if (typeof window !== 'undefined') {
      window.location.href = result.redirectUrl
    } else {
      uni.showModal({
        title: '当前端不支持直接跳转',
        content: '请在 H5 页面中完成支付宝实名认证。',
        showCancel: false
      })
    }
  } else {
    uni.showToast({ title: '未获取到支付宝认证地址', icon: 'none' })
  }
}

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
  if (isMaskedValue(form.value.idCard)) {
    idCardError.value = ''
    return true
  }
  const valid = validateIdCard(form.value.idCard)
  idCardError.value = valid ? '' : '身份证号格式不正确，请检查号码、出生日期和校验位'
  return valid
}

const chooseImage = async (type) => {
  if (isReadonly.value) return
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: async (res) => {
      const path = res.tempFilePaths?.[0]
      if (!path) return

      uploading.value = type
      try {
        const url = await uploadFile(path)
        if (type === 'front') form.value.idFront = url
        if (type === 'back') form.value.idBack = url
        uni.showToast({ title: '上传成功', icon: 'success' })
      } catch (err) {
        uni.showToast({ title: err.message || '上传失败', icon: 'none' })
      } finally {
        uploading.value = null
      }
    }
  })
}

const startFaceVerify = () => {
  if (useAlipayRealname.value) {
    submitForm()
    return
  }
  if (isReadonly.value || form.value.faceVerified) return
  if (!form.value.realName.trim() || !validateIdCardField()) {
    uni.showToast({ title: '请先填写正确的身份信息', icon: 'none' })
    return
  }
  uni.showModal({
    title: '人脸识别认证',
    content: '请确认由本人操作。确认后将标记为已完成，并随实名资料一并提交审核。',
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

const submitForm = async () => {
  if (useAlipayRealname.value) {
    submitting.value = true
    try {
      if (form.value.status === 2 && form.value.certifyId) {
        const status = await syncAlipayRealname({ certifyId: form.value.certifyId })
        form.value.status = status?.status ?? form.value.status
        form.value.rejectReason = status?.rejectReason || ''
        if (status?.status === 1) {
          form.value.faceVerified = true
          clearAlipayDraft()
          uni.showToast({ title: '实名认证成功', icon: 'success' })
        } else if (status?.status === 3) {
          uni.showToast({ title: form.value.rejectReason || '实名认证未通过，请重新发起', icon: 'none' })
        } else {
          const restored = restoreAlipayDraft(form.value.certifyId)
          uni.showModal({
            title: '认证未完成',
            content: restored
              ? '本次支付宝认证尚未完成，是否重新发起一次实名认证？'
              : '本次支付宝认证尚未完成。若要重新发起，请重新填写姓名和身份证号。',
            confirmText: restored ? '重新发起' : '知道了',
            success: async (res) => {
              if (!res.confirm || !restored) return
              try {
                submitting.value = true
                await launchAlipayRealname()
              } catch (retryErr) {
                uni.showToast({ title: retryErr.message || '重新发起失败', icon: 'none' })
              } finally {
                submitting.value = false
              }
            }
          })
        }
      } else {
        if (!validateIdCardField()) {
          uni.showToast({ title: '身份证号格式不正确', icon: 'none' })
          return
        }
        await launchAlipayRealname()
      }
    } catch (err) {
      uni.showToast({ title: err.message || '发起认证失败', icon: 'none' })
    } finally {
      submitting.value = false
    }
    return
  }

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

  submitting.value = true
  try {
    await submitRealnameCert({
      realName: form.value.realName.trim(),
      idCard: form.value.idCard.toUpperCase(),
      idFrontUrl: form.value.idFront,
      idBackUrl: form.value.idBack,
      faceVerified: form.value.faceVerified
    })
    form.value.status = 2
    form.value.submittedAt = new Date().toISOString()
    form.value.idCard = maskIdCard(form.value.idCard)
    form.value.idCardValid = true
    uni.showToast({ title: '提交成功，等待审核', icon: 'success' })
  } catch (err) {
    uni.showToast({ title: err.message || '提交失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    let certifyId = ''
    if (typeof window !== 'undefined') {
      const hashQuery = window.location.hash.includes('?') ? window.location.hash.split('?')[1] : ''
      certifyId = new URLSearchParams(window.location.search).get('certifyId')
        || new URLSearchParams(hashQuery).get('certifyId')
        || ''
    }
    if (certifyId) {
      await syncAlipayRealname({ certifyId })
    }
    const data = await getRealnameCertStatus()
    if (data && data.status > 0) {
      form.value.status = data.status
      const restored = data?.verifyMode === 'alipay' && data.status !== 1 && restoreAlipayDraft(data?.certifyId || certifyId)
      if (data.maskedRealName && !restored && !useAlipayRealname.value) {
        form.value.realName = data.maskedRealName
      }
      if (data.maskedIdCard && !restored && !useAlipayRealname.value) {
        form.value.idCard = data.maskedIdCard
        form.value.idCardValid = true
      }
      form.value.rejectReason = data.rejectReason || ''
      form.value.submittedAt = data.submittedAt || ''
    }
    form.value.verifyMode = data?.verifyMode || 'manual'
    form.value.certifyId = data?.certifyId || certifyId || ''
    if (useAlipayRealname.value && form.value.status === 1) {
      form.value.faceVerified = true
      clearAlipayDraft()
    }
  } catch (err) {
    console.warn('获取认证状态失败:', err.message)
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

.reject-reason {
  display: block;
  margin-top: 8rpx;
  color: #ff6b6b;
  font-size: 24rpx;
  line-height: 34rpx;
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

.status-pill.rejected {
  background: rgba(255, 107, 107, 0.16);
  color: #ff6b6b;
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

.alipay-card {
  margin-top: 22rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  background: rgba(22, 119, 255, 0.08);
  border: 1rpx solid rgba(22, 119, 255, 0.22);
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.alipay-badge {
  width: 72rpx;
  height: 72rpx;
  border-radius: 18rpx;
  background: rgba(22, 119, 255, 0.2);
  color: #63a7ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  font-weight: 800;
  flex-shrink: 0;
}

.alipay-copy {
  flex: 1;
}

.alipay-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #f6f2e8;
}

.alipay-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 34rpx;
  color: #9fb8d8;
}

.upload-card {
  height: 210rpx;
  border-radius: 16rpx;
  background: #202024;
  border: 1rpx dashed rgba(201, 162, 39, 0.44);
  overflow: hidden;
  position: relative;
}

.upload-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-mask-text {
  color: #f6f2e8;
  font-size: 26rpx;
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
  border: none;
  width: 100%;
}

.submit-btn.disabled {
  background: #3a3528;
  color: #7c7464;
}
</style>
