<template>
  <view class="app">
    <!-- 页面内容由 pages.json 中的页面路由决定 -->
  </view>
</template>

<script>
import pagesConfig from './pages.json'

// 文件服务器和网关地址（与 request.js 保持一致）
// Mac端真机调试需要HTTPS
const DEV_LAN_HOST = import.meta.env?.VITE_DEV_LAN_HOST || '192.168.1.144'
const MP_GATEWAY_ORIGIN = import.meta.env?.VITE_MP_GATEWAY_ORIGIN || `https://${DEV_LAN_HOST}:9443`
const MP_FILE_ORIGIN = import.meta.env?.VITE_MP_FILE_ORIGIN || `https://${DEV_LAN_HOST}:9447`
const IS_MP_WEIXIN = process.env.UNI_PLATFORM === 'mp-weixin'
const KNOWN_H5_ROUTES = (() => {
  const pageRoutes = (pagesConfig.pages || []).map((page) => `/${page.path}`)
  const subPackageRoutes = (pagesConfig.subPackages || []).flatMap((pkg) =>
    (pkg.pages || []).map((page) => `/${pkg.root}/${page.path}`)
  )
  return new Set(['/', ...pageRoutes, ...subPackageRoutes])
})()

const rewriteAppPayReturn = () => {
  if (typeof window === 'undefined' || IS_MP_WEIXIN) return
  const url = new URL(window.location.href)
  if (!url.pathname.startsWith('/app/pay-result')) return

  const target = new URL('/#/pages/order/pay', window.location.origin)
  const appScheme = new URL('yibenart://pay-result')
  url.searchParams.forEach((value, key) => {
    target.searchParams.set(key, value)
    appScheme.searchParams.set(key, value)
  })

  const ua = window.navigator?.userAgent || ''
  if (!/YibenArtIOSApp/i.test(ua)) {
    const openApp = () => {
      window.location.href = appScheme.toString()
    }
    document.body.innerHTML = `
      <div style="min-height:100vh;display:flex;align-items:center;justify-content:center;background:#0d0d0d;color:#fff;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;padding:28px;text-align:center;">
        <div style="max-width:360px;width:100%;">
          <div style="font-size:22px;font-weight:700;margin-bottom:12px;">支付结果确认中</div>
          <div style="font-size:15px;line-height:1.7;color:#bbb;margin-bottom:24px;">正在返回艺本艺术 APP 查看订单状态。如未自动返回，请点击下方按钮。</div>
          <button id="openYibenApp" style="width:100%;height:48px;border:0;border-radius:24px;background:#d8aa2f;color:#111;font-size:17px;font-weight:700;">返回艺本艺术 APP</button>
          <a href="${target.toString()}" style="display:block;margin-top:18px;color:#9f9f9f;font-size:14px;text-decoration:none;">继续在网页查看订单</a>
        </div>
      </div>
    `
    document.getElementById('openYibenApp')?.addEventListener('click', openApp)
    openApp()
    setTimeout(openApp, 800)
    setTimeout(() => {
      window.location.replace(target.toString())
    }, 3000)
    return
  }

  window.location.replace(target.toString())
}

const rewriteWechatOauthCallback = () => {
  if (typeof window === 'undefined' || IS_MP_WEIXIN) return
  const url = new URL(window.location.href)
  const oauth = url.searchParams.get('oauth')
  const code = url.searchParams.get('code')
  if (!code) return

  const targetPage = oauth === 'wechat_bind'
    ? '/#/pages/user-extra/pay-account/add'
    : '/#/pages/login/index'
  if (oauth !== 'wechat' && oauth !== 'wechat_bind') return

  const target = new URL(targetPage, window.location.origin)
  const redirect = url.searchParams.get('redirect')
  const state = url.searchParams.get('state')
  target.searchParams.set('code', code)
  if (oauth === 'wechat_bind') {
    target.searchParams.set('type', 'wechat')
    target.searchParams.set('wechatBind', '1')
    target.searchParams.set('setDefault', url.searchParams.get('setDefault') || '1')
  }
  if (redirect) {
    target.searchParams.set('redirect', redirect)
  }
  if (state) {
    target.searchParams.set('state', state)
  }
  window.location.replace(target.toString())
}

const rewriteUnknownH5Route = () => {
  if (typeof window === 'undefined' || IS_MP_WEIXIN) return

  const url = new URL(window.location.href)
  const hash = url.hash || ''
  const normalizedHash = hash.startsWith('#/') ? hash.slice(1) : ''

  if (!normalizedHash || normalizedHash === '/pages/common/not-found') return

  const pathname = normalizedHash.split('?')[0]
  if (KNOWN_H5_ROUTES.has(pathname)) return

  const target = new URL('/#/pages/common/not-found', url.origin)
  target.searchParams.set('from', normalizedHash)
  window.location.replace(target.toString())
}

export default {
  globalData: {
    // H5 走本地代理与相对路径，小程序走配置的 HTTPS 域名
    fileDomain: IS_MP_WEIXIN ? MP_FILE_ORIGIN : '',
    domain: IS_MP_WEIXIN ? MP_GATEWAY_ORIGIN : ''
  },
  onLaunch() {
    rewriteAppPayReturn()
    rewriteWechatOauthCallback()
    rewriteUnknownH5Route()
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
