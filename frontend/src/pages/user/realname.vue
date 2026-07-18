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
      <view class="progress-item" :class="{ active: form.realName && validIdCard, done: useAlipayRealname ? form.faceVerified : form.status === 2 || form.status === 1 }">
        <text class="progress-dot">2</text>
        <text class="progress-text">{{ useAlipayRealname ? '支付宝实名' : '人工审核' }}</text>
      </view>
    </view>

    <view class="form-section">
      <view class="section-title">选择认证方式</view>
      <text class="section-desc">推荐使用支付宝快速认证；如果没有支付宝，可以选择平台人工认证。</text>
      <view class="auth-mode-list">
        <view class="auth-mode-card primary" :class="{ active: useAlipayRealname, disabled: !form.alipayEnabled }" @click="selectAuthMode('alipay')">
          <view class="auth-mode-badge alipay">支</view>
          <view class="auth-mode-copy">
            <text class="auth-mode-title">支付宝认证</text>
            <text class="auth-mode-desc">{{ form.alipayEnabled ? '优先推荐，完成后自动同步认证结果。' : '当前暂未开通，开通后可直接使用。' }}</text>
          </view>
          <text class="auth-mode-tag">{{ useAlipayRealname ? '已选择' : '推荐' }}</text>
        </view>
        <view class="auth-mode-card" :class="{ active: useManualAudit }" @click="selectAuthMode('manual')">
          <view class="auth-mode-badge manual">审</view>
          <view class="auth-mode-copy">
            <text class="auth-mode-title">没有支付宝，人工认证</text>
            <text class="auth-mode-desc">提交姓名和身份证号，由平台后台人工审核。</text>
          </view>
          <text class="auth-mode-tag">{{ useManualAudit ? '已选择' : '切换' }}</text>
        </view>
      </view>
    </view>

    <view id="identity-section" class="form-section">
      <view class="section-title">身份信息</view>
      <view class="form-item">
        <text class="label">真实姓名</text>
        <input
          class="input"
          v-model="form.realName"
          placeholder="请输入与证件一致的姓名"
          placeholder-class="placeholder"
          :disabled="isReadonly"
          :focus="focusRealName"
          @blur="focusRealName = false"
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
          :focus="focusIdCard"
          @input="onIdCardInput"
          @blur="onIdCardBlur"
        />
        <text v-if="idCardError" class="error-text">{{ idCardError }}</text>
      </view>
    </view>

    <view v-if="useManualAudit" class="form-section">
      <view class="section-title">平台人工审核</view>
      <text class="section-desc">提交真实姓名和身份证号后，平台会在后台进行人工审核。</text>
      <view class="face-card verified">
        <view class="face-icon">审</view>
        <view class="face-copy">
          <text class="face-title">无需跳转第三方认证</text>
          <text class="face-desc">请确认姓名和身份证号填写无误，提交后等待平台审核通过。</text>
        </view>
        <text class="face-action">人工审核</text>
      </view>
    </view>

    <view v-else class="form-section">
      <view class="section-title">支付宝实名认证</view>
      <text class="section-desc">{{ form.alipayEnabled ? '提交真实姓名和身份证号后，将跳转支付宝完成实名校验。认证通过后会自动回到当前页面。' : '支付宝实名认证能力暂未开通。你也可以切换到“没有支付宝，人工认证”。' }}</text>
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

    <view v-if="form.status === 1" class="success-actions">
      <button v-if="redirect" class="success-action-btn" @click="safeNavigateBack">继续下单</button>
      <button v-else class="success-action-btn secondary" @click="goUserCenter">返回个人中心</button>
    </view>

    <view class="bottom-bar">
      <button class="submit-btn" :class="{ disabled: !canSubmit || isSubmitLocked }" :disabled="isSubmitLocked" @click="handleSubmitClick">
        {{ submitting ? '提交中...' : submitText }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { submitRealnameCert, getRealnameCertStatus, startAlipayRealname, syncAlipayRealname } from '@/api/user.js'
import { uploadFile } from '@/api/file.js'
import { AUCTION_ENABLED } from '@/utils/platform.js'

const ALIPAY_REALNAME_DRAFT_KEY = 'alipay_realname_draft'
const TAB_BAR_PAGES = new Set([
  '/pages/index/index',
  '/pages/gallery/index',
  '/pages/cart/index',
  '/pages/user/index'
])
if (AUCTION_ENABLED) {
  TAB_BAR_PAGES.add('/pages/auction/index')
}

const form = ref({
  realName: '',
  idCard: '',
  idFront: '',
  idBack: '',
  faceVerified: false,
  idCardValid: false,
  status: 0,
  verifyMode: 'manual',
  alipayEnabled: true,
  certifyId: '',
  rejectReason: '',
  submittedAt: ''
})

const idCardError = ref('')
const uploading = ref(null)
const submitting = ref(false)
const redirect = ref('')
const authMode = ref('alipay')
const focusRealName = ref(false)
const focusIdCard = ref(false)
const restartMode = ref(false)
const autoStartAlipay = ref(false)

const useAlipayRealname = computed(() => authMode.value === 'alipay')
const useManualAudit = computed(() => !useAlipayRealname.value)
const isReadonly = computed(() => {
  if (restartMode.value) return false
  return form.value.status === 1 || (form.value.status === 2 && !useAlipayRealname.value)
})
const validIdCard = computed(() => form.value.idCardValid || validateIdCard(form.value.idCard))
const hasIdImages = computed(() => Boolean(form.value.idFront && form.value.idBack))
const rejectReason = computed(() => form.value.rejectReason)
const isSubmitLocked = computed(() => {
  if (submitting.value) return true
  if (form.value.status === 1 && !restartMode.value) return true
  return form.value.status === 2 && !useAlipayRealname.value
})

const isMaskedValue = (value) => String(value || '').includes('*')

const canSubmit = computed(() => {
  if (form.value.status === 1 && !restartMode.value) return false
  if (useAlipayRealname.value) {
    if (form.value.status === 2 && form.value.certifyId) return !submitting.value
    return Boolean(form.value.realName.trim() && validIdCard.value && !submitting.value)
  }
  return Boolean(form.value.realName.trim() && validIdCard.value && !submitting.value)
})

const selectAuthMode = (mode) => {
  if ((form.value.status === 1 && !restartMode.value) || submitting.value) return
  if (form.value.status === 2 && mode !== authMode.value) {
    uni.showToast({ title: '当前认证已提交，暂不能切换方式', icon: 'none' })
    return
  }
  authMode.value = mode
  form.value.verifyMode = mode
}

const statusMeta = computed(() => {
  if (restartMode.value) {
    return { text: '重新认证', desc: '请重新填写完整身份证号，并跳转支付宝完成一次新的真人实名校验。', icon: '支', className: 'pending' }
  }
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
    desc: useAlipayRealname.value ? '填写真实姓名和身份证号后，将跳转支付宝完成实名校验。' : '填写真实姓名和身份证号后，提交平台人工审核。',
    icon: '认',
    className: 'idle'
  }
})

