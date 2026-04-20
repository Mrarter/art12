<template>
  <view class="address-edit">
    <view class="form-item">
      <text class="label">收货人</text>
      <input v-model="form.consignee" placeholder="请输入收货人" />
    </view>
    <view class="form-item">
      <text class="label">手机号</text>
      <input v-model="form.phone" type="number" placeholder="请输入手机号" />
    </view>
    <view class="form-item">
      <text class="label">地址</text>
      <input v-model="form.address" placeholder="请输入详细地址" />
    </view>
    <button class="save-btn" @click="save">保存</button>
  </view>
</template>

<script setup>
const form = reactive({
  consignee: '',
  phone: '',
  address: ''
})
const save = () => {
  uni.showToast({ title: '保存成功' })
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.address-edit {
  padding: 20rpx;
}
.form-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #eee;
}
.label {
  width: 160rpx;
}
.save-btn {
  margin-top: 40rpx;
  background: #667eea;
  color: #fff;
}
</style>