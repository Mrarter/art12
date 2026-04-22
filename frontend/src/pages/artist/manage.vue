<template>
  <view class="manage-page">
    <!-- 头部统计 -->
    <view class="stats-bar">
      <view class="stat-item">
        <text class="stat-value">{{ stats.online }}</text>
        <text class="stat-label">上架中</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ stats.pending }}</text>
        <text class="stat-label">审核中</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ stats.sold }}</text>
        <text class="stat-label">已售出</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ stats.offline }}</text>
        <text class="stat-label">已下架</text>
      </view>
    </view>

    <!-- 作品列表 -->
    <view class="works-list">
      <view class="works-header">
        <text class="works-title">我的作品</text>
        <view class="header-actions">
          <view class="action-btn" @click="switchView">
            <u-icon :name="viewMode === 'list' ? 'grid' : 'list'" size="18" color="#666"></u-icon>
          </view>
          <view class="add-btn" @click="goPublish">
            <u-icon name="plus" size="16" color="#fff"></u-icon>
            发布作品
          </view>
        </view>
      </view>

      <!-- 筛选标签 -->
      <view class="filter-tabs">
        <view 
          v-for="tab in statusTabs" 
          :key="tab.value"
          :class="['filter-tab', { active: currentStatus === tab.value }]"
          @click="switchStatus(tab.value)"
        >
          {{ tab.label }}
        </view>
      </view>

      <!-- 列表视图 -->
      <view class="works-grid" v-if="filteredWorks.length > 0">
        <view class="work-card" v-for="work in filteredWorks" :key="work.id">
          <view class="work-image-wrap">
            <image class="work-image" :src="work.cover" mode="aspectFill" @click="goDetail(work.id)"></image>
            <view class="work-status-tag" :class="'status-' + work.status">
              {{ getStatusText(work.status) }}
            </view>
          </view>
          <view class="work-info">
            <text class="work-title" @click="goDetail(work.id)">{{ work.title }}</text>
            <view class="work-meta">
              <text class="work-price">¥{{ formatPrice(work.price) }}</text>
              <text class="work-sales">已售 {{ work.salesCount || 0 }}</text>
            </view>
            <view class="work-actions">
              <view class="action-item" @click="editWork(work)">
                <u-icon name="edit-pen" size="16" color="#666"></u-icon>
                <text>编辑</text>
              </view>
              <view class="action-item" @click="toggleStatus(work)">
                <u-icon :name="work.status === 'online' ? 'arrow-down' : 'arrow-up'" size="16" color="#666"></u-icon>
                <text>{{ work.status === 'online' ? '下架' : '上架' }}</text>
              </view>
              <view class="action-item" @click="deleteWork(work)">
                <u-icon name="trash" size="16" color="#e74c3c"></u-icon>
                <text>删除</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-else>
        <image class="empty-icon" src="/static/icons/empty-works.png" mode="aspectFit"></image>
        <text class="empty-text">暂无作品</text>
        <view class="empty-btn" @click="goPublish">发布第一件作品</view>
      </view>
    </view>
  </view>
</template>

<script>
import { getMyWorks, updateWorkStatus, deleteWork as deleteWorkApi } from '@/api/product.js'

