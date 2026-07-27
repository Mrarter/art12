<template>
  <view class="cert-page">
    <!-- 顶部说明 -->
    <view class="cert-header">
      <view class="header-icon" :class="`status-${certStatus.status || 'none'}`">
        <text class="header-icon-symbol">{{ headerIcon }}</text>
      </view>
      <view class="header-text">
        <text class="title">{{ headerTitle }}</text>
        <text class="desc">{{ headerDesc }}</text>
        <text class="reject-reason" v-if="certStatus.status === 'rejected' && certStatus.rejectReason">
          认证未成功原因：{{ certStatus.rejectReason }}
        </text>
      </view>
    </view>

    <view class="guide-card" v-if="showGuideStep">
      <view class="guide-kicker">提交申请开始前的准备</view>
      <view class="guide-title">认证指南</view>
      <view class="guide-desc">当前阶段，分享平台并邀请好友完成注册，可获得平台赠送的 3,600 元年度服务费权益。</view>

      <view class="guide-steps">
        <view class="guide-step" v-for="item in guideSteps" :key="item.no">
          <text class="step-no">{{ item.no }}</text>
          <view class="step-copy">
            <text class="step-title">{{ item.title }}</text>
            <text class="step-desc">{{ item.desc }}</text>
          </view>
        </view>
      </view>

      <view class="fee-panel">
        <view class="fee-item">
          <text class="fee-label">年度服务费</text>
          <text class="fee-value">¥3,600 / 年</text>
        </view>
        <view class="fee-item secondary">
          <text class="fee-label">平台服务分成</text>
          <text class="fee-value subtle">每笔消费 15%</text>
        </view>
      </view>
    </view>

    <view class="agreement-card" v-if="showGuideStep">
      <view class="agreement-head">
        <text class="agreement-title">艺术家认证条款</text>
        <text class="agreement-desc">申请前请确认已阅读以下规则，平台将据此进行审核与后续管理。</text>
      </view>

      <view class="agreement-list">
        <view class="agreement-item" v-for="item in agreementRules" :key="item.title">
          <text class="agreement-item-title">{{ item.title }}</text>
          <text class="agreement-item-desc">{{ item.desc }}</text>
        </view>
      </view>

    </view>

    <!-- 认证状态 -->
    <view class="cert-status" v-if="certStatus.status !== 'none' && certStatus.status !== 'rejected'">
      <view class="status-badge" :class="certStatus.status">
        
        <text class="status-text">{{ certStatus.text }}</text>
      </view>
      <view class="status-detail" v-if="certStatus.status === 'pending'">
        <text>您的艺术家身份认证正在审核中，运营后台通过后才会开通艺术家主页、作品发布与艺术家工作台。</text>
      </view>
      <view class="status-detail" v-else-if="certStatus.status === 'approved'">
        <text>您的艺术家身份认证已通过，可以发布作品并管理艺术家主页。</text>
      </view>
      <view class="status-detail" v-else-if="certStatus.status === 'rejected'">
        <text>{{ certStatus.rejectReason ? `未通过原因：${certStatus.rejectReason}` : '本次认证未通过，可补充资料后重新提交。' }}</text>
      </view>
    </view>

    <!-- 认证表单 -->
    <view class="cert-form" v-if="showFormStep">
      <!-- 实名认证用户直接展示已同步身份信息，其他用户允许补充填写。 -->
      <view class="form-section identity-section">
        <view class="section-title">录入信息</view>
        <view class="form-item">
          <view class="item-label"><text class="required">*</text>真实姓名</view>
          <input
            class="item-input"
            :class="{ locked: realnameLocked }"
            v-model="form.realName"
            :disabled="realnameLocked"
            placeholder="请输入真实姓名"
            maxlength="30"
          />
          <text v-if="realnameLocked" class="field-hint">已根据实名认证信息自动填写，不可修改</text>
        </view>
        <view class="form-item">
          <view class="item-label"><text class="required">*</text>身份证号</view>
          <input
            class="item-input"
            :class="{ locked: realnameLocked }"
            v-model="form.idCard"
            :disabled="realnameLocked"
            placeholder="请输入18位身份证号"
            maxlength="18"
            @blur="validateIdCardField"
          />
          <text v-if="realnameLocked" class="field-hint">已根据实名认证信息自动填写，不可修改</text>
          <text v-else-if="idCardError" class="field-error">{{ idCardError }}</text>
        </view>
      </view>

      <view class="face-verify-card" :class="{ verified: form.faceVerified }" @click="startFaceVerify">
        <view class="face-icon alipay" :class="{ verified: form.faceVerified }">{{ form.faceVerified ? '支' : '支' }}</view>
        <view class="face-copy">
          <text class="face-title">真人识别</text>
          <text class="face-desc">
            {{ form.faceVerified ? '请选在光线明亮，背景为白墙的位置。' : '请确认本人操作，完成人脸识别后可提交认证' }}
          </text>
        </view>
        <text class="face-action">{{ form.faceVerified ? '真人识别' : '去认证' }}</text>
      </view>

      <!-- 作品展示 -->
      <view class="form-section">
        <view class="section-title">提供 20 件作品</view>
        <view class="upload-tips">请提供 20 件本人代表作品，平台将用于判断作品风格、创作稳定性与销售适配度。当前已选择 {{ form.artworks.length }}/{{ MAX_ARTWORK_COUNT }} 件，已上传 {{ uploadedArtworkCount }} 件。</view>
        
        <view class="works-uploader">
          <view class="works-list">
            <view class="work-item" v-for="(item, index) in form.artworks" :key="item.id" :class="`status-${item.status}`">
              <image :src="item.src" mode="aspectFill"></image>
              <view class="work-status" v-if="item.status === 'uploading'">
                <view class="work-spinner"></view>
                <text>上传中</text>
              </view>
              <view class="work-status failed" v-else-if="item.status === 'failed'" @click.stop="retryArtwork(index)">
                <text>上传失败</text>
                <text class="retry-text">点击重试</text>
              </view>
              <view class="work-delete" @click.stop="removeWork(index)"><text>×</text></view>
            </view>
            <view class="work-add" @click="chooseImage('artworks')" v-if="form.artworks.length < MAX_ARTWORK_COUNT">
              
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
    <view class="form-footer" v-if="showGuideStep">
      <view class="agreement-check footer-agreement-check" @click="toggleAgreement">
        <view class="agreement-checkbox" :class="{ checked: agreementAccepted }">
          <text v-if="agreementAccepted">✓</text>
        </view>
        <view class="agreement-copy">
          <text>我已阅读并同意</text>
          <text class="link" @click.stop="showAgreement">《艺术家认证条款》</text>
        </view>
      </view>
      <button class="submit-btn" :class="{ disabled: !agreementAccepted }" @click="enterFormStep">
        {{ certStatus.status === 'rejected' ? '重新申请认证' : '进入认证页面' }}
      </button>
      <view class="submit-tips">
        <text>需先勾选同意认证条款后才可进入认证页面</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { submitArtistCert, getArtistCertStatus, getRealnameCertStatus, startAlipayRealname, syncAlipayRealname } from '@/api/user.js'
