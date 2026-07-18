<template>
  <view class="article-page">
    <scroll-view class="article-scroll" scroll-y>
      <view class="article-shell">
        <view v-if="loading" class="state-card">
          <text class="state-title">加载中...</text>
          <text class="state-text">正在获取文章内容</text>
        </view>

        <view v-else-if="error" class="state-card">
          <text class="state-title">文章暂时无法查看</text>
          <text class="state-text">{{ error }}</text>
          <button class="retry-btn" @click="loadArticle">重新加载</button>
        </view>

        <view v-else-if="article.id" class="article-card">
          <image
            v-if="resolvedBodyImage"
            class="cover-image"
            :src="resolvedBodyImage"
            mode="widthFix"
            lazy-load
            @error="onBodyImageError"
            @click="previewBodyImage"
          />

          <view v-if="bodyImageFailed" class="cover-fallback-note">
            <text class="cover-fallback-text">正文图片加载失败，已切换为默认配图</text>
          </view>

          <view class="article-header">
            <text class="category-badge">{{ categoryLabel(article.category) }}</text>
            <text class="publish-time">{{ formatTime(article.publishTime || article.updateTime) }}</text>
          </view>

          <text class="article-title">{{ article.title }}</text>
          <text v-if="article.subtitle || article.summary" class="article-subtitle">
            {{ article.subtitle || article.summary }}
          </text>

          <view class="author-row">
            <view class="author-avatar">{{ authorInitial }}</view>
            <view class="author-meta">
              <text class="author-name">{{ article.author || '艺本艺术' }}</text>
              <text class="author-role">官方内容发布</text>
            </view>
          </view>

          <view v-if="article.tags.length" class="tag-row">
            <text v-for="tag in article.tags" :key="tag" class="tag-chip">{{ tag }}</text>
          </view>

          <view class="article-body">
            <text v-for="(paragraph, index) in paragraphs" :key="index" class="article-paragraph">
              {{ paragraph }}
            </text>
          </view>

          <view v-if="recentArticles.length" class="recent-section">
            <view class="recent-head">
              <text class="recent-title">往期内容</text>
              <text class="recent-subtitle">继续阅读 DAY DAY ART</text>
            </view>

            <view
              v-for="item in recentArticles"
              :key="item.id"
              class="recent-card"
              @click="goArticle(item.id)"
            >
              <image
                class="recent-cover"
                :src="item.coverThumbImage || item.coverOriginalImage || item.coverImage || FALLBACK_COVER"
                mode="aspectFill"
                lazy-load
              />
              <view class="recent-body">
                <text class="recent-card-title">{{ item.title || '未命名文章' }}</text>
                <text v-if="item.subtitle || item.summary" class="recent-card-summary">
                  {{ item.subtitle || item.summary }}
                </text>
                <view class="recent-card-meta">
                  <text class="recent-card-author">{{ item.author || '艺本艺术' }}</text>
                  <text class="recent-card-dot">/</text>
                  <text class="recent-card-time">{{ formatTime(item.publishTime || item.updateTime) }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShareAppMessage } from '@dcloudio/uni-app'
import request from '@/api/request'
import { buildImageThumbnailUrl, normalizeImageUrl } from '@/api/product'

const FALLBACK_COVER = '/static/images/artwork-fallback.png'

const loading = ref(true)
const error = ref('')
const articleId = ref('')
const bodyImageFailed = ref(false)
const recentArticles = ref([])
const article = ref({
  id: '',
  title: '',
  subtitle: '',
  summary: '',
  author: '',
  category: '',
  coverImage: '',
  bodyImage: '',
  content: '',
  tags: [],
  publishTime: '',
  updateTime: ''
})

const categoryMap = {
  APPRECIATION: 'DAY DAY ART',
  CURATION: '专题策展',
  ARTIST: '艺术家故事',
  NOTICE: '平台公告'
}

const paragraphs = computed(() => {
  const list = String(article.value.content || '')
    .split(/\n+/)
    .map(item => item.trim())
    .filter(Boolean)
  return list.length ? list : ['暂无正文内容']
})

const authorInitial = computed(() => {
  const name = String(article.value.author || '艺')
  return name.slice(0, 1)
})

const resolvedBodyImage = computed(() => {
  if (bodyImageFailed.value) return FALLBACK_COVER
  return article.value.bodyImage || article.value.coverOriginalImage || article.value.coverImage || FALLBACK_COVER
})

const categoryLabel = (category) => categoryMap[category] || '文章内容'

const formatTime = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

const normalizeTags = (tags) => {
  return String(tags || '')
    .split(/[,，]/)
    .map(item => item.trim())
    .filter(Boolean)
}

const normalizeListResult = (result) => {
  if (Array.isArray(result)) return result
  return result?.records || result?.list || []
}

const normalizeArticle = (data = {}) => ({
  ...data,
  coverImage: normalizeImageUrl(data.coverImage || data.cover || ''),
  coverOriginalImage: normalizeImageUrl(data.coverOriginalImage || data.coverImage || data.cover || ''),
  bodyImage: normalizeImageUrl(data.bodyImage || data.coverOriginalImage || data.coverImage || data.cover || ''),
  coverThumbImage: buildImageThumbnailUrl(data.coverThumbImage || data.coverImage || data.cover || '', 1080),
  tags: normalizeTags(data.tags)
})

const onBodyImageError = () => {
  bodyImageFailed.value = true
}

const previewBodyImage = () => {
  const current = article.value.bodyImage || article.value.coverOriginalImage || article.value.coverImage
  if (!current) return
  uni.previewImage({
    current,
    urls: [current]
  })
}

