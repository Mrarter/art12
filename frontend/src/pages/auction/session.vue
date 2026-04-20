<template>
  <view class="auction-session">
    <view class="header">
      <text class="title">{{ sessionName }}</text>
      <text class="time">开始时间: {{ startTime }}</text>
    </view>
    <view class="lots">
      <view class="lot" v-for="item in lots" :key="item.id">
        <image :src="item.image" mode="aspectFill" />
        <view class="info">
          <text class="name">{{ item.title }}</text>
          <text class="current-price">当前价: ¥{{ item.currentPrice }}</text>
          <text class="bid-count">{{ item.bidCount }}次出价</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
const sessionName = ref('')
const startTime = ref('')
const lots = ref([])
onLoad(() => {})
</script>

<style lang="scss" scoped>
.auction-session {
  padding: 20rpx;
}
</style>