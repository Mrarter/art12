<template>
  <view class="artist-list-page">
    <view class="search-panel">
      <view class="search-box">
        <text class="search-icon">⌕</text>
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索艺术家"
          placeholder-class="input-placeholder"
          confirm-type="search"
          @confirm="loadArtists(true)"
        />
      </view>
    </view>

    <scroll-view
      class="artist-scroll"
      scroll-y
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <view class="artist-card" v-for="artist in artists" :key="artist.id" @click="goArtist(artist)">
        <image class="artist-avatar" :src="artist.avatar || artist.cover || '/static/avatar/default.jpg'" mode="aspectFill"></image>
        <view class="artist-main">
          <view class="artist-topline">
            <text class="artist-name">{{ artist.name }}</text>
            <text class="artist-badge">艺术家</text>
          </view>
          <text class="artist-desc">{{ artist.bio || artist.signature || '作品正在持续整理中' }}</text>
          <view class="artist-meta">
            <text>{{ artist.worksCount }} 件作品</text>
            <text v-if="artist.uid">UID {{ artist.uid }}</text>
          </view>
        </view>
        <text class="artist-arrow">›</text>
      </view>

      <view class="load-more" v-if="artists.length > 0">
        <text v-if="loading">加载中...</text>
        <text v-else-if="noMore">没有更多了</text>
        <text v-else>上滑加载更多</text>
      </view>

      <view class="empty-state" v-if="!loading && artists.length === 0">
        <text class="empty-icon">🎨</text>
        <text class="empty-text">暂无艺术家</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { getProductList, getRecommend } from '@/api/product.js'

export default {
  data() {
    return {
      keyword: '',
      artists: [],
      page: 1,
      pageSize: 30,
      loading: false,
      refreshing: false,
      noMore: false
    }
  },

  onLoad() {
    this.loadArtists(true)
  },

  methods: {
    async loadArtists(reset = false) {
      if (this.loading) return
      if (reset) {
        this.page = 1
        this.noMore = false
      }

      this.loading = true
      try {
        const params = {
          page: this.page,
          pageSize: this.pageSize,
          authorName: this.keyword || undefined
        }
        let data = await getProductList(params)
        let works = data?.records || data || []
        if (!works.length && !this.keyword) {
          data = await getRecommend({ page: this.page, pageSize: this.pageSize })
          works = data?.records || data || []
        }

        const nextArtists = this.buildArtists(works)
        if (reset) {
          this.artists = nextArtists
        } else {
          const map = new Map(this.artists.map(item => [String(item.id), item]))
          nextArtists.forEach(item => {
            const key = String(item.id)
            const old = map.get(key)
            map.set(key, old ? { ...old, worksCount: old.worksCount + item.worksCount } : item)
          })
          this.artists = Array.from(map.values())
        }

        if (works.length < this.pageSize) {
          this.noMore = true
        } else {
          this.page += 1
        }
      } catch (e) {
        console.error('加载艺术家列表失败', e)
      } finally {
        this.loading = false
      }
    },

    buildArtists(works) {
      const map = new Map()
      works.forEach(work => {
        const id = work.authorId || work.artistId
        const name = work.authorName || work.artistName
        if (!id || !name) return
        const key = String(id)
        const current = map.get(key) || {
          id,
          name,
          uid: work.authorUid || work.artistUid || work.displayAuthorId,
          avatar: work.authorAvatar,
          cover: work.cover || work.coverImage,
          bio: work.authorBio,
          signature: work.authorBio,
          worksCount: 0
        }
        current.worksCount += 1
        if (!current.cover && (work.cover || work.coverImage)) current.cover = work.cover || work.coverImage
        map.set(key, current)
      })
      return Array.from(map.values())
    },

    onRefresh() {
      this.refreshing = true
      this.loadArtists(true).finally(() => {
        this.refreshing = false
      })
    },

    loadMore() {
      if (!this.noMore) this.loadArtists(false)
    },

    goArtist(artist) {
      uni.navigateTo({ url: `/pages/artist/home?id=${artist.id}` })
    }
  }
}
</script>

<style lang="scss" scoped>
$bg-primary: #0d0d0d;
$bg-card: #1b1b1b;
$bg-elevated: #252525;
$text-primary: #fff;
$text-secondary: #b8b8b8;
$text-muted: #777;
$accent-gold: #d4af37;

.artist-list-page {
  min-height: 100vh;
  background: $bg-primary;
  color: $text-primary;
}

.search-panel {
  position: sticky;
  top: 0;
  z-index: 10;
  padding: 18rpx 24rpx;
  background: rgba(13, 13, 13, 0.96);
}

.search-box {
  display: flex;
  align-items: center;
  height: 72rpx;
  padding: 0 24rpx;
  background: $bg-card;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 36rpx;
}

.search-icon {
  color: $text-muted;
  font-size: 34rpx;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  color: $text-primary;
  font-size: 28rpx;
}

.input-placeholder {
  color: $text-muted;
}

.artist-scroll {
  height: calc(100vh - 110rpx);
  padding: 0 24rpx 40rpx;
  box-sizing: border-box;
}

.artist-card {
  display: flex;
  align-items: center;
  min-height: 160rpx;
  padding: 24rpx;
  margin-bottom: 18rpx;
  background: $bg-card;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
  border-radius: 18rpx;
}

.artist-avatar {
  width: 108rpx;
  height: 108rpx;
  border-radius: 54rpx;
  margin-right: 22rpx;
  background: $bg-elevated;
}

.artist-main {
  flex: 1;
  min-width: 0;
}

.artist-topline {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 10rpx;
}

.artist-name {
  max-width: 330rpx;
  color: $text-primary;
  font-size: 32rpx;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.artist-badge {
  flex-shrink: 0;
  padding: 4rpx 12rpx;
  color: #151515;
  background: $accent-gold;
  border-radius: 8rpx;
  font-size: 20rpx;
}

.artist-desc {
  display: block;
  color: $text-secondary;
  font-size: 24rpx;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.artist-meta {
  display: flex;
  gap: 18rpx;
  margin-top: 12rpx;
  color: $text-muted;
  font-size: 22rpx;
}

.artist-arrow {
  margin-left: 16rpx;
  color: $accent-gold;
  font-size: 42rpx;
}

.load-more,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 0;
  color: $text-muted;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 16rpx;
}

.empty-text {
  font-size: 26rpx;
}
</style>
