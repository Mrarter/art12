<template>
  <view class="settings-page">
    <view class="page-glow"></view>

    <view class="account-card" @click="goProfile">
      <image class="avatar" :src="userInfo.avatar || '/static/images/avatar.png'" mode="aspectFill"></image>
      <view class="account-main">
        <text class="account-name">{{ userInfo.nickname || '未登录' }}</text>
        <text class="account-meta">{{ isLoggedIn ? `UID ${displayUid}` : '登录后管理账号与偏好' }}</text>
      </view>
      <view class="account-action">{{ isLoggedIn ? '编辑' : '登录' }}</view>
    </view>

    <view class="section" v-for="section in settingSections" :key="section.title">
      <view class="section-title">{{ section.title }}</view>
      <view class="menu-list">
        <view
          class="menu-row"
          v-for="item in section.items"
          :key="item.label"
          @click="handleMenu(item)"
        >
          <view class="menu-left">
            <view class="menu-icon" :class="item.tone">{{ item.icon }}</view>
            <view class="menu-copy">
              <text class="menu-label">{{ item.label }}</text>
              <text class="menu-desc" v-if="item.desc">{{ item.desc }}</text>
            </view>
          </view>

          <switch
            v-if="item.switchKey"
            :checked="settings[item.switchKey]"
            @change.stop="item.onChange"
            color="#c9a227"
          ></switch>
          <view class="menu-value" v-else>
            <text :class="{ certified: item.certified }">{{ item.value }}</text>
            <text class="chevron" v-if="item.path || item.action">›</text>
          </view>
        </view>
      </view>
    </view>

    <view class="logout-btn" :class="{ login: !isLoggedIn }" @click="isLoggedIn ? handleLogout() : goLogin()">
      {{ isLoggedIn ? '退出登录' : '登录 / 注册' }}
    </view>

    <view class="about-mask" v-if="showAboutPopup" @click="showAboutPopup = false">
      <view class="about-sheet" @click.stop>
        <text class="about-name">拾艺局</text>
        <text class="about-desc">连接艺术家、收藏者与艺荐官的艺术品交易平台</text>
        <view class="about-links">
          <text @click="goPage('/pages/user/agreement?type=user')">用户协议</text>
          <text @click="goPage('/pages/user/agreement?type=privacy')">隐私政策</text>
        </view>
        <view class="about-close" @click="showAboutPopup = false">知道了</view>
      </view>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '@/store/modules/user.js'

