<template>
  <view class="search-page">
    <!-- 搜索框 -->
    <view class="search-bar">
      <view class="search-input-wrapper">
        <u-icon name="search" size="20" color="#999"></u-icon>
        <input 
          class="search-input" 
          v-model="searchKeyword" 
          placeholder="搜索作品、艺术家"
          confirm-type="search"
          @confirm="doSearch"
          @input="onSearchInput"
        />
        <u-icon 
          v-if="searchKeyword" 
          name="close" 
          size="18" 
          color="#999" 
          @click="clearSearch"
        ></u-icon>
      </view>
      <text class="cancel-btn" @click="goBack">取消</text>
    </view>

    <!-- 搜索建议 -->
    <view class="search-suggest" v-if="showSuggest && suggestions.length > 0">
      <view 
        class="suggest-item" 
        v-for="(item, index) in suggestions" 
        :key="index"
        @click="selectSuggest(item)"
      >
        <u-icon name="search" size="18" color="#999"></u-icon>
        <text class="suggest-text">{{ item.keyword }}</text>
        <text class="suggest-tag" v-if="item.tag">{{ item.tag }}</text>
      </view>
    </view>

    <!-- 搜索结果前的状态 -->
    <view class="search-content" v-if="!showSuggest">
      <!-- 搜索历史 -->
      <view class="search-section" v-if="historyList.length > 0">
        <view class="section-header">
          <text class="section-title">搜索历史</text>
          <text class="section-action" @click="clearHistory">清空</text>
        </view>
        <view class="history-tags">
          <view 
            class="history-tag" 
            v-for="(item, index) in historyList" 
            :key="index"
            @click="searchFromHistory(item)"
          >
            {{ item }}
          </view>
        </view>
      </view>

      <!-- 热门搜索 -->
      <view class="search-section">
        <view class="section-header">
          <text class="section-title">热门搜索</text>
          <view class="section-action" @click="toggleHot">
            <u-icon :name="showAllHot ? 'eye-off' : 'eye'" size="16" color="#999"></u-icon>
            <text>{{ showAllHot ? '收起' : '展开' }}</text>
          </view>
        </view>
        <view class="hot-list">
          <view 
            class="hot-item" 
            v-for="(item, index) in (showAllHot ? hotList : hotList.slice(0, 6))" 
            :key="index"
            @click="searchFromHot(item)"
          >
            <view class="hot-rank" :class="{ top: index < 3 }">{{ index + 1 }}</view>
            <view class="hot-info">
              <text class="hot-title">{{ item.keyword }}</text>
              <text class="hot-desc">{{ item.desc }}</text>
            </view>
            <image v-if="item.icon" :src="item.icon" class="hot-icon"></image>
          </view>
        </view>
      </view>

      <!-- 搜索发现 -->
      <view class="search-section">
        <view class="section-header">
          <text class="section-title">发现</text>
        </view>
        <view class="discover-tags">
          <view 
            class="discover-tag" 
            v-for="(tag, index) in discoverTags" 
            :key="index"
            @click="searchFromDiscover(tag)"
          >
            {{ tag.name }}
          </view>
        </view>
      </view>

      <!-- 搜索结果 -->
      <view class="search-result" v-if="isSearching">
        <!-- 搜索类型切换 -->
        <view class="result-tabs">
          <view 
            class="tab-item" 
            :class="{ active: resultType === 'works' }"
            @click="switchResultType('works')"
          >
            作品
            <text class="tab-count">{{ resultCounts.works }}</text>
          </view>
          <view 
            class="tab-item" 
            :class="{ active: resultType === 'artists' }"
            @click="switchResultType('artists')"
          >
            艺术家
            <text class="tab-count">{{ resultCounts.artists }}</text>
          </view>
        </view>

        <!-- 筛选排序 -->
        <view class="result-filter" v-if="resultType === 'works'">
          <view class="filter-tabs">
            <view 
              class="filter-tab" 
              :class="{ active: filterType === 'default' }"
              @click="setFilter('default')"
            >综合</view>
            <view 
              class="filter-tab" 
              :class="{ active: filterType === 'sales' }"
              @click="setFilter('sales')"
            >销量</view>
            <view 
              class="filter-tab" 
              :class="{ active: filterType === 'price' }"
              @click="setFilter('price')"
            >
              价格
              <u-icon name="arrow-up" size="12" :color="filterType === 'price' ? '#667eea' : '#999'"></u-icon>
            </view>
            <view 
              class="filter-tab" 
              :class="{ active: filterType === 'latest' }"
              @click="setFilter('latest')"
            >最新</view>
          </view>
          <view class="filter-action" @click="showFilterModal">
            <u-icon name="filter" size="20" color="#666"></u-icon>
            <text>筛选</text>
          </view>
        </view>

        <!-- 作品结果 -->
        <view class="result-list works-list" v-if="resultType === 'works'">
          <view 
            class="work-item" 
            v-for="(item, index) in worksResult" 
            :key="index"
            @click="goWorkDetail(item.id)"
          >
            <image :src="item.cover" mode="aspectFill" class="work-cover"></image>
            <view class="work-info">
              <text class="work-title">{{ item.title }}</text>
              <view class="work-artist">
                <image :src="item.artistAvatar" class="artist-avatar"></image>
                <text>{{ item.artistName }}</text>
              </view>
              <view class="work-meta">
                <text class="work-price">¥{{ item.price }}</text>
                <text class="work-sales">已售 {{ item.sales }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 艺术家结果 -->
        <view class="result-list artists-list" v-if="resultType === 'artists'">
          <view 
            class="artist-item" 
            v-for="(item, index) in artistsResult" 
            :key="index"
            @click="goArtistHome(item.id)"
          >
            <image :src="item.avatar" mode="aspectFill" class="artist-avatar"></image>
            <view class="artist-info">
              <view class="artist-header">
                <text class="artist-name">{{ item.name }}</text>
                <view class="artist-badge" v-if="item.certified">
                  <u-icon name="checkmark-circle" size="14" color="#fff"></u-icon>
                  <text>认证</text>
                </view>
              </view>
              <text class="artist-field">{{ item.field }}</text>
              <view class="artist-stats">
                <text>作品 {{ item.worksCount }}</text>
                <text>粉丝 {{ formatCount(item.fansCount) }}</text>
              </view>
            </view>
            <view class="artist-action">
              <text class="follow-btn" v-if="!item.isFollowed">关注</text>
              <text class="followed-btn" v-else>已关注</text>
            </view>
          </view>
        </view>

        <!-- 加载更多 -->
        <view class="load-more" v-if="hasMore" @click="loadMoreResults">
          <u-loading mode="circle" v-if="loading"></u-loading>
          <text v-else>加载更多</text>
        </view>

        <!-- 空状态 -->
        <view class="empty-result" v-if="!loading && worksResult.length === 0 && artistsResult.length === 0">
          <image src="/static/empty/search.png" mode="aspectFit" class="empty-icon"></image>
          <text class="empty-text">未找到相关结果</text>
          <text class="empty-hint">换个关键词试试吧</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const searchKeyword = ref('')
const showSuggest = ref(false)
const isSearching = ref(false)
const showAllHot = ref(false)
const resultType = ref('works')
const filterType = ref('default')
const loading = ref(false)
const hasMore = ref(true)
const page = ref(1)

const historyList = ref(['油画', '山水画', '书法'])
const suggestions = ref([])

const hotList = ref([
  { keyword: '油画', desc: '经典油画作品', icon: '🔥', hot: 9860 },
  { keyword: '国画', desc: '传统国画艺术', icon: '🎨', hot: 8520 },
  { keyword: '书法', desc: '书法作品欣赏', icon: '', hot: 7650 },
  { keyword: '抽象艺术', desc: '当代抽象作品', icon: '✨', hot: 6540 },
  { keyword: '雕塑', desc: '雕塑艺术品', icon: '', hot: 5430 },
  { keyword: '水彩', desc: '水彩画作品', icon: '', hot: 4320 },
  { keyword: '摄影', desc: '艺术摄影作品', icon: '', hot: 3210 },
  { keyword: '插画', desc: '原创插画作品', icon: '', hot: 2100 }
])

const discoverTags = ref([
  { name: '抽象派' },
  { name: '写实主义' },
  { name: '印象派' },
  { name: '当代艺术' },
  { name: '传统水墨' },
  { name: '装置艺术' }
])

const resultCounts = ref({
  works: 156,
  artists: 28
})

const worksResult = ref([
  {
    id: 1,
    cover: 'https://pic.imgdb.cn/item/1.jpg',
    title: '山水意境·云起',
    artistName: '李明',
    artistAvatar: 'https://pic.imgdb.cn/avatar/1.jpg',
    price: '8888',
    sales: 56
  },
  {
    id: 2,
    cover: 'https://pic.imgdb.cn/item/2.jpg',
    title: '春暖花开',
    artistName: '王芳',
    artistAvatar: 'https://pic.imgdb.cn/avatar/2.jpg',
    price: '12800',
    sales: 32
  },
  {
    id: 3,
    cover: 'https://pic.imgdb.cn/item/3.jpg',
    title: '城市夜景',
    artistName: '张伟',
    artistAvatar: 'https://pic.imgdb.cn/avatar/3.jpg',
    price: '6800',
    sales: 18
  }
])

const artistsResult = ref([
  {
    id: 1,
    name: '李明',
    avatar: 'https://pic.imgdb.cn/avatar/1.jpg',
    field: '油画·国画',
    worksCount: 128,
    fansCount: 56800,
    certified: true,
    isFollowed: false
  },
  {
    id: 2,
    name: '王芳',
    avatar: 'https://pic.imgdb.cn/avatar/2.jpg',
    field: '当代艺术',
    worksCount: 86,
    fansCount: 32500,
    certified: true,
    isFollowed: true
  }
])

const formatCount = (count) => {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + '万'
  }
  return count.toString()
}

