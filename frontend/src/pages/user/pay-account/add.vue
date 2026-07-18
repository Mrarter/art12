<template>
  <view class="page">
    <view class="hero-card">
      <text class="hero-title">添加收款账户</text>
      <text class="hero-desc">用于提现分成、转售收益和平台结算。收款人姓名需与实名认证一致。</text>
    </view>

    <view class="section-title">选择账户类型</view>
    <view class="type-list">
      <view
        v-for="item in accountTypes"
        :key="item.type"
        class="type-card"
        :class="{ active: selectedType === item.type }"
        @click="selectType(item.type)"
      >
        <view class="type-icon" :class="item.tone">
          <text class="icon-char">{{ item.icon }}</text>
        </view>
        <view class="type-info">
          <text class="type-name">{{ item.name }}</text>
          <text class="type-desc">{{ item.desc }}</text>
        </view>
        <text class="type-badge">{{ selectedType === item.type ? '已选择' : '选择' }}</text>
      </view>
    </view>

    <view v-if="selectedType === 'alipay'" class="form-card">
      <view class="form-item">
        <text class="label">真实姓名</text>
        <input
          class="input"
          v-model="alipayForm.realName"
          placeholder="请输入支付宝实名姓名"
          placeholder-class="ph"
          maxlength="20"
        />
      </view>
      <view class="form-item">
        <text class="label">支付宝账号</text>
        <input
          class="input"
          v-model="alipayForm.alipayAccount"
          placeholder="手机号 / 邮箱"
          placeholder-class="ph"
          maxlength="80"
        />
      </view>
      <view class="form-item">
        <text class="label">预留手机号</text>
        <input
          class="input"
          v-model="alipayForm.phone"
          placeholder="选填，用于后续核验"
          placeholder-class="ph"
          maxlength="11"
          type="number"
        />
      </view>
      <view class="default-row" @click="alipayForm.setDefault = !alipayForm.setDefault">
        <view>
          <text class="default-title">设为默认收款账户</text>
          <text class="default-desc">提现时优先使用该支付宝账户</text>
        </view>
        <switch :checked="alipayForm.setDefault" color="#d4af37" @change="onAlipayDefaultChange" />
      </view>
    </view>

    <view v-if="selectedType === 'wechat'" class="wechat-card">
      <view class="wechat-icon">微</view>
      <text class="wechat-title">微信授权绑定</text>
      <text class="wechat-desc">点击后会跳转微信授权，授权成功后自动绑定到当前登录账号，不会切换登录账号。</text>
      <view class="default-row compact" @click="wechatSetDefault = !wechatSetDefault">
        <view>
          <text class="default-title">设为默认收款账户</text>
          <text class="default-desc">提现时优先使用微信收款</text>
        </view>
        <switch :checked="wechatSetDefault" color="#d4af37" @change="onWechatDefaultChange" />
      </view>
    </view>

    <view class="notice-card">
      <text class="notice-title">账户安全说明</text>
      <text class="notice-line">• 支付宝和银行卡会保存到后台收款账户，用于真实提现结算。</text>
      <text class="notice-line">• 微信绑定会先打开微信授权页，授权成功后回到本页自动完成绑定。</text>
      <text class="notice-line">• 如果已实名认证，收款人姓名必须与实名认证姓名一致。</text>
    </view>

    <view class="bottom-bar">
      <button class="submit-btn" :class="{ disabled: !canSubmit || submitting }" :disabled="!canSubmit || submitting" @click="handleSubmit">
        {{ submitText }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { addPayAccount, bindWechatPayAccountByCode } from '@/api/pay'
import { hasNativeWechatLoginBridge, requestNativeWechatLogin } from '@/utils/native'

const IS_MP_WEIXIN = process.env.UNI_PLATFORM === 'mp-weixin'
const H5_WECHAT_OFFICIAL_APP_ID = import.meta.env?.VITE_WECHAT_OFFICIAL_APP_ID || 'wx02fc79a8dd2d9f20'

const accountTypes = [
  { type: 'alipay', name: '支付宝', desc: '绑定支付宝账号收款', icon: '支', tone: 'alipay' },
  { type: 'wechat', name: '微信', desc: '绑定当前微信授权身份', icon: '微', tone: 'wechat' },
  { type: 'bank', name: '银行卡', desc: '支持各大银行储蓄卡', icon: '卡', tone: 'bank' }
]

const selectedType = ref('alipay')
const submitting = ref(false)
const wechatSetDefault = ref(true)
const oauthHandled = ref(false)
const isNativeWechatBridgeReady = ref(false)
const isIOSAppShell = ref(false)

const alipayForm = reactive({
  realName: '',
  alipayAccount: '',
  phone: '',
  setDefault: true
})

const selectType = (type) => {
  if (type === 'bank') {
    uni.navigateTo({ url: '/pages/user-extra/pay-account/bank-card' })
    return
  }
  selectedType.value = type
}

const isPhone = (value) => /^1[3-9]\d{9}$/.test(value)

const canSubmit = computed(() => {
  if (selectedType.value === 'wechat') return true
  if (selectedType.value !== 'alipay') return false
  const account = alipayForm.alipayAccount.trim()
  const phone = alipayForm.phone.trim()
  return alipayForm.realName.trim().length >= 2
    && account.length >= 5
    && (!phone || isPhone(phone))
})

const submitText = computed(() => {
  if (submitting.value) return '提交中...'
  if (selectedType.value === 'wechat') return '开始微信授权绑定'
  return '确认绑定支付宝'
})

const onAlipayDefaultChange = (event) => {
  alipayForm.setDefault = event.detail.value
}

const onWechatDefaultChange = (event) => {
  wechatSetDefault.value = event.detail.value
}

const showRealnameRequired = (message) => {
  uni.showModal({
    title: '需要实名认证',
    content: message,
    confirmText: '去认证',
    success: (res) => {
      if (res.confirm) uni.navigateTo({ url: '/pages/user-extra/realname' })
    }
  })
}

const goBindResult = (type) => {
  uni.redirectTo({
    url: `/pages/user-extra/pay-account/result?type=${encodeURIComponent(type)}`
  })
}

const handleSubmit = async () => {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    if (selectedType.value === 'wechat') {
      await startWechatBind()
      return
    }

    await addPayAccount({
      accountType: 2,
      realName: alipayForm.realName.trim(),
      alipayAccount: alipayForm.alipayAccount.trim(),
      phone: alipayForm.phone.trim(),
      setDefault: alipayForm.setDefault
    })
    goBindResult('alipay')
  } catch (e) {
    const message = e.message || '绑定失败'
    if (message.includes('实名认证')) {
      showRealnameRequired(message)
    } else {
      uni.showToast({ title: message, icon: 'none' })
    }
  } finally {
    submitting.value = false
  }
}

