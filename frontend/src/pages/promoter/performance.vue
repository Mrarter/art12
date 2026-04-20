<template>
  <view class="performance">
    <view class="header">
      <text class="title">业绩详情</text>
      <text class="amount">¥{{ totalAmount }}</text>
    </view>
    <view class="list">
      <view class="item" v-for="item in records" :key="item.id">
        <view class="info">
          <text class="order-no">订单: {{ item.orderNo }}</text>
          <text class="time">{{ item.createTime }}</text>
        </view>
        <text class="amount">+¥{{ item.amount }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
const totalAmount = ref(0)
const records = ref([])
onLoad(() => {})
</script>

<style lang="scss" scoped>
.performance {
  padding: 20rpx;
}
</style>