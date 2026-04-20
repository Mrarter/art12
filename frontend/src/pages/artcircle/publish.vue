<template>
  <view class="publish">
    <textarea v-model="content" placeholder="分享你的想法..." class="content-input" />
    <view class="images">
      <view class="image-item" v-for="(img, i) in images" :key="i">
        <image :src="img" />
        <text class="remove" @click="removeImage(i)">×</text>
      </view>
      <view class="add-image" @click="chooseImage">+</view>
    </view>
    <button @click="publish" class="publish-btn">发布</button>
  </view>
</template>

<script setup>
const content = ref('')
const images = ref([])
const chooseImage = () => {}
const removeImage = (i) => {
  images.value.splice(i, 1)
}
const publish = () => {
  uni.showToast({ title: '发布成功' })
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.publish {
  padding: 20rpx;
}
.content-input {
  width: 100%;
  height: 300rpx;
  border: 1rpx solid #eee;
  padding: 20rpx;
}
.publish-btn {
  margin-top: 40rpx;
  background: #667eea;
  color: #fff;
}
</style>