import { getCategories } from '@/api/product.js'
import { openCropper, uploadFile } from '@/api/file.js'

const artFields = ref([
  { id: '', name: '加载中...' }
])

const MAX_ARTWORK_COUNT = 20
const idCardError = ref('')
const currentStep = ref('guide')
const agreementAccepted = ref(false)
const submitting = ref(false)
const realnameSynced = ref(false)
const returnedCertifyId = ref('')
const guideSteps = [
  {
    no: '01',
    title: '阅读认证指南',
    desc: '了解平台审核标准、3,600 元年度服务费、每笔消费 15% 平台服务分成，以及当前邀请好友赠送年费的阶段权益。'
  },
  {
    no: '02',
    title: '自动读取实名认证',
    desc: '系统直接读取已完成实名认证的用户身份信息，姓名和证件号无需重复填写。'
  },
  {
    no: '03',
    title: '提供 20 件代表作品',
    desc: '上传 20 件本人原创代表作品，平台将结合题材、风格、完成度和连续创作能力进行审核。'
  }
]
const agreementRules = [
  {
    title: '真实身份与原创承诺',
    desc: '须使用本人真实身份申请，不得冒用他人信息，不得代发、代售或提交存在版权风险、抄袭争议的作品。'
  },
  {
    title: '作品与主页要求',
    desc: '平台会重点审核代表作品数量、创作成熟度、简介规范性与持续创作能力，课堂习作、草稿和非原创内容将影响审核结果。'
  },
  {
    title: '交易与平台规则',
    desc: '认证通过后可在平台发布和销售作品，须遵守站内交易规则，不得诱导站外交易、交换私人联系方式或规避平台流程。'
  },
  {
    title: '费用、分成与处罚',
    desc: '认证服务涉及年度服务费与平台分成；如出现侵权、欺诈、违规交易等行为，平台有权驳回申请、撤销认证或限制账号权限。'
  }
]