const submitText = computed(() => {
  if (restartMode.value) return submitting.value ? '提交中...' : '重新发起支付宝认证'
  if (form.value.status === 1) return '已完成认证'
  if (form.value.status === 2) return useAlipayRealname.value ? '同步认证结果' : '认证审核中'
  if (submitting.value) return '提交中...'
  if (useAlipayRealname.value) return '去支付宝认证'
  if (form.value.status === 3) return '重新提交人工审核'
  return '提交人工审核'
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

const getAlipayDraft = () => {
  const draft = uni.getStorageSync(ALIPAY_REALNAME_DRAFT_KEY)
  return draft && typeof draft === 'object' ? draft : null
}

const restoreAlipayDraft = (fallbackCertifyId = '') => {
  const draft = getAlipayDraft()
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

const resetAlipayRetryState = () => {
  form.value.status = 0
  form.value.certifyId = ''
  form.value.rejectReason = ''
  form.value.faceVerified = false
  clearAlipayDraft()
}

const hasEditedPendingAlipayIdentity = () => {
  if (!useAlipayRealname.value || form.value.status !== 2 || !form.value.certifyId) return false
  const draft = getAlipayDraft()
  if (!draft) return Boolean(form.value.realName.trim() || form.value.idCard.trim())
  const currentRealName = form.value.realName.trim()
  const currentIdCard = form.value.idCard.trim().toUpperCase()
  const draftRealName = String(draft.realName || '').trim()
  const draftIdCard = String(draft.idCard || '').trim().toUpperCase()
  return currentRealName !== draftRealName || currentIdCard !== draftIdCard
}

const decodeRedirect = (value = '') => {
  let text = value || ''
  for (let i = 0; i < 2; i++) {
    try {
      const decoded = decodeURIComponent(text)
      if (decoded === text) break
      text = decoded
    } catch (e) {
      break
    }
  }
  return text
}

const safeNavigateBack = () => {
  if (!redirect.value) return
  const purePath = redirect.value.split('?')[0]
  if (TAB_BAR_PAGES.has(purePath)) {
    uni.switchTab({ url: purePath })
    return
  }
  uni.redirectTo({ url: redirect.value })
}

const goUserCenter = () => {
  uni.switchTab({ url: '/pages/user/index' })
}

const buildCurrentReturnUrl = () => {
  if (typeof window === 'undefined') {
    const query = []
    if (restartMode.value) query.push('restart=1')
    if (redirect.value) query.push(`redirect=${encodeURIComponent(redirect.value)}`)
    const suffix = query.length ? `?${query.join('&')}` : ''
    return `https://a.art1.cn/#/pages/user-extra/realname${suffix}`
  }
  return window.location.href
}

const launchAlipayRealname = async () => {
  if (!form.value.alipayEnabled) {
    uni.showModal({
      title: '支付宝认证暂未开通',
      content: '当前平台暂未开通支付宝实名认证。没有支付宝或暂时无法使用时，请选择“人工认证”。',
      confirmText: '切换人工',
      cancelText: '知道了',
      success: (res) => {
        if (res.confirm) selectAuthMode('manual')
      }
    })
    return
  }
  saveAlipayDraft()
  const result = await startAlipayRealname({
    realName: form.value.realName.trim(),
    idCard: form.value.idCard.toUpperCase(),
    returnUrl: buildCurrentReturnUrl(),
    restart: restartMode.value
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

const enableRestartMode = () => {
  restartMode.value = true
  authMode.value = 'alipay'
  form.value.verifyMode = 'alipay'
  form.value.status = 0
  form.value.certifyId = ''
  form.value.faceVerified = false
  form.value.rejectReason = ''
  form.value.idCard = ''
  form.value.idCardValid = false
  idCardError.value = ''
  clearAlipayDraft()
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

const onIdCardBlur = () => {
  focusIdCard.value = false
  validateIdCardField()
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

const scrollToIdentityForm = (field = 'realName') => {
  uni.pageScrollTo({
    selector: '#identity-section',
    duration: 260
  })
  setTimeout(() => {
    focusRealName.value = field === 'realName'
    focusIdCard.value = field === 'idCard'
  }, 320)
}

const getIdentityMissingField = () => {
  if (!form.value.realName.trim()) return 'realName'
  if (!form.value.idCard.trim() || !validateIdCardField()) return 'idCard'
  return ''
}

const handleSubmitClick = () => {
  if (isSubmitLocked.value) return
  const canSyncAlipay = useAlipayRealname.value && form.value.status === 2 && form.value.certifyId
  if (!canSyncAlipay) {
    const missingField = getIdentityMissingField()
    if (missingField) {
      uni.showToast({
        title: missingField === 'realName' ? '请先填写真实姓名' : '请先填写正确的身份证号',
        icon: 'none'
      })
      scrollToIdentityForm(missingField)
      return
    }
  }
  submitForm()
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
      if (hasEditedPendingAlipayIdentity()) {
        resetAlipayRetryState()
        await launchAlipayRealname()
        return
      }
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
              if (!res.confirm) return
              if (!restored) {
                resetAlipayRetryState()
                return
              }
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
  submitting.value = true
  try {
    await submitRealnameCert({
      realName: form.value.realName.trim(),
      idCard: form.value.idCard.toUpperCase(),
      idFrontUrl: form.value.idFront,
      idBackUrl: form.value.idBack,
      faceVerified: form.value.faceVerified || useManualAudit.value
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
    const draft = getAlipayDraft()
    let syncCertifyId = certifyId || draft?.certifyId || ''
    if (syncCertifyId) {
      await syncAlipayRealname({ certifyId: syncCertifyId })
    }
    let data = await getRealnameCertStatus()
    if (!syncCertifyId && data?.status === 2 && data?.verifyMode === 'alipay' && data?.certifyId) {
      syncCertifyId = data.certifyId
      await syncAlipayRealname({ certifyId: syncCertifyId })
      data = await getRealnameCertStatus()
    }
    const isHistoricalAlipayPending = data?.status === 2 && data?.verifyMode === 'alipay' && !syncCertifyId && !data?.certifyId
    form.value.alipayEnabled = data?.alipayEnabled !== false
    authMode.value = data?.status === 2 && data?.verifyMode === 'manual' ? 'manual' : 'alipay'
    form.value.verifyMode = authMode.value

    if (data && data.status > 0) {
      form.value.status = isHistoricalAlipayPending ? 0 : data.status
      const restored = data?.verifyMode === 'alipay' && data.status !== 1 && restoreAlipayDraft(data?.certifyId || syncCertifyId)
      if (data.maskedRealName && !restored && !useAlipayRealname.value) {
        form.value.realName = data.maskedRealName
      }
      if (data.maskedIdCard && !restored && !useAlipayRealname.value) {
        form.value.idCard = data.maskedIdCard
        form.value.idCardValid = true
      }
      form.value.rejectReason = isHistoricalAlipayPending ? '' : (data.rejectReason || '')
      form.value.submittedAt = data.submittedAt || ''
    }
    const shouldResetDisabledAlipay = !form.value.alipayEnabled && data?.verifyMode === 'alipay' && form.value.status !== 1
    if (shouldResetDisabledAlipay) {
      form.value.status = 0
      authMode.value = 'alipay'
      form.value.verifyMode = 'alipay'
      form.value.certifyId = ''
      form.value.rejectReason = ''
      clearAlipayDraft()
    }
    form.value.certifyId = shouldResetDisabledAlipay || isHistoricalAlipayPending ? '' : (syncCertifyId || data?.certifyId || '')
    if (useAlipayRealname.value && form.value.status === 1) {
      form.value.faceVerified = true
      clearAlipayDraft()
    }
    if (restartMode.value) {
      enableRestartMode()
    }
    if (autoStartAlipay.value && restartMode.value && useAlipayRealname.value) {
      const missingField = getIdentityMissingField()
      if (missingField) {
        scrollToIdentityForm(missingField)
        uni.showToast({
          title: missingField === 'realName' ? '请先填写真实姓名' : '请先填写正确的身份证号',
          icon: 'none'
        })
        return
      }
      await submitForm()
    }
  } catch (err) {
    console.warn('获取认证状态失败:', err.message)
  }
})

onLoad((options = {}) => {
  redirect.value = decodeRedirect(options.redirect || '')
  restartMode.value = String(options.restart || '') === '1'
  autoStartAlipay.value = String(options.autoStart || '') === '1'
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

.auth-mode-list {
  margin-top: 22rpx;
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.auth-mode-card {
  padding: 24rpx;
  border-radius: 18rpx;
  background: #202024;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.auth-mode-card.primary {
  background: linear-gradient(135deg, rgba(22, 119, 255, 0.12), rgba(32, 32, 36, 0.98));
  border-color: rgba(22, 119, 255, 0.22);
}

.auth-mode-card.active {
  border-color: rgba(201, 162, 39, 0.72);
  box-shadow: 0 0 0 2rpx rgba(201, 162, 39, 0.16);
}

.auth-mode-card.disabled {
  opacity: 0.72;
}

.auth-mode-badge {
  width: 72rpx;
  height: 72rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  font-weight: 900;
  flex-shrink: 0;
}

.auth-mode-badge.alipay {
  background: rgba(22, 119, 255, 0.2);
  color: #63a7ff;
}

.auth-mode-badge.manual {
  background: rgba(201, 162, 39, 0.16);
  color: #f2c85b;
}

.auth-mode-copy {
  flex: 1;
  min-width: 0;
}

.auth-mode-title {
  display: block;
  color: #f6f2e8;
  font-size: 28rpx;
  font-weight: 800;
}

.auth-mode-desc {
  display: block;
  margin-top: 8rpx;
  color: #9b958a;
  font-size: 24rpx;
  line-height: 34rpx;
}

.auth-mode-tag {
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(201, 162, 39, 0.14);
  color: #f2c85b;
  font-size: 22rpx;
  font-weight: 800;
  flex-shrink: 0;
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

.success-actions {
  margin-top: 24rpx;
}

.success-action-btn {
  height: 88rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #e7c14c, #c89c1d);
  color: #16130b;
  font-size: 30rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  width: 100%;
}

.success-action-btn.secondary {
  background: #1f1f23;
  color: #f6f2e8;
  border: 1rpx solid rgba(255, 255, 255, 0.12);
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
