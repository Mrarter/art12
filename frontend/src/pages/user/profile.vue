<template>
  <view class="profile-page">
    <view v-if="!isLoggedIn" class="login-panel">
      <image class="login-avatar" src="/static/images/avatar.png" mode="aspectFill"></image>
      <text class="login-title">登录后编辑个人资料</text>
      <text class="login-desc">完善头像、昵称和简介，让收藏家、艺术家与艺荐官身份都更清晰。</text>
      <view class="login-btn" @click="goPage('/pages/login/index')">去登录</view>
    </view>

    <template v-else>
      <view class="hero-card">
        <view class="avatar-wrap" @click="changeAvatar">
          <image class="avatar" :src="draft.avatar || '/static/images/avatar.png'" mode="aspectFill"></image>
          <view class="avatar-action">换</view>
        </view>
        <view class="hero-main">
          <input
            class="nickname-input"
            v-model.trim="draft.nickname"
            maxlength="24"
            placeholder="输入昵称"
            placeholder-class="placeholder"
          />
          <text class="uid-line">UID {{ userInfo.uid || userInfo.id || '-' }}</text>
          <view class="identity-tags">
            <text
              class="identity-tag"
              v-for="item in identityTags"
              :key="item.key"
              :class="item.key"
            >
              {{ item.label }}
            </text>
          </view>
        </view>
      </view>

      <view class="completion-card">
        <view class="completion-head">
          <view>
            <text class="section-kicker">资料完整度</text>
            <text class="completion-title">{{ completionText }}</text>
          </view>
          <text class="completion-percent">{{ completionPercent }}%</text>
        </view>
        <view class="progress-track">
          <view class="progress-bar" :style="{ width: completionPercent + '%' }"></view>
        </view>
        <view class="missing-list" v-if="missingFields.length">
          <text class="missing-item" v-for="item in missingFields" :key="item">{{ item }}</text>
        </view>
      </view>

      <view class="form-section">
        <view class="section-head">
          <text class="section-title">基本资料</text>
          <text class="section-note">对外展示</text>
        </view>

        <view class="field-block">
          <text class="field-label">个人简介</text>
          <textarea
            class="textarea"
            v-model.trim="draft.bio"
            maxlength="120"
            placeholder="写一句你和艺术、收藏或创作有关的介绍"
            placeholder-class="placeholder"
          />
          <text class="count">{{ (draft.bio || '').length }}/120</text>
        </view>

        <view class="field-row">
          <text class="field-label">性别</text>
          <picker mode="selector" :range="genderOptions" :value="draft.gender" @change="onGenderChange">
            <view class="picker-value">{{ genderOptions[draft.gender] || '未设置' }}</view>
          </picker>
        </view>

        <view class="field-row">
          <text class="field-label">生日</text>
          <picker mode="date" :value="draft.birthday" @change="onBirthdayChange">
            <view class="picker-value">{{ draft.birthday || '选择日期' }}</view>
          </picker>
        </view>

        <view class="field-row">
          <text class="field-label">所在地区</text>
          <input
            class="row-input"
            v-model.trim="draft.region"
            maxlength="32"
            placeholder="城市 / 地区"
            placeholder-class="placeholder"
          />
        </view>
      </view>

      <view class="form-section">
        <view class="section-head">
          <text class="section-title">联系信息</text>
          <text class="section-note">仅用于账号与服务</text>
        </view>

        <view class="field-row readonly">
          <text class="field-label">手机号</text>
          <text class="readonly-value">{{ formatPhone(userInfo.phone) }}</text>
        </view>

        <view class="field-row readonly">
          <text class="field-label">邮箱</text>
          <text class="readonly-value">{{ userInfo.email || '暂未开放' }}</text>
        </view>

        <view class="field-row readonly">
          <text class="field-label">微信号</text>
          <text class="readonly-value">{{ userInfo.wechat || '暂未开放' }}</text>
        </view>
      </view>

      <view class="form-section">
        <view class="section-head">
          <text class="section-title">身份资料</text>
          <text class="section-note">按身份开放能力</text>
        </view>

        <view class="identity-row" v-for="item in identityCards" :key="item.key">
          <view class="identity-icon" :class="item.key">{{ item.short }}</view>
          <view class="identity-main">
            <text class="identity-title">{{ item.title }}</text>
            <text class="identity-desc">{{ item.desc }}</text>
          </view>
          <view class="identity-action" v-if="item.path" @click="goPage(item.path)">{{ item.action }}</view>
          <text class="identity-status" v-else>{{ item.status }}</text>
        </view>
      </view>

      <view class="safe-tip">
        <text>敏感资料需要验证后修改；昵称、头像、简介和地区会同步到个人主页。</text>
      </view>

      <view class="save-bar">
        <view class="save-secondary" @click="resetDraft" :class="{ disabled: !isDirty || saving }">还原</view>
        <view class="save-primary" @click="saveProfile" :class="{ disabled: !isDirty || saving }">
          {{ saving ? '保存中...' : '保存修改' }}
        </view>
      </view>
    </template>
  </view>
