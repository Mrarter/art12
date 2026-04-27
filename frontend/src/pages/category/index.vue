<template>
  <view class="category-page">
    <!-- 左侧分类导航 -->
    <scroll-view class="category-nav" scroll-y>
      <view 
        class="nav-item" 
        :class="{ active: currentIndex === index }"
        v-for="(item, index) in categories" 
        :key="item.id"
        @click="selectCategory(index)"
      >
        <image class="nav-icon" :src="item.icon" mode="aspectFit"></image>
        <text class="nav-text">{{ item.name }}</text>
        <view class="active-indicator" v-if="currentIndex === index"></view>
      </view>
    </scroll-view>

    <!-- 右侧内容 -->
    <scroll-view class="category-content" scroll-y @scrolltolower="loadMore">
      <!-- 子分类 -->
      <view class="sub-categories" v-if="currentCategory && currentCategory.children">
        <view 
          class="sub-item"
          v-for="sub in currentCategory.children"
          :key="sub.id"
          @click="goList(sub)"
        >
          <image class="sub-icon" :src="sub.icon" mode="aspectFit"></image>
          <text class="sub-name">{{ sub.name }}</text>
        </view>
      </view>

      <!-- 推荐品牌/艺术家 -->
      <view class="recommended-section" v-if="recommendedArtists.length > 0">
        <view class="section-title">
          <text>热门艺术家</text>
          <text class="more-btn" @click="goMore">查看更多 ></text>
        </view>
        <scroll-view class="artist-scroll" scroll-x>
          <view 
            class="artist-card"
            v-for="artist in recommendedArtists"
            :key="artist.id"
            @click="goArtist(artist.id)"
          >
            <image class="artist-avatar" :src="artist.avatar" mode="aspectFill"></image>
            <text class="artist-name">{{ artist.name }}</text>
            <text class="artist-fans">{{ artist.fansCount }}粉丝</text>
          </view>
        </scroll-view>
      </view>

      <!-- 热门作品 -->
      <view class="hot-works-section" v-if="hotWorks.length > 0">
        <view class="section-title">
          <text>热门作品</text>
          <text class="more-btn" @click="goMore">查看更多 ></text>
        </view>
        <view class="works-grid">
          <view 
            class="work-item"
            v-for="work in hotWorks"
            :key="work.id"
            @click="goDetail(work.id)"
          >
            <image class="work-image" :src="work.cover" mode="aspectFill"></image>
            <view class="work-info">
              <text class="work-title">{{ work.title }}</text>
              <text class="work-artist">{{ work.artistName }}</text>
              <text class="work-price">¥{{ formatPrice(work.price) }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 加载状态 -->
      <view class="loading-more" v-if="loading">
        <text>加载中...</text>
      </view>
      <view class="no-more" v-if="!hasMore && hotWorks.length > 0">
        <text>没有更多了</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      currentIndex: 0,
      categories: [
        { 
          id: 1, 
          name: '国画', 
          icon: '/static/icons/category-guohua.png',
          children: [
            { id: 11, name: '人物', icon: '/static/icons/sub-person.png' },
            { id: 12, name: '山水', icon: '/static/icons/sub-landscape.png' },
            { id: 13, name: '花鸟', icon: '/static/icons/sub-bird.png' },
            { id: 14, name: '书法', icon: '/static/icons/sub-calligraphy.png' }
          ]
        },
        { 
          id: 2, 
          name: '油画', 
          icon: '/static/icons/category-youhua.png',
          children: [
            { id: 21, name: '人物', icon: '/static/icons/sub-person.png' },
            { id: 22, name: '风景', icon: '/static/icons/sub-landscape.png' },
            { id: 23, name: '静物', icon: '/static/icons/sub-still.png' },
            { id: 24, name: '抽象', icon: '/static/icons/sub-abstract.png' }
          ]
        },
        { 
          id: 3, 
          name: '版画', 
          icon: '/static/icons/category-banhua.png',
          children: [
            { id: 31, name: '木版画', icon: '/static/icons/sub-wood.png' },
            { id: 32, name: '石版画', icon: '/static/icons/sub-stone.png' },
            { id: 33, name: '铜版画', icon: '/static/icons/sub-copper.png' }
          ]
        },
        { 
          id: 4, 
          name: '雕塑', 
          icon: '/static/icons/category-diaosu.png',
          children: [
            { id: 41, name: '石雕', icon: '/static/icons/sub-stone.png' },
            { id: 42, name: '木雕', icon: '/static/icons/sub-wood.png' },
            { id: 43, name: '铜雕', icon: '/static/icons/sub-copper.png' }
          ]
        },
        { 
          id: 5, 
          name: '书法', 
          icon: '/static/icons/category-shufa.png',
          children: [
            { id: 51, name: '楷书', icon: '/static/icons/sub-calligraphy.png' },
            { id: 52, name: '行书', icon: '/static/icons/sub-calligraphy.png' },
            { id: 53, name: '草书', icon: '/static/icons/sub-calligraphy.png' },
            { id: 54, name: '隶书', icon: '/static/icons/sub-calligraphy.png' }
          ]
        },
        { 
          id: 6, 
          name: '摄影', 
          icon: '/static/icons/category-photo.png',
          children: [
            { id: 61, name: '风光', icon: '/static/icons/sub-landscape.png' },
            { id: 62, name: '人像', icon: '/static/icons/sub-person.png' },
            { id: 63, name: '纪实', icon: '/static/icons/sub-documentary.png' }
          ]
        },
        { 
          id: 7, 
          name: '当代艺术', 
          icon: '/static/icons/category-contemporary.png',
          children: [
            { id: 71, name: '装置', icon: '/static/icons/sub-install.png' },
            { id: 72, name: '行为艺术', icon: '/static/icons/sub-performance.png' },
            { id: 73, name: '影像', icon: '/static/icons/sub-video.png' }
          ]
        },
        { 
          id: 8, 
          name: '周边文创', 
          icon: '/static/icons/category-merch.png',
          children: [
            { id: 81, name: '装饰画', icon: '/static/icons/sub-decor.png' },
            { id: 82, name: '衍生品', icon: '/static/icons/sub-derivative.png' }
          ]
        }
      ],
      recommendedArtists: [],
      hotWorks: [],
      loading: false,
      hasMore: true,
      page: 1
    }
  },

  computed: {
    currentCategory() {
      return this.categories[this.currentIndex] || null
    }
  },

  onLoad() {
    this.loadRecommended()
    this.loadHotWorks()
  },

  methods: {
    selectCategory(index) {
      this.currentIndex = index
      this.hotWorks = []
      this.page = 1
      this.hasMore = true
      this.loadHotWorks()
    },

    loadRecommended() {
      // 模拟推荐艺术家
      this.recommendedArtists = [
        { id: 1, name: '张大千', avatar: '/static/icons/avatar-default.png', fansCount: 12580 },
        { id: 2, name: '齐白石', avatar: '/static/icons/avatar-default.png', fansCount: 9860 },
        { id: 3, name: '徐悲鸿', avatar: '/static/icons/avatar-default.png', fansCount: 8540 },
        { id: 4, name: '潘天寿', avatar: '/static/icons/avatar-default.png', fansCount: 7200 },
        { id: 5, name: '吴冠中', avatar: '/static/icons/avatar-default.png', fansCount: 6800 }
      ]
    },

    loadHotWorks() {
      if (this.loading) return
      this.loading = true

      setTimeout(() => {
        const newWorks = [
          { id: 1, title: '山水长卷', artistName: '张大千', price: 128000, cover: '/static/icons/artwork-default.png' },
          { id: 2, title: '虾趣图', artistName: '齐白石', price: 88000, cover: '/static/icons/artwork-default.png' },
          { id: 3, title: '奔马图', artistName: '徐悲鸿', price: 156000, cover: '/static/icons/artwork-default.png' },
          { id: 4, title: '松鹰图', artistName: '潘天寿', price: 98000, cover: '/static/icons/artwork-default.png' },
          { id: 5, title: '江南春', artistName: '吴冠中', price: 220000, cover: '/static/icons/artwork-default.png' },
          { id: 6, title: '荷花', artistName: '张大千', price: 158000, cover: '/static/icons/artwork-default.png' }
        ]
        this.hotWorks = [...this.hotWorks, ...newWorks]
        this.loading = false
        this.hasMore = this.page < 2
        this.page++
      }, 500)
    },

    loadMore() {
      if (!this.hasMore || this.loading) return
      this.loadHotWorks()
    },

    goList(sub) {
      uni.navigateTo({
        url: `/pages/gallery/index?category=${sub.id}&name=${sub.name}`
      })
    },

    goArtist(id) {
      uni.navigateTo({
        url: `/pages/artist/home?id=${id}`
      })
    },

    goDetail(id) {
      uni.navigateTo({
        url: `/pages/gallery/detail?id=${id}`
      })
    },

    goMore() {
      const category = this.currentCategory
      if (category) {
        uni.navigateTo({
          url: `/pages/gallery/index?category=${category.id}&name=${category.name}`
        })
      }
    },

    formatPrice(price) {
      if (!price) return '0'
      const yuan = price / 100  // 分转元
      if (yuan >= 10000) {
        return (yuan / 10000).toFixed(yuan % 10000 === 0 ? 0 : 1) + '万'
      }
      return yuan.toLocaleString()
    }
  }
}
</script>

