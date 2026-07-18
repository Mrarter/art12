<template>
  <view class="security-page">
    <view class="summary-card">
      <view>
        <text class="summary-label">账号安全等级</text>
        <view class="summary-level">
          <text>{{ security.securityLevel || '评估中' }}</text>
          <text class="score">{{ security.securityScore || 0 }}</text>
        </view>
      </view>
      <view class="score-ring">
        <text>{{ security.securityScore || 0 }}</text>
      </view>
    </view>

    <view class="status-grid">
      <view class="status-item" :class="{ active: security.phoneBound }">
        <text class="status-name">手机号</text>
        <text class="status-value">{{ security.phoneBound ? security.phoneMasked : '未绑定' }}</text>
      </view>
      <view class="status-item" :class="{ active: security.passwordSet }">
        <text class="status-name">登录密码</text>
        <text class="status-value">{{ security.passwordSet ? '已设置' : '未设置' }}</text>
      </view>
      <view class="status-item" :class="{ active: security.wechatBound }">
        <text class="status-name">微信</text>
        <text class="status-value">{{ security.wechatBound ? '已绑定' : '未绑定' }}</text>
      </view>
    </view>

    <view class="tips" v-if="security.tips && security.tips.length">
      <text class="tips-title">安全建议</text>
      <text class="tip" v-for="tip in security.tips" :key="tip">{{ tip }}</text>
    </view>

    <view class="tabs">
      <view class="tab" :class="{ active: activeTab === 'phone' }" @click="activeTab = 'phone'">手机号</view>
      <view class="tab" :class="{ active: activeTab === 'password' }" @click="activeTab = 'password'">登录密码</view>
    </view>

    <view class="panel" v-if="activeTab === 'phone'">
      <view class="panel-head">
        <text class="panel-title">{{ security.phoneBound ? '换绑手机号' : '绑定手机号' }}</text>
        <text class="panel-desc">验证码会发送到新手机号，验证通过后立即生效</text>
      </view>
      <view class="form-row">
        <input v-model="phoneForm.phone" type="number" maxlength="11" placeholder="请输入手机号" />
      </view>
      <view class="form-row code-row">
        <input v-model="phoneForm.code" type="number" maxlength="8" placeholder="短信验证码" />
        <button class="code-btn" :disabled="phoneCountdown > 0" @click="sendCode('bind_phone')">
          {{ phoneCountdown > 0 ? `${phoneCountdown}s` : '获取验证码' }}
        </button>
      </view>
      <button class="submit-btn" :disabled="savingPhone" @click="submitPhone">
        {{ savingPhone ? '提交中...' : '确认保存' }}
      </button>
    </view>

    <view class="panel" v-else>
      <view class="panel-head">
        <text class="panel-title">{{ security.passwordSet ? '修改登录密码' : '设置登录密码' }}</text>
        <text class="panel-desc">{{ passwordTip }}</text>
      </view>
      <view class="form-row" v-if="security.passwordSet">
        <input v-model="passwordForm.currentPassword" password placeholder="当前密码，可用验证码替代" />
      </view>
      <view class="form-row">
        <input v-model="passwordForm.newPassword" password maxlength="32" placeholder="新密码，6-32位" />
      </view>
      <view class="form-row">
        <input v-model="passwordForm.confirmPassword" password maxlength="32" placeholder="再次输入新密码" />
      </view>
      <view class="form-row code-row" v-if="security.phoneBound">
        <input v-model="passwordForm.code" type="number" maxlength="8" placeholder="手机号验证码" />
        <button class="code-btn" :disabled="passwordCountdown > 0" @click="sendCode('change_password')">
          {{ passwordCountdown > 0 ? `${passwordCountdown}s` : '获取验证码' }}
        </button>
      </view>
      <button class="submit-btn" :disabled="savingPassword" @click="submitPassword">
        {{ savingPassword ? '保存中...' : '保存密码' }}
      </button>
    </view>
  </view>
</template>

<script>
import {
  getAccountSecurity,
  sendSmsCode,
  updateSecurityPhone,
  updateSecurityPassword
} from '@/api/user.js'
import { useUserStore } from '@/store/modules/user.js'