const loadRecentArticles = async () => {
  try {
    const data = await request.get('/product/article/list', { page: 1, size: 10, category: 'APPRECIATION' })
    recentArticles.value = normalizeListResult(data)
      .map(normalizeArticle)
      .filter(item => String(item.id) !== String(articleId.value))
      .slice(0, 6)
  } catch (e) {
    recentArticles.value = []
  }
}

const goArticle = (id) => {
  if (!id || String(id) === String(articleId.value)) return
  uni.navigateTo({ url: `/pages/content/article?id=${id}` })
}

const loadArticle = async () => {
  if (!articleId.value) {
    error.value = '缺少文章编号'
    loading.value = false
    return
  }

  loading.value = true
  error.value = ''
  bodyImageFailed.value = false
  try {
    const data = await request.get(`/product/article/${articleId.value}`)
    article.value = normalizeArticle(data || {})
    await loadRecentArticles()
    if (article.value.title) {
      uni.setNavigationBarTitle({ title: article.value.title })
    }
  } catch (e) {
    error.value = e.message || '文章加载失败，请稍后重试'
    recentArticles.value = []
  } finally {
    loading.value = false
  }
}

onLoad((options = {}) => {
  articleId.value = options.id || ''
  loadArticle()
})

onShareAppMessage(() => ({
  title: article.value.title || '艺本艺术文章',
  path: `/pages/content/article?id=${articleId.value}`
}))
</script>

<style lang="scss" scoped>
.article-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top, rgba(212, 175, 55, 0.14), transparent 35%),
    linear-gradient(180deg, #111111 0%, #191919 46%, #101010 100%);
}

.article-scroll {
  min-height: 100vh;
}

.article-shell {
  padding: 24rpx;
  padding-bottom: calc(48rpx + env(safe-area-inset-bottom));
}

.state-card,
.article-card {
  border: 1rpx solid rgba(212, 175, 55, 0.12);
  border-radius: 28rpx;
  background: rgba(20, 20, 20, 0.92);
  box-shadow: 0 24rpx 60rpx rgba(0, 0, 0, 0.28);
}

.state-card {
  margin-top: 40rpx;
  padding: 48rpx 32rpx;
  text-align: center;
}

.state-title {
  display: block;
  color: #f6e7bb;
  font-size: 34rpx;
  font-weight: 600;
}

.state-text {
  display: block;
  margin-top: 16rpx;
  color: rgba(255, 255, 255, 0.72);
  font-size: 26rpx;
  line-height: 1.7;
}

.retry-btn {
  margin-top: 28rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #d9b24f 0%, #f3d378 100%);
  color: #241c08;
  font-size: 28rpx;
}

.article-card {
  overflow: hidden;
}

.cover-image {
  width: 100%;
  display: block;
  background: #1b1b1b;
}

.cover-fallback-note {
  padding: 18rpx 28rpx 0;
}

.cover-fallback-text {
  color: rgba(255, 255, 255, 0.46);
  font-size: 22rpx;
}

.article-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 28rpx 28rpx 0;
}

.category-badge {
  display: inline-flex;
  align-items: center;
  min-height: 44rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: rgba(212, 175, 55, 0.16);
  color: #e7c76b;
  font-size: 22rpx;
  letter-spacing: 1rpx;
}

.publish-time {
  color: rgba(255, 255, 255, 0.48);
  font-size: 24rpx;
}

.article-title {
  display: block;
  padding: 20rpx 28rpx 0;
  color: #f7f1df;
  font-size: 46rpx;
  line-height: 1.24;
  font-weight: 700;
}

.article-subtitle {
  display: block;
  padding: 18rpx 28rpx 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: 28rpx;
  line-height: 1.7;
}

.author-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 28rpx;
}

.author-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #d7b052 0%, #9a7422 100%);
  color: #1b1405;
  font-size: 30rpx;
  font-weight: 700;
}

.author-meta {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.author-name {
  color: #f4ead0;
  font-size: 28rpx;
  font-weight: 600;
}

.author-role {
  color: rgba(255, 255, 255, 0.48);
  font-size: 22rpx;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  padding: 0 28rpx 8rpx;
}

.tag-chip {
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.7);
  font-size: 22rpx;
}

.article-body {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  padding: 20rpx 28rpx 36rpx;
}

.article-paragraph {
  color: rgba(255, 255, 255, 0.9);
  font-size: 30rpx;
  line-height: 1.9;
  white-space: pre-wrap;
}

.recent-section {
  padding: 8rpx 28rpx 32rpx;
  border-top: 1rpx solid rgba(212, 175, 55, 0.08);
}

.recent-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16rpx;
  padding: 24rpx 0 20rpx;
}

.recent-title {
  color: #f6e7bb;
  font-size: 32rpx;
  font-weight: 600;
}

.recent-subtitle {
  color: rgba(255, 255, 255, 0.42);
  font-size: 22rpx;
}

.recent-card {
  display: flex;
  gap: 20rpx;
  padding: 18rpx 0;
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.recent-cover {
  width: 200rpx;
  height: 136rpx;
  flex: 0 0 200rpx;
  border-radius: 18rpx;
  background: #1b1b1b;
}

.recent-body {
  min-width: 0;
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: space-between;
  gap: 10rpx;
}

.recent-card-title {
  color: #f7f1df;
  font-size: 28rpx;
  line-height: 1.45;
  font-weight: 600;
}

.recent-card-summary {
  color: rgba(255, 255, 255, 0.62);
  font-size: 24rpx;
  line-height: 1.5;
}

.recent-card-meta {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex-wrap: wrap;
}

.recent-card-author,
.recent-card-time,
.recent-card-dot {
  color: rgba(255, 255, 255, 0.42);
  font-size: 22rpx;
}
</style>
