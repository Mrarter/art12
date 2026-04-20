<template>
  <view class="team">
    <view class="header">
      <text class="title">我的团队</text>
      <text class="count">{{ memberCount }} 人</text>
    </view>
    <view class="list">
      <view class="item" v-for="item in members" :key="item.id">
        <image :src="item.avatar" class="avatar" />
        <view class="info">
          <text class="name">{{ item.nickname }}</text>
          <text class="time">加入时间: {{ item.joinTime }}</text>
        </view>
        <text class="level">{{ item.level }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
const memberCount = ref(0)
const members = ref([])
onLoad(() => {})
</script>

<style lang="scss" scoped>
.team {
  padding: 20rpx;
}
</style>