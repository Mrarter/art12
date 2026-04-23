<template>
  <view class="wallet-page">
    <!-- 头部余额卡片 -->
    <view class="balance-card">
      <view class="balance-bg"></view>
      <view class="balance-content">
        <view class="balance-header">
          <text class="balance-label">我的余额</text>
          <text class="balance-value">¥{{ formatAmount(balance) }}</text>
        </view>
        <view class="balance-hint">可提现余额</view>
        
        <!-- 账户概览 -->
        <view class="account-overview">
          <view class="overview-item">
            <text class="overview-value">¥{{ formatAmount(frozen) }}</text>
            <text class="overview-label">冻结中</text>
          </view>
          <view class="overview-divider"></view>
          <view class="overview-item">
            <text class="overview-value">¥{{ formatAmount(totalIncome) }}</text>
            <text class="overview-label">累计收益</text>
          </view>
        </view>
        
        <!-- 操作按钮 -->
        <view class="action-buttons">
          <view class="action-btn recharge" @click="goRecharge">
            
            <text>充值</text>
          </view>
          <view class="action-btn withdraw" @click="goWithdraw">
            
            <text>提现</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 功能列表 -->
    <view class="menu-section">
      <view class="menu-item" @click="goCoupon">
        <view class="menu-left">
          <view class="mencoupon-icon">
            <text>🎫</text>
          </view>
          <view class="menu-info">
            <text class="menu-title">我的优惠券</text>
            <text class="menu-count">{{ couponCount }}张可用</text>
          </view>
        </view>
        
      </view>
      
      <view class="menu-item" @click="goPoints">
        <view class="menu-left">
          <view class="menpoints-icon">
            <text>⭐</text>
          </view>
          <view class="menu-info">
            <text class="menu-title">我的积分</text>
            <text class="menu-count">{{ points }}积分</text>
          </view>
        </view>
        
      </view>
      
      <view class="menu-item" @click="goCards">
        <view class="menu-left">
          <view class="mencard-icon">
            <text>💳</text>
          </view>
          <view class="menu-info">
            <text class="menu-title">银行卡</text>
            <text class="menu-count">已绑定 {{ cardCount }}张</text>
          </view>
        </view>
        
      </view>
      
      <view class="menu-item" @click="goInvoice">
        <view class="menu-left">
          <view class="meninvoice-icon">
            <text>📄</text>
          </view>
          <view class="menu-info">
            <text class="menu-title">发票管理</text>
            <text class="menu-count">申请发票</text>
          </view>
        </view>
        
      </view>
    </view>
    
    <!-- 交易记录 -->
    <view class="transaction-section">
      <view class="section-header">
        <text class="section-title">交易记录</text>
        <text class="more-btn" @click="goTransactionList">查看全部 ></text>
      </view>
      
      <view class="transaction-tabs">
        <view 
          class="tab-item" 
          :class="{ active: transactionTab === 'all' }"
          @click="switchTransactionTab('all')"
        >
          全部
        </view>
        <view 
          class="tab-item" 
          :class="{ active: transactionTab === 'income' }"
          @click="switchTransactionTab('income')"
        >
          收入
        </view>
        <view 
          class="tab-item" 
          :class="{ active: transactionTab === 'expense' }"
          @click="switchTransactionTab('expense')"
        >
          支出
        </view>
      </view>
      
      <view class="transaction-list">
        <view 
          class="transaction-item"
          v-for="item in filteredTransactions"
          :key="item.id"
        >
          <view class="transaction-icon" :class="item.type">
            <text>{{ item.type === 'income' ? '+' : '-' }}</text>
          </view>
          <view class="transaction-info">
            <text class="transaction-title">{{ item.title }}</text>
            <text class="transaction-time">{{ item.time }}</text>
          </view>
          <text class="transaction-amount" :class="item.type">
            {{ item.type === 'income' ? '+' : '-' }}¥{{ formatAmount(item.amount) }}
          </text>
        </view>
        
        <view class="empty-transaction" v-if="filteredTransactions.length === 0">
          <text>暂无交易记录</text>
        </view>
      </view>
    </view>
    
    <!-- 充值/提现弹窗 -->
    <!-- 弹窗开始 -->
      <view class="recharge-popup">
        <view class="popup-title">充值金额</view>
        <view class="amount-input">
          <text class="currency">¥</text>
          <input type="digit" v-model="rechargeAmount" placeholder="请输入金额" />
        </view>
        <view class="quick-amounts">
          <view 
            class="quick-item" 
            :class="{ active: rechargeAmount === '100' }"
            @click="rechargeAmount = '100'"
          >100</view>
          <view 
            class="quick-item" 
            :class="{ active: rechargeAmount === '500' }"
            @click="rechargeAmount = '500'"
          >500</view>
          <view 
            class="quick-item" 
            :class="{ active: rechargeAmount === '1000' }"
            @click="rechargeAmount = '1000'"
          >1000</view>
          <view 
            class="quick-item" 
            :class="{ active: rechargeAmount === '5000' }"
            @click="rechargeAmount = '5000'"
          >5000</view>
        </view>
        <button class="confirm-btn" @click="confirmRecharge">确认充值</button>
      </view>