const form = ref({
  realName: '',
  idCard: '',
  artField: '',
  artFieldName: '',
  resume: '',  // 后端字段名统一为resume
  faceVerified: false,
  artworks: []  // { id, src }
})

const certStatus = ref({
  status: 'none',
  text: '',
  icon: '',
  color: '',
  rejectReason: ''
})

const realnameLocked = computed(() => {
  return realnameSynced.value && Boolean(form.value.realName && form.value.idCard)
})

const uploadedArtworkCount = computed(() => {
  return form.value.artworks.filter(item => item.status === 'uploaded' && item.url).length
})

const headerTitle = computed(() => {
  if (certStatus.value.status === 'pending') return '艺术家身份认证中'
  if (certStatus.value.status === 'approved') return '认证艺术家'
  if (certStatus.value.status === 'rejected') return '艺术家认证未通过'
  return '成为认证艺术家'
})

const headerIcon = computed(() => {
  if (certStatus.value.status === 'pending') return '…'
  if (certStatus.value.status === 'approved') return '✓'
  if (certStatus.value.status === 'rejected') return '!'
  return '艺'
})

const headerDesc = computed(() => {
  if (certStatus.value.status === 'pending') return '您的认证资料已提交，平台正在审核，请等待运营后台审核结果。'
  if (certStatus.value.status === 'approved') return '您已获得平台认证标识、作品发布权限和作品流通能力。'
  if (certStatus.value.status === 'rejected') return certStatus.value.rejectReason ? '请根据未通过原因补充资料后重新提交认证申请。' : '请根据审核意见补充资料后重新提交认证申请。'
  return '完成认证后可发布作品、进入艺术家主页、获得平台认证标识与作品流通能力'
})

const canSubmit = computed(() => {
  return form.value.realName && 
         form.value.idCard &&
         form.value.faceVerified &&
         form.value.artworks.length >= MAX_ARTWORK_COUNT &&
         uploadedArtworkCount.value >= MAX_ARTWORK_COUNT
})
const showGuideStep = computed(() => {
  return (certStatus.value.status === 'none' || certStatus.value.status === 'rejected') && currentStep.value === 'guide'
})
const showFormStep = computed(() => {
  return (certStatus.value.status === 'none' || certStatus.value.status === 'rejected') && currentStep.value === 'form'
})

