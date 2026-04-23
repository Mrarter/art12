<template>
  <view class="withdraw-page">
    <!-- 余额卡片 -->
    <view class="balance-card">
      <view class="balance-header">
        <text class="label">可提现余额（元）</text>
        <view class="refresh-btn" @click="refreshBalance">
          
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

    <!-- 提现规则 -->
    <view class="rules-section">
      <view class="rules-header" @click="showRules = !showRules">
        <text class="rules-title">提现规则</text>
        
      </view>
      <view class="rules-content" v-if="showRules">
        <view class="rule-item">
          <view class="rule-icon">1</view>
          <view class="rule-text">
            <text class="rule-title">最低提现金额</text>
            <text class="rule-desc">单次提现金额不低于 {{ minAmount }} 元</text>
          </view>
        </view>
        <view class="rule-item">
          <view class="rule-icon">2</view>
          <view class="rule-text">
            <text class="rule-title">提现手续费</text>
            <text class="rule-desc">每笔提现收取 {{ feeRate }}% 手续费</text>
          </view>
        </view>
        <view class="rule-item">
          <view class="rule-icon">3</view>
          <view class="rule-text">
            <text class="rule-title">到账时间</text>
            <text class="rule-desc">申请后 1-3 个工作日到账</text>
          </view>
        </view>
        <view class="rule-item">
          <view class="rule-icon">4</view>
          <view class="rule-text">
            <text class="rule-title">每日限额</text>
            <text class="rule-desc">每日最多提现 {{ dailyLimit }} 次</text>
          </view>
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
          <text class="fee-note">（含手续费 ¥{{ serviceFee }}）</text>
        </view>
      </view>

      <!-- 快速金额选择 -->
      <view class="quick-amounts">
        <view 
          class="quick-item" 
          :class="{ active: amount == balance * 0.25 }"
          @click="selectAmount(0.25)"
        >
          <text>25%</text>
        </view>
        <view 
          class="quick-item" 
          :class="{ active: amount == balance * 0.5 }"
          @click="selectAmount(0.5)"
        >
          <text>50%</text>
        </view>
        <view 
          class="quick-item" 
          :class="{ active: amount == balance * 0.75 }"
          @click="selectAmount(0.75)"
        >
          <text>75%</text>
        </view>
        <view 
          class="quick-item" 
          :class="{ active: amount == balance }"
          @click="selectAmount(1)"
        >
          <text>全部</text>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">到账方式</text>
        <view class="payment-method">
          <view class="method-item active">
            <image class="method-icon" src="/static/images/wechat-pay.png" mode="aspectFit"></image>
            <view class="method-info">
              <text class="method-name">微信零钱</text>
              <text class="method-desc">自动到账至微信零钱</text>
            </view>
            
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
        <text>📋</text>
        <text class="entry-text">提现记录</text>
      </view>
      <view class="entry-right">
        <text class="pending-count" v-if="pendingCount > 0">
          {{ pendingCount }} 笔处理中
        </text>
        
      </view>
    </view>

    <!-- 安全提示 -->
    <view class="security-tip">
      
      <text class="tip-text">资金安全由保险公司承保，请放心使用</text>
    </view>

    <!-- 提现确认弹窗 -->
    <!-- 弹窗开始 -->
      <view class="confirm-modal">
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
            <text class="confirm-value">微信零钱</text>
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
      <view class="success-modal">
        <view class="success-icon">
          
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

// 状态
const balance = ref(8888.88)
const todayEarnings = ref(288.50)
const monthEarnings = ref(5680.00)
const totalEarnings = ref(128800.00)
const amount = ref('')
const agreed = ref(false)
const showRules = ref(true)
const showConfirmModal = ref(false)
const showSuccessModal = ref(false)
const pendingCount = ref(1)

// 提现规则配置
const minAmount = ref(100)
const feeRate = ref(0.5)
const dailyLimit = ref(3)

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
  return amt >= minAmount.value && amt <= balance.value && agreed.value
})