<style lang="scss" scoped>
.category-page {
  display: flex;
  height: 100vh;
  background: #f5f5f5;
}

.category-nav {
  width: 180rpx;
  height: 100vh;
  background: #fff;
  border-right: 1rpx solid #f0f0f0;

  .nav-item {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 30rpx 0;
    color: #666;
    transition: all 0.3s;

    &.active {
      background: #f8f4ff;
      color: #667eea;
    }

    .nav-icon {
      width: 56rpx;
      height: 56rpx;
      margin-bottom: 12rpx;
    }

    .nav-text {
      font-size: 24rpx;
    }

    .active-indicator {
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 6rpx;
      height: 40rpx;
      background: #667eea;
      border-radius: 0 3rpx 3rpx 0;
    }
  }
}

.category-content {
  flex: 1;
  height: 100vh;
  padding: 20rpx;
}

.sub-categories {
  display: flex;
  flex-wrap: wrap;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;

  .sub-item {
    width: 25%;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20rpx 0;

    &:active {
      opacity: 0.7;
    }

    .sub-icon {
      width: 64rpx;
      height: 64rpx;
      margin-bottom: 12rpx;
      background: #f5f5f5;
      border-radius: 50%;
    }

    .sub-name {
      font-size: 24rpx;
      color: #333;
    }
  }
}

