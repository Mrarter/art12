<template>
  <view class="promoter-page">
    <!-- 头部信息 -->
    <view class="promoter-header">
      <view class="header-top">
        <view class="level-badge">
          <text>{{ levelName }}</text>
        </view>
      </view>
      <view class="header-stats">
        <view class="stat-item">
          <text class="stat-value">{{ formatPrice(data.totalCommission) }}</text>
          <text class="stat-label">累计佣金(元)</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-value">{{ formatPrice(data.withdrawableCommission) }}</text>
          <text class="stat-label">可提现(元)</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-value">{{ formatPrice(data.withdrawnCommission) }}</text>
          <text class="stat-label">已提现(元)</text>
        </view>
      </view>
      <view class="invite-code-section">
        <text class="invite-label">我的邀请码:</text>
        <text class="invite-code">{{ data.inviteCode || '暂无' }}</text>
        <button class="btn-copy" @click="copyCode">复制</button>
      </view>
    </view>

    <!-- 团队信息 -->
    <view class="team-section">
      <view class="section-card">
        <view class="card-icon">👥</view>
        <view class="card-info">
          <text class="card-value">{{ data.subordinateCount || 0 }}</text>
          <text class="card-label">团队人数</text>
        </view>
      </view>
      <view class="section-card" @click="goCommissionLog">
        <view class="card-icon">💰</view>
        <view class="card-info">
          <text class="card-value">{{ commissionCount }}</text>
          <text class="card-label">佣金记录</text>
        </view>
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="action-section">
      <button class="action-btn primary" @click="showWithdrawModal = true">
        申请提现
      </button>
      <button class="action-btn" @click="shareInvite">
        邀请好友
      </button>
    </view>

    <!-- 佣金规则 -->
    <view class="rules-section">
      <view class="section-title">佣金规则</view>
      <view class="rule-item">
        <view class="rule-header">
          <text class="rule-title">普通艺荐官</text>
          <text class="rule-level">Lv.1</text>
        </view>
        <view class="rule-content">
          <text>一级佣金: 10%</text>
          <text>二级佣金: 5%</text>
        </view>
      </view>
      <view class="rule-item">
        <view class="rule-header">
          <text class="rule-title">高级艺荐官</text>
          <text class="rule-level">Lv.2</text>
        </view>
        <view class="rule-content">
          <text>一级佣金: 15%</text>
          <text>二级佣金: 8%</text>
        </view>
      </view>
      <view class="rule-item">
        <view class="rule-header">
          <text class="rule-title">资深艺荐官</text>
          <text class="rule-level">Lv.3</text>
        </view>
        <view class="rule-content">
          <text>一级佣金: 20%</text>
          <text>二级佣金: 10%</text>
        </view>
      </view>
    </view>

    <!-- 提现弹窗 -->
    <uni-popup ref="withdrawPopup" type="bottom">
      <view class="withdraw-modal">
        <view class="modal-header">
          <text>申请提现</text>
          <text @click="showWithdrawModal = false">×</text>
        </view>
        <view class="modal-body">
          <view class="amount-input">
            <text class="currency">¥</text>
            <input type="digit" v-model="withdrawAmount" placeholder="请输入提现金额" />
          </view>
          <view class="available-amount">
            可提现余额: ¥{{ formatPrice(data.withdrawableCommission) }}
          </view>
          <view class="account-info">
            <view class="info-title">收款账户</view>
            <radio-group @change="onAccountTypeChange">
              <label class="account-option">
                <radio value="bank" checked />
                <text>银行卡</text>
              </label>
              <label class="account-option">
                <radio value="wechat" />
                <text>微信</text>
              </label>
            </radio-group>
          </view>
        </view>
        <view class="modal-footer">
          <button class="btn-withdraw" @click="applyWithdraw" :disabled="!withdrawAmount">
            确认提现
          </button>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getPromoterCenter, applyWithdraw as withdraw } from '@/api/promotion'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const data = ref({
  level: 1,
  totalCommission: 0,
  withdrawableCommission: 0,
  withdrawnCommission: 0,
  subordinateCount: 0,
  inviteCode: ''
})
const showWithdrawModal = ref(false)
const withdrawAmount = ref('')
const accountType = ref('bank')

const levelName = computed(() => {
  const map = { 1: '普通艺荐官', 2: '高级艺荐官', 3: '资深艺荐官' }
  return map[data.value.level] || '普通艺荐官'
})

const commissionCount = ref(0)

onMounted(() => {
  loadData()
})

async function loadData() {
  try {
    const res = await getPromoterCenter()
    data.value = res
  } catch (e) {
    data.value = {
      level: 1,
      totalCommission: 125800,
      withdrawableCommission: 35800,
      withdrawnCommission: 90000,
      subordinateCount: 28,
      inviteCode: 'SYJ12345678'
    }
  }
}

function formatPrice(price) {
  if (!price) return '0.00'
  return (price / 100).toFixed(2)
}

function copyCode() {
  uni.setClipboardData({
    data: data.value.inviteCode,
    success: () => {
      uni.showToast({ title: '已复制' })
    }
  })
}

function goCommissionLog() {
  uni.navigateTo({ url: '/pages/promoter/commission-log' })
}

function onAccountTypeChange(e) {
  accountType.value = e.detail.value
}

