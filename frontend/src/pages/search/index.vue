<template>
  <view class="search-page">
    <!-- 搜索栏 -->
    <view class="search-header">
      <view class="search-box">
        <u-icon name="search" size="18" color="#999"></u-icon>
        <input 
          class="search-input" 
          v-model="keyword" 
          placeholder="搜索艺术家/作品/标签"
          confirm-type="search"
          @confirm="onSearch"
          @input="onInput"
        />
        <u-icon v-if="keyword" name="close-circle" size="16" color="#999" @click="clearKeyword"></u-icon>
      </view>
      <text class="cancel-btn" @click="goBack">取消</text>
    </view>

    <!-- 搜索历史 -->
    <view class="history-section" v-if="showHistory && !keyword">
      <view class="section-header">
        <text class="section-title">搜索历史</text>
        <view class="clear-btn" @click="clearHistory">
          <u-icon name="trash" size="14" color="#999"></u-icon>
          <text>清除</text>
        </view>
      </view>
      <view class="history-tags">
        <view 
          class="history-tag" 
          v-for="(item, index) in historyList" 
          :key="index"
          @click="searchHistory(item)"
        >
          {{ item }}
        </view>
      </view>
    </view>

    <!-- 热门搜索 -->
    <view class="hot-section" v-if="!keyword">
      <view class="section-header">
        <text class="section-title">热门搜索</text>
        <view class="refresh-btn" @click="refreshHot">
          <u-icon name="reload" size="14" color="#999"></u-icon>
          <text>换一换</text>
        </view>
      </view>
      <view class="hot-list">
        <view 
          class="hot-item" 
          v-for="(item, index) in hotList" 
          :key="item.id"
          :class="{ top: index < 3 }"
          @click="searchHot(item)"
        >
          <view class="hot-rank">{{ index + 1 }}</view>
          <view class="hot-info">
            <text class="hot-title">{{ item.keyword }}</text>
            <text class="hot-desc">{{ item.desc }}</text>
          </view>
          <view class="hot-tag" v-if="item.isNew">新</view>
        </view>
      </view>
    </view>

    <!-- 搜索结果 -->
    <view class="result-section" v-if="keyword && hasSearched">
      <!-- 结果统计 -->
      <view class="result-header">
        <text class="result-count">找到 {{ totalCount }} 个结果</text>
        <view class="result-filter" @click="showFilter">
          <text>筛选</text>
          <u-icon name="filter" size="14" color="#666"></u-icon>
        </view>
      </view>

      <!-- 结果分类 -->
      <view class="result-tabs">
        <view 
          class="tab-item" 
          :class="{ active: resultTab === 'all' }"
          @click="switchResultTab('all')"
        >
          全部
        </view>
        <view 
          class="tab-item" 
          :class="{ active: resultTab === 'artwork' }"
          @click="switchResultTab('artwork')"
        >
          作品
        </view>
        <view 
          class="tab-item" 
          :class="{ active: resultTab === 'artist' }"
          @click="switchResultTab('artist')"
        >
          艺术家
        </view>
        <view 
          class="tab-item" 
          :class="{ active: resultTab === 'auction' }"
          @click="switchResultTab('auction')"
        >
          拍卖
        </view>
      </view>

      <!-- 作品结果 -->
      <view class="result-list" v-if="resultTab === 'all' || resultTab === 'artwork'">
        <view class="result-subtitle" v-if="resultTab === 'all'">相关作品</view>
        <view class="artwork-grid">
          <view 
            class="artwork-card" 
            v-for="work in artworkResults" 
            :key="work.id"
            @click="goArtwork(work)"
          >
            <image class="artwork-image" :src="work.cover" mode="aspectFill"></image>
            <view class="artwork-info">
              <text class="artwork-title">{{ work.title }}</text>
              <view class="artwork-meta">
                <text class="artwork-author">{{ work.author }}</text>
                <text class="artwork-price">¥{{ formatPrice(work.price) }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 艺术家结果 -->
      <view class="result-list" v-if="resultTab === 'all' || resultTab === 'artist'">
        <view class="result-subtitle" v-if="resultTab === 'all'">相关艺术家</view>
        <view class="artist-list">
          <view 
            class="artist-item" 
            v-for="artist in artistResults" 
            :key="artist.id"
            @click="goArtist(artist)"
          >
            <image class="artist-avatar" :src="artist.avatar"></image>
            <view class="artist-info">
              <view class="artist-header">
                <text class="artist-name">{{ artist.name }}</text>
                <view class="artist-badge">{{ artist.badge }}</view>
              </view>
              <text class="artist-desc">{{ artist.desc }}</text>
              <view class="artist-stats">
                <text>作品 {{ artist.worksCount }}</text>
                <text>粉丝 {{ artist.fansCount }}</text>
              </view>
            </view>
            <view class="follow-btn" :class="{ followed: artist.isFollowed }" @click.stop="toggleFollow(artist)">
              {{ artist.isFollowed ? '已关注' : '关注' }}
            </view>
          </view>
        </view>
      </view>

      <!-- 拍卖结果 -->
      <view class="result-list" v-if="resultTab === 'all' || resultTab === 'auction'">
        <view class="result-subtitle" v-if="resultTab === 'all'">相关拍卖</view>
        <view class="auction-list">
          <view 
            class="auction-item" 
            v-for="auction in auctionResults" 
            :key="auction.id"
            @click="goAuction(auction)"
          >
            <image class="auction-cover" :src="auction.cover" mode="aspectFill"></image>
            <view class="auction-info">
              <text class="auction-title">{{ auction.title }}</text>
              <view class="auction-meta">
                <text class="auction-status" :class="auction.status">{{ getAuctionStatusText(auction.status) }}</text>
                <text class="auction-time" v-if="auction.status === 'bidding'">{{ auction.endTime }}</text>
              </view>
              <view class="auction-price">
                <text class="current-price">¥{{ auction.currentPrice }}</text>
                <text class="bid-count">{{ auction.bidCount }}次出价</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="totalCount === 0">
        <image class="empty-icon" src="/static/icons/search-empty.png" mode="aspectFit"></image>
        <text class="empty-text">未找到相关结果</text>
        <text class="empty-hint">换个关键词试试吧</text>
      </view>
    </view>

    <!-- 搜索建议 -->
    <view class="suggestions" v-if="keyword && !hasSearched && suggestions.length > 0">
      <view 
        class="suggestion-item" 
        v-for="item in suggestions" 
        :key="item.id"
        @click="selectSuggestion(item)"
      >
        <u-icon name="search" size="16" color="#999"></u-icon>
        <text class="suggestion-text">{{ item.keyword }}</text>
        <text class="suggestion-type">{{ item.type }}</text>
      </view>
    </view>

    <!-- 加载状态 -->
    <view class="loading-overlay" v-if="loading">
      <u-loading mode="circle" size="40" color="#667eea"></u-loading>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

// 状态
const keyword = ref('')
const showHistory = ref(true)
const hasSearched = ref(false)
const loading = ref(false)
const resultTab = ref('all')
const totalCount = ref(0)

// 搜索历史
const historyList = ref(['张大千', '山水画', '油画', '书法', '当代艺术'])

// 热门搜索
const hotList = ref([
  { id: 1, keyword: '当代艺术', desc: '近期热门', isNew: false },
  { id: 2, keyword: '名家书法', desc: '热点关注', isNew: false },
  { id: 3, keyword: '油画作品', desc: '热搜上升中', isNew: false },
  { id: 4, keyword: '限量版画', desc: '收藏热门', isNew: true },
  { id: 5, keyword: '瓷器摆件', desc: '热门推荐', isNew: false },
  { id: 6, keyword: '玉雕摆件', desc: '热搜上升中', isNew: false }
])

// 搜索建议
const suggestions = ref([])

// 搜索结果
const artworkResults = ref([
  {
    id: 1,
    title: '张大千山水画《溪山行旅图》',
    author: '张大千',
    cover: 'https://picsum.photos/300/300?random=1',
    price: 128000
  },
  {
    id: 2,
    title: '齐白石花鸟系列·牡丹',
    author: '齐白石',
    cover: 'https://picsum.photos/300/300?random=2',
    price: 88000
  },
  {
    id: 3,
    title: '吴冠中江南水乡',
    author: '吴冠中',
    cover: 'https://picsum.photos/300/300?random=3',
    price: 256000
  },
  {
    id: 4,
    title: '徐悲鸿奔马图',
    author: '徐悲鸿',
    cover: 'https://picsum.photos/300/300?random=4',
    price: 320000
  }
])

const artistResults = ref([
  {
    id: 1,
    name: '当代艺术家李明',
    avatar: 'https://picsum.photos/100/100?random=10',
    badge: '认证艺术家',
    desc: '当代艺术，油画，水墨',
    worksCount: 128,
    fansCount: '2.3万',
    isFollowed: false
  },
  {
    id: 2,
    name: '青年画家王芳',
    avatar: 'https://picsum.photos/100/100?random=11',
    badge: '签约艺术家',
    desc: '油画，风景，抽象',
    worksCount: 86,
    fansCount: '1.8万',
    isFollowed: true
  }
])

const auctionResults = ref([
  {
    id: 1,
    title: '2024春季艺术品拍卖会·中国书画',
    cover: 'https://picsum.photos/200/150?random=20',
    status: 'bidding',
    endTime: '距结束 02:45:30',
    currentPrice: 128000,
    bidCount: 156
  },
  {
    id: 2,
    title: '当代艺术专场·油画精品',
    cover: 'https://picsum.photos/200/150?random=21',
    status: 'upcoming',
    endTime: '4月25日 10:00开拍',
    currentPrice: 50000,
    bidCount: 0
  }
])

// 格式化价格
const formatPrice = (price) => {
  return price.toLocaleString()
}

// 返回
const goBack = () => {
  uni.navigateBack()
}

// 输入事件
const onInput = () => {
  if (keyword.value) {
    showHistory.value = false
    // 模拟搜索建议
    suggestions.value = [
      { id: 1, keyword: keyword.value + '山水画', type: '作品' },
      { id: 2, keyword: keyword.value + '书法', type: '作品' },
      { id: 3, keyword: keyword.value + '作品展', type: '艺术家' }
    ]
  } else {
    showHistory.value = true
    hasSearched.value = false
    suggestions.value = []
  }
}

// 搜索
const onSearch = () => {
  if (!keyword.value.trim()) return
  search(keyword.value)
}

// 搜索方法
const search = (kw) => {
  keyword.value = kw
  hasSearched.value = true
  suggestions.value = []
  showHistory.value = false
  loading.value = true
  
  // 保存搜索历史
  if (!historyList.value.includes(kw)) {
    historyList.value.unshift(kw)
    if (historyList.value.length > 10) {
      historyList.value.pop()
    }
  }
  
  // 模拟搜索
  setTimeout(() => {
    loading.value = false
    totalCount.value = artworkResults.value.length + artistResults.value.length + auctionResults.value.length
  }, 500)
}

// 清空关键词
const clearKeyword = () => {
  keyword.value = ''
  hasSearched.value = false
  suggestions.value = []
  showHistory.value = true
}

// 清空历史
const clearHistory = () => {
  uni.showModal({
    title: '提示',
    content: '确定要清除搜索历史吗？',
    success: (res) => {
      if (res.confirm) {
        historyList.value = []
        uni.showToast({ title: '已清除', icon: 'success' })
      }
    }
  })
}

// 搜索历史
const searchHistory = (item) => {
  keyword.value = item
  search(item)
}

// 刷新热门
const refreshHot = () => {
  // 模拟刷新
  uni.showToast({ title: '已刷新', icon: 'success' })
}

// 搜索热门
const searchHot = (item) => {
  keyword.value = item.keyword
  search(item.keyword)
}

// 选择建议
const selectSuggestion = (item) => {
  keyword.value = item.keyword
  search(item.keyword)
}

// 切换结果标签
const switchResultTab = (tab) => {
  resultTab.value = tab
}

// 显示筛选
const showFilter = () => {
  uni.showToast({ title: '筛选功能开发中', icon: 'none' })
}

// 跳转作品
const goArtwork = (work) => {
  uni.navigateTo({ url: `/pages/gallery/detail?id=${work.id}` })
}

// 跳转艺术家
const goArtist = (artist) => {
  uni.navigateTo({ url: `/pages/artist/home?id=${artist.id}` })
}

// 跳转拍卖
const goAuction = (auction) => {
  uni.navigateTo({ url: `/pages/auction/detail?id=${auction.id}` })
}

// 切换关注
const toggleFollow = (artist) => {
  artist.isFollowed = !artist.isFollowed
  uni.showToast({
    title: artist.isFollowed ? '已关注' : '已取消关注',
    icon: 'success'
  })
}

// 获取拍卖状态文本
const getAuctionStatusText = (status) => {
  const map = {
    bidding: '竞拍中',
    upcoming: '即将开始',
    ended: '已结束'
  }
  return map[status] || status
}

onLoad((options) => {
  if (options.keyword) {
    keyword.value = options.keyword
    search(options.keyword)
  }
})
</script>

<style lang="scss" scoped>
.search-page {
  min-height: 100vh;
  background: #f5f6f8;
}

.search-header {
  display: flex;
  align-items: center;
  padding: 20rpx 30rpx;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 10;

  .search-box {
    flex: 1;
    display: flex;
    align-items: center;
    height: 72rpx;
    background: #f5f6f8;
    border-radius: 36rpx;
    padding: 0 24rpx;
    margin-right: 20rpx;

    .search-input {
      flex: 1;
      font-size: 28rpx;
      margin-left: 12rpx;
    }
  }

  .cancel-btn {
    font-size: 28rpx;
    color: #667eea;
  }
}

.history-section,
.hot-section {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20rpx;

    .section-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
    }

    .clear-btn,
    .refresh-btn {
      display: flex;
      align-items: center;
      gap: 6rpx;
      font-size: 24rpx;
      color: #999;
    }
  }

  .history-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;

    .history-tag {
      padding: 12rpx 24rpx;
      background: #f5f6f8;
      border-radius: 32rpx;
      font-size: 26rpx;
      color: #666;

      &:active {
        opacity: 0.7;
      }
    }
  }

  .hot-list {
    .hot-item {
      display: flex;
      align-items: center;
      padding: 20rpx 0;
      border-bottom: 1rpx solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      &.top .hot-rank {
        background: #e74c3c;
      }

      .hot-rank {
        width: 36rpx;
        height: 36rpx;
        background: #ddd;
        color: #fff;
        font-size: 24rpx;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 16rpx;
      }

      .hot-info {
        flex: 1;

        .hot-title {
          font-size: 28rpx;
          color: #333;
          display: block;
          margin-bottom: 4rpx;
        }

        .hot-desc {
          font-size: 24rpx;
          color: #999;
        }
      }

      .hot-tag {
        padding: 4rpx 12rpx;
        background: rgba(230, 126, 34, 0.1);
        color: #e67e22;
        font-size: 22rpx;
        border-radius: 6rpx;
      }
    }
  }
}

