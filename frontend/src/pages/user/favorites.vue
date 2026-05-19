<template>
  <view class="favorites-page">
    <!-- 顶部Tab -->
    <view class="tab-bar">
      <view
        class="tab-item"
        :class="{ active: currentTab === 'works' }"
        @click="switchTab('works')"
      >
        <text>作品</text>
        <view class="tab-line" v-if="currentTab === 'works'"></view>
      </view>
      <view
        class="tab-item"
        :class="{ active: currentTab === 'artists' }"
        @click="switchTab('artists')"
      >
        <text>艺术家</text>
        <view class="tab-line" v-if="currentTab === 'artists'"></view>
      </view>
    </view>

    <!-- 作品收藏列表 -->
    <view class="works-list" v-if="currentTab === 'works'">
      <view class="list-header">
        <text class="list-count">共 {{ worksList.length }} 件喜欢</text>
        <view class="batch-actions" v-if="worksList.length > 0">
          <text @click="toggleEditMode">{{ isEditMode ? '完成' : '管理' }}</text>
        </view>
      </view>

      <view class="works-grid" v-if="worksList.length > 0 && !loadingWorks">
        <view class="work-item" v-for="item in worksList" :key="item.id">
          <view class="edit-check" v-if="isEditMode" @click="toggleSelect(item.id)">
            
          </view>
          <image class="work-cover" :src="item.cover" mode="aspectFill" @click="goDetail(item.id)"></image>
          <view class="work-info">
            <view class="work-title" @click="goDetail(item.id)">{{ item.title }}</view>
            <view class="work-meta">
              <text class="artist">{{ item.artistName }}</text>
              <text class="price" v-if="item.price">¥{{ formatPrice(item.price) }}</text>
            </view>
          </view>
          <view class="work-actions" v-if="!isEditMode">
            <view class="action-btn" @click="shareWork(item)">
              <text>↗</text>
            </view>
            <view class="action-btn" @click="cancelFavorite(item)">
              <text>❤</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 加载中 -->
      <view class="loading-state" v-if="loadingWorks">
        <text>加载中...</text>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-else-if="worksList.length === 0 && !loadingWorks">
        <image class="empty-icon" src="/static/icons/heart-empty.png" mode="aspectFit"></image>
        <text class="empty-text">还没有喜欢任何作品</text>
        <view class="empty-btn" @click="goGallery">去逛逛</view>
      </view>
    </view>

    <!-- 艺术家收藏列表 -->
    <view class="artists-list" v-if="currentTab === 'artists'">
      <view class="list-header">
        <text class="list-count">共 {{ artistsList.length }} 位关注</text>
      </view>

      <view class="artists-grid" v-if="artistsList.length > 0 && !loadingArtists">
        <view class="artist-item" v-for="item in artistsList" :key="item.id">
          <view class="artist-avatar" @click="goArtistHome(item.id)">
            <image :src="item.avatar" mode="aspectFill"></image>
          </view>
          <view class="artist-info">
            <view class="artist-name">{{ item.name }}</view>
            <view class="artist-meta">
              <text>{{ item.worksCount }}作品</text>
              <text class="dot">·</text>
              <text>{{ item.fansCount }}粉丝</text>
            </view>
          </view>
          <view class="follow-btn" :class="{ following: item.isFollowing }" @click="toggleFollow(item)">
            {{ item.isFollowing ? '已关注' : '关注' }}
          </view>
        </view>
      </view>

      <!-- 加载中 -->
      <view class="loading-state" v-if="loadingArtists">
        <text>加载中...</text>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-else-if="artistsList.length === 0 && !loadingArtists">
        <image class="empty-icon" src="/static/icons/user-empty.png" mode="aspectFit"></image>
        <text class="empty-text">还没有关注任何艺术家</text>
        <view class="empty-btn" @click="goGallery">去发现</view>
      </view>
    </view>

    <!-- 底部批量操作栏 -->
    <view class="batch-bar" v-if="isEditMode && selectedList.length > 0">
      <view class="batch-info">
        <text>已选择 {{ selectedList.length }} 件</text>
      </view>
      <view class="batch-actions">
        <view class="batch-btn cancel" @click="cancelBatch">取消喜欢</view>
      </view>
    </view>
  </view>
</template>

<script>
import { getFavorites as getProductFavorites, removeFavorite } from '@/api/product'
import { getFollowingList, followArtist, unfollowArtist } from '@/api/user'
import { getAccessToken, isGuestUser } from '@/utils/auth'

const normalizeImage = (url) => {
  if (!url || typeof url !== 'string') return ''
  const t = url.trim()
  if (!t || t === '[]' || t === '{}') return ''
  if (t.startsWith('http://') || t.startsWith('https://')) return t
  if (t.startsWith('/')) {
    const app = getApp()
    const domain = app?.globalData?.fileDomain || app?.globalData?.domain || ''
    return domain ? domain + t : t
  }
  return t
}

