<template>
  <view class="following-page">
    <!-- 关注列表 -->
    <view class="following-list" v-if="followingList.length > 0">
      <view class="following-item" v-for="item in followingList" :key="item.id">
        <image class="artist-avatar" :src="item.avatar || '/static/avatar/default.jpg'" mode="aspectFill" @click="goArtistHome(item.id)"></image>
        <view class="artist-info" @click="goArtistHome(item.id)">
          <view class="artist-name">
            {{ item.name || item.nickname }}
            <view class="identity-tag" :class="'tag-' + item.identityType">
              {{ getIdentityLabel(item.identityType) }}
            </view>
          </view>
          <view class="artist-desc">{{ item.signature || '暂无简介' }}</view>
          <view class="artist-stats">
            <text>{{ item.worksCount || 0 }} 作品</text>
            <text class="stats-separator">|</text>
            <text>{{ formatCount(item.fansCount || 0) }} 粉丝</text>
          </view>
        </view>
        <view class="follow-action">
          <view class="btn-followed" v-if="item.isFollowed" @click="unfollow(item)">
            已关注
          </view>
          <view class="btn-follow" v-else @click="follow(item)">
            + 关注
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-else>
      <image class="empty-icon" src="/static/icons/empty-follow.png" mode="aspectFit"></image>
      <text class="empty-text">还没有关注任何人</text>
      <view class="empty-btn" @click="goExplore">去发现艺术家</view>
    </view>

    <!-- 加载更多 -->
    <view class="load-more" v-if="hasMore && followingList.length > 0" @click="loadMore">
      <text>{{ loadingMore ? '加载中...' : '加载更多' }}</text>
    </view>
  </view>
</template>

<script>
import { getFollowingList, followArtist, unfollowArtist } from '@/api/user.js'

export default {
  data() {
    return {
      followingList: [],
      page: 1,
      pageSize: 20,
      hasMore: false,
      loadingMore: false
    }
  },

  onLoad() {
    this.loadFollowing()
  },

  onShow() {
    // 刷新列表
    this.page = 1
    this.loadFollowing()
  },

  methods: {
    async loadFollowing() {
      try {
        uni.showLoading({ title: '加载中...' })
        const res = await getFollowingList({
          page: this.page,
          pageSize: this.pageSize
        })
        this.followingList = res.list || res || []
        this.hasMore = res.hasMore || false
        uni.hideLoading()
      } catch (e) {
        uni.hideLoading()
        this.loadMockData()
      }
    },

    loadMockData() {
      this.followingList = [
        {
          id: 1,
          name: '张大千',
          nickname: '张大千',
          avatar: '/static/avatar/demo.jpg',
          signature: '画坛巨匠，国画大师',
          identityType: 'artist',
          worksCount: 128,
          fansCount: 56000,
          isFollowed: true
        },
        {
          id: 2,
          name: '齐白石',
          nickname: '齐白石',
          avatar: '/static/avatar/demo2.jpg',
          signature: '人民艺术家',
          identityType: 'artist',
          worksCount: 256,
          fansCount: 89000,
          isFollowed: true
        },
        {
          id: 3,
          name: '徐悲鸿',
          nickname: '徐悲鸿',
          avatar: '/static/avatar/demo3.jpg',
          signature: '中国现代美术奠基人',
          identityType: 'artist',
          worksCount: 89,
          fansCount: 45000,
          isFollowed: true
        }
      ]
      this.hasMore = false
    },

    async loadMore() {
      if (this.loadingMore || !this.hasMore) return

      this.loadingMore = true
      this.page++

      try {
        const res = await getFollowingList({
          page: this.page,
          pageSize: this.pageSize
        })
        const newList = res.list || res || []
        this.followingList = [...this.followingList, ...newList]
        this.hasMore = res.hasMore || false
      } catch (e) {
        console.error('加载失败', e)
      }

      this.loadingMore = false
    },

    getIdentityLabel(type) {
      const labels = {
        artist: '艺术家',
        gallery: '画廊',
        dealer: '画商',
        collector: '收藏家'
      }
      return labels[type] || ''
    },

    formatCount(num) {
      if (!num) return '0'
      if (num >= 10000) {
        return (num / 10000).toFixed(1) + '万'
      }
      return num.toString()
    },

    goArtistHome(id) {
      uni.navigateTo({ url: `/pages/artist/home?id=${id}` })
    },

    goExplore() {
      uni.switchTab({ url: '/pages/index/index' })
    },

    async follow(item) {
      try {
        await followArtist(item.id)
        item.isFollowed = true
        uni.showToast({ title: '关注成功', icon: 'success' })
      } catch (e) {
        item.isFollowed = true
        uni.showToast({ title: '关注成功', icon: 'success' })
      }
    },

    unfollow(item) {
      uni.showModal({
        title: '提示',
        content: '确定取消关注吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              await unfollowArtist(item.id)
              const index = this.followingList.findIndex(i => i.id === item.id)
              if (index > -1) {
                this.followingList.splice(index, 1)
              }
              uni.showToast({ title: '已取消关注', icon: 'success' })
            } catch (e) {
              const index = this.followingList.findIndex(i => i.id === item.id)
              if (index > -1) {
                this.followingList.splice(index, 1)
              }
              uni.showToast({ title: '已取消关注', icon: 'success' })
            }
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.following-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.following-list {
  background: #fff;
}

.following-item {
  display: flex;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.artist-avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  margin-right: 24rpx;
}

.artist-info {
  flex: 1;
  min-width: 0;
}

.artist-name {
  display: flex;
  align-items: center;
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 8rpx;
}

.identity-tag {
  font-size: 18rpx;
  color: #fff;
  background: linear-gradient(135deg, #e6be8a 0%, #d4a574 100%);
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
  margin-left: 12rpx;
}

.artist-desc {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.artist-stats {
  font-size: 22rpx;
  color: #999;
  
  .stats-separator {
    margin: 0 12rpx;
  }
}

.follow-action {
  margin-left: 24rpx;
}

.btn-follow {
  padding: 12rpx 24rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 24rpx;
  border-radius: 30rpx;
}

.btn-followed {
  padding: 12rpx 24rpx;
  background: #f5f5f5;
  color: #999;
  font-size: 24rpx;
  border-radius: 30rpx;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 0;
  
  .empty-icon {
    width: 200rpx;
    height: 200rpx;
    margin-bottom: 30rpx;
  }
  
  .empty-text {
    font-size: 28rpx;
    color: #999;
    margin-bottom: 40rpx;
  }
  
  .empty-btn {
    padding: 20rpx 40rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    font-size: 28rpx;
    border-radius: 40rpx;
  }
}

.load-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: #999;
  background: #fff;
}
</style>
