<template>
  <view class="session-page">
    <!-- 专场头部 -->
    <view class="session-header">
      <image
        class="header-bg"
        :src="session.coverImage || '/static/icons/auction-default.png'"
        mode="aspectFill"
      ></image>
      <view class="header-overlay">
        <view class="header-content">
          <text class="session-name">{{ session.name || '拍卖专场' }}</text>
          <text class="session-desc">{{ session.description || '' }}</text>
          <view class="session-time">
            <text class="time-label">拍卖时间</text>
            <text class="time-value">
              {{ formatTime(session.startTime) }} - {{ formatTime(session.endTime) }}
            </text>
          </view>
          <view class="session-stats">
            <view class="stat-item">
              <text class="stat-value">{{ session.lotCount || lots.length }}</text>
              <text class="stat-label">件拍品</text>
            </view>
            <view class="stat-item">
              <text class="stat-value">{{ session.viewCount || 0 }}</text>
              <text class="stat-label">次浏览</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 专场规则 -->
    <view class="rules-section" v-if="session.rules">
      <view class="section-title">拍卖规则</view>
      <view class="rules-content">
        <view class="rule-item" v-for="(rule, index) in formatRules(session.rules)" :key="index">
          <text class="rule-index">{{ index + 1 }}.</text>
          <text class="rule-text">{{ rule }}</text>
        </view>
      </view>
    </view>

    <!-- 拍品列表 -->
    <view class="lots-section">
      <view class="section-header">
        <text class="section-title">拍品列表</text>
        <text class="lot-count">{{ lots.length }}件</text>
      </view>

      <view class="lot-card" v-for="item in lots" :key="item.id" @click="goLotDetail(item.id)">
        <view class="lot-header">
          <view class="lot-no">Lot {{ item.lotNo }}</view>
          <view class="lot-status" :class="getLotStatusClass(item.status)">
            {{ getLotStatusText(item.status) }}
          </view>
        </view>

        <image
          class="lot-image"
          :src="item.coverImage || item.image || '/static/icons/artwork-default.png'"
          mode="aspectFill"
        ></image>

        <view class="lot-info">
          <view class="lot-name">{{ item.title || item.artworkName }}</view>
          <view class="lot-author">{{ item.artistName || '未知艺术家' }}</view>
          <view class="lot-meta">
            <text class="lot-size" v-if="item.width && item.height">{{ item.width }}×{{ item.height }}cm</text>
            <text class="lot-material" v-if="item.material">{{ item.material }}</text>
          </view>
        </view>

        <view class="lot-price">
          <view class="price-row">
            <text class="price-label">当前价</text>
            <text class="current-price">¥{{ formatPrice(item.currentPrice) }}</text>
          </view>
          <view class="price-row">
            <text class="price-label">起拍价</text>
            <text class="start-price">¥{{ formatPrice(item.startPrice) }}</text>
          </view>
        </view>

        <view class="lot-footer">
          <view class="bid-info">
            <text class="bid-count">{{ item.bidCount || 0 }}次出价</text>
            <text class="remain-time" v-if="item.status === 1 && item.remainSeconds">
              剩余 {{ formatCountdown(item.remainSeconds) }}
            </text>
          </view>
          <view class="bid-btn" v-if="item.status === 1" @click.stop="goBidding(item)">
            去出价
          </view>
          <view class="deposit-btn" v-else-if="item.status === 0" @click.stop="handleDeposit(item)">
            缴纳保证金
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="!loading && lots.length === 0">
        <text class="empty-text">暂无拍品</text>
      </view>
    </view>

    <!-- 底部保证金按钮 -->
    <view class="bottom-bar" v-if="session.status === 1 && !hasDeposit && minDeposit > 0">
      <view class="deposit-info">
        <text>需缴纳保证金 ¥{{ minDeposit }}</text>
      </view>
      <view class="deposit-action" @click="handleDepositAll">缴纳保证金参与竞拍</view>
    </view>
  </view>
</template>

<script>
import { getAuctionSessions, getSessionDetail, payDeposit } from '@/api/auction'