<!-- 弹窗结束 -->
    
    <!-- 弹窗开始 -->
      <view class="withdraw-popup">
        <view class="popup-title">提现金额</view>
        <view class="withdraw-tip">
          <text>可提现余额：¥{{ formatAmount(balance) }}</text>
        </view>
        <view class="amount-input">
          <text class="currency">¥</text>
          <input type="digit" v-model="withdrawAmount" placeholder="请输入金额" />
        </view>
        <view class="withdraw-fee">
          <text>手续费：¥0.00</text>
          <text>实际到账：¥{{ formatAmount(withdrawAmount || 0) }}</text>
        </view>
        <button class="confirm-btn" @click="confirmWithdraw">确认提现</button>
        <view class="withdraw-rules">
          <text class="rules-title">提现规则：</text>
          <text>1. 提现需实名认证</text>
          <text>2. 提现到账时间1-3个工作日</text>
          <text>3. 每日限提现3次</text>
        </view>
      </view>
<!-- 弹窗结束 -->
  </view>
</template>

<script>
export default {
  data() {
    return {
      balance: 25868.50,
      frozen: 3200.00,
      totalIncome: 56800.00,
      couponCount: 5,
      points: 12680,
      cardCount: 2,
      transactionTab: 'all',
      transactions: [
        { id: 1, type: 'income', title: '作品销售分成', amount: 1280.00, time: '2026-04-21 14:30' },
        { id: 2, type: 'expense', title: '购买《山水长卷》', amount: 12800.00, time: '2026-04-20 10:15' },
        { id: 3, type: 'income', title: '艺荐官佣金', amount: 680.00, time: '2026-04-19 16:45' },
        { id: 4, type: 'income', title: '拍卖成交', amount: 5800.00, time: '2026-04-18 20:00' },
        { id: 5, type: 'expense', title: '充值', amount: 10000.00, time: '2026-04-15 09:30' },
        { id: 6, type: 'expense', title: '提现到账', amount: 5000.00, time: '2026-04-14 15:20' }
      ],
      showRecharge: false,
      showWithdraw: false,
      rechargeAmount: '',
      withdrawAmount: ''
    }
  },

  computed: {
    filteredTransactions() {
      if (this.transactionTab === 'all') return this.transactions
      return this.transactions.filter(t => t.type === this.transactionTab)
    }
  },

  methods: {
    formatAmount(amount) {
      return Number(amount || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
    },

    switchTransactionTab(tab) {
      this.transactionTab = tab
    },

    goRecharge() {
      this.rechargeAmount = ''
      this.showRecharge = true
    },

    goWithdraw() {
      this.withdrawAmount = ''
      this.showWithdraw = true
    },

    confirmRecharge() {
      if (!this.rechargeAmount) {
        uni.showToast({ title: '请输入金额', icon: 'none' })
        return
      }
      uni.showToast({ title: '充值功能开发中', icon: 'none' })
      this.showRecharge = false
    },

    confirmWithdraw() {
      if (!this.withdrawAmount) {
        uni.showToast({ title: '请输入金额', icon: 'none' })
        return
      }
      if (Number(this.withdrawAmount) > this.balance) {
        uni.showToast({ title: '超过可提现余额', icon: 'none' })
        return
      }
      uni.showToast({ title: '提现功能开发中', icon: 'none' })
      this.showWithdraw = false
    },

    goCoupon() {
      uni.navigateTo({ url: '/pages/user/coupon' })
    },

    goPoints() {
      uni.navigateTo({ url: '/pages/user/points' })
    },

    goCards() {
      uni.navigateTo({ url: '/pages/user/cards' })
    },

    goInvoice() {
      uni.navigateTo({ url: '/pages/user/invoice' })
    },

    goTransactionList() {
      uni.navigateTo({ url: '/pages/user/transaction' })
    }
  }
}
</script>

<style lang="scss" scoped>
.wallet-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40rpx;
}

