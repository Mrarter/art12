<template>
  <view class="bidding-page">
    <!-- 拍品信息 -->
    <view class="artwork-section">
      <image
        class="artwork-image"
        :src="lotInfo.coverImage || lotInfo.image || '/static/icons/artwork-default.png'"
        mode="aspectFill"
      ></image>
      <view class="artwork-info">
        <view class="lot-no">Lot {{ lotInfo.lotNo }}</view>
        <view class="artwork-name">{{ lotInfo.title || lotInfo.artworkName || '拍品详情' }}</view>
        <view class="artwork-author">{{ lotInfo.artistName || '未知艺术家' }}</view>
        <view class="artwork-meta" v-if="lotInfo.width">
          {{ lotInfo.width }} × {{ lotInfo.height }}cm
          <text v-if="lotInfo.material"> | {{ lotInfo.material }}</text>
        </view>
      </view>
    </view>

    <!-- 价格信息 -->
    <view class="price-section">
      <view class="price-card">
        <view class="price-item">
          <text class="price-label">当前价</text>
          <text class="current-price">¥{{ formatPrice(lotInfo.currentPrice) }}</text>
        </view>
        <view class="price-item">
          <text class="price-label">起拍价</text>
          <text class="start-price">¥{{ formatPrice(lotInfo.startPrice) }}</text>
        </view>
      </view>

      <view class="price-stats">
        <view class="stat-item">
          <text class="stat-value">{{ lotInfo.bidCount || 0 }}</text>
          <text class="stat-label">出价次数</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-value">{{ formatPrice(lotInfo.startPrice) }}</text>
          <text class="stat-label">最低出价</text>
        </view>
      </view>

      <!-- 倒计时 -->
      <view class="countdown-section" v-if="lotInfo.status === 1">
        <text class="countdown-label">剩余时间</text>
        <view class="countdown-time" :class="{ urgent: remainSeconds < 300 }">
          {{ formatCountdown(remainSeconds) }}
        </view>
      </view>

      <!-- 状态提示 -->
      <view class="status-tip" :class="statusClass">
        {{ statusText }}
      </view>
    </view>

    <!-- 出价记录 -->
    <view class="bids-section">
      <view class="section-header">
        <text class="section-title">出价记录</text>
        <text class="bid-count">{{ bids.length }}次出价</text>
      </view>

      <scroll-view class="bids-list" scroll-y>
        <view
          class="bid-item"
          v-for="(bid, index) in bids"
          :key="bid.id"
          :class="{ 'is-winning': index === 0 }"
        >
          <view class="bid-user">
            <image
              class="bid-avatar"
              :src="bid.userAvatar || '/static/icons/avatar-default.png'"
              mode="aspectFill"
            ></image>
            <view class="bid-user-info">
              <text class="bid-nickname">{{ bid.userName || '匿名用户' }}</text>
              <text class="bid-time">{{ formatBidTime(bid.bidTime) }}</text>
            </view>
          </view>
          <view class="bid-price-wrapper">
            <text class="bid-price">¥{{ formatPrice(bid.bidPrice) }}</text>
            <text class="winning-tag" v-if="index === 0">领先</text>
          </view>
        </view>
      </scroll-view>

      <!-- 空状态 -->
      <view class="empty-bids" v-if="bids.length === 0">
        <text>暂无出价记录</text>
      </view>
    </view>

    <!-- 出价弹幕 -->
    <view class="danmu-container">
      <view
        class="danmu-item"
        v-for="(danmu, index) in danmuList"
        :key="index"
        :style="{ animationDelay: danmu.delay + 's' }"
      >
        <text class="danmu-user">{{ danmu.userName }}</text>
        <text class="danmu-price">¥{{ formatPrice(danmu.price) }}</text>
      </view>
    </view>

    <!-- 底部出价区域 -->
    <view class="bid-bottom" v-if="lotInfo.status === 1">
      <view class="quick-bid">
        <view
          class="quick-btn"
          v-for="price in quickPrices"
          :key="price"
          @click="setBidPrice(price)"
        >
          +¥{{ formatPrice(price) }}
        </view>
      </view>

      <view class="bid-input-section">
        <view class="bid-input-wrapper">
          <text class="currency">¥</text>
          <input
            class="bid-input"
            type="number"
            v-model="bidPrice"
            :placeholder="'最低 ' + formatPrice(minBidPrice)"
            @input="onPriceInput"
          />
        </view>
        <view class="bid-submit" :class="{ disabled: !canBid }" @click="handleBid">
          <text>立即出价</text>
        </view>
      </view>
    </view>

    <!-- 底部按钮（未开始/已结束） -->
    <view class="bid-bottom disabled" v-else>
      <view class="bid-status-info">
        <text>{{ statusText }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getLotDetail, placeBid, getLotBids } from '@/api/auction'
