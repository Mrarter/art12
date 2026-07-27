<template>
  <view class="appreciation-page">
    <scroll-view
      class="page-scroll"
      scroll-y
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="daily-header">
        <text class="daily-date">{{ publishDateLabel }}</text>
        <text class="daily-name">DAY DAY ART</text>
      </view>

      <view class="artwork-stage">
        <image class="artwork-image" :src="featuredWork.coverThumb || featuredWork.cover" mode="aspectFit" lazy-load></image>
      </view>

      <view class="article">
        <view class="title-block">
          <text class="artist-name">{{ featuredWork.author }}</text>
          <text class="art-title">{{ featuredWork.title }}</text>
          <text class="art-meta">{{ featuredWork.subtitle }}</text>
        </view>

        <view class="fact-strip">
          <view class="fact-item" v-for="fact in facts" :key="fact.label">
            <text class="fact-label">{{ fact.label }}</text>
            <text class="fact-value">{{ fact.value }}</text>
          </view>
        </view>

        <view class="story-section">
          <text class="section-label">今日导读</text>
          <text class="lead-text">{{ featuredWork.summary }}</text>
          <text class="story-p" v-for="paragraph in featuredWork.paragraphs" :key="paragraph">{{ paragraph }}</text>
        </view>

        <view class="detail-section">
          <text class="section-label">怎么看</text>
          <view class="detail-card" v-for="detail in readingDetails" :key="detail.title">
            <view class="detail-index">{{ detail.index }}</view>
            <view class="detail-copy">
              <text class="detail-title">{{ detail.title }}</text>
              <text class="detail-desc">{{ detail.desc }}</text>
            </view>
          </view>
        </view>

        <view class="quote-box">
          <text class="quote-text">{{ featuredWork.quote }}</text>
        </view>
      </view>

      <view class="related-section">
        <view class="section-head">
          <text class="section-title">延伸欣赏</text>
          <text class="section-action" @click="refreshWorks">刷新文章</text>
        </view>
        <scroll-view class="related-scroll" scroll-x enable-flex>
          <view class="related-card" v-for="work in works" :key="work.id" @click="goDetail(work)">
            <image class="related-image" :src="work.coverThumb || work.cover || '/static/images/artwork-fallback.png'" mode="aspectFill" lazy-load></image>
            <text class="related-title">{{ getWorkTitle(work) }}</text>
            <text class="related-meta">{{ getWorkMeta(work) }}</text>
          </view>
        </scroll-view>
      </view>

      <view class="tool-section">
        <text class="section-title">DAY DAY ART 笔记</text>
        <view class="note-grid">
          <view class="note-card" v-for="method in methods" :key="method.title">
            <image class="note-icon" :src="method.icon" mode="aspectFit"></image>
            <text class="note-title">{{ method.title }}</text>
            <text class="note-desc">{{ method.desc }}</text>
          </view>
        </view>
      </view>

      <view class="bottom-actions">
        <view class="primary-action" @click="goGallery">进入画廊</view>
        <view class="secondary-action" @click="onRefresh">刷新 DAY DAY ART</view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import request from '@/api/request'
import { buildImageThumbnailUrl, normalizeImageUrl } from '@/api/product.js'

const normalizeListResult = (result) => {
  if (Array.isArray(result)) return result
  return result?.records || result?.list || []
}

const FALLBACK_COVER = '/static/images/artwork-fallback.png'
const DEFAULT_ARTICLE = {
  id: 'fallback-article',
  title: 'DAY DAY ART 内容整理中',
  subtitle: '后台已发布文章会在这里实时展示',
  author: '艺本策展组',
  coverImage: FALLBACK_COVER,
  summary: '当前页面将优先读取已发布的鉴赏文章，用真实封面、标题、摘要和正文更新首屏内容。',
  content: '当前暂无可展示的鉴赏文章。\n\n请先在后台发布文章，随后下拉刷新这里即可同步最新内容。',
  tags: 'DAY DAY ART,文章发布',
  category: 'APPRECIATION',
  publishTime: null
}

const CATEGORY_LABELS = {
  APPRECIATION: 'DAY DAY ART'
}

const parseParagraphs = (text) => {
  return String(text || '')
    .split(/\n+/)
    .map(item => item.trim())
    .filter(Boolean)
}

const parseTags = (text) => {
  return String(text || '')
    .split(/[，,、/\s]+/)
    .map(item => item.trim())
    .filter(Boolean)
}