const goBack = () => {
  uni.navigateBack()
}

const onSearchInput = (e) => {
  const value = e.detail.value
  if (value) {
    // 模拟搜索建议
    suggestions.value = [
      { keyword: value + '作品', tag: '作品' },
      { keyword: value + '艺术家', tag: '艺术家' },
      { keyword: '优秀的' + value, tag: '' }
    ]
    showSuggest.value = true
  } else {
    showSuggest.value = false
    suggestions.value = []
  }
}

const clearSearch = () => {
  searchKeyword.value = ''
  showSuggest.value = false
  isSearching.value = false
}

const doSearch = () => {
  if (!searchKeyword.value.trim()) return
  
  // 添加到历史
  if (!historyList.value.includes(searchKeyword.value)) {
    historyList.value.unshift(searchKeyword.value)
    if (historyList.value.length > 10) {
      historyList.value.pop()
    }
  }
  
  showSuggest.value = false
  isSearching.value = true
  page.value = 1
}

const selectSuggest = (item) => {
  searchKeyword.value = item.keyword
  doSearch()
}

const clearHistory = () => {
  historyList.value = []
}

const toggleHot = () => {
  showAllHot.value = !showAllHot.value
}

const searchFromHistory = (keyword) => {
  searchKeyword.value = keyword
  doSearch()
}

