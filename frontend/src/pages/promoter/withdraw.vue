<template>
  <view class="withdraw-page">
    <!-- 余额卡片 -->
    <view class="balance-card">
      <view class="balance-header">
        <text class="label">可提现余额（元）</text>
        <view class="refresh-btn" @click="refreshBalance">
          <text>刷</text>
        </view>
      </view>
      <view class="balance-amount">
        <text class="currency">¥</text>
        <text class="amount">{{ balance }}</text>
      </view>
      <view class="balance-stats">
        <view class="stat-item">
          <text class="stat-label">今日收益</text>
          <text class="stat-value">+{{ todayEarnings }}</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-label">本月收益</text>
          <text class="stat-value">+{{ monthEarnings }}</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-label">累计收益</text>
          <text class="stat-value">{{ totalEarnings }}</text>
        </view>
      </view>
    </view>

    <view class="rules-overview">
      <view class="overview-head">
        <text class="overview-title">提现须知</text>
        <text class="overview-subtitle">提交前请确认到账账户和提现规则</text>
      </view>
      <view class="overview-grid">
        <view class="overview-card" v-for="item in withdrawRuleCards" :key="item.label">
          <text class="overview-label">{{ item.label }}</text>
          <text class="overview-value">{{ item.value }}</text>
          <text class="overview-desc">{{ item.desc }}</text>
        </view>
      </view>
    </view>

    <!-- 提现表单 -->
    <view class="form-section">
      <view class="form-title">申请提现</view>
      
      <view class="form-item">
        <text class="form-label">提现金额</text>
        <view class="amount-input">
          <text class="currency-prefix">¥</text>
          <input 
            type="digit" 
            v-model="amount" 
            placeholder="0.00"
            class="amount-field"
            @input="onAmountInput"
          />
          <text class="all-btn" @click="withdrawAll">全部</text>
        </view>
        <view class="amount-hint" v-if="amount">
          <text>实际到账：¥{{ actualAmount }}</text>
          <text class="fee-note">（含第三方手续费 0.06%，合计 ¥{{ serviceFee }}）</text>
        </view>
      </view>

      <!-- 快速金额选择 -->
      <view class="quick-amounts">
        <view 
          class="quick-item" 
          :class="{ active: isQuickAmount(0.25) }"
          @click="selectAmount(0.25)"
        >
          <text>25%</text>
        </view>
        <view 
          class="quick-item" 
          :class="{ active: isQuickAmount(0.5) }"
          @click="selectAmount(0.5)"
        >
          <text>50%</text>
        </view>
        <view 
          class="quick-item" 
          :class="{ active: isQuickAmount(0.75) }"
          @click="selectAmount(0.75)"
        >
          <text>75%</text>
        </view>
        <view 
          class="quick-item" 
          :class="{ active: isQuickAmount(1) }"
          @click="selectAmount(1)"
        >
          <text>全部</text>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">到账方式</text>
        <view class="payment-method">
          <view v-if="selectedAccount" class="method-item active bank">
            <view class="method-icon-text">卡</view>
            <view class="method-info">
              <text class="method-name">{{ selectedAccount.bankName || '银行卡' }}</text>
              <text class="method-desc">{{ selectedAccount.bankCard || '默认提现至银行卡' }}</text>
            </view>
            <text class="method-tag">默认</text>
          </view>
          <view v-else class="method-item empty" @click="goBindBank">
            <view class="method-icon-text">卡</view>
            <view class="method-info">
              <text class="method-name">未绑定银行卡</text>
              <text class="method-desc">提现默认到账银行卡，请先添加银行卡</text>
            </view>
            <text class="method-tag">去绑定</text>
          </view>
        </view>
      </view>

      <!-- 协议勾选 -->
      <view class="agreement-row">
        <checkbox-group @change="onAgreeChange">
          <label class="agreement-label">
            <checkbox value="agree" :checked="agreed" color="#667eea" />
            <text class="agreement-text">
              我已阅读并同意
              <text class="link" @click.stop="showWithdrawAgreement">《提现服务协议》</text>
            </text>
          </label>
        </checkbox-group>
      </view>

      <button 
        class="submit-btn" 
        :class="{ disabled: !canSubmit }"
        @click="submitWithdraw"
        :disabled="!canSubmit"
      >
        确认提现
      </button>
    </view>

    <!-- 提现记录入口 -->
    <view class="records-entry" @click="goToRecords">
      <view class="entry-left">
        <text class="entry-icon">提</text>
        <text class="entry-text">提现记录</text>
      </view>
      <view class="entry-right">
        <text class="pending-count" v-if="pendingCount > 0">
          {{ pendingCount }} 笔处理中
        </text>
        
      </view>
    </view>

    <!-- 提现规则 -->
    <view class="rules-section">
      <view class="rules-header" @click="showRules = !showRules">
        <text class="rules-title">提现规则</text>
      </view>
      <view class="rules-content" v-if="showRules">
        <view class="rule-item">
          <view class="rule-icon">1</view>
          <view class="rule-text">
            <text class="rule-title">可提现额度</text>
            <text class="rule-desc">仅可对当前“可提现余额”发起申请，冻结中与待结算金额不可直接提现。</text>
          </view>
        </view>
        <view class="rule-item">
          <view class="rule-icon">2</view>
          <view class="rule-text">
            <text class="rule-title">提现金额范围</text>
            <text class="rule-desc">提现金额需大于 0 元，且不能超过当前可提现余额。</text>
          </view>
        </view>
        <view class="rule-item">
          <view class="rule-icon">3</view>
          <view class="rule-text">
            <text class="rule-title">每日提现次数</text>
            <text class="rule-desc">同一账户每日最多提现 {{ dailyLimit }} 次，超过次数请次日再申请。</text>
          </view>
        </view>
        <view class="rule-item">
          <view class="rule-icon">4</view>
          <view class="rule-text">
            <text class="rule-title">提现提交时间</text>
            <text class="rule-desc">每日均可提交提现申请，提交后进入平台审核，处理进度可在提现记录查看。</text>
          </view>
        </view>
        <view class="rule-item">
          <view class="rule-icon">5</view>
          <view class="rule-text">
            <text class="rule-title">到账时间</text>
            <text class="rule-desc">审核通过后预计 {{ withdrawDays }} 个工作日内到账，具体以银行或支付渠道处理时效为准。</text>
          </view>
        </view>
        <view class="rule-item">
          <view class="rule-icon">6</view>
          <view class="rule-text">
            <text class="rule-title">手续费与到账账户</text>
            <text class="rule-desc">每笔提现收取第三方手续费 {{ feeRate }}%，默认到账至已绑定银行卡，请确保账户信息真实有效。</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 安全提示 -->
    <view class="security-tip">
      
      <text class="tip-text">资金安全由保险公司承保，请放心使用</text>
    </view>

    <!-- 提现确认弹窗 -->
    <!-- 弹窗开始 -->
      <view v-if="showConfirmModal" class="confirm-modal">
        <view class="modal-title">确认提现</view>
        <view class="modal-content">
          <view class="confirm-item">
            <text class="confirm-label">提现金额</text>
            <text class="confirm-value primary">¥{{ amount }}</text>
          </view>
          <view class="confirm-item">
            <text class="confirm-label">手续费</text>
            <text class="confirm-value">¥{{ serviceFee }}</text>
          </view>
          <view class="confirm-item">
            <text class="confirm-label">实际到账</text>
            <text class="confirm-value highlight">¥{{ actualAmount }}</text>
          </view>
          <view class="confirm-item">
            <text class="confirm-label">到账方式</text>
            <text class="confirm-value">{{ withdrawAccountLabel }}</text>
          </view>
        </view>
        <view class="modal-actions">
          <button class="cancel-btn" @click="showConfirmModal = false">取消</button>
          <button class="confirm-btn" @click="confirmSubmit">确认提交</button>
        </view>
      </view>