const formatDate = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}.${month}.${day}`
}

const estimateReadTime = (text) => {
  const length = String(text || '').replace(/\s+/g, '').length
  return `${Math.max(1, Math.ceil(length / 320))}分钟`
}

const buildReadingDetails = (paragraphs, tags) => {
  const source = [...paragraphs.slice(0, 2), ...tags.slice(0, 1)]
  const titles = ['先看主旨', '再看细节', '最后看关联']
  const fallbacks = [
    '先从标题和摘要进入，抓住文章希望你优先注意的观看角度。',
    '把正文里反复出现的细节留下来，它们通常就是作品真正耐看的地方。',
    '再回到标签与作者信息，判断这篇鉴赏放在整个专题里想强调什么。'
  ]

  return titles.map((title, index) => ({
    index: String(index + 1).padStart(2, '0'),
    title,
    desc: source[index] || fallbacks[index]
  }))
}

const normalizeArticle = (item) => {
  const paragraphs = parseParagraphs(item.content || item.summary)
  const tags = parseTags(item.tags)
  return {
    ...item,
    id: item.id,
    author: item.author || '艺本策展组',
    cover: normalizeImageUrl(item.coverImage || item.cover) || FALLBACK_COVER,
    coverThumb: buildImageThumbnailUrl(item.coverThumbImage || item.coverImage || item.cover, 1200) || normalizeImageUrl(item.coverImage || item.cover) || FALLBACK_COVER,
    title: item.title || '未命名文章',
    subtitle: item.subtitle || item.summary || 'DAY DAY ART 文章',
    summary: item.summary || paragraphs[0] || 'DAY DAY ART 文章',
    paragraphs,
    tags,
    publishDateLabel: formatDate(item.publishTime || item.updateTime),
    readTime: estimateReadTime(item.content || item.summary),
    categoryLabel: CATEGORY_LABELS[item.category] || 'DAY DAY ART',
    quote: paragraphs[paragraphs.length - 1] || item.summary || '好的观看，往往从停下来开始。'
  }
}

export default {
  data() {
    return {
      refreshing: false,
      loading: false,
      works: [],
      pageSize: 10,
      featuredWork: normalizeArticle(DEFAULT_ARTICLE),
      readingDetails: buildReadingDetails(parseParagraphs(DEFAULT_ARTICLE.content), parseTags(DEFAULT_ARTICLE.tags)),
      methods: [
        { title: '三步观看', desc: '远看结构，近看笔触，再退后感受整体气息。', icon: '/static/art-icons/icon-preview.svg' },
        { title: '记录感受', desc: '写下第一眼注意到的位置，比套用术语更可靠。', icon: '/static/art-icons/icon-document.svg' },
        { title: '核对来源', desc: '结合证书、作者、尺寸与流转记录判断作品信息。', icon: '/static/art-icons/icon-verify.svg' },
        { title: '联系空间', desc: '把作品放进真实光照、墙面和观看距离里想象。', icon: '/static/art-icons/icon-gallery.svg' }
      ],
      fallbackWorks: [
        normalizeArticle({ ...DEFAULT_ARTICLE, id: 'fallback-1', title: 'DAY DAY ART 内容整理中' }),
        normalizeArticle({ ...DEFAULT_ARTICLE, id: 'fallback-2', title: '更多文章即将同步' }),
        normalizeArticle({ ...DEFAULT_ARTICLE, id: 'fallback-3', title: '下拉即可刷新最新发布' })
      ]
    }
  },
  computed: {
    publishDateLabel() {
      return this.featuredWork.publishDateLabel || formatDate(new Date())
    },
    facts() {
      return [
        { label: '栏目', value: this.featuredWork.categoryLabel },
        { label: '阅读', value: this.featuredWork.readTime },
        { label: '关键词', value: this.featuredWork.tags.join(' / ') || 'DAY DAY ART / 艺术 / 观察' }
      ]
    }
  },
  onLoad() {
    this.loadWorks()
  },
  methods: {
    async loadWorks() {
      if (this.loading) return
      this.loading = true
      try {
        const list = await this.fetchArticles({ page: 1, size: this.pageSize, category: 'APPRECIATION' })
        if (list.length) {
          this.featuredWork = list[0]
          this.readingDetails = buildReadingDetails(this.featuredWork.paragraphs, this.featuredWork.tags)
          this.works = list.slice(1).length ? list.slice(1) : [list[0]]
        } else {
          this.featuredWork = normalizeArticle(DEFAULT_ARTICLE)
          this.readingDetails = buildReadingDetails(this.featuredWork.paragraphs, this.featuredWork.tags)
          this.works = this.fallbackWorks
        }
      } catch (e) {
        this.featuredWork = normalizeArticle(DEFAULT_ARTICLE)
        this.readingDetails = buildReadingDetails(this.featuredWork.paragraphs, this.featuredWork.tags)
        this.works = this.fallbackWorks
      } finally {
        this.loading = false
      }
    },
    async fetchArticles(params) {
      const data = await request.get('/product/article/list', params)
      return normalizeListResult(data).map(normalizeArticle)
    },
    async onRefresh() {
      this.refreshing = true
      await this.loadWorks()
      this.refreshing = false
    },
    refreshWorks() {
      this.loadWorks()
    },
    goGallery() {
      uni.switchTab({ url: '/pages/gallery/index' })
    },
    goDetail(work) {
      if (String(work.id).startsWith('fallback-')) {
        return
      }
      if (work.id === this.featuredWork.id) {
        return
      }
      uni.navigateTo({ url: `/pages/content/article?id=${work.id}` })
    },
    getWorkTitle(work) {
      return work.title || '未命名文章'
    },
    getWorkMeta(work) {
      return [work.author || '艺本策展组', work.publishDateLabel || '待发布', work.tags.slice(0, 2).join(' / ')]
        .filter(Boolean)
        .join(' / ')
    }
  }
}
</script>

<style lang="scss" scoped>
.appreciation-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top, rgba(212, 175, 55, 0.22), transparent 30%),
    radial-gradient(circle at 50% 20%, rgba(124, 91, 27, 0.2), transparent 42%),
    linear-gradient(180deg, #090909 0%, #111111 28%, #0d0d0d 100%);
  color: #f3ead2;
}

.page-scroll {
  height: 100vh;
}

.daily-header {
  padding: 40rpx 32rpx 28rpx;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

.daily-date {
  color: rgba(224, 191, 112, 0.74);
  font-size: 24rpx;
  letter-spacing: 1rpx;
}

.daily-name {
  color: #e7c76b;
  font-size: 28rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}

.artwork-stage {
  margin: 0 28rpx;
  min-height: 540rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(180deg, rgba(34, 34, 34, 0.96), rgba(18, 18, 18, 0.98));
  border: 1rpx solid rgba(212, 175, 55, 0.18);
  border-radius: 28rpx;
  box-shadow:
    0 28rpx 80rpx rgba(0, 0, 0, 0.42),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.03);
  overflow: hidden;
}

.artwork-image {
  width: 100%;
  height: 520rpx;
}

.article {
  margin: 28rpx 28rpx 0;
  padding: 36rpx 32rpx 8rpx;
  background: rgba(16, 16, 16, 0.9);
  border: 1rpx solid rgba(212, 175, 55, 0.12);
  border-radius: 28rpx;
  box-shadow: 0 24rpx 70rpx rgba(0, 0, 0, 0.28);
}

.title-block {
  display: flex;
  flex-direction: column;
  padding-bottom: 28rpx;
  border-bottom: 1rpx solid rgba(212, 175, 55, 0.12);
}

.artist-name {
  color: #d8b25f;
  font-size: 26rpx;
  font-weight: 700;
  letter-spacing: 1rpx;
}

.art-title {
  margin-top: 12rpx;
  color: #f6efdb;
  font-size: 52rpx;
  font-weight: 700;
  line-height: 1.16;
}

.art-meta {
  margin-top: 14rpx;
  color: rgba(255, 255, 255, 0.64);
  font-size: 25rpx;
  line-height: 1.6;
}

.fact-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14rpx;
  margin-top: 28rpx;
}

.fact-item {
  min-height: 126rpx;
  padding: 20rpx 18rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: linear-gradient(180deg, rgba(31, 31, 31, 0.96), rgba(18, 18, 18, 0.94));
  border: 1rpx solid rgba(212, 175, 55, 0.1);
  border-radius: 18rpx;
  box-sizing: border-box;
}

.fact-label {
  color: rgba(224, 191, 112, 0.56);
  font-size: 21rpx;
}

.fact-value {
  margin-top: 8rpx;
  color: #f3ead2;
  font-size: 24rpx;
  font-weight: 700;
  line-height: 1.35;
}

.story-section,
.detail-section {
  margin-top: 44rpx;
}

.section-label {
  display: block;
  color: #d9b766;
  font-size: 24rpx;
  font-weight: 700;
  margin-bottom: 18rpx;
  letter-spacing: 1rpx;
}

.lead-text {
  display: block;
  color: #f6efdb;
  font-size: 34rpx;
  font-weight: 700;
  line-height: 1.6;
  margin-bottom: 24rpx;
}

.story-p {
  display: block;
  color: rgba(255, 255, 255, 0.82);
  font-size: 29rpx;
  line-height: 1.86;
  margin-bottom: 24rpx;
}

.detail-card {
  display: flex;
  gap: 22rpx;
  padding: 24rpx 0;
  border-top: 1rpx solid rgba(212, 175, 55, 0.1);
}

.detail-index {
  width: 70rpx;
  color: #cfa657;
  font-size: 25rpx;
  font-weight: 700;
}

.detail-copy {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.detail-title {
  color: #f4e9cd;
  font-size: 30rpx;
  font-weight: 700;
}

.detail-desc {
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.62);
  font-size: 25rpx;
  line-height: 1.62;
}

.quote-box {
  margin-top: 32rpx;
  padding: 28rpx;
  background: linear-gradient(180deg, rgba(36, 30, 19, 0.92), rgba(24, 22, 18, 0.92));
  border: 1rpx solid rgba(212, 175, 55, 0.16);
  border-left: 6rpx solid #c79f4f;
  border-radius: 20rpx;
}

.quote-text {
  color: rgba(245, 234, 209, 0.92);
  font-size: 28rpx;
  line-height: 1.65;
}

.related-section,
.tool-section {
  margin-top: 48rpx;
  padding: 0 30rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22rpx;
}

.section-title {
  color: #f1e4bf;
  font-size: 34rpx;
  font-weight: 700;
}

.section-action {
  color: #cda14f;
  font-size: 24rpx;
  font-weight: 700;
}

.related-scroll {
  width: 100%;
  white-space: nowrap;
}

.related-card {
  display: inline-flex;
  flex-direction: column;
  width: 290rpx;
  margin-right: 20rpx;
  padding-bottom: 18rpx;
  background: linear-gradient(180deg, rgba(24, 24, 24, 0.98), rgba(14, 14, 14, 0.98));
  border: 1rpx solid rgba(212, 175, 55, 0.12);
  border-radius: 22rpx;
  overflow: hidden;
  box-shadow: 0 18rpx 48rpx rgba(0, 0, 0, 0.24);
}

.related-image {
  width: 290rpx;
  height: 220rpx;
  background: #1e1e1e;
}

.related-title,
.related-meta {
  padding: 0 18rpx;
  white-space: normal;
}

.related-title {
  min-height: 74rpx;
  margin-top: 16rpx;
  color: #f5ebd0;
  font-size: 25rpx;
  font-weight: 700;
  line-height: 1.42;
}

.related-meta {
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.54);
  font-size: 22rpx;
  line-height: 1.4;
}

.note-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 22rpx;
}

.note-card {
  min-height: 214rpx;
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, rgba(22, 22, 22, 0.98), rgba(14, 14, 14, 0.98));
  border: 1rpx solid rgba(212, 175, 55, 0.1);
  border-radius: 22rpx;
  box-sizing: border-box;
}

.note-icon {
  width: 42rpx;
  height: 42rpx;
  margin-bottom: 16rpx;
  opacity: 0.9;
}

.note-title {
  color: #f1e4bf;
  font-size: 27rpx;
  font-weight: 700;
}

.note-desc {
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.6);
  font-size: 23rpx;
  line-height: 1.48;
}

.bottom-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  padding: 42rpx 30rpx 68rpx;
}

.primary-action,
.secondary-action {
  height: 82rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 27rpx;
  font-weight: 700;
  border-radius: 999rpx;
}

.primary-action {
  color: #1f1707;
  background: linear-gradient(135deg, #cba14c 0%, #f1d37a 100%);
  box-shadow: 0 12rpx 30rpx rgba(174, 130, 37, 0.22);
}

.secondary-action {
  color: #e6c46f;
  background: rgba(255, 255, 255, 0.02);
  border: 1rpx solid rgba(212, 175, 55, 0.18);
}
</style>