import websocket from '@/utils/websocket.js'
const { connectAuction, disconnectAuction, onBid, onNewBid, onAuctionEnd } = websocket

export default {
  data() {
    return {
      lotId: null,
      lotInfo: {
        id: null,
        lotNo: '',
        title: '',
        artistName: '',
        currentPrice: 0,
        startPrice: 0,
        bidIncrement: 100,
        bidCount: 0,
        status: 0,
        startTime: null,
        endTime: null
      },
      bids: [],
      bidPrice: '',
      minBidPrice: 0,
      quickPrices: [],
      remainSeconds: 0,
      timer: null,
      danmuList: [],
      wsConnected: false
    }
  },

  computed: {
    canBid() {
      const price = Number(this.bidPrice)
      return price >= this.minBidPrice && this.remainSeconds > 0
    },

    statusText() {
      const statusMap = {
        0: '即将开始',
        1: '正在拍卖',
        2: '已结束'
      }
      return statusMap[this.lotInfo.status] || '未知'
    },

    statusClass() {
      return {
        'status-upcoming': this.lotInfo.status === 0,
        'status-live': this.lotInfo.status === 1,
        'status-ended': this.lotInfo.status === 2
      }
    }
  },

  onLoad(options) {
    if (options.id) {
      this.lotId = Number(options.id)
      // 如果有传入的参数，使用传入的值
      if (options.title) this.lotInfo.title = decodeURIComponent(options.title)
      if (options.currentPrice) this.lotInfo.currentPrice = Number(options.currentPrice)
      if (options.image) this.lotInfo.coverImage = decodeURIComponent(options.image)

      this.loadLotDetail()
      this.loadBids()
      this.startCountdown()
      this.initWebSocket()
    }
  },

  onUnload() {
    this.stopCountdown()
    this.closeWebSocket()
  },

  methods: {
    async loadLotDetail() {
      try {
        const res = await getLotDetail(this.lotId)
        if (res.code === 200) {
          this.lotInfo = { ...this.lotInfo, ...res.data }
          this.calculateMinBid()
          this.generateQuickPrices()
        }
      } catch (e) {
        console.error('加载拍品详情失败', e)
      }
    },

    async loadBids() {
      try {
        const res = await getLotBids(this.lotId, 1, 20)
        if (res.code === 200) {
          this.bids = res.data.records || []
        }
      } catch (e) {
        console.error('加载出价记录失败', e)
      }
    },

    calculateMinBid() {
      // 最低出价 = 当前价 + 加价幅度
      this.minBidPrice = (this.lotInfo.currentPrice || this.lotInfo.startPrice || 0) + (this.lotInfo.bidIncrement || 100)
      this.bidPrice = this.minBidPrice.toString()
    },

    generateQuickPrices() {
      const increment = this.lotInfo.bidIncrement || 100
      this.quickPrices = [increment, increment * 2, increment * 5, increment * 10]
    },

    setBidPrice(increment) {
      const base = this.lotInfo.currentPrice || this.lotInfo.startPrice || 0
      this.bidPrice = (base + increment).toString()
    },

    onPriceInput() {
      // 验证输入
      const price = Number(this.bidPrice)
      if (price < this.minBidPrice) {
        // 可以提示用户
      }
    },

    async handleBid() {
      if (!this.canBid) {
        uni.showToast({ title: '出价金额不能低于最低出价', icon: 'none' })
        return
      }

      try {
        uni.showLoading({ title: '提交中...' })
        const res = await placeBid(this.lotId, Number(this.bidPrice))
        if (res.code === 200) {
          uni.showToast({ title: '出价成功' })
          // 刷新数据
          this.loadLotDetail()
          this.loadBids()
          // 添加弹幕
          this.addDanmu({ userName: '我', price: Number(this.bidPrice) })
        } else {
          uni.showToast({ title: res.message || '出价失败', icon: 'none' })
        }
      } catch (e) {
        uni.showToast({ title: '出价失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },

    startCountdown() {
      this.timer = setInterval(() => {
        if (this.remainSeconds > 0) {
          this.remainSeconds--
          // 更新拍品剩余时间
          if (this.lotInfo.endTime) {
            const endTime = new Date(this.lotInfo.endTime).getTime()
            this.remainSeconds = Math.max(0, Math.floor((endTime - Date.now()) / 1000))
          }
        } else {
          this.stopCountdown()
        }
      }, 1000)
    },

    stopCountdown() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },

    // 连接 WebSocket
    initWebSocket() {
      if (this.lotInfo.status !== 1) return
      
      connectAuction(this.lotId)
        .then(() => {
          this.wsConnected = true
          console.log('拍卖 WebSocket 连接成功')
        })
        .catch(err => {
          console.log('拍卖 WebSocket 连接失败，将使用轮询', err)
        })

      // 监听出价
      onBid((data) => {
        if (data.lotId === this.lotId) {
          this.lotInfo.currentPrice = data.price
          this.lotInfo.bidCount = data.bidCount
          this.calculateMinBid()
          this.addDanmu({ userName: data.userName, price: data.price })
        }
      })

      // 监听新出价弹幕
      onNewBid((data) => {
        if (data.lotId === this.lotId) {
          this.addDanmu({ userName: data.userName, price: data.price })
        }
      })

      // 监听拍卖结束
      onAuctionEnd((data) => {
        if (data.lotId === this.lotId) {
          this.lotInfo.status = 2
          uni.showModal({
            title: '拍卖结束',
            content: data.winnerId === getApp().globalData.userId ? '恭喜您竞拍成功！' : '很遗憾，您未能竞拍成功',
            showCancel: false
          })
        }
      })
    },

    closeWebSocket() {
      if (this.wsConnected) {
        disconnectAuction()
        this.wsConnected = false
      }
    },

    addDanmu(bid) {
      const danmu = {
        userName: bid.userName || '匿名',
        price: bid.price,
        delay: Math.random() * 0.5
      }
      this.danmuList = [...this.danmuList.slice(-5), danmu]
      // 3秒后移除
      setTimeout(() => {
        this.danmuList = this.danmuList.filter(d => d !== danmu)
      }, 3000)
    },

    formatPrice(price) {
      if (!price) return '0'
      return Number(price).toLocaleString()
    },

    formatCountdown(seconds) {
      if (seconds <= 0) return '已结束'
      const hours = Math.floor(seconds / 3600)
      const minutes = Math.floor((seconds % 3600) / 60)
      const secs = seconds % 60
      if (hours > 0) {
        return `${hours}时${minutes}分${secs}秒`
      }
      return `${minutes}分${secs}秒`
    },

    formatBidTime(timeStr) {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      const now = new Date()
      const diff = now - date
      if (diff < 60000) return '刚刚'
      if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
      if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
      return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
    }
  }
}
</script>

<style lang="scss" scoped>
.bidding-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 300rpx;
}

/* 拍品信息 */
.artwork-section {
  background: #fff;
  padding: 30rpx;
  display: flex;
}

.artwork-image {
  width: 200rpx;
  height: 200rpx;
  border-radius: 12rpx;
  margin-right: 24rpx;
  background: #f0f0f0;
}

.artwork-info {
  flex: 1;
}

.lot-no {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 8rpx;
}

.artwork-name {
  font-size: 32rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.artwork-author {
  font-size: 26rpx;
  color: #666;
  margin-bottom: 12rpx;
}

.artwork-meta {
  font-size: 24rpx;
  color: #999;
}

/* 价格信息 */
.price-section {
  background: #fff;
  margin-top: 20rpx;
  padding: 30rpx;
}

.price-card {
  display: flex;
  margin-bottom: 30rpx;
}

.price-item {
  flex: 1;
}

.price-label {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-bottom: 8rpx;
}

.current-price {
  font-size: 48rpx;
  color: #e74c3c;
  font-weight: 700;
}

.start-price {
  font-size: 32rpx;
  color: #666;
}

.price-stats {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-top: 1rpx solid #f0f0f0;
  border-bottom: 1rpx solid #f0f0f0;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 32rpx;
  color: #333;
  font-weight: 600;
}

.stat-label {
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
}

.stat-divider {
  width: 1rpx;
  height: 60rpx;
  background: #f0f0f0;
}

.countdown-section {
  text-align: center;
  padding: 30rpx 0;
}

.countdown-label {
  font-size: 24rpx;
  color: #999;
  margin-right: 12rpx;
}

.countdown-time {
  font-size: 48rpx;
  color: #333;
  font-weight: 700;
  font-family: monospace;
}

.countdown-time.urgent {
  color: #e74c3c;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.status-tip {
  text-align: center;
  padding: 16rpx;
  border-radius: 8rpx;
  font-size: 26rpx;
}

.status-upcoming {
  background: rgba(52, 152, 219, 0.1);
  color: #3498db;
}

.status-live {
  background: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
}

.status-ended {
  background: #f0f0f0;
  color: #999;
}

/* 出价记录 */
.bids-section {
  background: #fff;
  margin-top: 20rpx;
  padding: 30rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 30rpx;
  color: #333;
  font-weight: 600;
}

.bid-count {
  font-size: 24rpx;
  color: #999;
}

.bids-list {
  max-height: 400rpx;
}

.bid-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.bid-item:last-child {
  border-bottom: none;
}

.bid-item.is-winning {
  background: rgba(231, 76, 60, 0.05);
  margin: 0 -30rpx;
  padding: 20rpx 30rpx;
}

.bid-user {
  display: flex;
  align-items: center;
}

.bid-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  margin-right: 16rpx;
  background: #f0f0f0;
}

.bid-user-info {
  display: flex;
  flex-direction: column;
}

.bid-nickname {
  font-size: 28rpx;
  color: #333;
}

.bid-time {
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
}

.bid-price-wrapper {
  display: flex;
  align-items: center;
}

.bid-price {
  font-size: 32rpx;
  color: #333;
  font-weight: 600;
}

.winning-tag {
  margin-left: 12rpx;
  padding: 4rpx 12rpx;
  background: #e74c3c;
  color: #fff;
  font-size: 20rpx;
  border-radius: 20rpx;
}

.empty-bids {
  text-align: center;
  padding: 60rpx 0;
  color: #999;
  font-size: 28rpx;
}

/* 弹幕 */
.danmu-container {
  position: fixed;
  top: 300rpx;
  left: 0;
  right: 0;
  height: 80rpx;
  overflow: hidden;
  pointer-events: none;
  z-index: 100;
}

.danmu-item {
  position: absolute;
  right: -200rpx;
  display: flex;
  align-items: center;
  padding: 10rpx 24rpx;
  background: rgba(0, 0, 0, 0.7);
  border-radius: 30rpx;
  animation: danmuMove 5s linear forwards;
}

.danmu-user {
  color: #fff;
  font-size: 24rpx;
  margin-right: 12rpx;
}

.danmu-price {
  color: #ff6b6b;
  font-size: 28rpx;
  font-weight: 600;
}

@keyframes danmuMove {
  0% { right: -200rpx; }
  100% { right: 110%; }
}

/* 底部出价 */
.bid-bottom {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -2rpx 20rpx rgba(0, 0, 0, 0.1);
}

.bid-bottom.disabled {
  background: #f5f5f5;
}

.quick-bid {
  display: flex;
  margin-bottom: 20rpx;
}

.quick-btn {
  flex: 1;
  text-align: center;
  padding: 12rpx 0;
  margin-right: 16rpx;
  background: #f5f5f5;
  border-radius: 30rpx;
  font-size: 24rpx;
  color: #666;
}

.quick-btn:last-child {
  margin-right: 0;
}

.bid-input-section {
  display: flex;
  align-items: center;
}

.bid-input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  background: #f5f5f5;
  border-radius: 40rpx;
  margin-right: 20rpx;
}

.currency {
  font-size: 36rpx;
  color: #333;
  margin-right: 8rpx;
}

.bid-input {
  flex: 1;
  font-size: 36rpx;
  color: #333;
}

.bid-submit {
  padding: 24rpx 50rpx;
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  color: #fff;
  border-radius: 40rpx;
  font-size: 32rpx;
  font-weight: 500;
}

.bid-submit.disabled {
  background: #ccc;
}

.bid-status-info {
  text-align: center;
  padding: 24rpx;
  font-size: 30rpx;
  color: #999;
}
</style>