// 方法
const refreshBalance = () => {
  uni.showLoading({ title: '刷新中...' })
  setTimeout(() => {
    uni.hideLoading()
    uni.showToast({ title: '已刷新', icon: 'success' })
  }, 500)
}

const onAmountInput = (e) => {
  const value = e.detail.value
  if (parseFloat(value) > balance.value) {
    amount.value = balance.value.toString()
  }
}

const selectAmount = (ratio) => {
  amount.value = (balance.value * ratio).toFixed(2)
}

const withdrawAll = () => {
  amount.value = balance.value.toString()
}

const onAgreeChange = (e) => {
  agreed.value = e.detail.value.includes('agree')
}

const showWithdrawAgreement = () => {
  uni.navigateTo({ url: '/pages/user/agreement?type=withdraw' })
}

const submitWithdraw = () => {
  if (!canSubmit.value) return
  showConfirmModal.value = true
}

const confirmSubmit = () => {
  showConfirmModal.value = false
  uni.showLoading({ title: '提交中...' })
  setTimeout(() => {
    uni.hideLoading()
    showSuccessModal.value = true
  }, 1000)
}

const goToRecords = () => {
  uni.navigateTo({ url: '/pages/promoter/withdrawLog' })
}

const goBack = () => {
  showSuccessModal.value = false
  uni.navigateBack()
}

onLoad(() => {
  // 加载用户余额信息
})
</script>

<style lang="scss" scoped>
.withdraw-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding: 20rpx;
}

.balance-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 24rpx;
  padding: 40rpx 30rpx;
  color: #fff;
  margin-bottom: 20rpx;

  .balance-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20rpx;

    .label {
      font-size: 28rpx;
      opacity: 0.9;
    }

    .refresh-btn {
      width: 48rpx;
      height: 48rpx;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.2);
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .balance-amount {
    display: flex;
    align-items: baseline;
    margin-bottom: 30rpx;

    .currency {
      font-size: 36rpx;
      margin-right: 8rpx;
    }

    .amount {
      font-size: 72rpx;
      font-weight: bold;
    }
  }

  .balance-stats {
    display: flex;
    justify-content: space-between;
    padding-top: 20rpx;
    border-top: 1rpx solid rgba(255, 255, 255, 0.2);

    .stat-item {
      flex: 1;
      text-align: center;

      .stat-label {
        font-size: 24rpx;
        opacity: 0.8;
        display: block;
        margin-bottom: 8rpx;
      }

      .stat-value {
        font-size: 28rpx;
        font-weight: 500;
      }
    }

    .stat-divider {
      width: 1rpx;
      background: rgba(255, 255, 255, 0.2);
    }
  }
}

.rules-section {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;

  .rules-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .rules-title {
      font-size: 28rpx;
      color: #333;
      font-weight: 500;
    }
  }

  .rules-content {
    margin-top: 20rpx;

    .rule-item {
      display: flex;
      align-items: flex-start;
      margin-bottom: 20rpx;

      &:last-child {
        margin-bottom: 0;
      }

      .rule-icon {
        width: 36rpx;
        height: 36rpx;
        border-radius: 50%;
        background: #667eea;
        color: #fff;
        font-size: 22rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 16rpx;
        flex-shrink: 0;
      }

      .rule-text {
        .rule-title {
          font-size: 26rpx;
          color: #333;
          display: block;
          margin-bottom: 4rpx;
        }

        .rule-desc {
          font-size: 24rpx;
          color: #999;
        }
      }
    }
  }
}