.result-section {
  .result-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx 30rpx;
    background: #fff;

    .result-count {
      font-size: 26rpx;
      color: #666;
    }

    .result-filter {
      display: flex;
      align-items: center;
      gap: 8rpx;
      font-size: 26rpx;
      color: #666;
    }
  }

  .result-tabs {
    display: flex;
    background: #fff;
    padding: 0 30rpx;
    border-bottom: 1rpx solid #f0f0f0;

    .tab-item {
      padding: 20rpx 0;
      margin-right: 40rpx;
      font-size: 28rpx;
      color: #666;
      position: relative;

      &.active {
        color: #667eea;
        font-weight: 500;

        &::after {
          content: '';
          position: absolute;
          bottom: 0;
          left: 0;
          right: 0;
          height: 4rpx;
          background: #667eea;
          border-radius: 2rpx;
        }
      }
    }
  }

  .result-list {
    padding: 20rpx 30rpx;

    .result-subtitle {
      font-size: 28rpx;
      font-weight: 500;
      color: #333;
      margin-bottom: 20rpx;
    }

    .artwork-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 20rpx;

      .artwork-card {
        background: #fff;
        border-radius: 16rpx;
        overflow: hidden;

        .artwork-image {
          width: 100%;
          height: 300rpx;
        }

        .artwork-info {
          padding: 16rpx;

          .artwork-title {
            font-size: 26rpx;
            color: #333;
            display: block;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            margin-bottom: 8rpx;
          }

          .artwork-meta {
            display: flex;
            justify-content: space-between;
            align-items: center;

            .artwork-author {
              font-size: 24rpx;
              color: #999;
            }

            .artwork-price {
              font-size: 26rpx;
              color: #e74c3c;
              font-weight: 500;
            }
          }
        }
      }
    }

    .artist-list {
      .artist-item {
        display: flex;
        align-items: center;
        background: #fff;
        border-radius: 16rpx;
        padding: 24rpx;
        margin-bottom: 16rpx;

        .artist-avatar {
          width: 100rpx;
          height: 100rpx;
          border-radius: 50%;
          margin-right: 20rpx;
        }

        .artist-info {
          flex: 1;
          min-width: 0;

          .artist-header {
            display: flex;
            align-items: center;
            margin-bottom: 8rpx;

            .artist-name {
              font-size: 30rpx;
              font-weight: 500;
              color: #333;
              margin-right: 12rpx;
            }

            .artist-badge {
              padding: 4rpx 12rpx;
              background: rgba(102, 126, 234, 0.1);
              color: #667eea;
              font-size: 22rpx;
              border-radius: 6rpx;
            }
          }

          .artist-desc {
            font-size: 24rpx;
            color: #999;
            display: block;
            margin-bottom: 8rpx;
          }

          .artist-stats {
            display: flex;
            gap: 20rpx;
            font-size: 24rpx;
            color: #666;
          }
        }

        .follow-btn {
          padding: 12rpx 28rpx;
          background: #667eea;
          color: #fff;
          font-size: 26rpx;
          border-radius: 32rpx;

          &.followed {
            background: #f5f6f8;
            color: #999;
          }
        }
      }
    }

    .auction-list {
      .auction-item {
        display: flex;
        background: #fff;
        border-radius: 16rpx;
        overflow: hidden;
        margin-bottom: 16rpx;

        .auction-cover {
          width: 200rpx;
          height: 150rpx;
        }

        .auction-info {
          flex: 1;
          padding: 16rpx;
          display: flex;
          flex-direction: column;
          justify-content: space-between;

          .auction-title {
            font-size: 28rpx;
            color: #333;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .auction-meta {
            display: flex;
            align-items: center;
            gap: 16rpx;

            .auction-status {
              padding: 4rpx 12rpx;
              border-radius: 6rpx;
              font-size: 22rpx;

              &.bidding {
                background: rgba(230, 126, 34, 0.1);
                color: #e67e22;
              }

              &.upcoming {
                background: rgba(102, 126, 234, 0.1);
                color: #667eea;
              }

              &.ended {
                background: rgba(149, 165, 166, 0.1);
                color: #95a5a6;
              }
            }

            .auction-time {
              font-size: 24rpx;
              color: #999;
            }
          }

          .auction-price {
            display: flex;
            align-items: center;
            gap: 16rpx;

            .current-price {
              font-size: 28rpx;
              color: #e74c3c;
              font-weight: bold;
            }

            .bid-count {
              font-size: 24rpx;
              color: #999;
            }
          }
        }
      }
    }
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 100rpx 0;

    .empty-icon {
      width: 200rpx;
      height: 200rpx;
      opacity: 0.5;
    }

    .empty-text {
      font-size: 30rpx;
      color: #333;
      margin-top: 20rpx;
    }

    .empty-hint {
      font-size: 26rpx;
      color: #999;
      margin-top: 12rpx;
    }
  }
}

.suggestions {
  background: #fff;

  .suggestion-item {
    display: flex;
    align-items: center;
    padding: 24rpx 30rpx;
    border-bottom: 1rpx solid #f0f0f0;

    &:active {
      background: #f9f9f9;
    }

    .suggestion-text {
      flex: 1;
      font-size: 28rpx;
      color: #333;
      margin-left: 16rpx;
    }

    .suggestion-type {
      font-size: 24rpx;
      color: #999;
    }
  }
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  z-index: 100;
}
</style>
