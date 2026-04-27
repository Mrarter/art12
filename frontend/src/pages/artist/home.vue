<template>
  <view class="artist-home">
    <!-- 头部信息 -->
    <view class="artist-header">
      <image class="header-bg" src="/static/banner/artist-bg.jpg" mode="aspectFill"></image>
      <view class="header-content">
        <view class="artist-info">
          <image class="artist-avatar" :src="artistInfo.avatar || '/static/avatar/default.jpg'" mode="aspectFill"></image>
          <view class="artist-detail">
            <view class="artist-name">
              {{ artistInfo.name || artistInfo.nickname }}
              <view class="identity-tag" :class="'tag-' + artistInfo.identityType">
                {{ getIdentityLabel(artistInfo.identityType) }}
              </view>
              <view class="cert-status certified" v-if="artistInfo.certStatus === 'certified'">
                
                已认证
              </view>
            </view>
            <text class="artist-signature">{{ artistInfo.signature || '暂无签名' }}</text>
            <!-- 艺术家ID显示 -->
            <view class="artist-id-badge" v-if="formatToFourDigits(artistId)">
              ID: {{ formatToFourDigits(artistId) }}
            </view>
          </view>
        </view>
        <view class="artist-stats">
          <view class="stat-item" @click="goWorksList">
            <text class="stat-value">{{ artistInfo.worksCount || 0 }}</text>
            <text class="stat-label">作品</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ formatCount(artistInfo.fansCount) }}</text>
            <text class="stat-label">粉丝</text>
          </view>
          <view class="stat-item" @click="goFollowList">
            <text class="stat-value">{{ formatCount(artistInfo.followCount) }}</text>
            <text class="stat-label">关注</text>
          </view>
        </view>
        <view class="artist-actions" v-if="!isOwnProfile">
          <view class="action-btn btn-follow" v-if="!artistInfo.isFollowed" @click="onFollow">
            + 关注
          </view>
          <view class="action-btn btn-followed" v-else @click="onUnfollow">
            已关注
          </view>
          <view class="action-btn btn-message" @click="sendMessage">
            
            私信
          </view>
        </view>
      </view>
    </view>

    <!-- 艺术家私密数据看板（仅本人可见） -->
    <view class="private-dashboard card" v-if="isOwnProfile">
      <view class="dashboard-header">
        <text class="dashboard-title">
          <text>🔒</text>
          数据看板
        </text>
        <view class="dashboard-tip">仅自己可见</view>
      </view>
      <view class="dashboard-stats">
        <view class="dashboard-item" @click="goSalesDetail">
          <text class="dashboard-value">¥{{ formatPrice(privateData.totalSales || 0) }}</text>
          <text class="dashboard-label">总销售额</text>
        </view>
        <view class="dashboard-item" @click="goRevenueDetail">
          <text class="dashboard-value">¥{{ formatPrice(privateData.totalRevenue || 0) }}</text>
          <text class="dashboard-label">总收入</text>
        </view>
        <view class="dashboard-item">
          <text class="dashboard-value">{{ privateData.pendingOrders || 0 }}</text>
          <text class="dashboard-label">待发货</text>
        </view>
      </view>
      <view class="dashboard-trend">
        <view class="trend-header">
          <text class="trend-title">本月趋势</text>
          <view class="trend-period">
            <text 
              v-for="item in periodOptions" 
              :key="item.value"
              :class="['period-btn', { active: selectedPeriod === item.value }]"
              @click="changePeriod(item.value)"
            >{{ item.label }}</text>
          </view>
        </view>
        <view class="trend-chart">
          <view class="simple-bars">
            <view 
              v-for="(item, index) in trendData" 
              :key="index"
              class="bar-item"
            >
              <view 
                class="bar" 
                :style="{ height: (item.value / maxTrendValue * 80) + 'rpx' }"
              ></view>
              <text class="bar-label">{{ item.label }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 动态价格记录（仅本人可见） -->
    <view class="price-records card" v-if="isOwnProfile">
      <view class="section-header">
        <text class="section-title">
          
          作品价格动态
        </text>
        <view class="more-link" @click="goPriceRecords">
          <text>查看全部</text>
          
        </view>
      </view>
      <view class="price-list">
        <view class="price-item" v-for="item in recentPriceChanges" :key="item.artworkId">
          <image class="price-cover" :src="item.cover" mode="aspectFill"></image>
          <view class="price-info">
            <text class="price-title">{{ item.title }}</text>
            <view class="price-change">
              <text class="old-price">¥{{ item.oldPrice }}</text>
              
              <text class="new-price">¥{{ item.newPrice }}</text>
              <text class="change-rate" :class="item.changeRate > 0 ? 'up' : 'down'">
                {{ item.changeRate > 0 ? '+' : '' }}{{ item.changeRate }}%
              </text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 认证状态入口 -->
    <view class="cert-section card" v-if="isOwnProfile && artistInfo.identityType === 'artist'" @click="goCertApply">
      <view class="cert-content" v-if="!artistInfo.certStatus || artistInfo.certStatus === 'none'">
        
        <view class="cert-info">
          <text class="cert-title">申请艺术家认证</text>
          <text class="cert-desc">认证后可获得更多曝光和信任背书</text>
        </view>
        
      </view>
      <view class="cert-content" v-else-if="artistInfo.certStatus === 'pending'">
        
        <view class="cert-info">
          <text class="cert-title">认证审核中</text>
          <text class="cert-desc">预计3个工作日内完成审核</text>
        </view>
      </view>
      <view class="cert-content certified" v-else-if="artistInfo.certStatus === 'certified'">
        
        <view class="cert-info">
          <text class="cert-title">已通过艺术家认证</text>
          <text class="cert-desc">认证有效期至 {{ artistInfo.certExpireDate }}</text>
        </view>
        
      </view>
    </view>

    <!-- 简介 -->
    <view class="intro-section card" v-if="artistInfo.intro">
      <view class="section-header">
        <text class="section-title">个人简介</text>
      </view>
      <view class="intro-content" :class="{ collapsed: !introExpanded }">
        {{ artistInfo.intro }}
      </view>
      <view class="intro-toggle" @click="introExpanded = !introExpanded">
        <text>{{ introExpanded ? '收起' : '展开全部' }}</text>
        
      </view>
    </view>

    <!-- 作品列表 -->
    <view class="works-section card">
      <view class="section-header">
        <text class="section-title">作品</text>
        <view class="works-count">{{ worksList.length }}件</view>
      </view>
      <view class="works-tabs">
        <view class="tab-item" :class="{ active: worksTab === 'list' }" @click="worksTab = 'list'">
          作品列表
        </view>
        <view class="tab-item" :class="{ active: worksTab === 'gallery' }" @click="worksTab = 'gallery'">
          画廊陈列
        </view>
      </view>
      
      <!-- 列表模式 -->
      <view class="works-list" v-if="worksTab === 'list'">
        <view class="works-grid">
          <view class="work-card" v-for="work in worksList" :key="work.id" @click="goWorkDetail(work.id)">
            <view class="work-image-wrap">
              <image class="work-image" :src="work.cover || work.coverImage" mode="aspectFill"></image>
              <view class="work-status" v-if="work.status !== 'online'">
                {{ getStatusText(work.status) }}
              </view>
            </view>
            <view class="work-info">
              <text class="work-title">{{ work.title }}</text>
              <view class="work-meta">
                <text class="work-price">¥{{ formatPrice(work.price) }}</text>
                <text class="work-sales" v-if="work.salesCount > 0">已售{{ work.salesCount }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 画廊模式 -->
      <view class="works-gallery" v-if="worksTab === 'gallery'">
        <view class="gallery-item" v-for="work in worksList" :key="work.id" @click="goWorkDetail(work.id)">
          <image class="gallery-image" :src="work.cover || work.coverImage" mode="aspectFill"></image>
          <view class="gallery-overlay">
            <text class="gallery-title">{{ work.title }}</text>
            <text class="gallery-price">¥{{ formatPrice(work.price) }}</text>
          </view>
        </view>
      </view>
      
      <!-- 加载更多 -->
      <view class="load-more" v-if="hasMore && worksList.length > 0" @click="loadMoreWorks">
        <text>{{ loadingMore ? '加载中...' : '加载更多' }}</text>
      </view>
      
      <!-- 空状态 -->
      <view class="empty-works" v-if="worksList.length === 0 && !loadingMore">
        <text>暂无作品</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getArtistInfo, followArtist, unfollowArtist } from '@/api/user.js'
import { getEarningsTrend } from '@/api/promoter.js'
import { useUserStore } from '@/store/modules/user.js'

export default {
  data() {
    return {
      artistId: '',
      artistInfo: {
        identityType: 'artist'
      },
      worksList: [],
      introExpanded: false,
      worksTab: 'list',
      page: 1,
      pageSize: 20,
      hasMore: false,
      loadingMore: false,
      
      // 私密数据
      privateData: {
        totalSales: 0,
        totalRevenue: 0,
        pendingOrders: 0
      },
      trendData: [],
      maxTrendValue: 0,
      selectedPeriod: 'month',
      periodOptions: [
        { label: '周', value: 'week' },
        { label: '月', value: 'month' },
        { label: '季', value: 'quarter' }
      ],
      recentPriceChanges: []
    }
  },

  computed: {
    userStore() {
      return useUserStore()
    },
    isOwnProfile() {
      return this.userStore.userInfo?.id === Number(this.artistId)
    }
  },

  onLoad(options) {
    if (options.id) {
      this.artistId = options.id
      this.loadArtistInfo()
    }
  },

  onShow() {
    // 刷新艺术家信息
    if (this.artistId) {
      this.loadArtistInfo()
    }
  },

  methods: {
    async loadArtistInfo() {
      try {
        uni.showLoading({ title: '加载中...' })
        const res = await getArtistInfo(this.artistId)
        this.artistInfo = res
        this.worksList = res.works || []
        this.hasMore = res.hasMore || false
        
        // 如果是本人，加载私密数据
        if (this.isOwnProfile) {
          this.loadPrivateData()
        }
        
        uni.hideLoading()
      } catch (e) {
        uni.hideLoading()
        // 使用模拟数据
        this.loadMockData()
      }
    },

    async loadPrivateData() {
      try {
        // 加载收益趋势
        const trendRes = await getEarningsTrend(this.selectedPeriod)
        if (trendRes && trendRes.length > 0) {
          this.trendData = trendRes.map(item => ({
            label: item.label,
            value: item.amount
          }))
          this.maxTrendValue = Math.max(...this.trendData.map(item => item.value), 1)
        }
        
        // 模拟私密数据
        this.privateData = {
          totalSales: 1280000,
          totalRevenue: 960000,
          pendingOrders: 5
        }
        
        // 模拟价格变动
        this.recentPriceChanges = [
          { artworkId: 1, title: '山水长卷', cover: '/static/product/demo1.jpg', oldPrice: 120000, newPrice: 128000, changeRate: 6.67 },
          { artworkId: 2, title: '荷花图', cover: '/static/product/demo2.jpg', oldPrice: 85000, newPrice: 88000, changeRate: 3.53 },
          { artworkId: 3, title: '松云图', cover: '/static/product/demo3.jpg', oldPrice: 156000, newPrice: 156000, changeRate: 0 }
        ]
      } catch (e) {
        console.error('加载私密数据失败', e)
      }
    },

    loadMockData() {
      this.artistInfo = {
        id: 1,
        name: '张大千',
        nickname: '张大千',
        avatar: '/static/avatar/demo.jpg',
        signature: '画坛巨匠，国画大师',
        identityType: 'artist',
        certStatus: 'certified',
        certExpireDate: '2027-06-30',
        worksCount: 128,
        fansCount: 56000,
        followCount: 320,
        isFollowed: false,
        intro: '张大千（1899-1983），四川内江人，中国著名画家、书法家、诗人。他年轻时即擅长绘画，待人真挚诚恳。绘画方面融合了传统与创新，在山水画方面有突出贡献。'
      }
      
      this.worksList = [
        { id: 1, title: '山水长卷', cover: '/static/product/demo1.jpg', price: 128000, salesCount: 15 },
        { id: 2, title: '荷花图', cover: '/static/product/demo2.jpg', price: 88000, salesCount: 8 },
        { id: 3, title: '松云图', cover: '/static/product/demo3.jpg', price: 156000, salesCount: 12 },
        { id: 4, title: '庐山图', cover: '/static/product/demo4.jpg', price: 280000, salesCount: 3 }
      ]
    },

    async changePeriod(period) {
      this.selectedPeriod = period
      await this.loadPrivateData()
    },

    getIdentityLabel(type) {
      const labels = {
        artist: '艺术家',
        gallery: '画廊',
        dealer: '画商',
        promoter: '艺荐官',
        collector: '收藏家'
      }
      return labels[type] || ''
    },

    getStatusText(status) {
      const map = {
        pending: '审核中',
        offline: '已下架',
        sold: '已售罄'
      }
      return map[status] || ''
    },

    formatCount(num) {
      if (!num) return '0'
      if (num >= 10000) {
        return (num / 10000).toFixed(1) + '万'
      }
      return num.toString()
    },

    formatPrice(price) {
      if (!price) return '0'
      const yuan = price / 100  // 分转元
      if (yuan >= 10000) {
        return (yuan / 10000).toFixed(yuan % 10000 === 0 ? 0 : 1) + '万'
      }
      return yuan.toLocaleString()
    },

    // 格式化ID为4位数
    formatToFourDigits(id) {
      if (!id) return ''
      return String(id).padStart(4, '0')
    },

    async onFollow() {
      if (!this.userStore.isAuthenticated) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      
      try {
        await followArtist(this.artistId)
        this.artistInfo.isFollowed = true
        this.artistInfo.fansCount = (this.artistInfo.fansCount || 0) + 1
        uni.showToast({ title: '关注成功', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: '关注失败', icon: 'none' })
      }
    },

    onUnfollow() {
      uni.showModal({
        title: '提示',
        content: '确定取消关注吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              await unfollowArtist(this.artistId)
              this.artistInfo.isFollowed = false
              this.artistInfo.fansCount = Math.max((this.artistInfo.fansCount || 1) - 1, 0)
            } catch (e) {
              uni.showToast({ title: '取消失败', icon: 'none' })
            }
          }
        }
      })
    },

    sendMessage() {
      if (!this.userStore.isAuthenticated) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      uni.navigateTo({ url: `/pages/message/chat?userId=${this.artistId}` })
    },

    goWorkDetail(id) {
      uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` })
    },

    goWorksList() {
      uni.navigateTo({ url: '/pages/common/coming-soon?title=艺术家作品集&desc=更完整的作品集页还在整理中，当前可先在主页继续浏览作品。' })
    },

    goFollowList() {
      uni.navigateTo({ url: '/pages/common/coming-soon?title=关注与粉丝&desc=关注关系页还在建设中，后续会展示完整的关注列表。' })
    },

    goSalesDetail() {
      uni.navigateTo({ url: '/pages/common/coming-soon?title=销售明细&desc=销售明细页正在开发中，后续会补充订单与趋势数据。' })
    },

    goRevenueDetail() {
      uni.navigateTo({ url: '/pages/common/coming-soon?title=收益明细&desc=收益明细页正在开发中，后续会补充结算与账单信息。' })
    },

    goPriceRecords() {
      uni.navigateTo({ url: '/pages/common/coming-soon?title=价格记录&desc=价格轨迹页正在开发中，后续会展示涨跌历史和触发原因。' })
    },

    goCertApply() {
      if (this.artistInfo.certStatus === 'pending') {
        uni.showToast({ title: '审核中，请耐心等待', icon: 'none' })
        return
      }
      if (this.artistInfo.certStatus === 'certified') {
        uni.navigateTo({ url: '/pages/artist/cert' })
        return
      }
      uni.navigateTo({ url: '/pages/artist/cert' })
    },

    async loadMoreWorks() {
      if (this.loadingMore || !this.hasMore) return
      
      this.loadingMore = true
      this.page++
      
      try {
        const res = await getArtistInfo(this.artistId)
        const newWorks = res.works || []
        this.worksList = [...this.worksList, ...newWorks]
        this.hasMore = res.hasMore || false
      } catch (e) {
        console.error('加载更多失败', e)
      }
      
      this.loadingMore = false
    }
  }
}
</script>

<style lang="scss" scoped>
// 深色艺术主题色
$bg-primary: #0d0d0d;
$bg-card: #1a1a1a;
$bg-elevated: #242424;
$text-primary: #ffffff;
$text-secondary: #a0a0a0;
$text-muted: #666666;
$accent-gold: #c9a227;
$accent-gold-light: #e6c65c;

.artist-home {
  min-height: 100vh;
  background: $bg-primary;
  padding-bottom: 40rpx;
}

.card {
  margin: 20rpx;
  background: $bg-card;
  border-radius: 16rpx;
  padding: 24rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.04);
}

.artist-header {
  position: relative;
}

.header-bg {
  width: 100%;
  height: 300rpx;
}

.header-content {
  position: relative;
  background: $bg-card;
  padding: 0 30rpx 30rpx;
  margin-top: -80rpx;
  border-radius: 24rpx 24rpx 0 0;
  border: 1rpx solid rgba(255, 255, 255, 0.04);
}

.artist-info {
  display: flex;
  align-items: flex-end;
  margin-bottom: 30rpx;
}

.artist-avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  border: 6rpx solid $bg-card;
  margin-right: 24rpx;
  margin-top: -70rpx;
  background: $bg-elevated;
}

.artist-detail {
  flex: 1;
}

.artist-name {
  display: flex;
  align-items: center;
  font-size: 36rpx;
  color: $text-primary;
  font-weight: 600;
  margin-bottom: 10rpx;
  flex-wrap: wrap;
  gap: 12rpx;
}

.identity-tag {
  font-size: 20rpx;
  color: $bg-primary;
  background: linear-gradient(135deg, $accent-gold 0%, $accent-gold-light 100%);
  padding: 4rpx 16rpx;
  border-radius: 6rpx;
  
  &.tag-artist {
    background: linear-gradient(135deg, $accent-gold 0%, $accent-gold-light 100%);
  }
}

.cert-status {
  display: flex;
  align-items: center;
  font-size: 20rpx;
  color: $accent-gold;
  
  .iconfont {
    margin-right: 4rpx;
  }
}

.artist-signature {
  font-size: 26rpx;
  color: $text-secondary;
}

// 艺术家ID徽章样式
.artist-id-badge {
  display: inline-block;
  margin-top: 8rpx;
  padding: 4rpx 12rpx;
  background: linear-gradient(135deg, rgba(201, 162, 39, 0.15), rgba(201, 162, 39, 0.05));
  border: 1rpx solid rgba(201, 162, 39, 0.3);
  border-radius: 8rpx;
  font-size: 20rpx;
  font-family: 'Courier New', monospace;
  color: #c9a227;
  font-weight: 600;
  letter-spacing: 2rpx;
}

.artist-stats {
  display: flex;
  margin-bottom: 30rpx;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 36rpx;
  color: $text-primary;
  font-weight: 600;
  margin-bottom: 6rpx;
}

.stat-label {
  font-size: 24rpx;
  color: $text-secondary;
}

.artist-actions {
  display: flex;
  gap: 20rpx;
}

.action-btn {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  border-radius: 36rpx;
  font-size: 28rpx;
}

.btn-follow {
  background: linear-gradient(135deg, $accent-gold 0%, $accent-gold-light 100%);
  color: $bg-primary;
}

.btn-followed {
  background: $bg-elevated;
  color: $text-secondary;
  border: 1rpx solid rgba(255, 255, 255, 0.1);
}

.btn-message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  background: $bg-elevated;
  color: $text-secondary;
  border: 1rpx solid rgba(255, 255, 255, 0.1);
}

// 私密数据看板
.private-dashboard {
  .dashboard-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;
  }
  
  .dashboard-title {
    display: flex;
    align-items: center;
    font-size: 30rpx;
    font-weight: 600;
    color: $text-primary;
    
    .iconfont {
      margin-right: 8rpx;
    }
  }
  
  .dashboard-tip {
    font-size: 20rpx;
    color: $accent-gold;
    background: rgba($accent-gold, 0.1);
    padding: 6rpx 16rpx;
    border-radius: 20rpx;
  }
}

.dashboard-stats {
  display: flex;
  gap: 20rpx;
  margin-bottom: 30rpx;
}

.dashboard-item {
  flex: 1;
  text-align: center;
  background: linear-gradient(135deg, rgba($accent-gold, 0.2) 0%, rgba($accent-gold, 0.1) 100%);
  border: 1rpx solid rgba($accent-gold, 0.3);
  border-radius: 16rpx;
  padding: 24rpx 16rpx;
  
  .dashboard-value {
    display: block;
    font-size: 32rpx;
    font-weight: 600;
    color: $accent-gold;
    margin-bottom: 8rpx;
  }
  
  .dashboard-label {
    font-size: 22rpx;
    color: $text-secondary;
  }
}

.dashboard-trend {
  .trend-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20rpx;
  }
  
  .trend-title {
    font-size: 26rpx;
    color: $text-primary;
    font-weight: 500;
  }
  
  .trend-period {
    display: flex;
    gap: 12rpx;
  }
  
  .period-btn {
    font-size: 22rpx;
    color: $text-secondary;
    padding: 6rpx 16rpx;
    border-radius: 16rpx;
    background: $bg-elevated;
    
    &.active {
      background: $accent-gold;
      color: $bg-primary;
    }
  }
}

.trend-chart {
  height: 160rpx;
}

.simple-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 120rpx;
  padding-top: 20rpx;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .bar {
    width: 36rpx;
    background: linear-gradient(180deg, $accent-gold 0%, rgba($accent-gold, 0.5) 100%);
    border-radius: 6rpx 6rpx 0 0;
    margin-bottom: 10rpx;
    min-height: 10rpx;
  }
  
  .bar-label {
    font-size: 20rpx;
    color: $text-muted;
  }
}

// 价格动态
.price-records {
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20rpx;
  }
  
  .section-title {
    display: flex;
    align-items: center;
    font-size: 30rpx;
    font-weight: 600;
    color: $text-primary;
    
    .iconfont {
      margin-right: 8rpx;
    }
  }
  
  .more-link {
    display: flex;
    align-items: center;
    font-size: 24rpx;
    color: $text-muted;
    
    text {
      margin-right: 6rpx;
    }
  }
}

.price-list {
  .price-item {
    display: flex;
    align-items: center;
    padding: 16rpx 0;
    border-bottom: 1rpx solid rgba(255, 255, 255, 0.06);
    
    &:last-child {
      border-bottom: none;
    }
  }
  
  .price-cover {
    width: 80rpx;
    height: 80rpx;
    border-radius: 8rpx;
    margin-right: 16rpx;
    background: $bg-elevated;
  }
  
  .price-info {
    flex: 1;
    
    .price-title {
      display: block;
      font-size: 26rpx;
      color: $text-primary;
      margin-bottom: 8rpx;
    }
    
    .price-change {
      display: flex;
      align-items: center;
      font-size: 24rpx;
      
      .old-price {
        color: $text-muted;
        text-decoration: line-through;
      }
      
      .new-price {
        color: $text-primary;
        margin-left: 8rpx;
      }
      
      .change-rate {
        margin-left: 16rpx;
        padding: 2rpx 8rpx;
        border-radius: 4rpx;
        font-size: 20rpx;
        
        &.up {
          background: rgba(255, 107, 107, 0.15);
          color: #ff6b6b;
        }
        
        &.down {
          background: rgba(76, 175, 80, 0.15);
          color: #4CAF50;
        }
      }
    }
  }
}

// 认证状态
.cert-section {
  cursor: pointer;
}

.cert-content {
  display: flex;
  align-items: center;
  
  .cert-info {
    flex: 1;
    margin-left: 16rpx;
    
    .cert-title {
      display: block;
      font-size: 28rpx;
      color: $text-primary;
      font-weight: 500;
      margin-bottom: 6rpx;
    }
    
    .cert-desc {
      font-size: 22rpx;
      color: $text-secondary;
    }
  }
  
  &.certified {
    .cert-title {
      color: $accent-gold;
    }
  }
}

// 简介
.intro-section {
  .section-header {
    margin-bottom: 16rpx;
  }
  
  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: $text-primary;
  }
}

.intro-content {
  font-size: 28rpx;
  color: $text-secondary;
  line-height: 1.8;
  
  &.collapsed {
    max-height: 200rpx;
    overflow: hidden;
  }
}

.intro-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-top: 20rpx;
  font-size: 26rpx;
  color: $accent-gold;
}

// 作品列表
.works-section {
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20rpx;
  }
  
  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: $text-primary;
  }
  
  .works-count {
    font-size: 24rpx;
    color: $text-muted;
  }
}

.works-tabs {
  display: flex;
  background: $bg-elevated;
  border-radius: 12rpx;
  padding: 6rpx;
  margin-bottom: 30rpx;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  font-size: 26rpx;
  color: $text-secondary;
  border-radius: 8rpx;
  
  &.active {
    background: $accent-gold;
    color: $bg-primary;
    font-weight: 500;
  }
}

.works-grid {
  display: flex;
  flex-wrap: wrap;
  margin: 0 -10rpx;
}

.work-card {
  width: 50%;
  padding: 10rpx;
}

.work-image-wrap {
  position: relative;
}

.work-image {
  width: 100%;
  height: 300rpx;
  border-radius: 12rpx;
  margin-bottom: 16rpx;
  background: $bg-elevated;
}

.work-status {
  position: absolute;
  top: 16rpx;
  left: 16rpx;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}

.work-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.work-title {
  font-size: 26rpx;
  color: $text-primary;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-meta {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.work-price {
  font-size: 26rpx;
  color: $accent-gold;
  font-weight: 600;
}

.work-sales {
  font-size: 20rpx;
  color: $text-muted;
}

.works-gallery {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
}

.gallery-item {
  position: relative;
}

.gallery-image {
  width: 100%;
  height: 400rpx;
  border-radius: 12rpx;
  background: $bg-elevated;
}

.gallery-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.6));
  padding: 20rpx;
  border-radius: 0 0 12rpx 12rpx;
  
  .gallery-title {
    display: block;
    font-size: 26rpx;
    color: #fff;
    margin-bottom: 6rpx;
  }
  
  .gallery-price {
    font-size: 28rpx;
    color: #ffd700;
    font-weight: 600;
  }
}

.load-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: $text-muted;
}

.empty-works {
  text-align: center;
  padding: 60rpx;
  color: $text-muted;
  font-size: 28rpx;
}
</style>
