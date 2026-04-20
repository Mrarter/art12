<template>
  <view class="withdraw">
    <view class="balance">
      <text class="label">可提现余额</text>
      <text class="amount">¥{{ balance }}</text>
    </view>
    <view class="form">
      <view class="item">
        <text>提现金额</text>
        <input type="digit" v-model="amount" placeholder="请输入提现金额" />
      </view>
      <view class="item">
        <text>到账账户</text>
        <text class="account">微信自动到账</text>
      </view>
      <button @click="submit" class="submit-btn">申请提现</button>
      <text class="tip">提现将在1-3个工作日到账</text>
    </view>
  </view>
</template>

<script setup>
const balance = ref(0)
const amount = ref('')
const submit = () => {
  uni.showToast({ title: '申请成功' })
  uni.navigateBack()
}
onLoad(() => {})
</script>

<style lang="scss" scoped>
.withdraw {
  padding: 20rpx;
}
.balance {
  text-align: center;
  padding: 60rpx 0;
}
.amount {
  font-size: 48rpx;
  color: #e74c3c;
  font-weight: bold;
}
.submit-btn {
  margin-top: 40rpx;
  background: #667eea;
  color: #fff;
}
.tip {
  display: block;
  text-align: center;
  color: #999;
  margin-top: 20rpx;
}
</style>