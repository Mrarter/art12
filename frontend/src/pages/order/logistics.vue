<template>
  <view class="logistics-page">
    <!-- 物流头部 -->
    <view class="logistics-header">
      <view class="express-info">
        <text class="express-name">{{ logisticsInfo.expressName }}</text>
        <text class="express-no">{{ logisticsInfo.expressNo }}</text>
      </view>
      <view class="delivery-status" :class="statusClass">
        <text class="status-text">{{ logisticsInfo.statusText }}</text>
      </view>
    </view>

    <!-- 收货信息 -->
    <view class="receiver-info">
      <view class="receiver-avatar">
        
      </view>
      <view class="receiver-detail">
        <text class="receiver-name">{{ logisticsInfo.receiver }}</text>
        <text class="receiver-phone">{{ logisticsInfo.phone }}</text>
        <text class="receiver-address">{{ logisticsInfo.address }}</text>
      </view>
    </view>

    <!-- 物流时间线 -->
    <view class="timeline-section">
      <view class="timeline-header">
        <text class="header-title">物流动态</text>
      </view>
      
      <view class="timeline">
        <view 
          class="timeline-item" 
          v-for="(item, index) in logisticsInfo.traces" 
          :key="index"
          :class="{ current: index === 0 }"
        >
          <view class="timeline-dot">
            <view class="dot" v-if="index === 0"></view>
            <view class="line" v-else></view>
          </view>
          <view class="timeline-content">
            <text class="trace-desc">{{ item.desc }}</text>
            <text class="trace-time">{{ item.time }}</text>
            <text class="trace-location" v-if="item.location">{{ item.location }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 商品信息 -->
    <view class="goods-section">
      <view class="goods-header">
        <text class="header-title">商品信息</text>
      </view>
      <view class="goods-list">
        <view class="goods-item" v-for="(item, index) in orderInfo.goods" :key="index">
          <image :src="item.cover" mode="aspectFill" class="goods-cover"></image>
          <view class="goods-info">
            <text class="goods-title">{{ item.title }}</text>
            <text class="goods-spec">{{ item.spec }}</text>
            <view class="goods-price">
              <text class="price">¥{{ item.price }}</text>
              <text class="count">x{{ item.count }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部操作 -->
    <view class="logistics-footer">
      <button class="action-btn copy" @click="copyExpressNo">
        
        <text>复制单号</text>
      </button>
      <button class="action-btn contact" @click="contactExpress">
        
        <text>联系快递</text>
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getOrderDetail, getOrderLogistics } from '@/api/order'
import { getFullImageUrl } from '@/utils/image'

const logisticsInfo = ref({
  expressName: '--',
  expressNo: '--',
  statusText: '暂无物流',
  statusCode: 'pending',
  receiver: '--',
  phone: '--',
  address: '--',
  traces: []
})

const orderInfo = ref({
  orderNo: '',
  goods: []
})

const statusClass = computed(() => {
  const map = {
    pending: 'pending',
    delivering: 'delivering',
    completed: 'completed'
  }
  return map[logisticsInfo.value.statusCode] || ''
})

const statusCodeMap = {
  1: 'pending',
  2: 'delivering',
  3: 'delivering',
  4: 'completed'
}

const formatTime = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ')
}

const formatPrice = (value) => {
  const amount = Number(value || 0)
  return Number.isInteger(amount) ? String(amount) : amount.toFixed(2)
}

const normalizeGoods = (goods = []) => goods.map(item => ({
  cover: getFullImageUrl(item.goodsImage || item.coverImage || item.cover || ''),
  title: item.goodsName || item.title || item.itemTitle || '作品',
  spec: item.specName || item.spec || item.artType || '',
  price: formatPrice(item.price || item.unitPrice || 0),
  count: item.count || item.quantity || 1
}))

const normalizeTrace = (item) => ({
  desc: item.desc || item.description || '物流状态已更新',
  time: formatTime(item.time || item.trackTime || item.track_time),
  location: item.location || ''
})

const loadLogistics = async (orderId) => {
  try {
    const [logistics, detail] = await Promise.all([
      getOrderLogistics(orderId).catch(() => null),
      getOrderDetail(orderId).catch(() => null)
    ])

    if (detail) {
      orderInfo.value = {
        orderNo: detail.orderNo || detail.order_no || orderId,
        goods: normalizeGoods(detail.goodsList || detail.items || [])
      }
    }

    if (!logistics) {
      logisticsInfo.value.traces = [{ desc: '商家暂未发货', time: '', location: '' }]
      return
    }

    const traces = (logistics.tracks || logistics.traces || []).map(normalizeTrace)
    logisticsInfo.value = {
      expressName: logistics.companyName || logistics.expressName || '--',
      expressNo: logistics.trackingNo || logistics.expressNo || '--',
      statusText: logistics.statusText || '已发货',
      statusCode: statusCodeMap[Number(logistics.status)] || 'delivering',
      receiver: logistics.receiverName || logistics.receiver || '--',
      phone: logistics.receiverPhone || logistics.phone || '--',
      address: logistics.receiverAddress || logistics.address || '--',
      traces: traces.length ? traces : [{ desc: '商家已发货，等待物流揽收', time: formatTime(logistics.shipTime), location: '' }]
    }
  } catch (error) {
    uni.showToast({ title: error?.message || '物流加载失败', icon: 'none' })
  }
}

