<template>
  <view class="app">
    <!-- 页面内容由 pages.json 中的页面路由决定 -->
  </view>
</template>

<script>
// 文件服务器和网关地址（与 request.js 保持一致）
const DEV_LAN_HOST = import.meta.env?.VITE_DEV_LAN_HOST || '192.168.1.144'

export default {
  globalData: {
    fileDomain: `http://${DEV_LAN_HOST}:8087`,
    domain: `http://${DEV_LAN_HOST}:8080`
  },
  onLaunch() {
    console.log('App Launch - 拾艺局')
  },
  onShow() {
    console.log('App Show')
  },
  onHide() {
    console.log('App Hide')
  }
}
</script>

<style lang="scss">
@import '@/styles/common.scss';
@import '@/styles/iconfont.scss';

/* 全局样式 - 深色主题 */
page {
  background: #0D0D0D;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  color: #FFFFFF;
}

/* 重置样式 */
page {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

/* 移除按钮默认样式 */
button {
  background: none;
  border: none;
  outline: none;
  padding: 0;
  margin: 0;
}

button::after {
  border: none;
}

/* 图片显示 */
image {
  display: block;
}

/* 全局隐藏 icon-filter 伪元素文字 */
.icon-filter::before {
  content: '' !important;
}
</style>