export default {
  data() {
    return {
      version: '1.0.0',
      cacheSize: '0 KB',
      showAboutPopup: false,
      localRealNameStatus: null,
      settings: {
        pushEnabled: true,
        soundEnabled: true,
        wifiOnly: false
      }
    }
  },

  computed: {
    userStore() {
      return useUserStore()
    },
    userInfo() {
      return this.userStore.userInfo || {}
    },
    isLoggedIn() {
      return this.userStore.isAuthenticated || this.userStore.isLogin
    },
    displayUid() {
      return this.userInfo.uid || this.userInfo.id || '------'
    },
    effectiveRealNameStatus() {
      if (this.userInfo.realNameStatus === 1) return 1
      return this.localRealNameStatus ?? this.userInfo.realNameStatus
    },
    settingSections() {
      return [
        {
          title: '账号安全',
          items: [
            {
              label: '手机号绑定',
              desc: '用于登录验证和订单通知',
              icon: '机',
              tone: 'gold',
              value: this.userInfo.phone ? this.formatPhone(this.userInfo.phone) : '未绑定',
              path: this.comingSoon('绑定手机号', '手机号绑定页正在开发中，后续会补充验证码和换绑流程。')
            },
            {
              label: '实名认证',
              desc: '提现、发票等场景需要认证',
              icon: '认',
              tone: 'green',
              value: this.getRealNameStatus(this.effectiveRealNameStatus),
              certified: this.effectiveRealNameStatus === 1,
              path: '/pages/user/realname'
            },
            {
              label: '登录密码',
              desc: '管理密码和找回方式',
              icon: '密',
              tone: 'blue',
              value: '修改',
              path: this.comingSoon('密码管理', '密码设置页正在开发中，后续会补充修改与找回能力。')
            }
          ]
        },
        {
          title: '消息与偏好',
          items: [
            { label: '消息推送', desc: '订单、竞拍、互动提醒', icon: '推', tone: 'gold', switchKey: 'pushEnabled', onChange: this.togglePush },
            { label: '声音提醒', desc: '新消息声音提示', icon: '声', tone: 'blue', switchKey: 'soundEnabled', onChange: this.toggleSound },
            { label: '仅 WiFi 加载图片', desc: '减少移动网络流量消耗', icon: '图', tone: 'green', switchKey: 'wifiOnly', onChange: this.toggleWifiOnly },
            { label: '通知设置', desc: '细分消息类型', icon: '通', tone: 'purple', value: '配置', path: '/pages/setting/notification' }
          ]
        },
        {
          title: '通用',
          items: [
            { label: '清理缓存', desc: '搜索、浏览与临时数据', icon: '清', tone: 'red', value: this.cacheSize, action: this.clearCache },
            { label: '检查更新', desc: '当前版本 v' + this.version, icon: '版', tone: 'gold', value: '检查', action: this.checkUpdate }
          ]
        },
        {
          title: '关于与支持',
          items: [
            { label: '用户协议', icon: '协', tone: 'blue', value: '', path: '/pages/user/agreement?type=user' },
            { label: '隐私政策', icon: '隐', tone: 'green', value: '', path: '/pages/user/agreement?type=privacy' },
            { label: '意见反馈', icon: '馈', tone: 'purple', value: '', path: '/pages/user/feedback' },
            { label: '关于我们', icon: '关', tone: 'gold', value: '', action: this.showAbout }
          ]
        }
      ]
    }
  },

  onLoad() {
    this.calculateCacheSize()
    const saved = uni.getStorageSync('settings')
    if (saved) this.settings = { ...this.settings, ...saved }
  },

  onShow() {
    this.fetchRealNameStatus()
    if (this.isLoggedIn) this.userStore.fetchUserInfo()
  },

  methods: {
    async fetchRealNameStatus() {
      try {
        const { getRealnameCertStatus } = await import('@/api/user.js')
        const data = await getRealnameCertStatus()
        this.localRealNameStatus = data?.status ?? null
      } catch (err) {
        // fallback to localStorage
        const saved = uni.getStorageSync('realname_certification')
        this.localRealNameStatus = saved?.status ?? null
      }
    },
    comingSoon(title, desc) {
      return `/pages/common/coming-soon?title=${encodeURIComponent(title)}&desc=${encodeURIComponent(desc)}`
    },
    handleMenu(item) {
      if (item.switchKey) return
      if (item.action) {
        item.action()
        return
      }
      if (item.path) this.goPage(item.path)
    },
    formatPhone(phone) {
      if (!phone) return ''
      return String(phone).replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
    },
    getRealNameStatus(status) {
      const map = { 0: '未认证', 1: '已认证', 2: '审核中' }
      return map[status] || '未认证'
    },
    goProfile() {
      if (!this.isLoggedIn) {
        this.goLogin()
        return
      }
      uni.navigateTo({ url: '/pages/user/profile' })
    },
    goLogin() {
      uni.navigateTo({ url: '/pages/login/index' })
    },
    goPage(url) {
      if (!this.isLoggedIn && !url.includes('/pages/user/agreement') && !url.includes('/pages/user/feedback')) {
        this.goLogin()
        return
      }
      uni.navigateTo({ url })
    },
    persistSettings() {
      uni.setStorageSync('settings', this.settings)
    },
    togglePush(e) {
      this.settings.pushEnabled = e.detail.value
      this.persistSettings()
      uni.showToast({ title: this.settings.pushEnabled ? '已开启推送' : '已关闭推送', icon: 'none' })
    },
    toggleSound(e) {
      this.settings.soundEnabled = e.detail.value
      this.persistSettings()
    },
    toggleWifiOnly(e) {
      this.settings.wifiOnly = e.detail.value
      this.persistSettings()
    },
    clearCache() {
      uni.showModal({
        title: '清理缓存',
        content: '确定要清理搜索、浏览和临时缓存吗？',
        success: (res) => {
          if (!res.confirm) return
          ;['searchHistory', 'browseHistory', 'cacheData'].forEach(key => uni.removeStorageSync(key))
          this.cacheSize = '0 KB'
          uni.showToast({ title: '清理成功', icon: 'success' })
        }
      })
    },
    calculateCacheSize() {
      try {
        const info = uni.getStorageInfoSync()
        const size = info.currentSize || 0
        this.cacheSize = size < 1024 ? `${size} KB` : `${(size / 1024).toFixed(2)} MB`
      } catch (e) {
        this.cacheSize = '未知'
      }
    },
    checkUpdate() {
      uni.showLoading({ title: '检查中...' })
      setTimeout(() => {
        uni.hideLoading()
        uni.showToast({ title: '已是最新版本', icon: 'success' })
      }, 800)
    },
    showAbout() {
      this.showAboutPopup = true
    },
    handleLogout() {
      uni.showModal({
        title: '退出登录',
        content: '确定要退出当前账号吗？',
        success: (res) => {
          if (!res.confirm) return
          this.userStore.logout()
          uni.showToast({ title: '已退出登录', icon: 'success' })
          setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 800)
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
$bg: #0b0b0c;
$panel: #171719;
$panel2: #202024;
$line: rgba(255, 255, 255, 0.08);
$text: #f6f2e8;
$muted: #9b958a;
$dim: #68645c;
$gold: #c9a227;
$green: #58b982;
$blue: #5f8fc7;
$red: #c96262;
$purple: #8c73c9;

.settings-page {
  min-height: 100vh;
  background: $bg;
  color: $text;
  padding: 24rpx;
  box-sizing: border-box;
  position: relative;
}

.page-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 320rpx;
  background: linear-gradient(180deg, rgba($gold, 0.16), transparent);
  pointer-events: none;
}

.account-card,
.section,
.logout-btn {
  position: relative;
  z-index: 1;
  background: $panel;
  border: 1rpx solid $line;
  border-radius: 16rpx;
}

.account-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 26rpx;
  margin-bottom: 20rpx;
}

.avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  border: 2rpx solid rgba($gold, 0.35);
  background: $panel2;
}

.account-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.account-name {
  font-size: 32rpx;
  font-weight: 700;
}

.account-meta,
.menu-desc {
  font-size: 22rpx;
  color: $muted;
}

.account-action {
  padding: 10rpx 18rpx;
  border-radius: 8rpx;
  background: rgba($gold, 0.16);
  color: $gold;
  font-size: 24rpx;
  font-weight: 600;
}

.section {
  overflow: hidden;
  margin-bottom: 20rpx;
}

.section-title {
  padding: 24rpx;
  font-size: 28rpx;
  font-weight: 700;
  border-bottom: 1rpx solid $line;
}

.menu-list {
  padding: 0 24rpx;
}

.menu-row {
  min-height: 104rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1rpx solid $line;

  &:last-child {
    border-bottom: none;
  }
}

.menu-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
  flex: 1;
  min-width: 0;
}

.menu-icon {
  width: 54rpx;
  height: 54rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $gold;
  background: rgba($gold, 0.15);
  font-size: 22rpx;
  font-weight: 700;
  flex-shrink: 0;

  &.green { color: $green; background: rgba($green, 0.15); }
  &.blue { color: $blue; background: rgba($blue, 0.15); }
  &.red { color: $red; background: rgba($red, 0.15); }
  &.purple { color: $purple; background: rgba($purple, 0.15); }
}

.menu-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.menu-label {
  font-size: 27rpx;
  color: $text;
  font-weight: 600;
}

.menu-value {
  display: flex;
  align-items: center;
  gap: 10rpx;
  color: $muted;
  font-size: 24rpx;
  flex-shrink: 0;

  .certified {
    color: $green;
  }
}

.chevron {
  color: $dim;
  font-size: 32rpx;
}

.logout-btn {
  padding: 28rpx;
  text-align: center;
  color: $red;
  font-size: 30rpx;
  font-weight: 700;

  &.login {
    color: $gold;
  }
}

.about-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  background: rgba(0, 0, 0, 0.58);
  display: flex;
  align-items: flex-end;
}

.about-sheet {
  width: 100%;
  padding: 40rpx 32rpx calc(40rpx + env(safe-area-inset-bottom));
  background: $panel;
  border-radius: 24rpx 24rpx 0 0;
  border-top: 1rpx solid $line;
  box-sizing: border-box;
}

.about-name {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  margin-bottom: 12rpx;
}

.about-desc {
  display: block;
  font-size: 25rpx;
  line-height: 38rpx;
  color: $muted;
}

.about-links {
  display: flex;
  gap: 28rpx;
  margin: 32rpx 0;
  color: $gold;
  font-size: 26rpx;
}

.about-close {
  height: 84rpx;
  border-radius: 12rpx;
  background: $gold;
  color: #16130b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 700;
}
</style>
