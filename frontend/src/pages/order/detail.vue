<template>
  <view class="order-detail">
    <view class="detail-header">
      <text class="status-text">订单详情</text>
    </view>
    <view class="content">
      <text>订单ID: {{ orderId }}</text>
    </view>
  </view>
</template>

<script setup>
const orderId = ref('')
onLoad((options) => {
  orderId.value = options.id || ''
})
</script>

<style lang="scss" scoped>
.order-detail {
  padding: 20rpx;
}
</style>