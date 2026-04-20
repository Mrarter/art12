<template>
  <view class="commission">
    <view class="header">
      <text class="title">佣金明细</text>
      <view class="stat">
        <view class="item">
          <text class="value">{{ total }}</text>
          <text class="label">累计佣金</text>
        </view>
        <view class="item">
          <text class="value">{{ withdraw }}</text>
          <text class="label">已提现</text>
        </view>
        <view class="item">
          <text class="value">{{ available }}</text>
          <text class="label">可提现</text>
        </view>
      </view>
    </view>
    <view class="list">
      <view class="item" v-for="item in records" :key="item.id">
        <text class="desc">{{ item.desc }}</text>
        <text class="amount">+¥{{ item.amount }}</text>
        <text class="time">{{ item.createTime }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
const total = ref(0)
const withdraw = ref(0)
const available = ref(0)
const records = ref([])
onLoad(() => {})
</script>

<style lang="scss" scoped>
.commission {
  padding: 20rpx;
}
</style>