.balance-card {
  position: relative;
  margin: 20rpx;
  border-radius: 24rpx;
  overflow: hidden;

  .balance-bg {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 300rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }

  .balance-content {
    position: relative;
    padding: 40rpx 30rpx;

    .balance-header {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 8rpx;

      .balance-label {
        font-size: 28rpx;
        color: rgba(255, 255, 255, 0.8);
        margin-bottom: 16rpx;
      }

      .balance-value {
        font-size: 72rpx;
        font-weight: 700;
        color: #fff;
      }
    }

    .balance-hint {
      text-align: center;
      font-size: 24rpx;
      color: rgba(255, 255, 255, 0.7);
      margin-bottom: 40rpx;
    }

    .account-overview {
      display: flex;
      justify-content: center;
      align-items: center;
      background: rgba(255, 255, 255, 0.15);
      border-radius: 16rpx;
      padding: 24rpx 40rpx;
      margin-bottom: 30rpx;

      .overview-item {
        flex: 1;
        text-align: center;

        .overview-value {
          font-size: 32rpx;
          font-weight: 600;
          color: #fff;
          display: block;
          margin-bottom: 8rpx;
        }

        .overview-label {
          font-size: 24rpx;
          color: rgba(255, 255, 255, 0.7);
        }
      }

      .overview-divider {
        width: 1rpx;
        height: 60rpx;
        background: rgba(255, 255, 255, 0.3);
        margin: 0 40rpx;
      }
    }

    .action-buttons {
      display: flex;
      gap: 24rpx;

      .action-btn {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8rpx;
        height: 80rpx;
        border-radius: 40rpx;
        font-size: 30rpx;
        color: #fff;

        &.recharge {
          background: #fff;
          color: #667eea;
        }

        &.withdraw {
          background: rgba(255, 255, 255, 0.2);
        }
      }
    }
  }
}

.menu-section {
  background: #fff;
  margin: 0 20rpx 20rpx;
  border-radius: 16rpx;
  overflow: hidden;

  .menu-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 28rpx 24rpx;
    border-bottom: 1rpx solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    .menu-left {
      display: flex;
      align-items: center;

      .men{
        width: 72rpx;
        height: 72rpx;
        border-radius: 16rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 20rpx;
        font-size: 32rpx;

        &.coupon-icon {
          background: rgba(230, 126, 34, 0.1);
        }

        &.points-icon {
          background: rgba(241, 196, 15, 0.1);
        }

        &.card-icon {
          background: rgba(52, 152, 219, 0.1);
        }

        &.invoice-icon {
          background: rgba(155, 89, 182, 0.1);
        }
      }

      .menu-info {
        .menu-title {
          font-size: 30rpx;
          color: #333;
          display: block;
          margin-bottom: 4rpx;
        }

        .menu-count {
          font-size: 24rpx;
          color: #999;
        }
      }
    }
  }
}

