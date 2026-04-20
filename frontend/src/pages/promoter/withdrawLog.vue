<template>
  <view class="withdraw-log">
    <view class="list">
      <view class="item" v-for="item in records" :key="item.id">
        <view class="info">
          <text class="amount">¥{{ item.amount }}</text>
          <text class="status" :class="item.status">{{ item.statusText }}</text>
        </view>
        <text class="time">{{ item.createTime }}</text>
      </view>
    </view>
    <view class="empty" v-if="records.length === 0">
      <text>暂无提现记录</text>
    </view>
  </view>
</template>

<script setup>
const records = ref([])
onLoad(() => {})
</script>

<style lang="scss" scoped>
.withdraw-log {
  padding: 20rpx;
}
.item {
  padding: 30rpx;
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
}
.amount {
  font-size: 36rpx;
  font-weight: bold;
}
.status {
  margin-left: 20rpx;
}
.status.pending { color: #f90; }
.status.success { color: #52c41a; }
.status.failed { color: #f5222d; }
.empty {
  text-align: center;
  padding: 100rpx;
  color: #999;
}
</style>