const validateIdCard = (value) => {
  const id = String(value || '').trim().toUpperCase()
  if (!/^\d{17}[\dX]$/.test(id)) return false

  const birth = id.slice(6, 14)
  const year = Number(birth.slice(0, 4))
  const month = Number(birth.slice(4, 6))
  const day = Number(birth.slice(6, 8))
  const date = new Date(year, month - 1, day)
  if (
    date.getFullYear() !== year ||
    date.getMonth() + 1 !== month ||
    date.getDate() !== day
  ) {
    return false
  }

  const weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
  const checks = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']
  const sum = id.slice(0, 17).split('').reduce((total, num, index) => {
    return total + Number(num) * weights[index]
  }, 0)
  return checks[sum % 11] === id[17]
}

const isMaskedIdCard = (value) => {
  return /\*/.test(String(value || '').trim())
}

const validateIdCardField = () => {
  if (!form.value.idCard) {
    idCardError.value = ''
    return true
  }
  if (realnameLocked.value || isMaskedIdCard(form.value.idCard)) {
    idCardError.value = ''
    return true
  }
  const valid = validateIdCard(form.value.idCard)
  idCardError.value = valid ? '' : '身份证号格式不正确，请检查号码、出生日期和校验位'
  return valid
}

const onFieldChange = (e) => {
  const index = e.detail.value
  form.value.artField = artFields.value[index].id
  form.value.artFieldName = artFields.value[index].name
}

const applyRealnameInfo = (data = {}) => {
  if (Number(data?.status) !== 1) return
  const realName = String(data?.realName || '').trim()
  const idCard = String(data?.idCard || data?.maskedIdCard || '').trim().toUpperCase()
  if (!realName) return

  form.value.realName = realName
  form.value.idCard = idCard
  realnameSynced.value = Boolean(realName && idCard)
  form.value.faceVerified = true
  idCardError.value = ''
  validateIdCardField()
}