<!-- 弹窗结束 -->

    <!-- 成功弹窗 -->
    <!-- 弹窗开始 -->
      <view v-if="showSuccessModal" class="success-modal">
        <view class="success-icon">
          <text>✓</text>
        </view>
        <view class="success-title">提现申请已提交</view>
        <view class="success-desc">
          <text>预计 1-3 个工作日内到账</text>
          <text>实际到账金额：¥{{ actualAmount }}</text>
        </view>
        <view class="success-actions">
          <button class="view-btn" @click="goToRecords">查看提现记录</button>
          <button class="back-btn" @click="goBack">返回</button>
        </view>
      </view>
<!-- 弹窗结束 -->
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getPayAccountList } from '@/api/pay'
import { getWalletInfo, getWalletBills } from '@/api/wallet'
import { getEarningsList, getWithdrawList, withdrawApply } from '@/api/promoter'
import { formatYuanNumber } from '@/utils/price'

// 状态
const wallet = ref({ balance: 0, totalIncome: 0 })
const todayEarningsYuan = ref(0)
const monthEarningsYuan = ref(0)
const amount = ref('')
const agreed = ref(false)
const showRules = ref(true)
const showConfirmModal = ref(false)
const showSuccessModal = ref(false)
const pendingCount = ref(0)
const payAccounts = ref([])
const selectedAccount = ref(null)
const submitting = ref(false)
const loading = ref(false)