const copyExpressNo = () => {
  if (!logisticsInfo.value.expressNo || logisticsInfo.value.expressNo === '--') {
    uni.showToast({ title: '暂无快递单号', icon: 'none' })
    return
  }
  uni.setClipboardData({
    data: logisticsInfo.value.expressNo,
    success: () => {
      uni.showToast({ title: '单号已复制', icon: 'success' })
    }
  })
}

const contactExpress = () => {
  uni.makePhoneCall({
    phoneNumber: '95338', // 顺丰客服电话
    fail: () => {
      uni.showToast({ title: '拨打失败', icon: 'none' })
    }
  })
}

onLoad((options = {}) => {
  const orderId = options.id || options.orderId || ''
  if (orderId) {
    orderInfo.value.orderNo = orderId
    loadLogistics(orderId)
  } else {
    uni.showToast({ title: '缺少订单参数', icon: 'none' })
  }
})
</script>

<style lang="scss" scoped>
.logistics-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 160rpx;
}

.logistics-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 40rpx 30rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;

  .express-info {
    .express-name {
      font-size: 32rpx;
      font-weight: 600;
      display: block;
      margin-bottom: 12rpx;
    }

    .express-no {
      font-size: 26rpx;
      opacity: 0.9;
    }
  }

  .delivery-status {
    padding: 12rpx 24rpx;
    border-radius: 32rpx;
    background: rgba(255,255,255,0.2);

    &.pending {
      background: rgba(255,193,7,0.3);
    }

    &.delivering {
      background: rgba(76,175,80,0.3);
    }

    &.completed {
      background: rgba(33,150,243,0.3);
    }

    .status-text {
      font-size: 26rpx;
    }
  }
}

.receiver-info {
  display: flex;
  align-items: flex-start;
  padding: 30rpx;
  background: #fff;
  margin-bottom: 20rpx;

  .receiver-avatar {
    width: 80rpx;
    height: 80rpx;
    background: #f0f2f5;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 20rpx;
  }

  .receiver-detail {
    flex: 1;

    .receiver-name {
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
      margin-right: 16rpx;
    }

    .receiver-phone {
      font-size: 28rpx;
      color: #666;
    }

    .receiver-address {
      font-size: 26rpx;
      color: #999;
      margin-top: 12rpx;
      display: block;
    }
  }
}

.timeline-section {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;

  .timeline-header {
    margin-bottom: 30rpx;

    .header-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
    }
  }
}

.timeline {
  .timeline-item {
    display: flex;
    padding-bottom: 40rpx;

    &:last-child {
      padding-bottom: 0;
    }

    &.current {
      .timeline-dot {
        .dot {
          background: #667eea;
          box-shadow: 0 0 0 6rpx rgba(102,126,234,0.2);
        }
      }

      .timeline-content {
        .trace-desc {
          color: #333;
          font-weight: 500;
        }
      }
    }

    .timeline-dot {
      width: 40rpx;
      display: flex;
      flex-direction: column;
      align-items: center;

      .dot {
        width: 20rpx;
        height: 20rpx;
        background: #ddd;
        border-radius: 50%;
      }

      .line {
        width: 4rpx;
        flex: 1;
        background: #e0e0e0;
      }
    }

    .timeline-content {
      flex: 1;
      margin-left: 20rpx;
      padding-bottom: 10rpx;

      .trace-desc {
        font-size: 26rpx;
        color: #666;
        line-height: 1.5;
        display: block;
        margin-bottom: 12rpx;
      }

      .trace-time {
        font-size: 24rpx;
        color: #999;
        display: block;
      }

      .trace-location {
        font-size: 22rpx;
        color: #bbb;
        display: block;
        margin-top: 8rpx;
      }
    }
  }
}

.goods-section {
  background: #fff;
  padding: 30rpx;

  .goods-header {
    margin-bottom: 30rpx;

    .header-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
    }
  }

  .goods-item {
    display: flex;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .goods-cover {
      width: 160rpx;
      height: 160rpx;
      border-radius: 12rpx;
      margin-right: 20rpx;
    }

    .goods-info {
      flex: 1;

      .goods-title {
        font-size: 28rpx;
        color: #333;
        font-weight: 500;
        display: block;
        margin-bottom: 12rpx;
      }

      .goods-spec {
        font-size: 24rpx;
        color: #999;
        display: block;
        margin-bottom: 16rpx;
      }

      .goods-price {
        display: flex;
        justify-content: space-between;

        .price {
          font-size: 28rpx;
          color: #ff4d4f;
          font-weight: 600;
        }

        .count {
          font-size: 26rpx;
          color: #999;
        }
      }
    }
  }
}

.logistics-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 20rpx;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2rpx 20rpx rgba(0,0,0,0.05);

  .action-btn {
    flex: 1;
    height: 88rpx;
    border-radius: 44rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12rpx;
    font-size: 28rpx;
    border: none;

    &::after {
      border: none;
    }

    &.copy {
      background: #f0f2f5;
      color: #667eea;
    }

    &.contact {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
    }
  }
}
</style>
