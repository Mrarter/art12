<template>
  <view class="browse-history">
    <!-- Tab切换 -->
    <view class="tab-bar">
      <view class="tab-item" 
            :class="{ active: currentTab === 'works' }" 
            @click="switchTab('works')">
        作品足迹
      </view>
      <view class="tab-item" 
            :class="{ active: currentTab === 'artists' }" 
            @click="switchTab('artists')">
        艺术家足迹
      </view>
    </view>

    <!-- 作品列表 -->
    <scroll-view class="content-list" scroll-y v-if="currentTab === 'works'">
      <view class="time-section" v-for="(group, date) in worksGrouped" :key="date">
        <view class="time-header">{{ date }}</view>
        <view class="item-card" v-for="(item, index) in group" :key="index" @click="viewWork(item)">
          <image class="item-image" :src="item.image" mode="aspectFill" />
          <view class="item-info">
            <text class="item-name">{{ item.name }}</text>
            <text class="item-author">{{ item.author }}</text>
            <text class="item-price">¥{{ item.price }}</text>
          </view>
          <view class="item-actions">
            <text class="action-btn" @click.stop="addToCart(item)">加购</text>
            <text class="action-btn" @click.stop="removeItem(item)">删除</text>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="Object.keys(worksGrouped).length === 0">
        <image class="empty-icon" src="/static/empty-history.png" mode="aspectFit" />
        <text class="empty-text">暂无浏览记录</text>
        <text class="empty-hint">快去逛逛吧</text>
      </view>
    </scroll-view>

    <!-- 艺术家列表 -->
    <scroll-view class="content-list" scroll-y v-if="currentTab === 'artists'">
      <view class="artist-card" v-for="(artist, index) in artistsList" :key="index" @click="viewArtist(artist)">
        <image class="artist-avatar" :src="artist.avatar" mode="aspectFill" />
        <view class="artist-info">
          <text class="artist-name">{{ artist.name }}</text>
          <text class="artist-tags">
            <text class="tag" v-for="(tag, idx) in artist.tags" :key="idx">{{ tag }}</text>
          </text>
          <text class="artist-desc">{{ artist.intro }}</text>
        </view>
        <view class="artist-action">
          <text class="follow-btn" :class="{ following: artist.isFollowing }">
            {{ artist.isFollowing ? '已关注' : '关注' }}
          </text>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="artistsList.length === 0">
        <image class="empty-icon" src="/static/empty-history.png" mode="aspectFit" />
        <text class="empty-text">暂无艺术家足迹</text>
        <text class="empty-hint">快去关注喜欢的艺术家吧</text>
      </view>
    </scroll-view>

    <!-- 底部操作 -->
    <view class="bottom-bar" v-if="hasItems">
      <view class="select-all" @click="toggleSelectAll">
        <checkbox :checked="isSelectAll" />
        <text class="select-text">全选</text>
      </view>
      <view class="bottom-actions">
        <text class="clear-btn" @click="clearHistory">清空</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      currentTab: 'works',
      isSelectAll: false,
      worksGrouped: {},
      artistsList: []
    }
  },
  computed: {
    hasItems() {
      return Object.keys(this.worksGrouped).length > 0
    }
  },
  onShow() {
    this.loadHistory()
  },
  methods: {
    loadHistory() {
      const works = uni.getStorageSync('browseHistoryWorks') || []
      const groups = {}
      works.forEach(item => {
        const label = this.getDateLabel(item.time)
        if (!groups[label]) groups[label] = []
        groups[label].push({
          ...item,
          price: this.formatPrice(item.price),
          image: item.image || '/static/artwork-placeholder.png'
        })
      })
      this.worksGrouped = groups
      this.artistsList = (uni.getStorageSync('browseHistoryArtists') || []).map(item => ({
        ...item,
        avatar: item.avatar || '/static/avatar/default.png',
        tags: item.tags && item.tags.length ? item.tags : ['艺术家']
      }))
    },
    getDateLabel(time) {
      const value = Number(time || Date.now())
      const today = new Date()
      const target = new Date(value)
      const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime()
      const targetStart = new Date(target.getFullYear(), target.getMonth(), target.getDate()).getTime()
      const diff = Math.floor((todayStart - targetStart) / 86400000)
      if (diff <= 0) return '今天'
      if (diff === 1) return '昨天'
      return `${diff}天前`
    },
    formatPrice(price) {
      const yuan = Number(price || 0) / 100
      if (yuan >= 10000) return `${(yuan / 10000).toFixed(yuan % 10000 === 0 ? 0 : 1)}万`
      return yuan.toLocaleString()
    },
    switchTab(tab) {
      this.currentTab = tab
    },
    viewWork(item) {
      uni.navigateTo({ url: `/pages/gallery/detail?id=${item.id}` })
    },
    viewArtist(artist) {
      uni.navigateTo({ url: `/pages/artist/home?id=${artist.id}` })
    },
    addToCart(item) {
      uni.showToast({ title: '已加入购物车', icon: 'success' })
    },
    removeItem(item) {
      uni.showModal({
        title: '提示',
        content: '确定删除这条浏览记录？',
        success: (res) => {
          if (res.confirm) {
            uni.showToast({ title: '已删除', icon: 'success' })
          }
        }
      })
    },
    toggleSelectAll() {
      this.isSelectAll = !this.isSelectAll
    },
    clearHistory() {
      uni.showModal({
        title: '清空浏览记录',
        content: '确定清空所有浏览记录？',
        success: (res) => {
          if (res.confirm) {
            this.worksGrouped = {}
            this.artistsList = []
            uni.removeStorageSync('browseHistoryWorks')
            uni.removeStorageSync('browseHistoryArtists')
            uni.showToast({ title: '已清空', icon: 'success' })
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.browse-history {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 120rpx;
}

.tab-bar {
  background: #fff;
  display: flex;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 30rpx 0;
  font-size: 28rpx;
  color: #666;
  position: relative;
}

.tab-item.active {
  color: #667eea;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60rpx;
  height: 6rpx;
  background: #667eea;
  border-radius: 3rpx;
}

.content-list {
  padding: 24rpx;
  height: calc(100vh - 100rpx);
}

.time-section {
  margin-bottom: 30rpx;
}

.time-header {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 16rpx;
  padding-left: 10rpx;
}

.item-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
  display: flex;
  align-items: center;
}

.item-image {
  width: 140rpx;
  height: 140rpx;
  border-radius: 12rpx;
  margin-right: 20rpx;
}

.item-info {
  flex: 1;
}

.item-name {
  font-size: 28rpx;
  color: #333;
  display: block;
  margin-bottom: 8rpx;
}

.item-author {
  font-size: 24rpx;
  color: #999;
  display: block;
  margin-bottom: 8rpx;
}

.item-price {
  font-size: 28rpx;
  color: #e63946;
  font-weight: 600;
}

.item-actions {
  display: flex;
  gap: 12rpx;
}

.action-btn {
  font-size: 22rpx;
  padding: 10rpx 16rpx;
  background: #f5f5f5;
  color: #666;
  border-radius: 8rpx;
}

.artist-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  display: flex;
  align-items: center;
}

.artist-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  margin-right: 20rpx;
}

.artist-info {
  flex: 1;
}

.artist-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 8rpx;
}

.artist-tags {
  display: flex;
  gap: 8rpx;
  margin-bottom: 8rpx;
}

.tag {
  font-size: 22rpx;
  background: #f0f0f0;
  color: #666;
  padding: 4rpx 12rpx;
  border-radius: 4rpx;
}

.artist-desc {
  font-size: 24rpx;
  color: #999;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.follow-btn {
  padding: 12rpx 24rpx;
  background: #667eea;
  color: #fff;
  font-size: 24rpx;
  border-radius: 30rpx;
}

.follow-btn.following {
  background: #f0f0f0;
  color: #999;
}

.empty-state {
  text-align: center;
  padding: 100rpx 0;
}

.empty-icon {
  width: 200rpx;
  height: 200rpx;
  opacity: 0.5;
}

.empty-text {
  font-size: 28rpx;
  color: #666;
  display: block;
  margin-top: 20rpx;
}

.empty-hint {
  font-size: 24rpx;
  color: #999;
  display: block;
  margin-top: 8rpx;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.select-all {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.select-text {
  font-size: 28rpx;
  color: #333;
}

.clear-btn {
  font-size: 28rpx;
  color: #e63946;
  padding: 12rpx 24rpx;
}
</style>