export default {
  data() {
    return {
      currentTab: 'works',
      isEditMode: false,
      selectedList: [],
      worksList: [],
      artistsList: [],
      loadingWorks: false,
      loadingArtists: false,
      worksPage: 1,
      artistsPage: 1,
      hasMoreWorks: true,
      hasMoreArtists: true
    }
  },

  onLoad() {
    this.checkLoginAndLoad()
  },

  onShow() {
    if (this.currentTab === 'works') {
      this.loadWorks()
    } else {
      this.loadArtists()
    }
  },

  methods: {
    checkLoginAndLoad() {
      if (!getAccessToken() || isGuestUser()) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      this.loadWorks()
    },

    async loadWorks(append = false) {
      if (this.loadingWorks) return
      this.loadingWorks = true
      try {
        const page = append ? this.worksPage : 1
        const data = await getProductFavorites({ page, pageSize: 20 })
        const list = Array.isArray(data) ? data : (data.records || [])
        const normalized = list.map(item => ({
          id: item.id || item.artworkId,
          title: item.title || item.name || '未命名',
          cover: normalizeImage(item.coverImage || item.cover),
          artistName: item.artistName || item.authorName || '未知',
          artistId: item.artistId || item.authorId,
          price: item.price || item.currentPrice || 0
        }))
        this.worksList = append ? this.worksList.concat(normalized) : normalized
        this.worksPage = page + 1
        this.hasMoreWorks = list.length >= 20
      } catch (e) {
        console.warn('[喜欢列表] 加载失败:', e)
        if (!append) this.worksList = []
      } finally {
        this.loadingWorks = false
      }
    },

    async loadArtists(append = false) {
      if (this.loadingArtists) return
      this.loadingArtists = true
      try {
        const page = append ? this.artistsPage : 1
        const data = await getFollowingList({ page, pageSize: 20 })
        const list = Array.isArray(data) ? data : (data.records || [])
        const normalized = list.map(item => ({
          id: item.id || item.userId,
          avatar: normalizeImage(item.avatar || item.headimgurl),
          name: item.name || item.nickname || item.realName || '未知',
          worksCount: item.workCount || item.artworkCount || 0,
          fansCount: item.fansCount || item.followerCount || 0,
          isFollowing: true
        }))
        this.artistsList = append ? this.artistsList.concat(normalized) : normalized
        this.artistsPage = page + 1
        this.hasMoreArtists = list.length >= 20
      } catch (e) {
        console.warn('[关注列表] 加载失败:', e)
        if (!append) this.artistsList = []
      } finally {
        this.loadingArtists = false
      }
    },

    formatPrice(price) {
      if (!price) return ''
      const yuan = Math.round(Number(price) / 100)
      return yuan.toLocaleString()
    },

    switchTab(tab) {
      this.currentTab = tab
      this.isEditMode = false
      this.selectedList = []
      if (tab === 'works' && !this.worksList.length) {
        this.loadWorks()
      } else if (tab === 'artists' && !this.artistsList.length) {
        this.loadArtists()
      }
    },

    toggleEditMode() {
      this.isEditMode = !this.isEditMode
      if (!this.isEditMode) this.selectedList = []
    },

    toggleSelect(id) {
      const i = this.selectedList.indexOf(id)
      if (i > -1) this.selectedList.splice(i, 1)
      else this.selectedList.push(id)
    },

    goDetail(id) {
      if (!this.isEditMode) {
        uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` })
      }
    },

    goArtistHome(id) {
      if (!this.isEditMode) {
        uni.navigateTo({ url: `/pages/artist/home?id=${id}` })
      }
    },

    shareWork() {
      uni.showShareMenu({ withShareTicket: true })
    },

    async cancelFavorite(item) {
      try {
        await removeFavorite(item.id)
        const i = this.worksList.findIndex(w => w.id === item.id)
        if (i > -1) this.worksList.splice(i, 1)
        uni.showToast({ title: '已取消喜欢', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: '操作失败', icon: 'none' })
      }
    },

    cancelBatch() {
      uni.showModal({
        title: '提示',
        content: `确定取消喜欢选中的 ${this.selectedList.length} 件作品？`,
        success: async (res) => {
          if (!res.confirm) return
          await Promise.all(
            this.selectedList.map(id =>
              removeFavorite(id).catch(e =>
                console.warn('[批量取消] 失败 id=' + id, e)
              )
            )
          )
          this.worksList = this.worksList.filter(w => !this.selectedList.includes(w.id))
          this.selectedList = []
          this.isEditMode = false
          uni.showToast({ title: '已取消喜欢', icon: 'success' })
        }
      })
    },

    async toggleFollow(item) {
      const prev = item.isFollowing
      try {
        if (item.isFollowing) {
          await unfollowArtist(item.id)
          const i = this.artistsList.findIndex(a => a.id === item.id)
          if (i > -1) this.artistsList.splice(i, 1)
          uni.showToast({ title: '已取消关注', icon: 'success' })
        } else {
          await followArtist(item.id)
          item.isFollowing = true
          uni.showToast({ title: '关注成功', icon: 'success' })
        }
      } catch (e) {
        item.isFollowing = prev
        uni.showToast({ title: '操作失败', icon: 'none' })
      }
    },

    goGallery() {
      uni.switchTab({ url: '/pages/gallery/index' })
    }
  }
}
</script>

<style lang="scss" scoped>
.favorites-page {
  min-height: 100vh;
  background: #f5f6f7;
  padding-bottom: 100rpx;
}

.tab-bar {
  display: flex;
  background: #fff;
  padding: 0 60rpx;

  .tab-item {
    flex: 1;
    position: relative;
    padding: 28rpx 0;
    text-align: center;

    text {
      font-size: 30rpx;
      color: #666;
    }

    &.active text {
      color: #333;
      font-weight: 600;
    }

    .tab-line {
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 60rpx;
      height: 6rpx;
      background: #667eea;
      border-radius: 3rpx;
    }
  }
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 30rpx;

  .list-count {
    font-size: 26rpx;
    color: #999;
  }

  .batch-actions text {
    font-size: 26rpx;
    color: #667eea;
  }
}

.works-grid {
  display: grid;
  grid-template-columns: 48% 48%;
  gap: 20rpx;
  padding: 0 20rpx;

  .work-item {
    background: #fff;
    border-radius: 16rpx;
    overflow: hidden;
    position: relative;

    .edit-check {
      position: absolute;
      top: 16rpx;
      left: 16rpx;
      z-index: 10;
      width: 44rpx;
      height: 44rpx;
      background: rgba(255, 255, 255, 0.9);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .work-cover {
      width: 100%;
      height: 340rpx;
    }

    .work-info {
      padding: 20rpx;

      .work-title {
        font-size: 28rpx;
        color: #333;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .work-meta {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 12rpx;

        .artist {
          font-size: 24rpx;
          color: #999;
        }

        .price {
          font-size: 28rpx;
          color: #ff4d4f;
          font-weight: 600;
        }
      }
    }

    .work-actions {
      display: flex;
      justify-content: flex-end;
      padding: 0 20rpx 16rpx;

      .action-btn {
        margin-left: 20rpx;
        padding: 8rpx;
      }
    }
  }
}

.artists-grid {
  padding: 0 20rpx;

  .artist-item {
    display: flex;
    align-items: center;
    padding: 24rpx;
    background: #fff;
    border-radius: 16rpx;
    margin-bottom: 20rpx;

    .artist-avatar {
      width: 100rpx;
      height: 100rpx;
      border-radius: 50%;
      overflow: hidden;

      image {
        width: 100%;
        height: 100%;
      }
    }

    .artist-info {
      flex: 1;
      margin-left: 24rpx;

      .artist-name {
        font-size: 30rpx;
        font-weight: 600;
        color: #333;
      }

      .artist-meta {
        font-size: 24rpx;
        color: #999;
        margin-top: 8rpx;

        .dot {
          margin: 0 8rpx;
        }
      }
    }

    .follow-btn {
      padding: 12rpx 32rpx;
      font-size: 26rpx;
      color: #667eea;
      background: rgba(102, 126, 234, 0.1);
      border-radius: 32rpx;

      &.following {
        color: #999;
        background: #f5f5f5;
      }
    }
  }
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 120rpx 0;
  font-size: 28rpx;
  color: #999;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;

  .empty-icon {
    width: 200rpx;
    height: 200rpx;
  }

  .empty-text {
    margin-top: 32rpx;
    font-size: 28rpx;
    color: #999;
  }

  .empty-btn {
    margin-top: 32rpx;
    padding: 20rpx 60rpx;
    font-size: 28rpx;
    color: #667eea;
    border: 2rpx solid #667eea;
    border-radius: 40rpx;
  }
}

.batch-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2rpx 20rpx rgba(0, 0, 0, 0.1);

  .batch-info text {
    font-size: 28rpx;
    color: #333;
  }

  .batch-btn {
    padding: 16rpx 40rpx;
    font-size: 28rpx;
    border-radius: 32rpx;

    &.cancel {
      color: #fff;
      background: #ff4d4f;
    }
  }
}
</style>
