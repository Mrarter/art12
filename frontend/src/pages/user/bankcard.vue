<template>
  <view class="bankcard-page">
    <!-- 银行卡列表 -->
    <view class="card-list">
      <view class="card-item" v-for="(card, index) in cards" :key="index">
        <view class="card-header">
          <image class="bank-logo" :src="card.logo" mode="aspectFit" />
          <view class="card-info">
            <text class="bank-name">{{ card.bankName }}</text>
            <text class="card-type">{{ card.type === 'debit' ? '储蓄卡' : '信用卡' }}</text>
          </view>
          <text class="default-tag" v-if="card.isDefault">默认</text>
        </view>
        <text class="card-number">{{ card.cardNumber }}</text>
        <view class="card-actions">
          <text class="action-text" @click="setDefault(card)" v-if="!card.isDefault">设为默认</text>
          <text class="action-text delete" @click="deleteCard(card)">删除</text>
        </view>
      </view>
    </view>

    <!-- 添加按钮 -->
    <view class="add-card-btn" @click="addCard">
      <text class="add-icon">+</text>
      <text class="add-text">添加银行卡</text>
    </view>

    <!-- 温馨提示 -->
    <view class="tips">
      <text class="tips-title">温馨提示</text>
      <text class="tips-item">1. 请绑定持卡人本人的银行卡</text>
      <text class="tips-item">2. 绑定银行卡可用于提现和支付</text>
      <text class="tips-item">3. 为保障资金安全，仅支持借记卡提现</text>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      cards: [
        {
          id: 1,
          bankName: '中国工商银行',
          type: 'debit',
          cardNumber: '6222 **** **** 1234',
          logo: '/static/bank-icbc.png',
          isDefault: true
        },
        {
          id: 2,
          bankName: '中国建设银行',
          type: 'debit',
          cardNumber: '6217 **** **** 5678',
          logo: '/static/bank-ccb.png',
          isDefault: false
        }
      ]
    }
  },
  methods: {
    addCard() {
      uni.navigateTo({ url: '/pages/common/coming-soon?title=添加银行卡&desc=新增银行卡页正在开发中，后续会补充绑卡流程。' })
    },
    setDefault(card) {
      this.cards.forEach(c => c.isDefault = false)
      card.isDefault = true
      uni.showToast({ title: '已设为默认', icon: 'success' })
    },
    deleteCard(card) {
      uni.showModal({
        title: '删除银行卡',
        content: '确定删除该银行卡？',
        success: (res) => {
          if (res.confirm) {
            const index = this.cards.findIndex(c => c.id === card.id)
            if (index > -1) {
              this.cards.splice(index, 1)
            }
            uni.showToast({ title: '已删除', icon: 'success' })
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.bankcard-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 24rpx;
}

.card-list {
  margin-bottom: 30rpx;
}

.card-item {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  color: #fff;
}

.card-header {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.bank-logo {
  width: 60rpx;
  height: 60rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12rpx;
  margin-right: 16rpx;
}

.card-info {
  flex: 1;
}

.bank-name {
  font-size: 30rpx;
  font-weight: 600;
  display: block;
}

.card-type {
  font-size: 22rpx;
  opacity: 0.8;
}

.default-tag {
  font-size: 22rpx;
  background: rgba(255, 255, 255, 0.2);
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
}

.card-number {
  font-size: 36rpx;
  letter-spacing: 4rpx;
  display: block;
  margin-bottom: 20rpx;
}

.card-actions {
  display: flex;
  gap: 30rpx;
}

.action-text {
  font-size: 26rpx;
  opacity: 0.9;
}

.action-text.delete {
  opacity: 0.7;
}

.add-card-btn {
  background: #fff;
  border-radius: 16rpx;
  padding: 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  border: 2rpx dashed #ddd;
}

.add-icon {
  font-size: 60rpx;
  color: #667eea;
  font-weight: 300;
}

.add-text {
  font-size: 28rpx;
  color: #667eea;
}

.tips {
  margin-top: 40rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: 16rpx;
}

.tips-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 16rpx;
}

.tips-item {
  font-size: 24rpx;
  color: #666;
  display: block;
  margin-bottom: 10rpx;
  line-height: 1.5;
}
</style>