// 提现规则配置
const feeRate = ref(0.06)
const dailyLimit = ref(3)
const withdrawDays = ref(3)

const balanceYuanValue = computed(() => Number(wallet.value.balance || 0))
const balance = computed(() => formatYuanNumber(balanceYuanValue.value))
const todayEarnings = computed(() => formatYuanNumber(todayEarningsYuan.value))
const monthEarnings = computed(() => formatYuanNumber(monthEarningsYuan.value))
const totalEarnings = computed(() => formatYuanNumber(wallet.value.totalIncome || 0))

// 计算属性
const serviceFee = computed(() => {
  const amt = parseFloat(amount.value) || 0
  return (amt * feeRate.value / 100).toFixed(2)
})

const actualAmount = computed(() => {
  const amt = parseFloat(amount.value) || 0
  return (amt - serviceFee.value).toFixed(2)
})

const canSubmit = computed(() => {
  const amt = parseFloat(amount.value) || 0
  return amt > 0 &&
    amt <= balanceYuanValue.value &&
    Number(actualAmount.value) > 0 &&
    agreed.value &&
    !!selectedAccount.value &&
    !submitting.value
})

const withdrawAccountLabel = computed(() => {
  if (!selectedAccount.value) return '未绑定银行卡'
  return `${selectedAccount.value.bankName || '银行卡'} ${selectedAccount.value.bankCard || ''}`.trim()
})

const withdrawRuleCards = computed(() => [
  {
    label: '当前可提现',
    value: `¥${balance.value}`,
    desc: '仅可提现可用余额部分'
  },
  {
    label: '每日次数',
    value: `${dailyLimit.value} 次`,
    desc: '当日超限后需次日再申请'
  },
  {
    label: '提现提交时间',
    value: '每日可提交',
    desc: '提交后进入平台审核流程'
  },
  {
    label: '到账时效',
    value: `${withdrawDays.value} 个工作日内`,
    desc: '审核通过后按渠道处理到账'
  }
])

// 方法
const refreshBalance = async () => {
  uni.showLoading({ title: '刷新中...' })
  try {
    await loadData()
    uni.hideLoading()
    uni.showToast({ title: '已刷新', icon: 'success' })
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '刷新失败', icon: 'none' })
  }
}

