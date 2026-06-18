<template>
  <view class="app">
    <!-- 页面内容由 pages.json 中的页面路由决定 -->
  </view>
</template>

<script>
// 文件服务器和网关地址（与 request.js 保持一致）
// Mac端真机调试需要HTTPS
const DEV_LAN_HOST = import.meta.env?.VITE_DEV_LAN_HOST || '192.168.1.144'
const MP_GATEWAY_ORIGIN = import.meta.env?.VITE_MP_GATEWAY_ORIGIN || `https://${DEV_LAN_HOST}:9443`
const MP_FILE_ORIGIN = import.meta.env?.VITE_MP_FILE_ORIGIN || `https://${DEV_LAN_HOST}:9447`
const IS_MP_WEIXIN = process.env.UNI_PLATFORM === 'mp-weixin'

export default {
  globalData: {
    // H5 走本地代理与相对路径，小程序走配置的 HTTPS 域名
    fileDomain: IS_MP_WEIXIN ? MP_FILE_ORIGIN : '',
    domain: IS_MP_WEIXIN ? MP_GATEWAY_ORIGIN : ''
  },
  onLaunch() {
    console.log('App Launch - 艺本艺术')
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
