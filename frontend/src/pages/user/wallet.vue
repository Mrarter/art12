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
          <text class="metric-tip">未确认收货前不可提现</text>
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

    <view v-if="errorText" class="error-state">{{ errorText }}</view>

    <view class="section rules-section">
      <view class="section-head">
        <text class="section-title">提现说明</text>
      </view>
      <view class="rules-grid">
        <view class="rule-card" v-for="item in withdrawRuleCards" :key="item.label">
          <text class="rule-card-label">{{ item.label }}</text>
          <text class="rule-card-value">{{ item.value }}</text>
          <text class="rule-card-desc">{{ item.desc }}</text>
        </view>
      </view>
      <view class="rules-list">
        <view class="rules-row" v-for="item in withdrawRuleRows" :key="item.label">
          <text class="rules-row-label">{{ item.label }}</text>
          <text class="rules-row-value">{{ item.value }}</text>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title" @click="goPayAccounts">管理账户</text>
      </view>
      <view class="payment-grid">
        <view
          class="payment-entry"
          :class="{ bound: item.bound }"
          v-for="item in paymentBindingItems"
          :key="item.type"
          @click="goPage(item.path)"
        >
          <text v-if="item.bound" class="payment-status">已绑定</text>
          <view class="payment-icon" :class="item.tone">{{ item.icon }}</view>
          <view class="payment-copy">
            <text class="payment-title">{{ item.label }}</text>
            <text class="payment-desc">{{ item.desc }}</text>
          </view>
        </view>
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
            <view class="bill-title-row">
              <text class="bill-title">{{ billTitle(item) }}</text>
              <text class="bill-tag" v-if="relatedTypeText(item.relatedType)">
                {{ relatedTypeText(item.relatedType) }}
              </text>
            </view>
            <text v-if="billDescription(item)" class="bill-desc">{{ billDescription(item) }}</text>
            <view class="bill-meta">
              <text>{{ formatBillTime(resolveBillTime(item)) }}</text>
              <text v-if="billRelatedLabel(item)">{{ billRelatedLabel(item) }}</text>
            </view>
          </view>
          <view class="bill-side">
            <text class="bill-amount" :class="amountClass(item.amount)">
              {{ Number(item.amount) > 0 ? '+' : '' }}{{ formatAmount(item.amount) }}
            </text>
            <text v-if="hasBillBalance(item)" class="bill-balance">
              余额 ¥{{ formatAmount(item.afterBalance) }}
            </text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getWalletInfo, getWalletBills } from '@/api/wallet'
import { getPayAccountList } from '@/api/pay'
import { formatYuanNumber } from '@/utils/price'