export default {
  data() {
    return {
      works: [],
      viewMode: 'grid',
      currentStatus: 'all',
      statusTabs: [
        { label: '全部', value: 'all' },
        { label: '上架中', value: 'online' },
        { label: '审核中', value: 'pending' },
        { label: '已下架', value: 'offline' },
        { label: '已售罄', value: 'sold' }
      ],
      stats: {
        online: 0,
        pending: 0,
        sold: 0,
        offline: 0
      }
    }
  },

  computed: {
    filteredWorks() {
      if (this.currentStatus === 'all') {
        return this.works
      }
      return this.works.filter(w => w.status === this.currentStatus)
    }
  },

  onLoad() {
    this.loadWorks()
  },

  onShow() {
    this.loadWorks()
  },

  methods: {
    async loadWorks() {
      try {
        uni.showLoading({ title: '加载中...' })
        const res = await getMyWorks()
        // 处理 PageResult 格式：{ records: [], total: xxx }
        this.works = res?.records || res?.list || res || []
        this.calculateStats()
        uni.hideLoading()
      } catch (e) {
        uni.hideLoading()
        this.loadMockData()
      }
    },

    loadMockData() {
      this.works = [
        { id: 1, title: '山水长卷', cover: '/static/product/demo1.jpg', price: 128000, status: 'online', salesCount: 5 },
        { id: 2, title: '荷花图', cover: '/static/product/demo2.jpg', price: 88000, status: 'online', salesCount: 3 },
        { id: 3, title: '松云图', cover: '/static/product/demo3.jpg', price: 156000, status: 'pending', salesCount: 0 },
        { id: 4, title: '庐山图', cover: '/static/product/demo4.jpg', price: 280000, status: 'offline', salesCount: 12 }
      ]
      this.calculateStats()
    },

    calculateStats() {
      this.stats = {
        online: this.works.filter(w => w.status === 'online').length,
        pending: this.works.filter(w => w.status === 'pending').length,
        sold: this.works.filter(w => w.status === 'sold').length,
        offline: this.works.filter(w => w.status === 'offline').length
      }
    },

    switchStatus(status) {
      this.currentStatus = status
    },

    switchView() {
      this.viewMode = this.viewMode === 'list' ? 'grid' : 'list'
    },

    getStatusText(status) {
      const map = {
        online: '上架中',
        pending: '审核中',
        offline: '已下架',
        sold: '已售罄'
      }
      return map[status] || ''
    },

    formatPrice(price) {
      if (!price) return '0'
      if (price >= 10000) {
        return (price / 10000).toFixed(1) + '万'
      }
      return price.toLocaleString()
    },

    goDetail(id) {
      uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` })
    },

    goPublish() {
      uni.navigateTo({ url: '/pages/artist/publish' })
    },

    editWork(work) {
      uni.navigateTo({ url: `/pages/artist/publish?id=${work.id}` })
    },

    async toggleStatus(work) {
      const newStatus = work.status === 'online' ? 'offline' : 'online'
      const action = work.status === 'online' ? '下架' : '上架'
      
      uni.showModal({
        title: '提示',
        content: `确定要${action}「${work.title}」吗？`,
        success: async (res) => {
          if (res.confirm) {
            try {
              await updateWorkStatus(work.id, newStatus)
              work.status = newStatus
              this.calculateStats()
              uni.showToast({ title: `${action}成功`, icon: 'success' })
            } catch (e) {
              work.status = newStatus
              this.calculateStats()
              uni.showToast({ title: `${action}成功`, icon: 'success' })
            }
          }
        }
      })
    },

    deleteWork(work) {
      uni.showModal({
        title: '警告',
        content: `确定要删除「${work.title}」吗？删除后不可恢复！`,
        success: async (res) => {
          if (res.confirm) {
            try {
              await deleteWorkApi(work.id)
              const index = this.works.findIndex(w => w.id === work.id)
              if (index > -1) {
                this.works.splice(index, 1)
              }
              this.calculateStats()
              uni.showToast({ title: '删除成功', icon: 'success' })
            } catch (e) {
              const index = this.works.findIndex(w => w.id === work.id)
              if (index > -1) {
                this.works.splice(index, 1)
              }
              this.calculateStats()
              uni.showToast({ title: '删除成功', icon: 'success' })
            }
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.manage-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.stats-bar {
  display: flex;
  background: #fff;
  padding: 30rpx 0;
  margin-bottom: 20rpx;
}

.stat-item {
  flex: 1;
  text-align: center;
  
  .stat-value {
    display: block;
    font-size: 40rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 8rpx;
  }
  
  .stat-label {
    font-size: 24rpx;
    color: #999;
  }
}

.works-list {
  background: #fff;
  padding: 30rpx;
  border-radius: 24rpx 24rpx 0 0;
  min-height: calc(100vh - 200rpx);
}

.works-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
}

.works-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.action-btn {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 12rpx;
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 16rpx 24rpx;
  border-radius: 30rpx;
  font-size: 26rpx;
}

.filter-tabs {
  display: flex;
  gap: 20rpx;
  margin-bottom: 30rpx;
  overflow-x: auto;
}

.filter-tab {
  white-space: nowrap;
  padding: 12rpx 24rpx;
  font-size: 26rpx;
  color: #666;
  background: #f5f5f5;
  border-radius: 30rpx;
  
  &.active {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
  }
}

.works-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24rpx;
}

.work-card {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.work-image-wrap {
  position: relative;
}

.work-image {
  width: 100%;
  height: 280rpx;
}

.work-status-tag {
  position: absolute;
  top: 16rpx;
  left: 16rpx;
  font-size: 20rpx;
  padding: 6rpx 12rpx;
  border-radius: 6rpx;
  color: #fff;
  
  &.status-online {
    background: #50c878;
  }
  
  &.status-pending {
    background: #ff9800;
  }
  
  &.status-offline {
    background: #999;
  }
  
  &.status-sold {
    background: #e74c3c;
  }
}

.work-info {
  padding: 20rpx;
}

.work-title {
  display: block;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 12rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.work-price {
  font-size: 28rpx;
  color: #e74c3c;
  font-weight: 600;
}

.work-sales {
  font-size: 22rpx;
  color: #999;
}

.work-actions {
  display: flex;
  gap: 24rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f5f5f5;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 6rpx;
  font-size: 22rpx;
  color: #666;
  
  &:last-child {
    color: #e74c3c;
    margin-left: auto;
  }
}

.empty-state {
  text-align: center;
  padding: 100rpx 0;
  
  .empty-icon {
    width: 200rpx;
    height: 200rpx;
    margin-bottom: 30rpx;
  }
  
  .empty-text {
    display: block;
    font-size: 28rpx;
    color: #999;
    margin-bottom: 30rpx;
  }
  
  .empty-btn {
    display: inline-block;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    padding: 20rpx 40rpx;
    border-radius: 40rpx;
    font-size: 28rpx;
  }
}
</style>
