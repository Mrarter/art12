<template>
  <view class="favorites">
    <view class="list">
      <view class="item" v-for="item in favorites" :key="item.id">
        <image :src="item.coverImage" mode="aspectFill" />
        <text class="title">{{ item.title }}</text>
        <text class="price">¥{{ item.price }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
const favorites = ref([])
</script>

<style lang="scss" scoped>
.favorites {
  padding: 20rpx;
}
.list {
  display: grid;
  grid-template-columns: 48% 48%;
  gap: 20rpx;
}
</style>