export default {
  data() {
    return {
      wallet: { balance: 0, freezeAmount: 0, pendingAmount: 0, totalIncome: 0 },
      bills: [],
      payAccounts: [],
      loading: false,
      errorText: '',
      showWithdraw: false,
      withdrawAmount: ''
    }
  },

  computed: {
    withdrawRuleCards() {
      return [
        {
          label: '可提现额度',
          value: `¥${this.formatAmount(this.wallet.balance)}`,
          desc: '当前可直接申请提现的余额'
        },
        {
          label: '每日提现次数',
          value: '3 次',
          desc: '超过当日次数请次日再申请'
        },
        {
          label: '提现提交时间',
          value: '每日可提交',
          desc: '提交后进入平台审核流程'
        },
        {
          label: '到账时间',
          value: '1-3 个工作日',
          desc: '以银行或支付渠道处理为准'
        }
      ]
    },

    withdrawRuleRows() {
      return [
        {
          label: '到账账户',
          value: '默认提现至已绑定银行卡'
        },
        {
          label: '手续费',
          value: '含第三方手续费 0.06%'
        },
        {
          label: '冻结资金说明',
          value: '未确认收货前的销售款处于冻结中，不计入可提现余额'
        }
      ]
    },

    paymentBindingItems() {
      const alipay = this.payAccountByType(2)
      const wechat = this.payAccountByType(1)
      const bank = this.payAccountByType(3)
      return [
        {
          type: 'alipay',
          label: alipay ? '已绑定支付宝' : '绑定支付宝',
          desc: alipay ? this.payAccountSummary(alipay) : '用于收款、提现与实名认证',
          icon: '支',
          tone: 'alipay',
          bound: !!alipay,
          path: alipay ? '/pages/user-extra/pay-account/list' : '/pages/user-extra/pay-account/add?type=alipay'
        },
        {
          type: 'wechat',
          label: wechat ? '已绑定微信' : '绑定微信',
          desc: wechat ? this.payAccountSummary(wechat) : '用于微信支付身份与收款',
          icon: '微',
          tone: 'wechat',
          bound: !!wechat,
          path: wechat ? '/pages/user-extra/pay-account/list' : '/pages/user-extra/pay-account/add?type=wechat'
        },
        {
          type: 'bank',
          label: bank ? '已绑定银行卡' : '绑定银行卡',
          desc: bank ? this.payAccountSummary(bank) : '用于余额提现到账',
          icon: '卡',
          tone: 'bank',
          bound: !!bank,
          path: bank ? '/pages/user-extra/pay-account/list' : '/pages/user-extra/pay-account/bank-card'
        }
      ]
    }
  },

  onShow() {
    this.loadData()
  },

  methods: {
    async loadData() {
      this.loading = true
      this.errorText = ''
      try {
        const [walletResult, billsResult, accountsResult] = await Promise.allSettled([
          getWalletInfo(),
          getWalletBills(1, 50),
          getPayAccountList()
        ])

        if (walletResult.status === 'fulfilled') this.wallet = walletResult.value || this.wallet
        if (billsResult.status === 'fulfilled') this.bills = billsResult.value || []
        if (accountsResult.status === 'fulfilled') this.payAccounts = accountsResult.value || []

        const failed = [walletResult, billsResult].find(item => item.status === 'rejected')
        if (failed) throw failed.reason
      } catch (e) {
        console.error('加载钱包数据失败:', e)
        this.errorText = e?.message || '钱包数据加载失败'
      } finally {
        this.loading = false
      }
    },

    payAccountByType(type) {
      return this.payAccounts.find(item => Number(item.accountType) === Number(type))
    },

    payAccountSummary(item) {
      if (!item) return ''
      const suffix = item.isDefault ? ' · 默认账户' : ''
      if (Number(item.accountType) === 1) {
        return `${item.wechatOpenid ? `微信 ${item.wechatOpenid}` : '微信收款账户'}${suffix}`
      }
      if (Number(item.accountType) === 2) {
        return `${item.alipayAccount || '支付宝收款账户'}${suffix}`
      }
      if (Number(item.accountType) === 3) {
        return `${`${item.bankName || '银行卡'} ${item.bankCard || ''}`.trim()}${suffix}`
      }
      return item.accountTypeText || '已绑定收款账户'
    },

    formatAmount(val) {
      if (!val && val !== 0) return '0.00'
      return formatYuanNumber(val)
    },

    billTypeText(type) {
      const map = {
        income: '余额收入',
        withdraw: '余额提现',
        withdraw_fee: '提现手续费',
        withdraw_reject: '提现退回',
        freeze: '金额冻结',
        unfreeze: '冻结释放',
        commission: '推广分成',
        resale: '转售收入',
        seller_income: '卖家收入',
        artist_income: '艺术家收入',
        refund: '订单退款',
        refund_reverse: '退款冲正',
        deposit: '保证金',
        transfer: '余额转账',
        payment: '订单支付',
        pay: '订单支付',
        order_pay: '订单支付',
        order_income: '订单入账',
        platform_fee: '平台服务费',
        platform_commission: '平台佣金',
        platform_service_fee: '平台服务费',
        service_fee: '服务费',
        order_sale: '作品销售',
        order_sale_release: '销售款解冻'
      }
      return map[type] || this.fallbackBillTypeText(type)
    },

    billTitle(item) {
      if (item?.billType === 'order_sale') {
        return this.extractArtworkTitle(item) || '作品销售'
      }
      return this.billTypeText(item?.billType)
    },

    extractArtworkTitle(item) {
      const directTitle = item?.artworkTitle || item?.itemTitle || item?.title
      if (directTitle) return directTitle
      const remark = item?.remark || ''
      const match = String(remark).match(/^作品销售[:：]\s*(.*?)\s+SYJ\d+/)
      return match ? match[1].trim() : ''
    },

    extractOrderNo(item) {
      const explicitNo = item?.relatedNo || item?.orderNo || item?.order_no || item?.tradeNo || item?.trade_no
      if (explicitNo) return explicitNo
      const remark = item?.remark || ''
      const match = String(remark).match(/SYJ\d+/)
      return match ? match[0] : ''
    },

    billTypeIcon(type) {
      const map = {
        income: '入',
        commission: '佣',
        resale: '售',
        seller_income: '售',
        artist_income: '艺',
        refund: '退',
        refund_reverse: '冲',
        withdraw: '提',
        withdraw_fee: '费',
        withdraw_reject: '回',
        freeze: '冻',
        unfreeze: '解',
        payment: '付',
        pay: '付',
        order_pay: '付',
        order_income: '单',
        order_sale: '售',
        order_sale_release: '解',
        platform_fee: '费',
        platform_commission: '佣',
        platform_service_fee: '费',
        service_fee: '费',
        deposit: '保',
        transfer: '转'
      }
      return map[type] || '·'
    },

    billTypeClass(type) {
      const map = {
        income: 'green',
        commission: 'green',
        resale: 'green',
        seller_income: 'green',
        artist_income: 'green',
        order_income: 'green',
        order_sale: 'green',
        order_sale_release: 'green',
        platform_fee: 'green',
        platform_commission: 'green',
        platform_service_fee: 'green',
        refund: 'orange',
        refund_reverse: 'red',
        withdraw: 'red',
        withdraw_fee: 'red',
        payment: 'red',
        pay: 'red',
        order_pay: 'red',
        freeze: 'gray',
        unfreeze: 'gray'
      }
      return map[type] || ''
    },

    amountClass(amount) {
      return Number(amount) > 0 ? 'income' : 'expense'
    },

    billDescription(item) {
      if (item?.billType === 'order_sale') {
        const orderNo = this.extractOrderNo(item)
        return orderNo ? `作品销售冻结中 · ${orderNo}` : '作品销售冻结中'
      }
      if (item?.billType === 'order_sale_release') {
        const orderNo = this.extractOrderNo(item)
        return orderNo ? `确认收货后可提现 · ${orderNo}` : '确认收货后可提现'
      }
      if (item?.remark) return item.remark
      const map = {
        platform_fee: '平台服务费结算',
        platform_commission: '平台佣金结算',
        platform_service_fee: '平台服务费结算',
        service_fee: '服务费用结算',
        order_income: '订单完成后入账',
        seller_income: '转售订单收入',
        artist_income: '艺术家作品收入',
        resale: '作品转售收入',
        commission: '推广佣金入账',
        refund: '订单退款到账',
        refund_reverse: '退款失败或撤销后的冲正',
        withdraw: '余额提现申请',
        withdraw_fee: '提现产生的手续费',
        withdraw_reject: '提现失败退回余额',
        freeze: '交易资金暂时冻结',
        unfreeze: '冻结资金释放',
        payment: '订单余额支付',
        pay: '订单余额支付',
        order_pay: '订单余额支付'
      }
      return map[item?.billType] || ''
    },

    relatedTypeText(type) {
      const map = {
        order: '订单',
        ORDER: '订单',
        resale: '转售',
        RESALE: '转售',
        withdraw: '提现',
        WITHDRAW: '提现',
        refund: '退款',
        REFUND: '退款',
        ledger: '账务',
        LEDGER: '账务'
      }
      return map[type] || ''
    },

    fallbackBillTypeText(type) {
      if (!type) return '余额变动'
      return String(type)
        .split('_')
        .filter(Boolean)
        .map(part => part.slice(0, 1).toUpperCase() + part.slice(1))
        .join(' ')
    },

    formatBillTime(value) {
      if (!value) return '时间待同步'
      return String(value).replace('T', ' ').slice(0, 16)
    },

    resolveBillTime(item) {
      return item?.billTime || item?.createdTime || item?.createTime || item?.created_time || item?.time
    },

    billRelatedLabel(item) {
      if (!item) return ''
      const relatedNo = this.extractOrderNo(item)
      if (relatedNo) {
        return `${this.relatedTypeText(item.relatedType) || '关联'} ${relatedNo}`
      }
      return item.relatedId ? `关联编号 ${item.relatedId}` : ''
    },

    hasBillBalance(item) {
      return item && item.afterBalance !== undefined && item.afterBalance !== null && item.afterBalance !== ''
    },

    goWithdraw() {
      getPayAccountList().then(list => {
        const hasBankAccount = Array.isArray(list) && list.some(item => Number(item.accountType) === 3)
        if (!hasBankAccount) {
          uni.showModal({
            title: '提示',
            content: '提现默认到账银行卡，请先绑定银行卡',
            confirmText: '去绑定',
            success: (r) => {
              if (r.confirm) uni.navigateTo({ url: '/pages/user-extra/pay-account/bank-card' })
            }
          })
        } else {
          uni.navigateTo({ url: '/pages/promoter/withdraw' })
        }
      }).catch(() => {
        uni.navigateTo({ url: '/pages/promoter/withdraw' })
      })
    },

    goPayAccounts() {
      uni.navigateTo({ url: '/pages/user-extra/pay-account/list' })
    },

    goPage(path) {
      if (!path) return
      uni.navigateTo({ url: path })
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
.metric {
  flex: 1;
  min-width: 0;
  text-align: center;
  padding: 0 8rpx;
  box-sizing: border-box;
}
.metric-value { color: #d4af37; font-size: 32rpx; font-weight: 700; display: block; }
.metric-label { color: #888; font-size: 22rpx; margin-top: 6rpx; }
.metric-tip {
  display: block;
  margin-top: 6rpx;
  color: rgba(212, 175, 55, 0.72);
  font-size: 18rpx;
  line-height: 24rpx;
  word-break: keep-all;
}

.balance-actions { display: flex; gap: 20rpx; margin-top: 32rpx; }
.primary-btn {
  flex: 1; height: 88rpx; border-radius: 999rpx; background: #d4af37; color: #16130b;
  font-size: 30rpx; font-weight: 800; display: flex; align-items: center; justify-content: center;
}

.section { margin: 24rpx; }
.section-head { margin-bottom: 20rpx; }
.section-title { color: #f5f5f5; font-size: 30rpx; font-weight: 700; }

.rules-section {
  padding: 24rpx;
  border-radius: 16rpx;
  background:
    linear-gradient(180deg, rgba(212, 175, 55, 0.07), rgba(255, 255, 255, 0.025)),
    #151515;
  border: 1rpx solid rgba(212, 175, 55, 0.14);
}

.rules-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
}

.rule-card {
  min-height: 154rpx;
  padding: 20rpx;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.03);
  border: 1rpx solid rgba(255, 255, 255, 0.06);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.rule-card-label {
  color: #9a9588;
  font-size: 22rpx;
}

.rule-card-value {
  color: #f0d77a;
  font-size: 28rpx;
  line-height: 38rpx;
  font-weight: 800;
}

.rule-card-desc {
  color: #8b8b8b;
  font-size: 22rpx;
  line-height: 30rpx;
}

.rules-list {
  margin-top: 18rpx;
  padding-top: 10rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.rules-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
  padding: 14rpx 0;
}

.rules-row-label {
  width: 176rpx;
  flex-shrink: 0;
  color: #bcb7aa;
  font-size: 24rpx;
  line-height: 34rpx;
}

.rules-row-value {
  flex: 1;
  text-align: right;
  color: #8d8d8d;
  font-size: 24rpx;
  line-height: 34rpx;
}

.section-link {
  color: #d4af37;
  font-size: 24rpx;
  font-weight: 700;
}

.payment-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
}

.payment-entry {
  position: relative;
  min-height: 156rpx;
  padding: 22rpx 12rpx 18rpx;
  border-radius: 14rpx;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.025)),
    #1a1a1a;
  border: 1rpx solid rgba(255, 255, 255, 0.07);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  text-align: center;
  box-sizing: border-box;
}

.payment-entry.bound {
  border-color: rgba(82, 196, 26, 0.28);
  background:
    linear-gradient(180deg, rgba(82, 196, 26, 0.08), rgba(255, 255, 255, 0.025)),
    #1a1a1a;
}

.payment-status {
  position: absolute;
  top: 10rpx;
  right: 10rpx;
  padding: 2rpx 8rpx;
  border-radius: 999rpx;
  background: rgba(82, 196, 26, 0.14);
  color: #52c41a;
  font-size: 17rpx;
  line-height: 24rpx;
  font-weight: 800;
}

.payment-icon {
  width: 58rpx;
  height: 58rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 900;

  &.alipay {
    color: #63a7ff;
    background: rgba(22, 119, 255, 0.16);
  }

  &.wechat {
    color: #44d486;
    background: rgba(7, 193, 96, 0.16);
  }

  &.bank {
    color: #d4af37;
    background: rgba(212, 175, 55, 0.16);
  }
}

.payment-copy {
  width: 100%;
  min-width: 0;
}

.payment-title {
  display: block;
  color: #f5f5f5;
  font-size: 23rpx;
  line-height: 30rpx;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.payment-desc {
  display: block;
  margin-top: 6rpx;
  color: #888;
  font-size: 19rpx;
  line-height: 27rpx;
  min-height: 54rpx;
  word-break: break-all;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.bill-list { display: flex; flex-direction: column; gap: 2rpx; }
.bill-row {
  display: flex; align-items: center; gap: 18rpx; padding: 24rpx;
  background: #1a1a1a; border-radius: 16rpx; margin-bottom: 12rpx;
  box-sizing: border-box;
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

.bill-main {
  flex: 1;
  min-width: 0;
}

.bill-title-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
  min-width: 0;
}

.bill-title {
  color: #f5f5f5;
  font-size: 26rpx;
  line-height: 34rpx;
  font-weight: 700;
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bill-tag {
  flex-shrink: 0;
  padding: 2rpx 10rpx;
  border-radius: 999rpx;
  background: rgba(212, 175, 55, 0.12);
  color: #d4af37;
  font-size: 18rpx;
  line-height: 28rpx;
  font-weight: 700;
}

.bill-desc {
  display: block;
  margin-top: 6rpx;
  color: #c7c7c7;
  font-size: 22rpx;
  line-height: 30rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bill-meta {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-top: 4rpx;
  color: #888;
  font-size: 20rpx;
  line-height: 28rpx;
  flex-wrap: wrap;
}

.bill-side {
  flex-shrink: 0;
  min-width: 156rpx;
  text-align: right;
}

.bill-amount {
  display: block;
  font-size: 28rpx;
  line-height: 36rpx;
  font-weight: 800;
}
.bill-amount.income { color: #52c41a; }
.bill-amount.expense { color: #ff4d4f; }

.bill-balance {
  display: block;
  margin-top: 6rpx;
  color: #888;
  font-size: 19rpx;
  line-height: 26rpx;
}

.loading-state, .empty-state { text-align: center; color: #888; font-size: 26rpx; padding: 60rpx 0; }
.error-state {
  margin: 0 24rpx 24rpx;
  padding: 20rpx 24rpx;
  border-radius: 12rpx;
  background: rgba(255, 77, 79, 0.1);
  color: #ff7875;
  font-size: 24rpx;
  text-align: center;
}
</style>