const onAmountInput = (e) => {
  const value = String(e.detail.value || '')
    .replace(/[^\d.]/g, '')
    .replace(/^(\d*\.\d{0,2}).*$/, '$1')
  amount.value = value
  if (parseFloat(value) > balanceYuanValue.value) {
    amount.value = balanceYuanValue.value.toFixed(2)
  }
}

const selectAmount = (ratio) => {
  amount.value = (balanceYuanValue.value * ratio).toFixed(2)
}

const isQuickAmount = (ratio) => {
  return amount.value && Number(amount.value).toFixed(2) === (balanceYuanValue.value * ratio).toFixed(2)
}

const withdrawAll = () => {
  amount.value = balanceYuanValue.value.toFixed(2)
}

const onAgreeChange = (e) => {
  agreed.value = e.detail.value.includes('agree')
}

const showWithdrawAgreement = () => {
  uni.navigateTo({ url: '/pages/user-extra/agreement?type=withdraw' })
}

const submitWithdraw = () => {
  if (!selectedAccount.value) {
    uni.showModal({
      title: '请先绑定银行卡',
      content: '提现默认到账银行卡，请先添加银行卡后再提现。',
      confirmText: '去绑定',
      success: (res) => {
        if (res.confirm) goBindBank()
      }
    })
    return
  }
  const amt = Number(amount.value || 0)
  if (amt <= 0) {
    uni.showToast({ title: '请输入正确的提现金额', icon: 'none' })
    return
  }
  if (amt > balanceYuanValue.value) {
    uni.showToast({ title: '提现金额不能超过可提现余额', icon: 'none' })
    return
  }
  if (!agreed.value) {
    uni.showToast({ title: '请先阅读并同意提现服务协议', icon: 'none' })
    return
  }
  if (!canSubmit.value) return
  showConfirmModal.value = true
}