export default {
  data() {
    return {
      sessionId: null,
      session: {},
      lots: [],
      loading: false,
      hasDeposit: false,
      minDeposit: 0
    }
  },

  onLoad(options) {
    if (options.id) {
      this.sessionId = Number(options.id)
      this.loadSessionDetail()
    }
  },

  methods: {
    async loadSessionDetail() {
      this.loading = true
      try {
        const res = await getSessionDetail(this.sessionId)
        this.applySessionData(res)
      } catch (e) {
        console.error('加载专场详情失败，尝试从列表恢复', e)
        try {
          await this.loadSessionFallback()
        } catch (fallbackError) {
          uni.showToast({ title: '加载失败', icon: 'none' })
        }
      } finally {
        this.loading = false
      }
    },

    applySessionData(res) {
      const data = res?.data || res || {}
      this.session = data.session || data || {}
      this.lots = data.lots || data.records || []
      this.minDeposit = this.lots.length ? Math.min(...this.lots.map(l => l.depositAmount || 0)) : 0
      this.calculateRemainTime()
    },

    async loadSessionFallback() {
      const statuses = [1, 0, 2]
      for (const status of statuses) {
        const res = await getAuctionSessions({ page: 1, pageSize: 50, status })
        const list = res?.records || res?.list || (Array.isArray(res) ? res : [])
        const found = list.find(item => Number(item.id) === Number(this.sessionId))
        if (found) {
          this.session = found
          this.lots = []
          this.minDeposit = 0
          return
        }
      }
    },

    calculateRemainTime() {
      // 计算每个拍品的剩余时间
      const now = Date.now()
      this.lots = this.lots.map(lot => {
        if (lot.endTime) {
          const endTime = new Date(lot.endTime).getTime()
          lot.remainSeconds = Math.max(0, Math.floor((endTime - now) / 1000))
        }
        return lot
      })
    },

    goLotDetail(lotId) {
      uni.navigateTo({ url: `/pages/auction/bidding?id=${lotId}` })
    },

    goBidding(lot) {
      uni.navigateTo({
        url: `/pages/auction/bidding?id=${lot.id}&title=${encodeURIComponent(lot.title || lot.artworkName || '')}&currentPrice=${lot.currentPrice || lot.startPrice}&image=${encodeURIComponent(lot.coverImage || lot.image || '')}`
      })
    },

    async handleDeposit(lot) {
      try {
        uni.showLoading({ title: '处理中...' })
        await payDeposit(this.sessionId)
        this.hasDeposit = true
        uni.showToast({ title: '缴纳成功' })
      } catch (e) {
        uni.showToast({ title: '缴纳失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },

    handleDepositAll() {
      this.handleDeposit()
    },

    getLotStatusClass(status) {
      return {
        'lot-live': status === 1,
        'lot-upcoming': status === 0,
        'lot-ended': status === 2
      }
    },

    getLotStatusText(status) {
      const texts = { 0: '即将开始', 1: '正在拍卖', 2: '已结束' }
      return texts[status] || '未知'
    },

    formatTime(timeStr) {
      if (!timeStr) return '--'
      const date = new Date(timeStr)
      return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
    },

    formatRules(rules) {
      if (typeof rules === 'string') {
        return rules.split('\n').filter(r => r.trim())
      }
      return []
    },

    formatPrice(price) {
      if (!price) return '0'
      const yuan = price / 100  // 分转元
      if (yuan >= 10000) {
        return (yuan / 10000).toFixed(yuan % 10000 === 0 ? 0 : 1) + '万'
      }
      return yuan.toLocaleString()
    },

    formatCountdown(seconds) {
      if (seconds <= 0) return '已结束'
      const hours = Math.floor(seconds / 3600)
      const minutes = Math.floor((seconds % 3600) / 60)
      const secs = seconds % 60
      if (hours > 24) {
        const days = Math.floor(hours / 24)
        return `${days}天${hours % 24}时`
      }
      return `${hours}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
    }
  }
}
</script>

<style lang="scss" scoped>
.session-page {
  min-height: 100vh;
  background: #0d0d0d;
  padding-bottom: 120rpx;
}

.session-header {
  position: relative;
  height: 400rpx;
}

.header-bg {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #2b2414, #0d0d0d);
}

.header-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.8), transparent);
  padding: 40rpx 30rpx 30rpx;
}

.session-name {
  display: block;
  font-size: 40rpx;
  color: #fff;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.session-desc {
  display: block;
  font-size: 26rpx;
  color: rgba(255,255,255,0.8);
  margin-bottom: 20rpx;
  line-height: 1.4;
}

.session-time {
  margin-bottom: 20rpx;
}

.time-label {
  font-size: 22rpx;
  color: rgba(255,255,255,0.6);
  margin-right: 12rpx;
}

.time-value {
  font-size: 26rpx;
  color: #fff;
}

.session-stats {
  display: flex;
}

.stat-item {
  margin-right: 40rpx;
}

.stat-value {
  font-size: 32rpx;
  color: #fff;
  font-weight: 600;
  margin-right: 8rpx;
}

.stat-label {
  font-size: 22rpx;
  color: rgba(255,255,255,0.7);
}

.rules-section {
  background: #1a1a1a;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
}

.section-title {
  font-size: 30rpx;
  color: #f5f5f5;
  font-weight: 600;
  margin-bottom: 20rpx;
}

.rules-content {
  padding-left: 10rpx;
}

.rule-item {
  display: flex;
  margin-bottom: 12rpx;
  font-size: 26rpx;
  color: #b3b3b3;
  line-height: 1.5;
}

.rule-index {
  margin-right: 8rpx;
  color: #d4af37;
}

.lots-section {
  margin: 0 20rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
}

.lot-count {
  font-size: 24rpx;
  color: #a0a0a0;
}

.lot-card {
  background: #1a1a1a;
  border-radius: 16rpx;
  margin-bottom: 24rpx;
  overflow: hidden;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
}

.lot-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 24rpx;
  background: rgba(255, 255, 255, 0.03);
}

.lot-no {
  font-size: 26rpx;
  color: #d4af37;
  font-weight: 500;
}

.lot-status {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
}

.lot-live {
  background: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
}

.lot-upcoming {
  background: rgba(52, 152, 219, 0.1);
  color: #3498db;
}

.lot-ended {
  background: #f0f0f0;
  color: #999;
}

.lot-image {
  width: 100%;
  height: 400rpx;
  background: #f0f0f0;
}

.lot-info {
  padding: 24rpx;
}

.lot-name {
  font-size: 32rpx;
  color: #f5f5f5;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.lot-author {
  font-size: 26rpx;
  color: #a0a0a0;
  margin-bottom: 12rpx;
}

.lot-meta {
  display: flex;
  font-size: 24rpx;
  color: #888;
}

.lot-size {
  margin-right: 20rpx;
}

.lot-price {
  display: flex;
  padding: 0 24rpx 24rpx;
}

.price-row {
  margin-right: 40rpx;
}

.price-label {
  display: block;
  font-size: 22rpx;
  color: #888;
  margin-bottom: 4rpx;
}

.current-price {
  font-size: 36rpx;
  color: #d4af37;
  font-weight: 600;
}

.start-price {
  font-size: 28rpx;
  color: #b3b3b3;
}

.lot-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 24rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.bid-info {
  display: flex;
  flex-direction: column;
}

.bid-count {
  font-size: 24rpx;
  color: #888;
}

.remain-time {
  font-size: 22rpx;
  color: #e74c3c;
  margin-top: 4rpx;
}

.bid-btn {
  padding: 16rpx 40rpx;
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  color: #fff;
  border-radius: 30rpx;
  font-size: 28rpx;
}

.deposit-btn {
  padding: 16rpx 40rpx;
  background: linear-gradient(135deg, #3498db, #2980b9);
  color: #fff;
  border-radius: 30rpx;
  font-size: 28rpx;
}

.empty-state {
  text-align: center;
  padding: 100rpx 0;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2rpx 20rpx rgba(0, 0, 0, 0.1);
}

.deposit-info {
  font-size: 26rpx;
  color: #666;
}

.deposit-action {
  padding: 20rpx 50rpx;
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  color: #fff;
  border-radius: 40rpx;
  font-size: 30rpx;
  font-weight: 500;
}
</style>
