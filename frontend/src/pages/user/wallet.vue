<template>
  <view class="wallet-page">
    <view class="balance-card">
      <view class="balance-head">
        <text class="balance-label">可用余额</text>
        <text class="balance-value">¥{{ formatAmount(balance) }}</text>
        <text class="balance-desc">余额可用于收藏作品、保证金和服务支付</text>
      </view>

      <view class="balance-metrics">
        <view class="metric">
          <text class="metric-value">¥{{ formatAmount(frozen) }}</text>
          <text class="metric-label">冻结中</text>
        </view>
        <view class="metric">
          <text class="metric-value">¥{{ formatAmount(totalIncome) }}</text>
          <text class="metric-label">累计收益</text>
        </view>
      </view>

      <view class="balance-actions">
        <view class="primary-btn" @click="goRecharge">充值</view>
        <view class="ghost-btn" @click="goWithdraw">提现</view>
      </view>
    </view>

    <view class="section">
      <view class="section-title">账户工具</view>
      <view class="tool-grid">
        <view class="tool-card" v-for="item in walletTools" :key="item.label" @click="goPage(item.path)">
          <view class="tool-icon" :class="item.tone">{{ item.icon }}</view>
          <text class="tool-title">{{ item.label }}</text>
          <text class="tool-desc">{{ item.desc }}</text>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">交易记录</text>
        <text class="section-link" @click="goTransactionList">查看全部</text>
      </view>

      <view class="segmented">
        <view
          class="segment"
          v-for="item in transactionTabs"
          :key="item.value"
          :class="{ active: transactionTab === item.value }"
          @click="switchTransactionTab(item.value)"
        >{{ item.label }}</view>
      </view>

      <view class="transaction-list">
        <view class="transaction-row" v-for="item in filteredTransactions" :key="item.id">
          <view class="transaction-icon" :class="item.type">{{ item.type === 'income' ? '入' : '出' }}</view>
          <view class="transaction-main">
            <text class="transaction-title">{{ item.title }}</text>
            <text class="transaction-time">{{ item.time }}</text>
          </view>
          <text class="transaction-amount" :class="item.type">
            {{ item.type === 'income' ? '+' : '-' }}¥{{ formatAmount(item.amount) }}
          </text>
        </view>

        <view class="empty-state" v-if="filteredTransactions.length === 0">暂无交易记录</view>
      </view>
    </view>

    <view class="sheet-mask" v-if="showRecharge" @click="showRecharge = false">
      <view class="amount-sheet" @click.stop>
        <text class="sheet-title">充值金额</text>
        <view class="amount-input">
          <text>¥</text>
          <input type="digit" v-model="rechargeAmount" placeholder="请输入金额" />
        </view>
        <view class="quick-amounts">
          <view
            v-for="amount in quickAmounts"
            :key="amount"
            class="quick-item"
            :class="{ active: rechargeAmount === amount }"
            @click="rechargeAmount = amount"
          >{{ amount }}</view>
        </view>
        <view class="sheet-confirm" @click="confirmRecharge">确认充值</view>
      </view>
    </view>

    <view class="sheet-mask" v-if="showWithdraw" @click="showWithdraw = false">
      <view class="amount-sheet" @click.stop>
        <text class="sheet-title">提现金额</text>
        <text class="sheet-desc">可提现余额：¥{{ formatAmount(balance) }}</text>
        <view class="amount-input">
          <text>¥</text>
          <input type="digit" v-model="withdrawAmount" placeholder="请输入金额" />
        </view>
        <view class="withdraw-info">
          <text>手续费：¥0.00</text>
          <text>实际到账：¥{{ formatAmount(withdrawAmount || 0) }}</text>
        </view>
        <view class="sheet-confirm" @click="confirmWithdraw">确认提现</view>
      </view>
    </view>
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
      quickAmounts: ['100', '500', '1000', '5000'],
      transactionTabs: [
        { label: '全部', value: 'all' },
        { label: '收入', value: 'income' },
        { label: '支出', value: 'expense' }
      ],
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
    walletTools() {
      return [
        { label: '优惠券', desc: `${this.couponCount} 张可用`, icon: '券', tone: 'gold', path: '/pages/user/coupon' },
        { label: '积分', desc: `${this.points} 积分`, icon: '积', tone: 'green', path: '/pages/user/points' },
        { label: '银行卡', desc: `已绑定 ${this.cardCount} 张`, icon: '卡', tone: 'blue', path: '/pages/user/bankcard' },
        { label: '发票管理', desc: '申请发票', icon: '票', tone: 'purple', path: '/pages/user/invoice' }
      ]
    },
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
    goPage(url) {
      uni.navigateTo({ url })
    },
    goTransactionList() {
      uni.navigateTo({ url: '/pages/common/coming-soon?title=交易明细&desc=钱包流水页正在开发中，后续会补充充值、提现与消费记录。' })
    }
  }
}
</script>

