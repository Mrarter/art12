<template>
  <view class="video-page">
    <video 
      class="video-player" 
      :src="videoUrl" 
      controls 
      autoplay
      enable-danmu
      :danmu-list="danmuList"
    ></video>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const videoUrl = ref('')
const danmuList = ref([])

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const url = currentPage.options?.url
  if (url) {
    videoUrl.value = decodeURIComponent(url)
  }
})
</script>

<style lang="scss" scoped>
.video-page {
  min-height: 100vh;
  background-color: #000000;
  
  .video-player {
    width: 100%;
    height: 100vh;
  }
}
</style>
