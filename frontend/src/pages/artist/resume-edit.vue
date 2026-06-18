<template>
  <view class="resume-edit-page">
    <view class="top-bar">
      <view class="icon-button" @click="goBack">‹</view>
      <view class="page-title">{{ sectionTitle }}</view>
      <button class="save-button" :disabled="saving" @click="save">{{ saving ? '保存中' : '保存' }}</button>
    </view>

    <view class="editor-intro">
      <image :src="sectionIcon" mode="aspectFit"></image>
      <view>
        <view class="editor-title">{{ sectionTitle }}</view>
        <view class="editor-desc">按时间倒序展示，可增删和调整每条履历。</view>
      </view>
    </view>

    <view class="entry-card" v-for="(entry, index) in entries" :key="index">
      <view class="entry-head">
        <text>第 {{ index + 1 }} 条</text>
        <button class="delete-button" @click="removeEntry(index)">删除</button>
      </view>
      <view class="field-row">
        <text class="field-label">年份</text>
        <input v-model.trim="entry.year" maxlength="12" placeholder="如 2024" />
      </view>
      <view class="field-block">
        <text class="field-label">主要内容</text>
        <textarea v-model.trim="entry.primary" maxlength="100" placeholder="填写院校、展览、奖项或机构名称" />
      </view>
      <view class="field-block">
        <text class="field-label">补充说明</text>
        <textarea v-model.trim="entry.secondary" maxlength="100" placeholder="可选，如导师、地点、作品名称等" />
      </view>
    </view>

    <button class="add-button" @click="addEntry">+ 添加一条履历</button>
    <view class="bottom-space"></view>
  </view>
</template>

<script>
import * as userApi from '@/api/user'
import {
  RESUME_SECTION_DEFINITIONS,
  buildDefaultResumeEntries,
  normalizeResumeEntry,
  parseArtistResume,
  serializeArtistResume
} from '@/utils/artistResume'
import { getCurrentUserIdentity } from '@/utils/auth'

