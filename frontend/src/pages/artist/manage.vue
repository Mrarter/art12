<template>
  <view class="manage-page">
    <view class="art-nav">
      <view class="nav-icon" @click="goBack">
        <image src="/static/art-icons/icon-back.svg" mode="aspectFit"></image>
      </view>
      <view class="nav-title">作品管理</view>
      <view class="nav-action" @click="goPublish">发布作品</view>
    </view>

    <view class="manage-hero">
      <view class="hero-kicker">Artist Work Console</view>
      <view class="hero-title">管理你的上架作品与流转状态</view>
      <view class="hero-desc">发布、编辑、上下架和查看成交情况都会在这里汇总。</view>
    </view>

    <view class="stats-grid">
      <view class="stat-card" v-for="item in statCards" :key="item.key">
        <view class="stat-label">{{ item.label }}</view>
        <view class="stat-value">{{ stats[item.key] }}</view>
      </view>
    </view>

    <view class="notice-card" v-if="certificateNoticeCount">
      <view class="notice-copy">
        <view class="notice-title">待签署收藏证书 {{ certificateNoticeCount }} 件</view>
        <view class="notice-desc">{{ latestCertificateNotice.content }}</view>
      </view>
      <view class="notice-action" @click="goCertificateNotice(latestCertificateNotice)">去处理</view>
    </view>

    <view class="panel-card">
      <view class="panel-head">
        <view>
          <view class="panel-title">我的作品</view>
          <view class="panel-subtitle">共 {{ works.length }} 件，{{ currentStatusLabel }}</view>
        </view>
        <view class="head-actions">
          <view
            class="view-toggle"
            :class="{ active: viewMode === 'grid' }"
            @click="viewMode = 'grid'"
          >卡片</view>
          <view
            class="view-toggle"
            :class="{ active: viewMode === 'list' }"
            @click="viewMode = 'list'"
          >列表</view>
        </view>
      </view>

      <scroll-view class="filter-tabs" scroll-x enable-flex>
        <view
          v-for="tab in statusTabs"
          :key="tab.value"
          class="filter-tab"
          :class="{ active: currentStatus === tab.value }"
          @click="switchStatus(tab.value)"
        >
          {{ tab.label }}
        </view>
      </scroll-view>

      <view v-if="filteredWorks.length" :class="viewMode === 'grid' ? 'works-grid' : 'works-list'">
        <view
          class="work-card"
          :class="{ 'list-mode': viewMode === 'list' }"
          v-for="work in filteredWorks"
          :key="work.id"
        >
          <view class="work-cover-wrap" @click="goDetail(work.id)">
            <image class="work-cover" :src="work.cover || fallbackCover" mode="aspectFill"></image>
            <view class="work-status-tag" :class="'status-' + work.status">
              {{ getStatusText(work.status) }}
            </view>
          </view>

          <view class="work-body">
            <view class="work-topline">
              <view class="work-title" @click="goDetail(work.id)">{{ work.title || '未命名作品' }}</view>
              <view class="work-price">¥{{ formatPrice(work.price) }}</view>
            </view>

            <view class="work-meta">
              <text>{{ work.year || '年份待补充' }}</text>
              <text v-if="work.categoryName || work.category">{{ work.categoryName || work.category }}</text>
              <text>{{ getSalesText(work.salesCount) }}</text>
            </view>

            <view class="work-desc" v-if="work.description">{{ work.description }}</view>
            <view class="work-notice" v-if="getCertificateNotice(work)" @click="goCertificateNotice(getCertificateNotice(work))">
              <text>待签署收藏证书，签署后可获得认证费用</text>
              <text>去处理 ›</text>
            </view>

            <view class="work-actions">
              <view
                class="action-item"
                :class="{ disabled: !canEditWork(work) }"
                @click="editWork(work)"
              >编辑</view>
              <view
                class="action-item"
                :class="{ disabled: !canToggleStatus(work) }"
                @click="toggleStatus(work)"
              >
                {{ work.status === 'online' ? '下架' : '上架' }}
              </view>
              <view
                class="action-item danger"
                :class="{ disabled: !canDeleteWork(work) }"
                @click="deleteWork(work)"
              >删除</view>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="empty-state">
        <image class="empty-icon" src="/static/art-icons/icon-work.svg" mode="aspectFit"></image>
        <view class="empty-title">{{ currentStatus === 'all' ? '还没有作品' : `暂无${currentStatusLabel}` }}</view>
        <view class="empty-desc">{{ usingMockData ? '当前为演示数据模式，可直接体验作品操作。' : '发布后会同步进入艺术家主页、画廊列表和交易链路。' }}</view>
        <button class="empty-action" @click="goPublish">发布第一件作品</button>
      </view>
    </view>
  </view>
