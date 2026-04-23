<template>
  <view class="auction-detail-page">
    <!-- 顶部导航 -->
    <view class="nav-bar">
      <view class="nav-back" @click="goBack">
        
      </view>
      <view class="nav-title">拍品详情</view>
      <view class="nav-actions">
        <view class="action-item" @click="share">
          <text>↗</text>
        </view>
        <view class="action-item" @click="toggleFavorite">
          
        </view>
      </view>
    </view>

    <!-- 轮播图 -->
    <swiper class="image-swiper" :indicator-dots="true" :autoplay="false" :circular="true">
      <swiper-item v-for="(img, index) in artwork.images" :key="index">
        <image class="swiper-image" :src="img" mode="aspectFill" @click="previewImage(index)"></image>
      </swiper-item>
    </swiper>

    <!-- 拍品信息 -->
    <view class="artwork-info">
      <view class="info-header">
        <view class="artwork-title">{{ artwork.title }}</view>
        <view class="artwork-tags">
          <view class="tag" v-if="artwork.isPremium">精品</view>
          <view class="tag" v-if="artwork.isHot">热拍</view>
          <view class="tag" v-if="artwork.hasGuarantee">保真</view>
        </view>
      </view>
      
      <view class="artwork-meta">
        <view class="meta-item">
          <text class="meta-label">作者</text>
          <text class="meta-value">{{ artwork.author }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-label">年代</text>
          <text class="meta-value">{{ artwork.era }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-label">尺寸</text>
          <text class="meta-value">{{ artwork.size }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-label">材质</text>
          <text class="meta-value">{{ artwork.material }}</text>
        </view>
      </view>

      <view class="artwork-desc">
        <view class="desc-title">作品描述</view>
        <text class="desc-content">{{ artwork.description }}</text>
      </view>
    </view>

    <!-- 拍卖信息 -->
    <view class="auction-info-card">
      <view class="auction-header">
        <view class="auction-title">{{ sessionName }}</view>
        <view class="auction-status" :class="auctionStatus">
          {{ getAuctionStatusText() }}
        </view>
      </view>

      <!-- 价格信息 -->
      <view class="price-section">
        <view class="price-row">
          <view class="price-item">
            <text class="price-label">当前价</text>
            <view class="current-price">
              <text class="currency">¥</text>
              <text class="amount">{{ currentPrice }}</text>
              <view class="price-trend up" v-if="priceChange > 0">
                
                <text>+{{ priceChange }}</text>
              </view>
            </view>
          </view>
          <view class="price-item">
            <text class="price-label">起拍价</text>
            <text class="start-price">¥{{ startPrice }}</text>
          </view>
        </view>
        <view class="price-row">
          <view class="price-item">
            <text class="price-label">加价幅度</text>
            <text class="increment">¥{{ increment }}</text>
          </view>
          <view class="price-item">
            <text class="price-label">保证金</text>
            <text class="deposit">¥{{ deposit }}</text>
          </view>
        </view>
      </view>

      <!-- 倒计时 -->
      <view class="countdown-section" v-if="auctionStatus === 'bidding'">
        <view class="countdown-label">距结束</view>
        <view class="countdown-timer">
          <view class="time-block">
            <text class="time-num">{{ countdown.hours }}</text>
            <text class="time-unit">时</text>
          </view>
          <text class="time-sep">:</text>
          <view class="time-block">
            <text class="time-num">{{ countdown.minutes }}</text>
            <text class="time-unit">分</text>
          </view>
          <text class="time-sep">:</text>
          <view class="time-block">
            <text class="time-num">{{ countdown.seconds }}</text>
            <text class="time-unit">秒</text>
          </view>
        </view>
        <view class="countdown-tip" v-if="countdown.total < 300000">
          
          <text>即将结束，请及时出价</text>
        </view>
      </view>

      <!-- 竞价记录入口 -->
      <view class="bid-records" @click="showBidRecords">
        <view class="records-left">
          <view class="avatar-group">
            <view class="avatar" v-for="(bidder, index) in topBidders" :key="index" :style="{ zIndex: 5 - index }">
              <image :src="bidder.avatar" mode="aspectFill"></image>
            </view>
          </view>
          <text class="records-count">{{ bidCount }}次出价</text>
        </view>
        <view class="records-right">
          <text class="top-bidder">最高: {{ topBidder }}</text>
          
        </view>
      </view>
    </view>

    <!-- 实时出价弹幕 -->
    <view class="bid-barrage" v-if="showBarrage">
      <view 
        class="barrage-item" 
        v-for="(item, index) in barrageList" 
        :key="index"
        :style="{ animationDelay: index * 0.5 + 's' }"
      >
        <image class="barrage-avatar" :src="item.avatar"></image>
        <text class="barrage-text">出价 ¥{{ item.price }}</text>
      </view>
    </view>

    <!-- 出价记录弹窗 -->
    <!-- 弹窗开始 -->
      <view class="records-modal">
        <view class="modal-header">
          <text class="modal-title">出价记录</text>
          <text class="record-count">共 {{ bidCount }} 次出价</text>
        </view>
        <scroll-view class="records-list" scroll-y>
          <view class="record-item" v-for="(record, index) in bidRecords" :key="record.id">
            <view class="record-rank" :class="{ 'top-1': index === 0 }">
              {{ index + 1 }}
            </view>
            <image class="record-avatar" :src="record.avatar"></image>
            <view class="record-info">
              <text class="record-name">{{ record.name }}</text>
              <text class="record-time">{{ record.time }}</text>
            </view>
            <view class="record-price">
              <text class="price">¥{{ record.price }}</text>
              <text class="me" v-if="record.isMe">我的出价</text>
            </view>
          </view>
        </scroll-view>
      </view>
<!-- 弹窗结束 -->

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <view class="bar-left">
        <view class="icon-btn" @click="setReminder">
          
          <text class="btn-text">{{ hasReminder ? '已提醒' : '提醒' }}</text>
        </view>
        <view class="icon-btn" @click="contact">
          
          <text class="btn-text">咨询</text>
        </view>
      </view>
      <view class="bar-right">
        <!-- 未缴纳保证金 -->
        <button class="btn deposit-btn" v-if="!hasDeposit" @click="showDepositModal">
          缴纳保证金
        </button>
        <!-- 已缴纳保证金 -->
        <template v-else>
          <view class="price-display">
            <text class="current">¥{{ myBidPrice || currentPrice }}</text>
            <text class="label">我的出价</text>
          </view>
          <button class="btn bid-btn" @click="showBidModal" :disabled="auctionStatus !== 'bidding'">
            出价
          </button>
        </template>
      </view>
    </view>

    <!-- 出价弹窗 -->
    <!-- 弹窗开始 -->
      <view class="bid-modal">
        <view class="modal-title">我要出价</view>
        
        <view class="current-info">
          <view class="info-row">
            <text class="label">当前最高价</text>
            <text class="value primary">¥{{ currentPrice }}</text>
          </view>
          <view class="info-row">
            <text class="label">最低加价</text>
            <text class="value">¥{{ increment }}</text>
          </view>
        </view>

        <view class="bid-input-section">
          <text class="input-label">出价金额</text>
          <view class="bid-input">
            <text class="currency">¥</text>
            <input 
              type="digit" 
              v-model="bidPrice" 
              :placeholder="'最低 ' + minBidPrice"
              class="input-field"
            />
          </view>
          <view class="quick-prices">
            <view 
              class="quick-item" 
              :class="{ active: bidPrice == minBidPrice }"
              @click="selectBidPrice(minBidPrice)"
            >
              {{ minBidPrice }}
            </view>
            <view 
              class="quick-item" 
              :class="{ active: bidPrice == minBidPrice + increment }"
              @click="selectBidPrice(minBidPrice + increment)"
            >
              {{ minBidPrice + increment }}
            </view>
            <view 
              class="quick-item" 
              :class="{ active: bidPrice == minBidPrice + increment * 2 }"
              @click="selectBidPrice(minBidPrice + increment * 2)"
            >
              {{ minBidPrice + increment * 2 }}
            </view>
          </view>
        </view>

        <view class="bid-tips">
          
          <text>出价即表示您同意《竞拍协议》，出价后无法撤销</text>
        </view>

        <button class="confirm-bid-btn" @click="submitBid" :disabled="!canBid">
          确认出价 ¥{{ bidPrice || 0 }}
        </button>
      </view>
<!-- 弹窗结束 -->

    <!-- 保证金弹窗 -->
    <!-- 弹窗开始 -->
      <view class="deposit-modal">
        <view class="modal-title">缴纳保证金</view>
        
        <view class="deposit-info">
          <view class="deposit-amount">
            <text class="currency">¥</text>
            <text class="amount">{{ deposit }}</text>
          </view>
          <text class="deposit-label">保证金金额</text>
        </view>

        <view class="deposit-rules">
          <view class="rule-title">保证金规则</view>
          <view class="rule-item">1. 缴纳保证金后方可参与竞拍</view>
          <view class="rule-item">2. 竞拍结束后，保证金将原路退回</view>
          <view class="rule-item">3. 竞拍成功后违约，保证金将不予退还</view>
          <view class="rule-item">4. 可用保证金抵扣成交款</view>
        </view>

        <view class="deposit-agreement">
          <checkbox-group @change="onDepositAgreeChange">
            <label class="agreement-label">
              <checkbox value="agree" :checked="depositAgreed" color="#667eea" />
              <text class="agreement-text">我已阅读并同意《保证金协议》</text>
            </label>
          </checkbox-group>
        </view>

        <button class="confirm-deposit-btn" @click="confirmDeposit" :disabled="!depositAgreed">
          确认缴纳 ¥{{ deposit }}
        </button>
      </view>
<!-- 弹窗结束 -->

    <!-- 竞拍成功/失败弹窗 -->
    <!-- 弹窗开始 -->
      <view class="result-modal" :class="resultType">
        <view class="result-icon">
          <:name="resultType === 'success' ? 'checkmark-circle' : 'close-circle'" 
            :size="80" :color="resultType === 'success' ? '#07c160' : '#ff453a'">
        </view>
        <view class="result-title">{{ resultTitle }}</view>
        <view class="result-desc">{{ resultDesc }}</view>
        <view class="result-actions">
          <button class="result-btn primary" @click="onResultConfirm">{{ resultBtnText }}</button>
          <button class="result-btn secondary" @click="showResultModal = false" v-if="resultType === 'success'">
            再看看
          </button>
        </view>
      </view>
<!-- 弹窗结束 -->
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

// 路由参数
const itemId = ref('')
const sessionId = ref('')

// 拍品信息
const artwork = ref({
  id: '',
  title: '张大千山水画《溪山行旅图》',
  author: '张大千',
  era: '近现代',
  size: '136cm × 68cm',
  material: '纸本设色',
  description: '此幅《溪山行旅图》为张大千先生晚年泼墨泼彩代表之作，画面气势磅礴，山峦层叠，云雾缭绕，飞瀑直流而下，笔法苍劲有力，墨色淋漓痛快。作品收录于《张大千画集》，具有极高的艺术价值和收藏价值。',
  images: [
    'https://picsum.photos/750/750?random=1',
    'https://picsum.photos/750/750?random=2',
    'https://picsum.photos/750/750?random=3'
  ],
  isPremium: true,
  isHot: true,
  hasGuarantee: true
})

const sessionName = ref('2024春季艺术品拍卖会·中国书画专场')
const auctionStatus = ref('bidding') // bidding/ended/settled

// 价格信息
const currentPrice = ref(128000)
const startPrice = ref(50000)
const increment = ref(1000)
const deposit = ref(20000)
const priceChange = ref(2000)

// 我的出价
const hasDeposit = ref(true)
const myBidPrice = ref(125000)

// 倒计时
const countdown = ref({
  hours: '02',
  minutes: '45',
  seconds: '30',
  total: 9900000
})

// 出价记录
const bidCount = ref(28)
const topBidder = ref('艺术收藏家***')
const topBidders = ref([
  { avatar: 'https://picsum.photos/50/50?random=10' },
  { avatar: 'https://picsum.photos/50/50?random=11' },
  { avatar: 'https://picsum.photos/50/50?random=12' },
  { avatar: 'https://picsum.photos/50/50?random=13' },
  { avatar: 'https://picsum.photos/50/50?random=14' }
])
const bidRecords = ref([
  { id: 1, name: '艺术收藏家***', avatar: 'https://picsum.photos/50/50?random=10', price: 128000, time: '刚刚', isMe: false },
  { id: 2, name: '书画爱好者***', avatar: 'https://picsum.photos/50/50?random=11', price: 127000, time: '5分钟前', isMe: false },
  { id: 3, name: '我', avatar: 'https://picsum.photos/50/50?random=20', price: 125000, time: '10分钟前', isMe: true },
  { id: 4, name: '收藏达人***', avatar: 'https://picsum.photos/50/50?random=12', price: 123000, time: '15分钟前', isMe: false },
  { id: 5, name: '投资者***', avatar: 'https://picsum.photos/50/50?random=13', price: 121000, time: '20分钟前', isMe: false }
])

// 弹幕
const showBarrage = ref(true)
const barrageList = ref([
  { avatar: 'https://picsum.photos/30/30?random=30', price: 126000 },
  { avatar: 'https://picsum.photos/30/30?random=31', price: 125000 },
  { avatar: 'https://picsum.photos/30/30?random=32', price: 123000 }
])

// 弹窗状态
const showRecordsModal = ref(false)
const showBidModal = ref(false)
const showDepositModal = ref(false)
const showResultModal = ref(false)

// 出价相关
const bidPrice = ref('')
const depositAgreed = ref(false)

// 结果弹窗
const resultType = ref('success')
const resultTitle = ref('')
const resultDesc = ref('')
const resultBtnText = ref('')

// 其他状态
const isFavorite = ref(false)
const hasReminder = ref(false)

// 计算属性
const minBidPrice = computed(() => {
  const base = myBidPrice.value || currentPrice.value
  return base + increment.value
})

const canBid = computed(() => {
  const price = parseFloat(bidPrice.value) || 0
  return price >= minBidPrice.value
})

// 定时器
let countdownTimer = null
let wsTimer = null

// 方法
const goBack = () => {
  uni.navigateBack()
}

const previewImage = (index) => {
  uni.previewImage({
    urls: artwork.value.images,
    current: index
  })
}

const share = () => {
  uni.showShareMenu({
    withShareTicket: true
  })
}

const toggleFavorite = () => {
  isFavorite.value = !isFavorite.value
  uni.showToast({
    title: isFavorite.value ? '已收藏' : '已取消收藏',
    icon: 'success'
  })
}

const getAuctionStatusText = () => {
  const map = {
    bidding: '竞拍中',
    ended: '已结束',
    settled: '已成交'
  }
  return map[auctionStatus.value] || auctionStatus.value
}

const showBidRecords = () => {
  showRecordsModal.value = true
}

const setReminder = () => {
  hasReminder.value = !hasReminder.value
  uni.showToast({
    title: hasReminder.value ? '已设置提醒' : '已取消提醒',
    icon: 'success'
  })
}

const contact = () => {
  uni.makePhoneCall({
    phoneNumber: '400-888-8888'
  })
}

const showDepositModal_ = () => {
  showDepositModal.value = true
}

const onDepositAgreeChange = (e) => {
  depositAgreed.value = e.detail.value.includes('agree')
}

const confirmDeposit = () => {
  uni.showLoading({ title: '支付中...' })
  setTimeout(() => {
    uni.hideLoading()
    hasDeposit.value = true
    showDepositModal.value = false
    uni.showToast({ title: '缴纳成功', icon: 'success' })
  }, 1500)
}

const showBidModal_ = () => {
  bidPrice.value = minBidPrice.value.toString()
  showBidModal.value = true
}

const selectBidPrice = (price) => {
  bidPrice.value = price.toString()
}

const submitBid = () => {
  if (!canBid.value) return
  
  uni.showLoading({ title: '提交中...' })
  setTimeout(() => {
    uni.hideLoading()
    showBidModal.value = false
    
    // 模拟出价结果
    const isWinning = Math.random() > 0.3
    if (isWinning) {
      resultType.value = 'success'
      resultTitle.value = '出价成功!'
      resultDesc.value = `您已成为当前最高出价者 ¥${bidPrice.value}`
      resultBtnText.value = '查看详情'
      myBidPrice.value = parseFloat(bidPrice.value)
      currentPrice.value = parseFloat(bidPrice.value)
      
      // 更新出价记录
      bidRecords.value.unshift({
        id: Date.now(),
        name: '我',
        avatar: 'https://picsum.photos/50/50?random=20',
        price: parseFloat(bidPrice.value),
        time: '刚刚',
        isMe: true
      })
      bidCount.value++
    } else {
      resultType.value = 'failed'
      resultTitle.value = '出价被超越'
      resultDesc.value = '有其他用户出价更高，请重新出价'
      resultBtnText.value = '继续出价'
    }
    
    showResultModal.value = true
  }, 1000)
}

const onResultConfirm = () => {
  showResultModal.value = false
  if (resultType.value === 'success') {
    // 跳转到支付页面
  }
}

// 模拟WebSocket更新
const connectWebSocket = () => {
  wsTimer = setInterval(() => {
    // 模拟其他用户出价
    if (Math.random() > 0.7) {
      const newPrice = currentPrice.value + increment.value
      currentPrice.value = newPrice
      priceChange.value = increment.value
      
      // 添加弹幕
      barrageList.value.unshift({
        avatar: `https://picsum.photos/30/30?random=${Date.now()}`,
        price: newPrice
      })
      if (barrageList.value.length > 5) {
        barrageList.value.pop()
      }
    }
  }, 5000)
}

// 更新倒计时
const updateCountdown = () => {
  if (countdown.value.total > 0) {
    countdown.value.total -= 1000
    const hours = Math.floor(countdown.value.total / 3600000)
    const minutes = Math.floor((countdown.value.total % 3600000) / 60000)
    const seconds = Math.floor((countdown.value.total % 60000) / 1000)
    countdown.value.hours = String(hours).padStart(2, '0')
    countdown.value.minutes = String(minutes).padStart(2, '0')
    countdown.value.seconds = String(seconds).padStart(2, '0')
  } else {
    auctionStatus.value = 'ended'
    clearInterval(countdownTimer)
  }
}

onMounted(() => {
  countdownTimer = setInterval(updateCountdown, 1000)
  connectWebSocket()
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
  if (wsTimer) clearInterval(wsTimer)
})

onLoad((options) => {
  itemId.value = options.itemId || ''
  sessionId.value = options.sessionId || ''
})
</script>

<style lang="scss" scoped>
.auction-detail-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 120rpx;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20rpx;
  background: rgba(0, 0, 0, 0.3);
  z-index: 100;

  .nav-back {
    width: 60rpx;
    height: 60rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .nav-title {
    font-size: 32rpx;
    color: #fff;
    font-weight: 500;
  }

  .nav-actions {
    display: flex;
    gap: 20rpx;

    .action-item {
      width: 60rpx;
      height: 60rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }
}

.image-swiper {
  width: 750rpx;
  height: 750rpx;

  .swiper-image {
    width: 100%;
    height: 100%;
  }
}

.artwork-info {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;

  .info-header {
    margin-bottom: 20rpx;

    .artwork-title {
      font-size: 36rpx;
      font-weight: 600;
      color: #333;
      margin-bottom: 16rpx;
    }

    .artwork-tags {
      display: flex;
      gap: 12rpx;

      .tag {
        padding: 4rpx 16rpx;
        border-radius: 8rpx;
        font-size: 22rpx;

        &:nth-child(1) {
          background: rgba(230, 126, 34, 0.1);
          color: #e67e22;
        }

        &:nth-child(2) {
          background: rgba(231, 76, 60, 0.1);
          color: #e74c3c;
        }

        &:nth-child(3) {
          background: rgba(46, 204, 113, 0.1);
          color: #2ecc71;
        }
      }
    }
  }

  .artwork-meta {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16rpx;
    padding: 20rpx 0;
    border-top: 1rpx solid #f0f0f0;
    border-bottom: 1rpx solid #f0f0f0;

    .meta-item {
      .meta-label {
        font-size: 24rpx;
        color: #999;
        margin-right: 8rpx;
      }

      .meta-value {
        font-size: 26rpx;
        color: #333;
      }
    }
  }

  .artwork-desc {
    margin-top: 20rpx;

    .desc-title {
      font-size: 28rpx;
      font-weight: 500;
      color: #333;
      margin-bottom: 12rpx;
    }

    .desc-content {
      font-size: 26rpx;
      color: #666;
      line-height: 1.6;
    }
  }
}

.auction-info-card {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;

  .auction-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;

    .auction-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
      flex: 1;
    }

    .auction-status {
      padding: 6rpx 16rpx;
      border-radius: 8rpx;
      font-size: 24rpx;

      &.bidding {
        background: rgba(230, 126, 34, 0.1);
        color: #e67e22;
      }

      &.ended {
        background: rgba(149, 165, 166, 0.1);
        color: #95a5a6;
      }

      &.settled {
        background: rgba(46, 204, 113, 0.1);
        color: #2ecc71;
      }
    }
  }

  .price-section {
    .price-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 16rpx;

      .price-item {
        flex: 1;

        .price-label {
          font-size: 24rpx;
          color: #999;
          display: block;
          margin-bottom: 8rpx;
        }

        .current-price {
          display: flex;
          align-items: center;

          .currency {
            font-size: 28rpx;
            color: #e74c3c;
          }

          .amount {
            font-size: 48rpx;
            font-weight: bold;
            color: #e74c3c;
          }

          .price-trend {
            display: flex;
            align-items: center;
            margin-left: 12rpx;
            font-size: 24rpx;

            &.up {
              color: #07c160;
            }

            &.down {
              color: #ff453a;
            }
          }
        }

        .start-price,
        .increment,
        .deposit {
          font-size: 28rpx;
          color: #333;
        }
      }
    }
  }

  .countdown-section {
    background: #f9f9f9;
    border-radius: 16rpx;
    padding: 24rpx;
    margin: 20rpx 0;
    text-align: center;

    .countdown-label {
      font-size: 24rpx;
      color: #999;
      margin-bottom: 12rpx;
    }

    .countdown-timer {
      display: flex;
      justify-content: center;
      align-items: center;

      .time-block {
        display: flex;
        flex-direction: column;
        align-items: center;

        .time-num {
          font-size: 48rpx;
          font-weight: bold;
          color: #333;
          background: #fff;
          padding: 8rpx 16rpx;
          border-radius: 8rpx;
        }

        .time-unit {
          font-size: 20rpx;
          color: #999;
          margin-top: 4rpx;
        }
      }

      .time-sep {
        font-size: 48rpx;
        font-weight: bold;
        color: #333;
        margin: 0 8rpx;
      }
    }

    .countdown-tip {
      display: flex;
      align-items: center;
      justify-content: center;
      margin-top: 12rpx;
      font-size: 24rpx;
      color: #ff7f50;
    }
  }

  .bid-records {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20rpx;
    background: #f9f9f9;
    border-radius: 12rpx;

    .records-left {
      display: flex;
      align-items: center;

      .avatar-group {
        display: flex;

        .avatar {
          width: 40rpx;
          height: 40rpx;
          border-radius: 50%;
          border: 2rpx solid #fff;
          margin-left: -12rpx;
          overflow: hidden;

          &:first-child {
            margin-left: 0;
          }

          image {
            width: 100%;
            height: 100%;
          }
        }
      }

      .records-count {
        font-size: 26rpx;
        color: #666;
        margin-left: 20rpx;
      }
    }

    .records-right {
      display: flex;
      align-items: center;

      .top-bidder {
        font-size: 24rpx;
        color: #999;
        margin-right: 8rpx;
      }
    }
  }
}

.bid-barrage {
  position: fixed;
  top: 200rpx;
  left: 0;
  right: 0;
  pointer-events: none;
  z-index: 50;

  .barrage-item {
    display: inline-flex;
    align-items: center;
    background: rgba(0, 0, 0, 0.6);
    border-radius: 20rpx;
    padding: 8rpx 16rpx;
    margin: 8rpx 20rpx;
    animation: slideIn 3s linear forwards;

    .barrage-avatar {
      width: 32rpx;
      height: 32rpx;
      border-radius: 50%;
      margin-right: 8rpx;
    }

    .barrage-text {
      font-size: 24rpx;
      color: #fff;
    }
  }
}

@keyframes slideIn {
  0% {
    transform: translateX(100%);
    opacity: 0;
  }
  10% {
    transform: translateX(0);
    opacity: 1;
  }
  90% {
    transform: translateX(0);
    opacity: 1;
  }
  100% {
    transform: translateX(-100%);
    opacity: 0;
  }
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 110rpx;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20rpx;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
  z-index: 100;

  .bar-left {
    display: flex;
    gap: 40rpx;

    .icon-btn {
      display: flex;
      flex-direction: column;
      align-items: center;

      .btn-text {
        font-size: 20rpx;
        color: #666;
        margin-top: 4rpx;
      }
    }
  }

  .bar-right {
    display: flex;
    align-items: center;
    gap: 20rpx;

    .price-display {
      text-align: right;

      .current {
        font-size: 32rpx;
        font-weight: bold;
        color: #e74c3c;
        display: block;
      }

      .label {
        font-size: 20rpx;
        color: #999;
      }
    }

    .btn {
      height: 72rpx;
      line-height: 72rpx;
      border-radius: 36rpx;
      font-size: 28rpx;
      padding: 0 40rpx;
      border: none;

      &.deposit-btn {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        color: #fff;
      }

      &.bid-btn {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
      }
    }
  }
}

// 出价记录弹窗
.records-modal {
  padding: 30rpx;
  max-height: 70vh;

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;

    .modal-title {
      font-size: 34rpx;
      font-weight: 600;
      color: #333;
    }

    .record-count {
      font-size: 26rpx;
      color: #999;
    }
  }

  .records-list {
    max-height: 60vh;

    .record-item {
      display: flex;
      align-items: center;
      padding: 20rpx 0;
      border-bottom: 1rpx solid #f0f0f0;

      .record-rank {
        width: 40rpx;
        height: 40rpx;
        border-radius: 50%;
        background: #ddd;
        color: #fff;
        font-size: 24rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 16rpx;

        &.top-1 {
          background: #f39c12;
        }
      }

      .record-avatar {
        width: 56rpx;
        height: 56rpx;
        border-radius: 50%;
        margin-right: 16rpx;
      }

      .record-info {
        flex: 1;

        .record-name {
          font-size: 28rpx;
          color: #333;
          display: block;
        }

        .record-time {
          font-size: 24rpx;
          color: #999;
        }
      }

      .record-price {
        text-align: right;

        .price {
          font-size: 30rpx;
          font-weight: bold;
          color: #e74c3c;
          display: block;
        }

        .me {
          font-size: 22rpx;
          color: #667eea;
        }
      }
    }
  }
}