async function applyWithdraw() {
  if (!withdrawAmount.value) return
  
  try {
    await withdraw({
      amount: Math.floor(parseFloat(withdrawAmount.value) * 100),
      accountType: accountType.value,
      accountInfo: '',
      accountName: ''
    })
    uni.showToast({ title: '申请成功' })
    showWithdrawModal.value = false
    loadData()
  } catch (e) {
    uni.showToast({ title: '申请失败', icon: 'none' })
  }
}

function shareInvite() {
  uni.showShareMenu({
    withShareTicket: true
  })
}
</script>

<style lang="scss" scoped>
.promoter-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: env(safe-area-inset-bottom);
}

.promoter-header {
  background: linear-gradient(135deg, #333 0%, #1a1a1a 100%);
  padding: 48rpx 32rpx;
  color: #fff;
  
  .level-badge {
    display: inline-block;
    padding: 8rpx 24rpx;
    background: #c9a962;
    border-radius: 20rpx;
    font-size: 24rpx;
  }
  
  .header-stats {
    display: flex;
    justify-content: space-around;
    margin: 48rpx 0;
    
    .stat-item {
      text-align: center;
      
      .stat-value {
        display: block;
        font-size: 40rpx;
        font-weight: 700;
      }
      
      .stat-label {
        font-size: 22rpx;
        opacity: 0.8;
      }
    }
    
    .stat-divider {
      width: 1rpx;
      background: rgba(255, 255, 255, 0.2);
    }
  }
  
  .invite-code-section {
    display: flex;
    align-items: center;
    gap: 12rpx;
    padding: 24rpx;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 12rpx;
    
    .invite-label {
      font-size: 24rpx;
      opacity: 0.8;
    }
    
    .invite-code {
      flex: 1;
      font-size: 32rpx;
      font-weight: 600;
      letter-spacing: 2rpx;
    }
    
    .btn-copy {
      padding: 8rpx 24rpx;
      background: #c9a962;
      color: #fff;
      border-radius: 20rpx;
      font-size: 22rpx;
    }
  }
}

.team-section {
  display: flex;
  gap: 16rpx;
  padding: 24rpx 16rpx;
  
  .section-card {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 16rpx;
    padding: 24rpx;
    background: #fff;
    border-radius: 16rpx;
    
    .card-icon {
      font-size: 48rpx;
    }
    
    .card-info {
      .card-value {
        display: block;
        font-size: 36rpx;
        font-weight: 700;
        color: #333;
      }
      
      .card-label {
        font-size: 22rpx;
        color: #999;
      }
    }
  }
}

.action-section {
  display: flex;
  gap: 16rpx;
  padding: 0 16rpx 24rpx;
  
  .action-btn {
    flex: 1;
    height: 88rpx;
    background: #fff;
    color: #333;
    border-radius: 44rpx;
    font-size: 28rpx;
    
    &.primary {
      background: #c9a962;
      color: #fff;
    }
  }
}

.rules-section {
  background: #fff;
  padding: 24rpx;
  margin: 0 16rpx;
  border-radius: 16rpx;
  
  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    margin-bottom: 24rpx;
  }
  
  .rule-item {
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f0f0f0;
    
    &:last-child {
      border-bottom: none;
    }
    
    .rule-header {
      display: flex;
      align-items: center;
      gap: 12rpx;
      margin-bottom: 12rpx;
      
      .rule-title {
        font-size: 28rpx;
        color: #333;
      }
      
      .rule-level {
        font-size: 20rpx;
        padding: 4rpx 12rpx;
        background: #c9a962;
        color: #fff;
        border-radius: 4rpx;
      }
    }
    
    .rule-content {
      display: flex;
      gap: 32rpx;
      font-size: 24rpx;
      color: #666;
    }
  }
}

.withdraw-modal {
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
  
  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 32rpx;
    border-bottom: 1rpx solid #eee;
    font-size: 32rpx;
    font-weight: 600;
    
    text:last-child {
      font-size: 48rpx;
      color: #999;
    }
  }
  
  .modal-body {
    padding: 32rpx;
    
    .amount-input {
      display: flex;
      align-items: center;
      border-bottom: 2rpx solid #333;
      padding-bottom: 16rpx;
      
      .currency {
        font-size: 48rpx;
        font-weight: 700;
        margin-right: 8rpx;
      }
      
      input {
        flex: 1;
        font-size: 48rpx;
        font-weight: 700;
      }
    }
    
    .available-amount {
      margin-top: 16rpx;
      font-size: 24rpx;
      color: #999;
    }
    
    .account-info {
      margin-top: 32rpx;
      
      .info-title {
        font-size: 28rpx;
        margin-bottom: 16rpx;
      }
      
      .account-option {
        display: inline-flex;
        align-items: center;
        gap: 8rpx;
        margin-right: 32rpx;
        font-size: 28rpx;
      }
    }
  }
  
  .modal-footer {
    padding: 0 32rpx 32rpx;
    
    .btn-withdraw {
      width: 100%;
      height: 88rpx;
      background: #333;
      color: #fff;
      border-radius: 44rpx;
      font-size: 30rpx;
      
      &[disabled] {
        background: #ccc;
      }
    }
  }
}
</style>
