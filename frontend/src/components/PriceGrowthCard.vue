<template>
  <view class="price-card">
    <view class="card-header">
      <text class="price-label">当前流通价</text>
    </view>
    <view class="price-value">¥{{ formatPrice(price) }}</view>

    <view class="growth-row">
      <text class="row-label">今日涨幅</text>
      <text class="row-value growth">{{ growthRate }}</text>
    </view>

    <view class="growth-row">
      <text class="row-label">收藏热度</text>
      <text class="row-value">{{ collectCount || 0 }} 人收藏</text>
    </view>

    <view class="next-condition">
      下一次涨价条件：{{ nextCondition }}
    </view>
  </view>
</template>

<script>
export default {
  name: 'PriceGrowthCard',
  props: {
    price: { type: Number, default: 0 },
    growthRate: { type: String, default: '+0%' },
    collectCount: { type: Number, default: 0 },
    nextCondition: { type: String, default: '收藏人数增加后可能涨价' }
  },
  methods: {
    formatPrice(price) {
      if (!price) return '0'
      const yuan = Math.round(price / 100)
      return yuan.toLocaleString()
    }
  }
}
</script>

<style lang="scss" scoped>
.price-card {
  padding: 28rpx;
  border-radius: 24rpx;
  background: #1a1a1a;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
}

.card-header {
  margin-bottom: 8rpx;
}

.price-label {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.5);
}

.price-value {
  font-size: 48rpx;
  font-weight: 700;
  color: #d4af37;
  margin-bottom: 24rpx;
}

.growth-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  font-size: 26rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.08);

  .row-label {
    color: rgba(255, 255, 255, 0.6);
  }

  .row-value {
    color: rgba(255, 255, 255, 0.85);
  }

  .growth {
    color: #e74c3c;
    font-weight: 600;
  }
}

.next-condition {
  margin-top: 20rpx;
  padding: 18rpx;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.5);
  line-height: 1.5;
}
</style>
