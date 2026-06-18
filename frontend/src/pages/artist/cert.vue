<template>
  <view class="cert-page">
    <!-- 顶部说明 -->
    <view class="cert-header">
      <view class="header-icon">
        
      </view>
      <view class="header-text">
        <text class="title">{{ headerTitle }}</text>
        <text class="desc">{{ headerDesc }}</text>
      </view>
    </view>

    <view class="guide-card" v-if="certStatus.status === 'none' || certStatus.status === 'rejected'">
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
        <view class="fee-item">
          <text class="fee-label">平台服务分成</text>
          <text class="fee-value">每笔消费 15%</text>
        </view>
      </view>
    </view>

    <!-- 认证状态 -->
    <view class="cert-status" v-if="certStatus.status !== 'none'">
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
    <view class="cert-form" v-if="certStatus.status === 'none' || certStatus.status === 'rejected'">
      <!-- 基本信息 -->
      <view class="form-section">
        <view class="section-title">录入信息</view>
        
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
          <input
            class="item-input"
            v-model="form.idCard"
            placeholder="请输入18位身份证号"
            placeholder-class="placeholder"
            maxlength="18"
            @input="onIdCardInput"
            @blur="validateIdCardField"
          />
          <text v-if="idCardError" class="field-error">{{ idCardError }}</text>
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

        <view class="face-verify-card" :class="{ verified: form.faceVerified }" @click="startFaceVerify">
          <view class="face-icon">{{ form.faceVerified ? '✓' : '' }}</view>
          <view class="face-copy">
            <text class="face-title">人脸识别认证</text>
            <text class="face-desc">
              {{ form.faceVerified ? '已完成真人核验，将随认证资料一并提交' : '请确认本人操作，完成人脸识别后可提交认证' }}
            </text>
          </view>
          <text class="face-action">{{ form.faceVerified ? '已认证' : '去认证' }}</text>
        </view>
      </view>

      <!-- 作品展示 -->
      <view class="form-section">
        <view class="section-title">提供 20 件作品</view>
        <view class="upload-tips">请提供 20 件本人代表作品，平台将用于判断作品风格、创作稳定性与销售适配度。当前已上传 {{ form.artworks.length }}/{{ MAX_ARTWORK_COUNT }} 件。</view>
        
        <view class="works-uploader">
          <view class="works-list">
            <view class="work-item" v-for="(item, index) in form.artworks" :key="index">
              <image :src="item" mode="aspectFill"></image>
              <view class="work-delete" @click="removeWork(index)">
                
              </view>
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
    <view class="form-footer" v-if="certStatus.status === 'rejected'">
      <button class="submit-btn" @click="reApply">重新申请认证</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { becomeArtist, getArtistCertStatus } from '@/api/user.js'
import { getCategories } from '@/api/product.js'
import { openCropper } from '@/api/file.js'

const artFields = ref([
  { id: '', name: '加载中...' }
])

const MAX_ARTWORK_COUNT = 20
const idCardError = ref('')
const guideSteps = [
  {
    no: '01',
    title: '阅读认证指南',
    desc: '了解平台审核标准、3,600 元年度服务费、每笔消费 15% 平台服务分成，以及当前邀请好友赠送年费的阶段权益。'
  },
  {
    no: '02',
    title: '录入真实个人信息',
    desc: '填写真实姓名、身份证号、艺术领域和个人简介，并完成身份证照片与本人核验，避免冒用他人作品或身份。'
  },
  {
    no: '03',
    title: '提供 20 件代表作品',
    desc: '上传 20 件本人原创代表作品，平台将结合题材、风格、完成度和连续创作能力进行审核。'
  }
]

const form = ref({
  realName: '',
  idCard: '',
  artField: '',
  artFieldName: '',
  resume: '',  // 后端字段名统一为resume
  idCardFront: '',
  idCardBack: '',
  faceVerified: false,
  artworks: []  // 后端字段名统一为artworks
})