const startWechatBind = async () => {
  if (IS_MP_WEIXIN) {
    const loginRes = await resolveMiniWechatCode()
    await finishWechatBind(loginRes.code, 'mini')
    return
  }

  if (isH5Wechat()) {
    startOfficialWechatOauth()
    return
  }

  if (isNativeWechatBridgeReady.value) {
    const { code, loginScene } = await requestNativeWechatLogin({ source: 'pay-account-bind' })
    await finishWechatBind(code, loginScene || 'app')
    return
  }

  if (isIOSAppShell.value && !isNativeWechatBridgeReady.value) {
    uni.showModal({
      title: 'APP 暂未接入原生微信绑定',
      content: '当前 APP 容器不能直接打开公众号授权页完成绑定，请先在微信内打开 H5 页面绑定，或等我们接入原生微信 SDK。',
      showCancel: false,
      confirmText: '知道了'
    })
    submitting.value = false
    return
  }

  uni.showModal({
    title: '请在微信中打开',
    content: '微信绑定需要微信授权，请复制链接到微信内打开后重试。',
    showCancel: false,
    confirmText: '知道了'
  })
  submitting.value = false
}

const resolveMiniWechatCode = () => {
  return new Promise((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: (res) => {
        if (res?.code) {
          resolve(res)
        } else {
          reject(new Error('获取微信授权码失败'))
        }
      },
      fail: (err) => reject(err || new Error('微信授权不可用'))
    })
  })
}

const isH5Wechat = () => {
  if (typeof window === 'undefined') return false
  const ua = window.navigator?.userAgent || ''
  return /MicroMessenger/i.test(ua) && !/miniProgram/i.test(ua)
}

const startOfficialWechatOauth = () => {
  if (typeof window === 'undefined') {
    submitting.value = false
    return
  }
  const callbackUrl = new URL('/', window.location.origin)
  callbackUrl.searchParams.set('oauth', 'wechat_bind')
  callbackUrl.searchParams.set('setDefault', wechatSetDefault.value ? '1' : '0')
  const authUrl = `https://open.weixin.qq.com/connect/oauth2/authorize?appid=${encodeURIComponent(H5_WECHAT_OFFICIAL_APP_ID)}&redirect_uri=${encodeURIComponent(callbackUrl.toString())}&response_type=code&scope=snsapi_base&state=shiyiju_wechat_bind#wechat_redirect`
  window.location.replace(authUrl)
}

const finishWechatBind = async (code, loginScene = 'h5') => {
  if (!code) throw new Error('缺少微信授权码')
  await bindWechatPayAccountByCode({
    code,
    loginScene,
    setDefault: wechatSetDefault.value
  })
  goBindResult('wechat')
}

const readRouteOptions = () => {
  if (typeof window === 'undefined') return {}
  const url = new URL(window.location.href)
  const searchEntries = Array.from(url.searchParams.entries())
  const hashQuery = window.location.hash.split('?')[1] || ''
  const hashEntries = Array.from(new URLSearchParams(hashQuery).entries())
  return Object.fromEntries([...searchEntries, ...hashEntries])
}