.transaction-section {
  background: #fff;
  margin: 0 20rpx;
  border-radius: 16rpx;
  overflow: hidden;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx;
    border-bottom: 1rpx solid #f5f5f5;

    .section-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
    }

    .more-btn {
      font-size: 24rpx;
      color: #999;
    }
  }

  .transaction-tabs {
    display: flex;
    padding: 0 24rpx;
    border-bottom: 1rpx solid #f5f5f5;

    .tab-item {
      flex: 1;
      text-align: center;
      padding: 20rpx 0;
      font-size: 28rpx;
      color: #666;
      position: relative;

      &.active {
        color: #667eea;
        font-weight: 500;

        &::after {
          content: '';
          position: absolute;
          bottom: 0;
          left: 50%;
          transform: translateX(-50%);
          width: 48rpx;
          height: 4rpx;
          background: #667eea;
          border-radius: 2rpx;
        }
      }
    }
  }

  .transaction-list {
    .transaction-item {
      display: flex;
      align-items: center;
      padding: 24rpx;

      .transaction-icon {
        width: 64rpx;
        height: 64rpx;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 16rpx;
        font-size: 28rpx;
        font-weight: 600;

        &.income {
          background: rgba(7, 193, 96, 0.1);
          color: #07c160;
        }

        &.expense {
          background: rgba(231, 76, 60, 0.1);
          color: #e74c3c;
        }
      }

      .transaction-info {
        flex: 1;

        .transaction-title {
          font-size: 28rpx;
          color: #333;
          display: block;
          margin-bottom: 6rpx;
        }

        .transaction-time {
          font-size: 24rpx;
          color: #999;
        }
      }

      .transaction-amount {
        font-size: 30rpx;
        font-weight: 600;

        &.income {
          color: #07c160;
        }

        &.expense {
          color: #333;
        }
      }
    }

    .empty-transaction {
      text-align: center;
      padding: 60rpx 0;
      font-size: 26rpx;
      color: #999;
    }
  }
}

.recharge-popup,
.withdraw-popup {
  padding: 30rpx;

  .popup-title {
    font-size: 34rpx;
    font-weight: 600;
    text-align: center;
    margin-bottom: 30rpx;
  }

  .withdraw-tip {
    text-align: center;
    font-size: 26rpx;
    color: #999;
    margin-bottom: 20rpx;
  }

  .amount-input {
    display: flex;
    align-items: center;
    border-bottom: 2rpx solid #eee;
    padding-bottom: 20rpx;
    margin-bottom: 30rpx;

    .currency {
      font-size: 48rpx;
      font-weight: 600;
      color: #333;
      margin-right: 8rpx;
    }

    input {
      flex: 1;
      font-size: 48rpx;
      font-weight: 600;
    }
  }

  .quick-amounts {
    display: flex;
    gap: 16rpx;
    margin-bottom: 40rpx;

    .quick-item {
      flex: 1;
      height: 72rpx;
      background: #f5f5f5;
      border-radius: 12rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28rpx;
      color: #666;

      &.active {
        background: rgba(102, 126, 234, 0.1);
        color: #667eea;
        border: 1rpx solid #667eea;
      }
    }
  }

  .withdraw-fee {
    display: flex;
    justify-content: space-between;
    font-size: 24rpx;
    color: #999;
    margin-bottom: 30rpx;
  }

  .confirm-btn {
    width: 100%;
    height: 88rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    font-size: 32rpx;
    border-radius: 44rpx;
    border: none;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 20rpx;
  }

  .withdraw-rules {
    font-size: 24rpx;
    color: #999;
    line-height: 1.8;

    .rules-title {
      color: #333;
      display: block;
      margin-bottom: 8rpx;
    }
  }
}
</style>