// 出价弹窗
.bid-modal {
  padding: 40rpx 30rpx;

  .modal-title {
    font-size: 34rpx;
    font-weight: 600;
    text-align: center;
    margin-bottom: 30rpx;
  }

  .current-info {
    background: #f9f9f9;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 30rpx;

    .info-row {
      display: flex;
      justify-content: space-between;
      padding: 8rpx 0;

      .label {
        font-size: 26rpx;
        color: #666;
      }

      .value {
        font-size: 28rpx;
        color: #333;

        &.primary {
          color: #e74c3c;
          font-weight: bold;
        }
      }
    }
  }

  .bid-input-section {
    margin-bottom: 24rpx;

    .input-label {
      font-size: 28rpx;
      color: #333;
      display: block;
      margin-bottom: 16rpx;
    }

    .bid-input {
      display: flex;
      align-items: center;
      border-bottom: 2rpx solid #667eea;
      padding-bottom: 16rpx;
      margin-bottom: 20rpx;

      .currency {
        font-size: 48rpx;
        color: #333;
        font-weight: bold;
      }

      .input-field {
        flex: 1;
        font-size: 48rpx;
        color: #333;
        font-weight: bold;
      }
    }

    .quick-prices {
      display: flex;
      gap: 20rpx;

      .quick-item {
        flex: 1;
        height: 64rpx;
        background: #f5f6f8;
        border-radius: 32rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 26rpx;
        color: #666;

        &.active {
          background: rgba(102, 126, 234, 0.1);
          color: #667eea;
          border: 2rpx solid #667eea;
        }
      }
    }
  }

  .bid-tips {
    display: flex;
    align-items: center;
    margin-bottom: 30rpx;

    text {
      font-size: 24rpx;
      color: #999;
      margin-left: 8rpx;
    }
  }

  .confirm-bid-btn {
    width: 100%;
    height: 88rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    font-size: 32rpx;
    border-radius: 44rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;

    &[disabled] {
      background: #ccc;
    }
  }
}

