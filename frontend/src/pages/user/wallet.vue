<template>
  <view class="wallet-page">
    <view class="balance-card">
      <view class="balance-head">
        <text class="balance-label">可用余额</text>
        <text class="balance-value">¥{{ formatAmount(wallet.balance) }}</text>
      </view>

      <view class="balance-metrics">
        <view class="metric">
          <text class="metric-value">¥{{ formatAmount(wallet.freezeAmount) }}</text>
          <text class="metric-label">冻结中</text>
        </view>
        <view class="metric">
          <text class="metric-value">¥{{ formatAmount(wallet.pendingAmount) }}</text>
          <text class="metric-label">待结算</text>
        </view>
        <view class="metric">
          <text class="metric-value">¥{{ formatAmount(wallet.totalIncome) }}</text>
          <text class="metric-label">累计收入</text>
        </view>
      </view>

      <view class="balance-actions">
        <view class="primary-btn" @click="goWithdraw">提现</view>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">交易记录</text>
      </view>

      <view v-if="loading" class="loading-state">加载中...</view>
      <view v-else-if="bills.length === 0" class="empty-state">暂无交易记录</view>
      <view v-else class="bill-list">
        <view class="bill-row" v-for="item in bills" :key="item.id">
          <view class="bill-icon" :class="billTypeClass(item.billType)">
            {{ billTypeIcon(item.billType) }}
          </view>
          <view class="bill-main">
            <text class="bill-title">{{ billTypeText(item.billType) }}</text>
            <text class="bill-time">{{ item.createdTime }}</text>
          </view>
          <text class="bill-amount" :class="amountClass(item.amount)">
            {{ item.amount > 0 ? '+' : '' }}{{ formatAmount(item.amount) }}
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getWalletInfo, getWalletBills } from '@/api/wallet'
import { getPayAccountList } from '@/api/pay'

export default {
  data() {
    return {
      wallet: { balance: 0, freezeAmount: 0, pendingAmount: 0, totalIncome: 0 },
      bills: [],
      loading: false,
      showWithdraw: false,
      withdrawAmount: ''
    }
  },

  onShow() {
    this.loadData()
  },

  methods: {
    async loadData() {
      try {
        const [walletData, billsData] = await Promise.all([
          getWalletInfo(),
          getWalletBills(1, 50)
        ])
        this.wallet = walletData || this.wallet
        this.bills = billsData || []
      } catch (e) {
        console.error('加载钱包数据失败:', e)
      }
    },

    formatAmount(val) {
      if (!val && val !== 0) return '0.00'
      return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },

    billTypeText(type) {
      const map = {
        income: '收入', withdraw: '提现', freeze: '冻结', unfreeze: '解冻',
        commission: '佣金', resale: '转售', refund: '退款', deposit: '保证金',
        transfer: '转账'
      }
      return map[type] || type
    },

    billTypeIcon(type) {
      const map = { income: '入', commission: '佣', refund: '退', withdraw: '出', freeze: '冻' }
      return map[type] || '·'
    },

    billTypeClass(type) {
      const map = { income: 'green', commission: 'green', refund: 'orange', withdraw: 'red', freeze: 'gray' }
      return map[type] || ''
    },

    amountClass(amount) {
      return amount > 0 ? 'income' : 'expense'
    },

    goWithdraw() {
      // 检查是否有收款账户
      getPayAccountList().then(list => {
        if (!list || list.length === 0) {
          uni.showModal({
            title: '提示',
            content: '请先添加收款账户再提现',
            confirmText: '去添加',
            success: (r) => {
              if (r.confirm) uni.navigateTo({ url: '/pages/user/pay-account/list' })
            }
          })
        } else {
          uni.navigateTo({ url: '/pages/promoter/withdraw' })
        }
      }).catch(() => {
        uni.navigateTo({ url: '/pages/promoter/withdraw' })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.wallet-page {
  min-height: 100vh;
  background: #0d0d0d;
  padding-bottom: 40rpx;
}

.balance-card {
  background: linear-gradient(135deg, #1a1a1a, #2a2a2a);
  margin: 24rpx;
  border-radius: 24rpx;
  padding: 40rpx 32rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
}

.balance-head { text-align: center; }
.balance-label { color: #b3b3b3; font-size: 26rpx; }
.balance-value { color: #f5f5f5; font-size: 64rpx; font-weight: 800; display: block; margin: 12rpx 0; }

.balance-metrics {
  display: flex; justify-content: space-around; margin-top: 32rpx;
  padding: 20rpx 0; border-top: 1rpx solid rgba(255,255,255,0.06);
}
.metric { text-align: center; }
.metric-value { color: #d4af37; font-size: 32rpx; font-weight: 700; display: block; }
.metric-label { color: #888; font-size: 22rpx; margin-top: 6rpx; }

.balance-actions { display: flex; gap: 20rpx; margin-top: 32rpx; }
.primary-btn {
  flex: 1; height: 88rpx; border-radius: 999rpx; background: #d4af37; color: #16130b;
  font-size: 30rpx; font-weight: 800; display: flex; align-items: center; justify-content: center;
}

.section { margin: 24rpx; }
.section-head { margin-bottom: 20rpx; }
.section-title { color: #f5f5f5; font-size: 30rpx; font-weight: 700; }

.bill-list { display: flex; flex-direction: column; gap: 2rpx; }
.bill-row {
  display: flex; align-items: center; gap: 20rpx; padding: 24rpx;
  background: #1a1a1a; border-radius: 16rpx; margin-bottom: 12rpx;
}
.bill-icon {
  width: 56rpx; height: 56rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 24rpx; font-weight: 800; flex-shrink: 0;
}
.bill-icon.green { background: rgba(82,196,26,0.12); color: #52c41a; }
.bill-icon.red { background: rgba(255,77,79,0.12); color: #ff4d4f; }
.bill-icon.orange { background: rgba(250,173,20,0.12); color: #faad14; }
.bill-icon.gray { background: rgba(140,140,140,0.12); color: #8c8c8c; }

.bill-main { flex: 1; }
.bill-title { color: #f5f5f5; font-size: 26rpx; font-weight: 600; display: block; }
.bill-time { color: #888; font-size: 22rpx; margin-top: 4rpx; }

.bill-amount { font-size: 28rpx; font-weight: 700; flex-shrink: 0; }
.bill-amount.income { color: #52c41a; }
.bill-amount.expense { color: #ff4d4f; }

.loading-state, .empty-state { text-align: center; color: #888; font-size: 26rpx; padding: 60rpx 0; }
</style>