const createArtworkItem = (src, status = 'pending') => ({
  id: `artwork-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
  src,
  url: '',
  status
})

const cropArtworkPathsSequentially = async (paths = []) => {
  const result = []
  for (const path of paths) {
    const cropped = await openCropper(path, { ratio: 'free', shape: 'square' }).catch(() => path)
    result.push(createArtworkItem(cropped))
  }
  return result
}

const chooseImage = (type) => {
  uni.chooseImage({
    count: type === 'artworks' ? Math.min(MAX_ARTWORK_COUNT - form.value.artworks.length, 9) : 1,
    sizeType: ['compressed'],
    success: async (res) => {
      if (type === 'artworks') {
        const paths = res.tempFilePaths
        cropArtworkPathsSequentially(paths).then((croppedList) => {
          const entries = croppedList.slice(0, MAX_ARTWORK_COUNT - form.value.artworks.length)
          form.value.artworks = [...form.value.artworks, ...entries]
          entries.forEach(entry => uploadArtwork(entry))
        })
      }
    }
  })
}

const removeWork = (index) => {
  form.value.artworks.splice(index, 1)  // 改为artworks
}

const updateArtwork = (id, patch) => {
  const index = form.value.artworks.findIndex(item => item.id === id)
  if (index < 0) return
  const next = [...form.value.artworks]
  next[index] = { ...next[index], ...patch }
  form.value.artworks = next
}

const uploadArtwork = async (entry) => {
  updateArtwork(entry.id, { status: 'uploading', error: '' })
  try {
    const url = await uploadFile(entry.src, 'artist-artwork')
    updateArtwork(entry.id, { url, src: url, status: 'uploaded' })
    return true
  } catch (error) {
    updateArtwork(entry.id, { status: 'failed', error: error?.message || '上传失败' })
    return false
  }
}

const retryArtwork = (index) => {
  const entry = form.value.artworks[index]
  if (!entry || entry.status === 'uploading') return
  uploadArtwork(entry)
}

const startFaceVerify = () => {
  // 优先使用后台已保存的实名认证信息，直接拉起支付宝真人认证。
  // 只有后台没有完整实名信息时，才回到实名信息页补全资料。
  startDirectAlipayRestart()
}

const buildDirectRestartReturnUrl = () => {
  const fallback = 'https://a.art1.cn'
  const origin = typeof window !== 'undefined' && window.location?.origin ? window.location.origin : fallback
  return `${origin}/#/pages/artist/cert`
}

const extractReturnedCertifyId = () => {
  if (returnedCertifyId.value) return returnedCertifyId.value
  if (typeof window === 'undefined') return ''
  const hash = window.location.hash || ''
  const hashQuery = hash.includes('?') ? hash.split('?')[1] : ''
  return new URLSearchParams(window.location.search).get('certifyId')
    || new URLSearchParams(hashQuery).get('certifyId')
    || ''
}

const startDirectAlipayRestart = async () => {
  uni.showLoading({ title: '拉起支付宝中...' })
  try {
    const result = await startAlipayRealname({
      returnUrl: buildDirectRestartReturnUrl(),
      restart: true
    })
    if (result?.redirectUrl) {
      if (typeof window !== 'undefined') {
        window.location.href = result.redirectUrl
        return
      }
      uni.showToast({ title: '请在 H5 页面中完成支付宝认证', icon: 'none' })
      return
    }
    if (result?.verified) {
      uni.showToast({ title: '当前实名认证信息已就绪', icon: 'success' })
      return
    }
    uni.showToast({ title: '未获取到支付宝认证地址', icon: 'none' })
  } catch (e) {
    const message = e?.message || '拉起支付宝失败'
    if (message.includes('未找到完整实名信息')) {
      const redirect = encodeURIComponent('/pages/artist/cert')
      uni.navigateTo({ url: `/pages/user-extra/realname?restart=1&autoStart=1&redirect=${redirect}` })
      return
    }
    uni.showToast({ title: message, icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

const toggleAgreement = () => {
  agreementAccepted.value = !agreementAccepted.value
}

const enterFormStep = () => {
  if (!agreementAccepted.value) {
    uni.showToast({ title: '请先阅读并同意认证条款', icon: 'none' })
    return
  }
  currentStep.value = 'form'
  uni.pageScrollTo({
    scrollTop: 0,
    duration: 200
  })
}

const showAgreement = () => {
  uni.navigateTo({ url: '/pages/user-extra/agreement?type=artist_cert' })
}

const submitForm = async () => {
  if (submitting.value) return
  if (!canSubmit.value) {
    if (!validateIdCardField()) {
      uni.showToast({ title: '身份证号格式不正确', icon: 'none' })
      return
    }
    if (form.value.artworks.length < MAX_ARTWORK_COUNT) {
      uni.showToast({ title: `请上传${MAX_ARTWORK_COUNT}件代表作品`, icon: 'none' })
      return
    }
    if (form.value.artworks.some(item => item.status === 'uploading')) {
      uni.showToast({ title: '请等待图片上传完成', icon: 'none' })
      return
    }
    if (form.value.artworks.some(item => item.status === 'failed')) {
      uni.showToast({ title: '有图片上传失败，请点击图片重试', icon: 'none' })
      return
    }
    uni.showToast({ title: '请完善必填信息', icon: 'none' })
    return
  }

  uni.showLoading({ title: '提交中...' })
  submitting.value = true
  
  try {
    const artworks = form.value.artworks.map(item => item.url || item.src)
    await submitArtistCert({
      realName: form.value.realName.trim(),
      idCard: form.value.idCard.toUpperCase(),
      artField: String(form.value.artField || '综合艺术'),
      resume: form.value.resume.trim() || '已完成实名认证，申请成为平台认证艺术家。',
      faceVerified: form.value.faceVerified,
      artworks
    })
    
    uni.hideLoading()
    uni.showToast({ title: '提交成功，请等待审核', icon: 'success' })
    
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '提交失败，请重试', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

const ensureUploaded = async (path) => {
  const value = String(path || '').trim()
  if (!value) return value
  if (/^https?:\/\//.test(value) || value.startsWith('/upload/')) return value
  return uploadFile(value, 'image')
}

const reApply = () => {
  certStatus.value.status = 'none'
  currentStep.value = 'guide'
  agreementAccepted.value = false
}

const normalizeCertStatus = (data = {}) => {
  const status = data.status
  if (status === 0 || status === '0' || status === 'pending') {
    return {
      status: 'pending',
      text: '艺术家身份认证中',
      icon: '',
      color: '',
      rejectReason: data.rejectReason || ''
    }
  }
  if (status === 1 || status === '1' || status === 'approved' || data.isArtist === true) {
    return {
      status: 'approved',
      text: '认证艺术家',
      icon: '',
      color: '',
      rejectReason: ''
    }
  }
  if (status === 2 || status === '2' || status === 'rejected') {
    return {
      status: 'rejected',
      text: '艺术家认证未通过',
      icon: '',
      color: '',
      rejectReason: data.rejectReason || ''
    }
  }
  return {
    status: 'none',
    text: '',
    icon: '',
    color: '',
    rejectReason: ''
  }
}

const loadCertStatus = async () => {
  try {
    const data = await getArtistCertStatus()
    certStatus.value = normalizeCertStatus(data)
  } catch (e) {
    certStatus.value = normalizeCertStatus()
  }
  currentStep.value = certStatus.value.status === 'none' || certStatus.value.status === 'rejected' ? 'guide' : 'form'
}

const loadRealnameInfo = async () => {
  try {
    const data = await getRealnameCertStatus()
    applyRealnameInfo(data)
  } catch (e) {
    realnameSynced.value = false
  }
}

const syncReturnedAlipayResult = async () => {
  const certifyId = extractReturnedCertifyId()
  if (!certifyId) return
  try {
    const data = await syncAlipayRealname({ certifyId })
    applyRealnameInfo(data)
    if (Number(data?.status) === 1) {
      uni.showToast({ title: '已同步支付宝认证结果', icon: 'success' })
    } else if (Number(data?.status) === 3) {
      uni.showToast({ title: data?.rejectReason || '支付宝认证未通过', icon: 'none' })
    }
  } catch (e) {
    console.warn('同步支付宝认证结果失败', e)
  }
}

onMounted(async () => {
  await syncReturnedAlipayResult()
  await loadCertStatus()
  await loadRealnameInfo()

  // 从API加载分类数据
  try {
    const list = await getCategories()
    if (list && list.length > 0) {
      artFields.value = list.map(item => ({ id: item.id, name: item.name }))
    }
  } catch (e) {
    console.warn('获取分类失败，使用默认分类', e)
    artFields.value = [
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
  }
})

onLoad((options = {}) => {
  returnedCertifyId.value = String(options.certifyId || '').trim()
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
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 20rpx;
    flex-shrink: 0;
  }

  .header-icon-symbol {
    font-size: 40rpx;
    font-weight: 800;
    line-height: 1;
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

    .reject-reason {
      display: block;
      margin-top: 12rpx;
      font-size: 24rpx;
      line-height: 1.5;
      color: rgba(255, 255, 255, 0.88);
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

.guide-card {
  margin: 20rpx;
  padding: 36rpx;
  border-radius: 28rpx;
  color: #f4f4f4;
  background: #1c1c1c;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 20rpx 50rpx rgba(0, 0, 0, 0.18);

  .guide-kicker {
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.38);
    margin-bottom: 12rpx;
  }

  .guide-title {
    font-size: 44rpx;
    font-weight: 700;
    margin-bottom: 16rpx;
  }

  .guide-desc {
    display: block;
    font-size: 26rpx;
    line-height: 1.6;
    color: #d8bf79;
    margin-bottom: 30rpx;
  }

  .guide-step {
    display: flex;
    gap: 24rpx;
    padding: 28rpx 0;
    border-top: 1rpx solid rgba(255, 255, 255, 0.08);

    .step-no {
      width: 80rpx;
      font-size: 44rpx;
      font-weight: 700;
      color: rgba(255, 255, 255, 0.82);
    }

    .step-copy {
      flex: 1;
    }

    .step-title {
      display: block;
      font-size: 30rpx;
      font-weight: 700;
      color: #f7f7f7;
      margin-bottom: 12rpx;
    }

    .step-desc {
      display: block;
      font-size: 25rpx;
      line-height: 1.55;
      color: rgba(255, 255, 255, 0.62);
    }
  }

  .fee-panel {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16rpx;
    margin-top: 18rpx;
  }

  .fee-item {
    padding: 22rpx;
    border-radius: 18rpx;
    background: rgba(216, 191, 121, 0.1);
    border: 1rpx solid rgba(216, 191, 121, 0.24);

    &.secondary {
      background: rgba(255, 255, 255, 0.04);
      border-color: rgba(255, 255, 255, 0.08);
    }
  }

  .fee-label {
    display: block;
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.56);
    margin-bottom: 8rpx;
  }

  .fee-value {
    display: block;
    font-size: 28rpx;
    font-weight: 700;
    color: #d8bf79;

    &.subtle {
      font-size: 22rpx;
      font-weight: 500;
      color: rgba(255, 255, 255, 0.5);
    }
  }
}

.agreement-card {
  margin: 0 20rpx 20rpx;
  padding: 30rpx;
  border-radius: 20rpx;
  background: #171719;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
}

.agreement-head {
  margin-bottom: 24rpx;
}

.agreement-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #f6f2e8;
}

.agreement-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #9b958a;
}

.agreement-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.agreement-item {
  padding: 22rpx 24rpx;
  border-radius: 16rpx;
  background: #202024;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
}

.agreement-item-title {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
  color: #f6f2e8;
  margin-bottom: 10rpx;
}

.agreement-item-desc {
  display: block;
  font-size: 23rpx;
  line-height: 1.7;
  color: #9b958a;
}

.agreement-check {
  margin-top: 26rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.footer-agreement-check {
  margin: 0 0 20rpx;
  justify-content: center;
}

.agreement-checkbox {
  width: 36rpx;
  height: 36rpx;
  border-radius: 10rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.18);
  background: #202024;
  color: #16130b;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 22rpx;
  font-weight: 800;

  &.checked {
    background: #c9a227;
    border-color: #c9a227;
  }
}

.agreement-copy {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  font-size: 24rpx;
  color: #9b958a;

  .link {
    color: #c9a227;
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

.field-error {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 32rpx;
  color: #ff6b6b;
}

.field-hint {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 32rpx;
  color: #8bbd8f;
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

      .work-status {
        position: absolute;
        inset: 0;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 8rpx;
        background: rgba(0, 0, 0, 0.58);
        color: #fff;
        font-size: 22rpx;

        &.failed {
          background: rgba(125, 30, 35, 0.82);
          color: #ffe1e1;
        }
      }

      .retry-text {
        color: #ffd666;
        font-size: 20rpx;
      }

      .work-spinner {
        width: 32rpx;
        height: 32rpx;
        border: 4rpx solid rgba(255, 255, 255, 0.35);
        border-top-color: #fff;
        border-radius: 50%;
        animation: artwork-upload-spin 0.8s linear infinite;
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

        text {
          color: #fff;
          font-size: 32rpx;
          line-height: 1;
        }
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

@keyframes artwork-upload-spin {
  to { transform: rotate(360deg); }
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

/* 身份入口二级页：暗色重构覆盖层 */
.cert-page {
  background: #0b0b0c;
  color: #f6f2e8;
  padding: 24rpx 24rpx calc(160rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.cert-header {
  margin-bottom: 20rpx;
  padding: 30rpx;
  border-radius: 18rpx;
  background:
    linear-gradient(135deg, rgba(201, 162, 39, 0.24), rgba(23, 23, 25, 0.96)),
    #171719;
  border: 1rpx solid rgba(255, 255, 255, 0.08);

  .header-icon {
    background: rgba(201, 162, 39, 0.16);

    &.status-pending {
      background: rgba(201, 162, 39, 0.18);
      color: #f1d27a;
    }

    &.status-approved {
      background: rgba(48, 181, 104, 0.18);
      color: #7fe0a1;
    }

    &.status-rejected {
      background: rgba(255, 107, 107, 0.16);
      color: #ff9c9c;
    }

    &.status-none {
      background: rgba(201, 162, 39, 0.16);
      color: #f1d27a;
    }
  }

  .header-text {
    .title {
      color: #f6f2e8;
      font-size: 36rpx;
      line-height: 44rpx;
      font-weight: 800;
    }

    .desc {
      color: #9b958a;
      opacity: 1;
    }

    .reject-reason {
      color: #f3d7d7;
    }
  }
}

.cert-status,
.form-section {
  background: #171719;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 16rpx;
}

.cert-status {
  margin: 0 0 20rpx;
  color: #f6f2e8;

  .status-detail {
    color: #9b958a;
  }
}

.cert-form {
  padding: 0;
}

.form-section {
  .section-title {
    color: #f6f2e8;
    border-left-color: #c9a227;
  }

  .section-hint,
  .upload-tips {
    color: #68645c;
  }
}

.form-item {
  .item-label {
    color: #f6f2e8;
  }

  .item-input,
  .item-picker,
  .item-textarea {
    background: #202024;
    color: #f6f2e8;
  }

  .item-input.locked {
    color: #d7d1c7;
    background: rgba(201, 162, 39, 0.08);
  }

  .item-picker .picker-content {
    color: #f6f2e8;
  }
}

.form-footer {
  padding-bottom: calc(30rpx + env(safe-area-inset-bottom));
}

.placeholder,
.upload-group .upload-title {
  color: #9b958a;
}

.upload-grid .upload-item,
.works-uploader .works-list .work-add {
  background: #202024;
  border-color: rgba(255, 255, 255, 0.12);
}

.face-verify-card {
  margin-top: 28rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, rgba(201, 162, 39, 0.12), rgba(32, 32, 36, 0.96));
  border: 1rpx solid rgba(201, 162, 39, 0.32);
  display: flex;
  align-items: center;
  gap: 20rpx;

  &.verified {
    border-color: rgba(82, 196, 26, 0.45);
    background: linear-gradient(135deg, rgba(82, 196, 26, 0.12), rgba(32, 32, 36, 0.96));
  }
}

.face-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: rgba(22, 119, 255, 0.16);
  color: #1677ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  font-weight: 800;
  flex-shrink: 0;

  &.verified {
    background: rgba(82, 196, 26, 0.16);
    color: #52c41a;
  }

  &.alipay {
    font-size: 34rpx;
    letter-spacing: 1rpx;
  }
}

.face-copy {
  flex: 1;
  min-width: 0;
}

.face-title {
  display: block;
  color: #f6f2e8;
  font-size: 28rpx;
  font-weight: 700;
  margin-bottom: 8rpx;
}

.face-desc {
  display: block;
  color: #9b958a;
  font-size: 24rpx;
  line-height: 34rpx;
}

.face-action {
  color: #c9a227;
  font-size: 26rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.works-uploader .works-list .work-add text,
.upload-grid .upload-item .upload-placeholder .upload-text {
  color: #9b958a;
}

.form-footer {
  background: rgba(11, 11, 12, 0.96);
  border-top: 1rpx solid rgba(255, 255, 255, 0.08);
  box-shadow: none;

  .submit-btn {
    background: #c9a227;
    color: #16130b;

    &.disabled {
      background: #343436;
      color: #68645c;
    }
  }

  .submit-tips {
    color: #9b958a;

    .link {
      color: #c9a227;
    }
  }
}
</style>