.form-section {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx 24rpx;
  margin-bottom: 20rpx;

  .form-title {
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 30rpx;
  }

  .form-item {
    margin-bottom: 30rpx;

    .form-label {
      font-size: 28rpx;
      color: #333;
      display: block;
      margin-bottom: 16rpx;
    }

    .amount-input {
      display: flex;
      align-items: center;
      border-bottom: 2rpx solid #eee;
      padding-bottom: 16rpx;

      .currency-prefix {
        font-size: 48rpx;
        color: #333;
        font-weight: bold;
        margin-right: 8rpx;
      }

      .amount-field {
        flex: 1;
        font-size: 48rpx;
        color: #333;
        font-weight: bold;
      }

      .all-btn {
        font-size: 28rpx;
        color: #667eea;
        padding: 8rpx 16rpx;
        background: rgba(102, 126, 234, 0.1);
        border-radius: 8rpx;
      }
    }

    .amount-hint {
      margin-top: 16rpx;
      font-size: 24rpx;
      color: #666;

      .fee-note {
        color: #999;
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
      background: #f5f6f8;
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

  .payment-method {
    .method-item {
      display: flex;
      align-items: center;
      padding: 20rpx;
      border: 2rpx solid #eee;
      border-radius: 16rpx;

      &.active {
        border-color: #07c160;
        background: rgba(7, 193, 96, 0.05);
      }

      .method-icon {
        width: 48rpx;
        height: 48rpx;
        margin-right: 16rpx;
      }

      .method-info {
        flex: 1;

        .method-name {
          font-size: 28rpx;
          color: #333;
          display: block;
        }

        .method-desc {
          font-size: 24rpx;
          color: #999;
        }
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
        color: #666;
        margin-left: 8rpx;
      }

      .link {
        color: #667eea;
      }
    }
  }

  .submit-btn {
    width: 100%;
    height: 88rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    font-size: 32rpx;
    font-weight: 500;
    border-radius: 44rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;

    &.disabled {
      background: #ccc;
    }
  }
}

.records-entry {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;

  .entry-left {
    display: flex;
    align-items: center;

    .entry-text {
      font-size: 28rpx;
      color: #333;
      margin-left: 12rpx;
    }
  }

  .entry-right {
    display: flex;
    align-items: center;

    .pending-count {
      font-size: 24rpx;
      color: #ff7f50;
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
    color: #999;
    margin-left: 8rpx;
  }
}

// 确认弹窗
.confirm-modal {
  padding: 40rpx;
  width: 600rpx;

  .modal-title {
    font-size: 34rpx;
    font-weight: 600;
    text-align: center;
    margin-bottom: 30rpx;
  }

  .modal-content {
    .confirm-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20rpx 0;
      border-bottom: 1rpx solid #f0f0f0;

      .confirm-label {
        font-size: 28rpx;
        color: #666;
      }

      .confirm-value {
        font-size: 28rpx;
        color: #333;

        &.primary {
          color: #e74c3c;
          font-weight: bold;
        }

        &.highlight {
          color: #07c160;
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
      border: 2rpx solid #ddd;
      border-radius: 40rpx;
      background: #fff;
      font-size: 28rpx;
      color: #666;
    }

    .confirm-btn {
      flex: 1;
      height: 80rpx;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 40rpx;
      color: #fff;
      font-size: 28rpx;
    }
  }
}

// 成功弹窗
.success-modal {
  padding: 60rpx 40rpx;
  text-align: center;
  width: 600rpx;

  .success-icon {
    margin-bottom: 20rpx;
  }

  .success-title {
    font-size: 34rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 16rpx;
  }

  .success-desc {
    display: flex;
    flex-direction: column;
    gap: 8rpx;
    font-size: 26rpx;
    color: #666;
    margin-bottom: 40rpx;
  }

  .success-actions {
    display: flex;
    flex-direction: column;
    gap: 20rpx;

    .view-btn {
      height: 80rpx;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 40rpx;
      color: #fff;
      font-size: 28rpx;
    }

    .back-btn {
      height: 80rpx;
      border: 2rpx solid #ddd;
      border-radius: 40rpx;
      background: #fff;
      color: #666;
      font-size: 28rpx;
    }
  }
}
</style>