.recommended-section,
.hot-works-section {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;

  text:first-child {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
  }

  .more-btn {
    font-size: 24rpx;
    color: #999;
  }
}

.artist-scroll {
  display: flex;
  white-space: nowrap;

  .artist-card {
    display: inline-flex;
    flex-direction: column;
    align-items: center;
    margin-right: 30rpx;
    width: 140rpx;

    &:active {
      opacity: 0.7;
    }

    .artist-avatar {
      width: 100rpx;
      height: 100rpx;
      border-radius: 50%;
      margin-bottom: 12rpx;
      background: #f0f0f0;
    }

    .artist-name {
      font-size: 26rpx;
      color: #333;
      margin-bottom: 4rpx;
    }

    .artist-fans {
      font-size: 22rpx;
      color: #999;
    }
  }
}

.works-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;

  .work-item {
    background: #fafafa;
    border-radius: 12rpx;
    overflow: hidden;

    &:active {
      opacity: 0.8;
    }

    .work-image {
      width: 100%;
      height: 280rpx;
      background: #f0f0f0;
    }

    .work-info {
      padding: 16rpx;

      .work-title {
        font-size: 28rpx;
        color: #333;
        display: block;
        margin-bottom: 8rpx;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .work-artist {
        font-size: 24rpx;
        color: #999;
        display: block;
        margin-bottom: 8rpx;
      }

      .work-price {
        font-size: 28rpx;
        color: #e74c3c;
        font-weight: 600;
      }
    }
  }
}

.loading-more,
.no-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: #999;
}
</style>