<style lang="scss" scoped>
$bg: #0b0b0c;
$panel: #171719;
$panel2: #202024;
$line: rgba(255, 255, 255, 0.08);
$text: #f6f2e8;
$muted: #9b958a;
$dim: #68645c;
$gold: #c9a227;
$green: #58b982;
$blue: #5f8fc7;
$red: #c96262;
$purple: #8c73c9;

.wallet-page {
  min-height: 100vh;
  background: $bg;
  color: $text;
  padding: 24rpx;
  box-sizing: border-box;
}

.balance-card,
.section {
  background: $panel;
  border: 1rpx solid $line;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
}

.balance-card {
  padding: 30rpx;
  background:
    radial-gradient(circle at 20% 0%, rgba($gold, 0.28), transparent 42%),
    linear-gradient(135deg, #1c1810 0%, #171719 70%);
}

.balance-label,
.balance-desc,
.metric-label,
.tool-desc,
.transaction-time {
  color: $muted;
  font-size: 23rpx;
}

.balance-value {
  display: block;
  margin: 14rpx 0 8rpx;
  font-size: 64rpx;
  line-height: 74rpx;
  font-weight: 800;
  color: $text;
}

.balance-desc {
  display: block;
}

.balance-metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14rpx;
  margin-top: 30rpx;
}

.metric {
  padding: 22rpx;
  border-radius: 12rpx;
  background: rgba(255, 255, 255, 0.055);
}

.metric-value {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  margin-bottom: 8rpx;
}

.balance-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
  margin-top: 28rpx;
}

.primary-btn,
.ghost-btn,
.sheet-confirm {
  height: 82rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 700;
}

.primary-btn,
.sheet-confirm {
  background: $gold;
  color: #16130b;
}

.ghost-btn {
  background: rgba($gold, 0.14);
  color: $gold;
}

.section {
  padding: 24rpx;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  margin-bottom: 22rpx;
}

.section-head .section-title {
  margin-bottom: 0;
}

.section-link {
  color: $gold;
  font-size: 24rpx;
}

.tool-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14rpx;
}

.tool-card {
  min-height: 150rpx;
  background: $panel2;
  border-radius: 12rpx;
  padding: 18rpx;
  box-sizing: border-box;
}

.tool-icon,
.transaction-icon {
  width: 54rpx;
  height: 54rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 700;
  color: $gold;
  background: rgba($gold, 0.15);
  margin-bottom: 14rpx;

  &.green { color: $green; background: rgba($green, 0.15); }
  &.blue { color: $blue; background: rgba($blue, 0.15); }
  &.purple { color: $purple; background: rgba($purple, 0.15); }
  &.income { color: $green; background: rgba($green, 0.15); }
  &.expense { color: $red; background: rgba($red, 0.15); }
}

.tool-title {
  display: block;
  font-size: 27rpx;
  font-weight: 700;
  margin-bottom: 6rpx;
}

.segmented {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10rpx;
  padding: 10rpx;
  background: $panel2;
  border-radius: 12rpx;
  margin: 22rpx 0 8rpx;
}

.segment {
  height: 58rpx;
  border-radius: 9rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $muted;
  font-size: 24rpx;

  &.active {
    background: $gold;
    color: #16130b;
    font-weight: 700;
  }
}

.transaction-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-height: 104rpx;
  border-bottom: 1rpx solid $line;

  &:last-child {
    border-bottom: none;
  }
}

.transaction-icon {
  margin-bottom: 0;
  flex-shrink: 0;
}

.transaction-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.transaction-title {
  font-size: 26rpx;
  font-weight: 600;
}

.transaction-amount {
  font-size: 26rpx;
  font-weight: 700;
  color: $red;

  &.income {
    color: $green;
  }
}

.empty-state {
  padding: 50rpx 0 28rpx;
  text-align: center;
  color: $dim;
  font-size: 25rpx;
}

.sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  background: rgba(0, 0, 0, 0.58);
  display: flex;
  align-items: flex-end;
}

.amount-sheet {
  width: 100%;
  padding: 38rpx 32rpx calc(40rpx + env(safe-area-inset-bottom));
  background: $panel;
  border-radius: 24rpx 24rpx 0 0;
  border-top: 1rpx solid $line;
  box-sizing: border-box;
}

.sheet-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 18rpx;
}

.sheet-desc,
.withdraw-info {
  color: $muted;
  font-size: 24rpx;
}

.amount-input {
  height: 96rpx;
  margin: 20rpx 0;
  border-radius: 12rpx;
  background: $panel2;
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 0 24rpx;
  color: $gold;
  font-size: 34rpx;
}

.amount-input input {
  flex: 1;
  color: $text;
  font-size: 34rpx;
}

.quick-amounts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12rpx;
  margin-bottom: 22rpx;
}

.quick-item {
  height: 68rpx;
  border-radius: 10rpx;
  background: $panel2;
  color: $muted;
  display: flex;
  align-items: center;
  justify-content: center;

  &.active {
    background: rgba($gold, 0.18);
    color: $gold;
  }
}

.withdraw-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 22rpx;
}
</style>
