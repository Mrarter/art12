<template>
  <view class="bidding">
    <view class="artwork">
      <image :src="image" mode="aspectFill" />
      <text class="title">{{ title }}</text>
      <text class="current-price">当前价: ¥{{ currentPrice }}</text>
    </view>
    <view class="bid-form">
      <text>出价</text>
      <input type="number" v-model="price" :placeholder="'最低出价 ¥' + minPrice" />
      <button @click="submitBid">确认出价</button>
    </view>
  </view>
</template>

<script setup>
const image = ref('')
const title = ref('')
const currentPrice = ref(0)
const minPrice = ref(0)
const price = ref('')
const submitBid = () => {
  uni.showToast({ title: '出价成功' })
}
onLoad(() => {})
</script>

<style lang="scss" scoped>
.bidding {
  padding: 20rpx;
}
.bid-form {
  margin-top: 40rpx;
}
</style>