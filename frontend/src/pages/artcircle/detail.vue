<template>
  <view class="post-detail">
    <view class="post-header">
      <image :src="post.avatar" class="avatar" />
      <view class="user-info">
        <text class="nickname">{{ post.nickname }}</text>
        <text class="time">{{ post.createTime }}</text>
      </view>
    </view>
    <view class="content">
      <text>{{ post.content }}</text>
    </view>
    <view class="images" v-if="post.images">
      <image v-for="(img, i) in post.images" :key="i" :src="img" mode="widthFix" />
    </view>
    <view class="actions">
      <text @click="like">赞 {{ post.likeCount }}</text>
      <text @click="comment">评论 {{ post.commentCount }}</text>
    </view>
  </view>
</template>

<script setup>
const post = ref({
  avatar: '',
  nickname: '',
  content: '',
  images: [],
  likeCount: 0,
  commentCount: 0,
  createTime: ''
})
const like = () => {}
const comment = () => {}
onLoad(() => {})
</script>

<style lang="scss" scoped>
.post-detail {
  padding: 20rpx;
}
</style>