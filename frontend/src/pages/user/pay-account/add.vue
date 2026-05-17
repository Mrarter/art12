<template>
  <view class="page">
    <view class="section-title">选择账户类型</view>
    <view class="type-list">
      <view class="type-card" @click="goBankCard">
        <view class="type-icon bank">
          <text class="icon-char">卡</text>
        </view>
        <view class="type-info">
          <text class="type-name">银行卡</text>
          <text class="type-desc">支持各大银行储蓄卡</text>
        </view>
        <text class="type-arrow">›</text>
      </view>

      <view class="type-card" @click="addAlipay">
        <view class="type-icon alipay">
          <text class="icon-char">宝</text>
        </view>
        <view class="type-info">
          <text class="type-name">支付宝</text>
          <text class="type-desc">绑定支付宝账号收款</text>
        </view>
        <text class="type-arrow">›</text>
      </view>

      <view class="type-card" @click="addWechat">
        <view class="type-icon wechat">
          <text class="icon-char">信</text>
        </view>
        <view class="type-info">
          <text class="type-name">微信</text>
          <text class="type-desc">使用微信零钱收款</text>
        </view>
        <text class="type-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup>
const goBankCard = () => {
  uni.navigateTo({ url: '/pages/user/pay-account/bank-card' })
}

const addAlipay = () => {
  uni.showModal({
    title: '添加支付宝',
    content: '请输入您的真实姓名',
    editable: true,
    placeholderText: '请输入真实姓名',
    success: (nameRes) => {
      if (!nameRes.confirm || !nameRes.content) return
      uni.showModal({
        title: '支付宝账号',
        content: '请输入您的支付宝账号',
        editable: true,
        placeholderText: '手机号或邮箱',
        success: async (res) => {
          if (!res.confirm || !res.content) return
          try {
            const { addPayAccount } = await import('@/api/pay')
            await addPayAccount({
              accountType: 2,
              realName: nameRes.content.trim(),
              alipayAccount: res.content.trim(),
              setDefault: true
            })
            uni.showToast({ title: '添加成功', icon: 'success' })
            uni.navigateBack()
          } catch (e) {
            uni.showToast({ title: e.message || '添加失败', icon: 'none' })
          }
        }
      })
    }
  })
}

const addWechat = () => {
  uni.showModal({
    title: '添加微信',
    content: '微信收款通过实名认证后自动绑定，无需手动添加。',
    showCancel: false,
    confirmText: '知道了'
  })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #0d0d0d;
  padding: 24rpx;
  box-sizing: border-box;
}

.section-title {
  color: #f5f5f5;
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 24rpx;
  padding-left: 8rpx;
}

.type-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.type-card {
  background: #1a1a1a;
  border-radius: 20rpx;
  padding: 28rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
}

.type-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.type-icon.bank { background: rgba(212, 175, 55, 0.12); }
.type-icon.alipay { background: rgba(22, 119, 255, 0.12); }
.type-icon.wechat { background: rgba(7, 193, 96, 0.12); }

.icon-char { font-size: 36rpx; }

.type-info { flex: 1; }

.type-name {
  color: #f5f5f5;
  font-size: 28rpx;
  font-weight: 700;
  display: block;
  margin-bottom: 6rpx;
}

.type-desc {
  color: #888;
  font-size: 24rpx;
}

.type-arrow {
  color: #666;
  font-size: 36rpx;
  flex-shrink: 0;
}
</style>