// 保证金弹窗
.deposit-modal {
  padding: 40rpx 30rpx;

  .modal-title {
    font-size: 34rpx;
    font-weight: 600;
    text-align: center;
    margin-bottom: 30rpx;
  }

  .deposit-info {
    text-align: center;
    padding: 30rpx 0;
    border-bottom: 1rpx solid #f0f0f0;
    margin-bottom: 24rpx;

    .deposit-amount {
      display: flex;
      align-items: baseline;
      justify-content: center;
      margin-bottom: 12rpx;

      .currency {
        font-size: 32rpx;
        color: #e74c3c;
      }

      .amount {
        font-size: 64rpx;
        font-weight: bold;
        color: #e74c3c;
      }
    }

    .deposit-label {
      font-size: 26rpx;
      color: #999;
    }
  }

  .deposit-rules {
    margin-bottom: 24rpx;

    .rule-title {
      font-size: 28rpx;
      font-weight: 500;
      color: #333;
      margin-bottom: 16rpx;
    }

    .rule-item {
      font-size: 26rpx;
      color: #666;
      line-height: 1.8;
    }
  }

  .deposit-agreement {
    margin-bottom: 30rpx;

    .agreement-label {
      display: flex;
      align-items: center;

      .agreement-text {
        font-size: 26rpx;
        color: #666;
        margin-left: 8rpx;
      }
    }
  }

  .confirm-deposit-btn {
    width: 100%;
    height: 88rpx;
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    color: #fff;
    font-size: 32rpx;
    border-radius: 44rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;

    &[disabled] {
      background: #ccc;
    }
  }
}

// 结果弹窗
.result-modal {
  padding: 60rpx 40rpx;
  text-align: center;
  width: 600rpx;

  .result-icon {
    margin-bottom: 20rpx;
  }

  .result-title {
    font-size: 36rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 12rpx;
  }

  .result-desc {
    font-size: 28rpx;
    color: #666;
    margin-bottom: 40rpx;
  }

  .result-actions {
    display: flex;
    flex-direction: column;
    gap: 20rpx;

    .result-btn {
      height: 80rpx;
      border-radius: 40rpx;
      font-size: 28rpx;

      &.primary {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
      }

      &.secondary {
        background: #f5f6f8;
        color: #666;
      }
    }
  }
}
</style>
