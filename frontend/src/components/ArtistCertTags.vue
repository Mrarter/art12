<template>
  <view class="tag-wrap">
    <view class="tag-item" v-for="(tag, index) in tags" :key="index">{{ tag }}</view>
    <view class="tag-item tag-empty" v-if="!tags || tags.length === 0">平台认证艺术家</view>
  </view>
</template>

<script>
export default {
  name: 'ArtistCertTags',
  props: {
    tags: { type: Array, default: () => [] }
  }
}
</script>

<style lang="scss" scoped>
.tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
}

.tag-item {
  padding: 10rpx 22rpx;
  border-radius: 999rpx;
  background: rgba(201, 162, 39, 0.12);
  color: #d4af37;
  font-size: 24rpx;
  border: 1rpx solid rgba(201, 162, 39, 0.25);
}

.tag-empty {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.4);
  border-color: rgba(255, 255, 255, 0.1);
}
</style>
