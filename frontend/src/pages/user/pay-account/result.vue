<template>
  <view class="page">
    <view class="glow"></view>

    <view class="result-card">
      <view class="success-mark">
        <text class="check">✓</text>
      </view>
      <text class="title">{{ resultTitle }}</text>
      <text class="desc">{{ resultDesc }}</text>

      <view class="account-chip">
        <view class="chip-icon" :class="resultInfo.tone">{{ resultInfo.icon }}</view>
        <view class="chip-copy">
          <text class="chip-title">{{ resultInfo.name }}</text>
          <text class="chip-desc">已添加到你的收款账户</text>
        </view>
      </view>
    </view>

    <view class="tips-card">
      <text class="tips-title">下一步可以做什么</text>
      <text class="tips-line">• 默认收款账户会用于提现、转售收益和平台结算。</text>
      <text class="tips-line">• 后续可在“我的 - 收款账户”里管理、切换默认或删除账户。</text>
    </view>

    <view class="bottom-bar">
      <button class="primary-btn" @click="goAccountList">查看收款账户</button>
      <button class="secondary-btn" @click="goUserCenter">返回我的</button>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const type = ref('wechat')

const accountMap = {
  wechat: {
    name: '微信',
    icon: '微',
    tone: 'wechat',
    title: '微信绑定成功',
    desc: '微信授权已完成，当前微信已绑定为收款账户。'
  },
  alipay: {
    name: '支付宝',
    icon: '支',
    tone: 'alipay',
    title: '支付宝绑定成功',
    desc: '支付宝账号已添加成功，可用于后续收益结算。'
  },
  bank: {
    name: '银行卡',
    icon: '卡',
    tone: 'bank',
    title: '银行卡添加成功',
    desc: '银行卡已添加成功，可用于提现和平台结算。'
  }
}

const resultInfo = computed(() => accountMap[type.value] || accountMap.wechat)
const resultTitle = computed(() => resultInfo.value.title)
const resultDesc = computed(() => resultInfo.value.desc)

const goAccountList = () => {
  uni.redirectTo({ url: '/pages/user-extra/pay-account/list' })
}

const goUserCenter = () => {
  uni.switchTab({ url: '/pages/user/index' })
}

onLoad((options = {}) => {
  const nextType = String(options.type || '').toLowerCase()
  if (accountMap[nextType]) type.value = nextType
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 50% 6%, rgba(212, 175, 55, 0.24), transparent 34%),
    linear-gradient(180deg, #111 0%, #0b0b0b 100%);
  padding: 52rpx 28rpx 210rpx;
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
}

.glow {
  position: absolute;
  width: 520rpx;
  height: 520rpx;
  left: 50%;
  top: -220rpx;
  transform: translateX(-50%);
  background: rgba(212, 175, 55, 0.18);
  filter: blur(28rpx);
  border-radius: 50%;
}

.result-card,
.tips-card {
  position: relative;
  background: rgba(26, 26, 26, 0.94);
  border: 1rpx solid rgba(255, 255, 255, 0.07);
  border-radius: 28rpx;
  box-shadow: 0 28rpx 80rpx rgba(0, 0, 0, 0.26);
}

.result-card {
  padding: 56rpx 32rpx 34rpx;
  text-align: center;
}

.success-mark {
  width: 132rpx;
  height: 132rpx;
  border-radius: 50%;
  margin: 0 auto 30rpx;
  background: linear-gradient(135deg, #f5d86a, #b8891e);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 52rpx rgba(212, 175, 55, 0.36);
}

.check {
  color: #17120a;
  font-size: 72rpx;
  font-weight: 900;
  line-height: 1;
}

.title {
  display: block;
  color: #f7f2df;
  font-size: 42rpx;
  font-weight: 900;
  margin-bottom: 16rpx;
}

.desc {
  display: block;
  color: #aaa;
  font-size: 26rpx;
  line-height: 40rpx;
}

.account-chip {
  margin-top: 40rpx;
  padding: 24rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.045);
  display: flex;
  align-items: center;
  gap: 20rpx;
  text-align: left;
}

.chip-icon {
  width: 76rpx;
  height: 76rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  font-weight: 900;
  flex-shrink: 0;
}

.chip-icon.wechat {
  color: #07c160;
  background: rgba(7, 193, 96, 0.15);
}

.chip-icon.alipay {
  color: #1677ff;
  background: rgba(22, 119, 255, 0.15);
}

.chip-icon.bank {
  color: #d4af37;
  background: rgba(212, 175, 55, 0.15);
}

.chip-copy {
  flex: 1;
  min-width: 0;
}

.chip-title {
  color: #f5f5f5;
  font-size: 29rpx;
  font-weight: 800;
  display: block;
}

.chip-desc {
  color: #8d8d8d;
  font-size: 24rpx;
  display: block;
  margin-top: 6rpx;
}

.tips-card {
  margin-top: 24rpx;
  padding: 28rpx;
}

.tips-title {
  color: #d4af37;
  font-size: 28rpx;
  font-weight: 800;
  display: block;
  margin-bottom: 14rpx;
}

.tips-line {
  color: #999;
  font-size: 24rpx;
  line-height: 38rpx;
  display: block;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 18rpx 28rpx calc(22rpx + env(safe-area-inset-bottom));
  background: rgba(11, 11, 11, 0.96);
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.primary-btn,
.secondary-btn {
  height: 88rpx;
  border-radius: 999rpx;
  font-size: 29rpx;
  font-weight: 900;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.primary-btn {
  background: #d4af37;
  color: #17120a;
}

.secondary-btn {
  margin-top: 16rpx;
  background: rgba(255, 255, 255, 0.06);
  color: #d8d8d8;
}
</style>