const certStatus = ref({
  status: 'none',
  text: '',
  icon: '',
  color: '',
  rejectReason: ''
})

const headerTitle = computed(() => {
  if (certStatus.value.status === 'pending') return '艺术家身份认证中'
  if (certStatus.value.status === 'approved') return '认证艺术家'
  if (certStatus.value.status === 'rejected') return '艺术家认证未通过'
  return '成为认证艺术家'
})

const headerDesc = computed(() => {
  if (certStatus.value.status === 'pending') return '您的认证资料已提交，平台正在审核，请等待运营后台审核结果。'
  if (certStatus.value.status === 'approved') return '您已获得平台认证标识、作品发布权限和作品流通能力。'
  if (certStatus.value.status === 'rejected') return '请根据审核意见补充资料后重新提交认证申请。'
  return '完成认证后可发布作品、进入艺术家主页、获得平台认证标识与作品流通能力'
})

const canSubmit = computed(() => {
  return form.value.realName && 
         validateIdCard(form.value.idCard) &&
         form.value.artField && 
         form.value.resume &&  // 改为resume
         form.value.idCardFront && 
         form.value.idCardBack &&
         form.value.faceVerified &&
         form.value.artworks.length >= MAX_ARTWORK_COUNT
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

const onIdCardInput = (e) => {
  form.value.idCard = String(e.detail.value || '').toUpperCase()
  if (idCardError.value && validateIdCard(form.value.idCard)) {
    idCardError.value = ''
  }
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

const onFieldChange = (e) => {
  const index = e.detail.value
  form.value.artField = artFields.value[index].id
  form.value.artFieldName = artFields.value[index].name
}

const chooseImage = (type) => {
  uni.chooseImage({
    count: type === 'artworks' ? Math.min(MAX_ARTWORK_COUNT - form.value.artworks.length, 9) : 1,
    sizeType: ['compressed'],
    success: (res) => {
      if (type === 'idCardFront') {
        form.value.idCardFront = res.tempFilePaths[0]
      } else if (type === 'idCardBack') {
        form.value.idCardBack = res.tempFilePaths[0]
      } else if (type === 'artworks') {
        const paths = res.tempFilePaths
        Promise.all(paths.map(p =>
          openCropper(p, { ratio: 'free', shape: 'square' }).catch(() => p)
        )).then(croppedList => {
          form.value.artworks.push(...croppedList)
          form.value.artworks = form.value.artworks.slice(0, MAX_ARTWORK_COUNT)
        })
      }
    }
  })
}

const removeWork = (index) => {
  form.value.artworks.splice(index, 1)  // 改为artworks
}

const startFaceVerify = () => {
  if (form.value.faceVerified) return
  uni.showModal({
    title: '人脸识别认证',
    content: '请确保由申请人本人操作。当前为本地调试流程，确认后标记为已完成。',
    confirmText: '开始认证',
    success: (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '认证中...' })
        setTimeout(() => {
          uni.hideLoading()
          form.value.faceVerified = true
          uni.showToast({ title: '人脸认证完成', icon: 'success' })
        }, 700)
      }
    }
  })
}

const showAgreement = () => {
  uni.navigateTo({ url: '/pages/user/agreement?type=artist_cert' })
}

const submitForm = async () => {
  if (!canSubmit.value) {
    if (!validateIdCardField()) {
      uni.showToast({ title: '身份证号格式不正确', icon: 'none' })
      return
    }
    if (form.value.artworks.length < MAX_ARTWORK_COUNT) {
      uni.showToast({ title: `请上传${MAX_ARTWORK_COUNT}件代表作品`, icon: 'none' })
      return
    }
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
}

onMounted(async () => {
  await loadCertStatus()

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

  .item-picker .picker-content {
    color: #f6f2e8;
  }
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
  background: rgba(201, 162, 39, 0.2);
  color: #c9a227;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
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