export default {
  data() {
    return {
      artistId: '',
      artistName: '艺术家',
      sectionKey: 'education',
      entries: [],
      allSections: {},
      saving: false
    }
  },
  computed: {
    sectionDefinition() {
      return RESUME_SECTION_DEFINITIONS.find(item => item.key === this.sectionKey) || RESUME_SECTION_DEFINITIONS[0]
    },
    sectionTitle() {
      return this.sectionDefinition.title
    },
    sectionIcon() {
      return this.sectionDefinition.icon
    }
  },
  async onLoad(options = {}) {
    this.sectionKey = RESUME_SECTION_DEFINITIONS.some(item => item.key === options.section)
      ? options.section
      : 'education'
    const currentUserId = getCurrentUserIdentity().id
    this.artistId = options.userId || currentUserId
    if (!currentUserId) {
      uni.showToast({ title: '请先登录', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 600)
      return
    }
    if (this.artistId && String(this.artistId) !== String(currentUserId)) {
      uni.showToast({ title: '只能编辑自己的艺术履历', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 800)
      return
    }
    await this.loadResume()
  },
  methods: {
    async loadResume() {
      try {
        const artist = await userApi.getArtistInfo(this.artistId)
        this.artistName = artist?.nickname || artist?.name || artist?.realName || '艺术家'
        const defaults = buildDefaultResumeEntries(this.artistName)
        const saved = parseArtistResume(artist?.resume)
        this.allSections = { ...defaults, ...(saved?.sections || {}) }
        this.entries = (this.allSections[this.sectionKey] || []).map(item => ({ ...normalizeResumeEntry(item) }))
      } catch (error) {
        console.error('[resume-edit] 加载履历失败:', error)
        uni.showToast({ title: error?.message || '履历加载失败', icon: 'none' })
      }
    },
    addEntry() {
      this.entries.push({ year: '', primary: '', secondary: '' })
    },
    removeEntry(index) {
      this.entries.splice(index, 1)
    },
    async save() {
      if (this.saving) return
      const entries = this.entries
        .map(normalizeResumeEntry)
        .filter(item => item.year || item.primary || item.secondary)
      if (entries.some(item => !item.primary)) {
        uni.showToast({ title: '请填写每条履历的主要内容', icon: 'none' })
        return
      }
      this.saving = true
      try {
        const sections = { ...this.allSections, [this.sectionKey]: entries }
        await userApi.updateArtistResume(serializeArtistResume(sections))
        uni.showToast({ title: '履历已更新', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 500)
      } catch (error) {
        console.error('[resume-edit] 保存履历失败:', error)
        uni.showToast({ title: error?.message || '保存失败', icon: 'none' })
      } finally {
        this.saving = false
      }
    },
    goBack() {
      uni.navigateBack()
    }
  }
}
</script>

<style lang="scss" scoped>
.resume-edit-page {
  min-height: 100vh;
  padding: calc(92rpx + env(safe-area-inset-top)) 24rpx 40rpx;
  box-sizing: border-box;
  color: #f5f1e8;
  background: #050505;
}

.top-bar {
  position: fixed;
  z-index: 20;
  top: 0;
  left: 0;
  right: 0;
  height: calc(88rpx + env(safe-area-inset-top));
  padding: env(safe-area-inset-top) 24rpx 0;
  box-sizing: border-box;
  display: grid;
  grid-template-columns: 72rpx 1fr 104rpx;
  align-items: center;
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.08);
  background: rgba(5, 5, 5, 0.96);
}

.icon-button {
  font-size: 50rpx;
  line-height: 1;
  color: #e8bd49;
}

.page-title {
  text-align: center;
  font-size: 30rpx;
  font-weight: 600;
}

.save-button,
.delete-button,
.add-button {
  margin: 0;
  padding: 0;
  border: 0;
}

.save-button {
  width: 104rpx;
  height: 52rpx;
  border-radius: 6rpx;
  color: #171004;
  background: #e7b93e;
  font-size: 22rpx;
  font-weight: 500;
}

.save-button::after,
.delete-button::after,
.add-button::after {
  border: 0;
}

.editor-intro {
  margin: 20rpx 0;
  padding: 24rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
  border: 1rpx solid rgba(224, 180, 63, 0.28);
  border-radius: 10rpx;
  background: linear-gradient(135deg, #1b1a17, #101010);
}

.editor-intro image {
  width: 54rpx;
  height: 54rpx;
}

.editor-title {
  color: #e7b93e;
  font-size: 28rpx;
  font-weight: 600;
}

.editor-desc {
  margin-top: 6rpx;
  color: #8f8f8f;
  font-size: 20rpx;
}

.entry-card {
  margin-top: 18rpx;
  padding: 22rpx 24rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.1);
  border-radius: 10rpx;
  background: #151515;
}

.entry-head,
.field-row {
  display: flex;
  align-items: center;
}

.entry-head {
  justify-content: space-between;
  color: #d9d9d9;
  font-size: 22rpx;
}

.delete-button {
  width: 76rpx;
  height: 40rpx;
  color: #c78383;
  background: transparent;
  font-size: 20rpx;
  line-height: 40rpx;
}

.field-row,
.field-block {
  margin-top: 20rpx;
}

.field-label {
  color: #aaa;
  font-size: 21rpx;
}

.field-row .field-label {
  width: 96rpx;
}

input,
textarea {
  box-sizing: border-box;
  color: #f2f2f2;
  background: #0b0b0b;
  border: 1rpx solid rgba(255, 255, 255, 0.1);
  border-radius: 7rpx;
  font-size: 23rpx;
}

input {
  flex: 1;
  height: 64rpx;
  padding: 0 18rpx;
}

textarea {
  width: 100%;
  height: 108rpx;
  margin-top: 10rpx;
  padding: 16rpx 18rpx;
}

.add-button {
  width: 100%;
  height: 72rpx;
  margin-top: 22rpx;
  border: 1rpx dashed rgba(224, 180, 63, 0.55);
  border-radius: 8rpx;
  color: #e7b93e;
  background: rgba(224, 180, 63, 0.06);
  font-size: 24rpx;
  font-weight: 400;
}

.bottom-space {
  height: env(safe-area-inset-bottom);
}
</style>
