<template>
  <view class="address-list">
    <view class="address-item" v-for="item in addresses" :key="item.id">
      <view class="info">
        <text class="name">{{ item.consignee }}</text>
        <text class="phone">{{ item.phone }}</text>
      </view>
      <text class="address">{{ item.province }}{{ item.city }}{{ item.district }}{{ item.address }}</text>
    </view>
    <view class="add-btn" @click="addAddress">
      <text>+ 添加收货地址</text>
    </view>
  </view>
</template>

<script setup>
const addresses = ref([])
const addAddress = () => {
  uni.navigateTo({ url: '/pages/user/addressEdit' })
}
</script>

<style lang="scss" scoped>
.address-list {
  padding: 20rpx;
}
.add-btn {
  text-align: center;
  padding: 30rpx;
  color: #667eea;
}
</style>