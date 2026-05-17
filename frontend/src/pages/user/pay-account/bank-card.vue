<template>
  <view class="page">
    <view class="form-card">
      <view class="form-item">
        <text class="label">持卡人姓名</text>
        <input
          class="input"
          v-model="form.realName"
          placeholder="请输入银行卡持卡人姓名"
          placeholder-class="ph"
          maxlength="20"
        />
      </view>

      <view class="form-item">
        <text class="label">身份证号</text>
        <input
          class="input"
          v-model="form.idCard"
          placeholder="请输入持卡人身份证号"
          placeholder-class="ph"
          maxlength="18"
        />
      </view>

      <view class="form-item">
        <text class="label">开户银行</text>
        <picker class="picker" :value="bankIndex" :range="bankList" @change="onBankChange">
          <view class="picker-value">{{ bankIndex >= 0 ? bankList[bankIndex] : '请选择开户银行' }}</view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">银行卡号</text>
        <input
          class="input"
          v-model="form.bankCard"
          placeholder="请输入银行卡号"
          placeholder-class="ph"
          maxlength="23"
          type="number"
        />
      </view>

      <view class="form-item">
        <text class="label">手机号</text>
        <input
          class="input"
          v-model="form.phone"
          placeholder="请输入银行预留手机号"
          placeholder-class="ph"
          maxlength="11"
          type="number"
        />
      </view>
    </view>

    <view class="notice-card">
      <text class="notice-title">温馨提示</text>
      <text class="notice-line">• 银行卡信息将加密存储，保障您的资金安全</text>
      <text class="notice-line">• 收款人姓名需与实名认证信息一致，否则无法提现</text>
      <text class="notice-line">• 仅支持储蓄卡，不支持信用卡</text>
    </view>

    <view class="bottom-bar">
      <button class="submit-btn" :class="{ disabled: !canSubmit || submitting }" :disabled="!canSubmit || submitting" @click="handleSubmit">
        {{ submitting ? '提交中...' : '确认添加' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { addPayAccount } from '@/api/pay'

const bankList = [
  '中国工商银行', '中国农业银行', '中国银行', '中国建设银行',
  '交通银行', '招商银行', '浦发银行', '中信银行',
  '中国光大银行', '华夏银行', '中国民生银行', '广发银行',
  '兴业银行', '平安银行', '中国邮政储蓄银行'
]

const bankIndex = ref(-1)
const submitting = ref(false)

const form = ref({
  realName: '',
  idCard: '',
  bankCard: '',
  phone: ''
})

const canSubmit = computed(() => {
  return form.value.realName.trim()
    && form.value.bankCard.trim().length >= 10
    && bankIndex.value >= 0
})

const onBankChange = (e) => {
  bankIndex.value = e.detail.value
}

const handleSubmit = async () => {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    await addPayAccount({
      accountType: 3,
      realName: form.value.realName.trim(),
      idCard: form.value.idCard.trim(),
      phone: form.value.phone.trim(),
      bankName: bankList[bankIndex.value],
      bankCard: form.value.bankCard.replace(/\s/g, ''),
      setDefault: true
    })
    uni.showToast({ title: '添加成功', icon: 'success' })
    uni.navigateBack()
  } catch (e) {
    uni.showToast({ title: e.message || '添加失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #0d0d0d;
  padding: 24rpx 24rpx 160rpx;
  box-sizing: border-box;
}

.form-card {
  background: #1a1a1a;
  border-radius: 20rpx;
  padding: 28rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
}

.form-item {
  margin-bottom: 28rpx;
}

.form-item:last-child {
  margin-bottom: 0;
}

.label {
  display: block;
  color: #f5f5f5;
  font-size: 26rpx;
  font-weight: 700;
  margin-bottom: 12rpx;
}

.input {
  height: 88rpx;
  padding: 0 20rpx;
  border-radius: 12rpx;
  background: #242424;
  color: #f5f5f5;
  font-size: 28rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
  box-sizing: border-box;
}

.ph {
  color: #666;
}

.picker {
  height: 88rpx;
  padding: 0 20rpx;
  border-radius: 12rpx;
  background: #242424;
  display: flex;
  align-items: center;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
}

.picker-value {
  color: #f5f5f5;
  font-size: 28rpx;
}

.notice-card {
  margin-top: 24rpx;
  background: #1a1a1a;
  border-radius: 20rpx;
  padding: 24rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
}

.notice-title {
  color: #d4af37;
  font-size: 26rpx;
  font-weight: 700;
  display: block;
  margin-bottom: 12rpx;
}

.notice-line {
  display: block;
  color: #888;
  font-size: 24rpx;
  margin-top: 8rpx;
  line-height: 36rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(13, 13, 13, 0.95);
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.submit-btn {
  height: 88rpx;
  border-radius: 999rpx;
  background: #d4af37;
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