export default {
  data() {
    return {
      activeTab: 'phone',
      security: {},
      phoneForm: {
        phone: '',
        code: ''
      },
      passwordForm: {
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
        code: ''
      },
      phoneCountdown: 0,
      passwordCountdown: 0,
      phoneTimer: null,
      passwordTimer: null,
      savingPhone: false,
      savingPassword: false
    }
  },
  computed: {
    userStore() {
      return useUserStore()
    },
    passwordTip() {
      if (!this.security.passwordSet) return '首次设置密码需要短信验证；未绑定手机号时可直接设置'
      return this.security.phoneBound ? '输入当前密码，或使用已绑定手机号验证码完成修改' : '请输入当前密码后修改'
    }
  },
  onLoad(options) {
    if (options?.tab === 'password') this.activeTab = 'password'
    if (options?.tab === 'phone') this.activeTab = 'phone'
    this.loadSecurity()
  },
  onUnload() {
    clearInterval(this.phoneTimer)
    clearInterval(this.passwordTimer)
  },
  methods: {
    async loadSecurity() {
      try {
        const [security] = await Promise.all([
          getAccountSecurity(),
          this.userStore.fetchUserInfo().catch(() => null)
        ])
        this.security = security
      } catch (e) {
        uni.showToast({ title: e.message || '账号安全信息加载失败', icon: 'none' })
      }
    },
    validatePhone(phone) {
      return /^1[3-9]\d{9}$/.test(phone)
    },
    startCountdown(kind) {
      const field = kind === 'bind_phone' ? 'phoneCountdown' : 'passwordCountdown'
      const timerField = kind === 'bind_phone' ? 'phoneTimer' : 'passwordTimer'
      clearInterval(this[timerField])
      this[field] = 60
      this[timerField] = setInterval(() => {
        this[field] -= 1
        if (this[field] <= 0) clearInterval(this[timerField])
      }, 1000)
    },
    async sendCode(type) {
      const phone = type === 'bind_phone' ? this.phoneForm.phone : this.userStore.userInfo?.phone
      if (!this.validatePhone(phone || '')) {
        uni.showToast({ title: type === 'bind_phone' ? '请输入正确手机号' : '请先绑定手机号', icon: 'none' })
        return
      }
      try {
        await sendSmsCode(phone, type)
        this.startCountdown(type)
        uni.showToast({ title: '验证码已发送', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e.message || '验证码发送失败', icon: 'none' })
      }
    },
    async submitPhone() {
      if (!this.validatePhone(this.phoneForm.phone)) {
        uni.showToast({ title: '请输入正确手机号', icon: 'none' })
        return
      }
      if (!this.phoneForm.code) {
        uni.showToast({ title: '请输入验证码', icon: 'none' })
        return
      }
      this.savingPhone = true
      try {
        await updateSecurityPhone(this.phoneForm)
        uni.showToast({ title: '手机号已更新', icon: 'success' })
        this.phoneForm = { phone: '', code: '' }
        await this.userStore.fetchUserInfo()
        await this.loadSecurity()
      } catch (e) {
        uni.showToast({ title: e.message || '保存失败', icon: 'none' })
      } finally {
        this.savingPhone = false
      }
    },
    async submitPassword() {
      if (!this.passwordForm.newPassword || this.passwordForm.newPassword.length < 6) {
        uni.showToast({ title: '密码至少6位', icon: 'none' })
        return
      }
      if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
        uni.showToast({ title: '两次密码不一致', icon: 'none' })
        return
      }
      this.savingPassword = true
      try {
        await updateSecurityPassword({
          currentPassword: this.passwordForm.currentPassword,
          newPassword: this.passwordForm.newPassword,
          code: this.passwordForm.code
        })
        uni.showToast({ title: '密码已更新', icon: 'success' })
        this.passwordForm = { currentPassword: '', newPassword: '', confirmPassword: '', code: '' }
        await this.loadSecurity()
      } catch (e) {
        uni.showToast({ title: e.message || '保存失败', icon: 'none' })
      } finally {
        this.savingPassword = false
      }
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
$gold: #c9a227;
$green: #58b982;
$red: #c96262;

.security-page {
  min-height: 100vh;
  padding: 24rpx;
  box-sizing: border-box;
  background: $bg;
  color: $text;
}

.summary-card,
.status-item,
.tips,
.tabs,
.panel {
  background: $panel;
  border: 1rpx solid $line;
  border-radius: 16rpx;
}

.summary-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  margin-bottom: 20rpx;
}

.summary-label,
.panel-desc,
.status-name,
.tip {
  color: $muted;
  font-size: 24rpx;
}

.summary-level {
  display: flex;
  align-items: baseline;
  gap: 18rpx;
  margin-top: 10rpx;
  font-size: 42rpx;
  font-weight: 700;
}

.score {
  color: $gold;
  font-size: 28rpx;
}

.score-ring {
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
  border: 8rpx solid rgba($gold, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  color: $gold;
  font-size: 34rpx;
  font-weight: 700;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  margin-bottom: 20rpx;
}

.status-item {
  padding: 22rpx 16rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;

  &.active .status-value {
    color: $green;
  }
}

.status-value {
  font-size: 24rpx;
  color: $red;
  word-break: break-all;
}

.tips {
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.tips-title {
  display: block;
  font-size: 27rpx;
  font-weight: 700;
  margin-bottom: 14rpx;
}

.tip {
  display: block;
  line-height: 38rpx;
}

.tabs {
  display: flex;
  padding: 8rpx;
  margin-bottom: 20rpx;
}

.tab {
  flex: 1;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $muted;
  border-radius: 12rpx;
  font-size: 27rpx;

  &.active {
    background: rgba($gold, 0.16);
    color: $gold;
    font-weight: 700;
  }
}

.panel {
  padding: 28rpx;
}

.panel-head {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  margin-bottom: 24rpx;
}

.panel-title {
  font-size: 32rpx;
  font-weight: 700;
}

.form-row {
  min-height: 92rpx;
  margin-bottom: 18rpx;
  padding: 0 22rpx;
  background: $panel2;
  border-radius: 12rpx;
  border: 1rpx solid $line;
  display: flex;
  align-items: center;

  input {
    flex: 1;
    color: $text;
    font-size: 28rpx;
  }
}

.code-row {
  gap: 16rpx;
}

.code-btn {
  min-width: 176rpx;
  height: 64rpx;
  line-height: 64rpx;
  padding: 0 18rpx;
  border-radius: 10rpx;
  background: rgba($gold, 0.16);
  color: $gold;
  font-size: 24rpx;

  &[disabled] {
    color: $muted;
    background: rgba(255, 255, 255, 0.06);
  }
}

.submit-btn {
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 12rpx;
  background: $gold;
  color: #16130b;
  font-size: 30rpx;
  font-weight: 700;

  &[disabled] {
    opacity: 0.65;
  }
}
</style>
