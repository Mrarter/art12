<template>
  <view class="artist-cert">
    <view class="form">
      <view class="item">
        <text>真实姓名</text>
        <input v-model="form.realName" placeholder="请输入真实姓名" />
      </view>
      <view class="item">
        <text>身份证号</text>
        <input v-model="form.idCard" placeholder="请输入身份证号" />
      </view>
      <view class="item">
        <text>艺术领域</text>
        <input v-model="form.artField" placeholder="请输入擅长的艺术领域" />
      </view>
      <view class="upload">
        <text>身份证照片</text>
        <view class="images">
          <view class="upload-btn" @click="uploadIdCard('front')">上传正面</view>
          <view class="upload-btn" @click="uploadIdCard('back')">上传背面</view>
        </view>
      </view>
      <button @click="submit" class="submit-btn">提交认证</button>
    </view>
  </view>
</template>

<script setup>
const form = reactive({
  realName: '',
  idCard: '',
  artField: ''
})
const uploadIdCard = (type) => {}
const submit = () => {
  uni.showToast({ title: '提交成功' })
}
</script>

<style lang="scss" scoped>
.artist-cert {
  padding: 20rpx;
}
.item {
  display: flex;
  justify-content: space-between;
  padding: 30rpx 0;
  border-bottom: 1rpx solid #eee;
}
.submit-btn {
  margin-top: 60rpx;
  background: #667eea;
  color: #fff;
}
</style>