onLoad((options = {}) => {
  if (typeof window !== 'undefined') {
    const ua = window.navigator?.userAgent || ''
    isIOSAppShell.value = /YibenArt/i.test(ua)
    isNativeWechatBridgeReady.value = hasNativeWechatLoginBridge()
  }
  const query = readRouteOptions()
  const mergedOptions = { ...query, ...options }
  const type = String(mergedOptions.type || '').toLowerCase()
  if (!type) return
  if (type === 'bank' || type === 'bankcard') {
    setTimeout(() => uni.navigateTo({ url: '/pages/user-extra/pay-account/bank-card' }), 200)
    return
  }
  if (type === 'wechat' || type === 'alipay') {
    selectedType.value = type
  }
  if (selectedType.value === 'wechat') {
    wechatSetDefault.value = String(mergedOptions.setDefault || '1') !== '0'
  }
  const oauthCode = mergedOptions.code || ''
  if (selectedType.value === 'wechat' && mergedOptions.wechatBind && oauthCode && !oauthHandled.value) {
    oauthHandled.value = true
    submitting.value = true
    finishWechatBind(oauthCode, 'h5')
      .catch((e) => {
        oauthHandled.value = false
        uni.showToast({ title: e.message || '微信绑定失败', icon: 'none' })
      })
      .finally(() => {
        submitting.value = false
      })
  }
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #0d0d0d;
  padding: 24rpx 24rpx 160rpx;
  box-sizing: border-box;
}

.hero-card,
.form-card,
.wechat-card,
.notice-card {
  background: #1a1a1a;
  border-radius: 24rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
}

.hero-card {
  padding: 30rpx;
  margin-bottom: 30rpx;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.14), #191919 70%);
}

.hero-title {
  color: #f5f5f5;
  font-size: 36rpx;
  font-weight: 800;
  display: block;
  margin-bottom: 10rpx;
}

.hero-desc {
  color: #aaa;
  font-size: 25rpx;
  line-height: 38rpx;
}

.section-title {
  color: #f5f5f5;
  font-size: 30rpx;
  font-weight: 800;
  margin: 0 0 20rpx 8rpx;
}

.type-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-bottom: 24rpx;
}

.type-card {
  background: #1a1a1a;
  border-radius: 20rpx;
  padding: 26rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
}

.type-card.active {
  border-color: rgba(212, 175, 55, 0.55);
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.08), #1a1a1a 70%);
}

.type-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.type-icon.bank { background: rgba(212, 175, 55, 0.14); color: #d4af37; }
.type-icon.alipay { background: rgba(22, 119, 255, 0.14); color: #1677ff; }
.type-icon.wechat { background: rgba(7, 193, 96, 0.14); color: #07c160; }

.icon-char {
  font-size: 34rpx;
  font-weight: 800;
}

.type-info {
  flex: 1;
  min-width: 0;
}

.type-name {
  color: #f5f5f5;
  font-size: 29rpx;
  font-weight: 800;
  display: block;
  margin-bottom: 6rpx;
}

.type-desc {
  color: #888;
  font-size: 24rpx;
}

.type-badge {
  color: #d4af37;
  background: rgba(212, 175, 55, 0.12);
  border-radius: 999rpx;
  padding: 8rpx 16rpx;
  font-size: 22rpx;
  flex-shrink: 0;
}

.form-card,
.wechat-card,
.notice-card {
  padding: 28rpx;
  margin-top: 24rpx;
}

.form-item {
  margin-bottom: 28rpx;
}

.label,
.default-title,
.notice-title,
.wechat-title {
  color: #f5f5f5;
  font-size: 26rpx;
  font-weight: 800;
  display: block;
}

.label {
  margin-bottom: 12rpx;
}

.input {
  height: 88rpx;
  padding: 0 20rpx;
  border-radius: 14rpx;
  background: #242424;
  color: #f5f5f5;
  font-size: 28rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
  box-sizing: border-box;
}

.ph {
  color: #666;
}

.default-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding-top: 6rpx;
}

.default-row.compact {
  width: 100%;
  margin-top: 26rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.default-desc,
.wechat-desc,
.notice-line {
  color: #888;
  font-size: 24rpx;
  line-height: 36rpx;
  display: block;
  margin-top: 6rpx;
}

.wechat-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.wechat-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 24rpx;
  background: rgba(7, 193, 96, 0.14);
  color: #07c160;
  font-size: 40rpx;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18rpx;
}

.notice-title {
  color: #d4af37;
  margin-bottom: 12rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(13, 13, 13, 0.96);
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.submit-btn {
  height: 88rpx;
  border-radius: 999rpx;
  background: #d4af37;
  color: #16130b;
  font-size: 30rpx;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  width: 100%;
}

.submit-btn.disabled {
  background: #3a3528;
  color: #7c7464;
}
</style>