const searchFromHot = (item) => {
  searchKeyword.value = item.keyword
  doSearch()
}

const searchFromDiscover = (tag) => {
  searchKeyword.value = tag.name
  doSearch()
}

const switchResultType = (type) => {
  resultType.value = type
  page.value = 1
}

const setFilter = (type) => {
  filterType.value = type
}

const showFilterModal = () => {
  uni.showToast({ title: '筛选功能', icon: 'none' })
}

const goWorkDetail = (id) => {
  uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` })
}

const goArtistHome = (id) => {
  uni.navigateTo({ url: `/pages/artist/home?userId=${id}` })
}

const loadMoreResults = () => {
  if (loading.value || !hasMore.value) return
  loading.value = true
  
  setTimeout(() => {
    loading.value = false
    // hasMore.value = false // 模拟加载完毕
  }, 1000)
}

onMounted(() => {
  // 从全局获取搜索参数
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = currentPage.options || {}
  
  if (options.keyword) {
    searchKeyword.value = options.keyword
    doSearch()
  }
})
</script>

<style lang="scss" scoped>
.search-page {
  min-height: 100vh;
  background: #f5f6f8;
}

.search-bar {
  display: flex;
  align-items: center;
  padding: 20rpx 30rpx;
  background: #fff;

  .search-input-wrapper {
    flex: 1;
    display: flex;
    align-items: center;
    height: 72rpx;
    padding: 0 24rpx;
    background: #f5f6f8;
    border-radius: 36rpx;

    .search-input {
      flex: 1;
      margin-left: 12rpx;
      font-size: 28rpx;
    }
  }

  .cancel-btn {
    margin-left: 24rpx;
    font-size: 28rpx;
    color: #667eea;
  }
}

.search-suggest {
  background: #fff;
  padding: 0 30rpx;

  .suggest-item {
    display: flex;
    align-items: center;
    padding: 24rpx 0;
    border-bottom: 1rpx solid #f0f0f0;

    .suggest-text {
      flex: 1;
      margin-left: 16rpx;
      font-size: 28rpx;
      color: #333;
    }

    .suggest-tag {
      padding: 6rpx 16rpx;
      background: #f0f2f5;
      color: #666;
      font-size: 22rpx;
      border-radius: 6rpx;
    }
  }
}

.search-content {
  padding: 20rpx 30rpx;
}

.search-section {
  margin-bottom: 40rpx;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;

    .section-title {
      font-size: 32rpx;
      font-weight: 600;
      color: #333;
    }

    .section-action {
      display: flex;
      align-items: center;
      gap: 8rpx;
      font-size: 24rpx;
      color: #999;
    }
  }
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;

  .history-tag {
    padding: 12rpx 24rpx;
    background: #fff;
    border-radius: 32rpx;
    font-size: 26rpx;
    color: #666;
  }
}

.hot-list {
  .hot-item {
    display: flex;
    align-items: center;
    padding: 24rpx 0;
    border-bottom: 1rpx solid #f0f0f0;

    .hot-rank {
      width: 48rpx;
      height: 48rpx;
      background: #f0f2f5;
      color: #999;
      font-size: 24rpx;
      font-weight: 600;
      border-radius: 8rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 20rpx;

      &.top {
        background: linear-gradient(135deg, #ff6b6b, #ee5a24);
        color: #fff;
      }
    }

    .hot-info {
      flex: 1;

      .hot-title {
        font-size: 28rpx;
        color: #333;
        font-weight: 500;
      }

      .hot-desc {
        font-size: 24rpx;
        color: #999;
        margin-top: 4rpx;
      }
    }

    .hot-icon {
      width: 32rpx;
      height: 32rpx;
    }
  }
}

.discover-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;

  .discover-tag {
    padding: 16rpx 32rpx;
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: #fff;
    font-size: 26rpx;
    border-radius: 40rpx;
  }
}

.search-result {
  margin-top: 20rpx;
}

.result-tabs {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 8rpx;
  margin-bottom: 20rpx;

  .tab-item {
    flex: 1;
    height: 72rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8rpx;
    font-size: 28rpx;
    color: #666;
    border-radius: 12rpx;

    &.active {
      background: #667eea;
      color: #fff;

      .tab-count {
        background: rgba(255,255,255,0.3);
        color: #fff;
      }
    }

    .tab-count {
      padding: 2rpx 10rpx;
      background: #f0f2f5;
      color: #999;
      font-size: 22rpx;
      border-radius: 10rpx;
    }
  }
}

.result-filter {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;

  .filter-tabs {
    display: flex;
    gap: 32rpx;

    .filter-tab {
      display: flex;
      align-items: center;
      gap: 4rpx;
      font-size: 26rpx;
      color: #666;

      &.active {
        color: #667eea;
        font-weight: 600;
      }
    }
  }

  .filter-action {
    display: flex;
    align-items: center;
    gap: 8rpx;
    font-size: 26rpx;
    color: #666;
  }
}

.works-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;

  .work-item {
    background: #fff;
    border-radius: 16rpx;
    overflow: hidden;

    .work-cover {
      width: 100%;
      height: 320rpx;
    }

    .work-info {
      padding: 20rpx;

      .work-title {
        font-size: 28rpx;
        color: #333;
        font-weight: 500;
        display: block;
        margin-bottom: 12rpx;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .work-artist {
        display: flex;
        align-items: center;
        gap: 12rpx;
        margin-bottom: 12rpx;

        .artist-avatar {
          width: 40rpx;
          height: 40rpx;
          border-radius: 50%;
        }

        text {
          font-size: 24rpx;
          color: #999;
        }
      }

      .work-meta {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .work-price {
          font-size: 32rpx;
          color: #ff4d4f;
          font-weight: 600;
        }

        .work-sales {
          font-size: 22rpx;
          color: #999;
        }
      }
    }
  }
}

.artists-list {
  .artist-item {
    display: flex;
    align-items: center;
    padding: 30rpx;
    background: #fff;
    border-radius: 16rpx;
    margin-bottom: 16rpx;

    .artist-avatar {
      width: 120rpx;
      height: 120rpx;
      border-radius: 50%;
      margin-right: 24rpx;
    }

    .artist-info {
      flex: 1;

      .artist-header {
        display: flex;
        align-items: center;
        gap: 12rpx;
        margin-bottom: 8rpx;

        .artist-name {
          font-size: 32rpx;
          font-weight: 600;
          color: #333;
        }

        .artist-badge {
          display: flex;
          align-items: center;
          gap: 4rpx;
          padding: 4rpx 12rpx;
          background: linear-gradient(135deg, #667eea, #764ba2);
          color: #fff;
          font-size: 20rpx;
          border-radius: 12rpx;
        }
      }

      .artist-field {
        font-size: 24rpx;
        color: #999;
        display: block;
        margin-bottom: 12rpx;
      }

      .artist-stats {
        display: flex;
        gap: 24rpx;

        text {
          font-size: 24rpx;
          color: #666;
        }
      }
    }

    .artist-action {
      .follow-btn {
        padding: 12rpx 32rpx;
        background: linear-gradient(135deg, #667eea, #764ba2);
        color: #fff;
        font-size: 26rpx;
        border-radius: 32rpx;
      }

      .followed-btn {
        padding: 12rpx 32rpx;
        background: #f0f2f5;
        color: #999;
        font-size: 26rpx;
        border-radius: 32rpx;
      }
    }
  }
}

.load-more {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40rpx;
  font-size: 26rpx;
  color: #999;
}

.empty-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;

  .empty-icon {
    width: 200rpx;
    height: 200rpx;
    opacity: 0.5;
  }

  .empty-text {
    font-size: 32rpx;
    color: #333;
    margin-top: 30rpx;
  }

  .empty-hint {
    font-size: 26rpx;
    color: #999;
    margin-top: 12rpx;
  }
}
</style>