</template>

<script>
import { getMyWorks, updateWorkStatus, deleteWork as deleteWorkApi } from '@/api/product.js'
import { getCurrentUserIdentity } from '@/utils/auth'
import { getUserCertificateSignNotices, markCertificateSignNoticesReadByArtwork } from '@/utils/certificateNotice'
import { formatYuanNumber } from '@/utils/price'

const STATUS_OPTIONS = [
  { label: '全部', value: 'all' },
  { label: '上架中', value: 'online' },
  { label: '审核中', value: 'pending' },
  { label: '已下架', value: 'offline' },
  { label: '已收藏', value: 'sold' }
]

export default {
  data() {
    return {
      works: [],
      fallbackCover: '/static/art-icons/icon-preview.svg',
      viewMode: 'grid',
      currentStatus: 'all',
      statusTabs: STATUS_OPTIONS,
      usingMockData: false,
      certificateNotices: [],
      stats: {
        online: 0,
        pending: 0,
        sold: 0,
        offline: 0
      }
    }
  },

  computed: {
    statCards() {
      return [
        { key: 'online', label: '上架中' },
        { key: 'pending', label: '审核中' },
        { key: 'sold', label: '已收藏' },
        { key: 'offline', label: '已下架' }
      ]
    },
    currentStatusLabel() {
      const current = this.statusTabs.find(item => item.value === this.currentStatus)
      return current ? current.label : '全部作品'
    },
    certificateNoticeCount() {
      return this.certificateNotices.length
    },
    latestCertificateNotice() {
      return this.certificateNotices[0] || {}
    },
    filteredWorks() {
      const list = this.currentStatus === 'all'
        ? this.works
        : this.works.filter(item => item.status === this.currentStatus)
      const statusPriority = {
        online: 0,
        pending: 1,
        offline: 2,
        sold: 3
      }
      return [...list].sort((a, b) => {
        if (this.currentStatus === 'all') {
          const priorityDiff = (statusPriority[a.status] ?? 99) - (statusPriority[b.status] ?? 99)
          if (priorityDiff !== 0) return priorityDiff
        }
        const aTime = Number(a.updatedAt || a.createTime || a.id || 0)
        const bTime = Number(b.updatedAt || b.createTime || b.id || 0)
        return bTime - aTime
      })
    }
  },

  onShow() {
    this.loadWorks()
  },

  methods: {
    async loadWorks() {
      uni.showLoading({ title: '加载中...' })
      try {
        const res = await getMyWorks()
        const rows = Array.isArray(res) ? res : (res?.records || res?.list || [])
        this.usingMockData = false
        this.works = rows.map(this.normalizeWork)
        this.calculateStats()
        this.refreshCertificateNotices()
      } catch (error) {
        console.error('[artist-manage] 加载作品失败:', error)
        this.usingMockData = true
        this.loadMockData()
        this.refreshCertificateNotices()
      } finally {
        uni.hideLoading()
      }
    },

    loadMockData() {
      this.works = [
        {
          id: 1001,
          title: '山湖晨光',
          cover: '/static/product/demo1.jpg',
          price: 128000,
          status: 'online',
          salesCount: 5,
          year: '2024',
          categoryName: '布面油画',
          description: '杭州系列近作，已进入画廊推荐位。'
        },
        {
          id: 1002,
          title: '静物与花器',
          cover: '/static/product/demo2.jpg',
          price: 88000,
          status: 'pending',
          salesCount: 0,
          year: '2023',
          categoryName: '纸本水墨',
          description: '等待平台审核，可继续编辑资料。'
        },
        {
          id: 1003,
          title: '松风图',
          cover: '/static/product/demo3.jpg',
          price: 156000,
          status: 'sold',
          salesCount: 1,
          year: '2022',
          categoryName: '国画',
          description: '已成交并进入收藏档案。'
        },
        {
          id: 1004,
          title: '岸边人物',
          cover: '/static/product/demo4.jpg',
          price: 98000,
          status: 'offline',
          salesCount: 2,
          year: '2021',
          categoryName: '油画',
          description: '暂时下架，等待重新调价。'
        }
      ]
      this.calculateStats()
    },

    calculateStats() {
      this.stats = {
        online: this.works.filter(item => item.status === 'online').length,
        pending: this.works.filter(item => item.status === 'pending').length,
        sold: this.works.filter(item => item.status === 'sold').length,
        offline: this.works.filter(item => item.status === 'offline').length
      }
    },

    normalizeWork(work = {}) {
      return {
        ...work,
        cover: work.cover || work.coverImage || this.fallbackCover,
        title: work.title || work.name || '',
        year: work.year || work.createdYear || '',
        price: Number(work.price || 0),
        salesCount: Number(work.salesCount || work.sales || 0),
        status: this.normalizeStatus(work.status),
        description: work.description || work.introduction || ''
      }
    },

    normalizeStatus(status) {
      const value = String(status)
      if (value === 'online' || value === '1') return 'online'
      if (value === 'offline' || value === '0') return 'offline'
      if (value === 'sold' || value === '2') return 'sold'
      if (value === 'pending' || value === '3' || value === '-1') return 'pending'
      return 'offline'
    },

    getStatusText(status) {
      const map = {
        online: '上架中',
        pending: '审核中',
        offline: '已下架',
        sold: '已收藏'
      }
      return map[status] || '待处理'
    },

    getSalesText(count) {
      return `已售 ${Number(count || 0)}`
    },

    switchStatus(status) {
      this.currentStatus = status
    },

    refreshCertificateNotices() {
      const currentUserId = getCurrentUserIdentity().id
      const notices = getUserCertificateSignNotices(currentUserId)
      this.certificateNotices = notices.filter(item => this.works.some(work => String(work.id) === String(item.artworkId)))
    },

    getCertificateNotice(work) {
      return this.certificateNotices.find(item => String(item.artworkId) === String(work.id)) || null
    },

    canToggleStatus(work) {
      return work.status === 'online' || work.status === 'offline'
    },

    canEditWork(work) {
      return work.status !== 'sold'
    },

    canDeleteWork(work) {
      return work.status !== 'sold'
    },

    formatPrice(price) {
      const value = Number(price || 0)
      if (!value) return '0.00'
      const normalized = value >= 1000 ? value / 100 : value
      return formatYuanNumber(normalized)
    },

    goBack() {
      const pages = getCurrentPages()
      if (pages && pages.length > 1) {
        uni.navigateBack()
      } else {
        uni.switchTab({ url: '/pages/user/index' })
      }
    },

    goDetail(id) {
      uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` })
    },

    goPublish() {
      uni.navigateTo({ url: '/pages/artist/publish' })
    },

    goCertificateNotice(notice) {
      if (!notice?.artworkId) return
      markCertificateSignNoticesReadByArtwork(notice.artworkId)
      this.refreshCertificateNotices()
      uni.navigateTo({ url: `/pages/gallery/detail?id=${notice.artworkId}` })
    },

    editWork(work) {
      if (!this.canEditWork(work)) {
        uni.showToast({ title: '已收藏作品不可再编辑', icon: 'none' })
        return
      }
      uni.navigateTo({ url: `/pages/artist/publish?id=${work.id}` })
    },

    async toggleStatus(work) {
      if (!this.canToggleStatus(work)) return
      const newStatus = work.status === 'online' ? 'offline' : 'online'
      const action = newStatus === 'online' ? '上架' : '下架'

      uni.showModal({
        title: '确认操作',
        content: `确定要${action}「${work.title || '该作品'}」吗？`,
        success: async ({ confirm }) => {
          if (!confirm) return

          const previousStatus = work.status
          if (this.usingMockData) {
            work.status = newStatus
            this.calculateStats()
            uni.showToast({ title: `${action}成功`, icon: 'success' })
            return
          }

          try {
            await updateWorkStatus(work.id, newStatus)
            work.status = newStatus
            this.calculateStats()
            uni.showToast({ title: `${action}成功`, icon: 'success' })
          } catch (error) {
            work.status = previousStatus
            uni.showToast({ title: error?.message || `${action}失败`, icon: 'none' })
          }
        }
      })
    },

    deleteWork(work) {
      if (!this.canDeleteWork(work)) {
        uni.showToast({ title: '已收藏作品不可删除', icon: 'none' })
        return
      }
      uni.showModal({
        title: '删除作品',
        content: `删除后不可恢复，确认删除「${work.title || '该作品'}」吗？`,
        success: async ({ confirm }) => {
          if (!confirm) return

          const index = this.works.findIndex(item => item.id === work.id)
          if (index < 0) return

          if (this.usingMockData) {
            this.works.splice(index, 1)
            this.calculateStats()
            uni.showToast({ title: '删除成功', icon: 'success' })
            return
          }

          try {
            await deleteWorkApi(work.id)
            this.works.splice(index, 1)
            this.calculateStats()
            uni.showToast({ title: '删除成功', icon: 'success' })
          } catch (error) {
            uni.showToast({ title: error?.message || '删除失败', icon: 'none' })
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
  padding: calc(112rpx + env(safe-area-inset-top)) 24rpx 40rpx;
  box-sizing: border-box;
  color: #f6efe2;
  background:
    radial-gradient(circle at top, rgba(211, 166, 74, 0.16), transparent 34%),
    linear-gradient(180deg, #060606 0%, #111111 36%, #171513 100%);
}

.art-nav {
  position: fixed;
  z-index: 30;
  top: 0;
  left: 0;
  right: 0;
  height: calc(88rpx + env(safe-area-inset-top));
  padding: env(safe-area-inset-top) 24rpx 0;
  box-sizing: border-box;
  display: grid;
  grid-template-columns: 72rpx 1fr auto;
  align-items: center;
  background: rgba(6, 6, 6, 0.94);
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(10px);
}

.nav-icon {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.nav-icon image {
  width: 34rpx;
  height: 34rpx;
}

.nav-title {
  text-align: center;
  font-size: 30rpx;
  font-weight: 600;
}

.nav-action {
  min-width: 132rpx;
  padding: 14rpx 22rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  text-align: center;
  color: #201100;
  background: linear-gradient(135deg, #f1ca6e 0%, #d99a31 100%);
}

.manage-hero {
  padding: 18rpx 4rpx 10rpx;
}

.hero-kicker {
  font-size: 20rpx;
  letter-spacing: 4rpx;
  color: rgba(231, 185, 62, 0.72);
  text-transform: uppercase;
}

.hero-title {
  margin-top: 18rpx;
  font-size: 44rpx;
  line-height: 1.25;
  font-weight: 600;
  color: #f7f0e4;
}

.hero-desc {
  margin-top: 14rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.62);
}

.stats-grid {
  margin-top: 28rpx;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.notice-card {
  margin-top: 22rpx;
  padding: 22rpx 24rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
  border: 1rpx solid rgba(231, 185, 62, 0.16);
  border-radius: 18rpx;
  background: linear-gradient(135deg, rgba(46, 34, 13, 0.96), rgba(23, 18, 11, 0.96));
  box-shadow: 0 16rpx 36rpx rgba(0, 0, 0, 0.18);
}

.notice-copy {
  flex: 1;
  min-width: 0;
}

.notice-title {
  font-size: 27rpx;
  font-weight: 700;
  color: #f3ce73;
}

.notice-desc {
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: rgba(255, 244, 221, 0.76);
}

.notice-action {
  flex-shrink: 0;
  min-width: 112rpx;
  height: 60rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 23rpx;
  font-weight: 700;
  color: #241701;
  background: linear-gradient(135deg, #f1ce4a 0%, #d5a91c 100%);
}

.stat-card {
  padding: 24rpx 22rpx;
  border: 1rpx solid rgba(231, 185, 62, 0.14);
  border-radius: 18rpx;
  background: linear-gradient(145deg, rgba(31, 29, 25, 0.96), rgba(15, 14, 12, 0.96));
  box-shadow: 0 16rpx 36rpx rgba(0, 0, 0, 0.18);
}

.stat-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.58);
}

.stat-value {
  margin-top: 14rpx;
  font-size: 44rpx;
  line-height: 1;
  font-weight: 600;
  color: #f6ede1;
}

.panel-card {
  margin-top: 24rpx;
  padding: 28rpx 24rpx 30rpx;
  border-radius: 24rpx;
  border: 1rpx solid rgba(231, 185, 62, 0.12);
  background: linear-gradient(160deg, rgba(24, 22, 19, 0.98), rgba(11, 11, 11, 0.98));
  box-shadow: 0 20rpx 44rpx rgba(0, 0, 0, 0.24);
  color: #f4ecdf;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.panel-title {
  font-size: 34rpx;
  font-weight: 600;
}

.panel-subtitle {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: rgba(244, 236, 223, 0.56);
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.view-toggle {
  min-width: 84rpx;
  height: 56rpx;
  padding: 0 18rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  color: rgba(244, 236, 223, 0.62);
  background: rgba(255, 255, 255, 0.08);
}

.view-toggle.active {
  color: #211700;
  background: linear-gradient(135deg, #f1ce4a 0%, #d5a91c 100%);
}

.filter-tabs {
  margin-top: 28rpx;
  white-space: nowrap;
  scrollbar-width: none;
}

.filter-tabs::-webkit-scrollbar {
  display: none;
}

.filter-tab {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  padding: 0 24rpx;
  height: 60rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  color: rgba(244, 236, 223, 0.72);
  background: rgba(255, 255, 255, 0.08);
}

.filter-tab.active {
  color: #211700;
  background: linear-gradient(135deg, #f1ce4a 0%, #d5a91c 100%);
}

.works-grid {
  margin-top: 24rpx;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20rpx;
}

.works-list {
  margin-top: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.work-card {
  overflow: hidden;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
  border-radius: 20rpx;
  background: linear-gradient(180deg, rgba(37, 34, 30, 0.98), rgba(23, 21, 18, 0.98));
  box-shadow: 0 16rpx 32rpx rgba(0, 0, 0, 0.18);
}

.work-card.list-mode {
  display: flex;
}

.work-cover-wrap {
  position: relative;
}

.work-card.list-mode .work-cover-wrap {
  width: 240rpx;
  flex-shrink: 0;
}

.work-cover {
  width: 100%;
  height: 260rpx;
  display: block;
  background: #2c2925;
}

.work-card.list-mode .work-cover {
  height: 100%;
  min-height: 240rpx;
}

.work-status-tag {
  position: absolute;
  top: 16rpx;
  left: 16rpx;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  line-height: 1;
  color: #fff;
}

.status-online {
  background: rgba(46, 164, 100, 0.92);
}

.status-pending {
  background: rgba(233, 161, 23, 0.92);
}

.status-offline {
  background: rgba(104, 98, 91, 0.92);
}

.status-sold {
  background: rgba(217, 82, 82, 0.92);
}

.work-body {
  padding: 22rpx 20rpx 20rpx;
  display: flex;
  flex: 1;
  flex-direction: column;
}

.work-topline {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  align-items: flex-start;
}

.work-title {
  flex: 1;
  min-width: 0;
  font-size: 28rpx;
  line-height: 1.4;
  font-weight: 600;
  color: #f6efe3;
  word-break: break-all;
}

.work-price {
  flex-shrink: 0;
  font-size: 28rpx;
  font-weight: 600;
  color: #cb5036;
}

.work-meta {
  margin-top: 10rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx 14rpx;
  font-size: 20rpx;
  color: rgba(246, 239, 227, 0.54);
}

.work-desc {
  margin-top: 14rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: rgba(246, 239, 227, 0.72);
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.work-notice {
  margin-top: 14rpx;
  padding: 14rpx 16rpx;
  border-radius: 14rpx;
  display: flex;
  justify-content: space-between;
  gap: 12rpx;
  font-size: 20rpx;
  line-height: 1.5;
  color: #f0cb6b;
  background: rgba(213, 169, 28, 0.1);
}

.work-actions {
  margin-top: auto;
  padding-top: 18rpx;
  display: flex;
  gap: 12rpx;
}

.action-item {
  flex: 1;
  height: 58rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  color: #efe5d7;
  background: rgba(255, 255, 255, 0.08);
}

.action-item.disabled {
  color: rgba(239, 229, 215, 0.28);
  background: rgba(255, 255, 255, 0.04);
}

.action-item.danger {
  color: #c55241;
  background: rgba(197, 82, 65, 0.1);
}

.action-item.danger.disabled {
  color: rgba(239, 229, 215, 0.28);
  background: rgba(255, 255, 255, 0.04);
}

.empty-state {
  padding: 96rpx 24rpx 74rpx;
  text-align: center;
}

.empty-icon {
  width: 96rpx;
  height: 96rpx;
  opacity: 0.72;
}

.empty-title {
  margin-top: 26rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: #f6efe3;
}

.empty-desc {
  margin-top: 14rpx;
  font-size: 23rpx;
  line-height: 1.7;
  color: rgba(246, 239, 227, 0.58);
}

.empty-action {
  margin-top: 30rpx;
  width: 280rpx;
  height: 78rpx;
  line-height: 78rpx;
  border: 0;
  border-radius: 999rpx;
  color: #fff;
  font-size: 26rpx;
  background: linear-gradient(135deg, #6f5ee8 0%, #4c32d2 100%);
}

.empty-action::after {
  border: 0;
}
</style>