const confirmSubmit = async () => {
  if (!selectedAccount.value || submitting.value) return
  showConfirmModal.value = false
  submitting.value = true
  uni.showLoading({ title: '提交中...' })
  try {
    await withdrawApply({
      amount: Number(amount.value).toFixed(2),
      accountType: 'bank',
      accountInfo: withdrawAccountLabel.value,
      accountName: selectedAccount.value.realName || ''
    })
    uni.hideLoading()
    showSuccessModal.value = true
    amount.value = ''
    agreed.value = false
    await loadData()
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '提现申请失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

const goToRecords = () => {
  uni.navigateTo({ url: '/pages/promoter/withdrawLog' })
}

const goBack = () => {
  showSuccessModal.value = false
  uni.navigateBack()
}

const goBindBank = () => {
  uni.navigateTo({ url: '/pages/user-extra/pay-account/bank-card' })
}

const loadPayAccounts = async () => {
  try {
    const list = await getPayAccountList()
    payAccounts.value = Array.isArray(list) ? list : []
    selectedAccount.value = payAccounts.value.find(item => Number(item.accountType) === 3) || null
  } catch (e) {
    payAccounts.value = []
    selectedAccount.value = null
  }
}

const toYuanNumber = (value) => {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num : 0
}

const parsePageRecords = (data) => {
  if (Array.isArray(data)) return data
  return data?.records || data?.list || data?.items || []
}

const isSameDay = (value, date = new Date()) => {
  if (!value) return false
  const d = new Date(String(value).replace(' ', 'T'))
  return d.getFullYear() === date.getFullYear() &&
    d.getMonth() === date.getMonth() &&
    d.getDate() === date.getDate()
}

const isSameMonth = (value, date = new Date()) => {
  if (!value) return false
  const d = new Date(String(value).replace(' ', 'T'))
  return d.getFullYear() === date.getFullYear() && d.getMonth() === date.getMonth()
}

const loadWallet = async () => {
  const data = await getWalletInfo()
  wallet.value = data || { balance: 0, totalIncome: 0 }
}

const loadEarningsStats = async () => {
  todayEarningsYuan.value = 0
  monthEarningsYuan.value = 0
  try {
    const [billsData, earningsData] = await Promise.allSettled([
      getWalletBills(1, 100),
      getEarningsList({ page: 1, pageSize: 100 })
    ])
    const sources = []
    if (billsData.status === 'fulfilled') {
      sources.push(...parsePageRecords(billsData.value).map(item => ({
        amount: toYuanNumber(item.amount),
        time: item.createdTime
      })))
    }
    if (earningsData.status === 'fulfilled') {
      sources.push(...parsePageRecords(earningsData.value).map(item => ({
        amount: toYuanNumber(item.amount) / 100,
        time: item.createTime || item.createdTime
      })))
    }
    todayEarningsYuan.value = sources
      .filter(item => item.amount > 0 && isSameDay(item.time))
      .reduce((sum, item) => sum + item.amount, 0)
    monthEarningsYuan.value = sources
      .filter(item => item.amount > 0 && isSameMonth(item.time))
      .reduce((sum, item) => sum + item.amount, 0)
  } catch (e) {
    console.warn('加载收益统计失败:', e)
  }
}

const loadWithdrawSummary = async () => {
  try {
    const data = await getWithdrawList({ page: 1, pageSize: 50 })
    const records = parsePageRecords(data)
    pendingCount.value = records.filter(item => [0, 1, '0', '1', 'pending', 'processing'].includes(item.status)).length
  } catch (e) {
    pendingCount.value = 0
  }
}

const loadData = async () => {
  loading.value = true
  try {
    await Promise.all([
      loadWallet(),
      loadPayAccounts(),
      loadEarningsStats(),
      loadWithdrawSummary()
    ])
  } finally {
    loading.value = false
  }
}

onShow(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.withdraw-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 82% 0%, rgba(212, 175, 55, 0.16), transparent 32%),
    linear-gradient(180deg, #111 0%, #070707 52%, #050505 100%);
  padding: 24rpx 24rpx 48rpx;
  box-sizing: border-box;
}

.balance-card {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(212, 175, 55, 0.18), rgba(255, 255, 255, 0.035) 42%, rgba(212, 175, 55, 0.08)),
    #181818;
  border: 1rpx solid rgba(212, 175, 55, 0.28);
  border-radius: 20rpx;
  padding: 36rpx 30rpx 32rpx;
  color: #f7f2e3;
  margin-bottom: 24rpx;
  box-shadow: 0 24rpx 60rpx rgba(0, 0, 0, 0.28);

  .balance-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 18rpx;

    .label {
      font-size: 25rpx;
      color: #b8b0a0;
      font-weight: 700;
    }

    .refresh-btn {
      width: 54rpx;
      height: 54rpx;
      border-radius: 50%;
      background: rgba(212, 175, 55, 0.14);
      border: 1rpx solid rgba(212, 175, 55, 0.28);
      color: #d4af37;
      font-size: 21rpx;
      font-weight: 900;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .balance-amount {
    display: flex;
    align-items: baseline;
    margin-bottom: 34rpx;

    .currency {
      font-size: 34rpx;
      margin-right: 10rpx;
      color: #d4af37;
      font-weight: 800;
    }

    .amount {
      font-size: 70rpx;
      line-height: 80rpx;
      font-weight: 900;
      color: #fff;
      letter-spacing: 0;
    }
  }

  .balance-stats {
    display: flex;
    justify-content: space-between;
    padding-top: 22rpx;
    border-top: 1rpx solid rgba(212, 175, 55, 0.18);

    .stat-item {
      flex: 1;
      text-align: center;
      min-width: 0;

      .stat-label {
        font-size: 21rpx;
        color: #918b80;
        display: block;
        margin-bottom: 8rpx;
      }

      .stat-value {
        font-size: 25rpx;
        color: #f0d77a;
        font-weight: 800;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .stat-divider {
      width: 1rpx;
      background: rgba(212, 175, 55, 0.18);
    }
  }
}

.rules-overview {
  background:
    linear-gradient(180deg, rgba(212, 175, 55, 0.08), rgba(255, 255, 255, 0.025)),
    #151515;
  border: 1rpx solid rgba(212, 175, 55, 0.14);
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;

  .overview-head {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 24rpx;
    margin-bottom: 18rpx;
  }

  .overview-title {
    color: #f4f1e6;
    font-size: 30rpx;
    font-weight: 800;
  }

  .overview-subtitle {
    color: #8f8a7d;
    font-size: 22rpx;
    line-height: 30rpx;
    text-align: right;
  }

  .overview-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16rpx;
  }

  .overview-card {
    min-height: 150rpx;
    padding: 20rpx;
    border-radius: 16rpx;
    background: rgba(255, 255, 255, 0.03);
    border: 1rpx solid rgba(255, 255, 255, 0.06);
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }

  .overview-label {
    color: #9e9889;
    font-size: 22rpx;
  }

  .overview-value {
    color: #f0d77a;
    font-size: 30rpx;
    line-height: 38rpx;
    font-weight: 800;
  }

  .overview-desc {
    color: #8d8d8d;
    font-size: 22rpx;
    line-height: 30rpx;
  }
}

.rules-section {
  background: rgba(255, 255, 255, 0.035);
  border: 1rpx solid rgba(255, 255, 255, 0.055);
  border-radius: 14rpx;
  padding: 22rpx 24rpx;
  margin-bottom: 24rpx;

  .rules-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .rules-title {
      font-size: 25rpx;
      color: #b7b7b7;
      font-weight: 700;
    }
  }

  .rules-content {
    margin-top: 18rpx;

    .rule-item {
      display: flex;
      align-items: flex-start;
      margin-bottom: 16rpx;

      &:last-child {
        margin-bottom: 0;
      }

      .rule-icon {
        width: 32rpx;
        height: 32rpx;
        border-radius: 50%;
        background: rgba(212, 175, 55, 0.1);
        border: 1rpx solid rgba(212, 175, 55, 0.18);
        color: #a98b2c;
        font-size: 18rpx;
        font-weight: 900;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 14rpx;
        flex-shrink: 0;
      }

      .rule-text {
        min-width: 0;

        .rule-title {
          font-size: 23rpx;
          color: #cfcfcf;
          font-weight: 700;
          display: block;
          margin-bottom: 4rpx;
        }

        .rule-desc {
          font-size: 21rpx;
          line-height: 29rpx;
          color: #777;
        }
      }
    }
  }
}

.form-section {
  background: #151515;
  border: 1rpx solid rgba(255, 255, 255, 0.07);
  border-radius: 16rpx;
  padding: 32rpx 26rpx;
  margin-bottom: 24rpx;

  .form-title {
    font-size: 32rpx;
    font-weight: 900;
    color: #f5f5f5;
    margin-bottom: 30rpx;
  }

  .form-item {
    margin-bottom: 30rpx;

    .form-label {
      font-size: 28rpx;
      color: #f1f1f1;
      font-weight: 700;
      display: block;
      margin-bottom: 16rpx;
    }

    .amount-input {
      display: flex;
      align-items: center;
      min-height: 100rpx;
      border-bottom: 1rpx solid rgba(212, 175, 55, 0.24);
      padding-bottom: 14rpx;

      .currency-prefix {
        font-size: 48rpx;
        color: #d4af37;
        font-weight: 900;
        margin-right: 8rpx;
      }

      .amount-field {
        flex: 1;
        font-size: 48rpx;
        color: #fff;
        font-weight: 900;
        min-width: 0;
      }

      .all-btn {
        font-size: 25rpx;
        color: #d4af37;
        padding: 9rpx 18rpx;
        background: rgba(212, 175, 55, 0.12);
        border: 1rpx solid rgba(212, 175, 55, 0.2);
        border-radius: 10rpx;
        font-weight: 800;
      }
    }

    .amount-hint {
      margin-top: 16rpx;
      font-size: 24rpx;
      color: #c9c0a5;

      .fee-note {
        color: #888;
        margin-left: 8rpx;
      }
    }
  }

  .quick-amounts {
    display: flex;
    gap: 20rpx;
    margin-bottom: 30rpx;

    .quick-item {
      flex: 1;
      height: 64rpx;
      border-radius: 32rpx;
      background: #202020;
      border: 1rpx solid rgba(255, 255, 255, 0.06);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 26rpx;
      color: #a6a6a6;
      font-weight: 700;
      box-sizing: border-box;

      &.active {
        background: rgba(212, 175, 55, 0.16);
        color: #d4af37;
        border-color: rgba(212, 175, 55, 0.55);
      }
    }
  }

  .payment-method {
    .method-item {
      display: flex;
      align-items: center;
      padding: 22rpx;
      border: 1rpx solid rgba(255, 255, 255, 0.08);
      border-radius: 16rpx;
      background: #1b1b1b;

      &.active {
        border-color: rgba(212, 175, 55, 0.52);
        background: rgba(212, 175, 55, 0.08);
      }

      &.empty {
        border-style: dashed;
        background: #181818;
      }

      .method-icon {
        width: 48rpx;
        height: 48rpx;
        margin-right: 16rpx;
      }

      .method-icon-text {
        width: 56rpx;
        height: 56rpx;
        border-radius: 16rpx;
        margin-right: 16rpx;
        background: rgba(212, 175, 55, 0.16);
        color: #b8891e;
        font-size: 28rpx;
        font-weight: 800;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }

      .method-info {
        flex: 1;

        .method-name {
          font-size: 28rpx;
          color: #f4f4f4;
          font-weight: 800;
          display: block;
        }

        .method-desc {
          font-size: 24rpx;
          color: #8d8d8d;
          margin-top: 4rpx;
          display: block;
          word-break: break-all;
        }
      }

      .method-tag {
        color: #b8891e;
        background: rgba(212, 175, 55, 0.12);
        border-radius: 999rpx;
        padding: 8rpx 16rpx;
        font-size: 22rpx;
        flex-shrink: 0;
      }
    }
  }

  .agreement-row {
    margin-bottom: 30rpx;

    .agreement-label {
      display: flex;
      align-items: center;

      .agreement-text {
        font-size: 24rpx;
        color: #9a9a9a;
        margin-left: 8rpx;
      }

      .link {
        color: #d4af37;
      }
    }
  }

  .submit-btn {
    width: 100%;
    height: 88rpx;
    background: linear-gradient(135deg, #d8b84a 0%, #b68a24 100%);
    color: #111;
    font-size: 32rpx;
    font-weight: 900;
    border-radius: 44rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    box-shadow: 0 12rpx 28rpx rgba(212, 175, 55, 0.18);

    &.disabled {
      background: #2b2b2b;
      color: #666;
      box-shadow: none;
    }
  }
}

.records-entry {
  background: #151515;
  border: 1rpx solid rgba(255, 255, 255, 0.07);
  border-radius: 16rpx;
  padding: 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;

  .entry-left {
    display: flex;
    align-items: center;

    .entry-icon {
      width: 50rpx;
      height: 50rpx;
      border-radius: 14rpx;
      background: rgba(212, 175, 55, 0.14);
      color: #d4af37;
      font-size: 24rpx;
      font-weight: 900;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .entry-text {
      font-size: 28rpx;
      color: #f4f4f4;
      font-weight: 800;
      margin-left: 12rpx;
    }
  }

  .entry-right {
    display: flex;
    align-items: center;

    .pending-count {
      font-size: 24rpx;
      color: #d4af37;
      margin-right: 8rpx;
    }
  }
}

.security-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20rpx;

  .tip-text {
    font-size: 24rpx;
    color: #737373;
    margin-left: 8rpx;
  }
}

// 确认弹窗
.confirm-modal {
  position: fixed;
  left: 50%;
  top: 50%;
  z-index: 99;
  transform: translate(-50%, -50%);
  box-sizing: border-box;
  padding: 40rpx;
  width: 600rpx;
  max-width: calc(100vw - 64rpx);
  border-radius: 20rpx;
  background: #161616;
  border: 1rpx solid rgba(212, 175, 55, 0.24);
  box-shadow: 0 24rpx 80rpx rgba(0, 0, 0, 0.5);

  .modal-title {
    font-size: 34rpx;
    font-weight: 900;
    color: #f5f5f5;
    text-align: center;
    margin-bottom: 30rpx;
  }

  .modal-content {
    .confirm-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20rpx 0;
      border-bottom: 1rpx solid rgba(255, 255, 255, 0.06);

      .confirm-label {
        font-size: 28rpx;
        color: #8e8e8e;
      }

      .confirm-value {
        font-size: 28rpx;
        color: #f5f5f5;
        text-align: right;

        &.primary {
          color: #d4af37;
          font-weight: bold;
        }

        &.highlight {
          color: #52c41a;
          font-weight: bold;
          font-size: 32rpx;
        }
      }
    }
  }

  .modal-actions {
    display: flex;
    gap: 20rpx;
    margin-top: 30rpx;

    .cancel-btn {
      flex: 1;
      height: 80rpx;
      border: 1rpx solid rgba(255, 255, 255, 0.12);
      border-radius: 40rpx;
      background: #202020;
      font-size: 28rpx;
      color: #ddd;
    }

    .confirm-btn {
      flex: 1;
      height: 80rpx;
      background: linear-gradient(135deg, #d8b84a 0%, #b68a24 100%);
      border-radius: 40rpx;
      color: #111;
      font-weight: 900;
      font-size: 28rpx;
    }
  }
}

// 成功弹窗
.success-modal {
  position: fixed;
  left: 50%;
  top: 50%;
  z-index: 100;
  transform: translate(-50%, -50%);
  box-sizing: border-box;
  padding: 60rpx 40rpx;
  text-align: center;
  width: 600rpx;
  max-width: calc(100vw - 64rpx);
  border-radius: 20rpx;
  background: #161616;
  border: 1rpx solid rgba(212, 175, 55, 0.24);
  box-shadow: 0 24rpx 80rpx rgba(0, 0, 0, 0.5);

  .success-icon {
    width: 96rpx;
    height: 96rpx;
    margin: 0 auto 22rpx;
    border-radius: 50%;
    background: rgba(82, 196, 26, 0.14);
    border: 1rpx solid rgba(82, 196, 26, 0.28);
    color: #52c41a;
    font-size: 52rpx;
    font-weight: 900;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 20rpx;
  }

  .success-title {
    font-size: 34rpx;
    font-weight: 900;
    color: #f5f5f5;
    margin-bottom: 16rpx;
  }

  .success-desc {
    display: flex;
    flex-direction: column;
    gap: 8rpx;
    font-size: 26rpx;
    color: #9a9a9a;
    margin-bottom: 40rpx;
  }

  .success-actions {
    display: flex;
    flex-direction: column;
    gap: 20rpx;

    .view-btn {
      height: 80rpx;
      background: linear-gradient(135deg, #d8b84a 0%, #b68a24 100%);
      border-radius: 40rpx;
      color: #111;
      font-weight: 900;
      font-size: 28rpx;
    }

    .back-btn {
      height: 80rpx;
      border: 1rpx solid rgba(255, 255, 255, 0.12);
      border-radius: 40rpx;
      background: #202020;
      color: #ddd;
      font-size: 28rpx;
    }
  }
}
</style>