</template>

<script>
import { useUserStore } from '@/store/modules/user.js'
import { openCropper } from '@/api/file.js'
import { updateUserInfo } from '@/api/user.js'

const EDITABLE_FIELDS = ['avatar', 'nickname', 'bio', 'gender', 'birthday', 'region']
const createEmptyDraft = () => ({
  avatar: '',
  nickname: '',
  bio: '',
  gender: 0,
  birthday: '',
  region: '',
  email: '',
  wechat: ''
})

export default {
  data() {
    return {
      saving: false,
      genderOptions: ['未设置', '男', '女'],
      draft: createEmptyDraft(),
      original: createEmptyDraft()
    }
  },

  computed: {
    userStore() {
      return useUserStore()
    },
    isLoggedIn() {
      return this.userStore.isAuthenticated || this.userStore.isLogin
    },
    userInfo() {
      return this.userStore.userInfo || {}
    },
    identities() {
      return this.userStore.identities && this.userStore.identities.length
        ? this.userStore.identities
        : ['collector']
    },
    identityTags() {
      const map = {
        collector: '收藏家',
        artist: '艺术家',
        promoter: '艺荐官',
        agent: '代理人'
      }
      return this.identities.map(key => ({ key, label: map[key] || key }))
    },
    identityCards() {
      const cards = [
        {
          key: 'collector',
          short: '藏',
          title: '收藏家资料',
          desc: '用于收藏、订单、转售和证书归档',
          status: '已启用'
        }
      ]

      if (this.identities.includes('artist')) {
        cards.push({
          key: 'artist',
          short: '艺',
          title: '艺术家主页资料',
          desc: '作品发布、艺术家主页和认证信息在此维护',
          action: '管理',
          path: '/pages/artist/home'
        })
      } else {
        cards.push({
          key: 'artist',
          short: '艺',
          title: '艺术家身份',
          desc: '提交认证后可发布作品与维护主页',
          action: '申请',
          path: '/pages/artist/apply'
        })
      }

      if (this.identities.includes('promoter')) {
        cards.push({
          key: 'promoter',
          short: '荐',
          title: '艺荐官资料',
          desc: '推广海报、收益账户和团队关系独立管理',
          action: '进入',
          path: '/pages/promoter/index'
        })
      }

      if (this.identities.includes('agent')) {
        cards.push({
          key: 'agent',
          short: '代',
          title: '代理人资料',
          desc: '代理协议、客户归属和成交协作资料',
          status: '待开放'
        })
      }

      return cards
    },
    completionPercent() {
      const required = ['avatar', 'nickname', 'bio', 'gender', 'birthday', 'region']
      const done = required.filter(key => {
        if (key === 'gender') return Number(this.draft.gender) > 0
        return !!this.draft[key]
      }).length
      return Math.round((done / required.length) * 100)
    },
    completionText() {
      if (this.completionPercent >= 100) return '资料完整，展示状态很好'
      if (this.completionPercent >= 67) return '还差一点就完整'
      return '先补齐基础展示信息'
    },
    missingFields() {
      const labels = {
        avatar: '头像',
        nickname: '昵称',
        bio: '简介',
        gender: '性别',
        birthday: '生日',
        region: '地区'
      }
      return Object.keys(labels).filter(key => {
        if (key === 'gender') return Number(this.draft.gender) <= 0
        return !this.draft[key]
      }).map(key => labels[key])
    },
    isDirty() {
      return EDITABLE_FIELDS.some(key => String(this.draft[key] ?? '') !== String(this.original[key] ?? ''))
    }
  },

  async onShow() {
    if (!this.isLoggedIn) return
    const info = await this.userStore.fetchUserInfo()
    this.syncDraft(info || this.userInfo)
  },

  methods: {
    buildDraft(info = {}) {
      return {
        avatar: info.avatar || '',
        nickname: info.nickname || '',
        bio: info.bio || '',
        gender: Number(info.gender) || 0,
        birthday: info.birthday || '',
        region: info.region || info.location || '',
        email: info.email || '',
        wechat: info.wechat || ''
      }
    },
    syncDraft(info) {
      const next = this.buildDraft(info)
      this.draft = { ...next }
      this.original = { ...next }
    },
    resetDraft() {
      if (!this.isDirty || this.saving) return
      this.draft = { ...this.original }
      uni.showToast({ title: '已还原', icon: 'none' })
    },
    changeAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const path = res.tempFilePaths[0]
          openCropper(path, { ratio: '1:1', shape: 'circle' }).then(cropped => {
            this.draft.avatar = cropped
          }).catch(() => {
            this.draft.avatar = path
          })
        }
      })
    },
    onGenderChange(e) {
      this.draft.gender = Number(e.detail.value) || 0
    },
    onBirthdayChange(e) {
      this.draft.birthday = e.detail.value
    },
    formatPhone(phone) {
      if (!phone) return '未绑定'
      return String(phone).replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
    },
    validateDraft() {
      if (!this.draft.nickname) {
        uni.showToast({ title: '请填写昵称', icon: 'none' })
        return false
      }
      return true
    },
    buildPayload() {
      const payload = {}
      EDITABLE_FIELDS.forEach(key => {
        if (String(this.draft[key] ?? '') !== String(this.original[key] ?? '')) {
          payload[key] = this.draft[key]
        }
      })
      return payload
    },
    async saveProfile() {
      if (!this.isDirty || this.saving) return
      if (!this.validateDraft()) return

      this.saving = true
      try {
        const payload = this.buildPayload()
        await updateUserInfo(payload)
        const merged = {
          ...this.userInfo,
          ...payload,
          location: payload.region !== undefined ? payload.region : this.userInfo.location
        }
        this.userStore.setUserInfo(merged)
        this.original = { ...this.draft }
        uni.showToast({ title: '保存成功', icon: 'success' })
      } catch (error) {
        uni.showToast({ title: error.message || '保存失败', icon: 'none' })
      } finally {
        this.saving = false
      }
    },
    goPage(page) {
      uni.navigateTo({ url: page })
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
$purple: #8c73c9;
$danger: #d86b6b;

.profile-page {
  min-height: 100vh;
  background: $bg;
  color: $text;
  padding: 24rpx 24rpx 156rpx;
  box-sizing: border-box;
}

.login-panel,
.hero-card,
.completion-card,
.form-section {
  background: $panel;
  border: 1rpx solid $line;
  border-radius: 16rpx;
}

.login-panel {
  min-height: calc(100vh - 48rpx);
  padding: 80rpx 48rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.login-avatar {
  width: 132rpx;
  height: 132rpx;
  border-radius: 50%;
  margin-bottom: 28rpx;
  border: 2rpx solid rgba($gold, 0.38);
}

.login-title {
  font-size: 36rpx;
  font-weight: 800;
}

.login-desc {
  margin-top: 14rpx;
  color: $muted;
  font-size: 25rpx;
  line-height: 38rpx;
}

.login-btn {
  margin-top: 34rpx;
  width: 260rpx;
  height: 82rpx;
  border-radius: 12rpx;
  background: $gold;
  color: #17130a;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 800;
}

.hero-card {
  padding: 30rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  background:
    radial-gradient(circle at 12% 0%, rgba($gold, 0.24), transparent 40%),
    $panel;
}

.avatar-wrap {
  position: relative;
  width: 128rpx;
  height: 128rpx;
  flex-shrink: 0;
}

.avatar {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  border: 2rpx solid rgba($gold, 0.42);
  background: $panel2;
}

.avatar-action {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: $gold;
  color: #17130a;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20rpx;
  font-weight: 800;
}

.hero-main {
  flex: 1;
  min-width: 0;
}

.nickname-input {
  height: 56rpx;
  color: $text;
  font-size: 36rpx;
  font-weight: 800;
}

.uid-line {
  display: block;
  margin-top: 8rpx;
  color: $dim;
  font-size: 22rpx;
}

.identity-tags,
.missing-list {
  display: flex;
  gap: 10rpx;
  flex-wrap: wrap;
}

.identity-tags {
  margin-top: 16rpx;
}

.identity-tag,
.missing-item {
  padding: 6rpx 14rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  color: $gold;
  background: rgba($gold, 0.15);
}

.identity-tag.artist,
.identity-icon.artist {
  color: $green;
  background: rgba($green, 0.15);
}

.identity-tag.promoter,
.identity-icon.promoter {
  color: $blue;
  background: rgba($blue, 0.15);
}

.identity-tag.agent,
.identity-icon.agent {
  color: $purple;
  background: rgba($purple, 0.15);
}

.completion-card,
.form-section {
  margin-top: 20rpx;
  padding: 26rpx;
}

.completion-head,
.section-head,
.field-row,
.identity-row,
.save-bar {
  display: flex;
  align-items: center;
}

.completion-head,
.section-head,
.field-row,
.identity-row {
  justify-content: space-between;
}

.section-kicker,
.section-note,
.count {
  color: $dim;
  font-size: 22rpx;
}

.completion-title,
.section-title {
  display: block;
  margin-top: 6rpx;
  font-size: 30rpx;
  font-weight: 800;
}

.completion-percent {
  color: $gold;
  font-size: 42rpx;
  font-weight: 900;
}

.progress-track {
  height: 12rpx;
  margin-top: 24rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  border-radius: 999rpx;
  background: linear-gradient(90deg, $gold, $green);
}

.missing-list {
  margin-top: 18rpx;
}

.missing-item {
  color: $muted;
  background: rgba(255, 255, 255, 0.06);
}

.section-head {
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid $line;
}

.field-block {
  padding: 24rpx 0;
  border-bottom: 1rpx solid $line;
}

.field-label {
  color: $text;
  font-size: 27rpx;
  font-weight: 700;
}

.textarea {
  width: 100%;
  min-height: 148rpx;
  margin-top: 18rpx;
  padding: 22rpx;
  border-radius: 12rpx;
  background: $panel2;
  color: $text;
  font-size: 27rpx;
  line-height: 40rpx;
  box-sizing: border-box;
}

.count {
  display: block;
  text-align: right;
  margin-top: 10rpx;
}

.field-row {
  min-height: 100rpx;
  border-bottom: 1rpx solid $line;

  &:last-child {
    border-bottom: none;
  }
}

.row-input,
.picker-value,
.readonly-value {
  color: $text;
  font-size: 27rpx;
  text-align: right;
}

.row-input {
  flex: 1;
  min-width: 0;
  height: 88rpx;
  margin-left: 28rpx;
}

.picker-value,
.readonly-value {
  max-width: 430rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.readonly-value {
  color: $muted;
}

.readonly {
  opacity: 0.78;
}

.identity-row {
  gap: 18rpx;
  min-height: 116rpx;
  border-bottom: 1rpx solid $line;

  &:last-child {
    border-bottom: none;
  }
}

.identity-icon {
  width: 58rpx;
  height: 58rpx;
  border-radius: 14rpx;
  flex-shrink: 0;
  color: $gold;
  background: rgba($gold, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 800;
}

.identity-main {
  flex: 1;
  min-width: 0;
}

.identity-title,
.identity-desc {
  display: block;
}

.identity-title {
  font-size: 27rpx;
  font-weight: 800;
}

.identity-desc {
  margin-top: 6rpx;
  color: $muted;
  font-size: 22rpx;
  line-height: 32rpx;
}

.identity-action,
.identity-status {
  flex-shrink: 0;
  font-size: 24rpx;
}

.identity-action {
  color: $gold;
  font-weight: 800;
}

.identity-status {
  color: $dim;
}

.safe-tip {
  padding: 20rpx 8rpx 0;
  color: $muted;
  font-size: 23rpx;
  line-height: 36rpx;
}

.save-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  gap: 16rpx;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(11, 11, 12, 0.92);
  border-top: 1rpx solid $line;
  box-sizing: border-box;
}

.save-secondary,
.save-primary {
  height: 86rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 800;
}

.save-secondary {
  width: 190rpx;
  color: $gold;
  background: rgba($gold, 0.12);
}

.save-primary {
  flex: 1;
  color: #17130a;
  background: $gold;
}

.disabled {
  opacity: 0.45;
}

.placeholder {
  color: $dim;
}
</style>
